package org.oddlama.vane.util;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;

public class LazyBlock {

    private final UUID world_id;
    private int x;
    private int y;
    private int z;
    private Block block;

    public LazyBlock(final Block block) {
        if (block == null) {
            this.world_id = null;
            this.x = 0;
            this.y = 0;
            this.z = 0;
        } else {
            this.world_id = block.getWorld().getUID();
            this.x = block.getX();
            this.y = block.getY();
            this.z = block.getZ();
        }
        this.block = block;
    }

    public LazyBlock(final UUID world_id, int x, int y, int z) {
        this.world_id = world_id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = null;
    }

    public UUID world_id() {
        return world_id;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public Block block() {
        if (world_id != null && block == null) {
            this.block = Bukkit.getWorld(world_id).getBlockAt(x, y, z);
        }

        return block;
    }
}
