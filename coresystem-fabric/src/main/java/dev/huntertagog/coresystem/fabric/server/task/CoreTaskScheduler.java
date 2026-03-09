package dev.huntertagog.coresystem.fabric.server.task;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.platform.task.TaskScheduler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default-Implementierung des TaskSchedulers.
 * - hängt am END_SERVER_TICK
 * - verwaltet immediate, delayed & repeating Tasks
 * - stellt einen einfachen Async-Executor bereit
 */
public final class CoreTaskScheduler implements TaskScheduler {

    private static final Logger LOG = LoggerFactory.get("TaskScheduler");

    private final MinecraftServer server;
    private final AtomicLong tickCounter = new AtomicLong(0L);

    // Tasks für den nächsten Tick
    private final ConcurrentLinkedQueue<Runnable> nextTickTasks = new ConcurrentLinkedQueue<>();

    // Delayed-Tasks (PriorityQueue nach executeAtTick)
    private final Queue<ScheduledEntry> delayedTasks =
            new PriorityQueue<>();

    // Wiederkehrende Tasks
    private final CopyOnWriteArrayList<RepeatingEntry> repeatingTasks =
            new CopyOnWriteArrayList<>();

    // Async Executor
    private final ExecutorService asyncExecutor;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public CoreTaskScheduler(MinecraftServer server) {
        this.server = server;

        this.asyncExecutor = new ThreadPoolExecutor(
                1,
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> {
                    Thread t = new Thread(runnable);
                    t.setName("CoreSystem-AsyncScheduler");
                    t.setDaemon(true);
                    return t;
                }
        );

        // Tick-Hook
        ServerTickEvents.END_SERVER_TICK.register(this::onTick);

        LOG.info("CoreTaskScheduler initialisiert und am Server-Tick registriert.");
    }

    // -------------------------------------------------------
    // TaskScheduler-API
    // -------------------------------------------------------

    @Override
    public void runSync(Runnable task) {
        if (task == null || shutdown.get()) return;
        nextTickTasks.offer(task);
    }

    @Override
    public void runSyncDelayed(Runnable task, int delayTicks) {
        if (task == null || shutdown.get()) return;
        if (delayTicks < 0) delayTicks = 0;

        long executeAt = tickCounter.get() + delayTicks;
        synchronized (delayedTasks) {
            delayedTasks.offer(new ScheduledEntry(executeAt, task));
        }
    }

    @Override
    public ScheduledTask runSyncRepeating(Runnable task, int initialDelayTicks, int periodTicks) {
        if (task == null || shutdown.get()) {
            // Dummy-Handle
            return new ScheduledTask() {
                @Override
                public void cancel() {
                }

                @Override
                public boolean isCancelled() {
                    return true;
                }

                @Override
                public void pause() {
                }

                @Override
                public void resume() {
                }

                @Override
                public boolean isPaused() {
                    return false;
                }
            };
        }

        if (initialDelayTicks < 0) initialDelayTicks = 0;
        if (periodTicks <= 0) {
            throw new IllegalArgumentException("periodTicks must be > 0");
        }

        long firstTick = tickCounter.get() + initialDelayTicks;
        RepeatingEntry entry = new RepeatingEntry(task, firstTick, periodTicks);
        repeatingTasks.add(entry);
        return entry;
    }

    @Override
    public void runAsync(Runnable task) {
        if (task == null || shutdown.get()) return;
        asyncExecutor.submit(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                CoreError error = CoreError.of(
                                CoreErrorCode.TASK_EXECUTION_FAILED,
                                CoreErrorSeverity.ERROR,
                                "Async task threw an exception in CoreTaskScheduler"
                        )
                        .withCause(t)
                        .withContextEntry("thread", Thread.currentThread().getName());

                LOG.error(error.toLogString(), t);
            }
        });
    }

    @Override
    public void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            return; // bereits heruntergefahren
        }

        LOG.info("Shutting down CoreTaskScheduler...");

        // Async-Executor sauber herunterfahren
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                CoreError error = CoreError.of(
                                CoreErrorCode.TASK_SCHEDULER_SHUTDOWN_TIMEOUT,
                                CoreErrorSeverity.WARN,
                                "Async executor did not terminate in time, forcing shutdownNow()"
                        )
                        .withContextEntry("timeoutSeconds", 5);

                LOG.warn(error.toLogString());
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.TASK_SCHEDULER_SHUTDOWN_INTERRUPTED,
                            CoreErrorSeverity.WARN,
                            "Interrupted while waiting for async executor shutdown"
                    )
                    .withCause(e);

            LOG.warn(error.toLogString(), e);
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Queues leeren
        nextTickTasks.clear();
        synchronized (delayedTasks) {
            delayedTasks.clear();
        }
        repeatingTasks.clear();

        LOG.info("CoreTaskScheduler shutdown completed.");
    }

    // -------------------------------------------------------
    // Tick-Verarbeitung
    // -------------------------------------------------------

    private void onTick(MinecraftServer server) {
        if (shutdown.get()) {
            return;
        }

        long currentTick = tickCounter.incrementAndGet();

        // 1) Sofort-Tasks
        Runnable r;
        while ((r = nextTickTasks.poll()) != null) {
            runSafeSync(r);
        }

        // 2) Delayed-Tasks, deren executeAtTick <= currentTick
        synchronized (delayedTasks) {
            while (true) {
                ScheduledEntry head = delayedTasks.peek();
                if (head == null || head.executeAtTick > currentTick) {
                    break;
                }
                delayedTasks.poll();
                runSafeSync(head.task);
            }
        }

        // 3) Repeating-Tasks
        for (RepeatingEntry entry : repeatingTasks) {
            if (entry.isCancelled()) {
                // optional: komplett aus Liste entfernen
                repeatingTasks.remove(entry);
                continue;
            }
            if (entry.isPaused()) {
                continue;
            }
            if (entry.nextRunTick <= currentTick) {
                runSafeSync(entry.task);
                entry.nextRunTick = currentTick + entry.periodTicks;
            }
        }
    }

    private void runSafeSync(Runnable task) {
        try {
            // Wir sind bereits im Server Main-Thread, da END_SERVER_TICK dort läuft
            task.run();
        } catch (Throwable t) {
            CoreError error = CoreError.of(
                            CoreErrorCode.TASK_EXECUTION_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Sync scheduled task threw an exception in CoreTaskScheduler"
                    )
                    .withCause(t);

            LOG.error(error.toLogString(), t);
        }
    }

    // -------------------------------------------------------
    // Interne Strukturen
    // -------------------------------------------------------

    private static final class ScheduledEntry implements Comparable<ScheduledEntry> {
        private final long executeAtTick;
        private final Runnable task;

        private ScheduledEntry(long executeAtTick, Runnable task) {
            this.executeAtTick = executeAtTick;
            this.task = task;
        }

        @Override
        public int compareTo(ScheduledEntry o) {
            return Long.compare(this.executeAtTick, o.executeAtTick);
        }
    }

    private static final class RepeatingEntry implements ScheduledTask {
        private final Runnable task;
        private final int periodTicks;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean paused = new AtomicBoolean(false);
        private volatile long nextRunTick;

        private RepeatingEntry(Runnable task, long firstTick, int periodTicks) {
            this.task = task;
            this.nextRunTick = firstTick;
            this.periodTicks = periodTicks;
        }

        @Override
        public void cancel() {
            cancelled.set(true);
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public void pause() {
            paused.set(true);
        }

        @Override
        public void resume() {
            paused.set(false);
        }

        @Override
        public boolean isPaused() {
            return paused.get();
        }
    }
}
