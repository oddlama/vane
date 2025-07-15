package org.oddlama.vane.core.command.check;

import java.util.Objects;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;

public class ErrorCheckResult implements CheckResult {

    private int depth;
    private String message;
    private String arg_chain = "";

    public ErrorCheckResult(int depth, String message) {
        this.depth = depth;
        this.message = message;
    }

    @Override
    public int depth() {
        return depth;
    }

    @Override
    public boolean good() {
        return false;
    }

    @Override
    public boolean apply(Command<?> command, CommandSender sender) {
        return apply(command, sender, "");
    }

    public boolean apply(Command<?> command, CommandSender sender, String indent) {
        var str = indent;
        if (Objects.equals(indent, "")) {
            str += "§cerror: ";
        }
        str += "§6";
        str += arg_chain;
        str += message;
        sender.sendMessage(str);
        return false;
    }

    @Override
    public CheckResult prepend(String argument_type, Object parsed_arg, boolean include) {
        // Save parsed arguments in an argument chain, and propagate error
        arg_chain = "§3" + argument_type + "§6 → " + arg_chain;
        return this;
    }
}
