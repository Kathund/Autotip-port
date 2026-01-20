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

package club.sk1er.mods.autotip.util;

import club.sk1er.mods.autotip.Autotip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class HypixelUtil {
    private static String lastHypixelAddress = null;
    // Cooldown to prevent multiple logins in a short time
    private static long lastLoginTime = 0;

    public static boolean isOnHypixel() {
        Minecraft client = Minecraft.getInstance();
        ClientPacketListener listener = client.getConnection();

        if (listener == null || client.player == null) {
            return false;
        }

        String address = listener.getConnection().getRemoteAddress().toString();
        return address.toLowerCase().contains("hypixel.net");
    }

    /**
     * Checks if we should login:
     * 1. Must be on Hypixel
     * 2. Must not have logged in to this server yet
     * 3. Must respect 5-second cooldown
     */
    public static boolean shouldLogin() {
        if (!isOnHypixel()) return false;

        Minecraft client = Minecraft.getInstance();
        ClientPacketListener listener = client.getConnection();
        if (listener == null || client.player == null) return false;

        String address = listener.getConnection().getRemoteAddress().toString().toLowerCase();

        long now = System.currentTimeMillis();
        if (address.equals(lastHypixelAddress) || now - lastLoginTime < 5000) return false;

        lastHypixelAddress = address;
        lastLoginTime = now;
        return true;
    }

    public static void onServerJoin() {
        if (!shouldLogin()) return;

        // Run login asynchronously
        new Thread(() -> {
            try {
                Autotip.getInstance().getAuthManager().login();
            } catch (Exception e) {
                e.printStackTrace();
                Autotip.getInstance().getMessageUtil().error("&cAutotip login failed: " + e.getMessage());
            }
        }, "Autotip-Login-Thread").start();
    }

    public static void resetLogin() {
        lastHypixelAddress = null;
        lastLoginTime = 0;
    }

    public static void onServerDisconnect() {
        if (lastHypixelAddress != null) {
            try {
                Autotip.getInstance().getAuthManager().logout();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        resetLogin();
    }
}
