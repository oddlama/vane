package org.oddlama.vane.core;

import java.util.ArrayList;
import java.util.Collection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.oddlama.vane.packet.WrapperPlayServerTabComplete;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.mojang.brigadier.suggestion.Suggestion;

import org.oddlama.vane.core.Listener;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.command.params.AnyParam;
import org.oddlama.vane.core.module.Context;

public class TabCompletionRestricter extends ModuleComponent<Core> {
	public class Adapter extends PacketAdapter {
		public Adapter(ListenerPriority priority, PacketType[] packet_types) {
			super(TabCompletionRestricter.this.get_module(), priority, packet_types);
		}
		@Override
		public void onPacketSending(final PacketEvent event) {
			if (event.getPacketType() != PacketType.Play.Server.TAB_COMPLETE) {
				return;
			}

			final var packet = new WrapperPlayServerTabComplete(event.getPacket());
			final var suggestions = packet.getSuggestions();
			final var suggestion_list = suggestions.getList();
			if (suggestion_list.isEmpty() || !suggestion_list.get(0).getText().startsWith("/")) {
				return;
			}

			final var player = event.getPlayer();
			final var allowed_completions = new ArrayList<String>();
			for (var command : TabCompletionRestricter.this.get_module().getServer().getCommandMap().getKnownCommands().values()) {
				if (command.testPermissionSilent(player)) {
					allowed_completions.addAll(command.getAliases());
					allowed_completions.add(command.getName());
				}
			}

			/* filter existing completions based on allowed completions */
			final var new_suggestion_list = new ArrayList<Suggestion>();
			for (var suggestion : suggestion_list) {
				var completion = suggestion.getText();
				if (allowed_completions.contains(completion.substring(1, completion.length()))) {
					new_suggestion_list.add(suggestion);
				}
			}

			suggestion_list.clear();
			suggestion_list.addAll(new_suggestion_list);
			packet.setSuggestions(suggestions);
		}
	}

	public Adapter adapter;
	public TabCompletionRestricter(Context<Core> context) {
		super(context);
	}

	@Override
	protected void on_enable() {
		adapter = new Adapter(ListenerPriority.HIGHEST, new PacketType[] {PacketType.Play.Server.TAB_COMPLETE});
		get_module().protocol_manager.addPacketListener(adapter);
	}

	@Override
	protected void on_disable() {
		get_module().protocol_manager.removePacketListener(adapter);
	}
}
