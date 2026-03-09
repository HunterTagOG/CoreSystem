package dev.huntertagog.coresystem.fabric.server.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.huntertagog.coresystem.common.command.BaseCommand;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.permission.CorePermission;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.fabric.common.permission.PermissionService;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Map;

import static dev.huntertagog.coresystem.common.command.BaseCommand.logger;
import static net.minecraft.server.command.CommandManager.literal;

public final class CommandUtil {

    private CommandUtil() {
    }

    // ------------------------------------------------------------------------
    // INTERNER HILFER – Permission check mit CoreError-Handling
    // ------------------------------------------------------------------------
    private static boolean safeHasPermission(
            PermissionService perms,
            ServerCommandSource source,
            CorePermission perm,
            BaseCommand owner
    ) {
        try {
            return perms != null && perms.has(source, perm);
        } catch (Exception e) {
            CoreError error = new CoreError(
                    CoreErrorCode.PERMISSION_CHECK_FAILED,
                    CoreErrorSeverity.ERROR,
                    "Permission check failed in CommandUtil.requirePermission()",
                    e,
                    Map.of(
                            "command", owner.getClass().getName(),
                            "permission", perm.key(),
                            "source", source.getName()
                    )
            );
            logger("CommandUtil").error(error.toLogString(), e);
            return false; // sicherer Fallback
        }
    }

    // ------------------------------------------------------------------------
    // requirePermission(...)
    // ------------------------------------------------------------------------
    public static LiteralArgumentBuilder<ServerCommandSource> requirePermission(
            BaseCommand command,
            CorePermission perm,
            LiteralArgumentBuilder<ServerCommandSource> builder
    ) {
        PermissionService permissionService = ServiceProvider.getService(PermissionService.class);

        return builder.requires(source -> safeHasPermission(permissionService, source, perm, command));
    }

    // ------------------------------------------------------------------------
    // literalWithPermission(...)
    // ------------------------------------------------------------------------
    public static LiteralArgumentBuilder<ServerCommandSource> literalWithPermission(
            BaseCommand command,
            String name,
            CorePermission perm
    ) {
        PermissionService permissionService = ServiceProvider.getService(PermissionService.class);

        return literal(name)
                .requires(source -> safeHasPermission(permissionService, source, perm, command));
    }
}
