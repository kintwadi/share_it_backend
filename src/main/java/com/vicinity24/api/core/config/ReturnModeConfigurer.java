package com.vicinity24.api.core.config;

import com.vicinity24.api.core.model.enums.ReturnMode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReturnModeConfigurer {
    private final SettingsProperties settingsProperties;
    private final String returnModeOverride;

    public ReturnModeConfigurer(
            SettingsProperties settingsProperties,
            @Value("${settings.return.mode:}") String returnModeOverride
    ) {
        this.settingsProperties = settingsProperties;
        this.returnModeOverride = returnModeOverride;
    }

    @PostConstruct
    public void apply() {
        SettingsProperties.ReturnsConfig rc = settingsProperties != null ? settingsProperties.getReturns() : null;
        if (rc == null) return;

        String raw = returnModeOverride != null && !returnModeOverride.isBlank() ? returnModeOverride : rc.getMode();
        ReturnMode mode = ReturnMode.from(raw);
        rc.setMode(mode.name());

        boolean qr = mode == ReturnMode.ANY || mode == ReturnMode.QR_CODE;
        boolean manual = mode == ReturnMode.ANY || mode == ReturnMode.MANUAL;
        boolean dispute = mode == ReturnMode.ANY || mode == ReturnMode.DISPUTE;

        if (rc.getQr() != null) rc.getQr().setEnabled(qr);
        if (rc.getManual() != null) rc.getManual().setEnabled(manual);
        if (rc.getDispute() != null) rc.getDispute().setEnabled(dispute);
    }
}
