package org.oddlama.vane.regions.region

import java.io.IOException
import java.util.*
import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer

/** Role definition containing per-setting permissions inside a region group. */
class Role {
    /** Built-in role categories with special semantics. */
    enum class RoleType {
        ADMINS,
        OTHERS,
        NORMAL,
    }

    /** Unique role identifier. */
    private var id: UUID? = null

    /** Role display name. */
    private var name: String? = null

    /** Category of this role. */
    private var roleType: RoleType? = null

    /** Explicit permission values configured for this role. */
    private var settings: MutableMap<RoleSetting?, Boolean?>? =
        EnumMap<RoleSetting, Boolean?>(RoleSetting::class.java)

    /** Non-null accessor for the settings map. */
    private val settingsMap: MutableMap<RoleSetting?, Boolean?>
        get() = requireNotNull(settings)

    private constructor()

    /** Creates a role with defaults based on its role type. */
    constructor(name: String?, roleType: RoleType?) {
        this.id = UUID.randomUUID()
        this.name = name
        this.roleType = roleType
        for (rs in RoleSetting.entries) {
            settingsMap[rs] = rs.defaultValue(roleType == RoleType.ADMINS)
        }
    }

    /** Returns this role id. */
    fun id(): UUID? = id

    /** Returns this role name. */
    fun name(): String? = name

    /** Updates this role name. */
    fun name(name: String?) {
        this.name = name
    }

    /** Returns this role category. */
    fun roleType(): RoleType? = roleType

    /** Returns the mutable settings map. */
    fun settings(): MutableMap<RoleSetting?, Boolean?>? = settings

    /** Returns the effective value for a role setting, including global overrides. */
    fun getSetting(setting: RoleSetting): Boolean {
        if (setting.hasOverride()) {
            return setting.override == 1
        }
        return settingsMap[setting] ?: setting.defaultValue(false)
    }

    /** Returns the menu/chat color prefix for this role type. */
    fun color(): String =
        when (roleType) {
            RoleType.ADMINS -> "§c"
            RoleType.OTHERS -> "§a"
            RoleType.NORMAL -> "§b"
            else -> "§b"
        }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        /** Serializes a role to JSON-compatible data. */
        fun serialize(o: Any): Any {
            val role = o as Role
            val json = JSONObject()
            putSerialized(json, "id", UUID::class.java, role.id)
            putSerialized(json, "name", String::class.java, role.name)
            putSerialized(json, "roleType", RoleType::class.java, role.roleType)
            withField {
                json.put(
                    "settings",
                    PersistentSerializer.toJson(
                        Role::class.java.getDeclaredField("settings"),
                        role.settings,
                    ),
                )
            }

            return json
        }

        @JvmStatic
        @Throws(IOException::class)
        /** Deserializes a role from JSON-compatible data. */
        fun deserialize(o: Any): Role {
            val json = o as JSONObject
            val role = Role()
            role.id = readSerialized(json, "id", UUID::class.java)
            role.name = readSerialized(json, "name", String::class.java)
            role.roleType = readSerialized(json, "roleType", RoleType::class.java)
            withField {
                role.settings =
                    @Suppress("UNCHECKED_CAST")
                    (PersistentSerializer.fromJson(
                        Role::class.java.getDeclaredField("settings"),
                        json.get("settings"),
                    ) as MutableMap<RoleSetting?, Boolean?>?)
            }
            return role
        }
    }
}
