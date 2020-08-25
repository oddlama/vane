package org.oddlama.vane.core.module;

import static org.oddlama.vane.util.ResourceList.get_resources;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import java.io.File;
import java.util.logging.Level;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.StringBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.permissions.PermissionAttachment;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import org.bstats.bukkit.Metrics;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.config.ConfigManager;
import org.oddlama.vane.core.lang.LangManager;
import org.oddlama.vane.core.persistent.PersistentStorageManager;

public abstract class Module<T extends Module<T>> extends JavaPlugin implements Context<T>, org.bukkit.event.Listener {
	public VaneModule annotation = getClass().getAnnotation(VaneModule.class);
	public Core core;
	public Logger log;

	// Managers
	public ConfigManager config_manager = new ConfigManager(this);
	public LangManager lang_manager = new LangManager(this);
	public PersistentStorageManager persistent_storage_manager = new PersistentStorageManager(this);

	// Vane global catch-all permission (only registered by core)
	public Permission permission_command_catchall = new Permission("vane.*.commands.*", "Allow access to all vane commands (ONLY FOR ADMINS!)", PermissionDefault.FALSE);
	// Per module catch-all permissions
	public Permission permission_command_catchall_module;

	// Permission attachment for console
	private List<String> pending_console_permissions = new ArrayList<>();
	private PermissionAttachment console_attachment;

	// Version fields for config, lang, storage
	@ConfigVersion
	public long config_version;
	@LangVersion
	public long lang_version;
	@Persistent
	public long storage_version = 0;

	// Base configuration
	@ConfigString(def = "inherit", desc = "The language for this module. The corresponding language file must be named lang-{lang}.yml. Specifying 'inherit' will load the value set for vane-core.")
	public String config_lang;

	@ConfigBoolean(def = true, desc = "Enable plugin metrics via bStats. You can opt-out here or via the global bStats configuration.")
	public boolean config_metrics_enabled;

	// Context<T> interface proxy
	private ModuleGroup<T> context_group = new ModuleGroup<>(this, "", "The module will only add functionality if this is set to true.");
	@Override
	public void compile(ModuleComponent<T> component) { context_group.compile(component); }
	@Override
	public void add_child(Context<T> subcontext) {
		if (context_group == null) {
			// This happens, when context_group (above) is initialized and calls compile_self(),
			// while will try to register it to the parent context (us), but we fake that anyway.
			return;
		}
		context_group.add_child(subcontext);
	}
	@Override
	public Context<T> get_context() { return this; }
	@SuppressWarnings("unchecked")
	@Override
	public T get_module() { return (T)this; }
	@Override
	public String yaml_path() { return ""; }
	@Override
	public String variable_yaml_path(String variable) { return variable; }

	// Callbacks for derived classes
	protected void on_load() {}
	public void on_enable() {}
	public void on_disable() {}
	public void on_config_change() {}

	// ProtocolLib
	public ProtocolManager protocol_manager;

	// bStats
	Metrics metrics;

	public Module() {
		// Create per module command catch-all permission
		permission_command_catchall_module = new Permission("vane." + get_name() + ".commands.*", "Allow access to all vane-" + get_name() + " commands", PermissionDefault.FALSE);
		register_permission(permission_command_catchall_module);
	}

	@Override
	public final void onLoad() {
		// Create data directory
		if (!getDataFolder().exists()) {
			getDataFolder().mkdirs();
		}

		on_load();
	}

	@Override
	public final void onEnable() {
		// Get logger
		log = getLogger();

		// Get core plugin reference, important for inherited configuration
		if (this.getName().equals("vane-core")) {
			core = (Core)this;
		} else {
			core = (Core)getServer().getPluginManager().getPlugin("vane-core");
		}

		// Create console permission attachment
		console_attachment = getServer().getConsoleSender().addAttachment(this);
		for (var perm : pending_console_permissions) {
			console_attachment.setPermission(perm, true);
		}
		pending_console_permissions.clear();

		// Register in core
		core.register_module(this);

		// Get protocollib manager
		protocol_manager = ProtocolLibrary.getProtocolManager();

		load_persistent_storage();
		reload_configuration();
	}

	@Override
	public void onDisable() {
		// Save persistent storage
		save_persistent_storage();

		// Unregister in core
		core.unregister_module(this);
	}

	@Override
	public void enable() {
		if (config_metrics_enabled) {
			var id = annotation.bstats();
			if (id != -1) {
				metrics = new Metrics(this, id);
			}
		}
		on_enable();
		context_group.enable();
		register_listener(this);
	}

	@Override
	public void disable() {
		unregister_listener(this);
		context_group.disable();
		on_disable();
		metrics = null;
	}

