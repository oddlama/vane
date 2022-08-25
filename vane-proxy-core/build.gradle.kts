plugins {
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

dependencies {
    implementation(group = "org.yaml", name = "snakeyaml", version = "1.30")
    implementation(rootProject.project(":vane-core"))
}

tasks {
    shadowJar {
        dependencies {
            include(dependency("org.yaml:snakeyaml"))
        }

        relocate("org.yaml", "org.oddlama.vane.vane_waterfall.external.yaml")
    }
}
