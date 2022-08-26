plugins {
	id("com.github.johnrengelman.shadow") version "7.1.0"
}

dependencies {
	implementation(group = "io.github.waterfallmc", name = "waterfall-api", version = "1.18-R0.1-SNAPSHOT")
	implementation(group = "org.bstats", name = "bstats-bungeecord", version = "1.8")
	implementation(group = "org.json", name = "json", version = "20200518")
	implementation(rootProject.project(":vane-core"))
}

tasks.create<Copy>("copyJar") {
	from(tasks.shadowJar)
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

			// Utilities to include from vane-core.util
			val includedUtils = listOf(
				"Resolve",
				"TimeUtil"
			)

			from(rootProject.project(":vane-core").sourceSets.main.get().output) {
				for (i in includedUtils) {
					include("org/oddlama/vane/util/$i*.class")
				}
			}
		}

		relocate("org.bstats", "org.oddlama.vane.vane_waterfall.external.bstats")
		relocate("org.json", "org.oddlama.vane.vane_waterfall.external.json")
	}

	build {
		dependsOn("copyJar")
	}
}
