package org.oddlama.vane.annotation.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigLong {
    long def();

    long min() default Long.MIN_VALUE;

    long max() default Long.MAX_VALUE;

    String desc();

    boolean metrics() default true;
}
