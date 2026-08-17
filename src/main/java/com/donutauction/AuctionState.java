package com.donutauction;

import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds all live state for the auction HUD: the countdown, the current
 * highest bidder/bid, and the list of items shown in the browser screen.
 * <p>
 * This is a simple in-memory singleton since the mod is fully client-side -
 * nothing here is persisted or synced with a real server. The timer and
 * item list use reasonable placeholder logic that can later be replaced
 * with real data (e.g. parsed from a scoreboard, boss bar, or plugin
 * message) once DonutSMP exposes one.
 */
public final class AuctionState {

    public static final AuctionState INSTANCE = new AuctionState();

    /** How long a fresh auction runs for by default. */
    private static final long DEFAULT_DURATION_MS = 5 * 60 * 1000L;

    /** A new top bid within this many ms of the end extends the timer ("anti-snipe"). */
    private static final long BID_EXTENSION_MS = 30 * 1000L;

    private final List<AuctionItem> items = new ArrayList<>();

    private String currentItemName = "Mystery Crate Key";
    private String highestBidder = "Nobody yet";
    private double highestBid = 0;
    private long auctionEndTimeMillis;

    private AuctionState() {
        resetTimer();
        seedExampleItems();
    }

    // ----------------------------------------------------------------
    // Bidding
    // ----------------------------------------------------------------

    /**
     * Called whenever a "<name> paid you $ <amount>" chat message is parsed.
     * Updates the highest bidder if this bid beats the current one, and
     * applies anti-snipe extension if the auction was about to end.
     */
    public synchronized void registerBid(String bidderName, double amount) {
        if (amount <= 0) {
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

    // ----------------------------------------------------------------
    // Timer
    // ----------------------------------------------------------------

    public synchronized void resetTimer() {
        auctionEndTimeMillis = System.currentTimeMillis() + DEFAULT_DURATION_MS;
        highestBid = 0;
        highestBidder = "Nobody yet";
    }

    public synchronized long getRemainingMillis() {
        return Math.max(0, auctionEndTimeMillis - System.currentTimeMillis());
    }

    public synchronized boolean isEnded() {
        return getRemainingMillis() <= 0;
    }

    // ----------------------------------------------------------------
    // Getters
    // ----------------------------------------------------------------

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
    }

    public List<AuctionItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    // ----------------------------------------------------------------
    // Example / placeholder browsable items
    // ----------------------------------------------------------------

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
