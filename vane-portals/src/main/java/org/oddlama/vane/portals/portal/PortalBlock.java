package org.oddlama.vane.portals.portal;
import org.oddlama.vane.portals.PortalConstructor;
import org.oddlama.vane.portals.Portals;

import java.io.IOException;
import static org.oddlama.vane.core.persistent.PersistentSerializer.from_json;
import static org.oddlama.vane.core.persistent.PersistentSerializer.to_json;

import java.util.UUID;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.external.json.JSONObject;

public class PortalBlock {
	public static Object serialize(@NotNull final Object o) throws IOException {
		final var portal_block = (PortalBlock)o;
		final var json = new JSONObject();
		json.put("block", to_json(Block.class,            portal_block.block));
		json.put("type",  to_json(PortalBlock.Type.class, portal_block.type));
		return json;
	}

	public static PortalBlock deserialize(@NotNull final Object o) throws IOException {
		final var json = (JSONObject)o;
		final var block = from_json(Block.class,            json.get("block"));
		final var type  = from_json(PortalBlock.Type.class, json.get("type"));
		return new PortalBlock(block, type);
	}

	private Block block;
	private Type type;

	public PortalBlock(final Block block, final Type type) {
		this.block = block;
		this.type = type;
	}

	public Block block() { return block; }
	public Type type() { return type; }

	public PortalBlockLookup lookup(final UUID portal_id) {
		return new PortalBlockLookup(portal_id, type);
	}

	@Override
	public int hashCode() {
		return block.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof PortalBlock)) {
			return false;
		}

		final var po = (PortalBlock)other;
		// Only block is compared, as the same block can only have one functions.
		return block.equals(po.block);
	}

	public static enum Type {
		ORIGIN,
		CONSOLE,
		BOUNDARY,
		PORTAL;
	}
}
