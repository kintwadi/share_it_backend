package com.nearshare.api.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RuntimeSettingsService implements ConfigProvider {
    private final SettingsProperties settingsProperties;
    private final AppConfigOverrideRepository overridesRepository;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    private final AtomicLong version = new AtomicLong(0);
    private volatile long cachedVersion = -1;
    private volatile Map<String, Object> cachedEffectiveSettings = null;
    private volatile Map<String, Object> cachedBaseSettings = null;
    private volatile long cachedOverridesVersion = -1;
    private volatile Map<String, Object> cachedOverrides = null;

    private final List<ExtraKeyDef> extraKeyDefs;

    public RuntimeSettingsService(SettingsProperties settingsProperties, AppConfigOverrideRepository overridesRepository, ObjectMapper objectMapper, Environment environment) {
        this.settingsProperties = settingsProperties;
        this.overridesRepository = overridesRepository;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.extraKeyDefs = buildExtraKeyDefs();
    }

    public Map<String, Object> getEffectiveSettings() {
        long v = version.get();
        Map<String, Object> cached = cachedEffectiveSettings;
        if (cached != null && cachedVersion == v) {
            return cached;
        }
        Map<String, Object> base = getBaseSettings();
        Map<String, Object> effective = deepCopy(base);
        applyOverrides(effective);
        cachedBaseSettings = base;
        cachedEffectiveSettings = effective;
        cachedVersion = v;
        return effective;
    }

    public Map<String, Object> getBaseSettings() {
        Map<String, Object> base = cachedBaseSettings;
        if (base != null) {
            return base;
        }
        Map<String, Object> out = objectMapper.convertValue(settingsProperties, new TypeReference<Map<String, Object>>() {});
        cachedBaseSettings = out;
        return out;
    }

    public boolean isEnabled(String fullKey) {
        return isEnabled(fullKey, true);
    }

    public boolean isEnabled(String fullKey, boolean defaultValue) {
        Object v = getValue(fullKey);
        return toEnabledValue(v, defaultValue);
    }

    public Object getValue(String fullKey) {
        String k = String.valueOf(fullKey == null ? "" : fullKey).trim();
        if (!k.startsWith("settings.")) {
            return null;
        }
        String path = k.substring("settings.".length());
        Map<String, Object> effective = getEffectiveSettings();
        return getByPath(effective, path);
    }

    @Override
    public String getString(String key, String defaultValue) {
        Object v = getConfigValue(key);
        if (v == null) return defaultValue;
        String s = String.valueOf(v);
        return s != null ? s : defaultValue;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        Object v = getConfigValue(key);
        if (v == null) return defaultValue;
        if (v instanceof Number n) return n.intValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        Object v = getConfigValue(key);
        if (v == null) return defaultValue;
        if (v instanceof Number n) return n.doubleValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        Object v = getConfigValue(key);
        return toEnabledValue(v, defaultValue);
    }

    private Object getConfigValue(String key) {
        String k = String.valueOf(key == null ? "" : key).trim();
        if (k.isEmpty()) return null;
        Map<String, Object> overrides = getOverridesMap();
        if (overrides.containsKey(k)) return overrides.get(k);
        if (k.startsWith("settings.")) return getValue(k);
        try {
            return environment != null ? environment.getProperty(k) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public AdminSettingsResponse getEditableSettings() {
        Map<String, Object> base = getBaseSettings();
        Map<String, Object> effective = getEffectiveSettings();
        Map<String, Object> baseFlat = flattenSettings(base);
        Map<String, Object> effectiveFlat = flattenSettings(effective);
        Map<String, Object> overrides = getOverridesMap();

        List<AdminSettingsSection> sections = new ArrayList<>();
        Map<String, AdminSettingsSection> byId = new LinkedHashMap<>();

        for (Map.Entry<String, Object> e : baseFlat.entrySet()) {
            String key = e.getKey();
            if (!isEditableKey(key)) continue;
            String sectionId = sectionIdForKey(key);
            if (sectionId.equals("security")) continue;

            AdminSettingsSection section = byId.computeIfAbsent(sectionId, id -> {
                AdminSettingsSection s = new AdminSettingsSection();
                s.id = id;
                s.title = titleForSection(id);
                s.items = new ArrayList<>();
                sections.add(s);
                return s;
            });

            Object baseVal = e.getValue();
            Object effectiveVal = effectiveFlat.getOrDefault(key, baseVal);

            AdminSettingsItem item = new AdminSettingsItem();
            item.key = key;
            item.type = typeOfValue(baseVal);
            item.defaultValue = baseVal;
            item.value = effectiveVal;
            item.overridden = !Objects.equals(normalizePrimitive(baseVal), normalizePrimitive(effectiveVal));

            section.items.add(item);
        }

        for (ExtraKeyDef def : extraKeyDefs) {
            if (def == null || def.key == null) continue;
            if (!isEditableKey(def.key)) continue;
            AdminSettingsSection section = byId.computeIfAbsent(def.sectionId, id -> {
                AdminSettingsSection s = new AdminSettingsSection();
                s.id = id;
                s.title = titleForSection(id);
                s.items = new ArrayList<>();
                sections.add(s);
                return s;
            });

            Object baseRaw = readBaseProperty(def.key);
            Object baseVal = baseRaw != null ? coerceType(baseRaw, def.type) : def.defaultValue;
            Object effectiveVal = overrides.containsKey(def.key) ? coerceType(overrides.get(def.key), def.type) : baseVal;

            AdminSettingsItem item = new AdminSettingsItem();
            item.key = def.key;
            item.type = def.type;
            item.defaultValue = baseVal;
            item.value = effectiveVal;
            item.overridden = overrides.containsKey(def.key);

            section.items.add(item);
        }

        AdminSettingsResponse res = new AdminSettingsResponse();
        res.sections = sections;
        return res;
    }

    public void applyUpdates(List<AdminSettingsUpdate> updates, String updatedBy) {
        if (updates == null || updates.isEmpty()) return;

        Map<String, Object> base = getBaseSettings();
        Map<String, Object> baseFlat = flattenSettings(base);
        java.util.Set<String> extraKeys = new java.util.HashSet<>();
        for (ExtraKeyDef d : extraKeyDefs) {
            if (d != null && d.key != null) extraKeys.add(d.key);
        }

        for (AdminSettingsUpdate u : updates) {
            if (u == null) continue;
            String key = String.valueOf(u.key == null ? "" : u.key).trim();
            if (!isEditableKey(key)) {
                throw new IllegalArgumentException("key_not_editable: " + key);
            }
            if (key.startsWith("settings.")) {
                if (sectionIdForKey(key).equals("security")) {
                    throw new IllegalArgumentException("key_not_editable: " + key);
                }
                if (!baseFlat.containsKey(key)) {
                    throw new IllegalArgumentException("unknown_key: " + key);
                }
            } else {
                if (!extraKeys.contains(key)) {
                    throw new IllegalArgumentException("unknown_key: " + key);
                }
            }

            if (u.value == null) {
                overridesRepository.deleteById(key);
                continue;
            }

            String json;
            try {
                json = objectMapper.writeValueAsString(u.value);
            } catch (Exception ex) {
                throw new IllegalArgumentException("invalid_value: " + key);
            }

            AppConfigOverride entity = AppConfigOverride.builder()
                    .key(key)
                    .valueJson(json)
                    .updatedAt(LocalDateTime.now())
                    .updatedBy(updatedBy)
                    .build();
            overridesRepository.save(entity);
        }

        version.incrementAndGet();
        cachedEffectiveSettings = null;
        cachedVersion = -1;
        cachedOverrides = null;
        cachedOverridesVersion = -1;
    }

    private void applyOverrides(Map<String, Object> targetSettings) {
        Map<String, Object> overrides = getOverridesMap();
        for (Map.Entry<String, Object> e : overrides.entrySet()) {
            String key = String.valueOf(e.getKey() == null ? "" : e.getKey()).trim();
            if (!key.startsWith("settings.")) continue;
            String path = key.substring("settings.".length());
            setByPath(targetSettings, path, e.getValue());
        }
    }

    private Map<String, Object> getOverridesMap() {
        long v = version.get();
        Map<String, Object> cached = cachedOverrides;
        if (cached != null && cachedOverridesVersion == v) return cached;

        Map<String, Object> out = new LinkedHashMap<>();
        List<AppConfigOverride> overrides = overridesRepository.findAll();
        for (AppConfigOverride o : overrides) {
            if (o == null) continue;
            String key = String.valueOf(o.getKey() == null ? "" : o.getKey()).trim();
            if (key.isEmpty()) continue;
            try {
                Object value = objectMapper.readValue(o.getValueJson(), Object.class);
                out.put(key, value);
            } catch (Exception ignored) {
            }
        }
        cachedOverrides = out;
        cachedOverridesVersion = v;
        return out;
    }

    private List<ExtraKeyDef> buildExtraKeyDefs() {
        List<ExtraKeyDef> out = new ArrayList<>();

        out.add(new ExtraKeyDef("subscription.currency", "string", "subscription", "Subscription", "EUR"));
        out.add(new ExtraKeyDef("subscription.plus.monthly_amount_cents", "number", "subscription", "Subscription", 499));
        out.add(new ExtraKeyDef("subscription.plus.trial_days", "number", "subscription", "Subscription", 14));
        out.add(new ExtraKeyDef("subscription.plus.stripe_price_id", "string", "subscription", "Subscription", ""));
        out.add(new ExtraKeyDef("subscription.pro.monthly_amount_cents", "number", "subscription", "Subscription", 799));
        out.add(new ExtraKeyDef("subscription.pro.trial_days", "number", "subscription", "Subscription", 14));
        out.add(new ExtraKeyDef("subscription.pro.stripe_price_id", "string", "subscription", "Subscription", ""));
        out.add(new ExtraKeyDef("subscription.starter.enabled", "boolean", "subscription", "Subscription", true));
        out.add(new ExtraKeyDef("subscription.plus.enabled", "boolean", "subscription", "Subscription", true));
        out.add(new ExtraKeyDef("subscription.pro.enabled", "boolean", "subscription", "Subscription", true));

        out.add(new ExtraKeyDef("insurance.currency", "string", "insurance", "Insurance", "USD"));
        out.add(new ExtraKeyDef("insurance.quote-validity-minutes", "number", "insurance", "Insurance", 30));
        out.add(new ExtraKeyDef("insurance.zip-adjustment.prefix", "string", "insurance", "Insurance", "9"));
        out.add(new ExtraKeyDef("insurance.zip-adjustment.multiplier", "number", "insurance", "Insurance", 1.15));

        out.add(new ExtraKeyDef("insurance.rules.basic.percent", "number", "insurance", "Insurance", 0.05));
        out.add(new ExtraKeyDef("insurance.rules.basic.min", "number", "insurance", "Insurance", 5));
        out.add(new ExtraKeyDef("insurance.rules.basic.max", "number", "insurance", "Insurance", 50));

        out.add(new ExtraKeyDef("insurance.rules.premium.percent", "number", "insurance", "Insurance", 0.10));
        out.add(new ExtraKeyDef("insurance.rules.premium.min", "number", "insurance", "Insurance", 10));
        out.add(new ExtraKeyDef("insurance.rules.premium.max", "number", "insurance", "Insurance", 100));

        out.add(new ExtraKeyDef("insurance.rules.theft_protection.percent", "number", "insurance", "Insurance", 0.08));
        out.add(new ExtraKeyDef("insurance.rules.theft_protection.min", "number", "insurance", "Insurance", 8));
        out.add(new ExtraKeyDef("insurance.rules.theft_protection.max", "number", "insurance", "Insurance", 80));

        out.add(new ExtraKeyDef("insurance.rules.extended_warranty.percent", "number", "insurance", "Insurance", 0.03));
        out.add(new ExtraKeyDef("insurance.rules.extended_warranty.min", "number", "insurance", "Insurance", 3));
        out.add(new ExtraKeyDef("insurance.rules.extended_warranty.max", "number", "insurance", "Insurance", 30));

        out.add(new ExtraKeyDef("image.max.size.mb", "number", "uploads", "Uploads", 5));
        out.add(new ExtraKeyDef("allowed.image.types", "string", "uploads", "Uploads", "jpg,jpeg,png,gif,webp"));

        out.add(new ExtraKeyDef("geolocation.freeipapi.base_url", "string", "geolocation", "Geolocation", "https://free.freeipapi.com/api/json/"));

        return out;
    }

    private Object readBaseProperty(String key) {
        if (environment == null) return null;
        try {
            return environment.getProperty(key);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object coerceType(Object value, String type) {
        if (value == null) return null;
        if ("boolean".equals(type)) {
            if (value instanceof Boolean) return value;
            String s = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
            if (s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("on") || s.equals("enabled")) return true;
            if (s.equals("false") || s.equals("0") || s.equals("no") || s.equals("off") || s.equals("disabled")) return false;
            return value;
        }
        if ("number".equals(type)) {
            if (value instanceof Number) return value;
            String s = String.valueOf(value).trim();
            if (s.isEmpty()) return value;
            try {
                if (s.contains(".")) return Double.parseDouble(s);
                return Integer.parseInt(s);
            } catch (Exception e) {
                return value;
            }
        }
        return String.valueOf(value);
    }

    private static Map<String, Object> deepCopy(Map<String, Object> input) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : input.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Map<?, ?> m) {
                out.put(e.getKey(), deepCopy((Map<String, Object>) m));
            } else if (v instanceof List<?> list) {
                out.put(e.getKey(), new ArrayList<>(list));
            } else {
                out.put(e.getKey(), v);
            }
        }
        return out;
    }

    private static Object getByPath(Map<String, Object> root, String path) {
        if (root == null) return null;
        String p = String.valueOf(path == null ? "" : path).trim();
        if (p.isEmpty()) return root;
        String[] parts = p.split("\\.");
        Object cur = root;
        for (String part : parts) {
            if (cur == null) return null;
            if (!(cur instanceof Map<?, ?> m)) return null;
            cur = m.get(part);
        }
        return cur;
    }

    private static void setByPath(Map<String, Object> root, String path, Object value) {
        if (root == null) return;
        String p = String.valueOf(path == null ? "" : path).trim();
        if (p.isEmpty()) return;
        String[] parts = p.split("\\.");
        Map<String, Object> cur = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = cur.get(part);
            if (!(next instanceof Map<?, ?>)) {
                Map<String, Object> created = new LinkedHashMap<>();
                cur.put(part, created);
                cur = created;
            } else {
                cur = (Map<String, Object>) next;
            }
        }
        cur.put(parts[parts.length - 1], value);
    }

    private Map<String, Object> flattenSettings(Map<String, Object> settingsRoot) {
        Map<String, Object> out = new LinkedHashMap<>();
        walkFlatten(out, "settings", settingsRoot);
        return out;
    }

    private static void walkFlatten(Map<String, Object> out, String prefix, Object value) {
        if (value == null) return;
        if (value instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                String k = String.valueOf(e.getKey());
                walkFlatten(out, prefix + "." + k, e.getValue());
            }
            return;
        }
        if (value instanceof List<?>) {
            return;
        }
        if (isPrimitive(value)) {
            out.put(prefix, value);
        }
    }

    private static boolean isPrimitive(Object v) {
        return v instanceof String || v instanceof Number || v instanceof Boolean;
    }

    private static Object normalizePrimitive(Object v) {
        if (v == null) return null;
        if (v instanceof String s) {
            return s.trim();
        }
        return v;
    }

    private static String typeOfValue(Object v) {
        if (v instanceof Boolean) return "boolean";
        if (v instanceof Number) return "number";
        return "string";
    }

    private static boolean toEnabledValue(Object v, boolean defaultValue) {
        if (v == null) return defaultValue;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return defaultValue;
        if (s.equals("false") || s.equals("0") || s.equals("off") || s.equals("disabled") || s.equals("no")) return false;
        if (s.equals("true") || s.equals("1") || s.equals("on") || s.equals("enabled") || s.equals("yes")) return true;
        return defaultValue;
    }

    private static boolean isEditableKey(String key) {
        if (key == null) return false;
        String k = key.toLowerCase(Locale.ROOT);
        if (k.contains("password")) return false;
        if (k.contains("secret")) return false;
        if (k.contains("token")) return false;
        if (k.contains("private")) return false;
        if (k.startsWith("settings.security.")) return false;
        if (key.startsWith("settings.")) return true;
        for (ExtraKeyDef def : DEFAULT_EXTRA_KEYS) {
            if (def != null && key.equals(def.key)) return true;
        }
        return false;
    }

    private static String sectionIdForKey(String key) {
        String k = String.valueOf(key == null ? "" : key).trim();
        if (!k.startsWith("settings.")) return "other";
        String rest = k.substring("settings.".length());
        int dot = rest.indexOf('.');
        return dot > 0 ? rest.substring(0, dot) : rest;
    }

    private static String titleForSection(String id) {
        if (id == null) return "Settings";
        String clean = id.replace('_', ' ').trim();
        if (clean.isEmpty()) return "Settings";
        return clean.substring(0, 1).toUpperCase(Locale.ROOT) + clean.substring(1);
    }

    private static final List<ExtraKeyDef> DEFAULT_EXTRA_KEYS = List.of(
            new ExtraKeyDef("subscription.currency", "string", "subscription", "Subscription", "EUR"),
            new ExtraKeyDef("subscription.plus.monthly_amount_cents", "number", "subscription", "Subscription", 499),
            new ExtraKeyDef("subscription.plus.trial_days", "number", "subscription", "Subscription", 14),
            new ExtraKeyDef("subscription.plus.stripe_price_id", "string", "subscription", "Subscription", ""),
            new ExtraKeyDef("subscription.pro.monthly_amount_cents", "number", "subscription", "Subscription", 799),
            new ExtraKeyDef("subscription.pro.trial_days", "number", "subscription", "Subscription", 14),
            new ExtraKeyDef("subscription.pro.stripe_price_id", "string", "subscription", "Subscription", ""),
            new ExtraKeyDef("subscription.starter.enabled", "boolean", "subscription", "Subscription", true),
            new ExtraKeyDef("subscription.plus.enabled", "boolean", "subscription", "Subscription", true),
            new ExtraKeyDef("subscription.pro.enabled", "boolean", "subscription", "Subscription", true),
            new ExtraKeyDef("insurance.currency", "string", "insurance", "Insurance", "USD"),
            new ExtraKeyDef("insurance.quote-validity-minutes", "number", "insurance", "Insurance", 30),
            new ExtraKeyDef("insurance.zip-adjustment.prefix", "string", "insurance", "Insurance", "9"),
            new ExtraKeyDef("insurance.zip-adjustment.multiplier", "number", "insurance", "Insurance", 1.15),
            new ExtraKeyDef("insurance.rules.basic.percent", "number", "insurance", "Insurance", 0.05),
            new ExtraKeyDef("insurance.rules.basic.min", "number", "insurance", "Insurance", 5),
            new ExtraKeyDef("insurance.rules.basic.max", "number", "insurance", "Insurance", 50),
            new ExtraKeyDef("insurance.rules.premium.percent", "number", "insurance", "Insurance", 0.10),
            new ExtraKeyDef("insurance.rules.premium.min", "number", "insurance", "Insurance", 10),
            new ExtraKeyDef("insurance.rules.premium.max", "number", "insurance", "Insurance", 100),
            new ExtraKeyDef("insurance.rules.theft_protection.percent", "number", "insurance", "Insurance", 0.08),
            new ExtraKeyDef("insurance.rules.theft_protection.min", "number", "insurance", "Insurance", 8),
            new ExtraKeyDef("insurance.rules.theft_protection.max", "number", "insurance", "Insurance", 80),
            new ExtraKeyDef("insurance.rules.extended_warranty.percent", "number", "insurance", "Insurance", 0.03),
            new ExtraKeyDef("insurance.rules.extended_warranty.min", "number", "insurance", "Insurance", 3),
            new ExtraKeyDef("insurance.rules.extended_warranty.max", "number", "insurance", "Insurance", 30),
            new ExtraKeyDef("image.max.size.mb", "number", "uploads", "Uploads", 5),
            new ExtraKeyDef("allowed.image.types", "string", "uploads", "Uploads", "jpg,jpeg,png,gif,webp"),
            new ExtraKeyDef("geolocation.freeipapi.base_url", "string", "geolocation", "Geolocation", "https://free.freeipapi.com/api/json/")
    );

    private static final class ExtraKeyDef {
        final String key;
        final String type;
        final String sectionId;
        final String title;
        final Object defaultValue;

        ExtraKeyDef(String key, String type, String sectionId, String title, Object defaultValue) {
            this.key = key;
            this.type = type;
            this.sectionId = sectionId;
            this.title = title;
            this.defaultValue = defaultValue;
        }
    }

    public static class AdminSettingsResponse {
        public List<AdminSettingsSection> sections;
    }

    public static class AdminSettingsSection {
        public String id;
        public String title;
        public List<AdminSettingsItem> items;
    }

    public static class AdminSettingsItem {
        public String key;
        public String type;
        public Object value;
        public Object defaultValue;
        public boolean overridden;
    }

    public static class AdminSettingsUpdate {
        public String key;
        public Object value;
    }
}
