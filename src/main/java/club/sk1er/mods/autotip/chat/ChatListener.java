/**
 Autotip - tips online boosters, and other online Autotip users.
 You send and receive tips, increasing your coin and experience gain.
 Copyright (C) 2026 QWERTZ, Sk1erLLC

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package club.sk1er.mods.autotip.chat;

import club.sk1er.mods.autotip.Autotip;
import club.sk1er.mods.autotip.config.MessageMode;
import club.sk1er.mods.autotip.stats.StatsManager;
import club.sk1er.mods.autotip.util.HypixelUtil;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener {

    // "You tipped 5 players in 2 games!"
    private static final Pattern TIPS_SENT_MULTI = Pattern.compile(
            "You tipped (?<tips>\\d+) players?"
    );

    // "You tipped QWERTZ_EXE in SkyWars!"
    private static final Pattern TIPS_SENT_SINGLE = Pattern.compile(
            "You tipped (?<player>[\\w_]+) in (?<game>[^!]+)!"
    );

    // "You were tipped by 3 players!"
    private static final Pattern TIPS_RECEIVED = Pattern.compile(
            "You were tipped by (?<tips>\\d+) players?"
    );

    // "+50 Hypixel Experience" in hover
    private static final Pattern XP_HOVER = Pattern.compile(
            "\\+(?<xp>\\d+) Hypixel Experience"
    );

    // "+15 Blitz SG Coins" or "+15 The TNT Games Tokens" in hover
    private static final Pattern COINS_HOVER = Pattern.compile(
            "\\+(?<coins>\\d+) (?<game>.+?) (?:Coins|Tokens)"
    );

    // Error messages
    private static final String PLAYER_OFFLINE = "That player is not online, try another user!";
    private static final String ALREADY_TIPPED_HOUR = "You've already tipped someone in the past hour in";
    private static final String ALREADY_TIPPED_PERSON = "You've already tipped that person today in";
    private static final String NO_BOOSTERS = "No one has a network booster active right now!";
    private static final String ALL_TIPPED = "You already tipped everyone that has boosters active";

    public ChatListener() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            if (!HypixelUtil.isOnHypixel()) return true;

            String text = message.getString();
            MessageType type = getMessageType(text);

            if (type == MessageType.NONE) return true;

            processMessage(text, message);

            MessageMode mode = Autotip.getInstance().getConfig().getMessageMode();
            return shouldShowMessage(type, mode);
        });
    }

    private enum MessageType {
        NONE,       // Not a tip-related message
        SUCCESS,    // "You tipped" or "You were tipped"
        ERROR       // "Player not online" or "Already tipped"
    }

    private MessageType getMessageType(String text) {
        // Success messages
        if (TIPS_SENT_MULTI.matcher(text).find()) return MessageType.SUCCESS;
        if (TIPS_SENT_SINGLE.matcher(text).find()) return MessageType.SUCCESS;
        if (TIPS_RECEIVED.matcher(text).find()) return MessageType.SUCCESS;

        // Error messages
        if (text.contains(PLAYER_OFFLINE)) return MessageType.ERROR;
        if (text.contains(ALREADY_TIPPED_HOUR)) return MessageType.ERROR;
        if (text.contains(ALREADY_TIPPED_PERSON)) return MessageType.ERROR;
        if (text.contains(NO_BOOSTERS)) return MessageType.ERROR;
        if (text.contains(ALL_TIPPED)) return MessageType.ERROR;

        return MessageType.NONE;
    }

    private boolean shouldShowMessage(MessageType type, MessageMode mode) {
        return switch (mode) {
            case ALL -> true;
            case OFF -> false;
            case SUCCESS -> type == MessageType.SUCCESS;
            case ERROR -> type == MessageType.ERROR;
        };
    }

    // This method is not used in production, its there to debug components or RegExes in case hypixel changes the chat format
    private void debugPrintComponent(Component component, int depth) {
        String indent = "  ".repeat(depth);
        String text = component.getString();
        Style style = component.getStyle();

        System.out.println(indent + "[DEBUG] Component: \"" + text + "\"");
        System.out.println(indent + "  Literal content: \"" + component.getContents() + "\"");

        HoverEvent hover = style.getHoverEvent();
        if (hover != null) {
            System.out.println(indent + "  HoverEvent: " + hover);
        }

        for (Component sibling : component.getSiblings()) {
            debugPrintComponent(sibling, depth + 1);
        }
    }

    private String getHoverText(Component component) {
        Style style = component.getStyle();
        HoverEvent hover = style.getHoverEvent();
        if (hover != null) {
            try {
                String hoverStr = hover.toString();
                // Extract text between "literal{" and "}"
                int start = hoverStr.indexOf("literal{");
                if (start != -1) {
                    start += 8; // length of "literal{"
                    int end = hoverStr.indexOf("}[style=", start);
                    if (end == -1) end = hoverStr.indexOf("}", start);
                    if (end != -1) {
                        return hoverStr.substring(start, end);
                    }
                }
            } catch (Exception e) {
                System.err.println("[Autotip] Error parsing hover: " + e.getMessage());
            }
        }

        for (Component sibling : component.getSiblings()) {
            String result = getHoverText(sibling);
            if (result != null) return result;
        }

        return null;
    }

    private void processMessage(String message, Component component) {
        // Handle player offline error
        if (message.contains(PLAYER_OFFLINE)) {
            Autotip.getInstance().getTipManager().onPlayerOffline();
            return;
        }

        if (Autotip.DEBUG && message.contains("tipped")) {
            debugPrintComponent(component, 0);
        }

        StatsManager stats = Autotip.getInstance().getStatsManager();

        Matcher tipsSentMulti = TIPS_SENT_MULTI.matcher(message);
        if (tipsSentMulti.find()) {
            int tips = Integer.parseInt(tipsSentMulti.group("tips"));
            stats.addTipsSent(tips);
            if (Autotip.DEBUG) {
                Autotip.getInstance().getMessageUtil().log("§aRecorded " + tips + " tips sent");
            }

            String hoverText = getHoverText(component);
            if (hoverText != null) {
                Matcher xpMatcher = XP_HOVER.matcher(hoverText);
                if (xpMatcher.find()) {
                    int xp = Integer.parseInt(xpMatcher.group("xp"));
                    stats.addXpSent(xp);
                    if (Autotip.DEBUG) {
                        Autotip.getInstance().getMessageUtil().log("§aRecorded " + xp + " XP sent");
                    }
                }

                Matcher coinsMatcher = COINS_HOVER.matcher(hoverText);
                while (coinsMatcher.find()) {
                    int coins = Integer.parseInt(coinsMatcher.group("coins"));
                    String coinGame = coinsMatcher.group("game");
                    stats.addCoinsSent(coinGame, coins);
                    if (Autotip.DEBUG) {
                        Autotip.getInstance().getMessageUtil().log("§aRecorded " + coins + " coins/tokens sent for " + coinGame);
                    }
                }
            }
            return;
        }

        Matcher tipSentSingle = TIPS_SENT_SINGLE.matcher(message);
        if (tipSentSingle.find()) {
            stats.addTipSent();
            String player = tipSentSingle.group("player");
            String game = tipSentSingle.group("game");
            if (Autotip.DEBUG) {
                Autotip.getInstance().getMessageUtil().log("§aRecorded 1 tip sent to " + player + " in " + game);
            }

            String hoverText = getHoverText(component);
            if (hoverText != null) {
                Matcher xpMatcher = XP_HOVER.matcher(hoverText);
                if (xpMatcher.find()) {
                    int xp = Integer.parseInt(xpMatcher.group("xp"));
                    stats.addXpSent(xp);
                    if (Autotip.DEBUG) {
                        Autotip.getInstance().getMessageUtil().log("§aRecorded " + xp + " XP sent");
                    }
                }

                Matcher coinsMatcher = COINS_HOVER.matcher(hoverText);
                while (coinsMatcher.find()) {
                    int coins = Integer.parseInt(coinsMatcher.group("coins"));
                    String coinGame = coinsMatcher.group("game");
                    stats.addCoinsSent(coinGame, coins);
                    if (Autotip.DEBUG) {
                        Autotip.getInstance().getMessageUtil().log("§aRecorded " + coins + " coins/tokens sent for " + coinGame);
                    }
                }
            }
            return;
        }

        Matcher tipsReceived = TIPS_RECEIVED.matcher(message);
        if (tipsReceived.find()) {
            int tips = Integer.parseInt(tipsReceived.group("tips"));
            stats.addTipsReceived(tips);
            if (Autotip.DEBUG) {
                Autotip.getInstance().getMessageUtil().log("§aRecorded " + tips + " tips received");
            }

            String hoverText = getHoverText(component);
            if (hoverText != null) {
                Matcher xpMatcher = XP_HOVER.matcher(hoverText);
                if (xpMatcher.find()) {
                    int xp = Integer.parseInt(xpMatcher.group("xp"));
                    stats.addXpReceived(xp);
                    if (Autotip.DEBUG) {
                        Autotip.getInstance().getMessageUtil().log("§aRecorded " + xp + " XP received");
                    }
                }

                Matcher coinsMatcher = COINS_HOVER.matcher(hoverText);
                while (coinsMatcher.find()) {
                    int coins = Integer.parseInt(coinsMatcher.group("coins"));
                    String coinGame = coinsMatcher.group("game");
                    stats.addCoinsReceived(coinGame, coins);
                    if (Autotip.DEBUG) {
                        Autotip.getInstance().getMessageUtil().log("§aRecorded " + coins + " coins/tokens received for " + coinGame);
                    }
                }
            }
        }
    }
}
