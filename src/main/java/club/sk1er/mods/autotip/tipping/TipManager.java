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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TipManager {
    private final ScheduledExecutorService scheduler;
    private final Queue<Tip> tipQueue;

    private ScheduledFuture<?> keepAliveTask;
    private ScheduledFuture<?> tipWaveTask;
    private ScheduledFuture<?> tipCycleTask;

    private int keepAliveRate;
    private int tipWaveRate;
    private int tipCycleRate;

    private long lastTipWave;
    private long nextTipWave;

    public TipManager() {
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Autotip-Scheduler");
            t.setDaemon(true);
            return t;
        });
        this.tipQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Called after successful login - starts the keep-alive and tip wave tasks.
     */
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
            tipWaveTask = scheduler.scheduleAtFixedRate(
                    this::tipWave,
                    0,
                    tipWaveRate,
                    TimeUnit.SECONDS
            );
        }
    }

    /**
     * Called on logout or disconnect - cancels all scheduled tasks.
     */
    public void stop() {
        cancelTask(keepAliveTask);
        cancelTask(tipWaveTask);
        cancelTask(tipCycleTask);
        keepAliveTask = null;
        tipWaveTask = null;
        tipCycleTask = null;
        tipQueue.clear();
    }

    private void cancelTask(ScheduledFuture<?> task) {
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
        }
    }

    /**
     * Sends keep-alive ping to API to maintain session.
     */
    private void keepAlive() {
        if (!HypixelUtil.isOnHypixel() || !AuthManager.loggedIn) {
            return;
        }

        try {
            var request = AutotipAPIRequestFactory.createKeepAliveRequest(AuthManager.sessionKey);
            String response = Autotip.getInstance().getAutotipHttpClient().executeRequest(request);
            KeepAliveRecord record = KeepAliveRecord.fromResponseBody(response);

            if (record.success()) {
                System.out.println("[Autotip] Keep-alive sent");
            } else {
                System.err.println("[Autotip] Keep-alive failed: " + record.cause());
            }
        } catch (IOException e) {
            System.err.println("[Autotip] Keep-alive failed: " + e.getMessage());
        }
    }

    /**
     * Fetches new tips from the API and queues them for sending.
     */
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
            System.err.println("[Autotip] Failed to fetch tips, using default: " + e.getMessage());
            tipRecord = TipRecord.defaultTips();
        }

        tipQueue.addAll(tipRecord.tips());
        System.out.println("[Autotip] Tip queue: " + tipQueue);

        cancelTask(tipCycleTask);
        if (tipCycleRate > 0 && !tipQueue.isEmpty()) {
            tipCycleTask = scheduler.scheduleAtFixedRate(
                    this::tipCycle,
                    0,
                    tipCycleRate,
                    TimeUnit.SECONDS
            );
        }
    }

    /**
     * Sends one tip command from the queue.
     */
    private void tipCycle() {
        if (tipQueue.isEmpty() || !HypixelUtil.isOnHypixel()) {
            cancelTask(tipCycleTask);
            tipCycleTask = null;
            return;
        }

        Tip tip = tipQueue.poll();
        if (tip != null) {
            System.out.println("[Autotip] Tipping: " + tip);
            Autotip.getInstance().getMessageUtil().sendCommand(tip.asCommand());
        }
    }

    public long getLastTipWave() {
        return lastTipWave;
    }

    public long getNextTipWave() {
        return nextTipWave;
    }

    public int getQueueSize() {
        return tipQueue.size();
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
