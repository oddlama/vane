package org.oddlama.vane.busybar.common

import java.io.ByteArrayOutputStream
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Creates self-signed X.509 certificates with the JDK alone, which can sign data but has no public
 * API for building certificates.
 *
 * The result is a minimal v3 certificate without extensions: bridges pin its public key, so they
 * ignore its name and validity. It never expires (RFC 5280 `99991231235959Z`), so it never has to
 * be replaced.
 */
object SelfSignedCertificate {
    private const val TAG_INTEGER = 0x02
    private const val TAG_BIT_STRING = 0x03
    private const val TAG_OID = 0x06
    private const val TAG_UTF8_STRING = 0x0c
    private const val TAG_UTC_TIME = 0x17
    private const val TAG_GENERALIZED_TIME = 0x18
    private const val TAG_SEQUENCE = 0x30
    private const val TAG_SET = 0x31
    private const val TAG_EXPLICIT_0 = 0xa0

    /** ecdsa-with-SHA256 */
    private val ECDSA_WITH_SHA256 = oid(1, 2, 840, 10045, 4, 3, 2)

    /** id-at-commonName */
    private val COMMON_NAME = oid(2, 5, 4, 3)

    /** Validity end meaning "no well-defined expiration date" (RFC 5280, 4.1.2.5). */
    private const val NO_EXPIRY = "99991231235959Z"

    private val UTC_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'").withZone(ZoneOffset.UTC)

    /**
     * Generates a P-256 key pair and a certificate for it, signed by itself.
     *
     * @param commonName subject and issuer common name.
     */
    fun generate(commonName: String): Pair<KeyPair, X509Certificate> {
        val keys = KeyPairGenerator.getInstance("EC")
            .apply { initialize(ECGenParameterSpec("secp256r1")) }
            .generateKeyPair()

        val algorithm = sequence(ECDSA_WITH_SHA256)
        val name = sequence(set(sequence(COMMON_NAME, tlv(TAG_UTF8_STRING, commonName.toByteArray()))))
        // Positive and without a leading zero byte, as DER requires.
        val serial = ByteArray(SERIAL_BYTES).also(SecureRandom()::nextBytes)
        serial[0] = ((serial[0].toInt() and 0x3f) or 0x40).toByte()
        val notBefore = UTC_TIME.format(Instant.now().minus(1, ChronoUnit.DAYS))

        val tbs = sequence(
            tlv(TAG_EXPLICIT_0, tlv(TAG_INTEGER, byteArrayOf(2))), // v3
            tlv(TAG_INTEGER, serial),
            algorithm,
            name,
            sequence(tlv(TAG_UTC_TIME, notBefore.toByteArray()), tlv(TAG_GENERALIZED_TIME, NO_EXPIRY.toByteArray())),
            name,
            keys.public.encoded, // already a DER SubjectPublicKeyInfo
        )
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keys.private)
            update(tbs)
            sign()
        }
        val der = sequence(tbs, algorithm, tlv(TAG_BIT_STRING, byteArrayOf(0) + signature))

        val certificate = CertificateFactory.getInstance("X.509").generateCertificate(der.inputStream()) as X509Certificate
        certificate.verify(keys.public)
        return keys to certificate
    }

    private fun sequence(vararg parts: ByteArray) = tlv(TAG_SEQUENCE, *parts)

    private fun set(vararg parts: ByteArray) = tlv(TAG_SET, *parts)

    /** Encodes one DER element: tag, definite length, then [parts] concatenated. */
    private fun tlv(tag: Int, vararg parts: ByteArray): ByteArray {
        val content = parts.fold(ByteArray(0), ByteArray::plus)
        val out = ByteArrayOutputStream()
        out.write(tag)
        val length = content.size
        if (length < 0x80) {
            out.write(length)
        } else {
            val bytes = generateSequence(length) { it ushr 8 }.takeWhile { it > 0 }.map { it.toByte() }.toList().reversed()
            out.write(0x80 or bytes.size)
            out.write(bytes.toByteArray())
        }
        out.write(content)
        return out.toByteArray()
    }

    /** Encodes an object identifier such as `1.2.840.10045.4.3.2`. */
    private fun oid(vararg arcs: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(arcs[0] * 40 + arcs[1])
        for (arc in arcs.drop(2)) {
            val groups = generateSequence(arc) { it ushr 7 }.takeWhile { it > 0 }.map { it and 0x7f }.toList().reversed()
            groups.ifEmpty { listOf(0) }.forEachIndexed { i, group ->
                out.write(if (i < groups.size - 1) group or 0x80 else group)
            }
        }
        return tlv(TAG_OID, out.toByteArray())
    }

    /** Random bytes in the serial number. */
    private const val SERIAL_BYTES = 16
}
