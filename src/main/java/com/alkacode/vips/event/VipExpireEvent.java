package com.alkacode.vips.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class VipExpireEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final String vipTypeId;

    public VipExpireEvent(UUID playerUuid, String vipTypeId) {
        this.playerUuid = playerUuid;
        this.vipTypeId = vipTypeId;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getVipTypeId() { return vipTypeId; }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
