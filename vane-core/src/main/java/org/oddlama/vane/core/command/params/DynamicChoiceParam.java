package org.oddlama.vane.core.command.params;

import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.Param;
import org.oddlama.vane.core.command.Command;

import java.util.Collection;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import java.util.function.Supplier;
import org.oddlama.vane.core.functional.Function1;

public class DynamicChoiceParam<T> extends BaseParam {
	private String argument_type;
	private Supplier<Collection<? extends T>> choices;
	private Function1<T, String> to_string;
	private Function1<String, ? extends T> from_string;

	public DynamicChoiceParam(Command command, String argument_type, Supplier<Collection<? extends T>> choices, Function1<T, String> to_string, Function1<String, ? extends T> from_string) {
		super(command);
		this.argument_type = argument_type;
		this.choices = choices;
		this.to_string = to_string;;
		this.from_string = from_string;
	}

	@Override
	public CheckResult check_accept(String[] args, int offset) {
		if (args.length <= offset) {
			return new ErrorCheckResult(offset, "§6missing argument: §3" + argument_type + "§r");
		}
		var parsed = parse(args[offset]);
		if (parsed == null) {
			return new ErrorCheckResult(offset, "§6invalid §3" + argument_type + "§6: §b" + args[offset] + "§r");
		}
		return super.check_accept(args, offset)
			.prepend(argument_type, parsed);
	}

	private T parse(String arg) {
		return from_string.apply(arg);
	}
}
