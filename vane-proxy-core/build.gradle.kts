plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.blossom)
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("\$VERSION", project.version.toString())
            }
        }
    }
}

dependencies {
    implementation(libs.nightConfig)
    implementation(libs.slf4j)
    implementation(rootProject.project(":vane-core"))
    compileOnly(libs.json)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks {
    shadowJar {
        dependencies {
            include(dependency("com.electronwill.night-config:toml"))
        }

        relocate("com.electronwill.night-config", "org.oddlama.vane.vane_velocity.external.night-config")
        relocate("org.json", "org.oddlama.vane.external.json")
    }
}
