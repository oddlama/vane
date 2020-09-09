package org.oddlama.vane.annotation.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ResourcePackTranslation {
	String namespace() default "";  // May be overridden by *_translation_namespace()
	String key() default ""; // May be overridden by *_translation_key()
}
