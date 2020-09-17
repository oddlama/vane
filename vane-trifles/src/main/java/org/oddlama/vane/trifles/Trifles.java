package org.oddlama.vane.trifles;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.SoundCategory;
import org.oddlama.vane.annotation.VaneModule;
import org.bukkit.Particle;
import org.oddlama.vane.trifles.event.PlayerTeleportScrollEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.item.ModelDataEnum;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.persistent.PersistentSerializer;

@VaneModule(name = "trifles", bstats = 8644, config_version = 1, lang_version = 1, storage_version = 1)
public class Trifles extends Module<Trifles> {
	public final HashMap<UUID, Long> last_xp_bottle_consume_time = new HashMap<>();

	public Trifles() {
		var fast_walking_group = new FastWalkingGroup(this);
		new FastWalkingListener(fast_walking_group);
		new DoubleDoorListener(this);
		new HarvestListener(this);
		new RepairCostLimiter(this);
		new RecipeUnlock(this);
		new ChestSorter(this);

		new org.oddlama.vane.trifles.commands.Heads(this);
		new org.oddlama.vane.trifles.items.Sickle(this);
		new org.oddlama.vane.trifles.items.File(this);
		new org.oddlama.vane.trifles.items.EmptyXpBottle(this);
		new org.oddlama.vane.trifles.items.XpBottle(this);
		new org.oddlama.vane.trifles.items.HomeScroll(this);
		new org.oddlama.vane.trifles.items.UnstableScroll(this);
	}

	public boolean teleport_from_scroll(final Player player, final Location from, final Location to) {
		// Send scroll teleport event
		final var teleport_scroll_event = new PlayerTeleportScrollEvent(player, from, to);
		get_module().getServer().getPluginManager().callEvent(teleport_scroll_event);
		if (teleport_scroll_event.isCancelled()) {
			return false;
		}

		// Teleport
		player.teleport(to, PlayerTeleportEvent.TeleportCause.PLUGIN);

		// Play sounds
		from.getWorld().playSound(from, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.1f);
		to.getWorld().playSound(to, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.1f);
		from.getWorld().playSound(from, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f);
		to.getWorld().playSound(to, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f);

		// Create particles
		from.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0.5, 0.5, 0.5), 50, 0.0, 0.0, 0.0, 1.0);
		from.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0.5, 1.5, 0.5), 50, 0.0, 0.0, 0.0, 1.0);
		to.getWorld().spawnParticle(Particle.PORTAL, to.clone().add(0.5, 0.5, 0.5), 50, 0.0, 0.0, 0.0, 1.0);
		to.getWorld().spawnParticle(Particle.PORTAL, to.clone().add(0.5, 1.5, 0.5), 50, 0.0, 0.0, 0.0, 1.0);
		return true;
	}

	@Override
	public Class<? extends ModelDataEnum> model_data_enum() {
		return org.oddlama.vane.trifles.items.ModelData.class;
	}

	@Override
	public int model_data(int item_id, int variant_id) {
		return Core.model_data(0, item_id, variant_id);
	}
}
