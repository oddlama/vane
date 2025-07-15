package org.oddlama.vane.core.command.params;

import java.util.ArrayList;
import java.util.List;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.Param;

public abstract class BaseParam implements Param {

    private Command<?> command;
    private List<Param> params = new ArrayList<>();

    public BaseParam(Command<?> command) {
        this.command = command;
    }

    @Override
    public List<Param> get_params() {
        return params;
    }

    @Override
    public Command<?> get_command() {
        return command;
    }
}
