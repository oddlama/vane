package org.oddlama.vane.geyserextension

import org.geysermc.cumulus.form.CustomForm
import org.geysermc.cumulus.form.SimpleForm
import org.geysermc.geyser.api.command.Command
import org.geysermc.geyser.api.connection.GeyserConnection
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent
import org.geysermc.geyser.api.extension.Extension
import org.oddlama.vane.geyserextension.CommandsRegistration.sendEnchantMenu
import org.oddlama.vane.geyserextension.CommandsRegistration.sendVaneMenu

/**
 * Registers Bedrock-specific commands and provides form-based UI menus for Geyser connections.
 *
 * This singleton handles the entire Bedrock form navigation tree for the Vane plugin suite. When a
 * Bedrock player executes the `/vanegeyser menu` command, a hierarchical [SimpleForm] /
 * [CustomForm] navigation system is presented, allowing access to all vane modules without typing
 * commands.
 *
 * Menu hierarchy:
 * - **Core** — Custom items, enchanting, resource pack generation, reload.
 * - **Admin** — Autostop, game mode, slime chunk, time, weather.
 * - **Permissions** — Group/player management, vouching.
 * - **Regions** — Region management.
 * - **Trifles** — Item finder, heads, set spawn.
 * - **Velocity** — Maintenance scheduling, ping.
 */
object CommandsRegistration {

    /**
     * Registers the `menu` subcommand, reachable as `/vanegeyser menu` for Bedrock players.
     *
     * Geyser roots an extension's commands at [Extension.rootCommand], which defaults to the
     * extension id from `extension.yml` — hence the `vanegeyser` prefix. Overriding that to `vane`
     * shortens the command but registers a `/vane` root on the proxy, which then answers it instead
     * of forwarding to vane-core's own `/vane` on the backend server. The longer prefix is
     * deliberate.
     *
     * The command is restricted to Bedrock-only, player-only connections and opens the main
     * [sendVaneMenu] form when executed.
     *
     * @param event the Geyser command definition event to register commands with.
     * @param extension the parent [Extension] instance used for command builder context.
     */
    fun onGeyserDefineCommands(event: GeyserDefineCommandsEvent, extension: Extension) {
        event.register(
            Command.builder<GeyserConnection>(extension)
                .source(GeyserConnection::class.java)
                .name("menu")
                .description("Vane Command Menu")
                .playerOnly(true)
                .bedrockOnly(true)
                .permission("")
                .executor { source, _, _ ->
                    sendVaneMenu(source)
                }
                .build()
        )
    }

