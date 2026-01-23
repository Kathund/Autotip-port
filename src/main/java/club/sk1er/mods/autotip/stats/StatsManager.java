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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import club.sk1er.mods.autotip.Autotip;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class StatsManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final Path statsFile;
    // Structure: { "uuid": { "2026-01-20": DailyStats } }
    private Map<String, Map<String, DailyStats>> allStats;
    private LocalDate currentDate;

    public StatsManager() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("autotip");
        this.statsFile = configDir.resolve("stats.json");
        this.allStats = load();
        this.currentDate = LocalDate.now();
    }

    private String getCurrentUuid() {
        try {
            return Minecraft.getInstance().getUser().getProfileId().toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private Map<String, DailyStats> getPlayerStats() {
        String uuid = getCurrentUuid();
        return allStats.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    private DailyStats getToday() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            currentDate = today;
        }
        String key = today.toString();
        return getPlayerStats().computeIfAbsent(key, k -> new DailyStats());
    }

    public void save() {
        try {
            Files.createDirectories(statsFile.getParent());
            Files.writeString(statsFile, GSON.toJson(allStats));
        } catch (IOException e) {
            System.err.println("[Autotip] Failed to save stats: " + e.getMessage());
        }
    }

    private Map<String, Map<String, DailyStats>> load() {
        if (Files.exists(statsFile)) {
            try {
                String json = Files.readString(statsFile);
                Type type = new TypeToken<Map<String, Map<String, DailyStats>>>() {}.getType();
                Map<String, Map<String, DailyStats>> loaded = GSON.fromJson(json, type);
                if (loaded != null) {
                    int totalDays = loaded.values().stream().mapToInt(Map::size).sum();
                    Autotip.getInstance().getMessageUtil().log("§aLoaded stats for " + loaded.size() + " players, " + totalDays + " total days");
                    return loaded;
                }
            } catch (Exception e) {
                Autotip.getInstance().getMessageUtil().error("§cFailed to load stats: " + e.getMessage());
            }
        }
        Autotip.getInstance().getMessageUtil().log("§cNo stats found, creating new stats file");
        return new HashMap<>();
    }

    // Stats aggregation methods
    public Stats getStats(StatsPeriod period) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = switch (period) {
            case DAILY -> today;
            case WEEKLY -> today.minus(7, ChronoUnit.DAYS);
            case MONTHLY -> today.minus(30, ChronoUnit.DAYS);
            case YEARLY -> today.minus(365, ChronoUnit.DAYS);
            case LIFETIME -> LocalDate.of(2000, 1, 1);
        };
        return getStatsRange(startDate, today);
    }

    public Stats getStatsRange(LocalDate start, LocalDate end) {
        Stats aggregated = new Stats();
        Map<String, DailyStats> playerStats = getPlayerStats();

        for (Map.Entry<String, DailyStats> entry : playerStats.entrySet()) {
            try {
                LocalDate date = LocalDate.parse(entry.getKey());
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    DailyStats daily = entry.getValue();
                    aggregated.addTipsSent(daily.getTipsSent());
                    aggregated.addTipsReceived(daily.getTipsReceived());
                    aggregated.addXpSent(daily.getXpSent());
                    aggregated.addXpReceived(daily.getXpReceived());
                    
                    for (Map.Entry<String, Integer> coinEntry : daily.getCoinsSent().entrySet()) {
                        aggregated.addCoinsSent(coinEntry.getKey(), coinEntry.getValue());
                    }
                    
                    for (Map.Entry<String, Integer> coinEntry : daily.getCoinsReceived().entrySet()) {
                        aggregated.addCoinsReceived(coinEntry.getKey(), coinEntry.getValue());
                    }
                }
            } catch (Exception e) {
                if (Autotip.DEBUG) {
                    Autotip.getInstance().getMessageUtil().log("§cInvalid date key: " + e.getMessage());
                }
            }
        }
        return aggregated;
    }

    public Stats getStats() {
        return getStats(StatsPeriod.LIFETIME);
    }

    public Stats getStatsForDate(LocalDate date) {
        return getStatsRange(date, date);
    }

    public void addTipSent() {
        getToday().addTipsSent(1);
        save();
    }

    public void addTipsSent(int count) {
        getToday().addTipsSent(count);
        save();
    }

    public void addTipReceived() {
        getToday().addTipsReceived(1);
        save();
    }

    public void addTipsReceived(int count) {
        getToday().addTipsReceived(count);
        save();
    }

    public void addXpSent(int xp) {
        getToday().addXpSent(xp);
        save();
    }

    public void addXpReceived(int xp) {
        getToday().addXpReceived(xp);
        save();
    }

    public void addCoinsSent(String game, int coins) {
        getToday().addCoinsSent(game, coins);
        save();
    }

    public void addCoinsReceived(String game, int coins) {
        getToday().addCoinsReceived(game, coins);
        save();
    }

    public enum StatsPeriod {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY,
        LIFETIME
    }
}
