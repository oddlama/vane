package org.oddlama.vane.core.command.params;

import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.Param;
import org.oddlama.vane.core.command.Command;

import java.util.Optional;
import java.util.HashMap;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.function.Supplier;
import org.oddlama.vane.core.functional.Function1;

public class ChoiceParam<T> implements Param {
	private Class<T> persistent_class;
	private Command command;
	private HashMap<String, T> from_string = new HashMap<>();
	private Collection<? extends T> choices;

	@SuppressWarnings("unchecked")
	public ChoiceParam(Command command, Collection<? extends T> choices, Function1<T, String> to_string) {
		this.persistent_class = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		this.command = command;
		this.choices = choices;
		for (var c : choices) {
			from_string.put(to_string.apply(c), c);
		}
	}

	@Override
	public Command get_command() {
		return command;
	}

	@Override
	public CheckResult check_accept(String[] args, int offset) {
		if (args.length <= offset) {
			return new ErrorCheckResult("§6missing argument: §3" + persistent_class.getSimpleName() + "§r");
		}
		var parsed = parse(args[offset]);
		if (parsed == null) {
			return new ErrorCheckResult("§6invalid §3" + persistent_class.getSimpleName() + "§6: §3'" + args[offset] + "'§r");
		}
		return Param.super.check_accept(args, offset)
			.prepend(parsed);
	}

	private T parse(String arg) {
		return from_string.get(arg);
	}
}
