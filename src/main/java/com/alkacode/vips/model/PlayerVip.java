package com.alkacode.vips.model;

import com.alkacode.vips.model.enums.VipStatus;

import java.util.UUID;

public final class PlayerVip {

    private final long id;
    private final UUID playerUuid;
    private final String vipTypeId;
    private final String keyId;
    private VipStatus status;
    private long activatedAt;
    private long expiresAt;
    private long totalDuration;
    private boolean frozen;
    private long frozenAt;

    public PlayerVip(long id, UUID playerUuid, String vipTypeId, String keyId, VipStatus status,
                      long activatedAt, long expiresAt, long totalDuration, boolean frozen, long frozenAt) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.vipTypeId = vipTypeId;
        this.keyId = keyId;
        this.status = status;
        this.activatedAt = activatedAt;
        this.expiresAt = expiresAt;
        this.totalDuration = totalDuration;
        this.frozen = frozen;
        this.frozenAt = frozenAt;
    }

    public long id() { return id; }
    public UUID playerUuid() { return playerUuid; }
    public String vipTypeId() { return vipTypeId; }
    public String keyId() { return keyId; }
    public VipStatus status() { return status; }
    public void status(VipStatus status) { this.status = status; }
    public long activatedAt() { return activatedAt; }
    public void activatedAt(long activatedAt) { this.activatedAt = activatedAt; }
    public long expiresAt() { return expiresAt; }
    public void expiresAt(long expiresAt) { this.expiresAt = expiresAt; }
    public long totalDuration() { return totalDuration; }
    public void totalDuration(long totalDuration) { this.totalDuration = totalDuration; }
    public boolean frozen() { return frozen; }
    public void frozen(boolean frozen) { this.frozen = frozen; }
    public long frozenAt() { return frozenAt; }
    public void frozenAt(long frozenAt) { this.frozenAt = frozenAt; }

    public boolean isPermanent() {
        return expiresAt == 0;
    }

    public long remainingMillis() {
        if (isPermanent()) {
            return -1;
        }
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    public boolean isExpired() {
        return !isPermanent() && System.currentTimeMillis() >= expiresAt;
    }
}
