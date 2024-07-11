package org.oddlama.vane.core.resourcepack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class PlayerMessageDelayer extends Listener<Core> {

	private Adapter adapter;
	private final Map<UUID, List<PacketContainer>> message_queues = new HashMap<>();
	private final Map<UUID, Long> message_queue_start_time = new HashMap<>();

	// Force-stop delaying after 30 seconds.
	private static final long message_delaying_timeout = 30000;

	public PlayerMessageDelayer(Context<Core> context) {
		super(
			context.group(
				"message_delaying",
				"Enable delaying messages to players until their resource pack is fully loaded. This prevents display of untranslated chat messages."
			)
		);
	}

	@Override
	protected void on_enable() {
		adapter = new Adapter();
		get_module().protocol_manager.addPacketListener(adapter);

		super.on_enable();

		// Check for message delaying timeouts every 5 seconds.
		schedule_task_timer(
			() -> check_message_delay_timeout(),
			5 * 20,
			5 * 20
		);
	}

	private void check_message_delay_timeout() {
		final var now = System.currentTimeMillis();
		for (final var uuid : new HashMap<>(message_queue_start_time).keySet()) {
			final var start_time = message_queue_start_time.get(uuid);
			if (now - start_time > message_delaying_timeout) {
				final var offline_player = get_module().getServer().getOfflinePlayer(uuid);
				if (!offline_player.isOnline()) {
					stop_queueing(uuid);
					continue;
				}

				final var player = offline_player.getPlayer();
				relay_messages_and_stop_queueing(player);
				get_module()
					.log.warning(
						"Force stopped delaying messages to player '" +
						player.getName() +
						"' after their client didn't report any status after " +
						message_delaying_timeout +
						" ms. If this message appears again after they reconnect, it might be a configuration issue."
					);
				player.sendMessage(
					"Your client failed to report that the resource pack has been applied after " +
					message_delaying_timeout +
					" ms. If you encounter text formatting issues, please reconnect. If this message appears again, it might be a plugin configuration issue."
				);
			}
		}
	}

	@Override
	protected void on_disable() {
		get_module().protocol_manager.removePacketListener(adapter);
		super.on_disable();
	}

	private void start_queueing(UUID uuid) {
		message_queues.put(uuid, new ArrayList<PacketContainer>());
		message_queue_start_time.put(uuid, System.currentTimeMillis());
	}

	private List<PacketContainer> stop_queueing(UUID uuid) {
		message_queue_start_time.remove(uuid);
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

	private void relay_messages_and_stop_queueing(final Player player) {
		// Send delayed messages, which would otherwise be displayed in untranslated form
		// as the resource pack is only now fully loaded
		final var queue = stop_queueing(player.getUniqueId());
		if (queue == null) {
			return;
		}
		for (final var packet : queue) {
			get_module().protocol_manager.sendServerPacket(player, packet);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_kick(final PlayerKickEvent event) {
		stop_queueing(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_quit(final PlayerQuitEvent event) {
		stop_queueing(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void on_player_status(final PlayerResourcePackStatusEvent event) {
		switch (event.getStatus()) {
			case ACCEPTED:
				// Wait until the next status.
				return;
			case DECLINED:
			case FAILED_DOWNLOAD:
			case SUCCESSFULLY_LOADED:
				// Disable queueing
				break;
		}

		final var player = event.getPlayer();
		relay_messages_and_stop_queueing(player);

		switch (event.getStatus()) {
			case DECLINED:
				get_module()
					.log.info(
						"The player " +
						player.getName() +
						" rejected the resource pack. This will cause client-side issues with formatted text for them."
					);
				player.sendMessage(
					"You have rejected the resource pack. This will cause major issues with formatted text and custom items."
				);
				break;
			case FAILED_DOWNLOAD:
				get_module()
					.log.info(
						"The resource pack download for player " +
						player.getName() +
						" failed. This will cause client-side issues with formatted text for them."
					);
				player.sendMessage(
					"Your resource pack download failed. Please reconnect to retry, otherwise this will cause major issues with formatted text and custom items."
				);
				break;
			default:
				break;
		}
	}

	public class Adapter extends PacketAdapter {

		public Adapter() {
			super(
				PlayerMessageDelayer.this.get_module(),
				ListenerPriority.HIGHEST,
				PacketType.Play.Server.SYSTEM_CHAT
			);
		}

		@Override
		public void onPacketSending(final PacketEvent event) {
			if (event.getPacketType() != PacketType.Play.Server.SYSTEM_CHAT) {
				return;
			}

			final var queue = message_queues.get(event.getPlayer().getUniqueId());
			if (queue == null) {
				return;
			}

			queue.add(event.getPacket());
			event.setCancelled(true);
		}
	}
}
