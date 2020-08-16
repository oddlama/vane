package org.oddlama.imex.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Repeatable;

@Repeatable(LangMessages.class)
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface LangMessage {
	String name();
}
