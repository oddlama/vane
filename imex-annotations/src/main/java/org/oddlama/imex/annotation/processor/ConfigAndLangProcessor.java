package org.oddlama.imex.annotation.processor;

import java.util.Set;
import java.io.IOException;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes({
    "org.oddlama.imex.annotation.ConfigDouble",
    "org.oddlama.imex.annotation.ConfigLong",
    "org.oddlama.imex.annotation.ConfigVersion",
    "org.oddlama.imex.annotation.ConfigString",
    "org.oddlama.imex.annotation.LangMessage",
    "org.oddlama.imex.annotation.LangString",
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class ConfigAndLangProcessor extends AbstractProcessor {
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round_env) {
		for (var annotation : annotations) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "@" + annotation.getSimpleName());
			round_env.getElementsAnnotatedWith(annotation)
			    .forEach(e -> { process_annotation(annotation, e); });
		}

		return true;
	}

	private void generate_annotation_code(TypeElement annotation, Element element) {
	}

	private void process_annotation(TypeElement annotation, Element element) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "  " + element.getSimpleName());

		//var package_element = procEnv.getElementUtils().getPackageOf(element);
		//TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
		//                          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
		//                          .addMethod(main)
		//                          .build();

		//JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
		//                        .build();

		//javaFile.writeTo(System.out);

		//var name = element.getSimpleName();
		//var builderName = name.toString() + "Builder";
		//var builderFile = processingEnv.getFiler().createSourceFile(packageElement.getQualifiedName() + "." + builderName);
		//var writer = builderFile.openWriter();
		//writer.append("package" + packageElement.getQualifiedName() + ";\n");
		//writer.append("publicclass" + builderName + "{\n");
	}
}
