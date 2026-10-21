package org.oddlama.vane.annotation.processor

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.tools.Diagnostic

// Shared validation helpers used by the annotation processors in this package.

/**
 * Verifies that the provided element is a class. Prints a compiler error message via the processing
 * environment if it is not.
 *
 * @param processingEnv The annotation processing environment used to report errors.
 * @param element The element annotated with the annotation being validated.
 * @param annotationName The simple name of the annotation being validated (used in messages).
 */
fun verifyIsClass(processingEnv: ProcessingEnvironment, element: Element, annotationName: String) {
    if (element.kind != ElementKind.CLASS) {
        processingEnv.messager.printMessage(
            Diagnostic.Kind.ERROR,
            "${element.asType()}: @$annotationName must be applied to a class",
        )
    }
}

/**
 * Verifies that the provided element (class) extends or inherits from a type that starts with the
 * given prefix. If it does not, an error is emitted.
 *
 * @param processingEnv Processing environment used for error reporting.
 * @param element The element to check (expected to be a class).
 * @param requiredSuperPrefix Prefix of the required superclass or interface name.
 * @param annotationName Name of the annotation being validated (for diagnostics).
 * @param requiredFullName Human-readable full name used in diagnostic messages.
 */
fun verifyExtendsType(
    processingEnv: ProcessingEnvironment,
    element: Element,
    requiredSuperPrefix: String,
    annotationName: String,
    requiredFullName: String,
) {
    val start =
        element as? TypeElement
            ?: return processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "${element.asType()}: @$annotationName must be applied to a class",
            )

    val found =
        generateSequence(start) { current ->
                val sc = current.superclass
                if (sc is DeclaredType) sc.asElement() as? TypeElement else null
            }
            .any { it.asType().toString().startsWith(requiredSuperPrefix) }

    if (!found) {
        processingEnv.messager.printMessage(
            Diagnostic.Kind.ERROR,
            "${start.asType()}: @$annotationName must be applied to a class inheriting from $requiredFullName",
        )
    }
}
