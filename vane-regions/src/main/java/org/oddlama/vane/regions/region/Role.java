package org.oddlama.vane.regions.region;

import static org.oddlama.vane.core.persistent.PersistentSerializer.from_json;
import static org.oddlama.vane.core.persistent.PersistentSerializer.to_json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.oddlama.vane.regions.Regions;

public class Role {

	public enum RoleType {
		ADMINS,
		OTHERS,
		NORMAL,
	}

	public static Object serialize(@NotNull final Object o) throws IOException {
		final var role = (Role) o;
		final var json = new JSONObject();
		json.put("id", to_json(UUID.class, role.id));
		json.put("name", to_json(String.class, role.name));
		json.put("role_type", to_json(RoleType.class, role.role_type));
		try {
			json.put("settings", to_json(Role.class.getDeclaredField("settings"), role.settings));
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Invalid field. This is a bug.", e);
		}

		return json;
	}

	@SuppressWarnings("unchecked")
	public static Role deserialize(@NotNull final Object o) throws IOException {
		final var json = (JSONObject) o;
		final var role = new Role();
		role.id = from_json(UUID.class, json.get("id"));
		role.name = from_json(String.class, json.get("name"));
		role.role_type = from_json(RoleType.class, json.get("role_type"));
		try {
			role.settings =
				(Map<RoleSetting, Boolean>) from_json(Role.class.getDeclaredField("settings"), json.get("settings"));
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Invalid field. This is a bug.", e);
		}
		return role;
	}

	private UUID id;
	private String name;
	private RoleType role_type;
	private Map<RoleSetting, Boolean> settings = new HashMap<>();

	private Role() {}

	public Role(final String name, final RoleType role_type) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.role_type = role_type;
		for (final var rs : RoleSetting.values()) {
			this.settings.put(rs, rs.default_value(role_type == RoleType.ADMINS));
		}
	}

	public UUID id() {
		return id;
	}

	public String name() {
		return name;
	}

	public void name(final String name) {
		this.name = name;
	}

	public RoleType role_type() {
		return role_type;
	}

	public Map<RoleSetting, Boolean> settings() {
		return settings;
	}

	public boolean get_setting(final RoleSetting setting) {
		if (setting.has_override()) {
			return setting.get_override() == 1;
		}
		return settings.getOrDefault(setting, setting.default_value(false));
	}

	public String color() {
		switch (role_type) {
			case ADMINS:
				return "§c";
			case OTHERS:
				return "§a";
			default:
			case NORMAL:
				return "§b";
		}
	}
}
