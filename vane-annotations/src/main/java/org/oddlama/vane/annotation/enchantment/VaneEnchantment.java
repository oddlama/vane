package org.oddlama.vane.annotation.enchantment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VaneEnchantment {
	String name();
	int start_level() default 1;
	int max_level() default 1;
}
