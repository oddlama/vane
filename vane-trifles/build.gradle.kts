plugins {
	alias(libs.plugins.shadow)
}

repositories {
	maven("https://repo.codemc.io/repository/maven-releases/")
	maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
	compileOnly(libs.packetEvents)
	compileOnly(libs.json)
}

tasks {
    shadowJar {
        // Do not include dependencies (vane-core is provided as a separate plugin)
        configurations = listOf()
    }
}
