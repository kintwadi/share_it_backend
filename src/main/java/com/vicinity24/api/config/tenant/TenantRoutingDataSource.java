package com.vicinity24.api.config.tenant;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

public class TenantRoutingDataSource extends AbstractRoutingDataSource implements DisposableBean {
    private final Map<Object, DataSource> managedDataSources = new LinkedHashMap<>();

    public void setManagedTargetDataSources(Map<Object, Object> targetDataSources) {
        managedDataSources.clear();
        targetDataSources.forEach((key, value) -> {
            if (value instanceof DataSource dataSource) {
                managedDataSources.put(key, dataSource);
            }
        });
        super.setTargetDataSources(targetDataSources);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContextHolder.getTenantId();
    }

    public Map<Object, DataSource> getManagedDataSources() {
        return Map.copyOf(managedDataSources);
    }

    @Override
    public void destroy() {
        managedDataSources.values().forEach(dataSource -> {
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                hikariDataSource.close();
            }
        });
        managedDataSources.clear();
    }
}
