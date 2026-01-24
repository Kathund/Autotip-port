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

package club.sk1er.mods.autotip.config;

public enum MessageMode {
    ALL,      // Show all tip messages
    OFF,      // Hide all tip messages
    SUCCESS,  // Only show success messages (green "you tipped" / "you were tipped")
    ERROR;    // Only show error messages (red "player not online", "already tipped")

    public static MessageMode fromString(String str) {
        return switch (str.toLowerCase()) {
            case "all" -> ALL;
            case "off" -> OFF;
            case "success" -> SUCCESS;
            case "error" -> ERROR;
            default -> null;
        };
    }
}
