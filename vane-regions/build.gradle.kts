plugins {
    id("com.gradleup.shadow") version "9.2.2"
}

dependencies {
    implementation(project(":vane-portals"))
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("org.json:json:20250517")
}

tasks {
    shadowJar {
        dependencies {
            include(dependency(":vane-portals"))
        }
    }
}
