package org.oddlama.vane.trifles;

import io.papermc.paper.block.TileStateInventoryHolder;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.StorageUtil;

import java.util.Objects;

public class ItemFinder extends Listener<Trifles> {

    public static final NamespacedKey IS_ENTITY_FOUND = StorageUtil.namespaced_key(
            "vane_trifles",
            "is_entity_found"
    );

    public static final NamespacedKey FOUND_TASK_ID = StorageUtil.namespaced_key(
            "vane_trifles",
            "found_task_id"
    );

    private final static long ACTIVE_GLOW_TIME = 20 * 10L; // 10 seconds
    private final static float CONTAINER_DEFAULT_SCALE = 0.96f;
    private final static float CONTAINER_DEFAULT_TRANSLATION = 0.02f;
    private final static int X_COMPONENT = 0;
    private final static int Z_COMPONENT = 2;
    private final static float EPSILON = 0.5f;
    private final static float CHEST_DEFAULT_SCALE = 0.8f;
    private final static float CHEST_DEFAULT_TRANSLATION = 0.1f;
    private final static float CHEST_Y_TRANSLATION = 0.05f;
    private final static float DOUBLE_CHEST_SCALE = 1.8f;
    private final static float DOUBLE_CHEST_TRANSLATION_OFFSET = -0.9f;
    private final static AxisAngle4f BLANK_ROTATION_DATA = new AxisAngle4f();

    @ConfigInt(
        def = 2,
        min = 1,
        max = 10,
        desc = "The radius of chunks in which containers (and possibly entities) are checked for matching items."
    )
    public int config_radius;

    @ConfigBoolean(def = true, desc = "Also search entities such as players, mobs, minecarts, ...")
    public boolean config_search_entities;

    @ConfigBoolean(
        def = false,
        desc = "Only allow players to use the shift+rightclick shortcut when they have the shortcut permission `vane.trifles.use_item_find_shortcut`."
    )
    public boolean config_require_permission;

    // This permission allows players to use the shift+rightclick.
    public final Permission use_item_find_shortcut_permission;

    public ItemFinder(Context<Trifles> context) {
        super(
            context.group(
                "item_finder",
                "Enables players to search for items in nearby containers by either shift-right-clicking a similar item in their inventory or by using the `/finditem <item>` command."
            )
        );
        // Register admin permission
        use_item_find_shortcut_permission = new Permission(
            "vane." + get_module().get_name() + ".use_item_find_shortcut",
            "Allows a player to use shfit+rightclick to search for items if the require_permission config is set",
            PermissionDefault.FALSE
        );
        get_module().register_permission(use_item_find_shortcut_permission);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void on_player_click_inventory(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (config_require_permission && !player.hasPermission(use_item_find_shortcut_permission)) {
            return;
        }

        final var item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // Shift-rightclick
        if (
            !(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.getClick() == ClickType.SHIFT_RIGHT)
        ) {
            return;
        }

        event.setCancelled(true);
        if (find_item(player, item.getType())) {
            get_module().schedule_next_tick(player::closeInventory);
        }
    }

    private boolean is_container(final Block block) {
        return block.getState() instanceof Container;
    }

    // All the containers besides chests
    private void indicate_container_match_at(@NotNull Player player, @NotNull Location location, @NotNull BlockData data) {
        remove_existing_glow_entities(location);
        var display = player.getWorld().spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(data);
            entity.setTransformation(
                new Transformation(
                        new Vector3f(CONTAINER_DEFAULT_TRANSLATION, CONTAINER_DEFAULT_TRANSLATION, CONTAINER_DEFAULT_TRANSLATION),
                        BLANK_ROTATION_DATA,
                        new Vector3f(CONTAINER_DEFAULT_SCALE, CONTAINER_DEFAULT_SCALE, CONTAINER_DEFAULT_SCALE),
                        BLANK_ROTATION_DATA
                )
            );
            entity.setGlowing(true);
            entity.setGlowColorOverride(Color.WHITE);
            entity.setPersistent(false); // Marked non-persistent so it will be removed after server restart in case it is not killed
            entity.getPersistentDataContainer().set(IS_ENTITY_FOUND, PersistentDataType.BOOLEAN, true);
        });

