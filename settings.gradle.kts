pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://papermc.io/repo/repository/maven-public/")
	}
}

rootProject.name = "vane"

include(":vane-admin")
include(":vane-annotations")
include(":vane-bedtime")
include(":vane-core")
include(":vane-enchantments")
include(":vane-permissions")
include(":vane-portals")
include(":vane-regions")
include(":vane-trifles")
include(":vane-waterfall")
