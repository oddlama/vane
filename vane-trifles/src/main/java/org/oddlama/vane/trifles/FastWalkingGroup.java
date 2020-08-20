package org.oddlama.vane.trifles;

import static org.oddlama.vane.util.Util.ms_to_ticks;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigMaterialSet;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleGroup;
import org.oddlama.vane.core.module.Context;

public class FastWalkingGroup extends ModuleGroup<Trifles> {
	@ConfigLong(def = 2000, min = 50, max = 5000, desc = "Speed effect duration in milliseconds.")
	public long config_duration;

	@ConfigMaterialSet(def = {Material.GRASS_PATH}, desc = "Materials on which players will walk faster.")
	public Set<Material> config_materials;

	// Variables
	public PotionEffect walk_speed_effect;

	public FastWalkingGroup(Context<Trifles> context) {
		super(context, "fast_walking", "Enable faster walking on certain materials.");
	}

	@Override
	public void on_config_change() {
		walk_speed_effect = new PotionEffect(PotionEffectType.SPEED, (int)ms_to_ticks(config_duration), 1)
			.withAmbient(false)
			.withParticles(false)
			.withIcon(false);
	}
}
