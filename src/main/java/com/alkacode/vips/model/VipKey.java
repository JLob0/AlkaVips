package com.alkacode.vips.model;

import java.util.UUID;

public final class VipKey {

    private final String id;
    private final String vipTypeId;
    private final long duration;
    private boolean used;
    private UUID usedBy;
    private long usedAt;
    private final boolean bonus;

    private boolean forSale;
    private UUID sellerUuid;
    private String economyProvider;
    private double sellPrice;

    public VipKey(String id, String vipTypeId, long duration, boolean used, UUID usedBy, long usedAt,
                  boolean bonus, boolean forSale, UUID sellerUuid, String economyProvider, double sellPrice) {
        this.id = id;
        this.vipTypeId = vipTypeId;
        this.duration = duration;
        this.used = used;
        this.usedBy = usedBy;
        this.usedAt = usedAt;
        this.bonus = bonus;
        this.forSale = forSale;
        this.sellerUuid = sellerUuid;
        this.economyProvider = economyProvider;
        this.sellPrice = sellPrice;
    }

    public String id() { return id; }
    public String vipTypeId() { return vipTypeId; }
    public long duration() { return duration; }
    public boolean used() { return used; }
    public void used(boolean used) { this.used = used; }
    public UUID usedBy() { return usedBy; }
    public void usedBy(UUID usedBy) { this.usedBy = usedBy; }
    public long usedAt() { return usedAt; }
    public void usedAt(long usedAt) { this.usedAt = usedAt; }
    public boolean bonus() { return bonus; }
    public boolean forSale() { return forSale; }
    public void forSale(boolean forSale) { this.forSale = forSale; }
    public UUID sellerUuid() { return sellerUuid; }
    public void sellerUuid(UUID sellerUuid) { this.sellerUuid = sellerUuid; }
    public String economyProvider() { return economyProvider; }
    public void economyProvider(String economyProvider) { this.economyProvider = economyProvider; }
    public double sellPrice() { return sellPrice; }
    public void sellPrice(double sellPrice) { this.sellPrice = sellPrice; }
}
