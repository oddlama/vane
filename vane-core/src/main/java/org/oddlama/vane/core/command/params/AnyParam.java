package org.oddlama.vane.core.command.params;

import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.Param;
import org.oddlama.vane.core.command.Command;

import java.util.Optional;
import org.oddlama.vane.core.functional.Function1;

public class AnyParam<T> implements Param {
	private Command command;
	private Function1<String, ? extends T> from_string;

	public AnyParam(Command command, Function1<String, ? extends T> from_string) {
		this.command = command;
		this.from_string = from_string;
	}

	@Override
	public Command get_command() {
		return command;
	}

	@Override
	public CheckResult check_accept(String[] args, int offset) {
		if (args.length <= offset) {
			return new ErrorCheckResult("§6missing argument: §3" + "TODO" + "§r");
		}
		var parsed = parse(args[offset]);
		if (parsed == null) {
			return new ErrorCheckResult("§6invalid §3" + "TODO" + "§6: §3'" + args[offset] + "'§r");
		}
		return Param.super.check_accept(args, offset)
			.prepend(parsed);
	}

	private T parse(String arg) {
		return from_string.apply(arg);
	}
}
