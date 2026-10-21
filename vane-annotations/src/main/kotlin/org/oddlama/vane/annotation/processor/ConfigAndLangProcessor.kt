package org.oddlama.vane.annotation.processor

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

/** Placeholder used for annotations whose annotated field may have any type. */
private const val ANY_TYPE = "<any>"

/**
 * Maps each config/lang annotation to the exact Java type string the matching `ConfigField` or
 * `LangField` reader in vane-core expects on the annotated field. This map is also the single
 * source of truth for the annotations this processor claims — see
 * [ConfigAndLangProcessor.getSupportedAnnotationTypes].
 */
private val fieldTypeMapping: Map<String, String> =
    mapOf(
        "org.oddlama.vane.annotation.config.ConfigBoolean" to "boolean",
        "org.oddlama.vane.annotation.config.ConfigDict" to ANY_TYPE,
        "org.oddlama.vane.annotation.config.ConfigDouble" to "double",
        "org.oddlama.vane.annotation.config.ConfigDoubleList" to "java.util.List<java.lang.Double>",
        "org.oddlama.vane.annotation.config.ConfigExtendedMaterial" to
            "org.oddlama.vane.core.material.ExtendedMaterial",
        "org.oddlama.vane.annotation.config.ConfigInt" to "int",
        "org.oddlama.vane.annotation.config.ConfigIntList" to "java.util.List<java.lang.Integer>",
        "org.oddlama.vane.annotation.config.ConfigItemStack" to "org.bukkit.inventory.ItemStack",
        "org.oddlama.vane.annotation.config.ConfigLong" to "long",
        "org.oddlama.vane.annotation.config.ConfigMaterial" to "org.bukkit.Material",
        "org.oddlama.vane.annotation.config.ConfigMaterialMapMapMap" to
            "java.util.Map<java.lang.String,java.util.Map<java.lang.String,java.util.Map<java.lang.String,org.bukkit.Material>>>",
        "org.oddlama.vane.annotation.config.ConfigMaterialSet" to
            "java.util.Set<org.bukkit.Material>",
        "org.oddlama.vane.annotation.config.ConfigString" to "java.lang.String",
        "org.oddlama.vane.annotation.config.ConfigStringList" to "java.util.List<java.lang.String>",
        "org.oddlama.vane.annotation.config.ConfigStringListMap" to
            "java.util.Map<java.lang.String,java.util.List<java.lang.String>>",
        "org.oddlama.vane.annotation.config.ConfigVersion" to "long",
        "org.oddlama.vane.annotation.lang.LangMessage" to
            "org.oddlama.vane.core.lang.TranslatedMessage",
        "org.oddlama.vane.annotation.lang.LangMessageArray" to
            "org.oddlama.vane.core.lang.TranslatedMessageArray",
        "org.oddlama.vane.annotation.lang.LangVersion" to "long",
    )

/**
 * Annotation processor that validates configuration and language field annotations.
 *
 * It ensures that fields annotated with the config/lang annotations have the exact type the
 * corresponding vane-core field reader will cast them to, per [fieldTypeMapping].
 */
class ConfigAndLangProcessor : AbstractProcessor() {
    /** Claims exactly the annotations [fieldTypeMapping] knows a required type for. */
    override fun getSupportedAnnotationTypes(): Set<String> = fieldTypeMapping.keys

    /**
     * Tracks whatever the running JDK supports instead of pinning a release, so the toolchain can
     * move forward without javac warning about an outdated `@SupportedSourceVersion`.
     */
    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    /**
     * Processes the set of annotation types for this round and validates annotated elements against
     * the expected types.
     *
     * @param annotations The set of annotation types to process.
     * @param roundEnv The processing environment for this round.
     * @return true to indicate annotations are claimed by this processor.
     */
    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment,
    ): Boolean {
        annotations.forEach { annotation ->
            roundEnv.getElementsAnnotatedWith(annotation).forEach { verifyType(annotation, it) }
        }

        return true
    }

    /**
     * Verifies that the annotated element has the expected type for the given annotation. Emits an
     * error if the mapping is missing or types do not match.
     *
     * @param annotation The annotation type being validated.
     * @param element The element annotated with that annotation.
     */
    private fun verifyType(annotation: TypeElement, element: Element) {
        val type = element.asType().toString()
        val requiredType =
            fieldTypeMapping[annotation.asType().toString()]
                ?: return processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "${element.asType()}: @${annotation.simpleName} has no requiredType mapping! This is a bug.",
                )

        if (requiredType != ANY_TYPE && requiredType != type) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "${element.asType()}: @${annotation.simpleName} requires a field of type $requiredType but got $type",
            )
        }
    }
}
