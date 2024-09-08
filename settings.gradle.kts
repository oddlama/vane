pluginManagement {
	repositories {
		gradlePluginPortal()
		//maven("https://papermc.io/repo/repository/maven-public/")
	}
}

rootProject.name = "vane"

// https://docs.gradle.org/current/dsl/org.gradle.api.initialization.Settings.html
// Adds the given projects to the build. Each path in the supplied list is treated as the path of a project to add to
// the build. Note that these paths are not file paths, but instead specify the location of the new project in the
// project hierarchy. As such, the supplied paths must use the ':' character as separator (and NOT '/').
include(":vane-admin")
include(":vane-annotations")
include(":vane-bedtime")
include(":vane-core")
include(":vane-enchantments")
include(":vane-permissions")
include(":vane-portals")
include(":vane-proxy-core")
//include(":vane-plexmap")
include(":vane-regions")
include(":vane-trifles")
include(":vane-velocity")
