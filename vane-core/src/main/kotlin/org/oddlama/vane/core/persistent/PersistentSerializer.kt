package org.oddlama.vane.core.persistent

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.json.JSONArray
import org.json.JSONObject
import org.oddlama.vane.util.LazyBlock
import org.oddlama.vane.util.LazyLocation
import org.oddlama.vane.util.LegacyKeys
import org.oddlama.vane.util.MaterialUtil.materialFrom
import org.oddlama.vane.util.StorageUtil.namespacedKey
import java.io.IOException
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Serializes and deserializes persistent field values to and from JSON-compatible data.
 */
object PersistentSerializer {
    /**
     * Serializes a [NamespacedKey] to a string.
     */
    private fun serializeNamespacedKey(o: Any): Any = (o as NamespacedKey).toString()

    /**
     * Deserializes a [NamespacedKey] from its string representation.
     *
     * @throws IOException if the input has an invalid namespaced key format.
     */
    @Throws(IOException::class)
    private fun deserializeNamespacedKey(o: Any): NamespacedKey {
        val parts = (o as String).split(":")
        if (parts.size != 2) throw IOException("Invalid namespaced key '$o'")
        return namespacedKey(parts[0], parts[1])
    }

    /**
     * Serializes a [LazyLocation] to a JSON object.
     */
    @Throws(IOException::class)
    private fun serializeLazyLocation(o: Any): Any {
        val ll = o as LazyLocation
        val loc = ll.location()
        return JSONObject().apply {
            put("worldId", toJson(UUID::class.java, ll.worldId))
            put("x", toJson(Double::class.javaPrimitiveType, loc.x))
            put("y", toJson(Double::class.javaPrimitiveType, loc.y))
            put("z", toJson(Double::class.javaPrimitiveType, loc.z))
            put("pitch", toJson(Float::class.javaPrimitiveType, loc.pitch))
            put("yaw", toJson(Float::class.javaPrimitiveType, loc.yaw))
        }
    }

    /**
     * Deserializes a [LazyLocation] from a JSON object.
     */
    @Throws(IOException::class)
    private fun deserializeLazyLocation(o: Any): LazyLocation {
        val json = o as JSONObject
        val worldId = fromJson(UUID::class.java, json["worldId"])
        val x = fromJson(Double::class.javaPrimitiveType, json["x"]) as Double
        val y = fromJson(Double::class.javaPrimitiveType, json["y"]) as Double
        val z = fromJson(Double::class.javaPrimitiveType, json["z"]) as Double
        val pitch = fromJson(Float::class.javaPrimitiveType, json["pitch"]) as Float
        val yaw = fromJson(Float::class.javaPrimitiveType, json["yaw"]) as Float
        return LazyLocation(worldId, x, y, z, yaw, pitch)
    }

    /**
     * Serializes a [LazyBlock] to a JSON object.
     */
    @Throws(IOException::class)
    private fun serializeLazyBlock(o: Any): Any {
        val lb = o as LazyBlock
        return JSONObject().apply {
            put("worldId", toJson(UUID::class.java, lb.worldId))
            put("x", toJson(Int::class.javaPrimitiveType, lb.x))
            put("y", toJson(Int::class.javaPrimitiveType, lb.y))
            put("z", toJson(Int::class.javaPrimitiveType, lb.z))
        }
    }

    /**
     * Deserializes a [LazyBlock] from a JSON object.
     */
    @Throws(IOException::class)
    private fun deserializeLazyBlock(o: Any): LazyBlock {
        val json = o as JSONObject
        val worldId = fromJson(UUID::class.java, json["worldId"])
        val x = fromJson(Int::class.javaPrimitiveType, json["x"]) as Int
        val y = fromJson(Int::class.javaPrimitiveType, json["y"]) as Int
        val z = fromJson(Int::class.javaPrimitiveType, json["z"]) as Int
        return LazyBlock(worldId, x, y, z)
    }

    /**
     * Serializes a [Material] via its [NamespacedKey].
     */
    @Throws(IOException::class)
    private fun serializeMaterial(o: Any): Any? =
        toJson(NamespacedKey::class.java, (o as Material).key)

    /**
     * Deserializes a [Material] from its serialized [NamespacedKey].
     */
    @Throws(IOException::class)
    private fun deserializeMaterial(o: Any): Material? =
        materialFrom(fromJson(NamespacedKey::class.java, o) as NamespacedKey)

