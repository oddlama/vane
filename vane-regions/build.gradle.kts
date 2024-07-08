plugins {
	id("io.github.goooler.shadow") version "8.1.7"
}

dependencies {
	implementation(project(":vane-portals"))
	compileOnly(group = "com.github.MilkBowl", name = "VaultAPI", version = "1.7.1")
	compileOnly(group = "org.json", name = "json", version = "20240303")
}

tasks {
	shadowJar {
		configurations = listOf()
		relocate("org.json", "org.oddlama.vane.external.json")
	}
}
