package dev.huntertagog.coresystem.fabric.server.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public interface CoreCommand {

    /**
     * Registrierung im Brigadier-Dispatcher.
     */
    void register(CommandDispatcher<ServerCommandSource> dispatcher,
                  CommandRegistryAccess registryAccess,
                  CommandManager.RegistrationEnvironment environment);

    /**
     * Internes Flag: wurde in diesem Command ein `.requires(...)` gesetzt?
     */
    default void markRequiresUsed() {
    }

    /**
     * Read-only Flag für den CommandsProvider.
     */
    default boolean isRequiresUsed() {
        return false;
    }
}
