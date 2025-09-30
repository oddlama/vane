plugins {
	id("com.gradleup.shadow") version "9.2.2"
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
		configurations = listOf()
		relocate("org.json", "org.oddlama.vane.external.json")
	}
}
