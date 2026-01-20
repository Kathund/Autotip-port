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

package club.sk1er.mods.autotip.stats;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Stats {
    private int tipsSent;
    private int tipsReceived;
    private int xpSent;
    private int xpReceived;
    private Map<String, Integer> coinsSent;
    private Map<String, Integer> coinsReceived;

    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);

    public Stats() {
        this.tipsSent = 0;
        this.tipsReceived = 0;
        this.xpSent = 0;
        this.xpReceived = 0;
        this.coinsSent = new HashMap<>();
        this.coinsReceived = new HashMap<>();
    }

    public int getTipsSent() {
        return tipsSent;
    }

    public int getTipsReceived() {
        return tipsReceived;
    }

    public int getTipsTotal() {
        return tipsSent + tipsReceived;
    }

    public int getXpSent() {
        return xpSent;
    }

    public int getXpReceived() {
        return xpReceived;
    }

    public int getXpTotal() {
        return xpSent + xpReceived;
    }

    public Map<String, Integer> getCoinsSent() {
        return coinsSent;
    }

    public Map<String, Integer> getCoinsReceived() {
        return coinsReceived;
    }

    public int getTotalCoinsSent() {
        return coinsSent.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalCoinsReceived() {
        return coinsReceived.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalCoins() {
        return getTotalCoinsSent() + getTotalCoinsReceived();
    }

    public void addTipSent() {
        this.tipsSent++;
    }

    public void addTipsSent(int count) {
        this.tipsSent += count;
    }

    public void addTipReceived() {
        this.tipsReceived++;
    }

    public void addTipsReceived(int count) {
        this.tipsReceived += count;
    }

    public void addXpSent(int xp) {
        this.xpSent += xp;
    }

    public void addXpReceived(int xp) {
        this.xpReceived += xp;
    }

    public void addCoinsSent(String game, int coins) {
        coinsSent.merge(game, coins, Integer::sum);
    }

    public void addCoinsReceived(String game, int coins) {
        coinsReceived.merge(game, coins, Integer::sum);
    }

    public String getTipsSentFormatted() {
        return FORMAT.format(tipsSent);
    }

    public String getTipsReceivedFormatted() {
        return FORMAT.format(tipsReceived);
    }

    public String getTipsTotalFormatted() {
        return FORMAT.format(getTipsTotal());
    }

    public String getXpSentFormatted() {
        return FORMAT.format(xpSent);
    }

    public String getXpReceivedFormatted() {
        return FORMAT.format(xpReceived);
    }

    public String getXpTotalFormatted() {
        return FORMAT.format(getXpTotal());
    }

    public String getTotalCoinsFormatted() {
        return FORMAT.format(getTotalCoins());
    }

    public static String formatNumber(int num) {
        return FORMAT.format(num);
    }

    @Override
    public String toString() {
        return String.format("Stats{tips=%d/%d, xp=%d/%d, coins=%d/%d}",
                tipsSent, tipsReceived, xpSent, xpReceived, getTotalCoinsSent(), getTotalCoinsReceived());
    }
}
