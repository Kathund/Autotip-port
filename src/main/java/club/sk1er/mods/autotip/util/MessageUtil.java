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

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {
    private final Pattern PARAM_PATTERN = Pattern.compile("\\{}");
    private final String PREFIX = "[Autotip] "; // TODO: Improve prefix

    public MessageUtil() {
    }

    public void send(String message, Object... replacements) {
        send(format(message, replacements));
    }

    public void send(String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal(PREFIX + message), false);
            // TODO: AND TRANSLATIONS
        } else {
            System.out.println(PREFIX + message); // TODO: message if not on a server
        }
    }

    public void sendCommand(String command) {
        sendPlayerChat("/" + command);
    }
    private void sendPlayerChat(String message) {
        // TODO: Implement Player Chat Send
    }

    private String format(String input, Object... params) {
        if (params == null) {
            return input;
        }
        for (Object o : params) {
            if (o != null) {
                String replacement = Matcher.quoteReplacement(format(o.toString()));
                input = PARAM_PATTERN.matcher(input).replaceFirst(replacement);
            }
        }
        return input;
    }
}
