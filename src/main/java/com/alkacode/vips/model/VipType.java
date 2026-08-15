package com.alkacode.vips.model;

import com.alkacode.vips.model.enums.TimeType;

import java.util.List;
import java.util.Map;

public final class VipType {

    private final String id;
    private final String display;
    private final String prefix;
    private final TimeType timeType;
    private final double credit;
    private final double partyVipValue;
    private final boolean allowSell;
    private final boolean activationMenu;

    private final IconTemplate icon;
    private final IconTemplate keyPreview;
    private final IconTemplate activePreview;
    private final IconTemplate expiredPreview;

    private final String announceSound;
    private final String announceEffect;
    private final String announceActionBar;
    private final String announceTitle;
    private final String announceChat;
    private final String announceChatPrivate;

    private final List<String> groupAddCmds;
    private final List<String> groupRemoveCmds;
    private final List<String> groupAddTempCmds;
    private final List<String> groupRemoveTempCmds;

    // Nome do grupo do LuckPerms usado SO pra ler as permissoes dele (lore "permissoes
    // deste vip", ver VipsServices#permissionLoreLines/PermissionLoreUtil) - independente
    // dos comandos livres acima (groupAddCmds etc), que podem ter qualquer formato de
    // comando. Vazio/nao configurado = lore de permissoes fica vazia pra esse tier.
    private final String permissionGroup;

    private final List<String> activationCommands;
    private final List<VipItem> activationItems;

    // Presente de ativacao (uma vez por conta, pra sempre - ver ActivationService e
    // VipsRepository#hasClaimedActivationBonusSync) - deliberadamente separado do
    // activationItems/activationCommands acima, que rodam TODA ativacao independente
    // de duracao. Nasceu no AlkaKits (2026-08-13) mas voltou pra ca no mesmo dia:
    // precisa funcionar mesmo num servidor que so tenha o AlkaVips instalado.
    private final int activationBonusMinDurationDays;
    private final List<VipItem> activationBonusItems;
    private final String activationBonusSound;
    private final String activationBonusEffect;
    private final String activationBonusTitle;
    private final String activationBonusActionBar;
    private final String activationBonusChat;

    private final String upgradeTo;
    private final Map<String, Double> upgradePrices;
    private final List<String> upgradeCommands;
    private final String upgradeMessage;
    private final String upgradeBroadcast;

    private final boolean discordWebhookEnabled;
    private final String discordEmbedId;

    private final VipPerks perks;
    private final int order;
    private final boolean tagPermanent;

    // Integracao com hooks de plugins de terceiros (com.alkacode.vips.hook) - todos
    // opcionais, sem efeito se o plugin correspondente nao estiver instalado.
    private final double mcmmoXpBoost;
    private final int mcmmoXpFlat;
    private final String mythicDrop;
    private final int battlepassXp;
    private final List<String> pets;

    // Acoes executaveis configuraveis por evento (ver util/EventActionExecutor) -
    // formato "[tipo] conteudo" por linha, generico o suficiente pra substituir
    // qualquer "title com fade" hardcoded por config.
    private final List<String> eventsOnActivate;
    private final List<String> eventsOnExpire;
    private final List<String> eventsOnEnable;
    private final List<String> eventsOnDisable;

    // "VIP Legacy" (heranca com grace period) e "VIP Solidario" (boost de servidor) -
    // null quando o tier nao tem essa feature configurada.
    private final LegacyConfig legacy;
    private final ServerBoostConfig serverBoost;

    // Itens do AlkaItems entregues na primeira ativacao (nao-acumulada) do tier - ver
    // hook/AlkaItemsHook. Vazio/AlkaItems ausente = VIP funciona normal, sem itens.
    private final List<String> itemRewards;

