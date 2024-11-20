package org.oddlama.vane.core.command.check;

import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;

public class ParseCheckResult implements CheckResult {

    private int depth;
    private String argument_type;
    private Object parsed;
    private boolean include_param;

    public ParseCheckResult(int depth, String argument_type, Object parsed, boolean include_param) {
        this.depth = depth;
        this.argument_type = argument_type;
        this.parsed = parsed;
        this.include_param = include_param;
    }

    public String argument_type() {
        return argument_type;
    }

    public Object parsed() {
        return parsed;
    }

    public boolean include_param() {
        return include_param;
    }

    @Override
    public int depth() {
        return depth;
    }

    @Override
    public boolean good() {
        return true;
    }

    @Override
    public boolean apply(Command<?> command, CommandSender sender) {
        throw new RuntimeException("ParseCheckResult cannot be applied! This is a bug.");
    }

    @Override
    public CheckResult prepend(String argument_type, Object parsed_arg, boolean include) {
        throw new RuntimeException("Cannot prepend to ParseCheckResult! This is a bug.");
    }
}
