package org.oddlama.imex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ConfigDouble {
	String name();
	double def();
	double min() default Double.MIN_VALUE;
	double max() default Double.MAX_VALUE;
	String desc();
}
