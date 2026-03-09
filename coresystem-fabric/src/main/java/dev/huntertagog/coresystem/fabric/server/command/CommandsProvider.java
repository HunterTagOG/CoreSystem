package dev.huntertagog.coresystem.fabric.server.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.permission.CorePermission;
import dev.huntertagog.coresystem.platform.command.CommandMeta;
import dev.huntertagog.coresystem.platform.provider.Service;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Zentrale Drehscheibe für alle CoreSystem-Commands:
 * - Discover via Reflections
 * - Meta-Tracking (CommandMeta)
 * - Permission-Validierung (CorePermission)
 */
public final class CommandsProvider implements Service {

    private static final Logger LOGGER = LoggerFactory.get("CommandsProvider");

    /**
     * Alle aktivierten Command-Instanzen.
     */
    private final Set<CoreCommand> commands = new LinkedHashSet<>();

    /**
     * Command → zugehöriges CommandMeta (falls vorhanden).
     */
    private final Map<CoreCommand, CommandMeta> metaByCommand = new IdentityHashMap<>();

    /**
     * Permission → Command-Klassen, die diese Permission nutzen.
     */
    private final Map<CorePermission, Set<Class<? extends CoreCommand>>> permissionIndex =
            new EnumMap<>(CorePermission.class);

    public CommandsProvider() {
        try {
            discoverCommands();
        } catch (Exception e) {
            CoreError error = new CoreError(
                    CoreErrorCode.COMMAND_DISCOVERY_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to discover CoreSystem commands via Reflections",
                    e,
                    Collections.emptyMap()
            );
            LOGGER.error(error.toLogString(), e);
        }

        try {
            validatePermissions();
        } catch (Exception e) {
            CoreError error = new CoreError(
                    CoreErrorCode.COMMAND_DISCOVERY_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to validate command permissions",
                    e,
                    Collections.emptyMap()
            );
            LOGGER.error(error.toLogString(), e);
        }

        try {
            validateRequiresUsage();
        } catch (Exception e) {
            CoreError error = new CoreError(
                    CoreErrorCode.COMMAND_DISCOVERY_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Failed to validate requires() usage on commands",
                    e,
                    Collections.emptyMap()
            );
            LOGGER.error(error.toLogString(), e);
        }
    }

    // ------------------------------------------------------------
    // Discovery
    // ------------------------------------------------------------

    private void discoverCommands() {
        // Package mit deinen Command-Implementierungen
        Reflections reflections = new Reflections("dev.huntertagog.coresystem.server.command.impl");

        Set<Class<? extends CoreCommand>> classes = reflections.getSubTypesOf(CoreCommand.class);

        for (Class<? extends CoreCommand> clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            CommandMeta meta = clazz.getAnnotation(CommandMeta.class);
            if (meta != null && !meta.enabled()) {
                LOGGER.info("Skipping disabled command {}", clazz.getName());
                continue;
            }

            try {
                CoreCommand instance = clazz.getDeclaredConstructor().newInstance();
                this.commands.add(instance);

                if (meta != null) {
                    this.metaByCommand.put(instance, meta);
                }

                LOGGER.info("Discovered command: {}", clazz.getName());
            } catch (Exception e) {
                CoreError error = new CoreError(
                        CoreErrorCode.COMMAND_DISCOVERY_FAILED,
                        CoreErrorSeverity.ERROR,
                        "Failed to instantiate command",
                        e,
                        Map.of(
                                "commandClass", clazz.getName()
                        )
                );
                LOGGER.error(error.toLogString(), e);
            }
        }

        LOGGER.info("CommandsProvider discovered {} command(s).", this.commands.size());
    }

    // ------------------------------------------------------------
    // Permission-Validierung & Index
    // ------------------------------------------------------------

