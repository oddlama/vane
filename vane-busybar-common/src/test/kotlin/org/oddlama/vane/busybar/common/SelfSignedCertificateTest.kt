package org.oddlama.vane.busybar.common

import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.time.ZoneOffset
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLEngineResult.HandshakeStatus
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SelfSignedCertificateTest {
    @Test
    fun `builds a self-signed P-256 certificate that never expires`() {
        val (keys, certificate) = SelfSignedCertificate.generate("vane-busybar")
        assertEquals(3, certificate.version)
        assertEquals("CN=vane-busybar", certificate.subjectX500Principal.name)
        assertEquals(certificate.subjectX500Principal, certificate.issuerX500Principal)
        assertEquals("SHA256withECDSA", certificate.sigAlgName)
        assertEquals(9999, certificate.notAfter.toInstant().atZone(ZoneOffset.UTC).year)
        assertContentEquals(keys.public.encoded, certificate.publicKey.encoded)
        certificate.checkValidity()
        certificate.verify(keys.public)
    }

    @Test
    fun `pins the SHA-256 of the public key in base64url`() {
        val (_, certificate) = SelfSignedCertificate.generate("vane-busybar")
        val pin = BridgeTls.pinOf(certificate)
        assertEquals(43, pin.length)
        assertContentEquals(
            MessageDigest.getInstance("SHA-256").digest(certificate.publicKey.encoded),
            Base64.getUrlDecoder().decode(pin),
        )
        assertNotEquals(pin, BridgeTls.pinOf(SelfSignedCertificate.generate("vane-busybar").second))
    }

    @Test
    fun `completes a TLS handshake presenting the certificate`() {
        val (keys, certificate) = SelfSignedCertificate.generate("vane-busybar")
        val keyStore =
            KeyStore.getInstance("PKCS12").apply {
                load(null, null)
                setKeyEntry("busybar", keys.private, CharArray(0), arrayOf(certificate))
            }
        val keyManagers =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                .apply { init(keyStore, CharArray(0)) }
                .keyManagers
        val server =
            SSLContext.getInstance("TLS")
                .apply { init(keyManagers, null, null) }
                .createSSLEngine()
                .apply { useClientMode = false }

        // Like a pinning bridge: accept any chain, then compare the key afterwards.
        var presented: X509Certificate? = null
        val recorder =
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
                    Unit

                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                    presented = chain[0]
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
        val client =
            SSLContext.getInstance("TLS")
                .apply { init(null, arrayOf<TrustManager>(recorder), null) }
                .createSSLEngine("localhost", 9123)
                .apply { useClientMode = true }

        handshake(client, server)
        assertEquals("TLSv1.3", client.session.protocol)
        assertEquals(BridgeTls.pinOf(certificate), BridgeTls.pinOf(presented!!))
    }

    /** Runs a handshake between two engines in memory. */
    private fun handshake(client: SSLEngine, server: SSLEngine) {
        val size = maxOf(client.session.packetBufferSize, server.session.packetBufferSize)
        val toServer = ByteBuffer.allocate(size)
        val toClient = ByteBuffer.allocate(size)
        val sink =
            ByteBuffer.allocate(
                maxOf(client.session.applicationBufferSize, server.session.applicationBufferSize)
            )
        val empty = ByteBuffer.allocate(0)
        client.beginHandshake()
        server.beginHandshake()

        fun step(engine: SSLEngine, inbound: ByteBuffer, outbound: ByteBuffer) {
            when (engine.handshakeStatus) {
                HandshakeStatus.NEED_WRAP -> engine.wrap(empty, outbound)
                HandshakeStatus.NEED_UNWRAP,
                HandshakeStatus.NEED_UNWRAP_AGAIN -> {
                    inbound.flip()
                    engine.unwrap(inbound, sink)
                    inbound.compact()
                    sink.clear()
                }
                HandshakeStatus.NEED_TASK ->
                    generateSequence { engine.delegatedTask }.forEach(Runnable::run)
                else -> Unit
            }
        }

        repeat(MAX_STEPS) {
            if (
                client.handshakeStatus == HandshakeStatus.NOT_HANDSHAKING &&
                    server.handshakeStatus == HandshakeStatus.NOT_HANDSHAKING
            )
                return
            step(client, toClient, toServer)
            step(server, toServer, toClient)
        }
        error("handshake did not finish")
    }

    private companion object {
        const val MAX_STEPS = 100
    }
}
