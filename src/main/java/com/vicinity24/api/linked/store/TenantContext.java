package com.vicinity24.api.linked.store;

public final class TenantContext {
    public static final String HEADER_NAME = "X-Store-Id";
    private static final ThreadLocal<Long> STORE_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setStoreId(Long storeId) {
        STORE_ID.set(storeId);
    }

    public static Long getStoreId() {
        return STORE_ID.get();
    }

    public static void clear() {
        STORE_ID.remove();
    }
}


