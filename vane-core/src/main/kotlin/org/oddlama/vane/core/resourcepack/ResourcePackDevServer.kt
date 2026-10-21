package org.oddlama.vane.core.resourcepack

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.logging.Level

/**
 * Hosts a local HTTP server that serves a generated resource pack during development.
 *
 * @param resourcePackDistributor the distributor whose URL and SHA-1 are kept in sync.
 * @param file the resource pack zip file to serve.
 */
class ResourcePackDevServer(
    private val resourcePackDistributor: ResourcePackDistributor,
    private val file: File,
) : HttpHandler {
    /** The running HTTP server instance, if active. */
    private var httpServer: HttpServer? = null

    /** Stops the HTTP server if it is running. */
    fun stop() {
        httpServer?.stop(0)
        httpServer = null
    }

    /** Starts the HTTP server and updates the distributor URL and SHA-1 hash. */
    fun serve() {
        try {
            val server = HttpServer.create(InetSocketAddress(9000), 0)
            val hashBytes = MessageDigest.getInstance("SHA-1").digest(file.readBytes())
            resourcePackDistributor.packSha1 = hashBytes.joinToString("") { "%02x".format(it) }
            resourcePackDistributor.packUrl = "http://localhost:9000/vane-resource-pack.zip"

            server.apply {
                createContext("/", this@ResourcePackDevServer)
                executor = null
                start()
            }
            httpServer = server
        } catch (e: IOException) {
            resourcePackDistributor.module
                ?.log
                ?.log(
                    Level.SEVERE,
                    "Failed to start resource pack dev server",
                    e,
                )
        }
    }

    /**
     * Handles HTTP requests for the resource pack file.
     *
     * Supports `HEAD` and `GET`; returns `404` when the zip file is missing.
     *
     * @param he the incoming HTTP exchange.
     * @throws IOException if request handling fails.
     */
    @Throws(IOException::class)
    override fun handle(he: HttpExchange) {
        val method = he.requestMethod
        if (method !in setOf("HEAD", "GET")) {
            he.sendResponseHeaders(501, -1)
            return
        }

        val fis =
            try {
                file.inputStream()
            } catch (_: FileNotFoundException) {
                he.sendResponseHeaders(404, -1)
                return
            }

        he.responseHeaders.set("Content-Type", "application/zip")
        fis.use {
            if (method == "GET") {
                he.sendResponseHeaders(200, file.length())
                he.responseBody.use { os -> it.transferTo(os) }
            } else {
                he.sendResponseHeaders(200, -1)
            }
        }
    }
}
