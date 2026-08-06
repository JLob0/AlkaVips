package com.alkacode.vips.service;

import com.alkacode.vips.config.ConfigManager;
import com.alkacode.vips.hook.AlkaEconomyHook;
import com.alkacode.vips.manager.KeyManager;
import com.alkacode.vips.manager.VipTypeManager;
import com.alkacode.vips.model.VipKey;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * So chamado a partir da thread principal (comando, GUI) - usa leitura sincrona do
 * KeyManager (mesmo padrao de fallback bloqueante do EconomyManager) para poder tocar
 * a API do Bukkit (inventario, mensagens, economia) sem trocar de thread no meio do
 * fluxo de compra/venda.
 */
public final class MarketplaceService {

    private final KeyManager keyManager;
    private final VipTypeManager vipTypeManager;
    private final ConfigManager configManager;
    private final AlkaEconomyHook economyHook;

    public MarketplaceService(KeyManager keyManager, VipTypeManager vipTypeManager, ConfigManager configManager,
                               AlkaEconomyHook economyHook) {
        this.keyManager = keyManager;
        this.vipTypeManager = vipTypeManager;
        this.configManager = configManager;
        this.economyHook = economyHook;
    }

    public enum ListResult {
        SUCCESS, NOT_FOUND, ALREADY_USED, NOT_ALLOWED, ALREADY_FOR_SALE, INVALID_CURRENCY, INVALID_PRICE
    }

    public enum BuyResult {
        SUCCESS, NOT_FOUND, NOT_FOR_SALE, BUY_OWN, INSUFFICIENT_FUNDS
    }

    public enum CancelResult {
        SUCCESS, NOT_FOUND, NOT_FOR_SALE, NOT_OWNER
    }

    public ListResult listForSale(Player seller, String code, String currency, double price) {
        VipKey key = keyManager.findSync(code);
        if (key == null) {
            return ListResult.NOT_FOUND;
        }
        if (key.used() || key.bonus()) {
            return ListResult.ALREADY_USED;
        }
        VipType vipType = vipTypeManager.get(key.vipTypeId());
        if (vipType == null || !vipType.allowSell()) {
            return ListResult.NOT_ALLOWED;
        }
        if (key.forSale()) {
            return ListResult.ALREADY_FOR_SALE;
        }
        if (economyHook == null || !economyHook.isValidCurrency(currency)) {
            return ListResult.INVALID_CURRENCY;
        }
        if (price <= 0) {
            return ListResult.INVALID_PRICE;
        }
        key.forSale(true);
        key.sellerUuid(seller.getUniqueId());
        key.economyProvider(currency.toLowerCase());
        key.sellPrice(price);
        keyManager.save(key);
        return ListResult.SUCCESS;
    }

    public CancelResult cancelSale(Player seller, String code) {
        VipKey key = keyManager.findSync(code);
        if (key == null) {
            return CancelResult.NOT_FOUND;
        }
        if (!key.forSale()) {
            return CancelResult.NOT_FOR_SALE;
        }
        if (!seller.getUniqueId().equals(key.sellerUuid())) {
            return CancelResult.NOT_OWNER;
        }
        key.forSale(false);
        key.sellerUuid(null);
        key.economyProvider(null);
        key.sellPrice(0);
        keyManager.save(key);
        return CancelResult.SUCCESS;
    }

    public BuyResult buy(Player buyer, String code) {
        VipKey key = keyManager.findSync(code);
        if (key == null) {
            return BuyResult.NOT_FOUND;
        }
        if (!key.forSale()) {
            return BuyResult.NOT_FOR_SALE;
        }
        if (buyer.getUniqueId().equals(key.sellerUuid())) {
            return BuyResult.BUY_OWN;
        }
        if (economyHook == null || !economyHook.withdraw(buyer.getUniqueId(), key.economyProvider(), key.sellPrice())) {
            return BuyResult.INSUFFICIENT_FUNDS;
        }
        double tax = configManager.config().getDouble("marketplace.sale-tax", 0.0);
        double proceeds = key.sellPrice() * (1 - Math.clamp(tax, 0.0, 1.0));
        UUID sellerUuid = key.sellerUuid();
        economyHook.deposit(sellerUuid, key.economyProvider(), proceeds);

        key.forSale(false);
        key.sellerUuid(null);
        key.economyProvider(null);
        key.sellPrice(0);
        keyManager.save(key);

        VipType vipType = vipTypeManager.get(key.vipTypeId());
        if (vipType != null) {
            buyer.getInventory().addItem(keyManager.buildItem(key, vipType));
        }
        Player seller = Bukkit.getPlayer(sellerUuid);
        if (seller != null) {
            String raw = configManager.prefix() + configManager.message("market.sold");
            seller.sendMessage(TextUtil.parse(raw, java.util.Map.of(
                    "code", key.id(),
                    "price", economyHook.format(proceeds)
            )));
        }
        return BuyResult.SUCCESS;
    }

    public CompletableFuture<List<VipKey>> listings() {
        return keyManager.loadForSale();
    }
}
