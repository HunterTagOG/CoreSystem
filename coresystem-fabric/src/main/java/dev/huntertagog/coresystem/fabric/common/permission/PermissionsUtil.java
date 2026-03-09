package dev.huntertagog.coresystem.fabric.common.permission;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.model.ServerTargets;
import dev.huntertagog.coresystem.common.permission.CorePermission;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PermissionsUtil {

    private static final Logger LOG = LoggerFactory.get("PermissionsUtil");

    private PermissionsUtil() {
    }

    private static PermissionService getPermissionService() {
        return ServiceProvider.getService(PermissionService.class);
    }

    public static boolean hasDefault(ServerPlayerEntity player) {
        PermissionService service = getPermissionService();
        if (service == null) {
            CoreError error = CoreError.of(
                    CoreErrorCode.PERMISSION_CHECK_FAILED,
                    CoreErrorSeverity.WARN,
                    "PermissionService not available while checking SERVERSWITCH_DEFAULT – falling back to 'true'."
            ).withContextEntry("player", player.getGameProfile().getName());

            LOG.warn(error.toLogString());
            // Fallback: default darf erst mal alles
            return true;
        }

        try {
            return service.has(player, CorePermission.SERVERSWITCH_DEFAULT);
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.PERMISSION_CHECK_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Exception while checking SERVERSWITCH_DEFAULT."
                    )
                    .withContextEntry("player", player.getGameProfile().getName())
                    .withCause(e);

            LOG.error(error.toLogString(), e);
            // konservativer Fallback: Default trotzdem zulassen
            return true;
        }
    }

    public static boolean hasVip(ServerPlayerEntity player) {
        PermissionService service = getPermissionService();
        if (service == null) {
            CoreError error = CoreError.of(
                    CoreErrorCode.PERMISSION_CHECK_FAILED,
                    CoreErrorSeverity.WARN,
                    "PermissionService not available while checking SERVERSWITCH_VIP – falling back to 'false'."
            ).withContextEntry("player", player.getGameProfile().getName());

            LOG.warn(error.toLogString());
            return false;
        }

        try {
            return service.has(player, CorePermission.SERVERSWITCH_VIP);
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.PERMISSION_CHECK_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Exception while checking SERVERSWITCH_VIP."
                    )
                    .withContextEntry("player", player.getGameProfile().getName())
                    .withCause(e);

            LOG.error(error.toLogString(), e);
            // VIP lieber nicht „versehentlich“ geben
            return false;
        }
    }

    public static boolean hasAdmin(ServerPlayerEntity player) {
        PermissionService service = getPermissionService();
        if (service == null) {
            CoreError error = CoreError.of(
                    CoreErrorCode.PERMISSION_CHECK_FAILED,
                    CoreErrorSeverity.WARN,
                    "PermissionService not available while checking SERVERSWITCH_ADMIN – falling back to 'false'."
            ).withContextEntry("player", player.getGameProfile().getName());

            LOG.warn(error.toLogString());
            return false;
        }

        try {
            return service.has(player, CorePermission.SERVERSWITCH_ADMIN);
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.PERMISSION_CHECK_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Exception while checking SERVERSWITCH_ADMIN."
                    )
                    .withContextEntry("player", player.getGameProfile().getName())
                    .withCause(e);

            LOG.error(error.toLogString(), e);
            // Admin-Recht aus Sicherheitsgründen nicht vergeben
            return false;
        }
    }

    public static ServerTargets.Level resolveLevel(ServerPlayerEntity player) {
        PermissionService service = getPermissionService();

        if (service == null) {
            CoreError error = CoreError.of(
                    CoreErrorCode.PERMISSION_CHECK_FAILED,
                    CoreErrorSeverity.WARN,
                    "PermissionService not available while resolving permission level – fallback to DEFAULT."
            ).withContextEntry("player", player.getGameProfile().getName());

            LOG.warn(error.toLogString());
            return ServerTargets.Level.DEFAULT;
        }

        try {
            if (service.has(player, CorePermission.SERVERSWITCH_ADMIN)) {
                return ServerTargets.Level.ADMIN;
            }
            if (service.has(player, CorePermission.SERVERSWITCH_VIP)) {
                return ServerTargets.Level.VIP;
            }
            return ServerTargets.Level.DEFAULT;
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.PERMISSION_CHECK_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Exception while resolving permission level."
                    )
                    .withContextEntry("player", player.getGameProfile().getName())
                    .withCause(e);

            LOG.error(error.toLogString(), e);
            // Defensiver Fallback: DEFAULT
            return ServerTargets.Level.DEFAULT;
        }
    }
}
