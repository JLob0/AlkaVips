package com.alkacode.vips.event;

import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class VipActivateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final VipType vipType;
    private final PlayerVip playerVip;
    private final boolean accumulated;

    public VipActivateEvent(Player player, VipType vipType, PlayerVip playerVip, boolean accumulated) {
        this.player = player;
        this.vipType = vipType;
        this.playerVip = playerVip;
        this.accumulated = accumulated;
    }

    public Player getPlayer() { return player; }
    public VipType getVipType() { return vipType; }
    public PlayerVip getPlayerVip() { return playerVip; }
    public boolean isAccumulated() { return accumulated; }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
