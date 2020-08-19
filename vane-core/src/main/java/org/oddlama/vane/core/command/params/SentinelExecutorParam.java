package org.oddlama.vane.core.command.params;

import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ExecutorCheckResult;
import java.util.stream.Collectors;
import java.util.Collections;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.Executor;
import org.oddlama.vane.core.command.Param;
import org.oddlama.vane.core.command.Command;

import org.oddlama.vane.core.functional.Function1;
import java.util.Optional;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.functional.Function3;
import org.oddlama.vane.core.functional.Function4;
import org.oddlama.vane.core.functional.Function5;
import org.oddlama.vane.core.functional.Function6;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

public class SentinelExecutorParam<T> extends BaseParam implements Executor {
	private T function;

	public SentinelExecutorParam(Command command, T function) {
		super(command);
		this.function = function;
	}

	@Override
	public void add_param(Param param) {
		throw new RuntimeException("Cannot add element to sentinel executor!");
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
