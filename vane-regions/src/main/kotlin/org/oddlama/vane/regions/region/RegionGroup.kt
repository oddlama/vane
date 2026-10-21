package org.oddlama.vane.regions.region

import java.io.IOException
import java.util.*
import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer
import org.oddlama.vane.regions.Regions

/** Region group containing roles, member assignments, and environment settings. */
class RegionGroup {
    /** Unique region-group identifier. */
    private var id: UUID? = null

    /** Region-group display name. */
    private var name: String? = null

    /** Owner player UUID. */
    private var owner: UUID? = null

    /** Roles indexed by role id. */
    private var roles: MutableMap<UUID?, Role?>? = HashMap()

    /** Player-to-role assignment map. */
    private var playerToRole: MutableMap<UUID?, UUID?>? = HashMap()

    /** Fallback role id used for unassigned players. */
    private var roleOthers: UUID? = null

    /** Environment settings configured for this region group. */
    private var settings: MutableMap<EnvironmentSetting?, Boolean?>? =
        EnumMap<EnvironmentSetting, Boolean?>(EnvironmentSetting::class.java)

    /** Non-null accessor for role storage. */
    private val rolesMap: MutableMap<UUID?, Role?>
        get() = requireNotNull(roles)

    /** Non-null accessor for player-role assignments. */
    private val playerRoles: MutableMap<UUID?, UUID?>
        get() = requireNotNull(playerToRole)

    /** Non-null accessor for environment setting values. */
    private val settingsMap: MutableMap<EnvironmentSetting?, Boolean?>
        get() = requireNotNull(settings)

    private constructor()

    /** Creates a new region group with default built-in roles and settings. */
    constructor(name: String?, owner: UUID?) {
        this.id = UUID.randomUUID()
        this.name = name
        this.owner = owner

        // Add admins role
        /** Built-in admins role. */
        val admins = Role("[Admins]", Role.RoleType.ADMINS)
        this.addRole(admins)

        // Add another role
        /** Built-in fallback role. */
        val others = Role("[Others]", Role.RoleType.OTHERS)
        this.addRole(others)
        this.roleOthers = others.id()

        // Add "friends" role
        /** Built-in friends role. */
        val friends = Role("Friends", Role.RoleType.NORMAL)

        /** Mutable settings map for the friends role. */
        val friendSettings = requireNotNull(friends.settings())
        friendSettings[RoleSetting.BUILD] = true
        friendSettings[RoleSetting.USE] = true
        friendSettings[RoleSetting.CONTAINER] = true
        friendSettings[RoleSetting.PORTAL] = true
        this.addRole(friends)

        // Add "owner" to admins
        playerRoles[owner] = admins.id()

        // Set setting defaults
        for (es in EnvironmentSetting.entries) {
            settingsMap[es] = es.defaultValue()
        }
    }

    /** Returns this region-group id. */
    fun id(): UUID? = id

    /** Returns this region-group name. */
    fun name(): String? = name

    /** Updates this region-group name. */
    fun name(name: String?) {
        this.name = name
    }

    /** Returns owner UUID. */
    fun owner(): UUID? = owner

    /** Returns mutable environment settings. */
    fun settings(): MutableMap<EnvironmentSetting?, Boolean?>? = settings

    /** Returns effective environment setting value, including global overrides. */
    fun getSetting(setting: EnvironmentSetting): Boolean {
        if (setting.hasOverride()) {
            return setting.override == 1
        }
        return settingsMap[setting] ?: setting.defaultValue()
    }

    /** Adds a role to this group. */
    fun addRole(role: Role) {
        rolesMap[role.id()] = role
    }

    /** Returns mutable player-to-role assignment map. */
    fun playerToRole(): MutableMap<UUID?, UUID?>? = playerToRole

    /** Returns effective role for a player, or fallback role when unassigned. */
    fun getRole(player: UUID?): Role? = rolesMap[playerRoles[player] ?: roleOthers]

    /** Removes a role and clears assignments that reference it. */
    fun removeRole(roleId: UUID) {
        playerRoles.values.removeIf { roleId == it }
        rolesMap.remove(roleId)
    }

    /** Returns all roles in this group. */
    fun roles(): MutableCollection<Role?> = rolesMap.values

    /** Returns whether no region currently references this group. */
    fun isOrphan(regions: Regions): Boolean =
        regions.allRegions().none { it?.regionGroupId() == id }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        /** Serializes a region group to JSON-compatible data. */
        fun serialize(o: Any): Any {
            val regionGroup = o as RegionGroup
            val json = JSONObject()
            putOwnable(json, regionGroup.id, regionGroup.name, regionGroup.owner)
            withField {
                json.put(
                    "roles",
                    PersistentSerializer.toJson(
                        RegionGroup::class.java.getDeclaredField("roles"),
                        regionGroup.roles,
                    ),
                )
            }
            withField {
                json.put(
                    "playerToRole",
                    PersistentSerializer.toJson(
                        RegionGroup::class.java.getDeclaredField("playerToRole"),
                        regionGroup.playerToRole,
                    ),
                )
            }
            putSerialized(json, "roleOthers", UUID::class.java, regionGroup.roleOthers)
            withField {
                json.put(
                    "settings",
                    PersistentSerializer.toJson(
                        RegionGroup::class.java.getDeclaredField("settings"),
                        regionGroup.settings,
                    ),
                )
            }
            return json
        }

        @JvmStatic
        @Throws(IOException::class)
        /** Deserializes a region group from JSON-compatible data. */
        fun deserialize(o: Any): RegionGroup {
            val json = o as JSONObject
            val regionGroup = RegionGroup()
            val (id, name, owner) = readOwnable(json)
            regionGroup.id = id
            regionGroup.name = name
            regionGroup.owner = owner
            withField {
                @Suppress("UNCHECKED_CAST")
                regionGroup.roles =
                    PersistentSerializer.fromJson(
                        RegionGroup::class.java.getDeclaredField("roles"),
                        json.get("roles"),
                    ) as MutableMap<UUID?, Role?>?
            }
            withField {
                @Suppress("UNCHECKED_CAST")
                regionGroup.playerToRole =
                    PersistentSerializer.fromJson(
                        RegionGroup::class.java.getDeclaredField("playerToRole"),
                        json.get("playerToRole"),
                    ) as MutableMap<UUID?, UUID?>?
            }
            regionGroup.roleOthers = readSerialized(json, "roleOthers", UUID::class.java)
            withField {
                @Suppress("UNCHECKED_CAST")
                regionGroup.settings =
                    PersistentSerializer.fromJson(
                        RegionGroup::class.java.getDeclaredField("settings"),
                        json.get("settings"),
                    ) as MutableMap<EnvironmentSetting?, Boolean?>?
            }
            return regionGroup
        }
    }
}
