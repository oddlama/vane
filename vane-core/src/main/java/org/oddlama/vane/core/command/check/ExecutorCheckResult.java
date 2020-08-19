package org.oddlama.vane.core.command.check;

import org.oddlama.vane.core.command.Executor;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Collections;

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

	public boolean apply(CommandSender sender) {
		System.out.println("APPLY! " + executor);
		for (var o : parsed_args) {
			System.out.println(o);
		}
		return true;
	}

	@Override
	public CheckResult prepend(String argument_type, Object parsed_arg) {
		parsed_args.add(0, parsed_arg);
		return this;
	}
}
