plugins {
	id("com.gradleup.shadow") version "9.2.2"
}

repositories {
	maven("https://repo.codemc.io/repository/maven-releases/")
	maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
	compileOnly("com.github.retrooper:packetevents-spigot:2.9.5")
	compileOnly("org.json:json:20250517")
}

tasks {
    shadowJar {
        // Do not include dependencies (vane-core is provided as a separate plugin)
        configurations = listOf()
    }
}
