package org.oddlama.vane.core.command.params;

import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.Param;
import org.oddlama.vane.core.command.Command;

import org.oddlama.vane.core.functional.Function1;

public class FixedParam<T> extends BaseParam {
	private T fixed_arg;
	private String fixed_arg_str;

	public FixedParam(Command command, T fixed_arg, Function1<T, String> to_string) {
		super(command);
		this.fixed_arg = fixed_arg;
		this.fixed_arg_str = to_string.apply(fixed_arg);
	}

	@Override
	public CheckResult check_accept(String[] args, int offset) {
		if (args.length <= offset) {
			return new ErrorCheckResult(offset, "§6missing argument: §3" + fixed_arg_str + "§r");
		}
		var parsed = parse(args[offset]);
		if (parsed == null) {
			return new ErrorCheckResult(offset, "§6invalid argument: expected §3" + fixed_arg_str + "§6 got §b" + args[offset] + "§r");
		}
		return super.check_accept(args, offset)
			.prepend(fixed_arg_str, parsed);
	}

	private T parse(String arg) {
		if (arg.equals(fixed_arg_str)) {
			return fixed_arg;
		} else {
			return null;
		}
	}
}
