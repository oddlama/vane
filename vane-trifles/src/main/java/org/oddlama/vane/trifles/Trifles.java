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
import org.oddlama.vane.core.Module;

@VaneModule("trifles")
public class Trifles extends Module implements Listener {
	// Configuration
	@ConfigVersion(1)
	public long config_version;

	@ConfigBoolean(def = true, desc = "Enable faster walking on certain materials.")
	boolean config_enable_fast_walking;

	@ConfigLong(def = 2000, min = 50, max = 5000, desc = "Speed effect duration in milliseconds.")
	long config_speed_duration;

	@ConfigMaterialSet(def = {Material.GRASS_PATH}, desc = "Materials on which players will walk faster.")
	Set<Material> config_fast_walking_materials;

	// Language
	@LangVersion(1)
	public long lang_version;

	// Variables
	private WalkSpeedListener walk_speed_listener = new WalkSpeedListener(this);
	public PotionEffect walk_speed_effect;

	@Override
	public void on_enable() {
		if (config_enable_fast_walking) {
			register_listener(walk_speed_listener);
		}
	}

	@Override
	protected void on_disable() {
		unregister_listener(walk_speed_listener);
	}

	@Override
	protected void on_config_change() {
		walk_speed_effect = new PotionEffect(PotionEffectType.SPEED, (int)ms_to_ticks(config_speed_duration), 1)
			.withAmbient(false)
			.withParticles(false)
			.withIcon(false);
	}
}
