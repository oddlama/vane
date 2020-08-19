package org.oddlama.vane.core.command.check;

import org.oddlama.vane.core.command.Executor;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Collections;

public class ExecutorCheckResult implements CheckResult {
	private ArrayList<Object> parsed_args_reverse = new ArrayList<>();
	private Executor executor;

	public ExecutorCheckResult(Executor executor) {
		this.executor = executor;
	}

	@Override
	public boolean good() {
		return true;
	}

	public boolean apply(CommandSender sender) {
		var parsed_args = new ArrayList<>(parsed_args_reverse);
		Collections.reverse(parsed_args);

		System.out.println("APPLY!");
		for (var o : parsed_args) {
			System.out.println(o);
		}
		return true;
	}

	@Override
	public CheckResult prepend(Object parsed_arg) {
		parsed_args_reverse.add(parsed_arg);
		return this;
	}
}
