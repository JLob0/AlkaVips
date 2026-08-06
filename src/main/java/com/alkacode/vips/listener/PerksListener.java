package com.alkacode.vips.listener;

import com.alkacode.vips.manager.PerksManager;
import com.alkacode.vips.model.VipPerks;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Perks mecanicos que o AlkaVips implementa diretamente (ver [[project-alkavips]]): manter
 * inventario na morte, auto-smelt/auto-pickup ao minerar, placas coloridas e o local de
 * ultima morte usado pelo /vipback. Perks numericos (homes/pets/claim-blocks/etc) nao entram
 * aqui - continuam sendo so permissao concedida via group-command, lida por outro plugin.
 */
public final class PerksListener implements Listener {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final PerksManager perksManager;
    private final List<FurnaceRecipe> furnaceRecipes = new ArrayList<>();

    public PerksListener(PerksManager perksManager) {
        this.perksManager = perksManager;
        indexSmeltingRecipes();
    }

    private void indexSmeltingRecipes() {
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof FurnaceRecipe furnace) {
                furnaceRecipes.add(furnace);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        perksManager.recordDeath(player.getUniqueId(), player.getLocation());

        VipPerks perks = perksManager.perksOf(player.getUniqueId());
        if (perks.keepInventory()) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        VipPerks perks = perksManager.perksOf(player.getUniqueId());
        if (!perks.autoSmelt() && !perks.autoPickup()) {
            return;
        }

        Block block = event.getBlock();
        List<ItemStack> drops = List.copyOf(block.getDrops(player.getInventory().getItemInMainHand()));
        if (drops.isEmpty()) {
            return;
        }

        event.setDropItems(false);
        for (ItemStack drop : drops) {
            ItemStack toGive = perks.autoSmelt() ? smelted(drop) : drop;
            giveOrDrop(player, toGive);
        }
    }

    private ItemStack smelted(ItemStack drop) {
        for (FurnaceRecipe recipe : furnaceRecipes) {
            if (recipe.getInputChoice().test(drop)) {
                ItemStack smelted = recipe.getResult().clone();
                smelted.setAmount(drop.getAmount());
                return smelted;
            }
        }
        return drop;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack leftover : overflow.values()) {
            Item dropped = player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            dropped.setOwner(player.getUniqueId());
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        VipPerks perks = perksManager.perksOf(player.getUniqueId());
        if (!perks.coloredSigns()) {
            return;
        }
        MiniMessage mm = MiniMessage.miniMessage();
        List<net.kyori.adventure.text.Component> lines = event.lines();
        for (int i = 0; i < lines.size(); i++) {
            String raw = PLAIN_TEXT.serialize(lines.get(i));
            if (!raw.isBlank()) {
                event.line(i, mm.deserialize(raw));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (perksManager.isFlying(player.getUniqueId())) {
            perksManager.disableFlight(player);
        }
        perksManager.clear(player.getUniqueId());
    }
}
