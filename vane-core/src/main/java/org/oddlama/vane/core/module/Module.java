package org.oddlama.vane.core.module;

import static org.oddlama.vane.util.ResourceList.get_resources;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bstats.bukkit.Metrics;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.loot.LootTables;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.LootTable;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.config.ConfigManager;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.lang.LangManager;
import org.oddlama.vane.core.persistent.PersistentStorageManager;

public abstract class Module<T extends Module<T>> extends JavaPlugin implements Context<T>, org.bukkit.event.Listener {

	public final VaneModule annotation = getClass().getAnnotation(VaneModule.class);
	public Core core;
	public Logger log = getLogger();
	public ComponentLogger clog = getComponentLogger();
	private final String namespace = "vane_" + annotation.name().replaceAll("[^a-zA-Z0-9_]", "_");

	// Managers
	public ConfigManager config_manager = new ConfigManager(this);
	public LangManager lang_manager = new LangManager(this);
	public PersistentStorageManager persistent_storage_manager = new PersistentStorageManager(this);
	private boolean persistent_storage_dirty = false;

	// Per module catch-all permissions
	public Permission permission_command_catchall_module;
	public Random random = new Random();

	// Permission attachment for console
	private List<String> pending_console_permissions = new ArrayList<>();
	public PermissionAttachment console_attachment;

	// Version fields for config, lang, storage
	@ConfigVersion
	public long config_version;

	@LangVersion
	public long lang_version;

	@Persistent
	public long storage_version;

	// Base configuration
	@ConfigString(
		def = "inherit",
		desc = "The language for this module. The corresponding language file must be named lang-{lang}.yml. Specifying 'inherit' will load the value set for vane-core.",
		metrics = true
	)
	public String config_lang;

	@ConfigBoolean(
		def = true,
		desc = "Enable plugin metrics via bStats. You can opt-out here or via the global bStats configuration. All collected information is completely anonymous and publicly available."
	)
	public boolean config_metrics_enabled;

	// Context<T> interface proxy
	private ModuleGroup<T> context_group = new ModuleGroup<>(
		this,
		"",
		"The module will only add functionality if this is set to true."
	);

	@Override
	public void compile(ModuleComponent<T> component) {
		context_group.compile(component);
	}

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
	public Context<T> get_context() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get_module() {
		return (T) this;
	}

	@Override
	public String yaml_path() {
		return "";
	}

	@Override
	public String variable_yaml_path(String variable) {
		return variable;
	}

	@Override
	public boolean enabled() {
		return context_group.enabled();
	}

	// Callbacks for derived classes
	protected void on_load() {}

	public void on_enable() {}

	public void on_disable() {}

	public void on_config_change() {}

	public void on_generate_resource_pack() throws IOException {}

	public final void for_each_module_component(final Consumer1<ModuleComponent<?>> f) {
		context_group.for_each_module_component(f);
	}

	// Loot modification
	private final Map<NamespacedKey, LootTable> additional_loot_tables = new HashMap<>();

	// ProtocolLib
	public ProtocolManager protocol_manager;

	// bStats
	public Metrics metrics;

	public Module() {
		// Get core plugin reference, important for inherited configuration
		// and shared state between vane modules
		if (this.getName().equals("vane-core")) {
			core = (Core) this;
		} else {
			core = (Core) getServer().getPluginManager().getPlugin("vane-core");
		}

		// Create per module command catch-all permission
		permission_command_catchall_module =
			new Permission(
				"vane." + get_name() + ".commands.*",
				"Allow access to all vane-" + get_name() + " commands",
				PermissionDefault.FALSE
			);
		register_permission(permission_command_catchall_module);
	}

	/**
	 * The namespace used in resource packs
	 */
	public final String namespace() {
		return namespace;
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

		// Schedule persistent storage saving every minute
		schedule_task_timer(
			() -> {
				if (persistent_storage_dirty) {
					save_persistent_storage();
					persistent_storage_dirty = false;
				}
			},
			60 * 20,
			60 * 20
		);
	}