    /**
     * Displays the top-level Vane module selection menu.
     *
     * Presents buttons for Core, Admin, Permissions, Regions, Trifles, and Velocity, each
     * navigating to their respective sub-menu.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendVaneMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Vane Command Menu")
                .button("Core")
                .button("Admin")
                .button("Permissions")
                .button("Regions")
                .button("Trifles")
                .button("Velocity")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> sendCoreMenu(connection)
                        1 -> sendAdminMenu(connection)
                        2 -> sendPermissionsMenu(connection)
                        3 -> sendRegionsMenu(connection)
                        4 -> sendTriflesMenu(connection)
                        5 -> sendVelocityMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the Permissions module menu.
     *
     * Provides options to list groups/permissions, manage player-group assignments, and vouch for
     * players. Each option either executes a command directly or opens a follow-up input form.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendPermissionsMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Permissions Menu")
                .button("List all groups")
                .button("List all permissions")
                .button("List group permissions")
                .button("List player groups")
                .button("List player permissions")
                .button("Add player to group")
                .button("Remove player from group")
                .button("Vouch")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> connection.sendCommand("permission list groups")
                        1 -> connection.sendCommand("permission list permissions")
                        2 -> sendPermListGroupPermissionsForm(connection)
                        3 -> sendPermListPlayerGroupsForm(connection)
                        4 -> sendPermListPlayerPermissionsForm(connection)
                        5 -> sendPermAddPlayerGroupForm(connection)
                        6 -> sendPermRemovePlayerGroupForm(connection)
                        7 -> sendPermVouchForm(connection)
                        8 -> sendVaneMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays a form to query permissions for a specific group.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendPermListGroupPermissionsForm(connection: GeyserConnection) {
        val form =
            CustomForm.builder()
                .title("List Group Permissions")
                .input("Group Name", "e.g. admin", "")
                .validResultHandler { response ->
                    val group = response.asInput(0)
                    if (!group.isNullOrBlank())
                        connection.sendCommand("permission list permissions $group")
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays a form to query which groups a player belongs to.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendPermListPlayerGroupsForm(connection: GeyserConnection) {
        val form =
            CustomForm.builder()
                .title("List Player Groups")
                .input("Player Name", "PlayerName", "")
                .validResultHandler { response ->
                    val player = response.asInput(0)
                    if (!player.isNullOrBlank())
                        connection.sendCommand("permission list groups $player")
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays a form to query the effective permissions of a player.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendPermListPlayerPermissionsForm(connection: GeyserConnection) {
        val form =
            CustomForm.builder()
                .title("List Player Permissions")
                .input("Player Name", "PlayerName", "")
                .validResultHandler { response ->
                    val player = response.asInput(0)
                    if (!player.isNullOrBlank())
                        connection.sendCommand("permission list permissions $player")
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays a player selection form to add a player to a permission group.
     *
     * Shows online players as buttons, plus a "Not connected" option for manual entry and a "Back"
     * button to return to the permissions menu.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendPermAddPlayerGroupForm(connection: GeyserConnection) {
        val form = SimpleForm.builder().title("Add Player to Group")
        val players = getOnlinePlayerNames()

        players.forEach { form.button(it) }
        form.button("Not connected")
        form.button("Back")

        form.validResultHandler { response ->
            val id = response.clickedButtonId()
            if (id == players.size + 1) {
                sendPermissionsMenu(connection)
            } else if (id < players.size) {
                sendPermSelectGroupFlow(connection, players[id], "add")
            } else {
                sendPermAddManualPlayerForm(connection, "add")
            }
        }
        connection.sendForm(form.build())
    }

    /**
     * Displays a player selection form to remove a player from a permission group.
     *
     * Shows online players as buttons, plus a "Not connected" option for manual entry and a "Back"
     * button to return to the permissions menu.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendPermRemovePlayerGroupForm(connection: GeyserConnection) {
        val form = SimpleForm.builder().title("Remove Player from Group")
        val players = getOnlinePlayerNames()

        players.forEach { form.button(it) }
        form.button("Not connected")
        form.button("Back")

        form.validResultHandler { response ->
            val id = response.clickedButtonId()
            if (id == players.size + 1) {
                sendPermissionsMenu(connection)
            } else if (id < players.size) {
                sendPermSelectGroupFlow(connection, players[id], "remove")
            } else {
                sendPermAddManualPlayerForm(connection, "remove")
            }
        }
        connection.sendForm(form.build())
    }

    /**
     * Displays a text input form for manually entering a player name.
     *
     * Used as a fallback when the target player is not currently online.
     *
     * @param connection the Bedrock player connection to send the form to.
     * @param action the permission action to perform (`"add"` or `"remove"`).
     */
    private fun sendPermAddManualPlayerForm(connection: GeyserConnection, action: String) {
        val form =
            CustomForm.builder()
                .title("Player Name")
                .input("Player Name", "PlayerName", "")
                .validResultHandler { response ->
                    val player = response.asInput(0)
                    if (!player.isNullOrBlank()) {
                        sendPermSelectGroupFlow(connection, player, action)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays a group selection form for a permission action on a specific player.
     *
     * Shows the available groups (default, user, verified, admin) and executes the corresponding
     * `permission add/remove` command on selection.
     *
     * @param connection the Bedrock player connection to send the form to.
     * @param player the target player name.
     * @param action the permission action to perform (`"add"` or `"remove"`).
     */
    private fun sendPermSelectGroupFlow(
        connection: GeyserConnection,
        player: String,
        action: String,
    ) {
        val groups = listOf("default", "user", "verified", "admin")
        val form = SimpleForm.builder().title("Select Group for $player")
        groups.forEach { form.button(it) }
        form.button("Back")
        form.validResultHandler { response ->
            if (response.clickedButtonId() == groups.size) {
                sendPermissionsMenu(connection)
            } else {
                val group = groups[response.clickedButtonId()]
                connection.sendCommand("permission $action $player $group")
            }
        }
        connection.sendForm(form.build())
    }

    /**
     * Displays a player selection form for vouching.
     *
     * Shows online players as buttons, plus a "Not connected" option for manual entry.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendPermVouchForm(connection: GeyserConnection) {
        val form = SimpleForm.builder().title("Vouch for Player")
        val players = getOnlinePlayerNames()

        players.forEach { form.button(it) }
        form.button("Not connected")
        form.button("Back")

        form.validResultHandler { response ->
            val id = response.clickedButtonId()
            if (id == players.size + 1) {
                sendPermissionsMenu(connection)
            } else if (id < players.size) {
                connection.sendCommand("vouch ${players[id]}")
            } else {
                sendPermVouchManualForm(connection)
            }
        }
        connection.sendForm(form.build())
    }

    /**
     * Displays a text input form for manually entering a player name to vouch for.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendPermVouchManualForm(connection: GeyserConnection) {
        val form =
            CustomForm.builder()
                .title("Vouch for Player")
                .input("Player Name", "PlayerName", "")
                .validResultHandler { response ->
                    val player = response.asInput(0)
                    if (!player.isNullOrBlank()) connection.sendCommand("vouch $player")
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the Regions module menu.
     *
     * Provides access to the regions management menu and help commands.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendRegionsMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Regions Menu")
                .button("Menu")
                .button("Help")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> connection.sendCommand("regions")
                        1 -> connection.sendCommand("regions help")
                        2 -> sendVaneMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the Trifles module menu.
     *
     * Provides access to item finding, heads management, and spawn setting.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendTriflesMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Trifles Menu")
                .button("Find item")
                .button("Heads")
                .button("Set spawn")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> sendTriflesFindItemForm(connection)
                        1 -> sendTriflesHeadsMenu(connection)
                        2 -> connection.sendCommand("setspawn")
                        3 -> sendVaneMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays a text input form to search for an item by material name.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendTriflesFindItemForm(connection: GeyserConnection) {
        val form =
            CustomForm.builder()
                .title("Find Item")
                .input("Material Name", "e.g. diamond_sword", "")
                .validResultHandler { response ->
                    val material = response.asInput(0)
                    if (!material.isNullOrBlank()) connection.sendCommand("finditem $material")
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the Trifles Heads sub-menu.
     *
     * Provides access to the heads in-game menu and help command.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendTriflesHeadsMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Heads Menu")
                .button("Menu")
                .button("Help")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> connection.sendCommand("heads")
                        1 -> connection.sendCommand("heads help")
                        2 -> sendTriflesMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the Velocity proxy module menu.
     *
     * Provides access to maintenance management and the ping command.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendVelocityMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Velocity Menu")
                .button("Maintenance")
                .button("Ping")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> sendVelocityMaintenanceMenu(connection)
                        1 -> connection.sendCommand("ping")
                        2 -> sendVaneMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the Velocity maintenance management menu.
     *
     * Provides options to check status, enable/disable, cancel, or schedule maintenance.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendVelocityMaintenanceMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Maintenance Menu")
                .button("Status")
                .button("On")
                .button("Off")
                .button("Cancel")
                .button("Schedule")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> connection.sendCommand("maintenance status")
                        1 -> connection.sendCommand("maintenance on")
                        2 -> connection.sendCommand("maintenance off")
                        3 -> connection.sendCommand("maintenance cancel")
                        4 -> sendVelocityMaintenanceScheduleForm(connection)
                        5 -> sendVelocityMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays a form to schedule a maintenance window.
     *
     * Accepts two time-duration inputs: when to start and how long the maintenance lasts.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendVelocityMaintenanceScheduleForm(connection: GeyserConnection) {
        val form =
            CustomForm.builder()
                .title("Schedule Maintenance")
                .input("In (e.g. 1d, 3h5m)", "1h", "")
                .input("Duration (e.g. 2h, 30m)", "30m", "")
                .validResultHandler { response ->
                    val inTime = response.asInput(0)
                    val duration = response.asInput(1)
                    if (!inTime.isNullOrBlank() && !duration.isNullOrBlank()) {
                        connection.sendCommand("maintenance schedule $inTime $duration")
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the Admin module menu.
     *
     * Provides access to autostop, game mode, slime chunk detection, time, and weather controls.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendAdminMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Vane Admin Menu")
                .button("Autostop")
                .button("Game Mode")
                .button("Slimechunk")
                .button("Time")
                .button("Weather")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> sendAutostopMenu(connection)
                        1 -> sendGamemodeMenu(connection)
                        2 -> connection.sendCommand("slimechunk")
                        3 -> sendTimeMenu(connection)
                        4 -> sendWeatherMenu(connection)
                        5 -> sendVaneMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the Autostop management sub-menu.
     *
     * Provides options to abort, schedule, or check the status of an auto-stop timer.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendAutostopMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Vane Autostop Menu")
                .button("Abort")
                .button("Help")
                .button("Schedule")
                .button("Status")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> connection.sendCommand("autostop abort")
                        1 -> connection.sendCommand("autostop help")
                        2 -> sendAutostopScheduleForm(connection)
                        3 -> connection.sendCommand("autostop status")
                        4 -> sendAdminMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays a form to schedule an auto-stop with a duration input.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendAutostopScheduleForm(connection: GeyserConnection) {
        val form =
            CustomForm.builder()
                .title("Autostop Schedule")
                .input("Duration", "e.g. 5d, 10s, 30t", "")
                .validResultHandler { response ->
                    val duration = response.asInput(0)
                    if (!duration.isNullOrBlank()) {
                        connection.sendCommand("autostop schedule $duration")
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the game mode selection menu.
     *
     * Provides toggle and explicit mode options (Survival, Creative, Adventure, Spectator).
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendGamemodeMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Gamemode")
                .button("Toggle")
                .button("Survival")
                .button("Creative")
                .button("Adventure")
                .button("Spectator")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> connection.sendCommand("gamemode")
                        1 -> connection.sendCommand("gamemode survival")
                        2 -> connection.sendCommand("gamemode creative")
                        3 -> connection.sendCommand("gamemode adventure")
                        4 -> connection.sendCommand("gamemode spectator")
                        5 -> sendAdminMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the time-of-day selection menu.
     *
     * Provides presets: Dawn, Day, Noon, Afternoon, Dusk, Night, and Midnight.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendTimeMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Time")
                .button("Dawn")
                .button("Day")
                .button("Noon")
                .button("Afternoon")
                .button("Dusk")
                .button("Night")
                .button("Midnight")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> connection.sendCommand("time dawn")
                        1 -> connection.sendCommand("time day")
                        2 -> connection.sendCommand("time noon")
                        3 -> connection.sendCommand("time afternoon")
                        4 -> connection.sendCommand("time dusk")
                        5 -> connection.sendCommand("time night")
                        6 -> connection.sendCommand("time midnight")
                        7 -> sendAdminMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the weather selection menu.
     *
     * Provides presets: Clear, Sun, Rain, and Thunder.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendWeatherMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Weather")
                .button("Clear")
                .button("Sun")
                .button("Rain")
                .button("Thunder")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> connection.sendCommand("weather clear")
                        1 -> connection.sendCommand("weather sun")
                        2 -> connection.sendCommand("weather rain")
                        3 -> connection.sendCommand("weather thunder")
                        4 -> sendAdminMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the Core module menu.
     *
     * Provides access to custom item management, enchanting, resource pack generation, help,
     * reload, and a dev-only test command.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendCoreMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Vane Core Menu")
                .button("Custom item")
                .button("Enchant")
                .button("Generate resource pack")
                .button("Help")
                .button("Reload")
                .button("Test (DO NOT USE IF YOU ARE NOT A DEV!)")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> sendCustomItemMenu(connection)
                        1 -> sendEnchantMenu(connection)
                        2 -> connection.sendCommand("vane generate_resource_pack")
                        3 -> connection.sendCommand("vane help")
                        4 -> sendReloadMenu(connection)
                        5 -> connection.sendCommand("vane test_do_not_use_if_you_are_not_a_dev")
                        6 -> sendVaneMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the custom item management sub-menu.
     *
     * Provides options to give custom items or view help.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendCustomItemMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Vane Custom Item Menu")
                .button("Give")
                .button("Help")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> sendCustomItemGiveMenu(connection)
                        1 -> connection.sendCommand("customitem help")
                        2 -> sendCoreMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the custom item give menu, split by module.
     *
     * Provides sub-menus for Enchantments and Trifles custom items.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendCustomItemGiveMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Vane Custom Item Give Menu")
                .button("Enchantments")
                .button("Trifles")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> sendCustomItemGiveEnchantmentsMenu(connection)
                        1 -> sendCustomItemGiveTriflesMenu(connection)
                        2 -> sendCustomItemMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the Enchantment Tomes give menu.
     *
     * Lists all ancient tome variants (normal and enchanted) for direct giving.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendCustomItemGiveEnchantmentsMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Vane Enchantment Tomes")
                .button("Ancient Tome")
                .button("Ancient Tome of Knowledge")
                .button("Ancient Tome of the Gods")
                .button("Enchanted Ancient Tome")
                .button("Enchanted Ancient Tome of Knowledge")
                .button("Enchanted Ancient Tome of the Gods")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 ->
                            connection.sendCommand("customitem give vane_enchantments:ancient_tome")
                        1 ->
                            connection.sendCommand(
                                "customitem give vane_enchantments:ancient_tome_of_knowledge"
                            )
                        2 ->
                            connection.sendCommand(
                                "customitem give vane_enchantments:ancient_tome_of_the_gods"
                            )
                        3 ->
                            connection.sendCommand(
                                "customitem give vane_enchantments:enchanted_ancient_tome"
                            )
                        4 ->
                            connection.sendCommand(
                                "customitem give vane_enchantments:enchanted_ancient_tome_of_knowledge"
                            )
                        5 ->
                            connection.sendCommand(
                                "customitem give vane_enchantments:enchanted_ancient_tome_of_the_gods"
                            )
                        6 -> sendCustomItemGiveMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the context-aware enchantment menu.
     *
     * Inspects the player's held item via reflection into the Geyser session to determine which
     * enchantments are applicable, then presents only those enchantments as buttons. If no item is
     * held (or the item is air), all known enchantments are shown.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendEnchantMenu(connection: GeyserConnection) {
        val formBuilder = SimpleForm.builder().title("Vane Enchant Menu").button("Help")

        val item = getPlayerItemInHand(connection) ?: ""
        val isElytra = item.contains("elytra")
        val isFishingRod = item.contains("fishing_rod")
        val isHelmet =
            item.contains("helmet") ||
                item.contains("skull") ||
                item.contains("head") ||
                item.contains("turtle_shell") ||
                item.contains("pumpkin")
        val isBoots = item.contains("boots")
        val isLeggings = item.contains("leggings")
        val isChestplate = item.contains("chestplate") && !isElytra
        val isArmor = isHelmet || isChestplate || isLeggings || isBoots
        val isSword = item.contains("sword")
        val isAxe = item.contains("axe") && !item.contains("pickaxe")
        val isPickaxe = item.contains("pickaxe")
        val isShovel = item.contains("shovel")
        val isHoe = item.contains("hoe")
        val isTool = isPickaxe || isShovel || isAxe || isHoe
        val isShears = item.contains("shears")
        val isTrident = item.contains("trident")
        val isBow = item.contains("bow") && !item.contains("crossbow")
        val isCrossbow = item.contains("crossbow")
        val isMace = item.contains("mace")

        val hasDurability =
            isSword ||
                isTool ||
                isArmor ||
                isElytra ||
                isFishingRod ||
                isTrident ||
                isBow ||
                isCrossbow ||
                isMace ||
                isShears ||
                item.contains("shield") ||
                item.contains("flint_and_steel") ||
                item.contains("carrot_on_a_stick") ||
                item.contains("warped_fungus_on_a_stick") ||
                item.contains("brush")

        val availableEnchants = mutableListOf<String>()

        if (isElytra) {
            availableEnchants.add("vane_enchantments:angel")
            availableEnchants.add("vane_enchantments:take_off")
            availableEnchants.add("vane_enchantments:wings")
        }
        if (isFishingRod) {
            availableEnchants.add("vane_enchantments:grappling_hook")
            availableEnchants.add("luck_of_the_sea")
            availableEnchants.add("lure")
        }
        if (isHelmet) {
            availableEnchants.add("vane_enchantments:hell_bent")
            availableEnchants.add("respiration")
            availableEnchants.add("aqua_affinity")
        }
        if (isAxe) {
            availableEnchants.add("vane_enchantments:leafchopper")
        }
        if (isTrident) {
            availableEnchants.add("loyalty")
            availableEnchants.add("impaling")
            availableEnchants.add("riptide")
            availableEnchants.add("channeling")
        }
        if (isHoe) {
            availableEnchants.add("vane_enchantments:rake")
            availableEnchants.add("vane_enchantments:seeding")
        }
        if (item.isNotEmpty() && !item.contains("air")) {
            availableEnchants.add("vane_enchantments:soulbound")
            availableEnchants.add("vanishing_curse")
        }

        if (isArmor || isElytra) {
            availableEnchants.add("binding_curse")
        }

        if (isArmor) {
            availableEnchants.add("protection")
            availableEnchants.add("fire_protection")
            availableEnchants.add("blast_protection")
            availableEnchants.add("projectile_protection")
            availableEnchants.add("thorns")
        }
        if (isBoots) {
            availableEnchants.add("feather_falling")
            availableEnchants.add("depth_strider")
            availableEnchants.add("frost_walker")
            availableEnchants.add("soul_speed")
        }
        if (isLeggings) {
            availableEnchants.add("swift_sneak")
        }
        if (isSword) {
            availableEnchants.add("vane_enchantments:lightning")
            availableEnchants.add("sharpness")
            availableEnchants.add("smite")
            availableEnchants.add("bane_of_arthropods")
            availableEnchants.add("knockback")
            availableEnchants.add("fire_aspect")
            availableEnchants.add("looting")
            availableEnchants.add("sweeping_edge")
        }
        if (isAxe) {
            availableEnchants.add("sharpness")
            availableEnchants.add("smite")
            availableEnchants.add("bane_of_arthropods")
        }
        if (isTool) {
            availableEnchants.add("efficiency")
            availableEnchants.add("silk_touch")
            availableEnchants.add("fortune")
        }
        if (isShears) {
            availableEnchants.add("efficiency")
        }
        if (isBow) {
            availableEnchants.add("power")
            availableEnchants.add("punch")
            availableEnchants.add("flame")
            availableEnchants.add("infinity")
        }
        if (isCrossbow) {
            availableEnchants.add("multishot")
            availableEnchants.add("piercing")
            availableEnchants.add("quick_charge")
        }
        if (isMace) {
            availableEnchants.add("density")
            availableEnchants.add("breach")
            availableEnchants.add("wind_burst")
            availableEnchants.add("smite")
            availableEnchants.add("bane_of_arthropods")
            availableEnchants.add("fire_aspect")
        }
        if (hasDurability) {
            availableEnchants.add("vane_enchantments:unbreakable")
            availableEnchants.add("unbreaking")
            availableEnchants.add("mending")
        }

        if (item.isEmpty() || item.contains("air")) {
            val allEnchants =
                listOf(
                    "vane_enchantments:angel",
                    "vane_enchantments:grappling_hook",
                    "vane_enchantments:hell_bent",
                    "vane_enchantments:leafchopper",
                    "vane_enchantments:lightning",
                    "vane_enchantments:rake",
                    "vane_enchantments:seeding",
                    "vane_enchantments:soulbound",
                    "vane_enchantments:take_off",
                    "vane_enchantments:unbreakable",
                    "vane_enchantments:wings",
                    "protection",
                    "fire_protection",
                    "feather_falling",
                    "blast_protection",
                    "projectile_protection",
                    "respiration",
                    "aqua_affinity",
                    "thorns",
                    "depth_strider",
                    "frost_walker",
                    "binding_curse",
                    "sharpness",
                    "smite",
                    "bane_of_arthropods",
                    "knockback",
                    "fire_aspect",
                    "looting",
                    "sweeping_edge",
                    "efficiency",
                    "silk_touch",
                    "unbreaking",
                    "fortune",
                    "power",
                    "punch",
                    "flame",
                    "infinity",
                    "luck_of_the_sea",
                    "lure",
                    "loyalty",
                    "impaling",
                    "riptide",
                    "channeling",
                    "multishot",
                    "piercing",
                    "quick_charge",
                    "mending",
                    "vanishing_curse",
                    "soul_speed",
                    "swift_sneak",
                    "density",
                    "breach",
                    "wind_burst",
                )
            availableEnchants.addAll(allEnchants)
        }

        val uniqueEnchants = availableEnchants.distinct().sorted()
        uniqueEnchants.forEach { ench ->
            val displayName = ench.substringAfter(":")
            formBuilder.button(
                displayName.split("_").joinToString(" ") {
                    it.replaceFirstChar { c -> c.uppercase() }
                }
            )
        }

        formBuilder.validResultHandler { response ->
            if (response.clickedButtonId() == 0) {
                connection.sendCommand("enchant help")
            } else if (response.clickedButtonId() == uniqueEnchants.size + 1) {
                sendCoreMenu(connection)
            } else {
                val clickedEnchant = uniqueEnchants[response.clickedButtonId() - 1]
                val maxLvl = enchantMaxLevels[clickedEnchant.substringAfter(":")] ?: 1
                if (maxLvl > 1) {
                    sendEnchantLevelMenu(connection, clickedEnchant, maxLvl)
                } else {
                    connection.sendCommand("enchant $clickedEnchant")
                }
            }
        }

        formBuilder.button("Back")
        connection.sendForm(formBuilder.build())
    }

    /**
     * Displays a level selection form for multi-level enchantments.
     *
     * @param connection the Bedrock player connection to send the form to.
     * @param enchant the namespaced enchantment identifier (e.g. `"vane_enchantments:angel"`).
     * @param maxLevel the maximum level for this enchantment.
     */
    private fun sendEnchantLevelMenu(connection: GeyserConnection, enchant: String, maxLevel: Int) {
        val form =
            SimpleForm.builder()
                .title(
                    "Level: " +
                        enchant.substringAfter(":").split("_").joinToString(" ") {
                            it.replaceFirstChar { c -> c.uppercase() }
                        }
                )

        for (i in 1..maxLevel) {
            form.button("Level $i")
        }
        form.button("Back")

        form.validResultHandler { response ->
            val id = response.clickedButtonId()
            if (id == maxLevel) {
                sendEnchantMenu(connection)
            } else {
                connection.sendCommand("enchant $enchant ${id + 1}")
            }
        }
        connection.sendForm(form.build())
    }

    /**
     * Reflectively retrieves the Java item identifier of the item in the player's main hand.
     *
     * Uses reflection to access Geyser's internal `GeyserSession` API since the public
     * [GeyserConnection] interface does not expose inventory access. Returns `null` if the session
     * class is unavailable or any reflection call fails.
     *
     * @param connection the Bedrock player connection to inspect.
     * @return the Java identifier string (e.g. `"minecraft:diamond_sword"`), or `null` on failure.
     */
    private fun getPlayerItemInHand(connection: GeyserConnection): String? {
        try {
            val sessionClass = Class.forName("org.geysermc.geyser.session.GeyserSession")
            if (!sessionClass.isInstance(connection)) return null
            val getPlayerInventory = sessionClass.getMethod("getPlayerInventory")
            val inventory = getPlayerInventory.invoke(connection) ?: return null
            val getItemInHand = inventory.javaClass.getMethod("getItemInHand")
            val itemInHand = getItemInHand.invoke(inventory) ?: return null
            val asItem = itemInHand.javaClass.getMethod("asItem")
            val item = asItem.invoke(itemInHand) ?: return null
            val javaIdentifier = item.javaClass.getMethod("javaIdentifier")
            return javaIdentifier.invoke(item) as? String
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Displays the Trifles custom items give menu.
     *
     * Lists all Trifles items (sickles, scrolls, bottles, etc.) for direct giving.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendCustomItemGiveTriflesMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Vane Trifles Items")
                .button("Backpack")
                .button("Death Scroll")
                .button("Diamond Sickle")
                .button("Empty XP Bottle")
                .button("File")
                .button("Golden Sickle")
                .button("Home Scroll")
                .button("Iron Sickle")
                .button("Large XP Bottle")
                .button("Lodestone Scroll")
                .button("Medium XP Bottle")
                .button("Netherite Sickle")
                .button("North Compass")
                .button("Papyrus Scroll")
                .button("Pouch")
                .button("Reinforced Elytra")
                .button("Slime Bucket")
                .button("Small XP Bottle")
                .button("Spawn Scroll")
                .button("Stone Sickle")
                .button("Trowel")
                .button("Unstable Scroll")
                .button("Wooden Sickle")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> connection.sendCommand("customitem give vane_trifles:backpack")
                        1 -> connection.sendCommand("customitem give vane_trifles:death_scroll")
                        2 -> connection.sendCommand("customitem give vane_trifles:diamond_sickle")
                        3 -> connection.sendCommand("customitem give vane_trifles:empty_xp_bottle")
                        4 -> connection.sendCommand("customitem give vane_trifles:file")
                        5 -> connection.sendCommand("customitem give vane_trifles:golden_sickle")
                        6 -> connection.sendCommand("customitem give vane_trifles:home_scroll")
                        7 -> connection.sendCommand("customitem give vane_trifles:iron_sickle")
                        8 -> connection.sendCommand("customitem give vane_trifles:large_xp_bottle")
                        9 -> connection.sendCommand("customitem give vane_trifles:lodestone_scroll")
                        10 ->
                            connection.sendCommand("customitem give vane_trifles:medium_xp_bottle")
                        11 ->
                            connection.sendCommand("customitem give vane_trifles:netherite_sickle")
                        12 -> connection.sendCommand("customitem give vane_trifles:north_compass")
                        13 -> connection.sendCommand("customitem give vane_trifles:papyrus_scroll")
                        14 -> connection.sendCommand("customitem give vane_trifles:pouch")
                        15 ->
                            connection.sendCommand("customitem give vane_trifles:reinforced_elytra")
                        16 -> connection.sendCommand("customitem give vane_trifles:slime_bucket")
                        17 -> connection.sendCommand("customitem give vane_trifles:small_xp_bottle")
                        18 -> connection.sendCommand("customitem give vane_trifles:spawn_scroll")
                        19 -> connection.sendCommand("customitem give vane_trifles:stone_sickle")
                        20 -> connection.sendCommand("customitem give vane_trifles:trowel")
                        21 -> connection.sendCommand("customitem give vane_trifles:unstable_scroll")
                        22 -> connection.sendCommand("customitem give vane_trifles:wooden_sickle")
                        23 -> sendCustomItemGiveMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Displays the module reload selection menu.
     *
     * Provides options to reload all modules at once or individual modules.
     *
     * @param connection the Bedrock player connection to send the form to.
     */
    private fun sendReloadMenu(connection: GeyserConnection) {
        val form =
            SimpleForm.builder()
                .title("Vane Reload Menu")
                .button("All")
                .button("Admin")
                .button("Bedtime")
                .button("Core")
                .button("Enchantments")
                .button("Permissions")
                .button("Portals")
                .button("Regions")
                .button("Trifles")
                .button("Back")
                .validResultHandler { response ->
                    when (response.clickedButtonId()) {
                        0 -> connection.sendCommand("vane reload")
                        1 -> connection.sendCommand("vane reload admin")
                        2 -> connection.sendCommand("vane reload bedtime")
                        3 -> connection.sendCommand("vane reload core")
                        4 -> connection.sendCommand("vane reload enchantments")
                        5 -> connection.sendCommand("vane reload permissions")
                        6 -> connection.sendCommand("vane reload portals")
                        7 -> connection.sendCommand("vane reload regions")
                        8 -> connection.sendCommand("vane reload trifles")
                        9 -> sendCoreMenu(connection)
                    }
                }
                .build()
        connection.sendForm(form)
    }

    /**
     * Lookup table mapping enchantment names to their maximum levels.
     *
     * Used by [sendEnchantMenu] to determine whether to show a level selection sub-menu or apply
     * the enchantment directly. Enchantments not present in this map default to level 1.
     */
    private val enchantMaxLevels =
        mapOf(
            "angel" to 5,
            "grappling_hook" to 3,
            "rake" to 4,
            "seeding" to 4,
            "take_off" to 3,
            "wings" to 4,
            "protection" to 4,
            "fire_protection" to 4,
            "feather_falling" to 4,
            "blast_protection" to 4,
            "projectile_protection" to 4,
            "respiration" to 3,
            "thorns" to 3,
            "depth_strider" to 3,
            "frost_walker" to 2,
            "sharpness" to 5,
            "smite" to 5,
            "bane_of_arthropods" to 5,
            "knockback" to 2,
            "fire_aspect" to 2,
            "looting" to 3,
            "sweeping_edge" to 3,
            "efficiency" to 5,
            "unbreaking" to 3,
            "fortune" to 3,
            "power" to 5,
            "punch" to 2,
            "luck_of_the_sea" to 3,
            "lure" to 3,
            "loyalty" to 3,
            "impaling" to 5,
            "riptide" to 3,
            "piercing" to 4,
            "quick_charge" to 3,
            "soul_speed" to 3,
            "swift_sneak" to 3,
            "density" to 5,
            "breach" to 4,
            "wind_burst" to 3,
        )

    /**
     * Retrieves a sorted, deduplicated list of online player Java usernames.
     *
     * Used by permission and vouch forms to display selectable player buttons.
     *
     * @return a sorted list of unique Java usernames of currently connected Bedrock players.
     */
    private fun getOnlinePlayerNames(): List<String> {
        return org.geysermc.geyser.api.GeyserApi.api()
            .onlineConnections()
            .mapNotNull { it.javaUsername() }
            .sorted()
            .distinct()
    }
}
