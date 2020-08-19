package org.oddlama.vane.core.command.params;

import org.oddlama.vane.core.command.Param;
import org.oddlama.vane.core.command.Command;
import java.util.List;
import java.util.ArrayList;
import org.oddlama.vane.core.command.Executor;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Collections;

public abstract class BaseParam implements Param {
	private Command command;
	private List<Param> params = new ArrayList<>();

	public BaseParam(Command command) {
		this.command = command;
	}

	@Override
	public List<Param> get_params() {
		return params;
	}

	@Override
	public Command get_command() {
		return command;
	}
}
