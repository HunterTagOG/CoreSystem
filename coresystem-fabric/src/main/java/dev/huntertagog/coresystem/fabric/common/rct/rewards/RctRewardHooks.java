package dev.huntertagog.coresystem.fabric.common.rct.rewards;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public final class RctRewardHooks {

    private static final Logger LOG = LoggerFactory.get("RCT-Rewards");
    private static final RctRewardService REWARDS = new RctRewardService();

    private RctRewardHooks() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            REWARDS.seedDefaultsIfMissing();
            tryRegisterBattleWinHook(server);
        });
    }

    private static void tryRegisterBattleWinHook(MinecraftServer server) {
        // --- OPTION A: RCT API Event (wenn vorhanden) ---
        // Du musst ggf. den Class/Field-Namen anpassen, sobald du im RCT-API eine Event-Klasse findest.
        try {
            Class<?> events = Class.forName("com.gitlab.srcmc.rctapi.api.events.RCTEvents");
            // Beispiel: public static final Event<...> TRAINER_BATTLE_WIN;
            var field = events.getDeclaredField("TRAINER_BATTLE_WIN");
            Object event = field.get(null);

            // Fabric Event pattern: event.register(listener)
            var register = event.getClass().getMethod("register", Object.class);

            // Listener als Lambda über Proxy (damit wir keine harte Typbindung brauchen)
            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                    RctRewardHooks.class.getClassLoader(),
                    register.getParameterTypes()[0].isInterface()
                            ? new Class[]{register.getParameterTypes()[0]}
                            : new Class[]{},
                    (proxy, method, args) -> {
                        // Erwartung: args enthält player + trainerId (oder trainer entity)
                        // -> hier musst du einmal loggen und dann sauber mappen.
                        // Minimal: wir loggen und brechen nicht.
                        try {
                            // Heuristik:
                            // args[0] = ServerPlayerEntity, args[1] = String trainerId
                            if (args != null && args.length >= 2
                                    && args[0] instanceof net.minecraft.server.network.ServerPlayerEntity player
                                    && args[1] instanceof String trainerId) {
                                REWARDS.grantOnVictory(server, player, trainerId);
                            }
                        } catch (Exception e) {
                            LOG.warn("Failed to grant RCT reward via RCTEvents hook", e);
                        }
                        return null;
                    }
            );

            register.invoke(event, listener);
            LOG.info("Registered RCT TRAINER_BATTLE_WIN hook via reflection.");
            return;

        } catch (Throwable ignored) {
            // --- OPTION B: fallback (Cobblemon battle end) -> später passend einklinken ---
            LOG.warn("RCTEvents TRAINER_BATTLE_WIN hook not found. You need to bind Cobblemon battle-end event for rewards.");
        }
    }

    // Für OPTION B würdest du später sowas aufrufen:
    // REWARDS.grantOnVictory(server, player, trainerId);
}
