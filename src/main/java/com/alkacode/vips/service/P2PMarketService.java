package com.alkacode.vips.service;

import com.alkacode.vips.hook.AlkaEconomyHook;
import com.alkacode.vips.hook.DiscordWebhook;
import com.alkacode.vips.manager.PlayerVipManager;
import com.alkacode.vips.manager.VipTypeManager;
import com.alkacode.vips.manager.WalletManager;
import com.alkacode.vips.model.PlayerVip;
import com.alkacode.vips.model.VipType;
import com.alkacode.vips.model.enums.VipStatus;
import com.alkacode.vips.storage.VipsRepository;
import com.alkacode.vips.util.TextUtil;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mercado P2P de VIP - diferente do marketplace de KEYS ja existente
 * (KeyManager/MarketplaceService, que vende itens fisicos NAO usados): aqui o
 * jogador anuncia a propria assinatura ATIVA (com o tempo restante dela) e outro
 * jogador compra, recebendo o tempo restante transferido pra conta dele. Sem
 * "escrow" no sentido literal - o vendedor continua com o VIP ativo normalmente ate
 * alguem comprar (ele nao perde o uso enquanto o anuncio esta no ar).
 */
public final class P2PMarketService {

    private final VipsRepository database;
    private final PlayerVipManager playerVipManager;
    private final VipTypeManager vipTypeManager;
    private final AlkaEconomyHook economyHook;
    private final WalletManager walletManager;
    private final DiscordWebhook discordWebhook;
    private final double saleTaxPercent;

    public P2PMarketService(VipsRepository database, PlayerVipManager playerVipManager, VipTypeManager vipTypeManager,
                             AlkaEconomyHook economyHook, WalletManager walletManager, DiscordWebhook discordWebhook,
                             double saleTaxPercent) {
        this.database = database;
        this.playerVipManager = playerVipManager;
        this.vipTypeManager = vipTypeManager;
        this.economyHook = economyHook;
        this.walletManager = walletManager;
        this.discordWebhook = discordWebhook;
        this.saleTaxPercent = saleTaxPercent;
    }

    public enum ListResult { SUCCESS, NO_VIP_SELECTED, PERMANENT_NOT_SELLABLE, ALREADY_LISTED, NOT_SELLABLE_TIER }

    public ListResult list(Player seller, PlayerVip vip, double price, String currency) {
        if (vip == null || vip.status() != VipStatus.ACTIVE) {
            return ListResult.NO_VIP_SELECTED;
        }
        if (vip.isPermanent()) {
            return ListResult.PERMANENT_NOT_SELLABLE;
        }
        VipType type = vipTypeManager.get(vip.vipTypeId());
        if (type != null && !type.allowSell()) {
            return ListResult.NOT_SELLABLE_TIER;
        }
        for (VipsRepository.P2PListing existing : database.loadAllListingsSync()) {
            if (existing.playerVipId() == vip.id()) {
                return ListResult.ALREADY_LISTED;
            }
        }
        database.insertListingSync(new VipsRepository.P2PListing(-1, seller.getUniqueId(), vip.id(), vip.vipTypeId(),
                vip.remainingMillis(), price, currency, System.currentTimeMillis()));

        if (discordWebhook != null) {
            discordWebhook.sendEmbed("p2p-listing", Map.of(
                    "player", seller.getName(),
                    "vip_display", type != null ? TextUtil.plain(type.display()) : vip.vipTypeId(),
                    "price", String.valueOf(price),
                    "currency", currency));
        }
        return ListResult.SUCCESS;
    }

    public enum BuyResult { SUCCESS, NOT_FOUND, NOT_ENOUGH_CURRENCY, SELLER_NO_LONGER_HAS_VIP, CANNOT_BUY_OWN }

    public BuyResult buy(Player buyer, long listingId) {
        VipsRepository.P2PListing listing = database.loadListingSync(listingId);
        if (listing == null) {
            return BuyResult.NOT_FOUND;
        }
        if (listing.sellerUuid().equals(buyer.getUniqueId())) {
            return BuyResult.CANNOT_BUY_OWN;
        }
        if (!economyHook.has(buyer.getUniqueId(), listing.currency(), listing.price())) {
            return BuyResult.NOT_ENOUGH_CURRENCY;
        }
        PlayerVip sellerVip = playerVipManager.getVips(listing.sellerUuid()).stream()
                .filter(v -> v.id() == listing.playerVipId() && v.status() == VipStatus.ACTIVE)
                .findFirst().orElse(null);
        if (sellerVip == null) {
            database.deleteListingSync(listingId);
            return BuyResult.SELLER_NO_LONGER_HAS_VIP;
        }

        economyHook.withdraw(buyer.getUniqueId(), listing.currency(), listing.price());
        double taxed = listing.price() * (1 - saleTaxPercent / 100.0);
        economyHook.deposit(listing.sellerUuid(), listing.currency(), taxed);

        playerVipManager.remove(sellerVip);
        PlayerVip transferred = new PlayerVip(-1, buyer.getUniqueId(), sellerVip.vipTypeId(), "p2p-market", VipStatus.ACTIVE,
                System.currentTimeMillis(), sellerVip.expiresAt(), sellerVip.totalDuration(), false, 0);
        playerVipManager.addVip(transferred);
        database.deleteListingSync(listingId);

        walletManager.recordTransaction(buyer.getUniqueId(), "PURCHASE", listing.currency(), listing.price());
        return BuyResult.SUCCESS;
    }

    public boolean cancel(UUID sellerUuid, long listingId) {
        VipsRepository.P2PListing listing = database.loadListingSync(listingId);
        if (listing == null || !listing.sellerUuid().equals(sellerUuid)) {
            return false;
        }
        database.deleteListingSync(listingId);
        return true;
    }

    public List<VipsRepository.P2PListing> allListings() {
        return database.loadAllListingsSync();
    }
}
