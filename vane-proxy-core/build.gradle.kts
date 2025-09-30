plugins {
    id("com.gradleup.shadow") version "9.2.2"
    id("net.kyori.blossom") version "2.1.0"
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
    implementation("com.electronwill.night-config:toml:3.8.3")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation(rootProject.project(":vane-core"))
    compileOnly("org.json:json:20250517")
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
