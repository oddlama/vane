# Module vane-busybar-common

The parts of vane-busybar that do not depend on Paper, shared with the proxy side in
`vane-proxy-core`: the HTTP server bridges connect to, its TLS identity, player faces, pairing
strings and tokens, and the protocol backends use to report to the proxy.

It depends on nothing but the JDK and org.json. vane-busybar and vane-velocity both shade it, so
neither needs the other at runtime.

# Package org.oddlama.vane.busybar.common

Bridge server, TLS, heads, pairing, permission names, and the backend link protocol.
