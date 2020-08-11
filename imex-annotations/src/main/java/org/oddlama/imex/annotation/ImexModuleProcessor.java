package org.oddlama.imex.annotation;

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

@SupportedAnnotationTypes("org.oddlama.imex.annotation.ImexModule")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class ImexModuleProcessor extends AbstractProcessor {
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round_env) {
		round_env.getElementsAnnotatedWith(ImexModule.class)
			.forEach(this::verifyIsClass);
		round_env.getElementsAnnotatedWith(ImexModule.class)
			.forEach(this::verifyExtendsModule);
		return true;
	}

	private void verifyIsClass(Element element) {
		if (element.getKind() != ElementKind.CLASS) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@ImexModule must be applied to a class");
		}
	}

	private void verifyExtendsModule(Element element) {
		var t = (TypeElement)element;
		if (!t.getSuperclass().toString().equals("org.oddlama.imex.core.Module")) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@ImexModule must be applied to a class inheriting from org.oddlama.imex.core.Module");
		}
	}
}
