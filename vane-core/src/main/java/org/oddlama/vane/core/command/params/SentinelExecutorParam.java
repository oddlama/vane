package org.oddlama.vane.core.command.params;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;

import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.Executor;
import org.oddlama.vane.core.command.Param;
import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.ExecutorCheckResult;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.ErasedFunctor;
import org.oddlama.vane.core.functional.GenericsFinder;

public class SentinelExecutorParam<T> extends BaseParam implements Executor {
	private T function;
	private Function1<CommandSender, Boolean> check_requirements;

	public SentinelExecutorParam(Command command, T function, Function1<CommandSender, Boolean> check_requirements) {
		super(command);
		this.function = function;
		this.check_requirements = check_requirements;
	}

	private boolean check_signature(final Method method, final List<Object> args) {
		// Assert same amount of given and expected parameters
		if (args.size() != method.getParameters().length) {
			throw new RuntimeException(
					"Invalid command functor " + method.getDeclaringClass().getName() + "::" + method.getName() + "!"
					+ "\nFunctor takes " + method.getParameters().length + " parameters, but " + args.size() + " were given."
					+ "\nRequired: "
					+ Arrays.stream(method.getParameters()).map(p -> p.getType().getName()).collect(Collectors.toList())
					+ "\nGiven: "
					+ args.stream().map(p -> p.getClass().getName()).collect(Collectors.toList()));
		}

		// Assert assignable types
		for (int i = 0; i < args.size(); ++i) {
			var needs = method.getParameters()[i].getType();
			var got = args.get(i).getClass();
			if (!needs.isAssignableFrom(got)) {
				throw new RuntimeException(
						"Invalid command functor " + method.getDeclaringClass().getName() + "::" + method.getName() + "!"
						+ "\nArgument " + (i + 1) + " (" + needs.getName() + ") is not assignable from " + got.getName());
			}
		}
		return true;
	}

	@Override
	public boolean execute(Command command, CommandSender sender, List<Object> parsed_args) {
		// Replace command name argument (unused) with sender
		parsed_args.set(0, sender);

		// Get method reflection
		var gf = (GenericsFinder)function;
		var method = gf.method();

		// Check method signature against given argument types
		check_signature(method, parsed_args);

		// Check external requirements on the sender
		if (!check_requirements.apply(sender)) {
			return false;
		}

		// Check permission
		if (!sender.hasPermission(command.getPermission())) {
			// TODO localization
			sender.sendMessage("§cerror:§6 permission denied!");
			return false;
		}

		// Execute functor
		try {
			var result = ((ErasedFunctor)function).invoke(parsed_args);
			// Map null to "true" for consuming functions
			return result == null || (boolean)result;
		} catch (Exception e) {
			throw new RuntimeException("Error while invoking functor " + method.getDeclaringClass().getName() + "::" + method.getName() + "!", e);
		}
	}

	@Override
	public void add_param(Param param) {
		throw new RuntimeException("Cannot add element to sentinel executor! This is a bug.");
	}

	@Override
	public CheckResult check_parse(String[] args, int offset) {
		return null;
	}

	@Override
	public List<String> completions_for(String arg) {
		return Collections.emptyList();
	}

	@Override
	public CheckResult check_accept(String[] args, int offset) {
		if (args.length > offset) {
			// Excess arguments are an error of the previous level, so we subtract one from the offset (depth)
			// This will cause invalid arguments to be prioritized on optional arguments.
			// For example /vane reload [module], with an invalid module name should show "invalid module" over
			// excess arguments.
			return new ErrorCheckResult(offset - 1, "§6excess arguments: {" +
			                                          Arrays.stream(args, offset, args.length)
			                                              .map(s -> "§4" + s + "§6")
			                                              .collect(Collectors.joining(", ")) +
			                                          "}§r");
		} else if (args.length < offset) {
			throw new RuntimeException("Sentinel executor received missing arguments! This is a bug.");
		}
		return new ExecutorCheckResult(offset, this);
	}
}
