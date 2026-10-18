package com.donutauction.gui;

import com.donutauction.AuctionItem;
import com.donutauction.AuctionState;
import com.donutauction.util.MoneyFormat;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Full-screen auction item browser: a search/filter bar at the top and a
 * scrollable, hoverable list of items below it.
 * <p>
 * Clicking a row opens a setup panel where you choose the minimum bid and the
 * auction time, then press "Start Auction".
 */
public class AuctionBrowserScreen extends Screen {

    private static final int ROW_HEIGHT = 26;
    private static final int LIST_TOP_MARGIN = 62;
    private static final int LIST_BOTTOM_MARGIN = 16;
    private static final int LIST_SIDE_MARGIN = 60;
    private static final int MAX_LIST_WIDTH = 420;

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 228;
    private static final int PANEL_TOP = 70;
    private static final int MAX_DURATION_SECONDS = 24 * 60 * 60;

    private static final Pattern DURATION_PATTERN =
            Pattern.compile("^(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?$");

    private TextFieldWidget searchField;
    private TextFieldWidget minBidField;
    private TextFieldWidget timeField;
    private final List<ButtonWidget> setupButtons = new ArrayList<>();

    private String searchQuery = "";
    private String minBidText = "";
    private String timeText = "5m";
    private String errorMessage = "";

    private double scrollOffset = 0;
    private List<AuctionItem> filteredItems = new ArrayList<>();
    private AuctionItem selectedItem = null;
    private boolean setupMode = false;

    public AuctionBrowserScreen() {
        super(Text.literal("Auction Browser"));
    }

    @Override
    protected void init() {
        setupButtons.clear();

        // ---- Search field (list mode) ----
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
        this.searchField.setText(searchQuery);
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);

        // ---- Setup panel widgets ----
        int panelLeft = this.width / 2 - PANEL_WIDTH / 2;
        int innerLeft = panelLeft + 20;
        int innerWidth = PANEL_WIDTH - 40;

        this.minBidField = new TextFieldWidget(
                this.textRenderer, innerLeft, PANEL_TOP + 62, innerWidth, 20, Text.literal("Minimum bid"));
        this.minBidField.setPlaceholder(Text.literal("e.g. 250k, 1.5m, 2b").formatted(Formatting.GRAY));
        this.minBidField.setMaxLength(24);
        this.minBidField.setText(minBidText);
        this.minBidField.setChangedListener(text -> this.minBidText = text);
        this.addDrawableChild(this.minBidField);

        this.timeField = new TextFieldWidget(
                this.textRenderer, innerLeft, PANEL_TOP + 104, innerWidth, 20, Text.literal("Time"));
        this.timeField.setPlaceholder(Text.literal("e.g. 5m, 90s, 1m30s").formatted(Formatting.GRAY));
        this.timeField.setMaxLength(16);
        this.timeField.setText(timeText);
        this.timeField.setChangedListener(text -> this.timeText = text);
        this.addDrawableChild(this.timeField);

        // Quick time presets (two rows of four).
        String[][] presetRows = {
                {"1m", "5m", "10m", "30m"},
                {"45s", "30s", "20s", "15s"}
        };
        int gap = 4;
        int presetWidth = (innerWidth - gap * 3) / 4;
        for (int row = 0; row < presetRows.length; row++) {
            for (int i = 0; i < presetRows[row].length; i++) {
                final String preset = presetRows[row][i];
                ButtonWidget button = ButtonWidget.builder(Text.literal(preset), b -> {
                            this.timeField.setText(preset);
                            this.errorMessage = "";
                        })
                        .dimensions(innerLeft + i * (presetWidth + gap), PANEL_TOP + 130 + row * 22, presetWidth, 18)
                        .build();
                setupButtons.add(button);
                this.addDrawableChild(button);
            }
        }

        int bottomButtonWidth = (innerWidth - gap) / 2;
        ButtonWidget backButton = ButtonWidget.builder(Text.literal("Back"), b -> exitSetupMode())
                .dimensions(innerLeft, PANEL_TOP + 198, bottomButtonWidth, 20)
                .build();
        ButtonWidget startButton = ButtonWidget.builder(Text.literal("Start Auction"), b -> confirmStart())
                .dimensions(innerLeft + bottomButtonWidth + gap, PANEL_TOP + 198, bottomButtonWidth, 20)
                .build();
        setupButtons.add(backButton);
        setupButtons.add(startButton);
        this.addDrawableChild(backButton);
        this.addDrawableChild(startButton);

        refreshFilter();
        applyMode();
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
    // Mode switching
    // ------------------------------------------------------------

    private void enterSetupMode(AuctionItem item) {
        this.selectedItem = item;
        this.minBidText = MoneyFormat.format(item.startingBid());
        this.timeText = "5m";
        this.errorMessage = "";
        this.minBidField.setText(minBidText);
        this.timeField.setText(timeText);
        this.setupMode = true;
        applyMode();
        this.setFocused(this.minBidField);
    }

    private void exitSetupMode() {
        this.setupMode = false;
        this.errorMessage = "";
        applyMode();
        this.setFocused(this.searchField);
    }

    private void applyMode() {
        this.searchField.setVisible(!setupMode);
        this.searchField.active = !setupMode;

        this.minBidField.setVisible(setupMode);
        this.minBidField.active = setupMode;
        this.timeField.setVisible(setupMode);
        this.timeField.active = setupMode;
        for (ButtonWidget button : setupButtons) {
            button.visible = setupMode;
            button.active = setupMode;
        }

        if (!setupMode) {
            this.setFocused(this.searchField);
        }
    }

    // ------------------------------------------------------------
    // Starting the auction
    // ------------------------------------------------------------

