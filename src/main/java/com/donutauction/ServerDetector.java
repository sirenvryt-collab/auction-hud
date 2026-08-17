package com.donutauction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.Locale;

/**
 * Detects whether the player is currently connected to DonutSMP. All HUD
 * and browser features check this before rendering, so the mod is
 * completely invisible/inactive on every other server or in singleplayer.
 */
public final class ServerDetector {

    /** Matches donutsmp.net and any subdomain of it (e.g. play.donutsmp.net). */
    private static final String DONUT_SMP_DOMAIN = "donutsmp.net";

    private ServerDetector() {
    }

    public static boolean isDonutSmp() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return false;
        }

        // Singleplayer / integrated server is never DonutSMP.
        if (client.isIntegratedServerRunning()) {
            return false;
        }

        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo == null || serverInfo.address == null) {
            return false;
        }

        String address = serverInfo.address.toLowerCase(Locale.ROOT);
        return address.contains(DONUT_SMP_DOMAIN);
    }
}
