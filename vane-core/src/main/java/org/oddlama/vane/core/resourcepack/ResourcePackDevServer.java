package org.oddlama.vane.core.resourcepack;

import com.google.common.hash.Hashing;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class ResourcePackDevServer implements HttpHandler {

	private final ResourcePackDistributor resource_pack_distributor;
	private final File file;

	public ResourcePackDevServer(ResourcePackDistributor resource_pack_distributor, File file) {
		this.resource_pack_distributor = resource_pack_distributor;
		this.file = file;
	}

	@SuppressWarnings({"deprecation", "UnstableApiUsage"})
	public void serve() {
		try {
			final HttpServer httpServer = HttpServer.create(new InetSocketAddress(9000), 0);
			var hash = com.google.common.io.Files.asByteSource(this.file).hash(Hashing.sha1());
			resource_pack_distributor.sha1 = hash.toString();
			resource_pack_distributor.url = "http://localhost:9000/vane-resource-pack.zip";

			httpServer.createContext("/", this);
			httpServer.setExecutor(null);
			httpServer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void handle(HttpExchange he) throws IOException {
		String method = he.getRequestMethod();
		if (!("HEAD".equals(method) || "GET".equals(method))) {
			he.sendResponseHeaders(501, -1);
			return;
		}

		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			he.sendResponseHeaders(404, -1);
			return;
		}

		he.getResponseHeaders().set("Content-Type", "application/zip");
		if ("GET".equals(method)) {
			he.sendResponseHeaders(200, file.length());
			OutputStream os = he.getResponseBody();
			fis.transferTo(os);
			os.close();
		} else {
			he.sendResponseHeaders(200, -1);
		}
		fis.close();
	}
}
