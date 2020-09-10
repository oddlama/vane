package org.oddlama.vane.admin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.lang.TranslatedString;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class ResourcePackDistributor extends Listener<Admin> {
	@ConfigString(def = "https://your-server.tld/path/to/pack.zip", desc = "URL to an resource pack. Will request players to use the specified resource pack.")
	public String config_url;
	@ConfigString(def = "", desc = "Resource pack SHA-1 sum. Required to verify resource pack integrity.")
	public String config_sha1;
	@ConfigBoolean(def = true, desc = "Kick players if they deny to use the specified resource pack (if set). Individual players can be exempt from this rule by giving them the permission 'vane.admin.resource_pack.bypass'.")
	public boolean config_force;

	@LangString
	public TranslatedString lang_declined;
	@LangString
	public TranslatedString lang_download_failed;

	// The permission to bypass the resource pack
	private Permission bypass_permission;

	public ResourcePackDistributor(Context<Admin> context) {
		super(context.group("resource_pack", "Enable resource pack distribution."));

		// Register bypass permission
		bypass_permission = new Permission("vane." + get_module().get_name() + ".resource_pack.bypass", "Allows bypassing an enforced resource pack", PermissionDefault.FALSE);
		get_module().register_permission(bypass_permission);
	}

	@Override
	public void on_config_change() {
		// Check sha1 sum validity
		if (config_sha1.length() != 40) {
			get_module().log.warning("Invalid resource pack SHA-1 sum '" + config_sha1 + "', should be 40 characters long but has " + config_sha1.length());
			get_module().log.warning("Disabling resource pack serving");

			// Disable resource pack
			config_url = "";
		}

		config_sha1 = config_sha1.toLowerCase();
		if (!config_url.isEmpty()) {
			get_module().log.info("Distributing resource pack from '" + config_url + "' with sha1 " + config_sha1);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_join(final PlayerJoinEvent event) {
		if (config_url.isEmpty()) {
			return;
		}

		event.getPlayer().setResourcePack(config_url, config_sha1);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_status(final PlayerResourcePackStatusEvent event) {
		if (!config_force || event.getPlayer().hasPermission(bypass_permission)) {
			return;
		}

		switch (event.getStatus()) {
			case DECLINED:
				event.getPlayer().kickPlayer(lang_declined);
				break;

			case FAILED_DOWNLOAD:
				event.getPlayer().kickPlayer(lang_download_failed);
				break;
		}
	}
}
