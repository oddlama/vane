package org.oddlama.vane.annotation.processor

import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import org.oddlama.vane.annotation.command.Name

/** Fully qualified name of the marker annotation that pulls in the mandatory-annotation check. */
private const val VANE_COMMAND = "org.oddlama.vane.annotation.command.VaneCommand"

/** Annotations that must accompany `@VaneCommand` on a command class. */
private val mandatoryAnnotations = listOf(Name::class.java)

/**
 * Validates the command annotations: `@VaneCommand`, `@Name` and `@Aliases` must all sit on a class
 * extending vane-core's `Command`, and a `@VaneCommand` class must additionally carry every
 * annotation in [mandatoryAnnotations].
 */
@SupportedAnnotationTypes(
    "org.oddlama.vane.annotation.command.Aliases",
    "org.oddlama.vane.annotation.command.Name",
    VANE_COMMAND,
)
class CommandAnnotationProcessor :
    ClassPlacementProcessor("org.oddlama.vane.core.command.Command") {
    /**
     * Runs the inherited placement checks, then the `@VaneCommand`-only completeness check.
     *
     * @param annotations Annotation types to process.
     * @param roundEnv Information about the current processing round.
     * @return true to indicate that the annotations have been claimed.
     */
    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment,
    ): Boolean {
        super.process(annotations, roundEnv)

        annotations
            .filter { it.asType().toString() == VANE_COMMAND }
            .forEach { roundEnv.getElementsAnnotatedWith(it).forEach(::verifyHasAnnotations) }

        return true
    }

    /**
     * Ensures the required command annotations are present on the class. The generic base `Command`
     * itself is exempt — only its subclasses name a command.
     *
     * @param element The element (class) to validate for required annotations.
     */
    private fun verifyHasAnnotations(element: Element) {
        if (element.asType().toString().startsWith("org.oddlama.vane.core.command.Command<")) return

        mandatoryAnnotations
            .filter { element.getAnnotation(it) == null }
            .forEach {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "${element.asType()}: missing @${it.simpleName} annotation",
                )
            }
    }
}
