package com.donutauction;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AuctionState {

    public static final AuctionState INSTANCE = new AuctionState();

    /** How long a fresh auction runs for by default. */
    private static final long DEFAULT_DURATION_MS = 5 * 60 * 1000L;

    /** Starting bid used for items that aren't in the curated price list. */
    private static final double DEFAULT_STARTING_BID = 10_000;

    private final List<AuctionItem> items = new ArrayList<>();

    private String currentItemName = "Mystery Crate Key";
    private Item currentItemIcon = null;
    private String highestBidder = "Nobody yet";
    private double highestBid = 0;
    private double minimumBid = 0;
    private long auctionEndTimeMillis;
    private long durationMillis = DEFAULT_DURATION_MS;

    private boolean active = false;

    /** Full browsable list (curated items first, then every other vanilla item). Built lazily. */
    private List<AuctionItem> allItems = null;

    /** Result of an auction that just finished, waiting to be announced in chat. */
    private AuctionResult pendingResult = null;

    /** Snapshot of how an auction ended. */
    public record AuctionResult(String itemName, String winner, double winningBid) {
        public boolean hasWinner() {
            return winner != null && winningBid > 0;
        }
    }

    private AuctionState() {
        seedExampleItems();
    }

    public synchronized void start(String itemName, double minimumBid, int durationSec) {
        start(itemName, resolveItemIcon(itemName), minimumBid, durationSec);
    }

    public synchronized void start(String itemName, Item icon, double minimumBid, int durationSec) {
        this.currentItemName = itemName;
        this.currentItemIcon = icon;
        this.pendingResult = null;
        this.minimumBid = Math.max(0, minimumBid);
        this.durationMillis = Math.max(1, durationSec) * 1000L;
        this.active = true;
        resetTimer();
    }

    public synchronized void stop() {
        if (active) {
            finishAuction();
        }
    }

    public synchronized boolean isActive() {
        if (active && isEnded()) {
            finishAuction();
        }
        return active;
    }

    private void finishAuction() {
        active = false;
        pendingResult = new AuctionResult(currentItemName, highestBidder, highestBid);
    }

    /** Returns the result of a just-finished auction exactly once, then null until the next one ends. */
    public synchronized AuctionResult takePendingResult() {
        AuctionResult result = pendingResult;
        pendingResult = null;
        return result;
    }

    public synchronized double getMinimumBid() {
        return minimumBid;
    }

    /**
     * Called whenever a "<name> paid you $ <amount>" chat message is parsed.
     * Updates the highest bidder if this bid beats the current one and
     * meets the minimum. The timer is never touched here - once started it
     * only ever counts down to zero, no matter how many bids come in.
     */
    public synchronized void registerBid(String bidderName, double amount) {
        if (!isActive() || amount <= 0 || amount < minimumBid) {
            return;
        }
        if (amount > highestBid) {
            highestBid = amount;
            highestBidder = bidderName;
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

    public synchronized Item getCurrentItemIcon() {
        return currentItemIcon;
    }

    public synchronized List<AuctionItem> getItems() {
        if (allItems == null) {
            List<AuctionItem> built = new ArrayList<>(items);
            Set<Item> curated = new HashSet<>();
            for (AuctionItem item : items) {
                curated.add(item.item());
            }

            List<AuctionItem> rest = new ArrayList<>();
            for (Item item : Registries.ITEM) {
                if (item == Items.AIR || curated.contains(item)) {
                    continue;
                }
                rest.add(new AuctionItem(item.getName().getString(), item, DEFAULT_STARTING_BID));
            }
            rest.sort((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));
            built.addAll(rest);
            allItems = Collections.unmodifiableList(built);
        }
        return allItems;
    }

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
