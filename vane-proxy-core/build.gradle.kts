// The version constant used to be produced by blossom's `javaSources`, but this project has no
// Java sources at all, so nothing was ever templated and `VERSION` compiled to the literal
// string "$VERSION". Generating the file directly is both correct and one plugin lighter.
val generateVersionSource = tasks.register("generateVersionSource") {
    description = "Generates the build-time VERSION constant"
    val versionString = project.version.toString()
    val outputDir = layout.buildDirectory.dir("generated/sources/version/kotlin/main")
    inputs.property("version", versionString)
    outputs.dir(outputDir)
    doLast {
        val packageDir = outputDir.get().asFile.resolve("org/oddlama/vane/proxycore/util")
        packageDir.mkdirs()
        packageDir.resolve("Version.kt").writeText(
            """
            package org.oddlama.vane.proxycore.util

            /** Build-time injected plugin version string. */
            const val VERSION: String = "$versionString"
            """.trimIndent() + "\n"
        )
    }
}

kotlin.sourceSets.named("main") {
    kotlin.srcDir(generateVersionSource)
}

// Everything here is `compileOnly`: this module is never shipped on its own. Its classes are
// shaded into vane-velocity, and the Velocity proxy already provides night-config (it parses
// velocity.toml with it), slf4j and, via vane-velocity's own shading, org.json.
//
// This module is used to apply the shadow plugin and configure a shadowJar. That configuration was
// dead in two independent ways: nothing consumed the shadow jar (vane-velocity depends on the
// plain project artifact), and the relocation pattern `com.electronwill.night-config` never
// matched the real package `com.electronwill.nightconfig`. The shipped jar has always referenced
// night-config unrelocated, which is what lets it bind to the proxy's copy.
dependencies {
    compileOnly(libs.nightConfig)
    testImplementation(libs.nightConfig)
    compileOnly(libs.slf4j)
    compileOnly(libs.json)
    // Shaded into vane-velocity next to this module.
    compileOnly(project(":vane-busybar-common"))
    testImplementation(project(":vane-busybar-common"))
    testImplementation(libs.json)
}
