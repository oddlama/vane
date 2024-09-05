plugins {
    id("io.github.goooler.shadow") version "8.1.7"
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "external", "include" to listOf("*.jar"))))
    implementation(group = "com.velocitypowered", name = "velocity-api", version = "3.3.0-SNAPSHOT")
    annotationProcessor(group = "com.velocitypowered", name = "velocity-api", version = "3.3.0-SNAPSHOT")
    implementation(group = "org.bstats", name = "bstats-velocity", version = "3.0.2")
    implementation(group = "org.bstats", name = "bstats-base", version = "3.0.2")
    implementation(group = "org.json", name = "json", version = "20240303")
    implementation(rootProject.project(":vane-core"))
    implementation(rootProject.project(":vane-proxy-core"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
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
        }

        relocate("org.json", "org.oddlama.vane.vane_velocity.external.json")
        relocate("org.bstats", "org.oddlama.vane.vane_velocity.external.bstats")
    }

    build {
        dependsOn("copyJar")
    }
}
