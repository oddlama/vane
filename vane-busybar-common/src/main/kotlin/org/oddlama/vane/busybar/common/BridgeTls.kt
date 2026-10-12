package org.oddlama.vane.busybar.common

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

/**
 * TLS identity of a bridge server, in one of three modes:
 *
 * - `auto`: a self-signed certificate generated on first start and kept in the data folder.
 *   Pairing strings carry the SHA-256 of its public key, which bridges pin instead of trusting a
 *   certificate authority. Needs no domain, no open ports besides the stream's, and no renewal.
 * - `keystore`: a PKCS#12 keystore the admin provides, for example converted from Let's Encrypt.
 * - `off`: plain HTTP, for a TLS reverse proxy in front of the port.
 *
 * @param dataFolder where the auto certificate and its key are kept.
 * @param log where key generation is reported.
 */
class BridgeTls(private val dataFolder: File, private val log: BridgeLog) {
    /** How the event stream is secured. */
    enum class Mode { AUTO, KEYSTORE, OFF }

    /** Mode the event stream was last started with. */
    @Volatile
    var mode: Mode = Mode.OFF
        private set

    /** Base64url SHA-256 of the auto certificate's public key while in [Mode.AUTO], otherwise null. */
    @Volatile
    var pin: String? = null
        private set

    /**
     * Builds the TLS context for [mode] and updates [BridgeTls.mode] and [pin].
     *
     * @param keystore PKCS#12 file for [Mode.KEYSTORE], relative to the working directory.
     * @param password password of [keystore] and its key.
     * @return the context, or null to serve plain HTTP.
     * @throws IOException if a key or keystore file cannot be read or written.
     * @throws GeneralSecurityException if a key, certificate or keystore is invalid.
     */
    fun load(mode: Mode, keystore: String?, password: String?): SSLContext? {
        pin = null
        this.mode = mode
        return when (mode) {
            Mode.OFF -> null
            Mode.KEYSTORE -> loadKeystore(keystore, password)
            Mode.AUTO -> {
                val (key, certificate) = autoIdentity()
                pin = pinOf(certificate)
                val password = CharArray(0)
                val keyStore = KeyStore.getInstance("PKCS12").apply {
                    load(null, null)
                    setKeyEntry("busybar", key, password, arrayOf(certificate))
                }
                contextFor(keyStore, password)
            }
        }
    }

    /** Deletes the auto certificate, so the next [load] generates a new key. */
    fun deleteAutoIdentity() {
        autoKeyFile().delete()
        autoCertificateFile().delete()
    }

    /** Loads the admin's PKCS#12 keystore. */
    private fun loadKeystore(keystore: String?, keystorePassword: String?): SSLContext {
        val path = keystore?.takeIf { it.isNotBlank() }
            ?: throw GeneralSecurityException("Tls is 'keystore' but TlsKeystore is empty")
        val password = keystorePassword.orEmpty().toCharArray()
        val keyStore = KeyStore.getInstance("PKCS12")
        File(path).inputStream().use { keyStore.load(it, password) }
        return contextFor(keyStore, password)
    }

    /** Reads the auto certificate and its key, generating both when either is missing. */
    private fun autoIdentity(): Pair<PrivateKey, X509Certificate> {
        val keyFile = autoKeyFile()
        val certificateFile = autoCertificateFile()
        if (keyFile.isFile && certificateFile.isFile) {
            val key = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(readPem(keyFile)))
            val certificate = certificateFile.inputStream().use {
                CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
            }
            return key to certificate
        }

        val (keys, certificate) = SelfSignedCertificate.generate(COMMON_NAME)
        dataFolder.mkdirs()
        writePem(keyFile, "PRIVATE KEY", keys.private.encoded, private = true)
        writePem(certificateFile, "CERTIFICATE", certificate.encoded, private = false)
        log.info("Generated the BUSY Bar TLS certificate, key fingerprint sha256=${pinOf(certificate)}")
        return keys.private to certificate
    }

    private fun autoKeyFile() = File(dataFolder, KEY_FILE)

    private fun autoCertificateFile() = File(dataFolder, CERTIFICATE_FILE)

    companion object {
        /** Private key of the auto certificate, PKCS#8 PEM, readable by the server user only. */
        private const val KEY_FILE = "tls-auto.key"

        /** The auto certificate, PEM. */
        private const val CERTIFICATE_FILE = "tls-auto.crt"

        /** Subject of the auto certificate; bridges pin the key, so it is informational only. */
        private const val COMMON_NAME = "vane-busybar"

        /** The mode named [value], blank meaning [Mode.AUTO], or null for unknown names. */
        fun parseMode(value: String?): Mode? = when (value?.trim()?.lowercase()) {
            "auto", null, "" -> Mode.AUTO
            "keystore" -> Mode.KEYSTORE
            "off" -> Mode.OFF
            else -> null
        }

        /** Base64url SHA-256 of [certificate]'s SubjectPublicKeyInfo, the value bridges pin. */
        fun pinOf(certificate: X509Certificate): String {
            val hash = MessageDigest.getInstance("SHA-256").digest(certificate.publicKey.encoded)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
        }

        private fun contextFor(keyStore: KeyStore, password: CharArray): SSLContext {
            val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                .apply { init(keyStore, password) }
                .keyManagers
            return SSLContext.getInstance("TLS").apply { init(keyManagers, null, null) }
        }

        private fun readPem(file: File): ByteArray {
            val body = file.readLines().filterNot { it.startsWith("-----") }.joinToString("")
            return try {
                Base64.getDecoder().decode(body)
            } catch (e: IllegalArgumentException) {
                throw GeneralSecurityException("$file is not a valid PEM file", e)
            }
        }

        /** Writes [der] as PEM; [private] files get owner-only permissions where the filesystem supports them. */
        private fun writePem(file: File, label: String, der: ByteArray, private: Boolean) {
            val body = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(der)
            val pem = "-----BEGIN $label-----\n$body\n-----END $label-----\n"
            file.delete()
            val path = file.toPath()
            if (private && path.fileSystem.supportedFileAttributeViews().contains("posix")) {
                Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
            }
            Files.writeString(path, pem)
        }
    }
}
