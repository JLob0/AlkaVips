package com.alkacode.vips.model;

import java.util.UUID;

public final class VipPlayerData {

    private final UUID uuid;
    private double credits;
    private double spentCredits;
    private int totalActivations;
    private boolean autoSellEnabled;
    private Long selectedVipId;
    private long lastQuitAt;

    public VipPlayerData(UUID uuid, double credits, double spentCredits, int totalActivations,
                          boolean autoSellEnabled, Long selectedVipId, long lastQuitAt) {
        this.uuid = uuid;
        this.credits = credits;
        this.spentCredits = spentCredits;
        this.totalActivations = totalActivations;
        this.autoSellEnabled = autoSellEnabled;
        this.selectedVipId = selectedVipId;
        this.lastQuitAt = lastQuitAt;
    }

    public UUID uuid() { return uuid; }
    public double credits() { return credits; }
    public void credits(double credits) { this.credits = credits; }
    public double spentCredits() { return spentCredits; }
    public void spentCredits(double spentCredits) { this.spentCredits = spentCredits; }
    public int totalActivations() { return totalActivations; }
    public void totalActivations(int totalActivations) { this.totalActivations = totalActivations; }
    public boolean autoSellEnabled() { return autoSellEnabled; }
    public void autoSellEnabled(boolean autoSellEnabled) { this.autoSellEnabled = autoSellEnabled; }
    public Long selectedVipId() { return selectedVipId; }
    public void selectedVipId(Long selectedVipId) { this.selectedVipId = selectedVipId; }
    public long lastQuitAt() { return lastQuitAt; }
    public void lastQuitAt(long lastQuitAt) { this.lastQuitAt = lastQuitAt; }
}
