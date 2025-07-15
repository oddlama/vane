package org.oddlama.vane.annotation.enchantment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.bukkit.enchantments.EnchantmentTarget;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VaneEnchantment {
    String name();

    int max_level() default 1;

    Rarity rarity() default Rarity.COMMON;

    boolean curse() default false;

    boolean tradeable() default false;

    boolean treasure() default false;

    boolean generate_in_treasure() default false;

    EnchantmentTarget target() default EnchantmentTarget.BREAKABLE;

    boolean allow_custom() default false;
}
