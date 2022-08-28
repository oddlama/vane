package org.oddlama.vane.core.command;

import static org.oddlama.vane.util.StorageUtil.namespaced_key;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
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
import org.oddlama.vane.core.module.Module;

@SuppressWarnings("overloads")
public interface Param {
	public List<Param> get_params();

	public default boolean require_player(CommandSender sender) {
		if (!(sender instanceof Player)) {
			get_command().get_module().core.lang_command_not_a_player.send(sender);
			return false;
		}

		return true;
	}

	public default boolean is_executor() {
		return false;
	}

	public default void add_param(Param param) {
		if (param.is_executor() && get_params().stream().anyMatch(p -> p.is_executor())) {
			throw new RuntimeException("Cannot define multiple executors for the same parameter! This is a bug.");
		}
		get_params().add(param);
	}

	public default <T1> void exec_player(Consumer1<T1> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player, i -> i == 0));
	}

	public default <T1, T2> void exec_player(Consumer2<T1, T2> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player, i -> i == 0));
	}

	public default <T1, T2, T3> void exec_player(Consumer3<T1, T2, T3> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player, i -> i == 0));
	}

	public default <T1, T2, T3, T4> void exec_player(Consumer4<T1, T2, T3, T4> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player, i -> i == 0));
	}

	public default <T1, T2, T3, T4, T5> void exec_player(Consumer5<T1, T2, T3, T4, T5> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player, i -> i == 0));
	}

	public default <T1, T2, T3, T4, T5, T6> void exec_player(Consumer6<T1, T2, T3, T4, T5, T6> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player, i -> i == 0));
	}

	public default <T1> void exec_player(Function1<T1, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player, i -> i == 0));
	}

	public default <T1, T2> void exec_player(Function2<T1, T2, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player, i -> i == 0));
	}

	public default <T1, T2, T3> void exec_player(Function3<T1, T2, T3, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player, i -> i == 0));
	}

	public default <T1, T2, T3, T4> void exec_player(Function4<T1, T2, T3, T4, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player, i -> i == 0));
	}

	public default <T1, T2, T3, T4, T5> void exec_player(Function5<T1, T2, T3, T4, T5, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player, i -> i == 0));
	}

	public default <T1, T2, T3, T4, T5, T6> void exec_player(Function6<T1, T2, T3, T4, T5, T6, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f, this::require_player, i -> i == 0));
	}

	public default <T1> void exec(Consumer1<T1> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	public default <T1, T2> void exec(Consumer2<T1, T2> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	public default <T1, T2, T3> void exec(Consumer3<T1, T2, T3> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	public default <T1, T2, T3, T4> void exec(Consumer4<T1, T2, T3, T4> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	public default <T1, T2, T3, T4, T5> void exec(Consumer5<T1, T2, T3, T4, T5> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	public default <T1, T2, T3, T4, T5, T6> void exec(Consumer6<T1, T2, T3, T4, T5, T6> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	public default <T1> void exec(Function1<T1, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	public default <T1, T2> void exec(Function2<T1, T2, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	public default <T1, T2, T3> void exec(Function3<T1, T2, T3, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	public default <T1, T2, T3, T4> void exec(Function4<T1, T2, T3, T4, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	public default <T1, T2, T3, T4, T5> void exec(Function5<T1, T2, T3, T4, T5, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	public default <T1, T2, T3, T4, T5, T6> void exec(Function6<T1, T2, T3, T4, T5, T6, Boolean> f) {
		add_param(new SentinelExecutorParam<>(get_command(), f));
	}

	public default Param any_string() {
		return any("string", str -> str);
	}

	public default <T> AnyParam<? extends T> any(String argument_type, Function1<String, ? extends T> from_string) {
		var p = new AnyParam<>(get_command(), argument_type, from_string);
		add_param(p);
		return p;
	}

	public default FixedParam<String> fixed(String fixed) {
		return fixed(fixed, str -> str);
	}

	public default <T> FixedParam<T> fixed(T fixed, Function1<T, String> to_string) {
		var p = new FixedParam<>(get_command(), fixed, to_string);
		add_param(p);
		return p;
	}

	public default Param choice(Collection<String> choices) {
		return choice("choice", choices, str -> str);
	}

	public default <T> ChoiceParam<T> choice(
		String argument_type,
		Collection<? extends T> choices,
		Function1<T, String> to_string
	) {
		var p = new ChoiceParam<>(get_command(), argument_type, choices, to_string);
		add_param(p);
		return p;
	}

	public default <T> DynamicChoiceParam<T> choice(
		String argument_type,
		Function1<CommandSender, Collection<? extends T>> choices,
		Function2<CommandSender, T, String> to_string,
		Function2<CommandSender, String, ? extends T> from_string
	) {
		var p = new DynamicChoiceParam<>(get_command(), argument_type, choices, to_string, from_string);
		add_param(p);
		return p;
	}

	public default DynamicChoiceParam<Module<?>> choose_module() {
		return choice(
			"module",
			sender -> get_command().get_module().core.get_modules(),
			(sender, m) -> m.get_name(),
			(sender, str) ->
				get_command()
					.get_module()
					.core.get_modules()
					.stream()
					.filter(k -> k.get_name().equalsIgnoreCase(str))
					.findFirst()
					.orElse(null)
		);
	}

	public default DynamicChoiceParam<World> choose_world() {
		return choice(
			"world",
			sender -> get_command().get_module().getServer().getWorlds(),
			(sender, w) -> w.getName().toLowerCase(),
			(sender, str) ->
				get_command()
					.get_module()
					.getServer()
					.getWorlds()
					.stream()
					.filter(w -> w.getName().equalsIgnoreCase(str))
					.findFirst()
					.orElse(null)
		);
	}

	public default DynamicChoiceParam<OfflinePlayer> choose_any_player() {
		return choice(
			"any_player",
			sender -> get_command().get_module().get_offline_players_with_valid_name(),
			(sender, p) -> p.getName(),
			(sender, str) ->
				get_command()
					.get_module()
					.get_offline_players_with_valid_name()
					.stream()
					.filter(k -> k.getName().equalsIgnoreCase(str))
					.findFirst()
					.orElse(null)
		);
	}

	public default DynamicChoiceParam<Player> choose_online_player() {
		return choice(
			"online_player",
			sender -> get_command().get_module().getServer().getOnlinePlayers(),
			(sender, p) -> p.getName(),
			(sender, str) ->
				get_command()
					.get_module()
					.getServer()
					.getOnlinePlayers()
					.stream()
					.filter(k -> k.getName().equalsIgnoreCase(str))
					.findFirst()
					.orElse(null)
		);
	}

	// TODO (minor): Make choose_permission filter results based on the previously specified player.
	public default DynamicChoiceParam<Permission> choose_permission() {
		return choice(
			"permission",
			sender -> get_command().get_module().getServer().getPluginManager().getPermissions(),
			(sender, p) -> p.getName(),
			(sender, str) -> get_command().get_module().getServer().getPluginManager().getPermission(str)
		);
	}

	public default ChoiceParam<GameMode> choose_gamemode() {
		return choice("gamemode", List.of(GameMode.values()), m -> m.name().toLowerCase()).ignore_case();
	}

	public default DynamicChoiceParam<Enchantment> choose_enchantment() {
		return choose_enchantment((sender, e) -> true);
	}

	public default DynamicChoiceParam<Enchantment> choose_enchantment(
		final Function2<CommandSender, Enchantment, Boolean> filter
	) {
		return choice(
			"enchantment",
			sender ->
				Arrays.stream(Enchantment.values()).filter(e -> filter.apply(sender, e)).collect(Collectors.toList()),
			(sender, e) -> e.getKey().toString(),
			(sender, str) -> {
				var parts = str.split(":");
				if (parts.length != 2) {
					return null;
				}
				var e = Enchantment.getByKey(namespaced_key(parts[0], parts[1]));
				if (!filter.apply(sender, e)) {
					return null;
				}
				return e;
			}
		);
	}

	public default CheckResult check_accept_delegate(CommandSender sender, String[] args, int offset) {
		if (get_params().isEmpty()) {
			throw new RuntimeException("Encountered parameter without sentinel! This is a bug.");
		}

		// Delegate to children
		var results = get_params()
				.stream()
				.map(p -> p.check_accept(sender, args, offset + 1)).toList();

		// Return first executor result, if any
		for (var r : results) {
			if (r.good()) {
				return r;
			}
		}

		// Only retain errors from maximum depth
		var max_depth = results.stream().map(r -> r.depth()).reduce(0, Integer::max);

		var errors = results
			.stream()
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

	public default CheckResult check_accept(CommandSender sender, String[] args, int offset) {
		var result = check_parse(sender, args, offset);
		if (!(result instanceof ParseCheckResult)) {
			return result;
		}

		var p = (ParseCheckResult) result;
		return check_accept_delegate(sender, args, offset).prepend(p.argument_type(), p.parsed(), p.include_param());
	}

	public default List<String> build_completions_delegate(CommandSender sender, String[] args, int offset) {
		if (get_params().isEmpty()) {
			throw new RuntimeException("Encountered parameter without sentinel! This is a bug.");
		}

		// Delegate to children
		return get_params()
			.stream()
			.map(p -> p.build_completions(sender, args, offset + 1))
			.flatMap(Collection::stream)
			.collect(Collectors.toList());
	}

	public default List<String> build_completions(CommandSender sender, String[] args, int offset) {
		if (offset != args.length - 1) {
			// We are not the last argument.
			// Delegate completion to children if the param accepts the given arguments,
			// return no completions if it doesn't
			if (check_parse(sender, args, offset) instanceof ParseCheckResult) {
				return build_completions_delegate(sender, args, offset);
			} else {
				return Collections.emptyList();
			}
		} else {
			// We are the parameter that needs to be completed.
			// Offer (partial) completions if our depth is the completion depth
			return completions_for(sender, args, offset);
		}
	}

	public List<String> completions_for(CommandSender sender, String[] args, int offset);

	public CheckResult check_parse(CommandSender sender, String[] args, int offset);

	public Command<?> get_command();
}
