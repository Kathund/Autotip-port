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

package club.sk1er.mods.autotip.tipping;

import club.sk1er.mods.autotip.Autotip;
import club.sk1er.mods.autotip.api.AutotipAPIRequestFactory;
import club.sk1er.mods.autotip.api.records.KeepAliveRecord;
import club.sk1er.mods.autotip.api.records.TipRecord;
import club.sk1er.mods.autotip.auth.AuthManager;
import club.sk1er.mods.autotip.util.HypixelUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class TipManager {
    private static final int MAX_RETRY_ROUNDS = 3;

    private final ScheduledExecutorService scheduler;
    private final Queue<String> gameQueue;
    private final Map<String, Queue<Tip>> tipsByGame;
    private final Set<String> failedGames;

    private ScheduledFuture<?> keepAliveTask;
    private ScheduledFuture<?> tipWaveTask;
    private ScheduledFuture<?> tipCycleTask;

    private int keepAliveRate;
    private int tipWaveRate;
    private int tipCycleRate;

    private long lastTipWave;
    private long nextTipWave;

    private volatile Tip currentTip;
    private volatile long lastTipSent;

    public TipManager() {
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Autotip-Scheduler");
            t.setDaemon(true);
            return t;
        });
        this.gameQueue = new ConcurrentLinkedQueue<>();
        this.tipsByGame = new ConcurrentHashMap<>();
        this.failedGames = ConcurrentHashMap.newKeySet();
    }

    public void start(int keepAliveRate, int tipWaveRate, int tipCycleRate) {
        this.keepAliveRate = keepAliveRate;
        this.tipWaveRate = tipWaveRate;
        this.tipCycleRate = tipCycleRate;

        if (keepAliveRate > 0) {
            keepAliveTask = scheduler.scheduleAtFixedRate(
                    this::keepAlive,
                    keepAliveRate,
                    keepAliveRate,
                    TimeUnit.SECONDS
            );
        }

        if (tipWaveRate > 0) {
            long initialDelay = calculateInitialDelay();
            this.nextTipWave = System.currentTimeMillis() + (initialDelay * 1000L);

            tipWaveTask = scheduler.scheduleAtFixedRate(
                    this::tipWave,
                    initialDelay,
                    tipWaveRate,
                    TimeUnit.SECONDS
            );

            if (Autotip.DEBUG) {
                Autotip.getInstance().getMessageUtil().log("Tip wave scheduled in " + initialDelay + "s");
            }
        }
    }

    private long calculateInitialDelay() {
        if (lastTipWave == 0) {
            return 0;
        }

        long elapsed = (System.currentTimeMillis() - lastTipWave) / 1000L;
        long remaining = tipWaveRate - elapsed;

        if (remaining <= 0) {
            return 0;
        }

        return remaining;
    }

    public void stop() {
        cancelTask(keepAliveTask);
        cancelTask(tipWaveTask);
        cancelTask(tipCycleTask);
        keepAliveTask = null;
        tipWaveTask = null;
        tipCycleTask = null;
        gameQueue.clear();
        tipsByGame.clear();
        failedGames.clear();
        currentTip = null;
    }

    private void cancelTask(ScheduledFuture<?> task) {
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
        }
    }

    private void keepAlive() {
        if (!HypixelUtil.isOnHypixel() || !AuthManager.loggedIn) {
            return;
        }

        try {
            var request = AutotipAPIRequestFactory.createKeepAliveRequest(AuthManager.sessionKey);
            String response = Autotip.getInstance().getAutotipHttpClient().executeRequest(request);
            KeepAliveRecord record = KeepAliveRecord.fromResponseBody(response);

            if (record.success()) {
                if (Autotip.DEBUG) {
                    Autotip.getInstance().getMessageUtil().log("Keep-alive sent");
                }
            } else {
                Autotip.getInstance().getMessageUtil().log("§cKeep-alive failed: " + record.cause());
            }
        } catch (IOException e) {
            Autotip.getInstance().getMessageUtil().log("§cKeep-alive failed: " + e.getMessage());
        }
    }

    private void tipWave() {
        if (!HypixelUtil.isOnHypixel() || !AuthManager.loggedIn) {
            return;
        }

        this.lastTipWave = System.currentTimeMillis();
        this.nextTipWave = System.currentTimeMillis() + (tipWaveRate * 1000L);

        TipRecord tipRecord;
        try {
            var request = AutotipAPIRequestFactory.createTipRequest(AuthManager.sessionKey);
            String response = Autotip.getInstance().getAutotipHttpClient().executeRequest(request);
            tipRecord = TipRecord.fromResponseBody(response);

            if (!tipRecord.success() || tipRecord.tips() == null || tipRecord.tips().isEmpty()) {
                tipRecord = TipRecord.defaultTips();
            }
        } catch (Exception e) {
            Autotip.getInstance().getMessageUtil().log("§cFailed to fetch tips, using default: " + e.getMessage());
            tipRecord = TipRecord.defaultTips();
        }

        gameQueue.clear();
        tipsByGame.clear();
        failedGames.clear();

        for (Tip tip : tipRecord.tips()) {
            String game = tip.gamemode();
            if (!tipsByGame.containsKey(game)) {
                gameQueue.add(game);
                tipsByGame.put(game, new ConcurrentLinkedQueue<>());
            }
            tipsByGame.get(game).add(tip);
        }

        if (Autotip.DEBUG) {
            Autotip.getInstance().getMessageUtil().log("§aTip wave: " + gameQueue.size() + " games queued");
        }

        cancelTask(tipCycleTask);
        if (tipCycleRate > 0 && !gameQueue.isEmpty()) {
            tipCycleTask = scheduler.scheduleAtFixedRate(
                    this::tipCycle,
                    0,
                    tipCycleRate,
                    TimeUnit.SECONDS
            );
        }
    }

    private void tipCycle() {
        if (!HypixelUtil.isOnHypixel()) {
            cancelTask(tipCycleTask);
            tipCycleTask = null;
            return;
        }

        if (gameQueue.isEmpty()) {
            cancelTask(tipCycleTask);
            tipCycleTask = null;
            currentTip = null;

            // Start retry phase if there are failed games
            if (!failedGames.isEmpty()) {
                scheduler.schedule(() -> startRetryPhase(1), tipCycleRate, TimeUnit.SECONDS);
            }
            return;
        }

        String currentGame = gameQueue.peek();
        if (currentGame == null) {
            return;
        }

        Queue<Tip> tipsForGame = tipsByGame.get(currentGame);
        if (tipsForGame == null || tipsForGame.isEmpty()) {
            gameQueue.poll();
            return;
        }

        Tip tip = tipsForGame.poll();
        if (tip != null) {
            currentTip = tip;
            lastTipSent = System.currentTimeMillis();

            if (Autotip.DEBUG) {
                Autotip.getInstance().getMessageUtil().log("§aTipping: " + tip);
            }
            Autotip.getInstance().getMessageUtil().sendCommand(tip.asCommand());

            if (tipsForGame.isEmpty()) {
                gameQueue.poll();
            }
        }
    }

    public void onPlayerOffline() {
        if (currentTip == null) {
            return;
        }

        long timeSinceTip = System.currentTimeMillis() - lastTipSent;
        if (timeSinceTip > 5000) {
            return;
        }

        String failedGame = currentTip.gamemode();
        failedGames.add(failedGame);

        if (Autotip.DEBUG) {
            Autotip.getInstance().getMessageUtil().log("§ePlayer offline for " + failedGame + ", will retry at end");
        }
    }

    private void startRetryPhase(int round) {
        if (!HypixelUtil.isOnHypixel() || !AuthManager.loggedIn) {
            return;
        }

        if (failedGames.isEmpty() || round > MAX_RETRY_ROUNDS) {
            if (Autotip.DEBUG && round > MAX_RETRY_ROUNDS) {
                Autotip.getInstance().getMessageUtil().log("§cMax retry rounds reached");
            }
            failedGames.clear();
            return;
        }

        if (Autotip.DEBUG) {
            Autotip.getInstance().getMessageUtil().log("§eRetry round " + round + "/" + MAX_RETRY_ROUNDS + " for " + failedGames.size() + " games");
        }

        // Fetch new tips from API
        TipRecord tipRecord;
        try {
            var request = AutotipAPIRequestFactory.createTipRequest(AuthManager.sessionKey);
            String response = Autotip.getInstance().getAutotipHttpClient().executeRequest(request);
            tipRecord = TipRecord.fromResponseBody(response);

            if (!tipRecord.success() || tipRecord.tips() == null) {
                if (Autotip.DEBUG) {
                    Autotip.getInstance().getMessageUtil().log("§cFailed to fetch retry tips");
                }
                failedGames.clear();
                return;
            }
        } catch (Exception e) {
            if (Autotip.DEBUG) {
                Autotip.getInstance().getMessageUtil().log("§cFailed to fetch retry tips: " + e.getMessage());
            }
            failedGames.clear();
            return;
        }

        // Build retry queue for failed games only
        Queue<Tip> retryQueue = new ConcurrentLinkedQueue<>();
        Set<String> gamesToRetry = new HashSet<>(failedGames);
        failedGames.clear();

        for (Tip tip : tipRecord.tips()) {
            if (gamesToRetry.contains(tip.gamemode())) {
                retryQueue.add(tip);
                gamesToRetry.remove(tip.gamemode());
            }
        }

        if (retryQueue.isEmpty()) {
            if (Autotip.DEBUG) {
                Autotip.getInstance().getMessageUtil().log("§cNo retry tips available for failed games");
            }
            return;
        }

        sendRetryQueue(retryQueue, round);
    }

    private void sendRetryQueue(Queue<Tip> retryQueue, int round) {
        if (retryQueue.isEmpty()) {
            // After this round, check if any games still failed
            if (!failedGames.isEmpty()) {
                scheduler.schedule(() -> startRetryPhase(round + 1), tipCycleRate, TimeUnit.SECONDS);
            }
            return;
        }

        Tip tip = retryQueue.poll();
        if (tip != null) {
            currentTip = tip;
            lastTipSent = System.currentTimeMillis();

            if (Autotip.DEBUG) {
                Autotip.getInstance().getMessageUtil().log("§aRetry " + round + "/" + MAX_RETRY_ROUNDS + ": " + tip);
            }
            Autotip.getInstance().getMessageUtil().sendCommand(tip.asCommand());

            scheduler.schedule(() -> sendRetryQueue(retryQueue, round), tipCycleRate, TimeUnit.SECONDS);
        }
    }

    public long getLastTipWave() {
        return lastTipWave;
    }

    public long getNextTipWave() {
        return nextTipWave;
    }

    public int getQueueSize() {
        return gameQueue.size();
    }

    public int getKeepAliveRate() {
        return keepAliveRate;
    }

    public int getTipWaveRate() {
        return tipWaveRate;
    }

    public int getTipCycleRate() {
        return tipCycleRate;
    }

    public boolean isRunning() {
        return tipWaveTask != null && !tipWaveTask.isCancelled();
    }
}
