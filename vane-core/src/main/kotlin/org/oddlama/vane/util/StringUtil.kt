package org.oddlama.vane.util

/** Utility functions for string transformations. */

/** Converts a `snake_case` identifier to `PascalCase`. */
fun snakeCaseToPascalCase(snakeCase: String): String =
    snakeCase.split('_').joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
