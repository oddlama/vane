plugins {
	id("io.github.goooler.shadow") version "8.1.8"
}

dependencies {
	compileOnly(group = "org.json", name = "json", version = "20250107")
}

tasks {
	shadowJar {
		configurations = listOf()
		relocate("org.json", "org.oddlama.vane.external.json")
	}
}
