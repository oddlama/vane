package org.oddlama.vane.annotation.processor;

import java.lang.annotation.Annotation;
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
import org.oddlama.vane.annotation.command.Name;

@SupportedAnnotationTypes(
	{
		"org.oddlama.vane.annotation.command.Aliases",
		"org.oddlama.vane.annotation.command.Name",
		"org.oddlama.vane.annotation.command.VaneCommand",
	}
)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class CommandAnnotationProcessor extends AbstractProcessor {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static final Class<? extends Annotation>[] mandatory_annotations = new Class[] { Name.class };

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round_env) {
		for (var annotation : annotations) {
			round_env.getElementsAnnotatedWith(annotation).forEach(e -> verify_is_class(annotation, e));
			round_env.getElementsAnnotatedWith(annotation).forEach(e -> verify_extends_command(annotation, e));

			// Verify that all mandatory annotations are present
			if (annotation.asType().toString().equals("org.oddlama.vane.annotation.command.VaneCommand")) {
				round_env.getElementsAnnotatedWith(annotation).forEach(this::verify_has_annotations);
			}
		}

		return true;
	}

	private void verify_has_annotations(Element element) {
		// Only check subclasses
		if (element.asType().toString().startsWith("org.oddlama.vane.core.command.Command<")) {
			return;
		}

		for (var a_cls : mandatory_annotations) {
			if (element.getAnnotation(a_cls) == null) {
				processingEnv
					.getMessager()
					.printMessage(
						Diagnostic.Kind.ERROR,
						element.asType().toString() + ": missing @" + a_cls.getSimpleName() + " annotation"
					);
			}
		}
	}

	private void verify_is_class(TypeElement annotation, Element element) {
		if (element.getKind() != ElementKind.CLASS) {
			processingEnv
				.getMessager()
				.printMessage(
					Diagnostic.Kind.ERROR,
					element.asType().toString() + ": @" + annotation.getSimpleName() + " must be applied to a class"
				);
		}
	}

	private void verify_extends_command(TypeElement annotation, Element element) {
		var t = (TypeElement) element;
		if (
			!t.toString().equals("org.oddlama.vane.core.command.Command") &&
			!t.getSuperclass().toString().startsWith("org.oddlama.vane.core.command.Command<")
		) {
			processingEnv
				.getMessager()
				.printMessage(
					Diagnostic.Kind.ERROR,
					element.asType().toString() +
					": @" +
					annotation.getSimpleName() +
					" must be applied to a class inheriting from org.oddlama.vane.core.command.Command, but it inherits from " +
					t.getSuperclass().toString()
				);
		}
	}
}
