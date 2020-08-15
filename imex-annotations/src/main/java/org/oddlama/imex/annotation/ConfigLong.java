package org.oddlama.imex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ConfigLong {
	String name();
	long def();
	long min() default Long.MIN_VALUE;
	long max() default Long.MAX_VALUE;
	String desc();
}
