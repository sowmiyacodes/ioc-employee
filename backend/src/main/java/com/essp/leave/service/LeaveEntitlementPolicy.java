package com.essp.leave.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class LeaveEntitlementPolicy {
    private final String configuredDays;

    public LeaveEntitlementPolicy(
            @Value("${app.leave.entitlement-options:0,5,10,12,15,18,20,24,30}") String configuredDays) {
        this.configuredDays = configuredDays;
    }

    public List<Integer> allowedDays() {
        return Arrays.stream(configuredDays.split(","))
                .map(String::trim)
                .map(value -> {
                    try {
                        int days = Integer.parseInt(value);
                        if (days < 0 || days > 365) throw new NumberFormatException();
                        return days;
                    } catch (NumberFormatException exception) {
                        throw new IllegalStateException(
                                "app.leave.entitlement-options must contain values from 0 to 365", exception);
                    }
                })
                .distinct()
                .sorted()
                .toList();
    }

    public boolean allows(int days) {
        return allowedDays().contains(days);
    }
}
