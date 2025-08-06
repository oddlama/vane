package org.oddlama.vane.core.resourcepack;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.connection.configuration.PlayerConnectionReconfigureEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleGroup;
import org.oddlama.vane.util.Nms;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class ResourcePackDistributor extends Listener<Core> {

    // Assume debug environment if both add-plugin and vane-debug are defined, until run-paper adds
    // a better way.
    // https://github.com/jpenilla/run-paper/issues/14
    private static final boolean localDev =
        Nms.server_handle().options.hasArgument("add-plugin") && Boolean.getBoolean("disable.watchdog");

    @ConfigBoolean(
        def = true,
        desc = "Kick players if they deny to use the specified resource pack (if set). Individual players can be exempt from this rule by giving them the permission 'vane.core.resource_pack.bypass'."
    )
    public boolean config_force;

    @LangMessage
    public TranslatedMessage lang_declined;

    @LangMessage
    public TranslatedMessage lang_download_failed;

    public String url = null;
    public String sha1 = null;
    public UUID uuid = UUID.fromString("fbba121a-8f87-4e97-922d-2059777311bf");
    public int counter = 0;

    // The permission to bypass the resource pack
    public final Permission bypass_permission;

    public CustomResourcePackConfig custom_resource_pack_config;
    private ResourcePackFileWatcher file_watcher;
    private ResourcePackDevServer dev_server;

    private final Object2ObjectOpenHashMap<UUID, CountDownLatch> latches = new Object2ObjectOpenHashMap<>();

    public ResourcePackDistributor(Context<Core> context) {
        super(context.group("resource_pack", "Enable resource pack distribution."));

        custom_resource_pack_config = new CustomResourcePackConfig(get_context());

        // Register bypass permission
        bypass_permission = new Permission(
            "vane." + get_module().get_name() + ".resource_pack.bypass",
            "Allows bypassing an enforced resource pack",
            PermissionDefault.FALSE
        );
        get_module().register_permission(bypass_permission);
    }

    @Override
    public void on_enable() {
        if (localDev) {
            try {
                File pack_output = new File("vane-resource-pack.zip");
                if (!pack_output.exists()) {
                    get_module().log.info("Resource Pack Missing, first run? Generating resource pack.");
                    pack_output = get_module().generate_resource_pack();
                }
                file_watcher = new ResourcePackFileWatcher(this, pack_output);
                dev_server = new ResourcePackDevServer(this, pack_output);
                dev_server.serve();
                file_watcher.watch_for_changes();
            } catch (IOException | InterruptedException ignored) {
                ignored.printStackTrace();
            }

            get_module().log.info("Setting up dev lazy server");
        } else if (((ModuleGroup<Core>) custom_resource_pack_config.get_context()).config_enabled) {
            get_module().log.info("Serving custom resource pack");
            pack_url = custom_resource_pack_config.config_url;
            pack_sha1 = custom_resource_pack_config.config_sha1;
            pack_uuid = UUID.fromString(custom_resource_pack_config.config_uuid);
        } else {
            get_module().log.info("Serving official vane resource pack");
            try {
                Properties properties = new Properties();
                properties.load(Core.class.getResourceAsStream("/vane-core.properties"));
                pack_url = properties.getProperty("resource_pack_url");
                pack_sha1 = properties.getProperty("resource_pack_sha1");
                pack_uuid = UUID.fromString(properties.getProperty("resource_pack_uuid"));
            } catch (IOException e) {
                get_module().log.severe("Could not load official resource pack sha1 from included properties file");
                pack_url = "";
                pack_sha1 = "";
                pack_uuid = UUID.randomUUID();
            }
        }

        // Check sha1 sum validity
        if (pack_sha1.length() != 40) {
            get_module()
                .log.warning(
                    "Invalid resource pack SHA-1 sum '" +
                        pack_sha1 +
                    "', should be 40 characters long but has " +
                    pack_sha1.length() +
                    " characters"
                );
            get_module().log.warning("Disabling resource pack serving and message delaying");

            // Disable resource pack
            pack_url = "";
            // Prevent subcontexts from being enabling
            // FIXME this can be coded more cleanly. We need a way
            // to process config changes _before_ the module is enabled.
            // like on_config_change_pre_enable(), where we can override
            // the context group enable state.
            //((ModuleGroup<Core>) player_message_delayer.get_context()).config_enabled = false;
        }

        // Propagate enable after determining whether the player message delayer is active,
        // so it is only enabled when needed.
        super.on_enable();

        pack_sha1 = pack_sha1.toLowerCase();
        if (!pack_url.isEmpty()) {
            // Check if the server has a manually configured resource pack.
            // This would conflict.
            Nms.server_handle()
                .settings.getProperties()
                .serverResourcePackInfo.ifPresent(rp_info -> {
                    if (!rp_info.url().trim().isEmpty()) {
                        get_module()
                            .log.warning(
                                "You have manually configured a resource pack in your server.properties. This cannot be used together with vane, as servers only allow serving a single resource pack."
                            );
                    }
                });

            get_module().log.info("Distributing resource pack from '" + pack_url + "' with sha1 " + pack_sha1);
        }
    }

    @EventHandler
    public void on_player_async_connection_configure(AsyncPlayerConnectionConfigureEvent event) {
        var profile_uuid = event.getConnection().getProfile().getId();

        // Block the thread to prevent the question screen from going away
        var latch = new CountDownLatch(1);
        latches.put(profile_uuid, latch);

        send_resource_pack_during_configuration(event.getConnection());

        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            get_module().log.warning("Resource pack wait interrupted for player " + profile_uuid);
        }

        event.getConnection().completeReconfiguration();
    }

    @EventHandler
    public void on_player_connection_reconfigure(PlayerConnectionReconfigureEvent event) {
        send_resource_pack_during_configuration(event.getConnection());
    }

    @EventHandler
    public void on_player_connection_close(PlayerConnectionCloseEvent event) {
        // Cleanup
        Optional.ofNullable(latches.remove(event.getPlayerUniqueId())).ifPresent(CountDownLatch::countDown);
    }

    public void send_resource_pack_during_configuration(@NotNull PlayerConfigurationConnection connection) {
        var info = ResourcePackInfo.resourcePackInfo(pack_uuid, URI.create(pack_url), pack_sha1);
        var request = ResourcePackRequest.resourcePackRequest()
            .required(config_force).replace(true)
            .packs(info).callback((uuid, status, audience) -> {
                if (!status.intermediate()) {
                    Optional.ofNullable(latches.remove(connection.getProfile().getId())).ifPresent(CountDownLatch::countDown);
                }
            }).build();

        connection.getAudience().sendResourcePacks(request);
    }

    // For sending the resource pack during gameplay
    public void send_resource_pack(@NotNull Audience audience) {
        var url2 = pack_url;
        if (localDev) {
            url2 = pack_url + "?" + counter;
            audience.sendMessage(Component.text(url2 + " " + pack_sha1));
        }

        try {
            ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(pack_uuid, new URI(url2), pack_sha1);
            audience.sendResourcePacks(ResourcePackRequest.resourcePackRequest().packs(info).asResourcePackRequest());
        } catch (URISyntaxException e) {
            get_module().log.warning("The provided resource pack URL is incorrect: " + url2);
        }
    }

    // Not sure if this still needed?
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void on_player_status(final PlayerResourcePackStatusEvent event) {
        if (!config_force || event.getPlayer().hasPermission(bypass_permission)) {
            return;
        }

        switch (event.getStatus()) {
            case DECLINED:
                event.getPlayer().kick(lang_declined.str_component());
                break;
            case FAILED_DOWNLOAD:
                event.getPlayer().kick(lang_download_failed.str_component());
                break;
            default:
                break;
        }
    }

    @SuppressWarnings({ "deprecation", "UnstableApiUsage" })
    public void update_sha1(File file) {
        if (!localDev) return;
        try {
            var hash = Files.asByteSource(file).hash(Hashing.sha1());
            ResourcePackDistributor.this.pack_sha1 = hash.toString();
        } catch (IOException ignored) {}
    }
}
