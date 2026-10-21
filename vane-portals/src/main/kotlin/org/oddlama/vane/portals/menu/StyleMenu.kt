package org.oddlama.vane.portals.menu

import java.util.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.functional.Function1
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.menu.*
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.portals.Portals
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent
import org.oddlama.vane.portals.portal.Portal
import org.oddlama.vane.portals.portal.PortalBlock
import org.oddlama.vane.portals.portal.Style

/**
 * Menu component allowing players to configure a portal's visual style.
 *
 * Provides item selectors for each block type used in a style and actions to accept/reset changes.
 *
 * @param context Module context used to resolve translations, configuration and subcomponents.
 * @constructor Create a StyleMenu module component using the provided module [context].
 */
class StyleMenu(context: Context<Portals?>) :
    ModuleComponent<Portals?>(context.namespace("Style")) {
    /** Translated title used for the style editor menu. */
    @LangMessage var langTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the active console block. */
    @LangMessage var langSelectBlockConsoleActiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the active origin block. */
    @LangMessage var langSelectBlockOriginActiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the active portal-area block (unused by default). */
    @LangMessage @Suppress("unused") var langSelectBlockPortalActiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the active boundary variant 1 block. */
    @LangMessage var langSelectBlockBoundary1ActiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the active boundary variant 2 block. */
    @LangMessage var langSelectBlockBoundary2ActiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the active boundary variant 3 block. */
    @LangMessage var langSelectBlockBoundary3ActiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the active boundary variant 4 block. */
    @LangMessage var langSelectBlockBoundary4ActiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the active boundary variant 5 block. */
    @LangMessage var langSelectBlockBoundary5ActiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the inactive console block. */
    @LangMessage var langSelectBlockConsoleInactiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the inactive origin block. */
    @LangMessage var langSelectBlockOriginInactiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the inactive portal-area block. */
    @LangMessage var langSelectBlockPortalInactiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the inactive boundary variant 1 block. */
    @LangMessage var langSelectBlockBoundary1InactiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the inactive boundary variant 2 block. */
    @LangMessage var langSelectBlockBoundary2InactiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the inactive boundary variant 3 block. */
    @LangMessage var langSelectBlockBoundary3InactiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the inactive boundary variant 4 block. */
    @LangMessage var langSelectBlockBoundary4InactiveTitle: TranslatedMessage? = null

    /** Translated title shown when selecting the inactive boundary variant 5 block. */
    @LangMessage var langSelectBlockBoundary5InactiveTitle: TranslatedMessage? = null

    /** Translated title for the defined-style selector. */
    @LangMessage var langSelectStyleTitle: TranslatedMessage? = null

    /** Translated title for the styles filter input shown in the selector. */
    @LangMessage var langFilterStylesTitle: TranslatedMessage? = null

    /** Item representing the active console block when selecting an active block. */
    private val itemBlockConsoleActive: TranslatedItemStack<*>

    /** Item representing the active origin block when selecting an active block. */
    private val itemBlockOriginActive: TranslatedItemStack<*>

    /** Item representing the active portal-area block when selecting an active block (nullable). */
    private val itemBlockPortalActive: TranslatedItemStack<*>?

    /** Item representing the active boundary variant 1 block when selecting an active block. */
    private val itemBlockBoundary1Active: TranslatedItemStack<*>

    /** Item representing the active boundary variant 2 block when selecting an active block. */
    private val itemBlockBoundary2Active: TranslatedItemStack<*>

    /** Item representing the active boundary variant 3 block when selecting an active block. */
    private val itemBlockBoundary3Active: TranslatedItemStack<*>

    /** Item representing the active boundary variant 4 block when selecting an active block. */
    private val itemBlockBoundary4Active: TranslatedItemStack<*>

    /** Item representing the active boundary variant 5 block when selecting an active block. */
    private val itemBlockBoundary5Active: TranslatedItemStack<*>

    /** Item representing the inactive console block when selecting an inactive block. */
    private val itemBlockConsoleInactive: TranslatedItemStack<*>

    /** Item representing the inactive origin block when selecting an inactive block. */
    private val itemBlockOriginInactive: TranslatedItemStack<*>

    /** Item representing the inactive portal-area block when selecting an inactive block. */
    private val itemBlockPortalInactive: TranslatedItemStack<*>

    /** Item representing the inactive boundary variant 1 block when selecting an inactive block. */
    private val itemBlockBoundary1Inactive: TranslatedItemStack<*>

    /** Item representing the inactive boundary variant 2 block when selecting an inactive block. */
    private val itemBlockBoundary2Inactive: TranslatedItemStack<*>

    /** Item representing the inactive boundary variant 3 block when selecting an inactive block. */
    private val itemBlockBoundary3Inactive: TranslatedItemStack<*>

    /** Item representing the inactive boundary variant 4 block when selecting an inactive block. */
    private val itemBlockBoundary4Inactive: TranslatedItemStack<*>

    /** Item representing the inactive boundary variant 5 block when selecting an inactive block. */
    private val itemBlockBoundary5Inactive: TranslatedItemStack<*>

    /** Item used as the accept/apply action for the style editor. */
    private val itemAccept: TranslatedItemStack<*>

    /** Item used to reset any changes made in the style editor. */
    private val itemReset: TranslatedItemStack<*>

    /** Item used to open the defined-style selector. */
    private val itemSelectDefined: TranslatedItemStack<*>

    /** Item template used to represent defined styles in the selector. */
    private val itemSelectStyle: TranslatedItemStack<*>

    /** Item used to cancel style selection and return to the previous menu. */
    private val itemCancel: TranslatedItemStack<*>

    init {
        val ctx = getContext()!!
        itemBlockConsoleActive =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockConsoleActive",
                Material.BARRIER,
                1,
                "Used to select active console block.",
            )
        itemBlockOriginActive =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockOriginActive",
                Material.BARRIER,
                1,
                "Used to select active origin block.",
            )
        itemBlockPortalActive =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockPortalActive",
                Material.BARRIER,
                1,
                "Used to select active portal area block. Defaults to end gateway if unset.",
            )
        itemBlockBoundary1Active =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockBoundary1Active",
                Material.BARRIER,
                1,
                "Used to select active boundary variant 1 block.",
            )
        itemBlockBoundary2Active =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockBoundary2Active",
                Material.BARRIER,
                1,
                "Used to select active boundary variant 2 block.",
            )
        itemBlockBoundary3Active =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockBoundary3Active",
                Material.BARRIER,
                1,
                "Used to select active boundary variant 3 block.",
            )
        itemBlockBoundary4Active =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockBoundary4Active",
                Material.BARRIER,
                1,
                "Used to select active boundary variant 4 block.",
            )
        itemBlockBoundary5Active =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockBoundary5Active",
                Material.BARRIER,
                1,
                "Used to select active boundary variant 5 block.",
            )
        itemBlockConsoleInactive =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockConsoleInactive",
                Material.BARRIER,
                1,
                "Used to select inactive console block.",
            )
        itemBlockOriginInactive =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockOriginInactive",
                Material.BARRIER,
                1,
                "Used to select inactive origin block.",
            )
        itemBlockPortalInactive =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockPortalInactive",
                Material.BARRIER,
                1,
                "Used to select inactive portal area block.",
            )
        itemBlockBoundary1Inactive =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockBoundary1Inactive",
                Material.BARRIER,
                1,
                "Used to select inactive boundary variant 1 block.",
            )
        itemBlockBoundary2Inactive =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockBoundary2Inactive",
                Material.BARRIER,
                1,
                "Used to select inactive boundary variant 2 block.",
            )
        itemBlockBoundary3Inactive =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockBoundary3Inactive",
                Material.BARRIER,
                1,
                "Used to select inactive boundary variant 3 block.",
            )
        itemBlockBoundary4Inactive =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockBoundary4Inactive",
                Material.BARRIER,
                1,
                "Used to select inactive boundary variant 4 block.",
            )
        itemBlockBoundary5Inactive =
            TranslatedItemStack<Portals?>(
                ctx,
                "BlockBoundary5Inactive",
                Material.BARRIER,
                1,
                "Used to select inactive boundary variant 5 block.",
            )

        itemAccept =
            TranslatedItemStack<Portals?>(
                ctx,
                "Accept",
                Material.LIME_TERRACOTTA,
                1,
                "Used to apply the style.",
            )
        itemReset =
            TranslatedItemStack<Portals?>(
                ctx,
                "Reset",
                Material.MILK_BUCKET,
                1,
                "Used to reset any changes.",
            )
        itemSelectDefined =
            TranslatedItemStack<Portals?>(
                ctx,
                "SelectDefined",
                Material.ITEM_FRAME,
                1,
                "Used to select a defined style from the configuration.",
            )
        itemSelectStyle =
            TranslatedItemStack<Portals?>(
                ctx,
                "SelectStyle",
                Material.ITEM_FRAME,
                1,
                "Used to represent a defined style in the selector menu.",
            )
        itemCancel =
            TranslatedItemStack<Portals?>(
                ctx,
                "Cancel",
                Material.RED_TERRACOTTA,
                1,
                "Used to abort style selection.",
            )
    }

    /**
     * Build and return the style selection [Menu] for the given portal and player.
     *
     * The menu allows editing the portal's style by selecting materials for each portal block type
     * (console, origin, portal area, boundaries) in active and inactive variants. The returned menu
     * will return to [previous] on natural close.
     *
     * @param portal Portal to edit.
     * @param player The player opening the menu (may be null for non-player contexts).
     * @param previous Previous menu to open when the style menu is closed.
     * @return A configured [Menu] instance for editing the provided portal's style.
     */
    @Suppress("UNUSED_PARAMETER")
    fun create(portal: Portal, player: Player?, previous: Menu): Menu {
        val title = langTitle!!.strComponent("§5§l" + portal.name())
        val styleMenu = Menu(getContext()!!, Bukkit.createInventory(null, 4 * COLUMNS, title))
        styleMenu.tag(PortalMenuTag(portal.id()))

        val styleContainer = StyleContainer()
        styleContainer.definedStyle = portal.style()
        styleContainer.style = portal.copyStyle(module!!, null)

        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                0,
                itemBlockConsoleInactive,
                module!!.constructor.configMaterialConsole!!,
                langSelectBlockConsoleInactiveTitle!!.str(),
                PortalBlock.Type.CONSOLE,
                false,
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                1,
                itemBlockOriginInactive,
                module!!.constructor.configMaterialOrigin!!,
                langSelectBlockOriginInactiveTitle!!.str(),
                PortalBlock.Type.ORIGIN,
                false,
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                2,
                itemBlockPortalInactive,
                module!!.constructor.configMaterialPortalArea!!,
                langSelectBlockPortalInactiveTitle!!.str(),
                PortalBlock.Type.PORTAL,
                false,
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                4,
                itemBlockBoundary1Inactive,
                module!!.constructor.configMaterialBoundary1!!,
                langSelectBlockBoundary1InactiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY1,
                false,
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                5,
                itemBlockBoundary2Inactive,
                module!!.constructor.configMaterialBoundary2!!,
                langSelectBlockBoundary2InactiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY2,
                false,
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                6,
                itemBlockBoundary3Inactive,
                module!!.constructor.configMaterialBoundary3!!,
                langSelectBlockBoundary3InactiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY3,
                false,
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                7,
                itemBlockBoundary4Inactive,
                module!!.constructor.configMaterialBoundary4!!,
                langSelectBlockBoundary4InactiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY4,
                false,
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                8,
                itemBlockBoundary5Inactive,
                module!!.constructor.configMaterialBoundary5!!,
                langSelectBlockBoundary5InactiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY5,
                false,
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS,
                itemBlockConsoleActive,
                module!!.constructor.configMaterialConsole!!,
                langSelectBlockConsoleActiveTitle!!.str(),
                PortalBlock.Type.CONSOLE,
                true,
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS + 1,
                itemBlockOriginActive,
                module!!.constructor.configMaterialOrigin!!,
                langSelectBlockOriginActiveTitle!!.str(),
                PortalBlock.Type.ORIGIN,
                true,
            )
        )
        // styleMenu.add(menuItemBlockSelector(portal, styleContainer, 1 * COLUMNS
        // + 2, itemBlockPortalActive,
        // module!!.constructor.configMaterialPortalArea,
        // langSelectBlockPortalActiveTitle.str(), PortalBlock.Type.PORTAL, true));
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS + 4,
                itemBlockBoundary1Active,
                module!!.constructor.configMaterialBoundary1!!,
                langSelectBlockBoundary1ActiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY1,
                true,
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS + 5,
                itemBlockBoundary2Active,
                module!!.constructor.configMaterialBoundary2!!,
                langSelectBlockBoundary2ActiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY2,
                true,
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS + 6,
                itemBlockBoundary3Active,
                module!!.constructor.configMaterialBoundary3!!,
                langSelectBlockBoundary3ActiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY3,
                true,
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS + 7,
                itemBlockBoundary4Active,
                module!!.constructor.configMaterialBoundary4!!,
                langSelectBlockBoundary4ActiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY4,
                true,
            )
        )
        styleMenu.add(
            menuItemBlockSelector(
                portal,
                styleContainer,
                COLUMNS + 8,
                itemBlockBoundary5Active,
                module!!.constructor.configMaterialBoundary5!!,
                langSelectBlockBoundary5ActiveTitle!!.str(),
                PortalBlock.Type.BOUNDARY5,
                true,
            )
        )

        styleMenu.add(menuItemAccept(portal, styleContainer, previous))
        styleMenu.add(menuItemReset(portal, styleContainer))
        styleMenu.add(menuItemSelectDefined(portal, styleContainer))
        styleMenu.add(menuItemCancel(previous))

        styleMenu.onNaturalClose { player2: Player? -> previous.open(player2!!) }
        return styleMenu
    }

    /**
     * Create a menu widget that opens an item selector for the given [type] of portal block.
     *
     * The selector will update [styleContainer] with the chosen material (or reset to a default)
     * and reopen the parent menu after selection. The [tItem] is used as the base item for display
     * in the menu and [slot] determines where it will be placed.
     *
     * @param portal Portal being edited.
     * @param styleContainer Container holding the currently edited style and defined-style key.
     * @param slot Slot index inside the menu inventory where the widget will be placed.
     * @param tItem Translated item template used to render the widget.
     * @param buildingMaterial Default building Material used to describe the slot when empty.
     * @param title Title shown in the item selector inventory.
     * @param type The [PortalBlock.Type] this selector edits.
     * @param active True to edit the active variant, false to edit the inactive variant.
     * @return A [MenuWidget] that opens an item selector for the specified block type.
     */
    private fun menuItemBlockSelector(
        portal: Portal,
        styleContainer: StyleContainer,
        slot: Int,
        tItem: TranslatedItemStack<*>,
        buildingMaterial: Material,
        title: String,
        type: PortalBlock.Type,
        active: Boolean,
    ): MenuWidget {
        return object :
            MenuItem(
                slot,
                null,
                Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
                    menu!!.close(player!!)
                    MenuFactory.itemSelector(
                            getContext()!!,
                            player,
                            title,
                            itemForType(styleContainer, active, type),
                            true,
                            { player2: Player?, item: ItemStack? ->
                                styleContainer.definedStyle = null
                                if (item == null) {
                                    if (active && type == PortalBlock.Type.PORTAL) {
                                        styleContainer.style!!.setMaterial(
                                            active,
                                            type,
                                            Material.END_GATEWAY,
                                            true,
                                        )
                                    }
                                    styleContainer.style!!.setMaterial(
                                        active,
                                        type,
                                        Material.AIR,
                                        true,
                                    )
                                } else {
                                    styleContainer.style!!.setMaterial(
                                        active,
                                        type,
                                        item.type,
                                        true,
                                    )
                                }
                                menu.open(player2!!)
                                Menu.ClickResult.SUCCESS
                            },
                            { player2: Player? -> menu.open(player2!!) },
                            { item: ItemStack? ->
                                // Only allow placeable solid blocks
                                if (item == null || !(item.type.isBlock && item.type.isSolid)) {
                                    return@itemSelector null
                                }

                                // Nothing from the blacklist
                                if (
                                    module!!.configBlacklistedMaterials?.contains(item.type) == true
                                ) {
                                    return@itemSelector null
                                }

                                // Must be a block (should be given at this point)
                                val block = item.type.asBlockType() ?: return@itemSelector null

                                // Must be a full block with one AABB of extent (1,1,1)
                                val blockdata = block.createBlockData()
                                val voxelshape = blockdata.getCollisionShape(player.location)
                                val bbs = voxelshape.boundingBoxes
                                if (
                                    bbs.size != 1 ||
                                        !bbs.all { x ->
                                            x.widthX == 1.0 && x.widthZ == 1.0 && x.height == 1.0
                                        }
                                ) {
                                    return@itemSelector null
                                }

                                // Always select one
                                item.amount = 1
                                item
                            },
                        )
                        .tag(PortalMenuTag(portal.id()))
                        .open(player)
                    menu.update()
                    Menu.ClickResult.SUCCESS
                },
            ) {
            override fun item(item: ItemStack?) {
                var stack: ItemStack = itemForType(styleContainer, active, type)
                if (stack.type == Material.AIR) {
                    stack = ItemStack(Material.BARRIER)
                }
                super.item(tItem.alternative(stack, "§6" + buildingMaterial.key))
            }
        }
    }

    /**
     * Create the accept/apply menu widget which applies the edited style to [portal].
     *
     * This will perform permission/event checks (fires [PortalChangeSettingsEvent]), apply the
     * style, update portal blocks and return to [previous].
     *
     * @param portal Portal to update.
     * @param styleContainer Container holding the edited style.
     * @param previous Menu to return to after applying changes.
     * @return A [MenuWidget] that applies the style on activation.
     */
    private fun menuItemAccept(
        portal: Portal,
        styleContainer: StyleContainer,
        previous: Menu,
    ): MenuWidget {
        return MenuItem(
            3 * COLUMNS,
            itemAccept.item(),
            Function3 { player: Player?, menu: Menu?, _: MenuItem? ->
                menu!!.close(player!!)
                val settingsEvent = PortalChangeSettingsEvent(player, portal, false)
                module!!.server.pluginManager.callEvent(settingsEvent)
                if (
                    settingsEvent.isCancelled() && !player.hasPermission(module!!.adminPermission)
                ) {
                    return@Function3 Menu.ClickResult.ERROR
                }

                val s = styleContainer.style ?: return@Function3 Menu.ClickResult.ERROR
                portal.style(s)
                portal.updateBlocks(module!!)
                previous.open(player)
                Menu.ClickResult.SUCCESS
            },
        )
    }

    /**
     * Create the reset menu widget which reverts unsaved style changes.
     *
     * When activated the style in [styleContainer] is reset to a copy of the portal's current style
     * and the menu display is updated.
     *
     * @param portal Portal whose style to copy when resetting.
     * @param styleContainer Container holding the edited style to be reset.
     * @return A [MenuWidget] that resets the working style to the portal's style.
     */
    private fun menuItemReset(portal: Portal, styleContainer: StyleContainer): MenuWidget {
        return MenuItem(3 * COLUMNS + 3, itemReset.item()) { _: Player?, menu: Menu?, _: MenuItem?
            ->
            styleContainer.style = portal.copyStyle(module!!, null)
            menu!!.update()
            Menu.ClickResult.SUCCESS
        }
    }

    /**
     * Create a menu widget that opens a selector for defined styles from configuration.
     *
     * Selecting a defined style will set the container's `definedStyle` and replace the working
     * `style` in the provided `styleContainer` with a copy of the selected style.
     *
     * @param portal Portal being edited.
     * @param styleContainer Container holding the edited style and selected defined key.
     * @return A [MenuWidget] that opens the defined-style selector.
     */
    private fun menuItemSelectDefined(portal: Portal, styleContainer: StyleContainer): MenuWidget {
        val itemFor: Function1<Style?, ItemStack?> = Function1 { style: Style? ->
            val mat = style!!.material(false, PortalBlock.Type.BOUNDARY1)
            if (mat == null) {
                return@Function1 ItemStack(Material.BARRIER)
            } else {
                return@Function1 ItemStack(mat)
            }
        }

        return MenuItem(
            3 * COLUMNS + 4,
            itemSelectDefined.item(),
        ) { player: Player?, menu: Menu?, _: MenuItem? ->
            menu!!.close(player!!)
            val allStyles: ArrayList<Style?> = ArrayList(module!!.styles.values)
            val filter =
                Filter.StringFilter({ s: Style?, str: String? ->
                    s!!.key().toString().lowercase(Locale.getDefault()).contains(str!!)
                })
            MenuFactory.genericSelector<Style?, Filter.StringFilter<Style?>?>(
                    getContext()!!,
                    player,
                    langSelectStyleTitle!!.str(),
                    langFilterStylesTitle!!.str(),
                    allStyles,
                    { s: Style? ->
                        itemSelectStyle.alternative(itemFor.apply(s)!!, s!!.key()!!.key)
                    },
                    filter,
                    { player2: Player?, m: Menu?, t: Style? ->
                        m!!.close(player2!!)
                        styleContainer.definedStyle = t!!.key()
                        styleContainer.style = t.copy(null)
                        menu.open(player2)
                        Menu.ClickResult.SUCCESS
                    },
                    { player2: Player? -> menu.open(player2!!) },
                )
                .tag(PortalMenuTag(portal.id()))
                .open(player)
            Menu.ClickResult.SUCCESS
        }
    }

    /**
     * Create the cancel/back menu widget which returns the player to [previous] without applying
     * changes.
     *
     * @param previous Menu to open when the cancel action is invoked.
     * @return A [MenuWidget] that cancels editing and returns to [previous].
     */
    private fun menuItemCancel(previous: Menu): MenuWidget {
        return MenuItem(3 * COLUMNS + 8, itemCancel.item()) {
            player: Player?,
            menu: Menu?,
            _: MenuItem? ->
            menu!!.close(player!!)
            previous.open(player)
            Menu.ClickResult.SUCCESS
        }
    }

    /** Called when this module component is enabled. No-op implementation. */
    public override fun onEnable() {}

    /** Called when this module component is disabled. No-op implementation. */
    public override fun onDisable() {}

    /**
     * Container used while editing a style in the menu.
     *
     * Holds an optional defined-style key and the working copy of the [Style].
     */
    private class StyleContainer {
        /** The NamespacedKey of a defined style when one is selected, otherwise null. */
        var definedStyle: NamespacedKey? = null

        /** The working copy of the style being edited; null when not set. */
        var style: Style? = null
    }

    companion object {
        private const val COLUMNS = 9

        /**
         * Get an ItemStack representing the material for the given [type] in [styleContainer].
         *
         * For the special case of an active PORTAL type this returns an AIR item stack to indicate
         * the portal-area selection is empty.
         *
         * @param styleContainer Container holding the working style.
         * @param active True for active variant, false for inactive.
         * @param type The portal block [PortalBlock.Type] to query.
         * @return An [ItemStack] that represents the material for the requested type/variant.
         */
        private fun itemForType(
            styleContainer: StyleContainer,
            active: Boolean,
            type: PortalBlock.Type,
        ): ItemStack {
            if (active && type == PortalBlock.Type.PORTAL) {
                return ItemStack(Material.AIR)
            }
            return ItemStack(styleContainer.style!!.material(active, type)!!)
        }
    }
}
