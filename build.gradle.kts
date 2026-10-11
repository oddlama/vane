plugins {
    `java-library`
    alias(libs.plugins.paperweightUserdev)
    alias(libs.plugins.runPaper) // Adds runServer and runMojangMappedServer tasks for testing
    alias(libs.plugins.dokka)
    alias(libs.plugins.shadow) apply false
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle(rootProject.libs.versions.paper)
    implementation(kotlin("stdlib"))

    // Dokka multi-module aggregation
    dokka(project(":vane-admin"))
    dokka(project(":vane-annotations"))
    dokka(project(":vane-bedtime"))
    dokka(project(":vane-busybar"))
    dokka(project(":vane-core"))
    dokka(project(":vane-enchantments"))
    dokka(project(":vane-permissions"))
    dokka(project(":vane-portals"))
    dokka(project(":vane-proxy-core"))
    dokka(project(":vane-regions"))
    dokka(project(":vane-trifles"))
    dokka(project(":vane-velocity"))
    dokka(project(":vane-geyser-extension"))
}

dokka {
    moduleName.set("Vane")
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
    }
}

kotlin {
    jvmToolchain(25)
}

// We don't need to generate an empty `vane.jar`
tasks.withType<Jar> {
    enabled = false
}


tasks {
    runServer {
        pluginJars(vanePlugins.map { it.tasks.findByName("copyJar")?.inputs?.files })
        jvmArgs(
            "-Xms2G",
            "-Xmx2G",
            "-XX:+AlwaysPreTouch",
            "-XX:+DisableExplicitGC",
            "-XX:+ParallelRefProcEnabled",
            "-XX:+PerfDisableSharedMem",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseG1GC",
            "-XX:G1HeapRegionSize=8M",
            "-XX:G1HeapWastePercent=5",
            "-XX:G1MaxNewSizePercent=40",
            "-XX:G1MixedGCCountTarget=4",
            "-XX:G1MixedGCLiveThresholdPercent=90",
            "-XX:G1NewSizePercent=30",
            "-XX:G1RSetUpdatingPauseTimePercent=5",
            "-XX:G1ReservePercent=20",
            "-XX:InitiatingHeapOccupancyPercent=15",
            "-XX:MaxGCPauseMillis=200",
            "-XX:MaxTenuringThreshold=1",
            "-XX:SurvivorRatio=32",
            "-Dusing.aikars.flags=https://mcflags.emc.gs",
            "-Daikars.new.flags=true",
            "-Dcom.mojang.eula.agree=true"
        )
    }

    register("runVelocity") {
        description = "Runs Velocity using the :vane-velocity project configuration"
        group = "run velocity"
        dependsOn(":vane-velocity:runVelocity")
    }
}

// Common settings to all subprojects.
subprojects {
    // `java-library` already applies `java`, so applying both is redundant.
    pluginManager.apply("java-library")
    pluginManager.apply("org.jetbrains.dokka")
    // Every module is pure Kotlin, so the JVM plugin and toolchain are applied centrally
    // instead of being repeated in all thirteen module scripts.
    pluginManager.apply("org.jetbrains.kotlin.jvm")

    group = "org.oddlama.vane"
    version = "1.23.0-beta.3"

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.opencollab.dev/main/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.mikeprimm.com/")
        maven("https://repo.codemc.org/repository/maven-public/")
        maven("https://jitpack.io")
        maven("https://api.modrinth.com/maven")
        maven("https://repo.bluecolored.de/releases")
        maven("https://repo.thenextlvl.net/releases")
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(arrayOf("-Xlint:all", "-Xlint:-processing", "-Xdiags:verbose"))
        options.encoding = "UTF-8"
    }

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(25)
    }

    // The shadowed jars must not carry Kotlin's per-file module metadata, which collides
    // whenever two vane jars are loaded by the same class loader.
    tasks.withType<Jar>().configureEach {
        if (name == "shadowJar") {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            exclude("META-INF/*.kotlin_module")
        }
    }

    dependencies {
        compileOnly(rootProject.libs.annotations)
        implementation(kotlin("stdlib"))
        testImplementation(kotlin("test"))

        // paper-api pulls maven-resolver-provider 3.9.6, whose maven-artifact/maven-model chain
        // resolves commons-lang3 3.12.0 (CVE-2025-48924, fixed in 3.18.0) and plexus-utils 3.5.1
        // (CVE-2025-67030, fixed in 3.6.1). No vane code touches either library and both stay
        // compile-only -- nothing shadows them into a jar -- but dependency scanners report the
        // resolved graph, so pin the patched releases. 3.20.0 is also the commons-lang3 Paper
        // itself ships in its bundled libraries, so the pin matches what runs on the server.
        //
        // `compileOnly` is constrained next to `implementation` because it does not extend it, and
        // the Paper artifact arrives through it in every module (plain `paper-api` in
        // vane-annotations, the paperweight dev bundle everywhere else).
        constraints {
            implementation(rootProject.libs.commonsLang3)
            implementation(rootProject.libs.plexusUtils)
            compileOnly(rootProject.libs.commonsLang3)
            compileOnly(rootProject.libs.plexusUtils)
        }
    }

    configure<org.jetbrains.dokka.gradle.DokkaExtension> {
        moduleName.set(project.name)
        dokkaSourceSets.configureEach {
            // Module-level overview and per-package docs, if the module provides them.
            // The `# Module <name>` heading inside must match moduleName above.
            val moduleDoc = project.file("Module.md")
            if (moduleDoc.exists()) {
                includes.from(moduleDoc)
            }

            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl("https://github.com/0xSoul24/vane/blob/Kotlin/${project.name}/src/main/kotlin")
                remoteLineSuffix.set("#L")
            }
        }
    }
}

