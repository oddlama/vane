import java.security.MessageDigest

plugins {
    alias(libs.plugins.shadow)
}

// The version constant used to be produced by blossom's `javaSources`, but this project has no
// Java sources at all, so nothing was ever templated and `VERSION` compiled to the literal
// string "$VERSION". Generating the file directly is both correct and one plugin lighter.
val generateVersionSource =
    tasks.register("generateVersionSource") {
        description = "Generates the build-time VERSION constant"
        val versionString = project.version.toString()
        val outputDir = layout.buildDirectory.dir("generated/sources/version/kotlin/main")
        inputs.property("version", versionString)
        outputs.dir(outputDir)
        doLast {
            val packageDir = outputDir.get().asFile.resolve("org/oddlama/vane/util")
            packageDir.mkdirs()
            packageDir
                .resolve("Version.kt")
                .writeText(
                    """
            package org.oddlama.vane.util

            /** Build-time injected plugin version string. */
            const val VERSION: String = "$versionString"
            """
                        .trimIndent() + "\n"
                )
        }
    }

kotlin.sourceSets.named("main") {
    kotlin.srcDir(generateVersionSource)
}

dependencies {
    implementation(libs.bstatsBase)
    implementation(libs.bstatsBukkit)
    api(libs.json)
    implementation(project(":vane-annotations"))
}

val resourcePackSha1: String by lazy {
    val resourcePack =
        rootProject.layout.projectDirectory
            .file("docs/resourcepacks/v${project.version}.zip")
            .asFile
    if (!resourcePack.exists()) {
        throw GradleException("The resource pack file $resourcePack is missing.")
    }
    val md: MessageDigest = MessageDigest.getInstance("SHA-1")
    val resourcePackBytes: ByteArray = resourcePack.readBytes()
    md.update(resourcePackBytes, 0, resourcePackBytes.size)
    val sha1Bytes: ByteArray = md.digest()
    val sha1HashString: String = String.format("%040x", BigInteger(1, sha1Bytes))
    sha1HashString
}

tasks {
    shadowJar {
        dependencies {
            include(dependency("org.bstats:bstats-base"))
            include(dependency("org.bstats:bstats-bukkit"))
            include(dependency("org.json:json"))
            include(dependency(":vane-annotations"))
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        }
        relocate("org.bstats", "org.oddlama.vane.external.bstats")
        relocate("org.json", "org.oddlama.vane.external.json")
        relocate("kotlin", "org.oddlama.vane.external.kotlin")

        // The vane-annotations jar carries `javax.annotation.processing` validators alongside the
        // annotations themselves. Those are compile-time-only tooling, so neither the classes nor
        // their service registration belong in a runtime plugin jar.
        exclude("org/oddlama/vane/annotation/processor/**")
        exclude("META-INF/services/javax.annotation.processing.Processor")
    }

    val projectVersion = project.version.toString()
    val localResourcePackSha1 = resourcePackSha1
    processResources {
        inputs.property("version", projectVersion)
        inputs.property("resourcePackSha1", localResourcePackSha1)
        filesMatching("vane-core.properties") {
            expand(mapOf("version" to projectVersion, "resourcePackSha1" to localResourcePackSha1))
        }
    }
}
