package org.oddlama.vane.core.command;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.oddlama.vane.core.Module;
import org.oddlama.vane.core.command.check.CheckResult;
import org.oddlama.vane.core.command.check.CombinedErrorCheckResult;
import org.oddlama.vane.core.command.check.ErrorCheckResult;
import org.oddlama.vane.core.command.check.ParseCheckResult;
import org.oddlama.vane.core.command.params.AnyParam;
import org.oddlama.vane.core.command.params.ChoiceParam;
import org.oddlama.vane.core.command.params.DynamicChoiceParam;
import org.oddlama.vane.core.command.params.FixedParam;
import org.oddlama.vane.core.command.params.SentinelExecutorParam;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.core.functional.Consumer3;
import org.oddlama.vane.core.functional.Consumer4;
import org.oddlama.vane.core.functional.Consumer5;
import org.oddlama.vane.core.functional.Consumer6;
import org.oddlama.vane.core.functional.Function1;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.functional.Function3;
import org.oddlama.vane.core.functional.Function4;
import org.oddlama.vane.core.functional.Function5;
import org.oddlama.vane.core.functional.Function6;

@SuppressWarnings("overloads")
public interface Param {
	public List<Param> get_params();

	default public boolean require_player(CommandSender sender) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(get_command().module.core.lang_command_not_a_player);
			return false;
		}

		return true;
	}

	default public void add_param(Param param) {
		get_params().add(param);
	}

	default public <T1> void exec_player(Consumer1<T1> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player));
	}

	default public <T1, T2> void exec_player(Consumer2<T1, T2> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player));
	}

	default public <T1, T2, T3> void exec_player(Consumer3<T1, T2, T3> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player));
	}

	default public <T1, T2, T3, T4> void exec_player(Consumer4<T1, T2, T3, T4> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player));
	}

	default public <T1, T2, T3, T4, T5> void exec_player(Consumer5<T1, T2, T3, T4, T5> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player));
	}

	default public <T1, T2, T3, T4, T5, T6> void exec_player(Consumer6<T1, T2, T3, T4, T5, T6> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player));
	}

	default public <T1> void exec_player(Function1<T1, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player));
	}

	default public <T1, T2> void exec_player(Function2<T1, T2, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player));
	}

	default public <T1, T2, T3> void exec_player(Function3<T1, T2, T3, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player));
	}

	default public <T1, T2, T3, T4> void exec_player(Function4<T1, T2, T3, T4, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player));
	}

	default public <T1, T2, T3, T4, T5> void exec_player(Function5<T1, T2, T3, T4, T5, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player));
	}

	default public <T1, T2, T3, T4, T5, T6> void exec_player(Function6<T1, T2, T3, T4, T5, T6, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player));
	}

	default public <T1> void exec(Consumer1<T1> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, s -> true));
	}

	default public <T1, T2> void exec(Consumer2<T1, T2> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, s -> true));
	}

	default public <T1, T2, T3> void exec(Consumer3<T1, T2, T3> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, s -> true));
	}

	default public <T1, T2, T3, T4> void exec(Consumer4<T1, T2, T3, T4> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, s -> true));
	}

	default public <T1, T2, T3, T4, T5> void exec(Consumer5<T1, T2, T3, T4, T5> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, s -> true));
	}

	default public <T1, T2, T3, T4, T5, T6> void exec(Consumer6<T1, T2, T3, T4, T5, T6> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, s -> true));
	}

	default public <T1> void exec(Function1<T1, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, s -> true));
	}

	default public <T1, T2> void exec(Function2<T1, T2, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, s -> true));
	}

	default public <T1, T2, T3> void exec(Function3<T1, T2, T3, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, s -> true));
	}

	default public <T1, T2, T3, T4> void exec(Function4<T1, T2, T3, T4, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, s -> true));
	}

	default public <T1, T2, T3, T4, T5> void exec(Function5<T1, T2, T3, T4, T5, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, s -> true));
	}

	default public <T1, T2, T3, T4, T5, T6> void exec(Function6<T1, T2, T3, T4, T5, T6, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, s -> true));
	}

	default public Param any_string() {
		return any("string", str -> str);
	}

	default public <T> AnyParam<? extends T> any(String argument_type, Function1<String, ? extends T> from_string) {
		var p = new AnyParam<>(get_command(), argument_type, from_string);
		add_param(p);
		return p;
	}

	default public FixedParam<String> fixed(String fixed) {
		return fixed(fixed, str -> str);
	}

	default public <T> FixedParam<T> fixed(T fixed, Function1<T, String> to_string) {
		var p = new FixedParam<>(get_command(), fixed, to_string);
		add_param(p);
		return p;
	}

	default public Param choice(Collection<String> choices) {
		return choice("choice", choices, str -> str);
	}

	default public <T> ChoiceParam<T> choice(String argument_type,
	                                Collection<? extends T> choices,
	                                Function1<T, String> to_string) {
		var p = new ChoiceParam<>(get_command(), argument_type, choices, to_string);
		add_param(p);
		return p;
	}

	default public <T> DynamicChoiceParam<T> choice(String argument_type,
	                                Supplier<Collection<? extends T>> choices,
	                                Function1<T, String> to_string,
	                                Function1<String, ? extends T> from_string) {
		var p = new DynamicChoiceParam<>(get_command(), argument_type, choices, to_string, from_string);
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

	default public CheckResult check_accept_delegate(String[] args, int offset) {
		if (get_params().isEmpty()) {
			throw new RuntimeException("Encountered parameter without sentinel! This is a bug.");
		}

		// Delegate to children
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

	default public CheckResult check_accept(String[] args, int offset) {
		var result = check_parse(args, offset);
		if (!(result instanceof ParseCheckResult)) {
			return result;
		}

		var p = (ParseCheckResult)result;
		return check_accept_delegate(args, offset)
			.prepend(p.argument_type(), p.parsed(), p.include_param());
	}

	default public List<String> build_completions_delegate(String[] args, int offset) {
		if (get_params().isEmpty()) {
			throw new RuntimeException("Encountered parameter without sentinel! This is a bug.");
		}

		// Delegate to children
		return get_params().stream()
			.map(p -> p.build_completions(args, offset + 1))
			.flatMap(Collection::stream)
			.collect(Collectors.toList());
	}

	default public List<String> build_completions(String[] args, int offset) {
		if (offset != args.length - 1) {
			// We are not the last argument.
			// Delegate completion to children if the param accepts the given arguments,
			// return no completions if it doesn't
			if (check_parse(args, offset) instanceof ParseCheckResult) {
				return build_completions_delegate(args, offset);
			} else {
				return Collections.emptyList();
			}
		} else {
			// We are the parameter that needs to be completed.
			// Offer (partial) completions if our depth is the completion depth
			return completions_for(args[offset]);
		}
	}

	public List<String> completions_for(String arg);
	public CheckResult check_parse(String[] args, int offset);
	public Command get_command();
}
