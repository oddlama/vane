package org.oddlama.vane.core.misc;

import static org.oddlama.vane.util.Resolve.resolve_skin;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import com.destroystokyo.paper.profile.ProfileProperty;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.oddlama.vane.util.Resolve;

public class AuthMultiplexer extends Listener<Core> implements PluginMessageListener {
	// Channel for proxy messages to multiplex connections
	public static final String CHANNEL_AUTH_MULTIPLEX = "vane_proxy:auth_multiplex";

	// Persistent storage
	@Persistent
	public Map<UUID, UUID> storage_auth_multiplex = new HashMap<>();

	@Persistent
	public Map<UUID, Integer> storage_auth_multiplexer_id = new HashMap<>();

	public AuthMultiplexer(Context<Core> context) {
		super(context);
	}

	@Override
	protected void on_enable() {
		super.on_enable();
		get_module().getServer().getMessenger().registerIncomingPluginChannel(get_module(), CHANNEL_AUTH_MULTIPLEX, this);
	}

	@Override
	protected void on_disable() {
		super.on_disable();
		get_module().getServer().getMessenger().unregisterIncomingPluginChannel(get_module(), CHANNEL_AUTH_MULTIPLEX, this);
	}

	public synchronized String auth_multiplex_player_name(final UUID uuid) {
		final var original_player_id = storage_auth_multiplex.get(uuid);
		final var multiplexer_id = storage_auth_multiplexer_id.get(uuid);
		if (original_player_id == null || multiplexer_id == null) {
			return null;
		}

		final var original_player = get_module().getServer().getOfflinePlayer(original_player_id);
		return "§7[" + multiplexer_id + "]§r " + original_player.getName();
	}

	private void try_init_multiplexed_player_name(final Player player) {
		final var id = player.getUniqueId();
		final var display_name = auth_multiplex_player_name(id);
		if (display_name == null) {
			return;
		}

		get_module().log.info(
			"[multiplex] Init player '" +
			display_name +
			"' for registered auth multiplexed player {" +
			id +
			", " +
			player.getName() +
			"}"
		);
		final var display_name_component = LegacyComponentSerializer.legacySection().deserialize(display_name);
		player.displayName(display_name_component);
		player.playerListName(display_name_component);

		final var original_player_id = storage_auth_multiplex.get(id);
		Resolve.Skin skin;
		try {
			skin = resolve_skin(original_player_id);
		} catch (IOException e) {
			Bukkit.getLogger().log(Level.WARNING, "Failed to resolve skin for uuid '" + id + "'", e);
			return;
		}

		final var profile = player.getPlayerProfile();
		profile.setProperty(new ProfileProperty("textures", skin.texture, skin.signature));
		player.setPlayerProfile(profile);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void on_player_join(PlayerJoinEvent event) {
		try_init_multiplexed_player_name(event.getPlayer());
	}

	@Override
	public synchronized void onPluginMessageReceived(final String channel, final Player player, byte[] bytes) {
		if (!channel.equals(CHANNEL_AUTH_MULTIPLEX)) {
			return;
		}

		final var stream = new ByteArrayInputStream(bytes);
		final var in = new DataInputStream(stream);

		try {
			final var multiplexer_id = in.readInt();
			final var old_uuid = UUID.fromString(in.readUTF());
			final var old_name = in.readUTF();
			final var new_uuid = UUID.fromString(in.readUTF());
			final var new_name = in.readUTF();

			get_module().log.info(
				"[multiplex] Registered auth multiplexed player {" +
				new_uuid +
				", " +
				new_name +
				"} from player {" +
				old_uuid +
				", " +
				old_name +
				"} multiplexer_id " +
				multiplexer_id
			);
			storage_auth_multiplex.put(new_uuid, old_uuid);
			storage_auth_multiplexer_id.put(new_uuid, multiplexer_id);
			mark_persistent_storage_dirty();

			final var multiplexed_player = get_module().getServer().getOfflinePlayer(new_uuid);
			if (multiplexed_player.isOnline()) {
				try_init_multiplexed_player_name(multiplexed_player.getPlayer());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
