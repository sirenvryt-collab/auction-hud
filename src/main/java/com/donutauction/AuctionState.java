package com.donutauction;

import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AuctionState {

    public static final AuctionState INSTANCE = new AuctionState();

    private static final long DEFAULT_DURATION_MS = 5 * 60 * 1000L;
    private static final long BID_EXTENSION_MS = 30 * 1000L;

    private final List<AuctionItem> items = new ArrayList<>();

    private String currentItemName = "Mystery Crate Key";
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
    }

    public List<AuctionItem> getItems() {
        return Collections.unmodifiableList(items);
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
