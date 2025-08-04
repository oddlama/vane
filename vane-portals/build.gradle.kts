plugins {
	id("com.gradleup.shadow") version "9.0.0-rc3"
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
