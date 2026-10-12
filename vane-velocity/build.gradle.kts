plugins {
    alias(libs.plugins.shadow)
    id("xyz.jpenilla.run-velocity")
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "external", "include" to listOf("*.jar"))))
    compileOnly(libs.spotbugsAnnotations)
    // Provided by the proxy at runtime; never shaded (it is absent from the include list below).
    compileOnly(libs.velocity)
    implementation(libs.bstatsVelocity)
    implementation(libs.bstatsBase)
    implementation(libs.json)
    implementation(project(":vane-proxy-core"))
    implementation(project(":vane-busybar-common"))
}

tasks.register<Copy>("copyJar") {
    description = "Copies the shaded vane-velocity jar to the root target directory"
    from(tasks.shadowJar)
    into("${project.rootProject.projectDir}/target")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    rename("(.*)-all.jar", "$1.jar")
}

tasks {
    val velocityPluginVersion = project.version.toString()

    runVelocity {
        description = "Runs a local Velocity proxy with the vane-velocity plugin"
        velocityVersion(rootProject.libs.versions.velocity.get())
        jvmArgs(
            "-XX:+UseG1GC",
            "-XX:G1HeapRegionSize=4M",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+ParallelRefProcEnabled",
            "-XX:+AlwaysPreTouch",
            "-XX:MaxInlineLevel=15"
        )
    }

    shadowJar {
        // The plugin's own code is ~240 KiB while the shaded Kotlin runtime is ~5 MB, and a
        // Velocity plugin has no other plugin to borrow it from. Pruning to what is actually
        // reachable takes the jar from 2190 KiB to 1608 KiB. Verified with jdeps (no dangling
        // references) and by starting a proxy.
        minimize()
        dependencies {
            include(dependency("org.bstats:bstats-velocity"))
            include(dependency("org.bstats:bstats-base"))
            include(dependency("org.json:json"))
            include(dependency("org.oddlama.vane:vane-proxy-core"))
            include(dependency("org.oddlama.vane:vane-busybar-common"))
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        }

        relocate("org.json", "org.oddlama.vane.vane_velocity.external.json")
        relocate("org.bstats", "org.oddlama.vane.vane_velocity.external.bstats")
        relocate("kotlin", "org.oddlama.vane.vane_velocity.external.kotlin")
    }

    build {
        dependsOn("copyJar")
    }

    processResources {
        // Keep this config-cache friendly by passing explicit values instead of project.properties.
        inputs.property("version", velocityPluginVersion)
        filesMatching("velocity-plugin.json") {
            expand(mapOf("version" to velocityPluginVersion))
        }
    }
}
