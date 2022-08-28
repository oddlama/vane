package org.oddlama.vane.core.resourcepack;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleGroup;
import org.oddlama.vane.util.Nms;

public class ResourcePackDistributor extends Listener<Core> {

	// Assume debug environment if both add-plugin and vane-debug are defined, until run-paper adds a better way.
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
	public int counter = 0;

	// The permission to bypass the resource pack
	public final Permission bypass_permission;

	public CustomResourcePackConfig custom_resource_pack_config;
	public PlayerMessageDelayer player_message_delayer;
	private ResourcePackFileWatcher file_watcher;
	private ResourcePackDevServer dev_server;

	public ResourcePackDistributor(Context<Core> context) {
		super(context.group("resource_pack", "Enable resource pack distribution."));
		// Delay messages if this the distributor is active.
		custom_resource_pack_config = new CustomResourcePackConfig(get_context());
		// Delay messages if this the distributor is active.
		player_message_delayer = new PlayerMessageDelayer(get_context());

		// Register bypass permission
		bypass_permission =
			new Permission(
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
				var pack_output = new File("vane-resource-pack.zip");
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
			url = custom_resource_pack_config.config_url;
			sha1 = custom_resource_pack_config.config_sha1;
		} else {
			get_module().log.info("Serving official vane resource pack");
			try {
				Properties properties = new Properties();
				properties.load(Core.class.getResourceAsStream("/vane-core.properties"));
				url = properties.getProperty("resource_pack_url");
				sha1 = properties.getProperty("resource_pack_sha1");
			} catch (IOException e) {
				get_module().log.severe("Could not load official resource pack sha1 from included properties file");
				url = "";
				sha1 = "";
			}
		}

		// Check sha1 sum validity
		if (sha1.length() != 40) {
			get_module()
				.log.warning(
					"Invalid resource pack SHA-1 sum '" +
					sha1 +
					"', should be 40 characters long but has " +
					sha1.length() +
					" characters"
				);
			get_module().log.warning("Disabling resource pack serving and message delaying");

			// Disable resource pack
			url = "";
			// Prevent subcontexts from being enabling
			// FIXME this can be coded more cleanly. We need a way
			// to process config changes _before_ the module is enabled.
			// like on_config_change_pre_enable(), where we can override
			// the context group enable state.
			((ModuleGroup<Core>) player_message_delayer.get_context()).config_enabled = false;
		}

		// Propagate enable after determining whether the player message delayer is active,
		// so it is only enabled when needed.
		super.on_enable();

		sha1 = sha1.toLowerCase();
		if (!url.isEmpty()) {
			// Check if the server has a manually configured resource pack.
			// This would conflict.
			Nms.server_handle().settings.getProperties().serverResourcePackInfo.ifPresent(rp_info -> {
				if (!rp_info.url().trim().isEmpty()) {
					get_module().log.warning("You have manually configured a resource pack in your server.properties. This cannot be used together with vane, as servers only allow serving a single resource pack.");
				}
			});

			get_module().log.info("Distributing resource pack from '" + url + "' with sha1 " + sha1);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_join(final PlayerJoinEvent event) {
		if (url.isEmpty()) {
			return;
		}
		send_resource_pack(event.getPlayer());
	}

	public void send_resource_pack(Player player) {
		var url2 = url;
		if (localDev) {
			url2 = url + "?" + counter;
			player.sendMessage(url2 + " " + sha1);
		}
		player.setResourcePack(url2, sha1);
	}

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

	@SuppressWarnings({"deprecation", "UnstableApiUsage"})
	public void update_sha1(File file) {
		if (!localDev) return;
		try {
			var hash = Files.asByteSource(file).hash(Hashing.sha1());
			ResourcePackDistributor.this.sha1 = hash.toString();
		} catch (IOException ignored) {}
	}
}
