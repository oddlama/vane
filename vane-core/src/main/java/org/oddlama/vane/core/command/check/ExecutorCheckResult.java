package org.oddlama.vane.core.command.check;

import java.util.ArrayList;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.Executor;

public class ExecutorCheckResult implements CheckResult {

    private int depth;
    private Executor executor;
    private ArrayList<Object> parsed_args = new ArrayList<>();

    public ExecutorCheckResult(int depth, Executor executor) {
        this.depth = depth;
        this.executor = executor;
    }

    @Override
    public int depth() {
        return depth;
    }

    @Override
    public boolean good() {
        return true;
    }

    public boolean apply(Command<?> command, CommandSender sender) {
        return executor.execute(command, sender, parsed_args);
    }

    @Override
    public CheckResult prepend(String argument_type, Object parsed_arg, boolean include) {
        if (include) {
            parsed_args.add(0, parsed_arg);
        }
        return this;
    }
}
