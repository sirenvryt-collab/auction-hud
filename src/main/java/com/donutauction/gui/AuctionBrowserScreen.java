package com.donutauction.gui;

import com.donutauction.AuctionItem;
import com.donutauction.AuctionState;
import com.donutauction.util.MoneyFormat;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Full-screen auction item browser: a search/filter bar at the top and a
 * scrollable, hoverable list of items below it. Purely cosmetic/browsing -
 * clicking a row does not currently place a bid, it just highlights it.
 */
public class AuctionBrowserScreen extends Screen {

    private static final int ROW_HEIGHT = 26;
    private static final int LIST_TOP_MARGIN = 62;
    private static final int LIST_BOTTOM_MARGIN = 16;
    private static final int LIST_SIDE_MARGIN = 60;
    private static final int MAX_LIST_WIDTH = 420;

    private TextFieldWidget searchField;
    private String searchQuery = "";
    private double scrollOffset = 0;
    private List<AuctionItem> filteredItems = new ArrayList<>();
    private AuctionItem selectedItem = null;

    public AuctionBrowserScreen() {
        super(Text.literal("Auction Browser"));
    }

    @Override
    protected void init() {
        int fieldWidth = Math.min(320, this.width - 80);
        this.searchField = new TextFieldWidget(
                this.textRenderer,
                this.width / 2 - fieldWidth / 2,
                34,
                fieldWidth,
                20,
                Text.literal("Search")
        );
        this.searchField.setPlaceholder(Text.literal("Search items...").formatted(Formatting.GRAY));
        this.searchField.setMaxLength(64);
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);
        this.setInitialFocus(this.searchField);

