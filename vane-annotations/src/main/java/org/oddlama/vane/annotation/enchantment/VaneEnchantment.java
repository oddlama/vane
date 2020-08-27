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
	Rarity rarity() default Rarity.COMMON;
	int max_level() default 1;
	boolean treasure() default false;
	EnchantmentTarget target() default EnchantmentTarget.BREAKABLE;
}
