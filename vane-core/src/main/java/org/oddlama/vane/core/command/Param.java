package org.oddlama.vane.core.command;

import static org.oddlama.vane.util.Util.append;

import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.CombinedErrorCheckResult;
import org.oddlama.vane.core.command.params.AnyParam;
import org.oddlama.vane.core.command.params.ChoiceParam;
import org.oddlama.vane.core.Module;
import org.oddlama.vane.core.command.params.DynamicChoiceParam;
import org.oddlama.vane.core.command.params.FixedParam;
import org.oddlama.vane.core.command.params.SentinelExecutorParam;

import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.functional.Function3;
import org.oddlama.vane.core.functional.Function4;
import org.oddlama.vane.core.functional.Function5;
import org.oddlama.vane.core.functional.Function6;

public interface Param {
	public List<Param> get_params();
	default public void add_param(Param param) {
		get_params().add(param);
	}

	default public <T1> void exec(Function1<T1, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	default public <T1, T2> void exec(Function2<T1, T2, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	default public <T1, T2, T3> void exec(Function3<T1, T2, T3, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	default public <T1, T2, T3, T4> void exec(Function4<T1, T2, T3, T4, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	default public <T1, T2, T3, T4, T5> void exec(Function5<T1, T2, T3, T4, T5, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	default public <T1, T2, T3, T4, T5, T6> void exec(Function6<T1, T2, T3, T4, T5, T6, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	default public Param any_string() {
		return any("string", str -> str);
	}

	default public <T> AnyParam<? extends T> any(String argument_type, Function1<String, ? extends T> from_string) {
		final var p = new AnyParam<>(get_command(), argument_type, from_string);
		add_param(p);
		return p;
	}

	default public FixedParam<String> fixed(String fixed) {
		return fixed(fixed, str -> str);
	}

	default public <T> FixedParam<T> fixed(T fixed, Function1<T, String> to_string) {
		final var p = new FixedParam<>(get_command(), fixed, to_string);
		add_param(p);
		return p;
	}

	default public Param choice(Collection<String> choices) {
		return choice("choice", choices, str -> str);
	}

	default public <T> ChoiceParam<T> choice(String argument_type,
	                                Collection<? extends T> choices,
	                                Function1<T, String> to_string) {
		final var p = new ChoiceParam<>(get_command(), argument_type, choices, to_string);
		add_param(p);
		return p;
	}

	default public <T> DynamicChoiceParam<T> choice(String argument_type,
	                                Supplier<Collection<? extends T>> choices,
	                                Function1<T, String> to_string,
	                                Function1<String, ? extends T> from_string) {
		final var p = new DynamicChoiceParam<>(get_command(), argument_type, choices, to_string, from_string);
		add_param(p);
		return p;
	}

	default public DynamicChoiceParam<Module> choose_module() {
		return choice("module",
				() -> get_command().module.core.get_modules(),
				m -> m.get_name(),
				m -> get_command().module.core.get_modules().stream()
					.filter(k -> k.get_name().equalsIgnoreCase(m))
					.findFirst()
					.orElse(null));
	}

	default public DynamicChoiceParam<Player> choose_online_player() {
		return choice("online_player",
				() -> get_command().module.getServer().getOnlinePlayers(),
				p -> p.getName(),
				p -> get_command().module.getServer().getOnlinePlayers().stream()
					.filter(k -> k.getName().equals(p))
					.findFirst()
					.orElse(null));
	}

	public default CheckResult check_accept(String[] args, int offset) {
		if (get_params().isEmpty()) {
			throw new RuntimeException("Encountered parameter without sentinel! This is a bug.");
		}

		var results = get_params().stream()
			.map(p -> p.check_accept(args, offset + 1))
			.collect(Collectors.toList());

		// Return first executor result, if any
		for (var r : results) {
			if (r.good()) {
				return r;
			}
		}

		// Only retain errors from maximum depth
		var max_depth = results.stream()
			.map(r -> r.depth())
			.reduce(0, Integer::max);

		var errors = results.stream()
			.filter(r -> r.depth() == max_depth)
			.map(ErrorCheckResult.class::cast)
			.collect(Collectors.toList());

		// If there is only a single max-depth sub-error, propagate it.
		// Otherwise, combine multiple errors into new error.
		if (errors.size() == 1) {
			return errors.get(0);
		} else {
			return new CombinedErrorCheckResult(errors);
		}
	}

	public Command get_command();
}
