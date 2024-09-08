plugins {
    id("io.github.goooler.shadow") version "8.1.7"
    id("net.kyori.blossom") version "1.2.0"
}

blossom {
	replaceToken("\$VERSION", project.version)
}

dependencies {
    implementation(group = "com.electronwill.night-config", name = "toml", version = "3.8.0")
    implementation(group = "org.slf4j", name = "slf4j-api", version = "2.0.13")
    implementation(rootProject.project(":vane-core"))
    compileOnly(group = "org.json", name = "json", version = "20200518")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks {
    shadowJar {
        dependencies {
            include(dependency("com.electronwill.night-config:toml"))
        }

        relocate("com.electronwill.night-config", "org.oddlama.vane.vane_velocity.external.night-config")
        relocate("org.json", "org.oddlama.vane.external.json")
    }
}
