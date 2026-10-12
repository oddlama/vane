// The shadowJar contents rule for this plugin is applied centrally from the root build
// script: it bundles only vane-busybar-common and relocates onto the copies vane-core ships.
plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(project(":vane-core"))
    compileOnly(libs.json)
    implementation(project(":vane-busybar-common"))

    // Optional integrations. Each one lives in its own class under `hooks/`, which is only
    // loaded after the corresponding plugin was found at runtime.
    compileOnly(project(":vane-admin"))
    compileOnly(project(":vane-bedtime"))
    compileOnly(project(":vane-permissions"))
    compileOnly(project(":vane-portals"))
    compileOnly(project(":vane-regions"))

    // The proxy side, to test a proxied bridge end to end. The proxy provides night-config at runtime.
    testImplementation(project(":vane-proxy-core"))
    testImplementation(libs.nightConfig)
}

// The BUSY Bar bridge (Python, see bridge/README.md) ships next to the plugin jar, with the same
// version. pip installs the zip directly: `pip install barmc-<version>.zip`.
val bridgeZip = tasks.register<Zip>("bridgeZip") {
    description = "Packs the BUSY Bar bridge into the target directory"
    val bridgeVersion = project.version.toString()
    inputs.property("version", bridgeVersion)
    archiveFileName.set("barmc-$bridgeVersion.zip")
    destinationDirectory.set(rootProject.layout.projectDirectory.dir("target"))
    from("bridge") {
        exclude("tests/**", ".venv/**", "**/__pycache__/**", ".pytest_cache/**", "*.egg-info/**", "build/**", "dist/**")
        // Keep the bridge's version in step with the jars; PEP 440 normalizes 1.23.0-beta.2.
        filesMatching("pyproject.toml") {
            filter { line -> if (line.startsWith("version = ")) "version = \"$bridgeVersion\"" else line }
        }
    }
}

tasks.named("build") {
    dependsOn(bridgeZip)
}