	@Override
	public void onDisable() {
		disable();

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
				config_manager.register_metrics(metrics);
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

	@Override
	public void generate_resource_pack(final ResourcePackGenerator pack) throws IOException {
		// Generate language
		final var pattern = Pattern.compile("lang-.*\\.yml");
		Arrays
			.stream(getDataFolder().listFiles((d, name) -> pattern.matcher(name).matches()))
			.sorted()
			.forEach(lang_file -> {
				final var yaml = YamlConfiguration.loadConfiguration(lang_file);
				try {
					lang_manager.generate_resource_pack(pack, yaml, lang_file);
				} catch (Exception e) {
					throw new RuntimeException(
						"Error while generating language for '" + lang_file + "' of module " + get_name(),
						e
					);
				}
			});

		on_generate_resource_pack(pack);
		context_group.generate_resource_pack(pack);
	}

	private boolean try_reload_configuration() {
		// Generate new file if not existing
		final var file = config_manager.standard_file();
		if (!file.exists() && !config_manager.generate_file(file, null)) {
			return false;
		}

		// Reload automatic variables
		return config_manager.reload(file);
	}

	private void update_lang_file(String lang_file) {
		final var file = new File(getDataFolder(), lang_file);
		final var file_version = YamlConfiguration.loadConfiguration(file).getLong("version", -1);
		long resource_version = -1;

		final var res = getResource(lang_file);
		try (final var reader = new InputStreamReader(res)) {
			resource_version = YamlConfiguration.loadConfiguration(reader).getLong("version", -1);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error while updating lang file '" + file + "' of module " + get_name(), e);
		}

		if (resource_version > file_version) {
			try {
				Files.copy(getResource(lang_file), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				log.log(Level.SEVERE, "Error while copying lang file '" + file + "' of module " + get_name(), e);
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

			if (lang_code == null) {
				// Core failed to load, so the server will be shutdown anyway.
				// Prevent an additional warning by falling back to en.
				lang_code = "en";
			} else if ("inherit".equals(lang_code)) {
				// Fallback to en in case 'inherit' is used in vane-core.
				lang_code = "en";
			}
		}

		// Generate new file if not existing
		final var file = new File(getDataFolder(), "lang-" + lang_code + ".yml");
		if (!file.exists()) {
			log.severe("Missing language file '" + file.getName() + "' for module " + get_name());
			return false;
		}

		// Reload automatic variables
		return lang_manager.reload(file);
	}

	public boolean reload_configuration() {
		boolean was_enabled = enabled();

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

		if (was_enabled && !enabled()) {
			// Disable plugin if needed
			disable();
		} else if (!was_enabled && enabled()) {
			// Enable plugin if needed
			enable();
		}

		config_change();
		return true;
	}

	public File get_persistent_storage_file() {
		// Generate new file if not existing
		return new File(getDataFolder(), "storage.json");
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

	public void mark_persistent_storage_dirty() {
		persistent_storage_dirty = true;
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
		if (
			!getServer()
				.getCommandMap()
				.register(command.get_name(), command.get_prefix(), command.get_bukkit_command())
		) {
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

	public LootTable loot_table(final LootTables table) {
		return loot_table(table.getKey());
	}

	public List<OfflinePlayer> get_offline_players_with_valid_name() {
		return Arrays
			.stream(getServer().getOfflinePlayers())
			.filter(k -> k.getName() != null)
			.collect(Collectors.toList());
	}

	public LootTable loot_table(final NamespacedKey key) {
		var additional_loot_table = additional_loot_tables.get(key);
		if (additional_loot_table == null) {
			additional_loot_table = new LootTable();
			additional_loot_tables.put(key, additional_loot_table);
		}
		return additional_loot_table;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void on_module_loot_generate(final LootGenerateEvent event) {
		final var loot_table = event.getLootTable();
		// Should never happen because according to the api this is @NotNull,
		// yet it happens for some people that copied their world from singleplayer to the server.
		if (loot_table == null) {
			return;
		}
		final var additional_loot_table = additional_loot_tables.get(loot_table.getKey());
		if (additional_loot_table == null) {
			return;
		}

		final var loc = event.getLootContext().getLocation();
		final var local_random = new Random(random.nextInt()
				+ (loc.getBlockX() & 0xffff << 16)
				+ (loc.getBlockY() & 0xffff << 32)
				+ (loc.getBlockZ() & 0xffff << 48));
		additional_loot_table.generate_loot(event.getLoot(), local_random);
	}
}
