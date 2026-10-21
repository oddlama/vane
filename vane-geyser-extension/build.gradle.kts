import groovy.json.JsonGenerator
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

plugins {
    alias(libs.plugins.shadow)
}

/**
 * Converts vane's Java resource pack (`docs/resourcepacks/v<version>.zip`) into the Bedrock pack
 * that the extension registers with Geyser, so the two can never drift apart:
 * - `assets/<ns>/textures/item/<name>.png` → `textures/items/<name>.png`, plus an
 *   `item_texture.json` entry `<ns>.item_<name>` (the icon names used by `ItemRegistration`).
 *   Animated textures are cropped to their first frame, since Bedrock item icons do not animate.
 * - `pack.png` → `pack_icon.png`, and a generated `manifest.json`.
 *
 * Translations are not part of the pack: Geyser reads Java language JSON directly from its
 * `locales/overrides` directory. The `assets/<ns>/lang/<locale>.json` files of all modules are
 * merged per locale into `bedrock/locales.json`, which the extension deploys there on startup.
 *
 * The output is deterministic (sorted entries, fixed timestamps), so it only changes when the Java
 * pack or the version does.
 */
abstract class GenerateBedrockPack : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val javaPack: RegularFileProperty

    @get:Input abstract val packVersion: Property<String>

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val files = sortedMapOf<String, ByteArray>()
        val textures = sortedMapOf<String, String>()
        val locales = sortedMapOf<String, java.util.SortedMap<String, String>>()

        ZipFile(javaPack.get().asFile).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toSet()
            fun read(name: String) = zip.getInputStream(zip.getEntry(name)).use { it.readBytes() }

            for (name in names.sorted()) {
                TEXTURE.matchEntire(name)?.let { m ->
                    val (ns, item) = m.destructured
                    textures.put(item, ns)?.let { other ->
                        throw GradleException(
                            "Bedrock texture name collision: '$item' exists in both $other and $ns"
                        )
                    }
                    val png = read(name)
                    files["textures/items/$item.png"] =
                        if ("$name.mcmeta" in names) firstFrame(png) else png
                }
                LANG.matchEntire(name)?.let { m ->
                    @Suppress("UNCHECKED_CAST")
                    val strings = JsonSlurper().parse(read(name)) as Map<String, String>
                    locales.getOrPut(m.groupValues[1]) { sortedMapOf() }.putAll(strings)
                }
            }
            files["pack_icon.png"] = read("pack.png")
        }

        files["textures/item_texture.json"] =
            json(
                mapOf(
                    "resource_pack_name" to "vane",
                    "texture_data" to
                        textures
                            .map { (item, ns) ->
                                "$ns.item_$item" to mapOf("textures" to "textures/items/$item")
                            }
                            .toMap(sortedMapOf()),
                )
            )
        files["manifest.json"] = manifest(packVersion.get())

        val bedrockDir = outputDir.get().asFile.resolve("bedrock")
        bedrockDir.mkdirs()
        bedrockDir.resolve("locales.json").writeBytes(json(locales))

        val out = bedrockDir.resolve("vane.mcpack")
        ZipOutputStream(out.outputStream()).use { zip ->
            files.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name).apply { time = FIXED_TIME })
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    private fun firstFrame(png: ByteArray): ByteArray {
        val image = ImageIO.read(png.inputStream())
        val frame = image.getSubimage(0, 0, image.width, minOf(image.width, image.height))
        return ByteArrayOutputStream().also { ImageIO.write(frame, "png", it) }.toByteArray()
    }

    /**
     * Bedrock clients cache packs by UUID and version, so every release needs a distinct version.
     * `a.b.c-pre.N` maps to `[a, b, c * 1000 + N]` and `a.b.c` to `[a, b, c * 1000 + 999]`, which
     * keeps pre-releases unique and ordered before their release.
     */
    private fun manifest(version: String): ByteArray {
        val m =
            VERSION.matchEntire(version)
                ?: throw GradleException("Unsupported version for the Bedrock pack: $version")
        val (major, minor, patch, pre) = m.destructured
        val preNumber =
            if (pre.isEmpty()) 999 else pre.takeLastWhile { it.isDigit() }.ifEmpty { "0" }.toInt()
        val packVersion = listOf(major.toInt(), minor.toInt(), patch.toInt() * 1000 + preNumber)
        return json(
            mapOf(
                "format_version" to 2,
                "header" to
                    mapOf(
                        "name" to "Vane",
                        "description" to "Vane plugin resource pack ($version)",
                        "uuid" to HEADER_UUID,
                        "version" to packVersion,
                        "min_engine_version" to listOf(1, 16, 0),
                    ),
                "modules" to
                    listOf(
                        mapOf(
                            "type" to "resources",
                            "description" to "Vane plugin resource pack",
                            "uuid" to MODULE_UUID,
                            "version" to packVersion,
                        )
                    ),
            )
        )
    }

    private fun json(value: Any) =
        (JsonOutput.prettyPrint(JSON.toJson(value), true) + "\n").toByteArray(Charsets.UTF_8)

    private companion object {
        val TEXTURE = Regex("""assets/([^/]+)/textures/item/([^/]+)\.png""")
        val LANG = Regex("""assets/[^/]+/lang/([a-z]+_[a-z]+)\.json""")
        val JSON: JsonGenerator = JsonGenerator.Options().disableUnicodeEscaping().build()
        val VERSION = Regex("""(\d+)\.(\d+)\.(\d+)(?:-(.+))?""")

        // Kept from the original hand-converted pack, so clients treat this as the same pack.
        const val HEADER_UUID = "577fb1c6-db62-4588-ad1d-cc175558c0c3"
        const val MODULE_UUID = "8854d882-fa96-4fd9-8772-6a49243dc38b"

        // 1980-02-01: the earliest time a zip entry can hold in every timezone.
        const val FIXED_TIME = 318211200000L
    }
}

