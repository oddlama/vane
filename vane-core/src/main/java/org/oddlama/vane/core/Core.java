package org.oddlama.vane.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.json.JSONException;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.enchantments.EnchantmentManager;
import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.item.CustomItemRegistry;
import org.oddlama.vane.core.item.CustomModelDataRegistry;
import org.oddlama.vane.core.item.DurabilityManager;
import org.oddlama.vane.core.item.ExistingItemConverter;
import org.oddlama.vane.core.item.VanillaFunctionalityInhibitor;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.MenuManager;
import org.oddlama.vane.core.misc.AuthMultiplexer;
import org.oddlama.vane.core.misc.CommandHider;
import org.oddlama.vane.core.misc.HeadLibrary;
import org.oddlama.vane.core.misc.LootChestProtector;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.core.resourcepack.ResourcePackDistributor;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import static org.oddlama.vane.util.Conversions.ms_to_ticks;
import static org.oddlama.vane.util.IOUtil.read_json_from_url;

@VaneModule(name = "core", bstats = 8637, config_version = 6, lang_version = 5, storage_version = 1)
public class Core extends Module<Core> {

    /** Use sparingly. */
    private static Core INSTANCE = null;

    public static Core instance() {
        return INSTANCE;
    }

    public EnchantmentManager enchantment_manager;
    private CustomModelDataRegistry model_data_registry;
    private CustomItemRegistry item_registry;

    @ConfigBoolean(
        def = true,
        desc = "Allow loading of player heads in relevant menus. Disabling this will show all player heads using the Steve skin, which may perform better on low-performance servers and clients."
    )
    public boolean config_player_heads_in_menus;

    @LangMessage
    public TranslatedMessage lang_command_not_a_player;

    @LangMessage
    public TranslatedMessage lang_command_permission_denied;

    @LangMessage
    public TranslatedMessage lang_invalid_time_format;

    // Module registry
    private SortedSet<Module<?>> vane_modules = new TreeSet<>((a, b) -> a.get_name().compareTo(b.get_name()));

    public final ResourcePackDistributor resource_pack_distributor;

    public void register_module(Module<?> module) {
        vane_modules.add(module);
    }

    public void unregister_module(Module<?> module) {
        vane_modules.remove(module);
    }

    public SortedSet<Module<?>> get_modules() {
        return Collections.unmodifiableSortedSet(vane_modules);
    }

    // Vane global command catch-all permission
    public Permission permission_command_catchall = new Permission(
        "vane.*.commands.*",
        "Allow access to all vane commands (ONLY FOR ADMINS!)",
        PermissionDefault.FALSE
    );

    public MenuManager menu_manager;

    // core-config
    @ConfigBoolean(
        def = true,
        desc = "Let the client translate messages using the generated resource pack. This allows every player to select their preferred language, and all plugin messages will also be translated. Disabling this won't allow you to skip generating the resource pack, as it will be needed for custom item textures."
    )
    public boolean config_client_side_translations;

    @ConfigBoolean(def = true, desc = "Send update notices to OPed player when a new version of vane is available.")
    public boolean config_update_notices;

    public String current_version = null;
    public String latest_version = null;

