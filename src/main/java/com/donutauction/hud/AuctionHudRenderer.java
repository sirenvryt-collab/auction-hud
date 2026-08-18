package com.donutauction.hud;

import com.donutauction.AuctionState;
import com.donutauction.ServerDetector;
import com.donutauction.gui.AuctionBrowserScreen;
import com.donutauction.util.MoneyFormat;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class AuctionHudRenderer implements HudElement {

    public static boolean hudVisible = true;

    private static final int PANEL_WIDTH = 210;
    private static final int PANEL_HEIGHT = 92;
    private static final int MARGIN = 8;
    private static final int ICON_SIZE = 16;

    // Colors (blue theme)
    private static final int COLOR_PANEL_BG = 0xCC0A1622;
    private static final int COLOR_PANEL_BORDER = 0xFF3B82F6;
    private static final int COLOR_TITLE_BG = 0xFF3B82F6;
    private static final int COLOR_TITLE_TEXT = 0xFFF5F9FF;
    private static final int COLOR_LABEL = 0xFFAAAAAA;
    private static final int COLOR_BIDDER = 0xFF7EC8FF;
    private static final int COLOR_BID = 0xFF55FF7F;
    private static final int COLOR_TIME_SAFE = 0xFF55FF7F;
    private static final int COLOR_TIME_WARN = 0xFFFFC107;
    private static final int COLOR_TIME_CRITICAL = 0xFFFF5555;

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!hudVisible || !ServerDetector.isDonutSmp()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        if (client.currentScreen instanceof AuctionBrowserScreen) {
            return;
        }

        AuctionState state = AuctionState.INSTANCE;

        if (!state.isActive()) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;

        int screenWidth = client.getWindow().getScaledWidth();
        int x = screenWidth - PANEL_WIDTH - MARGIN;
        int y = MARGIN;

        drawPanel(context, textRenderer, state, x, y);
    }

    private void drawPanel(DrawContext context, TextRenderer textRenderer, AuctionState state, int x, int y) {
        context.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, COLOR_PANEL_BG);
        context.drawStrokedRectangle(x, y, PANEL_WIDTH, PANEL_HEIGHT, COLOR_PANEL_BORDER);

        int titleHeight = 16;
        context.fill(x, y, x + PANEL_WIDTH, y + titleHeight, COLOR_TITLE_BG);
        Text title = Text.literal("Current Auction").formatted(Formatting.BOLD);
        context.drawText(textRenderer, title, x + 6, y + 4, COLOR_TITLE_TEXT, false);

        int contentX = x + 8;
        int contentY = y + titleHeight + 6;
        int lineHeight = textRenderer.fontHeight + 4;

        // Item name (+ icon, if the typed name matched a real Minecraft item).
        int itemTextX = contentX;
        int itemMaxWidth = PANEL_WIDTH - 16;
        int itemRowHeight = lineHeight;

        Item icon = state.getCurrentItemIcon();
        if (icon != null) {
            context.drawItem(new ItemStack(icon), contentX, contentY - 3);
            itemTextX = contentX + ICON_SIZE + 4;
            itemMaxWidth -= (ICON_SIZE + 4);
            itemRowHeight = Math.max(lineHeight, ICON_SIZE + 2);
        }

        String itemLine = trimToWidth(textRenderer, state.getCurrentItemName(), itemMaxWidth);
        context.drawText(textRenderer, Text.literal(itemLine), itemTextX, contentY, 0xFFFFFFFF, true);
        contentY += itemRowHeight;

        // Countdown timer.
        long remainingMs = state.getRemainingMillis();
        String timeStr = formatDuration(remainingMs);
        int timeColor = pickTimeColor(remainingMs);

        Text endsInLabel = Text.literal("Ends in: ").formatted(Formatting.GRAY);
        context.drawText(textRenderer, endsInLabel, contentX, contentY, COLOR_LABEL, true);
        int labelWidth = textRenderer.getWidth(endsInLabel);
        context.drawText(textRenderer, Text.literal(timeStr), contentX + labelWidth, contentY, timeColor, true);
        contentY += lineHeight;

        // Minimum bid.
        Text minBidLabel = Text.literal("Min Bid: ");
        context.drawText(textRenderer, minBidLabel, contentX, contentY, COLOR_LABEL, true);
        int minBidLabelWidth = textRenderer.getWidth(minBidLabel);
        double minimumBid = state.getMinimumBid();
        String minBidStr = minimumBid > 0 ? "$" + MoneyFormat.format(minimumBid) : "None";
        context.drawText(textRenderer, Text.literal(minBidStr), contentX + minBidLabelWidth, contentY, 0xFFFFFFFF, true);
        contentY += lineHeight;

        // Highest bidder.
        Text bidderLabel = Text.literal("Top Bidder: ");
        context.drawText(textRenderer, bidderLabel, contentX, contentY, COLOR_LABEL, true);
        int bidderLabelWidth = textRenderer.getWidth(bidderLabel);
        String bidderName = trimToWidth(textRenderer, state.getHighestBidder(), PANEL_WIDTH - 16 - bidderLabelWidth);
        context.drawText(textRenderer, Text.literal(bidderName), contentX + bidderLabelWidth, contentY, COLOR_BIDDER, true);
        contentY += lineHeight;

        // Highest bid amount.
        Text bidLabel = Text.literal("Bid: ");
        context.drawText(textRenderer, bidLabel, contentX, contentY, COLOR_LABEL, true);
        int bidLabelWidth = textRenderer.getWidth(bidLabel);
        String bidStr = "$" + MoneyFormat.format(state.getHighestBid());
        context.drawText(textRenderer, Text.literal(bidStr), contentX + bidLabelWidth, contentY, COLOR_BID, true);
    }

    private static int pickTimeColor(long remainingMs) {
        if (remainingMs <= 10_000) {
            return COLOR_TIME_CRITICAL;
        }
        if (remainingMs <= 30_000) {
            return COLOR_TIME_WARN;
        }
        return COLOR_TIME_SAFE;
    }

    private static String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static String trimToWidth(TextRenderer textRenderer, String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String trimmed = textRenderer.trimToWidth(text, maxWidth - textRenderer.getWidth("..."));
        return trimmed + "...";
    }
}
