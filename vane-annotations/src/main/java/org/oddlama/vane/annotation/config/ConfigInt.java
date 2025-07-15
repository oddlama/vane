package org.oddlama.vane.annotation.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigInt {
    int def();

    int min() default Integer.MIN_VALUE;

    int max() default Integer.MAX_VALUE;

    String desc();

    boolean metrics() default true;
}
