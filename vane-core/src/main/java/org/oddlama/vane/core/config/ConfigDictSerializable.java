package org.oddlama.vane.core.config;

import java.util.Map;

public interface ConfigDictSerializable {
    public Map<String, Object> to_dict();

    public void from_dict(Map<String, Object> dict);
}