    /**
     * Serializes an [ItemStack] to Base64-encoded bytes.
     */
    private fun serializeItemStack(o: Any): Any =
        Base64.getEncoder().encode((o as ItemStack).serializeAsBytes()).toString(StandardCharsets.UTF_8)

    /**
     * Deserializes an [ItemStack] from Base64-encoded bytes.
     */
    private fun deserializeItemStack(o: Any): ItemStack =
        ItemStack.deserializeBytes(Base64.getDecoder().decode((o as String).toByteArray(StandardCharsets.UTF_8)))

    /**
     * Returns whether a value should be treated as JSON null.
     */
    private fun isNull(o: Any?): Boolean = o == null || o === JSONObject.NULL

    /**
     * Serializer registry keyed by target class.
     */
    @JvmField
    val serializers: MutableMap<Class<*>?, Function<Any?, Any?>> = HashMap()

    /**
     * Deserializer registry keyed by source class.
     */
    @JvmField
    val deserializers: MutableMap<Class<*>?, Function<Any?, Any?>> = HashMap()

    init {
        /**
         * Registers serializers for primitive and boxed primitive types.
         */
        for (cls in listOf(
            Boolean::class.javaPrimitiveType, Boolean::class.java, Boolean::class.javaObjectType,
            Char::class.javaPrimitiveType, Char::class.java, Char::class.javaObjectType,
            Double::class.javaPrimitiveType, Double::class.java, Double::class.javaObjectType,
            Float::class.javaPrimitiveType, Float::class.java, Float::class.javaObjectType,
            Int::class.javaPrimitiveType, Int::class.java, Int::class.javaObjectType,
            Long::class.javaPrimitiveType, Long::class.java, Long::class.javaObjectType,
        )) {
            @Suppress("UNCHECKED_CAST")
            serializers[cls] = Function { it.toString() }
        }

        deserializers[Boolean::class.javaPrimitiveType] = Function { (it as String?).toBoolean() }
        deserializers[Boolean::class.java] = Function { (it as String?).toBoolean() }
        deserializers[Boolean::class.javaObjectType] = Function { (it as String?).toBoolean() }
        deserializers[Char::class.javaPrimitiveType] = Function { (it as String)[0] }
        deserializers[Char::class.java] = Function { (it as String)[0] }
        deserializers[Char::class.javaObjectType] = Function { (it as String)[0] }
        deserializers[Double::class.javaPrimitiveType] = Function { (it as String).toDouble() }
        deserializers[Double::class.java] = Function { (it as String).toDouble() }
        deserializers[Double::class.javaObjectType] = Function { (it as String).toDouble() }
        deserializers[Float::class.javaPrimitiveType] = Function { (it as String).toFloat() }
        deserializers[Float::class.java] = Function { (it as String).toFloat() }
        deserializers[Float::class.javaObjectType] = Function { (it as String).toFloat() }
        deserializers[Int::class.javaPrimitiveType] = Function { (it as String).toInt() }
        deserializers[Int::class.java] = Function { (it as String).toInt() }
        deserializers[Int::class.javaObjectType] = Function { (it as String).toInt() }
        deserializers[Long::class.javaPrimitiveType] = Function { (it as String).toLong() }
        deserializers[Long::class.java] = Function { (it as String).toLong() }
        deserializers[Long::class.javaObjectType] = Function { (it as String).toLong() }

        /**
         * Registers serializers for standard library value types.
         */
        serializers[String::class.java] = Function { it }
        deserializers[String::class.java] = Function { it }
        serializers[UUID::class.java] = Function { it.toString() }
        deserializers[UUID::class.java] = Function { UUID.fromString(it as String?) }

        /**
         * Registers serializers for Bukkit and vane-specific types.
         */
        serializers[NamespacedKey::class.java] = Function { serializeNamespacedKey(it!!) }
        deserializers[NamespacedKey::class.java] = Function { deserializeNamespacedKey(it!!) }
        serializers[LazyLocation::class.java] = Function { serializeLazyLocation(it!!) }
        deserializers[LazyLocation::class.java] = Function { deserializeLazyLocation(it!!) }
        serializers[LazyBlock::class.java] = Function { serializeLazyBlock(it!!) }
        deserializers[LazyBlock::class.java] = Function { deserializeLazyBlock(it!!) }
        serializers[Material::class.java] = Function { serializeMaterial(it!!) }
        deserializers[Material::class.java] = Function { deserializeMaterial(it!!) }
        serializers[ItemStack::class.java] = Function { serializeItemStack(it!!) }
        deserializers[ItemStack::class.java] = Function { deserializeItemStack(it!!) }
    }

