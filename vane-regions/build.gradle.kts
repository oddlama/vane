plugins {
	alias(libs.plugins.shadow)
}

dependencies {
	implementation(project(":vane-portals"))
	compileOnly(libs.vault)
	compileOnly(libs.json)
}

tasks {
    shadowJar {
        configurations = listOf()
        relocate("org.json", "org.oddlama.vane.external.json")
    }
}
