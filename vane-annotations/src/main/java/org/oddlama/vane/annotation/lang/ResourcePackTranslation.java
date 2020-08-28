package org.oddlama.vane.annotation.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ResourcePackTranslation {
	String namespace();
	String key() default ""; // A empty ("") key will cause *_translation_key() to be called insted.
}