        refreshFilter();
    }

    private void onSearchChanged(String text) {
        this.searchQuery = text;
        refreshFilter();
    }

    private void refreshFilter() {
        String query = searchQuery.toLowerCase(Locale.ROOT).trim();
        this.filteredItems = AuctionState.INSTANCE.getItems().stream()
                .filter(item -> query.isEmpty() || item.displayName().toLowerCase(Locale.ROOT).contains(query))
                .collect(Collectors.toList());
        this.scrollOffset = 0;
    }

    // ------------------------------------------------------------
    // Layout helpers
    // ------------------------------------------------------------

    private int listLeft() {
        int desired = this.width / 2 - Math.min(MAX_LIST_WIDTH, this.width - LIST_SIDE_MARGIN * 2) / 2;
        return desired;
    }

    private int listWidth() {
        return Math.min(MAX_LIST_WIDTH, this.width - LIST_SIDE_MARGIN * 2);
    }

    private int listTop() {
        return LIST_TOP_MARGIN;
    }

    private int listBottom() {
        return this.height - LIST_BOTTOM_MARGIN;
    }

    // ------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dim the game world behind the screen.
        this.renderBackground(context, mouseX, mouseY, delta);

        // Header.
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Current Auction Browser").formatted(Formatting.BOLD, Formatting.BLUE),
                this.width / 2,
                12,
                0xFFFFFFFF
        );

        AuctionState state = AuctionState.INSTANCE;
        String subtitle = "Current item: " + state.getCurrentItemName()
                + "   |   Top bid: $" + MoneyFormat.format(state.getHighestBid())
                + " by " + state.getHighestBidder();
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(subtitle).formatted(Formatting.GRAY),
                this.width / 2,
                24,
                0xFFAAAAAA
        );

        int left = listLeft();
        int width = listWidth();
        int top = listTop();
        int bottom = listBottom();

        // List panel background + border.
        context.fill(left, top, left + width, bottom, 0xB0000000);
        context.drawStrokedRectangle(left, top, width, bottom - top, 0xFF3A3A3A);

        // Clip rendering to the list area so rows don't bleed outside it.
        context.enableScissor(left + 1, top + 1, left + width - 1, bottom - 1);

        int rowY = top + 4 - (int) scrollOffset;
        for (AuctionItem item : filteredItems) {
            if (rowY + ROW_HEIGHT >= top && rowY <= bottom) {
                renderRow(context, item, left + 4, rowY, width - 8, mouseX, mouseY);
            }
            rowY += ROW_HEIGHT;
        }

        context.disableScissor();

        if (filteredItems.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("No items match your search.").formatted(Formatting.GRAY),
                    this.width / 2,
                    top + 20,
                    0xFF888888
            );
        }

        // Draw a subtle scrollbar if content overflows.
        drawScrollbar(context, left, width, top, bottom);

        // Footer hint.
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Scroll to browse  |  Type to search  |  Esc to close").formatted(Formatting.DARK_GRAY),
                this.width / 2,
                this.height - 12,
                0xFF777777
        );

        // Draws the search field and any other drawable children.
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderRow(DrawContext context, AuctionItem item, int x, int y, int width, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
        boolean selected = item == selectedItem;

        int bg;
        if (selected) {
            bg = 0x8047A0FF;
        } else if (hovered) {
            bg = 0x60FFFFFF;
        } else {
            bg = 0x25FFFFFF;
        }
        context.fill(x, y, x + width, y + ROW_HEIGHT - 2, bg);
        if (selected) {
            context.drawStrokedRectangle(x, y, width, ROW_HEIGHT - 2, 0xFF47A0FF);
        }

        // Item icon.
        ItemStack stack = new ItemStack(item.item());
        context.drawItem(stack, x + 4, y + 3);

        // Name.
        context.drawText(this.textRenderer, Text.literal(item.displayName()), x + 26, y + 3, 0xFFFFFFFF, true);

        // Starting bid, right-aligned.
        String priceStr = "$" + MoneyFormat.format(item.startingBid());
        int priceWidth = this.textRenderer.getWidth(priceStr);
        context.drawText(
                this.textRenderer,
                Text.literal(priceStr).formatted(Formatting.GREEN),
                x + width - priceWidth - 6,
                y + 3,
                0xFF55FF7F,
                true
        );

        // Small "starting bid" caption under the name.
        context.drawText(
                this.textRenderer,
                Text.literal("Starting bid").formatted(Formatting.DARK_GRAY),
                x + 26,
                y + 14,
                0xFF888888,
                false
        );
    }

    private void drawScrollbar(DrawContext context, int left, int width, int top, int bottom) {
        int contentHeight = filteredItems.size() * ROW_HEIGHT;
        int visibleHeight = bottom - top;
        if (contentHeight <= visibleHeight) {
            return;
        }

        int trackX = left + width - 6;
        int trackHeight = visibleHeight - 8;
        int trackY = top + 4;

        context.fill(trackX, trackY, trackX + 4, trackY + trackHeight, 0x40FFFFFF);

        double maxScroll = contentHeight - visibleHeight;
        double scrollFraction = MathHelper.clamp(scrollOffset / maxScroll, 0, 1);
        int thumbHeight = Math.max(16, (int) ((double) visibleHeight / contentHeight * trackHeight));
        int thumbY = trackY + (int) ((trackHeight - thumbHeight) * scrollFraction);

        context.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0xFF3B82F6);
    }

    // ------------------------------------------------------------
    // Input
    // ------------------------------------------------------------

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentHeight = filteredItems.size() * ROW_HEIGHT;
        int visibleHeight = listBottom() - listTop();
        double maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = MathHelper.clamp(scrollOffset - verticalAmount * (ROW_HEIGHT * 1.5), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();

        int left = listLeft();
        int width = listWidth();
        int top = listTop();
        int bottom = listBottom();

        if (mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < bottom) {
            int relativeY = (int) (mouseY - top - 4 + scrollOffset);
            int index = relativeY / ROW_HEIGHT;
            if (index >= 0 && index < filteredItems.size()) {
                selectedItem = filteredItems.get(index);
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() {
        // Never pause the game (relevant in singleplayer/LAN testing).
        return false;
    }
}
