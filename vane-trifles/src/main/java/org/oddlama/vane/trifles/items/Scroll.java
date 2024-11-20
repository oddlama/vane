package org.oddlama.vane.trifles.items;

import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

public abstract class Scroll extends CustomItem<Trifles> {

    @ConfigInt(def = 0, min = 0, desc = "Cooldown in milliseconds until another scroll can be used.")
    public int config_cooldown;

    @ConfigBoolean(def = false, desc = "Allow this scroll to be repaired via the mending enchantment.")
    private boolean config_allow_mending;

    private int default_cooldown;

    public Scroll(Context<Trifles> context, int default_cooldown) {
        super(context);
        this.default_cooldown = default_cooldown;
    }

    public int config_cooldown_def() {
        return default_cooldown;
    }

    /**
     * Get the teleport location for the given player. Return null to prevent teleporting. Cooldown
     * is already handled by the base class, you only need to assert that a valid location is
     * available. For example, home scrolls may prevent teleport because of a missing bed or respawn
     * point here and notify the player about that. If imminent_teleport is true, the player will be
     * teleported if this function returns a valid location. The player should only be notified of
     * errors if this is set.
     */
    public abstract Location teleport_location(final ItemStack scroll, final Player player, boolean imminent_teleport);

    @Override
    public EnumSet<InhibitBehavior> inhibitedBehaviors() {
        final var set = EnumSet.of(
            InhibitBehavior.USE_IN_VANILLA_RECIPE,
            InhibitBehavior.TEMPT,
            InhibitBehavior.USE_OFFHAND
        );
        if (!config_allow_mending) {
            set.add(InhibitBehavior.MEND);
        }
        return set;
    }
}
