package org.oddlama.vane.core.persistent;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.function.Function;
import org.json.JSONObject;

public class PersistentField {

    private Object owner;
    private Field field;
    private String path;

    public PersistentField(Object owner, Field field, Function<String, String> map_name) {
        this.owner = owner;
        this.field = field;
        this.path = map_name.apply(field.getName().substring("storage_".length()));

        field.setAccessible(true);
    }

    public String path() {
        return path;
    }

    public Object get() {
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }

    public void save(JSONObject json) throws IOException {
        json.put(path, PersistentSerializer.to_json(field, get()));
    }

    public void load(JSONObject json) throws IOException {
        if (!json.has(path)) {
            throw new IOException("Missing key in persistent storage: '" + path + "'");
        }

        try {
            field.set(owner, PersistentSerializer.from_json(field, json.get(path)));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
        }
    }
}
