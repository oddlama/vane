package org.oddlama.vane.core.command.params;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.ParseCheckResult;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.Function2;

public class DynamicChoiceParam<T> extends BaseParam {

    private String argument_type;
    private Function1<CommandSender, Collection<? extends T>> choices;
    private Function2<CommandSender, T, String> to_string;
    private Function2<CommandSender, String, ? extends T> from_string;

    public DynamicChoiceParam(
        Command<?> command,
        String argument_type,
        Function1<CommandSender, Collection<? extends T>> choices,
        Function2<CommandSender, T, String> to_string,
        Function2<CommandSender, String, ? extends T> from_string
    ) {
        super(command);
        this.argument_type = argument_type;
        this.choices = choices;
        this.to_string = to_string;
        this.from_string = from_string;
    }

    @Override
    public CheckResult check_parse(CommandSender sender, String[] args, int offset) {
        if (args.length <= offset) {
            return new ErrorCheckResult(offset, "§6missing argument: §3" + argument_type + "§r");
        }
        var parsed = parse(sender, args[offset]);
        if (parsed == null) {
            return new ErrorCheckResult(offset, "§6invalid §3" + argument_type + "§6: §b" + args[offset] + "§r");
        }
        return new ParseCheckResult(offset, argument_type, parsed, true);
    }

    @Override
    public List<String> completions_for(CommandSender sender, String[] args, int offset) {
        return choices
            .apply(sender)
            .stream()
            .map(choice -> to_string.apply(sender, choice))
            .filter(str -> str.toLowerCase().contains(args[offset].toLowerCase()))
            .collect(Collectors.toList());
    }

    private T parse(CommandSender sender, String arg) {
        return from_string.apply(sender, arg);
    }
}
