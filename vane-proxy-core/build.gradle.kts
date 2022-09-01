plugins {
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

dependencies {
    implementation(group = "com.electronwill.night-config", name = "toml", version = "3.6.4")
    implementation(group = "org.slf4j", name = "slf4j-api", version = "1.7.30")
    implementation(rootProject.project(":vane-core"))
}

tasks {
    shadowJar {
        dependencies {
            include(dependency("com.electronwill.night-config:toml"))
        }

        relocate("com.electronwill.night-config", "org.oddlama.vane.vane_velocity.external.night-config")
    }
}
