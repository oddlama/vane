import java.security.MessageDigest;

plugins {
	id("com.github.johnrengelman.shadow") version "7.1.0"
}

dependencies {
	implementation(group = "org.bstats", name = "bstats-bukkit", version = "1.8")
	implementation(group = "org.reflections", name = "reflections", version = "0.10.2")
	implementation(group = "org.json", name = "json", version = "20200518")
	implementation(project(":vane-annotations"))
}

val resource_pack_sha1 by lazy {
	val resourcePack = File("${projectDir}/../docs/resourcepacks/v" + project.version + ".zip")
	val md = MessageDigest.getInstance("SHA-1")
	val resourcePackBytes = resourcePack.readBytes()
	md.update(resourcePackBytes, 0, resourcePackBytes.size)
	val sha1bytes = md.digest()
	val sha1hashString = String.format("%040x", BigInteger(1, sha1bytes))
	sha1hashString
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
