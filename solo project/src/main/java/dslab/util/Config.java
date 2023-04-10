package dslab.util;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Reads the configuration from a {@code .properties} file.
 */
public final class Config {

    private final ResourceBundle bundle;
    private final Map<String, Object> properties = new HashMap<>();

    public Config(String name) {
        if (name.endsWith(".properties")) {
            this.bundle = ResourceBundle.getBundle(name.substring(0, name.length() - 11));
        } else {
            this.bundle = ResourceBundle.getBundle(name);
        }
    }

    public String getString(String key) {
        if (properties.containsKey(key)) {
            return properties.get(key).toString();
        }
        return this.bundle.getString(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    public boolean containsKey(String key) {
        return properties.containsKey(key) || bundle.containsKey(key);
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Set<String> listKeys() {
        Set<String> keys = bundle.keySet();
        keys.addAll(properties.keySet());
        return keys;
    }
}
