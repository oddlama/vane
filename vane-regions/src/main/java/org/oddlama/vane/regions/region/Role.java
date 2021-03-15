package org.oddlama.vane.regions.region;

import static org.oddlama.vane.core.persistent.PersistentSerializer.from_json;
import static org.oddlama.vane.core.persistent.PersistentSerializer.to_json;

import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;
import static org.oddlama.vane.util.BlockUtil.unpack;
import static org.oddlama.vane.util.ItemUtil.name_item;
import static org.oddlama.vane.util.Nms.item_handle;
import static org.oddlama.vane.util.Nms.register_entity;
import static org.oddlama.vane.util.Nms.spawn;
import static org.oddlama.vane.util.Util.ms_to_ticks;
import static org.oddlama.vane.util.Util.namespaced_key;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.EnumCreatureType;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigExtendedMaterial;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigMaterialMapEntry;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapEntry;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMap;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMapEntry;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.material.ExtendedMaterial;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.util.LazyBlock;
import org.oddlama.vane.regions.Regions;

import org.oddlama.vane.external.json.JSONObject;

public class Role {
	public enum RoleType {
		ADMINS,
		OTHERS,
		NORMAL;
	}

	public static Object serialize(@NotNull final Object o) throws IOException {
		final var role = (Role)o;
		final var json = new JSONObject();
		json.put("id",             to_json(UUID.class,              role.id));
		json.put("name",           to_json(String.class,            role.name));
		json.put("role_type",      to_json(RoleType.class,          role.role_type));
		try {
			json.put("settings",   to_json(Role.class.getDeclaredField("settings"), role.settings));
		} catch (NoSuchFieldException e) { throw new RuntimeException("Invalid field. This is a bug.", e); }

		return json;
	}

	@SuppressWarnings("unchecked")
	public static Role deserialize(@NotNull final Object o) throws IOException {
		final var json = (JSONObject)o;
		final var role = new Role();
		role.id             = from_json(UUID.class,              json.get("id"));
		role.name           = from_json(String.class,            json.get("name"));
		role.role_type      = from_json(RoleType.class,          json.get("role_type"));
		try {
			role.settings   = (Map<RoleSetting, Boolean>)from_json(Role.class.getDeclaredField("settings"), json.get("settings"));
		} catch (NoSuchFieldException e) { throw new RuntimeException("Invalid field. This is a bug.", e); }
		return role;
	}

	private UUID id;
	private String name;
	private RoleType role_type;
	private Map<RoleSetting, Boolean> settings = new HashMap<>();

	private Role() { }
	public Role(final String name, final RoleType role_type) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.role_type = role_type;
		for (final var rs : RoleSetting.values()) {
			this.settings.put(rs, rs.default_value(role_type == RoleType.ADMINS));
		}
	}

	public UUID id() { return id; }
	public String name() { return name; }
	public void name(final String name) { this.name = name; }
	public RoleType role_type() { return role_type; }
	public boolean get_setting(final RoleSetting setting) {
		return settings.getOrDefault(setting, setting.default_value(false));
	}
}
