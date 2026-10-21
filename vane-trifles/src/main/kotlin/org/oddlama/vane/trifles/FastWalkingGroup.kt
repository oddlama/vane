package org.oddlama.vane.trifles

import org.bukkit.Material
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.annotation.config.ConfigMaterialSet
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleGroup
import org.oddlama.vane.util.msToTicks

/** Configuration group and shared runtime state for fast-walking effects. */
class FastWalkingGroup(context: Context<Trifles?>) :
    ModuleGroup<Trifles?>(context, "FastWalking", "Enable faster walking on certain materials.") {
    /** Potion amplifier level applied while entities stand on configured materials. */
    @ConfigInt(def = 2, min = 1, max = 10, desc = "Speed effect level.")
    var configSpeedLevel: Int = 0

    /** Duration in milliseconds for each applied speed effect refresh. */
    @ConfigLong(def = 2000, min = 50, max = 5000, desc = "Speed effect duration in milliseconds.")
    var configDuration: Long = 0

    /** Materials that trigger fast-walking behavior. */
    @JvmField
    @ConfigMaterialSet(
        def = [Material.DIRT_PATH],
        desc = "Materials on which players will walk faster.",
    )
    var configMaterials: MutableSet<Material> = mutableSetOf()

    /** Cached effect instance rebuilt from current config values. */
    @JvmField var walkSpeedEffect: PotionEffect? = null

    /** Recomputes the cached potion effect from current configuration values. */
    override fun onConfigChange() {
        super.onConfigChange()
        val ticks = msToTicks(configDuration)
        walkSpeedEffect =
            PotionEffect(PotionEffectType.SPEED, ticks.toInt(), configSpeedLevel - 1)
                .withAmbient(false)
                .withParticles(false)
                .withIcon(false)
    }
}
