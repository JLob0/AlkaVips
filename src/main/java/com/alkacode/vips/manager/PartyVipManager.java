package com.alkacode.vips.manager;

import com.alkacode.vips.config.ConfigManager;
import com.alkacode.vips.event.PartyVipGoalEvent;
import com.alkacode.vips.storage.VipsRepository;
import com.alkacode.vips.util.CommandUtil;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class PartyVipManager {

    private final VipsRepository database;
    private final ConfigManager configManager;
    private final AtomicReference<Double> progress = new AtomicReference<>(0.0);

    public PartyVipManager(VipsRepository database, ConfigManager configManager) {
        this.database = database;
        this.configManager = configManager;
    }

    public void load() {
        progress.set(database.loadPartyProgressSync());
    }

    public boolean isEnabled() {
        return configManager.config().getBoolean("party-vip.enabled", true);
    }

    public double getGoal() {
        return configManager.config().getDouble("party-vip.goal", 1000.0);
    }

    public double getProgress() {
        return progress.get();
    }

    public double getPercentage() {
        double goal = getGoal();
        return goal <= 0 ? 0 : Math.min(100.0, (getProgress() / goal) * 100.0);
    }

    public void addProgress(double amount) {
        if (!isEnabled() || amount <= 0) {
            return;
        }
        double updated = progress.updateAndGet(current -> current + amount);
        database.savePartyProgress(updated);
        double goal = getGoal();
        if (updated >= goal) {
            triggerGoalReached(goal, updated);
        }
    }

    private void triggerGoalReached(double goal, double reachedProgress) {
        progress.set(0.0);
        database.savePartyProgress(0.0);

        Bukkit.getPluginManager().callEvent(new PartyVipGoalEvent(goal, reachedProgress));

        for (String command : configManager.config().getStringList("party-vip.reward-commands")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        CommandUtil.substitute(command, Map.of("player", player.getName())));
            }
        }

        String announce = configManager.config().getString("party-vip.announce", "");
        if (!announce.isBlank()) {
            Bukkit.broadcast(TextUtil.parse(announce, Map.of()));
        }
    }
}
