package dev.huntertagog.coresystem.fabric.server.world;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.Nullable;

public final class GameRuleStore {

    private final Reference2BooleanMap<GameRules.Key<GameRules.BooleanRule>> booleanRules =
            new Reference2BooleanOpenHashMap<>();

    private final Reference2IntMap<GameRules.Key<GameRules.IntRule>> intRules =
            new Reference2IntOpenHashMap<>();

    // ----------------------------------------------------
    // Setter
    // ----------------------------------------------------

    public void set(GameRules.Key<GameRules.BooleanRule> key, boolean value) {
        this.booleanRules.put(key, value);
    }

    public void set(GameRules.Key<GameRules.IntRule> key, int value) {
        this.intRules.put(key, value);
    }

    // ----------------------------------------------------
    // Anwenden auf GameRules
    // ----------------------------------------------------

    public void applyTo(GameRules rules, @Nullable MinecraftServer server) {

        // ----------------------------
        // Boolean GameRules
        // ----------------------------
        Reference2BooleanMaps.fastForEach(this.booleanRules, entry -> {
            try {
                GameRules.BooleanRule rule = rules.get(entry.getKey());
                if (rule == null) {
                    CoreError.of(
                                    CoreErrorCode.MESSAGE_ERROR,
                                    CoreErrorSeverity.WARN,
                                    "GameRuleStore: BooleanRule key not found"
                            )
                            .withContextEntry("ruleKey", safeKey(entry.getKey()))
                            .log();
                    return;
                }

                rule.set(entry.getBooleanValue(), server);

            } catch (Throwable t) {
                CoreError.of(
                                CoreErrorCode.MESSAGE_ERROR,
                                CoreErrorSeverity.ERROR,
                                "GameRuleStore: Failed to apply BooleanRule"
                        )
                        .withContextEntry("ruleKey", safeKey(entry.getKey()))
                        .withContextEntry("value", String.valueOf(entry.getBooleanValue()))
                        .withContextEntry("exception", t.toString())
                        .log();
            }
        });

        // ----------------------------
        // Int GameRules
        // ----------------------------
        Reference2IntMaps.fastForEach(this.intRules, entry -> {
            try {
                GameRules.IntRule rule = rules.get(entry.getKey());
                if (rule == null) {
                    CoreError.of(
                                    CoreErrorCode.MESSAGE_ERROR,
                                    CoreErrorSeverity.WARN,
                                    "GameRuleStore: IntRule key not found"
                            )
                            .withContextEntry("ruleKey", safeKey(entry.getKey()))
                            .log();
                    return;
                }

                rule.set(entry.getIntValue(), server);

            } catch (Throwable t) {
                CoreError.of(
                                CoreErrorCode.MESSAGE_ERROR,
                                CoreErrorSeverity.ERROR,
                                "GameRuleStore: Failed to apply IntRule"
                        )
                        .withContextEntry("ruleKey", safeKey(entry.getKey()))
                        .withContextEntry("value", String.valueOf(entry.getIntValue()))
                        .withContextEntry("exception", t.toString())
                        .log();
            }
        });
    }

    // ----------------------------------------------------
    // Helper (saubere Ausgabe des Rule-Key)
    // ----------------------------------------------------

    private static String safeKey(GameRules.Key<?> key) {
        return key == null ? "<null>" : key.getName();
    }
}
