package com.nearshare.api.config;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigOverrideRepository extends JpaRepository<AppConfigOverride, String> {
}

