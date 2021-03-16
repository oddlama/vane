package org.oddlama.vane.admin.commands;

import org.bukkit.entity.Player;

import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

@Name("slimechunk")
public class SlimeChunk extends Command<Admin> {
	@LangMessage private TranslatedMessage lang_slime_chunk_yes;
	@LangMessage private TranslatedMessage lang_slime_chunk_no;

	public SlimeChunk(Context<Admin> context) {
		super(context);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		params().exec_player(this::is_slimechunk);
	}

	private void is_slimechunk(final Player player) {
		if (player.getLocation().getChunk().isSlimeChunk()) {
			lang_slime_chunk_yes.send(player);
		} else {
			lang_slime_chunk_no.send(player);
		}
	}
}
