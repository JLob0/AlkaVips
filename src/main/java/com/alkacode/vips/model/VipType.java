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

    private final List<String> activationCommands;
    private final List<VipItem> activationItems;

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
        this.activationCommands = builder.activationCommands;
        this.activationItems = builder.activationItems;
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
    public List<String> activationCommands() { return activationCommands; }
    public List<VipItem> activationItems() { return activationItems; }
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
        private List<String> activationCommands = List.of();
        private List<VipItem> activationItems = List.of();
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
        public Builder activationCommands(List<String> activationCommands) { this.activationCommands = activationCommands; return this; }
        public Builder activationItems(List<VipItem> activationItems) { this.activationItems = activationItems; return this; }
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

        public VipType build() {
            return new VipType(this);
        }
    }
}
