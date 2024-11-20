package org.oddlama.vane.core.command.params;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.ParseCheckResult;
import org.oddlama.vane.core.functional.Function1;

public class FixedParam<T> extends BaseParam {

    private T fixed_arg;
    private String fixed_arg_str;
    private boolean include_param = false;
    private boolean ignore_case = false;

    public FixedParam(Command<?> command, T fixed_arg, Function1<T, String> to_string) {
        super(command);
        this.fixed_arg = fixed_arg;
        this.fixed_arg_str = to_string.apply(fixed_arg);
    }

    /** Will ignore the case of the given argument when matching */
    public FixedParam<T> ignore_case() {
        this.ignore_case = true;
        return this;
    }

    /** Will pass this fixed parameter as an argument to the executed function */
    public FixedParam<T> include_param() {
        this.include_param = true;
        return this;
    }

    @Override
    public CheckResult check_parse(CommandSender sender, String[] args, int offset) {
        if (args.length <= offset) {
            return new ErrorCheckResult(offset, "§6missing argument: §3" + fixed_arg_str + "§r");
        }
        var parsed = parse(args[offset]);
        if (parsed == null) {
            return new ErrorCheckResult(
                offset,
                "§6invalid argument: expected §3" + fixed_arg_str + "§6 got §b" + args[offset] + "§r"
            );
        }
        return new ParseCheckResult(offset, fixed_arg_str, parsed, include_param);
    }

    @Override
    public List<String> completions_for(CommandSender sender, String[] args, int offset) {
        return Collections.singletonList(fixed_arg_str);
    }

    private T parse(String arg) {
        if (ignore_case) {
            if (arg.equalsIgnoreCase(fixed_arg_str)) {
                return fixed_arg;
            }
        } else {
            if (arg.equals(fixed_arg_str)) {
                return fixed_arg;
            }
        }

        return null;
    }
}