    public Core() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Cannot instanciate Core twice.");
        }
        INSTANCE = this;

        // Create global command catch-all permission
        register_permission(permission_command_catchall);

        // Allow registration of new enchantments and entities
        unfreeze_registries();

        // Components
        enchantment_manager = new EnchantmentManager(this);
        new HeadLibrary(this);
        new AuthMultiplexer(this);
        new LootChestProtector(this);
        new VanillaFunctionalityInhibitor(this);
        new DurabilityManager(this);
        new org.oddlama.vane.core.commands.Vane(this);
        new org.oddlama.vane.core.commands.CustomItem(this);
        new org.oddlama.vane.core.commands.Enchant(this);
        menu_manager = new MenuManager(this);
        resource_pack_distributor = new ResourcePackDistributor(this);
        new CommandHider(this);
        model_data_registry = new CustomModelDataRegistry();
        item_registry = new CustomItemRegistry();
        new ExistingItemConverter(this);
    }

    @Override
    public void on_enable() {
        if (config_update_notices) {
            // Now, and every hour after that, check if a new version is available.
            // OPs will get a message about this when they join.
            schedule_task_timer(this::check_for_update, 1l, ms_to_ticks(2 * 60l * 60l * 1000l));
        }
    }

    public void unfreeze_registries() {
        // NOTE: MAGIC VALUES! Introduced for 1.18.2 when registries were frozen. Sad, no workaround
        // at the time.
        try {
            // Make relevant fields accessible
            final var frozen = MappedRegistry.class.getDeclaredField("frozen"/* frozen */);
            frozen.setAccessible(true);
            final var intrusive_holder_cache =
                MappedRegistry.class.getDeclaredField(
                        "unregisteredIntrusiveHolders"/* unregisteredIntrusiveHolders (1.19.3+), intrusiveHolderCache (until 1.19.2) */
                    );
            intrusive_holder_cache.setAccessible(true);

            // Unfreeze required registries
            frozen.set(BuiltInRegistries.ENTITY_TYPE, false);
            intrusive_holder_cache.set(
                BuiltInRegistries.ENTITY_TYPE,
                new IdentityHashMap<EntityType<?>, Holder.Reference<EntityType<?>>>()
            );
            // Since 1.20.2 this is also needed for enchantments:
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            log.log(Level.SEVERE, "Failed to unfreeze registries", e);
        }
    }

    @Override
    public void on_disable() {}

    public File generate_resource_pack() {
        try {
            var file = new File("vane-resource-pack.zip");
            var pack = new ResourcePackGenerator();

            for (var m : vane_modules) {
                m.generate_resource_pack(pack);
            }

            pack.write(file);
            return file;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error while generating resourcepack", e);
            return null;
        }
    }

    public void for_all_module_components(final Consumer1<ModuleComponent<?>> f) {
        for (var m : vane_modules) {
            m.for_each_module_component(f);
        }
    }

    public CustomItemRegistry item_registry() {
        return item_registry;
    }

    public CustomModelDataRegistry model_data_registry() {
        return model_data_registry;
    }

    public void check_for_update() {
        if (current_version == null) {
            try {
                Properties properties = new Properties();
                properties.load(Core.class.getResourceAsStream("/vane-core.properties"));
                current_version = "v" + properties.getProperty("version");
            } catch (IOException e) {
                log.severe("Could not load current version from included properties file: " + e);
                return;
            }
        }

        try {
            final var json = read_json_from_url("https://api.github.com/repos/oddlama/vane/releases/latest");
            latest_version = json.getString("tag_name");
            if (latest_version != null && !latest_version.equals(current_version)) {
                log.warning(
                    "A newer version of vane is available online! (current=" +
                    current_version +
                    ", new=" +
                    latest_version +
                    ")"
                );
                log.warning("Please update as soon as possible to get the latest features and fixes.");
                log.warning("Get the latest release here: https://github.com/oddlama/vane/releases/latest");
            }
        } catch (IOException | JSONException | URISyntaxException e) {
            log.warning("Could not check for updates: " + e);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void on_player_join_send_update_notice(PlayerJoinEvent event) {
        if (!config_update_notices) {
            return;
        }

        // Send an update message if a new version is available and player is OP.
        if (latest_version != null && !latest_version.equals(current_version) && event.getPlayer().isOp()) {
            // This message is intentionally not translated to ensure it will
            // be displayed correctly and so that everyone understands it.
            event
                .getPlayer()
                .sendMessage(
                    Component.text("A new version of vane ", NamedTextColor.GREEN)
                        .append(Component.text("(" + latest_version + ")", NamedTextColor.AQUA))
                        .append(Component.text(" is available!", NamedTextColor.GREEN))
                );
            event
                .getPlayer()
                .sendMessage(Component.text("Please update soon to get the latest features.", NamedTextColor.GREEN));
            event
                .getPlayer()
                .sendMessage(
                    Component.text("Click here to go to the download page", NamedTextColor.AQUA).clickEvent(
                        ClickEvent.openUrl("https://github.com/oddlama/vane/releases/latest")
                    )
                );
        }
    }
}
