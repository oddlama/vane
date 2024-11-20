package org.oddlama.vane.annotation.item;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.bukkit.Material;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VaneItem {
    String name();

    Material base();

    int model_data();

    int version();

    int durability() default 0;

    boolean enabled() default true;
}
