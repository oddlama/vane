plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.json)
}

tasks {
    shadowJar {
        configurations = listOf()
        relocate("org.json", "org.oddlama.vane.external.json")
    }
}
