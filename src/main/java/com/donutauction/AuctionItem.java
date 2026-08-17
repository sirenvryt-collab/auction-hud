package com.donutauction;

import net.minecraft.item.Item;

/**
 * A single browsable entry in the auction item list.
 *
 * @param displayName the human-readable name shown in the browser
 * @param item        the vanilla item used to render the icon
 * @param startingBid an example/starting bid value shown next to the item
 */
public record AuctionItem(String displayName, Item item, double startingBid) {
}
