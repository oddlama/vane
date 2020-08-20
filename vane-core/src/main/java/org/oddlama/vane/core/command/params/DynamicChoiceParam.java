package org.oddlama.vane.core.command.params;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.ParseCheckResult;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.util.Util;

public class DynamicChoiceParam<T> extends BaseParam {
	private String argument_type;
	private Supplier<Collection<? extends T>> choices;
	private Function1<T, String> to_string;
	private Function1<String, ? extends T> from_string;

	public DynamicChoiceParam(Command<?> command, String argument_type, Supplier<Collection<? extends T>> choices, Function1<T, String> to_string, Function1<String, ? extends T> from_string) {
		super(command);
		this.argument_type = argument_type;
		this.choices = choices;
		this.to_string = to_string;;
		this.from_string = from_string;
	}

	@Override
	public CheckResult check_parse(String[] args, int offset) {
		if (args.length <= offset) {
			return new ErrorCheckResult(offset, "§6missing argument: §3" + argument_type + "§r");
		}
		var parsed = parse(args[offset]);
		if (parsed == null) {
			return new ErrorCheckResult(offset, "§6invalid §3" + argument_type + "§6: §b" + args[offset] + "§r");
		}
		return new ParseCheckResult(offset, argument_type, parsed, true);
	}

	@Override
	public List<String> completions_for(final String arg) {
		//System.out.println("cpl");
		//Final var larg = arg.toLowerCase();
		//Return choices.get().stream()
		//	.map(choice -> to_string.apply(choice))
		//	.sorted((a, b) -> Util.compare_levenshtein(larg, a, b, 8))
		//	.collect(Collectors.toList());
		return choices.get().stream()
			.map(choice -> to_string.apply(choice))
			.collect(Collectors.toList());
	}

	private T parse(String arg) {
		return from_string.apply(arg);
	}
}
