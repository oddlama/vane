plugins {
	`java-library`
    alias(libs.plugins.paperweightUserdev)
	alias(libs.plugins.runPaper) // Adds runServer and runMojangMappedServer tasks for testing
}

dependencies {
	paperweight.paperDevBundle(rootProject.libs.versions.paper)
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// We don't need to generate an empty `vane.jar`
tasks.withType<Jar> {
	enabled = false
}

tasks.runServer {
    pluginJars(vanePlugins.map { it.tasks.findByName("copyJar")?.inputs?.files })
    downloadPlugins {
        github("dmulloy2", "ProtocolLib", "dev-build", "ProtocolLib.jar")
    }
}

// Common settings to all subprojects.
subprojects {
	apply(plugin = "java-library")
	apply(plugin = "java")

	group = "org.oddlama.vane"
	version = "1.19.0"

	repositories {
		mavenLocal()
		mavenCentral()
		maven("https://repo.papermc.io/repository/maven-public/")
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
		compileOnly(rootProject.libs.annotations)
		annotationProcessor(rootProject.libs.annotations)
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

    tasks {
        reobfJar {
            enabled = false
        }
    }
	dependencies {
		paperweight.paperDevBundle(rootProject.libs.versions.paper)
	}
}

// All Projects with jar shadow
configure(subprojects.filter {
	listOf("vane-regions", "vane-core", "vane-portals", "vane-regions", "vane-trifles").contains(it.name)
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
	listOf("vane-admin", "vane-bedtime", "vane-enchantments", "vane-permissions").contains(it.name)
}) {
	tasks.register<Copy>("copyJar") {
		from(tasks.jar)
		into("${project.rootProject.projectDir}/target")
	}
}

// All Projects except proxies and annotations.
val vanePlugins = subprojects.filter {
	!listOf("vane-annotations", "vane-velocity", "vane-proxy-core").contains(it.name)
}
configure(vanePlugins) {
	val projectProperties = project.properties

	tasks {
		build {
			dependsOn("copyJar")
		}

		processResources {
			filesMatching("**/*plugin.yml") {
				expand(projectProperties)
			}
		}
	}

	dependencies {
		implementation(rootProject.libs.protocollib)

		compileOnly(project(":vane-annotations"))
		annotationProcessor(project(path = ":vane-annotations"))
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
		implementation(rootProject.libs.dynmap)
		implementation(rootProject.libs.bluemap)
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