// All Paper plugins.
// Excluded modules never touch Paper internals, so pulling in the multi-hundred-megabyte
// Paper dev bundle (and its per-module remapping cache) there is pure overhead:
//   - vane-geyser-extension targets the Geyser API only,
//   - vane-velocity / vane-proxy-core are proxy-side,
//   - vane-annotations only names `org.bukkit.Material` in annotation members, which the
//     plain `paper-api` artifact provides just as well (see its own build script).
configure(subprojects.filter {
    !listOf("vane-velocity", "vane-proxy-core", "vane-geyser-extension", "vane-annotations")
        .contains(it.name)
}) {
    pluginManager.apply("io.papermc.paperweight.userdev")

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
    listOf(
        "vane-admin",
        "vane-bedtime",
        "vane-busybar",
        "vane-core",
        "vane-enchantments",
        "vane-permissions",
        "vane-portals",
        "vane-regions",
        "vane-trifles"
    ).contains(it.name)
}) {
    tasks.register<Copy>("copyJar") {
        description = "Copies the shadow jar to the target directory"
        evaluationDependsOn(project.path)
        from(tasks.findByPath("shadowJar"))
        into("${project.rootProject.projectDir}/target")
        rename("(.+)-all.jar", "$1.jar")
    }
}


// All Projects except proxies, annotations and Geyser extension.
val vanePlugins = subprojects.filter {
    !listOf("vane-annotations", "vane-velocity", "vane-proxy-core", "vane-geyser-extension").contains(it.name)
}
configure(vanePlugins) {
    tasks {
        named("build") {
            dependsOn("copyJar")
        }

        val projectName = project.name
        val projectVersion = project.version.toString()
        named<ProcessResources>("processResources") {
            inputs.property("name", projectName)
            inputs.property("version", projectVersion)
            filesMatching("**/*plugin.yml") {
                expand(mapOf("name" to projectName, "version" to projectVersion))
            }
        }
    }

    dependencies {
        compileOnly(project(":vane-annotations"))
    }
}

// All Paper plugins except core.
// vane-geyser-extension is excluded for the same reason it is excluded from paperweight above: it
// targets the Geyser API only and imports nothing from vane-core, so depending on it just put
// vane-core (and everything vane-core resolves) on this module's compile classpath and made its
// build wait on :vane-core:shadowJar for nothing.
configure(subprojects.filter {
    !listOf("vane-annotations", "vane-core", "vane-velocity", "vane-proxy-core", "vane-geyser-extension")
        .contains(it.name)
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

// Shipped Paper plugins other than vane-core.
//
// vane-core is the only plugin that bundles shared third-party code. Every other vane plugin
// declares `vane-core: required: true` in its paper-plugin.yml, which places it in the same Paper
// class loader group, so it can use the copy vane-core already ships instead of carrying its own.
// The project has always relied on this for `org.json`; extending it to the Kotlin standard
// library drops an identical ~4.9 MB / 989 classes from each of these seven jars.
//
// The relocations still have to be declared: they rewrite the references inside each plugin's own
// bytecode to the names vane-core publishes.
//
// vane-enchantments is deliberately absent: it is the one plugin with a `bootstrapper`, and a
// bootstrapper runs inside `Bootstrap.bootStrap` before any plugin is loaded, so its class loader
// has no dependency group yet and can only see its own jar. Sharing there fails with
// NoClassDefFoundError on kotlin.jvm.internal.Intrinsics, so it keeps its own copy.
configure(subprojects.filter {
    listOf(
        "vane-admin",
        "vane-bedtime",
        "vane-busybar",
        "vane-permissions",
        "vane-portals",
        "vane-regions",
        "vane-trifles"
    ).contains(it.name)
}) {
    tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
        // Bundle none of the resolved dependencies; vane-core provides them all at runtime.
        dependencies {
            exclude { true }
        }
        relocate("kotlin", "org.oddlama.vane.external.kotlin")
        relocate("org.json", "org.oddlama.vane.external.json")
    }
}

// All plugins with map integration
configure(subprojects.filter {
    listOf("vane-core", "vane-bedtime", "vane-portals", "vane-regions").contains(it.name)
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
    description = "Deletes generated runtime translation files"
    delete(fileTree("run").matching {
        include("plugins/vane-*/lang-*.yml")
    })
}

tasks.register<Delete>("cleanVaneConfigurations") {
    group = "run paper"
    description = "Deletes generated configuration files"
    delete(fileTree("run").matching {
        include("plugins/vane-*/config.yml")
    })
}

tasks.register<Delete>("cleanVaneStorage") {
    group = "run paper"
    description = "Deletes runtime storage files"
    delete(fileTree("run").matching {
        include("plugins/vane-*/storage.json")
    })
}

tasks.register<Delete>("cleanVane") {
    group = "run paper"
    description = "Deletes all runtime plugin files"
    delete(fileTree("run").matching {
        include("plugins/vane-*/")
    })
}

tasks.register<Delete>("cleanWorld") {
    group = "run paper"
    description = "Deletes generated world folders"
    delete(fileTree("run").matching {
        include(
            "world",
            "world_nether",
            "world_the_end"
        )
    })
}
