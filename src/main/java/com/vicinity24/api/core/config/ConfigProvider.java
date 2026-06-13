package com.vicinity24.api.core.config;

public interface ConfigProvider {
    String getString(String key, String defaultValue);

    int getInt(String key, int defaultValue);

    double getDouble(String key, double defaultValue);

    boolean getBoolean(String key, boolean defaultValue);
}

