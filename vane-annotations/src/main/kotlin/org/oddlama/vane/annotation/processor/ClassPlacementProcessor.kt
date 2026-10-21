package org.oddlama.vane.annotation.processor

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

/**
 * Base for the processors whose whole job is to check *placement*: every element carrying one of
 * the annotations they support must be a class, and that class must inherit — at any depth — from a
 * particular framework base type.
 *
 * Subclasses only declare their `@SupportedAnnotationTypes` and name the base type; the round loop
 * and the diagnostics live here. See the note in the module documentation: these processors are
 * currently dormant because nothing in the build runs `javax.annotation.processing`.
 *
 * @property requiredSuperType Fully qualified name of the base type, as shown in diagnostics.
 */
abstract class ClassPlacementProcessor(private val requiredSuperType: String) :
    AbstractProcessor() {
    /**
     * Vane's base types are all generic, so the erased supertype name that [verifyExtendsType]
     * matches against is the raw name followed by `<`.
     */
    private val requiredSuperPrefix = "$requiredSuperType<"

    /**
     * Tracks whatever the running JDK supports instead of pinning a release, so the toolchain can
     * move forward without javac warning about an outdated `@SupportedSourceVersion`.
     */
    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    /**
     * Validates every element annotated with one of the supported annotations.
     *
     * @param annotations Annotation types to process.
     * @param roundEnv Information about the current processing round.
     * @return true to indicate that the annotations have been claimed.
     */
    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment,
    ): Boolean {
        annotations.forEach { annotation ->
            val name = annotation.simpleName.toString()
            roundEnv.getElementsAnnotatedWith(annotation).forEach { element ->
                verifyIsClass(processingEnv, element, name)
                verifyExtendsType(
                    processingEnv,
                    element,
                    requiredSuperPrefix,
                    name,
                    requiredSuperType,
                )
            }
        }

        return true
    }
}