val id = project.property("id") as String
val extensionName = project.property("name") as String
val author = project.property("author") as String
val version = project.version as String

// Resolve the Geyser version now and strip any qualifier like "-SNAPSHOT"
val geyserApiVersion: String = rootProject.libs.versions.geyser.get().substringBefore("-")

repositories {
    // Add other repositories here (main repo moved to root build.gradle.kts)
    mavenCentral()
}

dependencies {
    // Geyser API - requested via the version catalog
    compileOnly(rootProject.libs.geyserApi)
    compileOnly(rootProject.libs.gson)

    // Include other dependencies here - e.g., configuration libraries.
}

// The Bedrock pack is generated from the same Java pack vane-core distributed and bundled into the
// jar, where VaneGeyser registers it with Geyser.
val generateBedrockPack =
    tasks.register<GenerateBedrockPack>("generateBedrockPack") {
        description =
            "Converts the Java resource pack into the Bedrock pack bundled with the extension"
        javaPack.set(rootProject.layout.projectDirectory.file("docs/resourcepacks/v$version.zip"))
        packVersion.set(version)
        outputDir.set(layout.buildDirectory.dir("generated/bedrock-pack"))
    }

sourceSets.main {
    resources.srcDir(generateBedrockPack)
}

// These are plain constants from gradle.properties, so they can be validated right away
// instead of deferring to afterEvaluate.
require(Regex("[a-z][a-z0-9-_]{0,63}").matches(id)) {
    "Invalid extension id $id! Must only contain lowercase letters, and cannot start with a number."
}

require(Regex("^[A-Za-z_.-]+$").matches(extensionName)) {
    "Invalid extension name $extensionName! Must only contain letters, dots, dashes and underscores."
}

tasks {
    // This automatically fills in the extension.yml file.
    // The values are copied into locals first: capturing the script-level properties directly
    // makes the task hold a reference to the build script, which the configuration cache
    // cannot serialize.
    val extensionId = id
    val displayName = extensionName
    val extensionAuthor = author
    val extensionVersion = version
    val apiVersion = geyserApiVersion
    processResources {
        inputs.property("id", extensionId)
        inputs.property("name", displayName)
        inputs.property("api", apiVersion)
        inputs.property("version", extensionVersion)
        inputs.property("author", extensionAuthor)
        filesMatching("extension.yml") {
            expand(
                "id" to extensionId,
                "name" to displayName,
                "api" to apiVersion,
                "version" to extensionVersion,
                "author" to extensionAuthor,
            )
        }
    }

    // Disable the default plain jar so we only output a single jar file
    // and configure the shadowJar (fat jar) to not use the "-all" classifier
    // so the produced artifact matches the normal jar naming.
    named("jar") {
        enabled = false
    }

    // Configure the shadow/fat jar: include kotlin stdlib and relocate it
    // so the extension doesn't conflict with other plugins at runtime.
    shadowJar {
        // Ensure the shadow/fat jar has no "-all" classifier so the artifact
        // matches the standard jar name (single output file).
        archiveClassifier.set("")

        // Three classes of extension code sit on top of a ~5.3 MB shaded Kotlin runtime, and a
        // Geyser extension has no other artifact to share one with. Pruning to what is actually
        // reachable is by far the largest thing that can be done to this jar.
        minimize()

        dependencies {
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
        }

        relocate("kotlin", "org.oddlama.vane.vane_geyser_extension.external.kotlin")
    }

    // Make assemble produce the shadow jar
    named("assemble") {
        dependsOn(named("shadowJar"))
    }

    // Copy produced shadow jar to the repository target directory like other modules
    // (registered at top-level below to avoid task container receiver issues)
}

// Register copyJar at top-level so we can reference the shadowJar task provider
tasks.register<Copy>("copyJar") {
    description = "Copy the produced shadow jar to the repository target directory"
    from(tasks.named("shadowJar"))
    into(rootProject.layout.projectDirectory.dir("target"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    rename("(.*)-all.jar", "$1.jar")
}

// Ensure build depends on copyJar so the artifact is placed in /target
tasks.named("build") {
    dependsOn("copyJar")
}
