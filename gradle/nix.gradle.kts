import java.io.File
import org.gradle.api.tasks.testing.Test

/** Export the actual Test task's runtime, including module-specific classpath fixes. */
abstract class ExportTestRuntime : DefaultTask() {
    @get:Classpath abstract val runtimeClasspath: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testClassesDirs: ConfigurableFileCollection

    @get:OutputDirectory abstract val destination: DirectoryProperty

    @TaskAction
    fun export() {
        val output = destination.get().asFile
        output.deleteRecursively()
        output.mkdirs()
        val entries =
            runtimeClasspath.files
                .filter { it.exists() }
                .mapIndexed { index, source ->
                    val name = "$index-${source.name}"
                    source.copyRecursively(File(output, name), overwrite = true)
                    name
                }
        File(output, "classpath").writeText(entries.joinToString("\n", postfix = "\n"))
        val classes =
            testClassesDirs.files
                .flatMap { root ->
                    if (!root.exists()) emptyList()
                    else
                        root
                            .walkTopDown()
                            .filter {
                                it.isFile && it.name.endsWith("Test.class") && '$' !in it.name
                            }
                            .map {
                                it.relativeTo(root)
                                    .invariantSeparatorsPath
                                    .removeSuffix(".class")
                                    .replace('/', '.')
                            }
                            .toList()
                }
                .sorted()
        File(output, "classes").writeText(classes.joinToString("\n", postfix = "\n"))
    }
}

val libraries = setOf("vane-annotations", "vane-busybar-common", "vane-proxy-core")

tasks.register("dist") {
    group = "build"
    description = "Build every distributable without starting servers or running tests"
    dependsOn(subprojects.filter { it.name !in libraries }.map { "${it.path}:copyJar" })
    dependsOn(":vane-busybar:bridgeZip")
}

val runtimes =
    subprojects
        .filter { it.file("src/test/kotlin").exists() }
        .map { module ->
            module.tasks.register<ExportTestRuntime>("exportTestRuntime") {
                group = "verification"
                val test = module.tasks.named<Test>("test")
                dependsOn(module.tasks.named("testClasses"))
                // A TaskProvider.map would implicitly depend on executing Test itself.
                runtimeClasspath.from(module.provider { test.get().classpath })
                testClassesDirs.from(module.provider { test.get().testClassesDirs })
                destination.set(module.layout.buildDirectory.dir("nix-test-runtime"))
            }
        }

tasks.register("nixTestRuntimes") {
    group = "verification"
    description = "Compile and export test runtimes for individually sandboxed Nix checks"
    dependsOn(runtimes)
}

// Apply to all archives, including bridgeZip, not only shadowJar.
allprojects {
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}
