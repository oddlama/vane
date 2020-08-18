package org.oddlama.vane.annotation.processor;

import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("org.oddlama.vane.annotation.VaneModule")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class VaneModuleProcessor extends AbstractProcessor {
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round_env) {
		for (var annotation : annotations) {
			round_env.getElementsAnnotatedWith(annotation)
				.forEach(this::verify_is_class);
			round_env.getElementsAnnotatedWith(annotation)
				.forEach(this::verify_extends_module);
		}

		return true;
	}

	private void verify_is_class(Element element) {
		if (element.getKind() != ElementKind.CLASS) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@VaneModule must be applied to a class");
		}
	}

	private void verify_extends_module(Element element) {
		var t = (TypeElement)element;
		if (!t.getSuperclass().toString().equals("org.oddlama.vane.core.Module")) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@VaneModule must be applied to a class inheriting from org.oddlama.vane.core.Module");
		}
	}
}