    /**
     * Serializes a field value using the field's generic type.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun toJson(field: Field, value: Any?): Any? = toJson(field.genericType, value)

    /**
     * Serializes a value for a concrete class.
     *
     * @throws IOException if no serializer is registered for the class.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun toJson(cls: Class<*>?, value: Any?): Any? {
        if (isNull(value)) return JSONObject.NULL
        val serializer: Function<Any?, Any?> = serializers[cls]
            ?: if (cls != null && cls.isEnum) Function { (it as Enum<*>).name }
            else throw IOException("No serializer registered for type $cls. This is a bug.")
        return serializer.apply(value)
    }

    /**
     * Serializes a value for a generic type such as maps, sets, or lists.
     *
     * @throws IOException if the type cannot be serialized.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun toJson(type: Type?, value: Any?): Any? {
        if (isNull(value)) return JSONObject.NULL
        if (type !is ParameterizedType) return toJson(type as Class<*>?, value)

        val typeArgs = type.actualTypeArguments
        return when (type.rawType) {
            // Null members are written as JSONObject.NULL, which fromJson reads back as null.
            MutableMap::class.java -> JSONObject().also { json ->
                for ((k, v) in value as Map<*, *>) {
                    json.put(toJson(typeArgs[0] as Class<*>?, k) as String?, toJson(typeArgs[1], v))
                }
            }

            MutableSet::class.java -> JSONArray().also { json ->
                for (t in value as Set<*>) json.put(toJson(typeArgs[0], t))
            }

            MutableList::class.java -> JSONArray().also { json ->
                for (t in value as List<*>) json.put(toJson(typeArgs[0], t))
            }

            else -> throw IOException("Cannot serialize $type. This is a bug.")
        }
    }

    /**
     * Deserializes a field value using the field's generic type.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun fromJson(field: Field, value: Any?): Any? = fromJson(field.genericType, value)

    /**
     * Deserializes a value for a concrete class.
     *
     * @throws IOException if no deserializer is registered for the class.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun <U> fromJson(cls: Class<U>?, value: Any?): U? {
        if (isNull(value)) return null
        // Every serialized object passes through here, so data saved before v1.22.0 is upgraded
        // on load and written back with the new keys on the next save.
        if (value is JSONObject) LegacyKeys.upgradeJsonObject(value)
        val deserializer: Function<Any?, Any?> = deserializers[cls]
            ?: if (cls != null && cls.isEnum) {
                @Suppress("UNCHECKED_CAST")
                Function { java.lang.Enum.valueOf(cls as Class<out Enum<*>>, it as String) }
            } else throw IOException("No deserializer registered for type $cls. This is a bug.")
        @Suppress("UNCHECKED_CAST")
        return deserializer.apply(value) as U?
    }

    /**
     * Deserializes a value for a generic type such as maps, sets, or lists.
     *
     * @throws IOException if the type cannot be deserialized.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun fromJson(type: Type?, json: Any?): Any? {
        if (isNull(json)) return null
        if (type !is ParameterizedType) return fromJson(type as Class<*>?, json)

        val typeArgs = type.actualTypeArguments
        return when (type.rawType) {
            MutableMap::class.java -> buildMap {
                for (key in (json as JSONObject).keySet()) {
                    put(fromJson(typeArgs[0] as Class<*>?, key), fromJson(typeArgs[1], json[key]))
                }
            }.toMutableMap()

            MutableSet::class.java -> (json as JSONArray).mapTo(HashSet()) { fromJson(typeArgs[0], it) }
            MutableList::class.java -> (json as JSONArray).map { fromJson(typeArgs[0], it) }
            else -> throw IOException("Cannot deserialize $type. This is a bug.")
        }
    }

    /**
     * Functional abstraction used by serializer and deserializer registries.
     */
    fun interface Function<T1, R> {
        /**
         * Applies the transformation.
         *
         * @param t1 the input value.
         * @return the transformed value.
         * @throws IOException if transformation fails.
         */
        @Throws(IOException::class)
        fun apply(t1: T1?): R?
    }
}