    private void confirmStart() {
        if (selectedItem == null) {
            return;
        }

        double minBid = MoneyFormat.parseAmount(minBidText);
        if (minBid <= 0) {
            errorMessage = "Enter a valid minimum bid (e.g. 250k, 1.5m).";
            return;
        }

        int seconds = parseDurationSeconds(timeText);
        if (seconds <= 0) {
            errorMessage = "Enter a valid time (e.g. 5m, 90s, 1m30s).";
            return;
        }

        AuctionState.INSTANCE.start(selectedItem.displayName(), selectedItem.item(), minBid, seconds);

        if (this.client != null && this.client.player != null) {
            this.client.player.sendMessage(
                    Text.literal("[Auction] ").formatted(Formatting.BLUE, Formatting.BOLD)
                            .append(Text.literal("Started auction for ").formatted(Formatting.GRAY))
                            .append(Text.literal(selectedItem.displayName()).formatted(Formatting.WHITE))
                            .append(Text.literal(" (min bid $" + MoneyFormat.format(minBid)
                                    + ", " + formatDuration(seconds) + ")").formatted(Formatting.GRAY)),
                    false);
        }
        this.close();
    }

    /**
     * Accepts "5m", "90s", "1m30s", "1h", "2h15m". A plain number is treated
     * as minutes. Returns -1 if it can't be parsed.
     */
    private static int parseDurationSeconds(String raw) {
        if (raw == null) {
            return -1;
        }
        String text = raw.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        if (text.isEmpty()) {
            return -1;
        }

        try {
            if (text.matches("\\d+")) {
                long minutes = Long.parseLong(text);
                return clampDuration(minutes * 60L);
            }

            Matcher matcher = DURATION_PATTERN.matcher(text);
            if (!matcher.matches()) {
                return -1;
            }
            long hours = matcher.group(1) == null ? 0 : Long.parseLong(matcher.group(1));
            long minutes = matcher.group(2) == null ? 0 : Long.parseLong(matcher.group(2));
            long seconds = matcher.group(3) == null ? 0 : Long.parseLong(matcher.group(3));
            return clampDuration(hours * 3600L + minutes * 60L + seconds);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int clampDuration(long totalSeconds) {
        if (totalSeconds <= 0) {
            return -1;
        }
        return (int) Math.min(totalSeconds, MAX_DURATION_SECONDS);
    }

    private static String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    // ------------------------------------------------------------
    // Layout helpers
    // ------------------------------------------------------------

    private int listLeft() {
        return this.width / 2 - Math.min(MAX_LIST_WIDTH, this.width - LIST_SIDE_MARGIN * 2) / 2;
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

        if (setupMode) {
            renderSetupPanel(context);
        } else {
            renderList(context, mouseX, mouseY);
        }

        // Footer hint.
        String hint = setupMode
                ? "Set the minimum bid and time, then press Start Auction  |  Esc to close"
                : "Click an item to set it up  |  Scroll  |  Type to search  |  Esc to close";
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(hint).formatted(Formatting.DARK_GRAY),
                this.width / 2,
                this.height - 12,
                0xFF777777
        );

        // Draws the visible text fields / buttons.
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderSetupPanel(DrawContext context) {
        int panelLeft = this.width / 2 - PANEL_WIDTH / 2;
        int innerLeft = panelLeft + 20;

        context.fill(panelLeft, PANEL_TOP, panelLeft + PANEL_WIDTH, PANEL_TOP + PANEL_HEIGHT, 0xB0000000);
        context.drawStrokedRectangle(panelLeft, PANEL_TOP, PANEL_WIDTH, PANEL_HEIGHT, 0xFF3A3A3A);

        if (selectedItem != null) {
            context.drawItem(new ItemStack(selectedItem.item()), innerLeft, PANEL_TOP + 12);
            context.drawText(
                    this.textRenderer,
                    Text.literal(selectedItem.displayName()).formatted(Formatting.BOLD),
                    innerLeft + 24,
                    PANEL_TOP + 12,
                    0xFFFFFFFF,
                    true
            );
            context.drawText(
                    this.textRenderer,
                    Text.literal("Suggested start: $" + MoneyFormat.format(selectedItem.startingBid()))
                            .formatted(Formatting.DARK_GRAY),
                    innerLeft + 24,
                    PANEL_TOP + 24,
                    0xFF888888,
                    false
            );
        }

        context.drawText(this.textRenderer,
                Text.literal("Minimum bid").formatted(Formatting.GRAY),
                innerLeft, PANEL_TOP + 51, 0xFFAAAAAA, false);
        context.drawText(this.textRenderer,
                Text.literal("Time  (plain number = minutes)").formatted(Formatting.GRAY),
                innerLeft, PANEL_TOP + 93, 0xFFAAAAAA, false);

        if (!errorMessage.isEmpty()) {
            context.drawText(this.textRenderer,
                    Text.literal(errorMessage).formatted(Formatting.RED),
                    innerLeft, PANEL_TOP + 178, 0xFFFF5555, false);
        }
    }

    private void renderList(DrawContext context, int mouseX, int mouseY) {
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

        drawScrollbar(context, left, width, top, bottom);
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
        if (setupMode) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int contentHeight = filteredItems.size() * ROW_HEIGHT;
        int visibleHeight = listBottom() - listTop();
        double maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = MathHelper.clamp(scrollOffset - verticalAmount * (ROW_HEIGHT * 1.5), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (setupMode) {
            // Text fields and buttons handle their own clicks.
            return super.mouseClicked(click, doubled);
        }

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
                enterSetupMode(filteredItems.get(index));
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
