plugins {
	`java-library`
	id("io.papermc.paperweight.userdev") version "2.0.0-beta.16"
	id("xyz.jpenilla.run-paper") version "2.3.1" // Adds runServer and runMojangMappedServer tasks for testing
}

dependencies {
	paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// We don't need to generate an empty `vane.jar`
tasks.withType<Jar> {
	enabled = false
}

// Common settings to all subprojects.
subprojects {
	apply(plugin = "java-library")
	apply(plugin = "java")

	group = "org.oddlama.vane"
	version = "1.18.0"

	repositories {
		mavenLocal()
		mavenCentral()
		maven("https://repo.papermc.io/repository/maven-public/")
		maven("https://repo.dmulloy2.net/repository/public/")
		maven("https://repo.mikeprimm.com/")
		maven("https://repo.codemc.org/repository/maven-public/")
		maven("https://jitpack.io")
		maven("https://api.modrinth.com/maven")
		maven("https://repo.bluecolored.de/releases")
	}

	tasks.withType<JavaCompile> {
		options.compilerArgs.addAll(arrayOf("-Xlint:all", "-Xlint:-processing", "-Xdiags:verbose"))
		options.encoding = "UTF-8"
	}

	dependencies {
		compileOnly(group = "org.jetbrains", name = "annotations", version = "26.0.1")
		annotationProcessor("org.jetbrains:annotations:26.0.1")
	}
}

// All Paper Plugins + Annotations.
configure(subprojects.filter {
	!listOf("vane-velocity", "vane-proxy-core").contains(it.name)
}) {
	apply(plugin = "io.papermc.paperweight.userdev")

	tasks.withType<JavaCompile> {
		options.compilerArgs.addAll(arrayOf("-Xlint:-this-escape"))
	}

	dependencies {
		paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")
	}
}

// All Projects with jar shadow
configure(subprojects.filter {
	listOf("vane-regions", "vane-core", "vane-portals", "vane-regions").contains(it.name)
}) {
	tasks.register<Copy>("copyJar") {
		evaluationDependsOn(project.path)
		from(tasks.findByPath("shadowJar"))
		into("${project.rootProject.projectDir}/target")
		rename("(.+)-all.jar", "$1.jar")
	}
}

// All Projects without jar shadow
configure(subprojects.filter {
	listOf("vane-admin", "vane-bedtime", "vane-enchantments", "vane-permissions", "vane-trifles").contains(it.name)
}) {
	tasks.register<Copy>("copyJar") {
		from(tasks.jar)
		into("${project.rootProject.projectDir}/target")
	}
}

// All Projects except proxies and annotations.
configure(subprojects.filter {
	!listOf("vane-annotations", "vane-velocity", "vane-proxy-core").contains(it.name)
}) {
	tasks {
		build {
			dependsOn("copyJar")
		}

		processResources {
			filesMatching("**/*plugin.yml") {
				expand(project.properties)
			}
		}
	}

	dependencies {
		implementation(group = "com.comphenix.protocol", name = "ProtocolLib", version = "5.3.0")

		compileOnly(project(":vane-annotations"))
		annotationProcessor(project(path = ":vane-annotations", configuration = "reobf"))
	}

	rootProject.tasks.runMojangMappedServer {
		// the input to reobf, is the mojmapped jars.
		pluginJars(tasks.named<io.papermc.paperweight.tasks.RemapJar>("reobfJar").flatMap { it.inputJar })
	}

	rootProject.tasks.runServer {
		// the output is the obfuscated jars.
		pluginJars(tasks.named<io.papermc.paperweight.tasks.RemapJar>("reobfJar").flatMap { it.outputJar })
	}
}

// All paper plugins except core.
configure(subprojects.filter {
	!listOf("vane-annotations", "vane-core", "vane-velocity", "vane-proxy-core").contains(it.name)
}) {
	dependencies {
		// https://imperceptiblethoughts.com/shadow/multi-project/#depending-on-the-shadow-jar-from-another-project
		// In a multi-project build, there may be one project that applies Shadow and another that requires the shadowed
		// JAR as a dependency. In this case, use Gradle's normal dependency declaration mechanism to depend on the
		// shadow configuration of the shadowed project.
		implementation(project(path = ":vane-core", configuration = "shadow"))
		// But also depend on core itself.
		implementation(project(path = ":vane-core"))
	}
}

// All plugins with map integration
configure(subprojects.filter {
	listOf("vane-bedtime", "vane-portals", "vane-regions").contains(it.name)
}) {
	dependencies {
		implementation(group = "us.dynmap", name = "DynmapCoreAPI", version = "3.7-beta-6")
		implementation(group = "de.bluecolored", name = "bluemap-api", version = "2.7.3")
	}
}

runPaper {
	disablePluginJarDetection()
}

tasks.register<Delete>("cleanVaneRuntimeTranslations") {
	group = "run paper"
	delete(fileTree("run").matching {
		include("plugins/vane-*/lang-*.yml")
	})
}

tasks.register<Delete>("cleanVaneConfigurations") {
	group = "run paper"
	delete(fileTree("run").matching {
		include("plugins/vane-*/config.yml")
	})
}

tasks.register<Delete>("cleanVaneStorage") {
	group = "run paper"
	delete(fileTree("run").matching {
		include("plugins/vane-*/storage.json")
	})
}

tasks.register<Delete>("cleanVane") {
	group = "run paper"
	delete(fileTree("run").matching {
		include("plugins/vane-*/")
	})
}

tasks.register<Delete>("cleanWorld") {
	group = "run paper"
	delete(fileTree("run").matching {
		include(
			"world",
			"world_nether",
			"world_the_end"
		)
	})
}