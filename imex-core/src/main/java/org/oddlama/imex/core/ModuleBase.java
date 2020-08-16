package org.oddlama.imex.core;

import java.io.IOException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.imex.annotation.ConfigString;
import java.util.logging.Logger;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public abstract class ModuleBase extends JavaPlugin {
	private static final int CONFIG_VERSION = 1;
	private static final int LANG_VERSION = 1;

	private Core core = null;
	public Core get_core() {
		return core;
	}

	protected Logger log;


	@Override
	public void onEnable() {
		// Get core plugin reference, important for inherited configuration
		if (this.getName().equals("imex-core")) {
			core = (Core)this;
		} else {
			core = (Core)getServer().getPluginManager().getPlugin("imex-core");
		}

		log = getLogger();
		if (!reload_configuration()) {
			// Force stop server, we encountered an invalid config file version
			log.severe("Invalid plugin configuration. Shutting down.");
			getServer().shutdown();
		}
	}

	public boolean reload_configuration() {
		// Get data directory
		var data_folder = getDataFolder();
		if (!data_folder.exists()) {
			 data_folder.mkdirs();
		}

		// Generate new file if not existing
		var file = new File(data_folder, "config.yml");
		if (!file.exists()) {
			var builder = new StringBuilder();
			generate_configuration(builder);
			var contents = builder.toString();

			// Save contents to file
			try {
				Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Load config file
		var yaml = YamlConfiguration.loadConfiguration(file);

		// Check config file version
		var version = yaml.getLong("version", 0);
		if (!verify_config_version(file, version)) {
			return false;
		}

		// Reload automatic variables
		on_reload_configuration();

		// Reload localization
		if (!reload_localization()) {
			return false;
		}

		return true;
	}

	private List<String> getResourceFiles(String path) throws IOException {
		var filenames = new ArrayList<String>();

		try (
		    var in = getResourceAsStream(path);
		    var br = new BufferedReader(new InputStreamReader(in))) {
			String resource;
			while ((resource = br.readLine()) != null) {
				filenames.add(resource);
			}
		}

		return filenames;
	}

	private InputStream getResourceAsStream(String resource) {
		final var in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
		return in == null ? getClass().getResourceAsStream(resource) : in;
	}

	public boolean reload_localization() {
		// Get data directory
		var data_folder = getDataFolder();
		if (!data_folder.exists()) {
			 data_folder.mkdirs();
		}

		// Copy all embedded lang files, if version is greater.
		try {
			var f = getResourceFiles("/");
			f.stream().forEach(System.out::println);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Get configured language code
		var lang = get_config_lang();
		if ("inherit".equals(lang)) {
			lang = core.get_config_lang();

			// Fallback to en in case 'inherit' is used in imex-core.
			if ("inherit".equals(lang)) {
				lang = "en";
			}
		}

		// Generate new file if not existing
		var file = new File(data_folder, "lang-" + lang + ".yml");
		if (!file.exists()) {
			log.severe("");
			return false;
		}

		// Load config file
		var yaml = YamlConfiguration.loadConfiguration(file);

		// Check version
		var version = yaml.getLong("version", 0);
		if (!verify_lang_version(file, version)) {
			return false;
		}

		// Reload automatic variables
		on_reload_localization();
		return true;
	}

	private boolean verify_config_version(File file, long version) {
		var expected_version = CONFIG_VERSION;
		if (version != expected_version) {
			log.severe(file.getName() + ": expected version " + expected_version + ", but got " + version);

			if (version == 0) {
				log.severe("Something went wrong while generating or loading the configuration.");
				log.severe("If you are sure your configuration is correct and this isn't a file");
				log.severe("system permission issue, please report this to https://github.com/oddlama/imex/issues");
			} else if (version < expected_version) {
				log.severe("This config is for an older version of " + getName() + ".");
				log.severe("Please backup the file and delete it afterwards. It will");
				log.severe("then be regenerated the next time the server is started.");
			} else {
				log.severe("This config is for a future version of " + getName() + ".");
				log.severe("Please use the correct file for this version, or delete it and");
				log.severe("it will be regenerated next time the server is started.");
			}

			return false;
		}

		return true;
	}

	private boolean verify_lang_version(File file, long version) {
		var expected_version = LANG_VERSION;
		if (version != expected_version) {
			log.severe(file.getName() + ": expected version " + expected_version + ", but got " + version);

			if (version == 0) {
				log.severe("Something went wrong while generating or loading the configuration.");
				log.severe("If you are sure your configuration is correct and this isn't a file");
				log.severe("system permission issue, please report this to https://github.com/oddlama/imex/issues");
			} else if (version < expected_version) {
				log.severe("This language file is for an older version of " + getName() + ".");
				log.severe("Please update your file or use an officially supported language file.");
			} else {
				log.severe("This language file is for a future version of " + getName() + ".");
				log.severe("Please use the correct file for this version, or use an officially");
				log.severe("supported language file.");
			}

			return false;
		}

		return true;
	}

	public abstract String get_config_lang();
	protected void on_reload_configuration() {
		// Automatically generated by @Config*
	}

	protected void on_reload_localization() {
		// Automatically generated by @Lang*
	}

	protected void generate_configuration(StringBuilder builder) {
		// Automatically generated by @Config*
	}
}
