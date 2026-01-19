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

package club.sk1er.mods.autotip.api.records;

import club.sk1er.mods.autotip.tipping.Tip;
import com.google.gson.Gson;
import java.util.List;

public record TipRecord(
        boolean success,
        String cause,
        List<Tip> tips
) {
    public static TipRecord fromResponseBody(String responseBody) {
        return new Gson().fromJson(responseBody, TipRecord.class);
    }

    public static TipRecord defaultTips() {
        return new TipRecord(true, null, List.of(new Tip("all", null)));
    }

}
