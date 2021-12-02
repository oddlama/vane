plugins {
	id("com.github.johnrengelman.shadow") version "6.0.0"
}

dependencies {
	implementation(group = "io.github.waterfallmc", name = "waterfall-api", version = "1.18-R0.1-SNAPSHOT")
	implementation(group = "org.bstats", name = "bstats-bungeecord", version = "1.7")
	implementation(group = "org.json", name = "json", version = "20200518")
}

tasks.create<Copy>("copyJar") {
	from("shadowJar")
	into("${project.rootProject.projectDir}/target")
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
	rename("(.*)-all.jar", "$1.jar")
}

tasks {
	processResources {
		filesMatching("**/bungee.yml") {
			expand(project.properties)
		}
	}

	shadowJar {
		dependencies {
			include(dependency("org.bstats:bstats-bungeecord"))
			include(dependency("org.json:json"))
		}
		relocate("org.bstats", "org.oddlama.vane.vane_waterfall.external.bstats")
		relocate("org.json", "org.oddlama.vane.vane_waterfall.external.json")
	}

	build {
		dependsOn("copyJar")
	}
}
