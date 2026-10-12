// The BUSY Bar code shared by vane-busybar on Paper and vane-velocity on the proxy: the bridge HTTP
// server, its TLS, player heads, pairing strings, and the backend link protocol. It uses nothing
// but the JDK and org.json, and is shaded into both plugins, each relocating org.json onto its own copy.
dependencies {
    compileOnly(libs.json)
    testImplementation(libs.json)
}