	@Override
	public void config_change() {
		on_config_change();
		context_group.config_change();
	}

	private boolean try_reload_configuration() {
		// Generate new file if not existing
		final var file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) {
			final var builder = new StringBuilder();
			config_manager.generate_yaml(builder);
			final var content = builder.toString();

			// Save content to file
			try {
				Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		// Reload automatic variables
		return config_manager.reload(file);
	}

	private void update_lang_file(String lang_file) {
		final var file = new File(getDataFolder(), lang_file);
		final var file_version = YamlConfiguration.loadConfiguration(file)
		                             .getLong("version", -1);
		long resource_version = -1;

		final var res = getResource(lang_file);
		try (final var reader = new InputStreamReader(res)) {
			resource_version = YamlConfiguration.loadConfiguration(reader)
			                       .getLong("version", -1);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (resource_version > file_version) {
			try {
				Files.copy(getResource(lang_file), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean try_reload_localization() {
		// Copy all embedded lang files, if their version is newer.
		get_resources(getClass(), Pattern.compile("lang-.*\\.yml")).stream().forEach(this::update_lang_file);

		// Get configured language code
		var lang_code = config_lang;
		if ("inherit".equals(lang_code)) {
			lang_code = core.config_lang;

			// Fallback to en in case 'inherit' is used in vane-core.
			if ("inherit".equals(lang_code)) {
				lang_code = "en";
			}
		}

		// Generate new file if not existing
		final var file = new File(getDataFolder(), "lang-" + lang_code + ".yml");
		if (!file.exists()) {
			log.severe("Missing language file '" + file.getName() + "'");
			return false;
		}

		// Reload automatic variables
		return lang_manager.reload(file);
	}

	public boolean reload_configuration() {
		boolean was_enabled = context_group.enabled();

		if (!try_reload_configuration()) {
			// Force stop server, we encountered an invalid config file
			log.severe("Invalid plugin configuration. Shutting down.");
			getServer().shutdown();
			return false;
		}

		// Reload localization
		if (!try_reload_localization()) {
			// Force stop server, we encountered an invalid lang file
			log.severe("Invalid localization file. Shutting down.");
			getServer().shutdown();
			return false;
		}

		if (was_enabled && !context_group.enabled()) {
			// Disable plugin if needed
			disable();
		} else if (!was_enabled && context_group.enabled()) {
			// Enable plugin if needed
			enable();
		}

		config_change();
		return true;
	}

	public File get_persistent_storage_file() {
		// Generate new file if not existing
		return new File(getDataFolder(), "storage.dat");
	}

	public void load_persistent_storage() {
		// Load automatic persistent variables
		final var file = get_persistent_storage_file();
		if (!persistent_storage_manager.load(file)) {
			// Force stop server, we encountered an invalid persistent storage file.
			// This prevents further corruption.
			log.severe("Invalid persistent storage. Shutting down to prevent further corruption.");
			getServer().shutdown();
		}
	}

	public void save_persistent_storage() {
		// Save automatic persistent variables
		final var file = get_persistent_storage_file();
		persistent_storage_manager.save(file);
	}

	public void register_listener(Listener listener) {
		getServer().getPluginManager().registerEvents(listener, this);
	}

	public void unregister_listener(Listener listener) {
		HandlerList.unregisterAll(listener);
	}

	public String get_name() {
		return annotation.name();
	}

	public void register_command(Command<?> command) {
		if (!getServer().getCommandMap().register(command.get_name(), command.get_prefix(), command.get_bukkit_command())) {
			log.warning("Command " + command.get_name() + " was registered using the fallback prefix!");
			log.warning("Another command with the same name already exists!");
		}
	}

	public void unregister_command(Command<?> command) {
		var bukkit_command = command.get_bukkit_command();
		getServer().getCommandMap().getKnownCommands().values().remove(bukkit_command);
		bukkit_command.unregister(getServer().getCommandMap());
	}

	public void add_console_permission(Permission permission) {
		add_console_permission(permission.getName());
	}

	public void add_console_permission(String permission) {
		if (console_attachment == null) {
			pending_console_permissions.add(permission);
		} else {
			console_attachment.setPermission(permission, true);
		}
	}

	public void register_permission(Permission permission) {
		try {
			getServer().getPluginManager().addPermission(permission);
		} catch (IllegalArgumentException e) {
			log.log(Level.SEVERE, "Permission '" + permission.getName() + "' was already defined", e);
		}
	}

	public void unregister_permission(Permission permission) {
		getServer().getPluginManager().removePermission(permission);
	}
}
