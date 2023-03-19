package org.oddlama.vane.regions.region;

import static org.oddlama.vane.core.persistent.PersistentSerializer.from_json;
import static org.oddlama.vane.core.persistent.PersistentSerializer.to_json;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.oddlama.vane.regions.Regions;

public class RegionGroup {

	public static Object serialize(@NotNull final Object o) throws IOException {
		final var region_group = (RegionGroup) o;
		final var json = new JSONObject();
		json.put("id", to_json(UUID.class, region_group.id));
		json.put("name", to_json(String.class, region_group.name));
		json.put("owner", to_json(UUID.class, region_group.owner));
		try {
			json.put("roles", to_json(RegionGroup.class.getDeclaredField("roles"), region_group.roles));
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Invalid field. This is a bug.", e);
		}
		try {
			json.put(
				"player_to_role",
				to_json(RegionGroup.class.getDeclaredField("player_to_role"), region_group.player_to_role)
			);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Invalid field. This is a bug.", e);
		}
		json.put("role_others", to_json(UUID.class, region_group.role_others));
		try {
			json.put("settings", to_json(RegionGroup.class.getDeclaredField("settings"), region_group.settings));
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Invalid field. This is a bug.", e);
		}

		return json;
	}

	@SuppressWarnings("unchecked")
	public static RegionGroup deserialize(@NotNull final Object o) throws IOException {
		final var json = (JSONObject) o;
		final var region_group = new RegionGroup();
		region_group.id = from_json(UUID.class, json.get("id"));
		region_group.name = from_json(String.class, json.get("name"));
		region_group.owner = from_json(UUID.class, json.get("owner"));
		try {
			region_group.roles =
				(Map<UUID, Role>) from_json(RegionGroup.class.getDeclaredField("roles"), json.get("roles"));
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Invalid field. This is a bug.", e);
		}
		try {
			region_group.player_to_role =
				(Map<UUID, UUID>) from_json(
					RegionGroup.class.getDeclaredField("player_to_role"),
					json.get("player_to_role")
				);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Invalid field. This is a bug.", e);
		}
		region_group.role_others = from_json(UUID.class, json.get("role_others"));
		try {
			region_group.settings =
				(Map<EnvironmentSetting, Boolean>) from_json(
					RegionGroup.class.getDeclaredField("settings"),
					json.get("settings")
				);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Invalid field. This is a bug.", e);
		}
		return region_group;
	}

	private UUID id;
	private String name;
	private UUID owner;

	private Map<UUID, Role> roles = new HashMap<>();
	private Map<UUID, UUID> player_to_role = new HashMap<>();
	private UUID role_others;

	private Map<EnvironmentSetting, Boolean> settings = new HashMap<>();

	private RegionGroup() {}

	public RegionGroup(final String name, final UUID owner) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.owner = owner;

		// Add admins role
		final var admins = new Role("[Admins]", Role.RoleType.ADMINS);
		this.add_role(admins);

		// Add others role
		final var others = new Role("[Others]", Role.RoleType.OTHERS);
		this.add_role(others);
		this.role_others = others.id();

		// Add friends role
		final var friends = new Role("Friends", Role.RoleType.NORMAL);
		friends.settings().put(RoleSetting.BUILD, true);
		friends.settings().put(RoleSetting.USE, true);
		friends.settings().put(RoleSetting.CONTAINER, true);
		friends.settings().put(RoleSetting.PORTAL, true);
		this.add_role(friends);

		// Add owner to admins
		this.player_to_role.put(owner, admins.id());

		// Set setting defaults
		for (var es : EnvironmentSetting.values()) {
			this.settings.put(es, es.default_value());
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

	public UUID owner() {
		return owner;
	}

	public Map<EnvironmentSetting, Boolean> settings() {
		return settings;
	}

	public boolean get_setting(final EnvironmentSetting setting) {
		if (setting.has_override()) {
			return setting.get_override() == 1;
		}
		return settings.getOrDefault(setting, setting.default_value());
	}

	public void add_role(final Role role) {
		this.roles.put(role.id(), role);
	}

	public Map<UUID, UUID> player_to_role() {
		return player_to_role;
	}

	public Role get_role(final UUID player) {
		return roles.get(player_to_role.getOrDefault(player, role_others));
	}

	public void remove_role(final UUID role_id) {
		player_to_role.values().removeIf(r -> role_id.equals(r));
		roles.remove(role_id);
	}

	public Collection<Role> roles() {
		return roles.values();
	}

	public boolean is_orphan(final Regions regions) {
		return !regions.all_regions().stream().anyMatch(r -> id.equals(r.region_group_id()));
	}
}
