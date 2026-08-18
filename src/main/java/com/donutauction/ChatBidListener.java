package com.donutauction;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.donutauction.util.MoneyFormat;

/**
 * Listens to incoming chat/system messages and looks for DonutSMP's payment
 * confirmation format:
 * <pre>
 *     Name paid you $ Amount
 * </pre>
 * e.g. {@code "Steve paid you $ 15000"} or {@code "Steve paid you $ 47M"}.
 * <p>
 * Every matching message is treated as a new auction bid: if the paid
 * amount is higher than the current top bid, the HUD's highest
 * bidder/amount is updated immediately.
 */
public final class ChatBidListener {

    // Player names are 1-16 chars of [A-Za-z0-9_]. Amount may have a decimal
    // point, thousands separators and an optional K/M/B/T shorthand suffix.
    // Not anchored to the whole line - real DonutSMP messages can have extra
    // text/formatting before or after this exact phrase (tags, punctuation,
    // "for the <item>!", etc.), so we search for the phrase anywhere in the
    // message instead of requiring an exact full-line match.
    private static final Pattern PAID_YOU_PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9_])(?<name>[A-Za-z0-9_]{1,16}) paid you \\$ ?(?<amount>[0-9][0-9,]*(?:\\.[0-9]+)?[kKmMbBtT]?)"
    );

    private ChatBidListener() {
    }

    public static void register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register(ChatBidListener::onGameMessage);
    }

    private static boolean onGameMessage(Text message, boolean overlay) {
        if (overlay) {
            return true;
        }

        if (!ServerDetector.isDonutSmp()) {
            return true;
        }

        String plain = message.getString().trim();
        Matcher matcher = PAID_YOU_PATTERN.matcher(plain);
        if (!matcher.find()) {
            return true;
        }

        String bidderName = matcher.group("name");
        double amount = MoneyFormat.parseAmount(matcher.group("amount"));

        if (amount > 0) {
            AuctionState.INSTANCE.registerBid(bidderName, amount);
        }

        return true;
    }
}