    private VipType(Builder builder) {
        this.id = builder.id;
        this.display = builder.display;
        this.prefix = builder.prefix;
        this.timeType = builder.timeType;
        this.credit = builder.credit;
        this.partyVipValue = builder.partyVipValue;
        this.allowSell = builder.allowSell;
        this.activationMenu = builder.activationMenu;
        this.icon = builder.icon;
        this.keyPreview = builder.keyPreview;
        this.activePreview = builder.activePreview;
        this.expiredPreview = builder.expiredPreview;
        this.announceSound = builder.announceSound;
        this.announceEffect = builder.announceEffect;
        this.announceActionBar = builder.announceActionBar;
        this.announceTitle = builder.announceTitle;
        this.announceChat = builder.announceChat;
        this.announceChatPrivate = builder.announceChatPrivate;
        this.groupAddCmds = builder.groupAddCmds;
        this.groupRemoveCmds = builder.groupRemoveCmds;
        this.groupAddTempCmds = builder.groupAddTempCmds;
        this.groupRemoveTempCmds = builder.groupRemoveTempCmds;
        this.permissionGroup = builder.permissionGroup;
        this.activationCommands = builder.activationCommands;
        this.activationItems = builder.activationItems;
        this.activationBonusMinDurationDays = builder.activationBonusMinDurationDays;
        this.activationBonusItems = builder.activationBonusItems;
        this.activationBonusSound = builder.activationBonusSound;
        this.activationBonusEffect = builder.activationBonusEffect;
        this.activationBonusTitle = builder.activationBonusTitle;
        this.activationBonusActionBar = builder.activationBonusActionBar;
        this.activationBonusChat = builder.activationBonusChat;
        this.upgradeTo = builder.upgradeTo;
        this.upgradePrices = builder.upgradePrices;
        this.upgradeCommands = builder.upgradeCommands;
        this.upgradeMessage = builder.upgradeMessage;
        this.upgradeBroadcast = builder.upgradeBroadcast;
        this.discordWebhookEnabled = builder.discordWebhookEnabled;
        this.discordEmbedId = builder.discordEmbedId;
        this.perks = builder.perks;
        this.order = builder.order;
        this.tagPermanent = builder.tagPermanent;
        this.mcmmoXpBoost = builder.mcmmoXpBoost;
        this.mcmmoXpFlat = builder.mcmmoXpFlat;
        this.mythicDrop = builder.mythicDrop;
        this.battlepassXp = builder.battlepassXp;
        this.pets = builder.pets;
        this.eventsOnActivate = builder.eventsOnActivate;
        this.eventsOnExpire = builder.eventsOnExpire;
        this.eventsOnEnable = builder.eventsOnEnable;
        this.eventsOnDisable = builder.eventsOnDisable;
        this.legacy = builder.legacy;
        this.serverBoost = builder.serverBoost;
        this.itemRewards = builder.itemRewards;
    }

