dependencies {
    compileOnly(group = "maven.modrinth", name = "pl3xmap", version = "1.19.2-310")
}

tasks {
    processResources {
        filteringCharset = Charsets.UTF_8.name()
        filesMatching(listOf("addon.yml")) {
            expand("version" to project.version)
        }
    }
}