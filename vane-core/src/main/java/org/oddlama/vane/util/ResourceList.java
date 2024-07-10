package org.oddlama.vane.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * from: forums.devx.com/showthread.php?t=153784
 * list resources available from the jar file of the given class
 */
public class ResourceList {

	/**
	 * For all elements of java.class.path get a Collection of resource Pattern
	 * pattern = Pattern.compile(".*"); gets all resources
	 *
	 * @param pattern the pattern to match
	 * @return the resources in the order they are found
	 */
	public static Collection<String> get_resources(final Class<?> clazz, final Pattern pattern) {
		final var jar_url = clazz.getProtectionDomain().getCodeSource().getLocation();
		try {
			return get_resources(new URI(jar_url.toString()).getPath(), pattern);
		} catch (URISyntaxException e) {
			return new ArrayList<String>();
		}
	}

	private static Collection<String> get_resources(final String path, final Pattern pattern) {
		final var retval = new ArrayList<String>();
		final var file = new File(path);
		if (file.isDirectory()) {
			retval.addAll(get_resources_from_directory(file, pattern));
		} else {
			retval.addAll(get_resources_from_jar_file(file, pattern));
		}
		return retval;
	}

	private static Collection<String> get_resources_from_jar_file(final File file, final Pattern pattern) {
		final var retval = new ArrayList<String>();
		ZipFile zf;
		try {
			zf = new ZipFile(file);
		} catch (final IOException e) {
			throw new Error(e);
		}
		final var e = zf.entries();
		while (e.hasMoreElements()) {
			final ZipEntry ze = e.nextElement();
			final String fileName = ze.getName();
			final boolean accept = pattern.matcher(fileName).matches();
			if (accept) {
				retval.add(fileName);
			}
		}
		try {
			zf.close();
		} catch (final IOException e1) {
			throw new Error(e1);
		}
		return retval;
	}

	private static Collection<String> get_resources_from_directory(final File directory, final Pattern pattern) {
		final var retval = new ArrayList<String>();
		final var fileList = directory.listFiles();
		for (final File file : fileList) {
			if (file.isDirectory()) {
				retval.addAll(get_resources_from_directory(file, pattern));
			} else {
				try {
					final String fileName = file.getCanonicalPath();
					final boolean accept = pattern.matcher(fileName).matches();
					if (accept) {
						retval.add(fileName);
					}
				} catch (final IOException e) {
					throw new Error(e);
				}
			}
		}
		return retval;
	}
}