    public String id() { return id; }
    public String display() { return display; }
    public String prefix() { return prefix; }
    public TimeType timeType() { return timeType; }
    public double credit() { return credit; }
    public double partyVipValue() { return partyVipValue; }
    public boolean allowSell() { return allowSell; }
    public boolean activationMenu() { return activationMenu; }
    public IconTemplate icon() { return icon; }
    public IconTemplate keyPreview() { return keyPreview; }
    public IconTemplate activePreview() { return activePreview; }
    public IconTemplate expiredPreview() { return expiredPreview; }
    public String announceSound() { return announceSound; }
    public String announceEffect() { return announceEffect; }
    public String announceActionBar() { return announceActionBar; }
    public String announceTitle() { return announceTitle; }
    public String announceChat() { return announceChat; }
    public String announceChatPrivate() { return announceChatPrivate; }
    public List<String> groupAddCmds() { return groupAddCmds; }
    public List<String> groupRemoveCmds() { return groupRemoveCmds; }
    public List<String> groupAddTempCmds() { return groupAddTempCmds; }
    public List<String> groupRemoveTempCmds() { return groupRemoveTempCmds; }
    public String permissionGroup() { return permissionGroup; }
    public List<String> activationCommands() { return activationCommands; }
    public List<VipItem> activationItems() { return activationItems; }
    public boolean hasActivationBonus() { return !activationBonusItems.isEmpty(); }
    public int activationBonusMinDurationDays() { return activationBonusMinDurationDays; }
    public List<VipItem> activationBonusItems() { return activationBonusItems; }
    public String activationBonusSound() { return activationBonusSound; }
    public String activationBonusEffect() { return activationBonusEffect; }
    public String activationBonusTitle() { return activationBonusTitle; }
    public String activationBonusActionBar() { return activationBonusActionBar; }
    public String activationBonusChat() { return activationBonusChat; }
    public boolean hasUpgrade() { return upgradeTo != null && !upgradeTo.isBlank(); }
    public String upgradeTo() { return upgradeTo; }
    public Map<String, Double> upgradePrices() { return upgradePrices; }
    public List<String> upgradeCommands() { return upgradeCommands; }
    public String upgradeMessage() { return upgradeMessage; }
    public String upgradeBroadcast() { return upgradeBroadcast; }
    public boolean discordWebhookEnabled() { return discordWebhookEnabled; }
    public String discordEmbedId() { return discordEmbedId; }
    public VipPerks perks() { return perks; }
    public int getOrder() { return order; }
    public boolean isTagPermanent() { return tagPermanent; }
    public double mcmmoXpBoost() { return mcmmoXpBoost; }
    public int mcmmoXpFlat() { return mcmmoXpFlat; }
    public String mythicDrop() { return mythicDrop; }
    public int battlepassXp() { return battlepassXp; }
    public List<String> pets() { return pets; }
    public List<String> eventsOnActivate() { return eventsOnActivate; }
    public List<String> eventsOnExpire() { return eventsOnExpire; }
    public List<String> eventsOnEnable() { return eventsOnEnable; }
    public List<String> eventsOnDisable() { return eventsOnDisable; }
    public LegacyConfig legacy() { return legacy; }
    public ServerBoostConfig serverBoost() { return serverBoost; }
    public List<String> itemRewards() { return itemRewards; }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String display = "";
        private String prefix = "";
        private TimeType timeType = TimeType.ONLINE;
        private double credit;
        private double partyVipValue;
        private boolean allowSell;
        private boolean activationMenu;
        private IconTemplate icon;
        private IconTemplate keyPreview;
        private IconTemplate activePreview;
        private IconTemplate expiredPreview;
        private String announceSound = "";
        private String announceEffect = "";
        private String announceActionBar = "";
        private String announceTitle = "";
        private String announceChat = "";
        private String announceChatPrivate = "";
        private List<String> groupAddCmds = List.of();
        private List<String> groupRemoveCmds = List.of();
        private List<String> groupAddTempCmds = List.of();
        private List<String> groupRemoveTempCmds = List.of();
        private String permissionGroup = "";
        private List<String> activationCommands = List.of();
        private List<VipItem> activationItems = List.of();
        private int activationBonusMinDurationDays = 0;
        private List<VipItem> activationBonusItems = List.of();
        private String activationBonusSound = "";
        private String activationBonusEffect = "";
        private String activationBonusTitle = "";
        private String activationBonusActionBar = "";
        private String activationBonusChat = "";
        private String upgradeTo = "";
        private Map<String, Double> upgradePrices = Map.of();
        private List<String> upgradeCommands = List.of();
        private String upgradeMessage = "";
        private String upgradeBroadcast = "";
        private boolean discordWebhookEnabled;
        private String discordEmbedId = "";
        private VipPerks perks = VipPerks.NONE;
        private int order;
        private boolean tagPermanent;
        private double mcmmoXpBoost = 1.0;
        private int mcmmoXpFlat = 0;
        private String mythicDrop = "";
        private int battlepassXp = 0;
        private List<String> pets = List.of();
        private List<String> eventsOnActivate = List.of();
        private List<String> eventsOnExpire = List.of();
        private List<String> eventsOnEnable = List.of();
        private List<String> eventsOnDisable = List.of();
        private LegacyConfig legacy;
        private ServerBoostConfig serverBoost;
        private List<String> itemRewards = List.of();

        private Builder(String id) {
            this.id = id;
        }

