package dev.huntertagog.coresystem.platform.task;

import dev.huntertagog.coresystem.platform.provider.Service;

/**
 * Zentrales Scheduling-Interface für das CoreSystem.
 * Kapselt sync/async-Dispatch und Tick-basierte Verzögerungen.
 */
public interface TaskScheduler extends Service {

    /**
     * Führt einen Task im nächsten Server-Tick auf dem Main-Thread aus.
     */
    void runSync(Runnable task);

    /**
     * Führt einen Task nach delayTicks Server-Ticks auf dem Main-Thread aus.
     *
     * @param task       auszuführender Task
     * @param delayTicks Delay in Ticks (1 Tick = 50ms bei 20 TPS)
     */
    void runSyncDelayed(Runnable task, int delayTicks);

    /**
     * Führt einen Task wiederholt auf dem Main-Thread aus.
     *
     * @param task              auszuführender Task
     * @param initialDelayTicks initiale Verzögerung in Ticks
     * @param periodTicks       Wiederholungsintervall in Ticks
     * @return Handle, um den Task zu steuern (cancel, pause, resume)
     */
    ScheduledTask runSyncRepeating(Runnable task, int initialDelayTicks, int periodTicks);

    /**
     * Führt einen Task asynchron in einem separaten Thread aus.
     * Achtung: Kein direkter Zugriff auf Minecraft-API außerhalb des Main-Threads.
     */
    void runAsync(Runnable task);

    /**
     * Wird idealerweise beim Server-Shutdown aufgerufen, um Threads sauber zu beenden.
     */
    void shutdown();

    /**
     * Handle für wiederkehrende Tasks.
     */
    interface ScheduledTask {
        void cancel();

        boolean isCancelled();

        void pause();

        void resume();

        boolean isPaused();
    }
}
