package com.alkacode.vips.event;

import com.alkacode.vips.model.VipType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class VipUpgradeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final VipType from;
    private final VipType to;

    public VipUpgradeEvent(Player player, VipType from, VipType to) {
        this.player = player;
        this.from = from;
        this.to = to;
    }

    public Player getPlayer() { return player; }
    public VipType getFrom() { return from; }
    public VipType getTo() { return to; }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
