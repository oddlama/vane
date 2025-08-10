plugins {
	id("io.github.goooler.shadow") version "8.1.8"
}

repositories {
	maven("https://repo.codemc.io/repository/maven-releases/")
	maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
	compileOnly("com.github.retrooper:packetevents-spigot:2.9.4")
	compileOnly(group = "org.json", name = "json", version = "20250107")
}

tasks {
	shadowJar {
		configurations = listOf()
		relocate("org.json", "org.oddlama.vane.external.json")
	}
}
