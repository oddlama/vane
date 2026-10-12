pluginManagement {
    repositories {
        gradlePluginPortal()
        //maven("https://repo.papermc.io/repository/maven-public/")
    }
    plugins {
        kotlin("jvm") version "2.4.21"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "vane"

// https://docs.gradle.org/current/dsl/org.gradle.api.initialization.Settings.html
// Adds the given projects to the build. Each path in the supplied list is treated as the path of a project to add to
// the build. Note that these paths are not file paths but instead specify the location of the new project in the
// project hierarchy. As such, the supplied paths must use the ':' character as separator (and NOT '/').
include(":vane-admin")
include(":vane-annotations")
include(":vane-bedtime")
include(":vane-busybar")
include(":vane-busybar-common")
include(":vane-core")
include(":vane-enchantments")
include(":vane-permissions")
include(":vane-portals")
include(":vane-proxy-core")
//include(":vane-plexmap")
include(":vane-regions")
include(":vane-trifles")
include(":vane-velocity")

include("vane-geyser-extension")