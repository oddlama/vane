plugins {
	`java-library`
	id("com.diffplug.spotless") version "6.0.1"
	id("io.papermc.paperweight.userdev") version "1.3.3"
	id("xyz.jpenilla.run-paper") version "1.0.6" // Adds runServer and runMojangMappedServer tasks for testing
}

dependencies {
	paperDevBundle("1.18.1-R0.1-SNAPSHOT")
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

subprojects {
	apply(plugin = "java-library")
	apply(plugin = "java")
	apply(plugin = "com.diffplug.spotless")

	group = "org.oddlama.vane"
	version = "1.6.7"

	repositories() {
		mavenCentral()
		maven("https://papermc.io/repo/repository/maven-public/")
		maven("https://repo.dmulloy2.net/nexus/repository/public/")
		maven("https://repo.mikeprimm.com/")
		maven("https://repo.codemc.org/repository/maven-public/")
		maven("https://jitpack.io")
	}

	tasks.withType<JavaCompile> {
		options.compilerArgs.addAll(arrayOf("-Xlint:all", "-Xlint:-processing", "-Xdiags:verbose"))
		options.encoding = "UTF-8"
	}

	dependencies {
		compileOnly(group = "org.jetbrains", name = "annotations", version = "20.0.0")
		annotationProcessor("org.jetbrains:annotations:20.0.0")
	}

	spotless {
		java {
			importOrder("java", "javax", "com", "net", "org", "")
			removeUnusedImports()
			trimTrailingWhitespace()
			prettier(mapOf("prettier" to "2.5.0", "prettier-plugin-java" to "1.6.0")).config(mapOf("parser" to "java", "printWidth" to 120, "tabWidth" to 4, "useTabs" to true))
		}
	}
}

configure(subprojects.filter {
	!listOf("vane-waterfall").contains(it.name)
}) {
	apply(plugin = "io.papermc.paperweight.userdev")

	dependencies {
		paperDevBundle("1.18.1-R0.1-SNAPSHOT")
	}

	tasks {
		build {
			dependsOn("reobfJar")
		}
	 }
}

configure(subprojects.filter {
	!listOf("vane-annotations", "vane-waterfall").contains(it.name)
}) {
	tasks.create<Copy>("copyJar") {
		from(tasks.reobfJar)
		into("${project.rootProject.projectDir}/target")
	}

	tasks {
		build {
			dependsOn("copyJar")
		}

		processResources {
			filesMatching("**/plugin.yml") {
				expand(project.properties)
			}
		}
	}

	dependencies {
		implementation(group = "com.comphenix.protocol", name = "ProtocolLib", version = "4.8.0-SNAPSHOT")

		compileOnly(project(":vane-annotations"))
		annotationProcessor(project(path = ":vane-annotations", configuration = "reobf"))
	}

	rootProject.tasks.runMojangMappedServer {
		pluginJars(tasks.named<io.papermc.paperweight.tasks.RemapJar>("reobfJar").flatMap { it.inputJar })
	}

	rootProject.tasks.runServer {
		pluginJars(tasks.named<io.papermc.paperweight.tasks.RemapJar>("reobfJar").flatMap { it.outputJar })
	}
}

configure(subprojects.filter {
	!listOf("vane-annotations", "vane-core", "vane-waterfall").contains(it.name)
}) {
	dependencies {
		implementation(project(path = ":vane-core", configuration = "shadow"))
	}
}

configure(subprojects.filter {
	listOf("vane-bedtime", "vane-portals", "vane-regions").contains(it.name)
}) {
	dependencies {
		implementation(group = "us.dynmap", name = "dynmap-api", version = "3.2-SNAPSHOT")
		implementation(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "v1.7.0")
	}
}

runPaper {
	disablePluginJarDetection()
}

tasks.create<Delete>("cleanVaneRuntimeTranslations") {
	group = "run paper"
	delete(fileTree("run").matching {
		include("plugins/vane-*/lang-*.yml")
	})
}

tasks.create<Delete>("cleanVaneConfigurations") {
	group = "run paper"
	delete(fileTree("run").matching {
		include("plugins/vane-*/config.yml")
	})
}

tasks.create<Delete>("cleanVaneStorage") {
	group = "run paper"
	delete(fileTree("run").matching {
		include("plugins/vane-*/storage.json")
	})
}

tasks.create<Delete>("cleanVane") {
	group = "run paper"
	delete(fileTree("run").matching {
		include("plugins/vane-*/")
	})
}

tasks.create<Delete>("cleanWorld") {
	group = "run paper"
	delete(fileTree("run").matching {
		include(
				"world",
				"world_nether",
				"world_the_end"
		)
	})
}
