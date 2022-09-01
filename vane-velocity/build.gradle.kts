plugins {
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "external", "include" to listOf("*.jar"))))
    implementation(group = "com.velocitypowered", name = "velocity-api", version = "3.0.1")
    annotationProcessor(group = "com.velocitypowered", name = "velocity-api", version = "3.0.1")
    implementation(group = "org.bstats", name = "bstats-velocity", version = "3.0.0")
    implementation(group = "org.bstats", name = "bstats-base", version = "3.0.0")
    implementation(group = "org.json", name = "json", version = "20200518")
    implementation(rootProject.project(":vane-core"))
    implementation(rootProject.project(":vane-proxy-core"))
}

tasks.create<Copy>("copyJar") {
    from(tasks.shadowJar)
    into("${project.rootProject.projectDir}/target")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    rename("(.*)-all.jar", "$1.jar")
}

tasks {
    shadowJar {
        dependencies {
            include(dependency("org.bstats:bstats-velocity"))
            include(dependency("org.bstats:bstats-base"))
            include(dependency("org.json:json"))
            include(dependency(rootProject.project(":vane-proxy-core")))

            // Utilities to include from vane-core.util
            val includedUtils = listOf(
                "Resolve",
                "TimeUtil",
                "IOUtil"
            )

            from(rootProject.project(":vane-core").sourceSets.main.get().output) {
                for (i in includedUtils) {
                    include("org/oddlama/vane/util/$i*.class")
                }
            }
        }

        relocate("org.json", "org.oddlama.vane.vane_velocity.external.json")
        relocate("org.bstats", "org.oddlama.vane.vane_velocity.external.bstats")
    }

    build {
        dependsOn("copyJar")
    }
}
