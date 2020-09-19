package org.oddlama.vane.core;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.packet.WrapperPlayServerChat;

public class PlayerMessageDelayer extends Listener<Core> {
	private Adapter adapter;
	private final Map<UUID, List<PacketContainer>> message_queues = new HashMap<>();

	public PlayerMessageDelayer(Context<Core> context) {
		super(context.group("message_delaying", "Enable delaying messages to players until their resource pack is fully loaded. This prevents display of untranslated chat messages."));
	}

	@Override
	protected void on_enable() {
		adapter = new Adapter();
		get_module().protocol_manager.addPacketListener(adapter);
		super.on_enable();
	}

	@Override
	protected void on_disable() {
		get_module().protocol_manager.removePacketListener(adapter);
		super.on_disable();
	}

	private void start_queueing(UUID uuid) {
		message_queues.put(uuid, new ArrayList<PacketContainer>());
	}

	private List<PacketContainer> stop_queueing(UUID uuid) {
		return message_queues.remove(uuid);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_pre_login(final AsyncPlayerPreLoginEvent event) {
		if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
			return;
		}

		// Begin queueing messages for the player
		start_queueing(event.getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_kick(final PlayerKickEvent event) {
		stop_queueing(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_quit(final PlayerQuitEvent event) {
		stop_queueing(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_status(final PlayerResourcePackStatusEvent event) {
		// Only if messages were queued
		if (event.getStatus() != PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
			return;
		}

		// Send delayed messages, which would otherwise be displayed in untranslated form
		// as the resource pack is only now fully loaded
		final var player = event.getPlayer();
		final var queue = stop_queueing(player.getUniqueId());
		if (queue == null) {
			return;
		}
		for (final var packet : queue) {
			try {
				get_module().protocol_manager.sendServerPacket(player, packet);
			} catch (InvocationTargetException e) {
				get_module().log.log(Level.WARNING, "Could not send queued message packet " + packet, e);
			}
		}
	}

	public class Adapter extends PacketAdapter {
		public Adapter() {
			super(PlayerMessageDelayer.this.get_module(), ListenerPriority.HIGHEST, new PacketType[] {PacketType.Play.Server.CHAT});
		}

		@Override
		public void onPacketSending(final PacketEvent event) {
			if (event.getPacketType() != PacketType.Play.Server.CHAT) {
				return;
			}

			final var queue = message_queues.get(event.getPlayer().getUniqueId());
			if (queue == null) {
				return;
			}

			final var packet = new WrapperPlayServerChat(event.getPacket());
			queue.add(packet.getHandle().deepClone());
			event.setCancelled(true);
		}
	}
}
