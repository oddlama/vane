package org.oddlama.vane.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class PlayerMessageDelayer extends Listener<Core> {
	//private Adapter adapter;
	private final Map<UUID, List<BaseComponent[]>> queued_messages = new HashMap<>();

	public PlayerMessageDelayer(Context<Core> context) {
		super(context.group("message_delaying", "Enable delaying messages to players until their resource pack is fully loaded. This prevents display of untranslated chat messages."));
	}

	@Override
	protected void on_enable() {
		//adapter = new Adapter();
		//get_module().protocol_manager.addPacketListener(adapter);
	}

	@Override
	protected void on_disable() {
		//get_module().protocol_manager.removePacketListener(adapter);
	}

	private void start_queueing(UUID uuid) {
		queued_messages.put(uuid, new ArrayList<BaseComponent[]>());
	}

	private void stop_queueing(UUID uuid) {
		queued_messages.remove(uuid);
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

		stop_queueing(event.getPlayer().getUniqueId());
		// Send delayed messages, which would otherwise be messages would show
		// TODO
	}

//	public class Adapter extends PacketAdapter {
//		public Adapter() {
//			super(TabCompletionRestricter.this.get_module(), ListenerPriority.HIGHEST, new PacketType[] {PacketType.Play.Server.TAB_COMPLETE});
//		}
//
//		@Override
//		public void onPacketSending(final PacketEvent event) {
//			if (event.getPacketType() != PacketType.Play.Server.TAB_COMPLETE) {
//				return;
//			}
//
//			final var packet = new WrapperPlayServerTabComplete(event.getPacket());
//			final var suggestions = packet.getSuggestions();
//			final var suggestion_list = suggestions.getList();
//			if (suggestion_list.isEmpty() || !suggestion_list.get(0).getText().startsWith("/")) {
//				return;
//			}
//
//			final var player = event.getPlayer();
//			final var allowed_completions = new ArrayList<String>();
//			for (var command : TabCompletionRestricter.this.get_module().getServer().getCommandMap().getKnownCommands().values()) {
//				if (command.testPermissionSilent(player)) {
//					allowed_completions.addAll(command.getAliases());
//					allowed_completions.add(command.getName());
//				}
//			}
//
//			/* filter existing completions based on allowed completions */
//			final var new_suggestion_list = new ArrayList<Suggestion>();
//			for (var suggestion : suggestion_list) {
//				var completion = suggestion.getText();
//				if (allowed_completions.contains(completion.substring(1, completion.length()))) {
//					new_suggestion_list.add(suggestion);
//				}
//			}
//
//			suggestion_list.clear();
//			suggestion_list.addAll(new_suggestion_list);
//			packet.setSuggestions(suggestions);
//		}
//	}
}