        get_module().getServer().getScheduler().runTaskLater(get_module(), new KillEntityTask(display), ACTIVE_GLOW_TIME);
    }

    // Special function for chests
    private void indicate_chest_match_at(@NotNull Player player, @NotNull Container container) {
        remove_existing_glow_entities(container.getLocation());
        var scale_vector = new Vector3f(CHEST_DEFAULT_SCALE, CHEST_DEFAULT_SCALE, CHEST_DEFAULT_SCALE); // Defaults for single chest
        var translation_vector = new Vector3f(CHEST_DEFAULT_TRANSLATION, CHEST_Y_TRANSLATION, CHEST_DEFAULT_TRANSLATION); // Defaults for single chest

        // Scale calculations for a double chest
        if (container.getInventory() instanceof DoubleChestInventory) {
            var left_side = ((DoubleChestInventory) container.getInventory()).getLeftSide();
            var right_side = ((DoubleChestInventory) container.getInventory()).getRightSide();
            if (Objects.equals(right_side.getLocation(), container.getLocation())) {
                return; // Only do work on left sides to avoid spawning 2 entities
            }
            var left_x = left_side.getLocation().getX();
            var left_z = left_side.getLocation().getZ();
            var right_x = right_side.getLocation().getX();
            var right_z = right_side.getLocation().getZ();

            if (right_x - left_x > EPSILON) { // facing south
                scale_vector.setComponent(X_COMPONENT, DOUBLE_CHEST_SCALE);
            } else if (left_x - right_x > EPSILON) { // facing north
                scale_vector.setComponent(X_COMPONENT, DOUBLE_CHEST_SCALE);
                translation_vector.setComponent(0, DOUBLE_CHEST_TRANSLATION_OFFSET);
            } else if (right_z - left_z > EPSILON) { // facing east
                scale_vector.setComponent(Z_COMPONENT, DOUBLE_CHEST_SCALE);
            } else if (left_z - right_z > EPSILON) { // facing west
                scale_vector.setComponent(Z_COMPONENT, DOUBLE_CHEST_SCALE);
                translation_vector.setComponent(Z_COMPONENT, DOUBLE_CHEST_TRANSLATION_OFFSET);
            }
        }
        var display = player.getWorld().spawn(container.getLocation(), BlockDisplay.class, entity -> {
            entity.setBlock(Material.BLACK_CONCRETE.createBlockData());
            entity.setTransformation(
                    new Transformation(
                            translation_vector,
                            BLANK_ROTATION_DATA,
                            scale_vector,
                            BLANK_ROTATION_DATA
                    )
            );
            entity.setGlowing(true);
            entity.setGlowColorOverride(Color.WHITE);
            entity.setPersistent(false); // Marked non-persistent so it will be removed after server restart in case it is not killed
            entity.getPersistentDataContainer().set(IS_ENTITY_FOUND, PersistentDataType.BOOLEAN, true);
        });
        get_module().getServer().getScheduler().runTaskLater(get_module(), new KillEntityTask(display), ACTIVE_GLOW_TIME);
    }

    // Remove the glow when the player opens the glowing block inventory
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void on_interact_event(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        if (!(event.getClickedBlock().getState() instanceof TileStateInventoryHolder)) {
            return;
        }
        var checked_location = event.getClickedBlock().getLocation();
        // Stupid double chest check shenanigans
        if (((TileStateInventoryHolder) event.getClickedBlock().getState()).getInventory() instanceof DoubleChestInventory) {
            checked_location = ((DoubleChestInventory) ((TileStateInventoryHolder) event.getClickedBlock().getState()).getInventory()).getLeftSide().getLocation();
        }
        if (checked_location != null) {
            remove_existing_glow_entities(checked_location);
        }
    }
    // Remove the glow when the player opens the glowing entity inventory
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void on_interact_event(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getPersistentDataContainer().has(IS_ENTITY_FOUND)) {
            event.getRightClicked().setGlowing(false);
        }
    }

    // Entities like chest minecarts
    private void indicate_entity_match_at(@NotNull Player player, @NotNull Entity entity) {
        // Make sure we don't cut the glow time short if the command is run twice in a row
        if (entity.getPersistentDataContainer().has(FOUND_TASK_ID)) {
            var task_id = entity.getPersistentDataContainer().get(FOUND_TASK_ID, PersistentDataType.INTEGER);
            if (task_id != null) {
                get_module().getServer().getScheduler().cancelTask(task_id);
            }
        }
        entity.setGlowing(true);
        entity.getPersistentDataContainer().set(IS_ENTITY_FOUND, PersistentDataType.BOOLEAN, true);

        var task = get_module().getServer().getScheduler().runTaskLater(get_module(), new UnsetGlowingTask(entity), ACTIVE_GLOW_TIME);
        entity.getPersistentDataContainer().set(FOUND_TASK_ID, PersistentDataType.INTEGER, task.getTaskId());
    }

    // Handle bugged glowing entities (minecart, boats, etc) on load
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void on_load_entities(EntitiesLoadEvent event) {
        for (var entity : event.getEntities()) {
            if (entity.isGlowing()) {
                if (entity.getPersistentDataContainer().has(IS_ENTITY_FOUND, PersistentDataType.BOOLEAN)) {
                    entity.setGlowing(false);
                    entity.getPersistentDataContainer().remove(IS_ENTITY_FOUND);
                }
                if (entity.getPersistentDataContainer().has(FOUND_TASK_ID, PersistentDataType.BOOLEAN)) {
                    entity.setGlowing(false);
                    entity.getPersistentDataContainer().remove(FOUND_TASK_ID);
                }
            }
        }
    }

    // Living entities
    private void indicate_living_entity_match_at(@NotNull Player player, @NotNull LivingEntity entity) {
        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int) ACTIVE_GLOW_TIME, 0, true, false, false));
    }

    private void remove_existing_glow_entities(Location location) {
        // Make sure we aren't creating multiple entities on top of each other with multiple commands ran
        var entities = location.getNearbyEntities(1f, 1f, 1f);
        for (Entity entity : entities) {
            if (entity.getPersistentDataContainer().has(IS_ENTITY_FOUND)) {
                entity.remove();
            }
        }
    }

    public boolean find_item(@NotNull final Player player, @NotNull final Material material) {
        // Find chests in configured radius and sort them.
        boolean any_found = false;
        final var world = player.getWorld();
        final var origin_chunk = player.getChunk();
        for (int cx = origin_chunk.getX() - config_radius; cx <= origin_chunk.getX() + config_radius; ++cx) {
            for (int cz = origin_chunk.getZ() - config_radius; cz <= origin_chunk.getZ() + config_radius; ++cz) {
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                final var chunk = world.getChunkAt(cx, cz);
                for (final var tile_entity : chunk.getTileEntities(this::is_container, false)) {
                    if (tile_entity instanceof Container container) {
                        if (container.getInventory().contains(material)) {
                            if (container.getType() == Material.CHEST) {
                                indicate_chest_match_at(player, container);
                            } else {
                                indicate_container_match_at(player, container.getLocation(), container.getBlockData());
                            }
                            any_found = true;
                        }
                    }
                }
                if (config_search_entities) {
                    for (final var entity : chunk.getEntities()) {
                        // Don't indicate the player
                        if (entity == player) {
                            continue;
                        }

                        if (entity instanceof InventoryHolder holder) {
                            if (holder.getInventory().contains(material)) {
                                if (entity instanceof LivingEntity living_entity) {
                                    indicate_living_entity_match_at(player, living_entity);
                                } else {
                                    indicate_entity_match_at(player, entity);
                                }
                                any_found = true;
                            }
                        }
                    }
                }
            }
        }

        if (any_found) {
            player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.MASTER, 1.0f, 1.3f);
        } else {
            player.playSound(player, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 5.0f);
        }

        return any_found;
    }
}

class KillEntityTask implements Runnable {
    private final BlockDisplay display;

    public KillEntityTask(BlockDisplay display) {
        this.display = display;
    }

    @Override
    public void run() {
        if (display.isValid()) {
            display.remove();
        }
    }
}

class UnsetGlowingTask implements Runnable {
    private final Entity entity;

    public UnsetGlowingTask(Entity entity) {
        this.entity = entity;
    }

    @Override
    public void run() {
        if (entity.isValid()) {
            entity.setGlowing(false);
        }
    }
}