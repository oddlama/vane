package imex;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	@Override
	public void onLoad() {
	}

	@Override
	public void onEnable() {
		// Connect to database

		// Enable dynmap integration

		// Register listeners

		// Register commands

		// Schedule shutdown if there are no players
		if (getServer().getOnlinePlayers().isEmpty()) {
		}
	}

	@Override
	public void onDisable() {
		// Unregister commands

		// Unregister listeners

		// Disable dynmap integration

		// Close database
	}
}
