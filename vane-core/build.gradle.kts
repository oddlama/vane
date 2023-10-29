import java.security.MessageDigest

plugins {
	id("com.github.johnrengelman.shadow") version "7.1.0"
	id("net.kyori.blossom") version "1.2.0" // Text replacement for version numbers
}

blossom {
	replaceToken("\$VERSION", project.version)
}

dependencies {
	implementation(group = "org.bstats", name = "bstats-base", version = "3.0.0")
	implementation(group = "org.bstats", name = "bstats-bukkit", version = "3.0.0")
	implementation(group = "org.reflections", name = "reflections", version = "0.10.2")
	implementation(group = "org.json", name = "json", version = "20200518")
	implementation(project(":vane-annotations"))
}

val resource_pack_sha1 by lazy {
	val resource_pack = File("${projectDir}/../docs/resourcepacks/v" + project.version + ".zip")
	if (!resource_pack.exists()) {
		throw GradleException("The resource pack file " + resource_pack + " is missing.")
	}
	val md = MessageDigest.getInstance("SHA-1")
	val resource_pack_bytes = resource_pack.readBytes()
	md.update(resource_pack_bytes, 0, resource_pack_bytes.size)
	val sha1_bytes = md.digest()
	val sha1_hash_string = String.format("%040x", BigInteger(1, sha1_bytes))
	sha1_hash_string
}

tasks {
	shadowJar {
		dependencies {
			include(dependency("org.bstats:bstats-base"))
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
