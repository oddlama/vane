import java.io.ByteArrayOutputStream

plugins {
	id("com.github.johnrengelman.shadow") version "7.1.0"
}

dependencies {
	implementation(group = "org.bstats", name = "bstats-bukkit", version = "1.8")
	implementation(group = "org.reflections", name = "reflections", version = "0.10.2")
	implementation(group = "org.json", name = "json", version = "20200518")
	implementation(project(":vane-annotations"))
}

val resource_pack_sha1: String = ByteArrayOutputStream().use { outputStream ->
	project.exec {
		commandLine("sha1sum", "../docs/resourcepacks/v" + project.version + ".zip")
		standardOutput = outputStream
	}
	outputStream.toString().split(" ")[0]
}

tasks {
	shadowJar {
		dependencies {
			include(dependency("org.bstats:bstats-bukkit"))
			include(dependency("org.reflections:reflections"))
			include(dependency("org.json:json"))
			include(dependency(":vane-annotations"))
		}
		relocate("org.bstats", "org.oddlama.vane.external.bstats")
		relocate("org.reflections", "org.oddlama.vane.external.reflections")
		relocate("org.json", "org.oddlama.vane.external.json")
	}

	processResources {
		filesMatching("vane-core.properties") {
			expand(project.properties + mapOf("resource_pack_sha1" to resource_pack_sha1))
		}
	}
}
