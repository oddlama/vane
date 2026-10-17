// The shadowJar contents rule for this plugin is applied centrally from the root build
// script: it bundles nothing and relocates onto the copies vane-core ships.
plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.json)
    compileOnly(project(":vane-core"))
}

// The root build puts vane-core's shadow jar on the classpath next to vane-core itself. Its
// relocated org.json does not match the JSONObjects these tests build, so leave it out here.
tasks.test {
    classpath = classpath.filter { !it.name.matches(Regex("vane-core-.*-all\\.jar")) }
}
