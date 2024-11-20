package org.oddlama.vane.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VaneModule {
    String name();

    int bstats() default -1;

    long config_version();

    long lang_version();

    long storage_version();
}
