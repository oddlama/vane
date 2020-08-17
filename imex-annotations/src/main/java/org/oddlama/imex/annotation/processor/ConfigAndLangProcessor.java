package org.oddlama.imex.annotation.processor;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.io.IOException;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes({
    "org.oddlama.imex.annotation.ConfigDouble",
    "org.oddlama.imex.annotation.ConfigLong",
    "org.oddlama.imex.annotation.ConfigString",
    "org.oddlama.imex.annotation.ConfigVersion",
    "org.oddlama.imex.annotation.LangMessage",
    "org.oddlama.imex.annotation.LangString",
    "org.oddlama.imex.annotation.LangVersion",
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class ConfigAndLangProcessor extends AbstractProcessor {
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round_env) {
		for (var annotation : annotations) {
			round_env.getElementsAnnotatedWith(annotation)
				.forEach(a -> { verify_type(annotation, a); });
		}

		return true;
	}

	private static final Map<String, String> field_type_mapping;
	static {
		Map<String, String> map = new HashMap<>();
		map.put("org.oddlama.imex.annotation.ConfigDouble", "double");
		map.put("org.oddlama.imex.annotation.ConfigLong", "long");
		map.put("org.oddlama.imex.annotation.ConfigString", "java.lang.String");
    	map.put("org.oddlama.imex.annotation.ConfigLong", "long");
    	map.put("org.oddlama.imex.annotation.ConfigString", "java.lang.String");
    	map.put("org.oddlama.imex.annotation.ConfigVersion", "long");
    	map.put("org.oddlama.imex.annotation.LangMessage", "java.text.MessageFormat");
    	map.put("org.oddlama.imex.annotation.LangString", "java.lang.String");
    	map.put("org.oddlama.imex.annotation.LangVersion", "long");
		field_type_mapping = Collections.unmodifiableMap(map);
	}

	private void verify_type(TypeElement annotation, Element element) {
		var type = ((VariableElement)element).asType().toString();
		var required_type = field_type_mapping.get(annotation.asType().toString());
		if (!required_type.equals(type)) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@" + annotation.getSimpleName() + " requires a field of type " + required_type);
		}
	}
}
