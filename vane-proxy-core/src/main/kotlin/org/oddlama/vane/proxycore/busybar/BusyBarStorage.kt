package org.oddlama.vane.proxycore.busybar

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

/**
 * What the proxy remembers about BUSY Bar players across restarts, kept in `busybar.json`: token
 * hashes, focus mode, the grants backends last reported, names, and the backend each player was
 * last on. Every change is written at once. All methods are thread-safe.
 *
 * @param file where the state is kept.
 */
class BusyBarStorage(private val file: File) {
    private val tokens = mutableMapOf<UUID, String>()
    private val busy = mutableSetOf<UUID>()
    private val grants = mutableMapOf<UUID, List<String>>()
    private val names = mutableMapOf<UUID, String>()
    private val servers = mutableMapOf<UUID, String>()

    /** Reads the file, keeping the current state when it does not exist. */
    @Synchronized
    @Throws(IOException::class)
    fun load() {
        if (!file.exists()) return
        val json = try {
            JSONObject(file.readText())
        } catch (e: JSONException) {
            throw IOException("${file.name} is not valid JSON: ${e.message}", e)
        }
        tokens.clear()
        busy.clear()
        grants.clear()
        names.clear()
        servers.clear()
        json.optJSONObject("tokens")?.let { readMap(it, tokens) { value -> value as? String } }
        json.optJSONArray("busy")?.let { a -> (0 until a.length()).mapNotNullTo(busy) { uuid(a.optString(it)) } }
        json.optJSONObject("grants")?.let {
            readMap(it, grants) { value -> (value as? JSONArray)?.let { a -> (0 until a.length()).map(a::optString) } }
        }
        json.optJSONObject("names")?.let { readMap(it, names) { value -> value as? String } }
        json.optJSONObject("servers")?.let { readMap(it, servers) { value -> value as? String } }
    }

    @Synchronized
    fun tokenHashes(): Map<UUID, String> = tokens.toMap()

    @Synchronized
    fun setTokenHash(player: UUID, hash: String?) = change { if (hash == null) tokens.remove(player) else tokens[player] = hash }

    @Synchronized
    fun busyPlayers(): Set<UUID> = busy.toSet()

    /** Records [player]'s focus mode; returns whether it changed. */
    @Synchronized
    fun setBusy(player: UUID, value: Boolean): Boolean {
        val changed = if (value) busy.add(player) else busy.remove(player)
        if (changed) save()
        return changed
    }

    @Synchronized
    fun grants(player: UUID): List<String>? = grants[player]

    /** Records [player]'s grants; returns whether they changed. */
    @Synchronized
    fun setGrants(player: UUID, value: List<String>): Boolean {
        if (grants[player] == value) return false
        change { grants[player] = value }
        return true
    }

    @Synchronized
    fun name(player: UUID): String? = names[player]

    /** The player last seen under [name], ignoring case. */
    @Synchronized
    fun playerNamed(name: String): UUID? = names.entries.firstOrNull { it.value.equals(name, ignoreCase = true) }?.key

    @Synchronized
    fun setName(player: UUID, name: String) {
        if (names[player] != name) change { names[player] = name }
    }

    @Synchronized
    fun lastServer(player: UUID): String? = servers[player]

    @Synchronized
    fun setLastServer(player: UUID, server: String) {
        if (servers[player] != server) change { servers[player] = server }
    }

    /** Applies [block] and writes the file. */
    private inline fun change(block: () -> Unit) {
        block()
        save()
    }

    /** Writes the file through a temporary one, so a crash never leaves it half written. */
    private fun save() {
        val json = JSONObject()
            .put("tokens", JSONObject(tokens.mapKeys { it.key.toString() }))
            .put("busy", JSONArray(busy.map(UUID::toString)))
            .put("grants", JSONObject(grants.mapKeys { it.key.toString() }.mapValues { JSONArray(it.value) }))
            .put("names", JSONObject(names.mapKeys { it.key.toString() }))
            .put("servers", JSONObject(servers.mapKeys { it.key.toString() }))
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(json.toString(2))
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun <T> readMap(json: JSONObject, into: MutableMap<UUID, T>, value: (Any?) -> T?) {
        for (key in json.keySet()) {
            val player = uuid(key) ?: continue
            into[player] = value(json.opt(key)) ?: continue
        }
    }

    private fun uuid(value: String): UUID? = try {
        UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
        null
    }
}