    private void validatePermissions() {
        // Baseline: mapping von Permission-String → CorePermission
        Map<String, CorePermission> permissionByKey = new HashMap<>();
        for (CorePermission perm : CorePermission.values()) {
            permissionByKey.put(perm.key(), perm);
        }

        for (Map.Entry<CoreCommand, CommandMeta> entry : metaByCommand.entrySet()) {
            CoreCommand command = entry.getKey();
            CommandMeta meta = entry.getValue();

            String permKey = meta.permission();
            if (permKey == null || permKey.isEmpty()) {
                CoreError error = CoreError.of(
                                CoreErrorCode.COMMAND_PERMISSION_MAPPING_WARNING,
                                CoreErrorSeverity.WARN,
                                "Command is missing a permission in @CommandMeta (permission=\"\")"
                        )
                        .withContextEntry("commandClass", command.getClass().getName());
                LOGGER.warn(error.toLogString());
                continue;
            }

            CorePermission resolved = permissionByKey.get(permKey);
            if (resolved == null) {
                // Harte Warnung: Permission-String hat kein Matching im Enum
                CoreError error = CoreError.of(
                                CoreErrorCode.COMMAND_PERMISSION_MAPPING_WARNING,
                                CoreErrorSeverity.WARN,
                                "Command uses unknown permission key"
                        )
                        .withContextEntry("commandClass", command.getClass().getName())
                        .withContextEntry("permissionKey", permKey);
                LOGGER.warn(error.toLogString());
                continue;
            }

            // Index aufbauen: welche Commands nutzen welche Permission
            this.permissionIndex
                    .computeIfAbsent(resolved, k -> new LinkedHashSet<>())
                    .add((Class<? extends CoreCommand>) command.getClass());
        }

        LOGGER.info("Permission index built: {} permission(s) referenced by commands.", this.permissionIndex.size());
    }

    private void validateRequiresUsage() {
        for (Map.Entry<CoreCommand, CommandMeta> entry : metaByCommand.entrySet()) {
            CoreCommand command = entry.getKey();
            CommandMeta meta = entry.getValue();

            if (meta == null) {
                continue;
            }

            String permKey = meta.permission();
            if (permKey == null || permKey.isEmpty()) {
                // Command ohne Permission-String → kein requires() erzwingen
                continue;
            }

            if (!command.isRequiresUsed()) {
                CoreError error = CoreError.of(
                                CoreErrorCode.COMMAND_PERMISSION_MAPPING_WARNING,
                                CoreErrorSeverity.WARN,
                                "Command defines a permission in @CommandMeta but does not call .requires(...)"
                        )
                        .withContextEntry("commandClass", command.getClass().getName())
                        .withContextEntry("permissionKey", permKey);
                LOGGER.warn(error.toLogString());
            }
        }
    }

    // ------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------

    /**
     * Registriert alle Commands beim Brigadier-Dispatcher.
     */
    public void registerAll(CommandDispatcher<ServerCommandSource> dispatcher,
                            CommandRegistryAccess registryAccess,
                            CommandManager.RegistrationEnvironment environment) {

        for (CoreCommand command : this.commands) {
            try {
                command.register(dispatcher, registryAccess, environment);
            } catch (Exception e) {
                CoreError error = new CoreError(
                        CoreErrorCode.COMMAND_REGISTRATION_FAILED,
                        CoreErrorSeverity.ERROR,
                        "Error while registering command",
                        e,
                        Map.of(
                                "commandClass", command.getClass().getName(),
                                "environment", environment.name()
                        )
                );
                LOGGER.error(error.toLogString(), e);
            }
        }
    }

    /**
     * Alle Commands, die der Provider aktuell verwaltet.
     */
    public Set<CoreCommand> getCommands() {
        return Collections.unmodifiableSet(commands);
    }

    /**
     * Liefert das @CommandMeta einer Command-Instanz (falls vorhanden).
     */
    public Optional<CommandMeta> getMeta(CoreCommand command) {
        return Optional.ofNullable(this.metaByCommand.get(command));
    }

    /**
     * Mapping: CorePermission → Set der Command-Klassen,
     * die diese Permission per @CommandMeta referenzieren.
     */
    public Map<CorePermission, Set<Class<? extends CoreCommand>>> getPermissionIndex() {
        Map<CorePermission, Set<Class<? extends CoreCommand>>> copy =
                new EnumMap<>(CorePermission.class);
        this.permissionIndex.forEach(
                (perm, set) -> copy.put(perm, Collections.unmodifiableSet(set))
        );
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Alle CorePermissions, die von Commands tatsächlich genutzt werden.
     */
    public Set<CorePermission> getUsedPermissions() {
        return Collections.unmodifiableSet(this.permissionIndex.keySet());
    }
}
