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
        dependencies {
            include(dependency(":vane-portals"))
        }
    }
}
