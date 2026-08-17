package com.donutauction;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class AuctionState {

    public static final AuctionState INSTANCE = new AuctionState();

    private static final long DEFAULT_DURATION_MS = 5 * 60 * 1000L;
    private static final long BID_EXTENSION_MS = 30 * 1000L;

    private final List<AuctionItem> items = new ArrayList<>();

    private String currentItemName = "Mystery Crate Key";
    private Item currentItemIcon = null;
    private String highestBidder = "Nobody yet";
    private double highestBid = 0;
    private double minimumBid = 0;
    private long auctionEndTimeMillis;
    private long durationMillis = DEFAULT_DURATION_MS;

    private boolean active = false;

    private AuctionState() {
        seedExampleItems();
    }

    public synchronized void start(String itemName, double minimumBid, int durationSec) {
        this.currentItemName = itemName;
        this.currentItemIcon = resolveItemIcon(itemName);
        this.minimumBid = Math.max(0, minimumBid);
        this.durationMillis = Math.max(1, durationSec) * 1000L;
        this.active = true;
        resetTimer();
    }

    public synchronized void stop() {
        this.active = false;
    }

    public synchronized boolean isActive() {
        return active && !isEnded();
    }

    public synchronized double getMinimumBid() {
        return minimumBid;
    }

    public synchronized void registerBid(String bidderName, double amount) {
        if (!active || amount <= 0 || amount < minimumBid) {
            return;
        }
        if (amount > highestBid) {
            highestBid = amount;
            highestBidder = bidderName;
            extendIfNearEnd();
        }
    }

    private void extendIfNearEnd() {
        long now = System.currentTimeMillis();
        long remaining = auctionEndTimeMillis - now;
        if (remaining < BID_EXTENSION_MS) {
            auctionEndTimeMillis = now + BID_EXTENSION_MS;
        }
    }

    public synchronized void resetTimer() {
        auctionEndTimeMillis = System.currentTimeMillis() + durationMillis;
        highestBid = 0;
        highestBidder = "Nobody yet";
    }

    public synchronized long getRemainingMillis() {
        return Math.max(0, auctionEndTimeMillis - System.currentTimeMillis());
    }

    public synchronized boolean isEnded() {
        return getRemainingMillis() <= 0;
    }

    public synchronized String getHighestBidder() {
        return highestBidder;
    }

    public synchronized double getHighestBid() {
        return highestBid;
    }

    public synchronized String getCurrentItemName() {
        return currentItemName;
    }

    public synchronized void setCurrentItemName(String name) {
        this.currentItemName = name;
        this.currentItemIcon = resolveItemIcon(name);
    }

    /**
     * The vanilla item icon to draw next to the item name, if the typed
     * name matched a real Minecraft item (or one of the browsable example
     * items). Null if no match was found - the HUD just won't draw an icon.
     */
    public synchronized Item getCurrentItemIcon() {
        return currentItemIcon;
    }

    public List<AuctionItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Tries to resolve a typed item name to a real Minecraft item, so the
     * HUD can show its icon. First tries a direct vanilla registry match
     * (e.g. "Elytra" -> minecraft:elytra, "Nether Star" -> minecraft:nether_star),
     * then falls back to matching against the browsable example item list
     * by display name. Returns null if nothing matches.
     */
    private Item resolveItemIcon(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String normalized = name.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replaceAll("[^a-z0-9_]", "");

        if (!normalized.isEmpty()) {
            Identifier id = Identifier.of("minecraft", normalized);
            Item registryMatch = Registries.ITEM.get(id);
            if (registryMatch != Items.AIR) {
                return registryMatch;
            }
        }

        String query = name.trim().toLowerCase(Locale.ROOT);

        for (AuctionItem item : items) {
            if (item.displayName().toLowerCase(Locale.ROOT).equals(query)) {
                return item.item();
            }
        }

        for (AuctionItem item : items) {
            String itemName = item.displayName().toLowerCase(Locale.ROOT);
            if (itemName.contains(query) || query.contains(itemName)) {
                return item.item();
            }
        }

        return null;
    }

    private void seedExampleItems() {
        items.add(new AuctionItem("Netherite Upgrade Template", Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 250_000));
        items.add(new AuctionItem("Totem of Undying", Items.TOTEM_OF_UNDYING, 500_000));
        items.add(new AuctionItem("Enchanted Golden Apple", Items.ENCHANTED_GOLDEN_APPLE, 750_000));
        items.add(new AuctionItem("Nether Star", Items.NETHER_STAR, 1_200_000));
        items.add(new AuctionItem("Elytra", Items.ELYTRA, 3_000_000));
        items.add(new AuctionItem("Dragon Egg", Items.DRAGON_EGG, 15_000_000));
        items.add(new AuctionItem("Trident", Items.TRIDENT, 900_000));
        items.add(new AuctionItem("Mace", Items.MACE, 4_500_000));
        items.add(new AuctionItem("Diamond Block", Items.DIAMOND_BLOCK, 40_000));
        items.add(new AuctionItem("Netherite Block", Items.NETHERITE_BLOCK, 380_000));
        items.add(new AuctionItem("Beacon", Items.BEACON, 220_000));
        items.add(new AuctionItem("Shulker Box", Items.SHULKER_BOX, 60_000));
        items.add(new AuctionItem("Enchanted Book (Mending)", Items.ENCHANTED_BOOK, 180_000));
        items.add(new AuctionItem("Wither Skeleton Skull", Items.WITHER_SKELETON_SKULL, 95_000));
        items.add(new AuctionItem("Ancient Debris", Items.ANCIENT_DEBRIS, 25_000));
    }
}
