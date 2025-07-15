package org.oddlama.vane.core.command.params;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.ParseCheckResult;
import org.oddlama.vane.core.functional.Function1;

public class AnyParam<T> extends BaseParam {

    private String argument_type;
    private Function1<String, ? extends T> from_string;

    public AnyParam(Command<?> command, String argument_type, Function1<String, ? extends T> from_string) {
        super(command);
        this.argument_type = argument_type;
        this.from_string = from_string;
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
        return Collections.emptyList();
    }

    private T parse(String arg) {
        return from_string.apply(arg);
    }
}
