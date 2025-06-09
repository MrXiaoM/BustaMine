package me.sat7.bustamine.utils;

import java.util.function.Function;

public class Property<V> {
    public interface IPropertyRegistry {
        void registerProperty(Property<?> property);
    }
    private final String key;
    private final CustomConfig config;
    private final Function<Property<V>, V> reload;
    private final Object defaultValue;
    private V value;

    public Property(CustomConfig config, String key, Function<Property<V>, V> reload, Object defaultValue) {
        this.config = config;
        this.key = key;
        this.reload = reload;
        this.defaultValue = defaultValue;
        if (config instanceof IPropertyRegistry) {
            ((IPropertyRegistry) config).registerProperty(this);
        }
    }

    public String getKey() {
        return key;
    }

    public CustomConfig getConfig() {
        return config;
    }

    public void setup() {
        config.get().addDefault(getKey(), defaultValue);
    }

    public void reload() {
        this.value = this.reload.apply(this);
    }

    public V val() {
        return value;
    }

    public boolean isEmpty() {
        if (value instanceof String) {
            return ((String) value).isEmpty();
        }
        return true;
    }

    public String replace(CharSequence target, CharSequence replacement) {
        if (value instanceof String) {
            return ((String) value).replace(target, replacement);
        }
        return "";
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public static Property<String> property(CustomConfig config, String key, String def) {
        return new Property<>(config, key, property -> property.getConfig().getString(key, def), def);
    }
    public static Property<Integer> property(CustomConfig config, String key, int def) {
        return new Property<>(config, key, property -> property.getConfig().getInt(key, def), def);
    }
    public static Property<Double> property(CustomConfig config, String key, double def) {
        return new Property<>(config, key, property -> property.getConfig().getDouble(key, def), def);
    }
    public static Property<Boolean> property(CustomConfig config, String key, boolean def) {
        return new Property<>(config, key, property -> property.getConfig().getBoolean(key, def), def);
    }
    public static Property<Item> propertyItem(CustomConfig config, String key, String def) {
        return new Property<>(config, key, property -> property.getConfig().getItem(key), def);
    }
}
