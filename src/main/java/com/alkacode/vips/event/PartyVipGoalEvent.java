package com.alkacode.vips.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PartyVipGoalEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final double goal;
    private final double progress;

    public PartyVipGoalEvent(double goal, double progress) {
        this.goal = goal;
        this.progress = progress;
    }

    public double getGoal() { return goal; }
    public double getProgress() { return progress; }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
