package org.oddlama.vane.core.command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static org.oddlama.vane.util.ArrayUtil.prepend;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.VaneCommand;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.command.params.AnyParam;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

@VaneCommand
public abstract class Command<T extends Module<T>> extends ModuleComponent<T> {

	public class BukkitCommand extends org.bukkit.command.Command implements PluginIdentifiableCommand {

		public BukkitCommand(String name) {
			super(name);
			setPermission(Command.this.permission.getName());
		}

		@Override
		public String getUsage() {
			return Command.this.lang_usage.str("§7/§3" + name);
		}

		@Override
		public String getDescription() {
			return Command.this.lang_description.str();
		}

		@Override
		public Plugin getPlugin() {
			return Command.this.get_module();
		}

		@Override
		public boolean execute(CommandSender sender, String alias, String[] args) {
			System.out.println("exec " + alias + " from " + sender);
			// Pre-check permission
			if (!sender.hasPermission(Command.this.permission)) {
				get_module().core.lang_command_permission_denied.send(sender);
				System.out.println("no perms!");
				return true;
			}

			// Ambiguous matches will always execute the
			// first chain based on definition order.
			try {
				return root_param.check_accept(sender, prepend(args, alias), 0).apply(Command.this, sender);
			} catch (Exception e) {
				sender.sendMessage(
					"§cAn unexpected error occurred. Please examine the console log and/or notify a server administrator."
				);
				throw e;
			}
		}

		@Override
		public List<String> tabComplete(CommandSender sender, String alias, String[] args)
			throws IllegalArgumentException {
			// Don't allow information exfiltration!
			if (!sender.hasPermission(getPermission())) {
				return Collections.emptyList();
			}

			try {
				return root_param.build_completions(sender, prepend(args, alias), 0);
			} catch (Exception e) {
				sender.sendMessage(
					"§cAn unexpected error occurred. Please examine the console log and/or notify a server administrator."
				);
				throw e;
			}
		}
	}

	// Language
	@LangMessage
	public TranslatedMessage lang_usage;

	@LangMessage
	public TranslatedMessage lang_description;

	@LangMessage
	public TranslatedMessage lang_help;

	// Variables
	private String name;
	private Permission permission;
	private BukkitCommand bukkit_command;

	// Root parameter
	private AnyParam<String> root_param;

	private LiteralArgumentBuilder<CommandSourceStack> brigadier_command;
	private Aliases aliases;

	public Command(Context<T> context) {
		this(context, PermissionDefault.OP);
	}
	public Command(Context<T> context, PermissionDefault permission_default) {
		super(null);
		// Make namespace
		name = getClass().getAnnotation(Name.class).value();
		context = context.group("command_" + name, "Enable command " + name);
		set_context(context);

		// Register permission
		permission =
			new Permission(
				"vane." + get_module().get_name() + ".commands." + name,
				"Allow access to /" + name,
				permission_default
			);
		get_module().register_permission(permission);
		permission.addParent(get_module().permission_command_catchall_module, true);
		permission.addParent(get_module().core.permission_command_catchall, true);

		// Always allow the console to execute commands
		get_module().add_console_permission(permission);

		// Initialize root parameter
		root_param = new AnyParam<String>(this, "/" + get_name(), str -> str);

		// Create bukkit command
		bukkit_command = new BukkitCommand(name);
		bukkit_command.setLabel(name);
		bukkit_command.setName(name);


		aliases = getClass().getAnnotation(Aliases.class);
		brigadier_command = Commands.literal(name);
		if (aliases != null) {
			bukkit_command.setAliases(List.of(aliases.value()));
		}
	}

	public BukkitCommand get_bukkit_command() {
		return bukkit_command;
	}

	public String get_name() {
		return name;
	}

	public String get_permission() {
		return permission.getName();
	}

	public String get_prefix() {
		return "vane:" + get_module().get_name();
	}

	public Param params() {
		return root_param;
	}

	public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {
		return brigadier_command;
	}

	public LiteralCommandNode<CommandSourceStack> get_command() {
		var cmd = get_command_base();
		var old_requirement = cmd.getRequirement();
		return cmd
			.requires(stack -> stack.getSender().hasPermission(permission) && old_requirement.test(stack))
			.build();
	}

	public List<String> get_aliases() {
		if (aliases != null && aliases.value().length > 0) {
			return List.of(aliases.value());
		} else {
			return Collections.emptyList();
		}

	}

	@Override
	protected void on_enable() {
		get_module().register_command(this);
	}

	@Override
	protected void on_disable() {
		get_module().unregister_command(this);
	}

	public void print_help(CommandSender sender) {
		lang_usage.send(sender, "§7/§3" + name);
		lang_help.send(sender);
	}

	public int print_help2(CommandContext<CommandSourceStack> ctx) {
		lang_usage.send(ctx.getSource().getSender(), "§7/§3" + name);
		lang_help.send(ctx.getSource().getSender());
		return com.mojang.brigadier.Command.SINGLE_SUCCESS;
	}

	public LiteralArgumentBuilder<CommandSourceStack> help(){
		return literal("help").executes(ctx -> {print_help2(ctx); return SINGLE_SUCCESS;});
	}

}
