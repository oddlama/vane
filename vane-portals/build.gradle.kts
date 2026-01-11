plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.json)
}

tasks {
	shadowJar {
		// No special configuration needed
	}
}
