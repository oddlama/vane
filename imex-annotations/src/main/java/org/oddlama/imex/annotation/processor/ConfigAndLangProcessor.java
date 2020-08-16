package org.oddlama.imex.annotation.processor;

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

@SupportedAnnotationTypes({
    "org.oddlama.imex.annotation.ConfigDouble",
    "org.oddlama.imex.annotation.ConfigDoubles",
    "org.oddlama.imex.annotation.ConfigLong",
    "org.oddlama.imex.annotation.ConfigLongs",
    "org.oddlama.imex.annotation.ConfigString",
    "org.oddlama.imex.annotation.ConfigStrings",
    "org.oddlama.imex.annotation.LangMessage",
    "org.oddlama.imex.annotation.LangMessages",
    "org.oddlama.imex.annotation.LangString",
    "org.oddlama.imex.annotation.LangStrings",
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class ConfigAndLangProcessor extends AbstractProcessor {
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round_env) {
		for (var annotation : annotations) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "@" + annotation.getSimpleName());
			round_env.getElementsAnnotatedWith(annotation)
			    .forEach(e -> { verify_is_imex_module(annotation, e); });
			round_env.getElementsAnnotatedWith(annotation)
			    .forEach(e -> { process_annotation(annotation, e); });
		}

		return true;
	}

	private void verify_is_imex_module(TypeElement annotation, Element element) {
		var t = (TypeElement)element;
		if (!t.getSuperclass().toString().equals("org.oddlama.imex.core.Module") && !t.getSuperclass().toString().equals("org.oddlama.imex.core.ModuleBase")) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@" + annotation.getSimpleName() + " must be applied to a class inheriting from org.oddlama.imex.core.Module");
		}
	}

	private void generate_annotation_code(TypeElement annotation, Element element) {
	}

	private void process_annotation(TypeElement annotation, Element element) {
		var t = (TypeElement)element;
		processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "  " + element.getSimpleName());

		//MethodSpec main = MethodSpec.methodBuilder("main")
		//                      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
		//                      .returns(void.class)
		//                      .addParameter(String[].class, "args")
		//                      .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
		//                      .build();

		//TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
		//                          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
		//                          .addMethod(main)
		//                          .build();

		//JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
		//                        .build();

		//javaFile.writeTo(System.out);

		//var package_element = procEnv.getElementUtils().getPackageOf(element);
		//var name = element.getSimpleName();
		//var builderName = name.toString() + "Builder";
		//var builderFile = processingEnv.getFiler().createSourceFile(packageElement.getQualifiedName() + "." + builderName);
		//var writer = builderFile.openWriter();
		//writer.append("package" + packageElement.getQualifiedName() + ";\n");
		//writer.append("publicclass" + builderName + "{\n");
	}
}
