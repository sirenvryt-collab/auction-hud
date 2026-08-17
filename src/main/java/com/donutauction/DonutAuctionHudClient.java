package com.donutauction;

import com.donutauction.gui.AuctionBrowserScreen;
import com.donutauction.hud.AuctionHudRenderer;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class DonutAuctionHudClient implements ClientModInitializer {

    public static final String MOD_ID = "donutauctionhud";

    private static final KeyBinding.Category KEY_CATEGORY =
            new KeyBinding.Category(Identifier.of(MOD_ID, "general"));

    private static KeyBinding openBrowserKey;
    private static KeyBinding toggleHudKey;

    @Override
    public void onInitializeClient() {
        ChatBidListener.register();

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "auction_hud"), new AuctionHudRenderer());

        openBrowserKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.donutauctionhud.open_browser",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                KEY_CATEGORY
        ));

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.donutauctionhud.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        registerCommands();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                // /auctionstart "<item name>" <minBid> <durationSeconds>
                // Quote the item name if it has spaces, e.g. "Dragon Egg".
                ClientCommandManager.literal("auctionstart")
                        .then(ClientCommandManager.argument("item", StringArgumentType.string())
                                .then(ClientCommandManager.argument("minBid", DoubleArgumentType.doubleArg(0))
                                        .then(ClientCommandManager.argument("seconds", IntegerArgumentType.integer(1))
                                                .executes(ctx -> runStart(
                                                        StringArgumentType.getString(ctx, "item"),
                                                        DoubleArgumentType.getDouble(ctx, "minBid"),
                                                        IntegerArgumentType.getInteger(ctx, "seconds")
                                                )))))
        ));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("auctionstop")
                        .executes(ctx -> {
                            AuctionState.INSTANCE.stop();
                            ctx.getSource().sendFeedback(
                                    Text.literal("Auction ended.").formatted(Formatting.BLUE));
                            return 1;
                        })
        ));
    }

    private int runStart(String item, double minBid, int seconds) {
        AuctionState.INSTANCE.start(item, minBid, seconds);
        return 1;
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
