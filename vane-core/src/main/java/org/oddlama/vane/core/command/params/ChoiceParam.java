package org.oddlama.vane.core.command.params;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.ParseCheckResult;
import org.oddlama.vane.core.functional.Function1;

public class ChoiceParam<T> extends BaseParam {

    private String argument_type;
    private Collection<? extends T> choices;
    private Function1<T, String> to_string;
    private HashMap<String, T> from_string = new HashMap<>();
    private boolean ignore_case = false;

    public ChoiceParam(
        Command<?> command,
        String argument_type,
        Collection<? extends T> choices,
        Function1<T, String> to_string
    ) {
        super(command);
        this.argument_type = argument_type;
        this.choices = choices;
        this.to_string = to_string;
        for (var c : choices) {
            from_string.put(to_string.apply(c), c);
        }
    }

    /** Will ignore the case of the given argument when matching */
    public ChoiceParam<T> ignore_case() {
        this.ignore_case = true;
        from_string.clear();
        for (var c : choices) {
            from_string.put(to_string.apply(c), c);
        }
        return this;
    }

    @Override
    public CheckResult check_parse(CommandSender sender, String[] args, int offset) {
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
    public List<String> completions_for(CommandSender sender, String[] args, int offset) {
        return choices
            .stream()
            .map(choice -> to_string.apply(choice))
            .filter(str -> str.toLowerCase().contains(args[offset].toLowerCase()))
            .collect(Collectors.toList());
    }

    private T parse(String arg) {
        if (ignore_case) {
            return from_string.get(arg.toLowerCase());
        } else {
            return from_string.get(arg);
        }
    }
}