        public Builder display(String display) { this.display = display; return this; }
        public Builder prefix(String prefix) { this.prefix = prefix; return this; }
        public Builder timeType(TimeType timeType) { this.timeType = timeType; return this; }
        public Builder credit(double credit) { this.credit = credit; return this; }
        public Builder partyVipValue(double partyVipValue) { this.partyVipValue = partyVipValue; return this; }
        public Builder allowSell(boolean allowSell) { this.allowSell = allowSell; return this; }
        public Builder activationMenu(boolean activationMenu) { this.activationMenu = activationMenu; return this; }
        public Builder icon(IconTemplate icon) { this.icon = icon; return this; }
        public Builder keyPreview(IconTemplate keyPreview) { this.keyPreview = keyPreview; return this; }
        public Builder activePreview(IconTemplate activePreview) { this.activePreview = activePreview; return this; }
        public Builder expiredPreview(IconTemplate expiredPreview) { this.expiredPreview = expiredPreview; return this; }
        public Builder announceSound(String announceSound) { this.announceSound = announceSound; return this; }
        public Builder announceEffect(String announceEffect) { this.announceEffect = announceEffect; return this; }
        public Builder announceActionBar(String announceActionBar) { this.announceActionBar = announceActionBar; return this; }
        public Builder announceTitle(String announceTitle) { this.announceTitle = announceTitle; return this; }
        public Builder announceChat(String announceChat) { this.announceChat = announceChat; return this; }
        public Builder announceChatPrivate(String announceChatPrivate) { this.announceChatPrivate = announceChatPrivate; return this; }
        public Builder groupAddCmds(List<String> groupAddCmds) { this.groupAddCmds = groupAddCmds; return this; }
        public Builder groupRemoveCmds(List<String> groupRemoveCmds) { this.groupRemoveCmds = groupRemoveCmds; return this; }
        public Builder groupAddTempCmds(List<String> groupAddTempCmds) { this.groupAddTempCmds = groupAddTempCmds; return this; }
        public Builder groupRemoveTempCmds(List<String> groupRemoveTempCmds) { this.groupRemoveTempCmds = groupRemoveTempCmds; return this; }
        public Builder permissionGroup(String permissionGroup) { this.permissionGroup = permissionGroup; return this; }
        public Builder activationCommands(List<String> activationCommands) { this.activationCommands = activationCommands; return this; }
        public Builder activationItems(List<VipItem> activationItems) { this.activationItems = activationItems; return this; }
        public Builder activationBonusMinDurationDays(int activationBonusMinDurationDays) { this.activationBonusMinDurationDays = activationBonusMinDurationDays; return this; }
        public Builder activationBonusItems(List<VipItem> activationBonusItems) { this.activationBonusItems = activationBonusItems; return this; }
        public Builder activationBonusSound(String activationBonusSound) { this.activationBonusSound = activationBonusSound; return this; }
        public Builder activationBonusEffect(String activationBonusEffect) { this.activationBonusEffect = activationBonusEffect; return this; }
        public Builder activationBonusTitle(String activationBonusTitle) { this.activationBonusTitle = activationBonusTitle; return this; }
        public Builder activationBonusActionBar(String activationBonusActionBar) { this.activationBonusActionBar = activationBonusActionBar; return this; }
        public Builder activationBonusChat(String activationBonusChat) { this.activationBonusChat = activationBonusChat; return this; }
        public Builder upgradeTo(String upgradeTo) { this.upgradeTo = upgradeTo; return this; }
        public Builder upgradePrices(Map<String, Double> upgradePrices) { this.upgradePrices = upgradePrices; return this; }
        public Builder upgradeCommands(List<String> upgradeCommands) { this.upgradeCommands = upgradeCommands; return this; }
        public Builder upgradeMessage(String upgradeMessage) { this.upgradeMessage = upgradeMessage; return this; }
        public Builder upgradeBroadcast(String upgradeBroadcast) { this.upgradeBroadcast = upgradeBroadcast; return this; }
        public Builder discordWebhookEnabled(boolean discordWebhookEnabled) { this.discordWebhookEnabled = discordWebhookEnabled; return this; }
        public Builder discordEmbedId(String discordEmbedId) { this.discordEmbedId = discordEmbedId; return this; }
        public Builder perks(VipPerks perks) { this.perks = perks; return this; }
        public Builder order(int order) { this.order = order; return this; }
        public Builder tagPermanent(boolean tagPermanent) { this.tagPermanent = tagPermanent; return this; }
        public Builder mcmmoXpBoost(double mcmmoXpBoost) { this.mcmmoXpBoost = mcmmoXpBoost; return this; }
        public Builder mcmmoXpFlat(int mcmmoXpFlat) { this.mcmmoXpFlat = mcmmoXpFlat; return this; }
        public Builder mythicDrop(String mythicDrop) { this.mythicDrop = mythicDrop; return this; }
        public Builder battlepassXp(int battlepassXp) { this.battlepassXp = battlepassXp; return this; }
        public Builder pets(List<String> pets) { this.pets = pets; return this; }
        public Builder eventsOnActivate(List<String> v) { this.eventsOnActivate = v; return this; }
        public Builder eventsOnExpire(List<String> v) { this.eventsOnExpire = v; return this; }
        public Builder eventsOnEnable(List<String> v) { this.eventsOnEnable = v; return this; }
        public Builder eventsOnDisable(List<String> v) { this.eventsOnDisable = v; return this; }
        public Builder legacy(LegacyConfig legacy) { this.legacy = legacy; return this; }
        public Builder serverBoost(ServerBoostConfig serverBoost) { this.serverBoost = serverBoost; return this; }
        public Builder itemRewards(List<String> v) { this.itemRewards = v; return this; }

        public VipType build() {
            return new VipType(this);
        }
    }
}
