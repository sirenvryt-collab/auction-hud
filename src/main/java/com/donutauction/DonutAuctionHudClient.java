package com.donutauction;

import com.donutauction.gui.AuctionBrowserScreen;
import com.donutauction.hud.AuctionHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side entrypoint for the Donut Auction HUD mod.
 * <p>
 * This mod is entirely client-side: it does not add any blocks, items or
 * server logic. It only reads chat messages and renders UI, and every
 * feature is gated behind {@link ServerDetector#isDonutSmp()} so it stays
 * completely dormant on any server other than DonutSMP.
 */
public class DonutAuctionHudClient implements ClientModInitializer {

    public static final String MOD_ID = "donutauctionhud";

    private static KeyBinding openBrowserKey;
    private static KeyBinding toggleHudKey;

    @Override
    public void onInitializeClient() {
        // Parse "Name paid you $ Amount" chat messages into bids.
        ChatBidListener.register();

        // Register the always-on countdown/bidder HUD panel.
        HudElementRegistry.addLast(Identifier.of(MOD_ID, "auction_hud"), new AuctionHudRenderer());

        // Register keybindings.
        openBrowserKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.donutauctionhud.open_browser",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.donutauctionhud.general"
        ));

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.donutauctionhud.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.donutauctionhud.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        while (openBrowserKey.wasPressed()) {
            if (client.currentScreen == null && ServerDetector.isDonutSmp()) {
                client.setScreen(new AuctionBrowserScreen());
            }
        }

        while (toggleHudKey.wasPressed()) {
            AuctionHudRenderer.hudVisible = !AuctionHudRenderer.hudVisible;
        }
    }
}
