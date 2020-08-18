package org.oddlama.vane.annotation.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes({
    "org.oddlama.vane.annotation.ConfigBoolean",
    "org.oddlama.vane.annotation.ConfigDouble",
    "org.oddlama.vane.annotation.ConfigInt",
    "org.oddlama.vane.annotation.ConfigLong",
    "org.oddlama.vane.annotation.ConfigMaterialSet",
    "org.oddlama.vane.annotation.ConfigString",
    "org.oddlama.vane.annotation.ConfigVersion",
    "org.oddlama.vane.annotation.LangMessage",
    "org.oddlama.vane.annotation.LangString",
    "org.oddlama.vane.annotation.LangVersion",
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class ConfigAndLangProcessor extends AbstractProcessor {
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round_env) {
		for (var annotation : annotations) {
			round_env.getElementsAnnotatedWith(annotation)
			    .forEach(a -> verify_type(annotation, a));
		}

		return true;
	}

	private static final Map<String, String> field_type_mapping;
	static {
		Map<String, String> map = new HashMap<>();
		map.put("org.oddlama.vane.annotation.ConfigBoolean", "boolean");
		map.put("org.oddlama.vane.annotation.ConfigInt", "int");
		map.put("org.oddlama.vane.annotation.ConfigMaterialSet", "java.util.Set<org.bukkit.Material>");
		map.put("org.oddlama.vane.annotation.ConfigDouble", "double");
		map.put("org.oddlama.vane.annotation.ConfigLong", "long");
		map.put("org.oddlama.vane.annotation.ConfigString", "java.lang.String");
		map.put("org.oddlama.vane.annotation.ConfigLong", "long");
		map.put("org.oddlama.vane.annotation.ConfigString", "java.lang.String");
		map.put("org.oddlama.vane.annotation.ConfigVersion", "long");
		map.put("org.oddlama.vane.annotation.LangMessage", "java.text.MessageFormat");
		map.put("org.oddlama.vane.annotation.LangString", "java.lang.String");
		map.put("org.oddlama.vane.annotation.LangVersion", "long");
		field_type_mapping = Collections.unmodifiableMap(map);
	}

	private void verify_type(TypeElement annotation, Element element) {
		var type = ((VariableElement)element).asType().toString();
		var required_type = field_type_mapping.get(annotation.asType().toString());
		if (!required_type.equals(type)) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@" + annotation.getSimpleName() + " requires a field of type " + required_type + " but got " + type);
		}
	}
}
