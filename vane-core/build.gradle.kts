import java.security.MessageDigest

plugins {
    id("com.gradleup.shadow") version "9.2.2"
    id("net.kyori.blossom") version "2.1.0" // Text replacement for version numbers
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("\$VERSION", project.version.toString())
            }
        }
    }
}

dependencies {
    implementation("org.bstats:bstats-base:3.1.0")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.apache.commons:commons-lang3:3.19.0")
    implementation("org.apache.commons:commons-text:1.14.0")
    api("org.json:json:20250517")
    implementation(project(":vane-annotations"))
}

val resource_pack_sha1 by lazy {
    val resource_pack = File("${projectDir}/../docs/resourcepacks/v" + project.version + ".zip")
    if (!resource_pack.exists()) {
        throw GradleException("The resource pack file $resource_pack is missing.")
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
            include(project(":vane-annotations"))
            include(dependency("org.apache.commons:commons-lang3"))
            include(dependency("org.apache.commons:commons-text"))
        }
        relocate("org.bstats", "org.oddlama.vane.external.bstats")
        relocate("org.reflections", "org.oddlama.vane.external.reflections")
        relocate("org.apache.commons.lang3", "org.oddlama.vane.external.apache.commons.lang3")
        relocate("org.apache.commons.text", "org.oddlama.vane.external.apache.commons.text")
    }

    processResources {
        filesMatching("vane-core.properties") {
            expand(project.properties + mapOf("resource_pack_sha1" to resource_pack_sha1))
        }
    }
}