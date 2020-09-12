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
	private static Object serialize(@NotNull final Object o) throws IOException {
		final var portal_block = (PortalBlock)o;
		final var json = new JSONObject();
		json.put("portal_id", to_json(UUID.class,   portal_block.portal_id));
		json.put("block",     to_json(Block.class,  portal_block.block));
		json.put("type",      to_json(String.class, portal_block.type.name()));
		return json;
	}

	private static PortalBlock deserialize(@NotNull final Object o) throws IOException {
		final var json = (JSONObject)o;
		final var portal_id = from_json(UUID.class,  json.get("portal_id"));
		final var block     = from_json(Block.class, json.get("block"));
		final var type      = Type.valueOf(from_json(String.class, json.get("type")));
		return new PortalBlock(portal_id, block, type);
	}

	// Add (de-)serializer
	static {
		PersistentSerializer.serializers.put(PortalBlock.class,   PortalBlock::serialize);
		PersistentSerializer.deserializers.put(PortalBlock.class, PortalBlock::deserialize);
	}

	private UUID portal_id;
	private Block block;
	private Type type;

	public PortalBlock(final UUID portal_id, final Block block, final Type type) {
		this.portal_id = portal_id;
		this.block = block;
		this.type = type;
	}

	public UUID portal_id() { return portal_id; }
	public Block block() { return block; }
	public Type type() { return type; }

	public static enum Type {
		ORIGIN,
		CONSOLE,
		BOUNDARY,
		PORTAL;
	}
}
