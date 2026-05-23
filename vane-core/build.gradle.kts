import java.security.MessageDigest

plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.blossom) // Text replacement for version numbers
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
    implementation(libs.bstatsBase)
    implementation(libs.bstatsBukkit)
    implementation(libs.reflections)
    implementation(libs.commonsLang)
    implementation(libs.commonsText)
    api(libs.json)
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

val projectProperties = project.properties

tasks {
    shadowJar {
        dependencies {
            include(dependency("org.bstats:bstats-base"))
            include(dependency("org.bstats:bstats-bukkit"))
            include(dependency("org.reflections:reflections"))
            include(dependency("org.json:json"))
            include(dependency(":vane-annotations"))
            include(dependency("org.apache.commons:commons-lang3"))
            include(dependency("org.apache.commons:commons-text"))
        }
        relocate("org.bstats", "org.oddlama.vane.external.bstats")
        relocate("org.reflections", "org.oddlama.vane.external.reflections")
        relocate("org.json", "org.oddlama.vane.external.json")
        relocate("org.apache.commons.lang3", "org.oddlama.vane.external.apache.commons.lang3")
        relocate("org.apache.commons.text", "org.oddlama.vane.external.apache.commons.text")
    }

    processResources {
        filesMatching("vane-core.properties") {
            expand(projectProperties + mapOf("resource_pack_sha1" to resource_pack_sha1))
        }
    }
}