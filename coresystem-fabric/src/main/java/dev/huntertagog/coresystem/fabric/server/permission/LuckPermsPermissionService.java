package dev.huntertagog.coresystem.fabric.server.permission;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.permission.CorePermission;
import dev.huntertagog.coresystem.fabric.common.permission.PermissionService;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class LuckPermsPermissionService implements PermissionService {

    private static final Logger LOGGER = LoggerFactory.get("LuckPermsPermissionService");

    private final LuckPerms luckPerms;

    public LuckPermsPermissionService() {
        LuckPerms lp = null;
        try {
            lp = LuckPermsProvider.get();
            LOGGER.info("LuckPermsPermissionService initialized – LuckPerms API available.");
        } catch (Exception ex) {

            // ------- CoreError Integration -------
            CoreError.of(
                            CoreErrorCode.PERMISSION_BACKEND_NOT_AVAILABLE,
                            CoreErrorSeverity.WARN,
                            "LuckPerms not present or not initialized."
                    ).withCause(ex)
                    .log();
            // -------------------------------------

        }
        this.luckPerms = lp;
    }

    @Override
    public boolean has(ServerCommandSource source, CorePermission perm) {
        return has(source, perm.key());
    }

    @Override
    public boolean has(ServerCommandSource source, String permissionNode) {

        // Konsole & Commandblocks: immer durchwinken
        if (!source.isExecutedByPlayer()) {
            return true;
        }

        // Fallback wenn LuckPerms fehlt
        if (luckPerms == null) {

            CoreError.of(
                            CoreErrorCode.PERMISSION_BACKEND_NOT_AVAILABLE,
                            CoreErrorSeverity.INFO,
                            "LuckPerms unavailable – using fallback permission policy."
                    ).withContextEntry("permission", permissionNode)
                    .log();

            return source.hasPermissionLevel(2);
        }

        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            return true;
        }

        try {
            var adapter = luckPerms.getPlayerAdapter(ServerPlayerEntity.class);
            var tristate = adapter.getPermissionData(player).checkPermission(permissionNode);
            return tristate.asBoolean();

        } catch (Exception ex) {

            CoreError.of(
                            CoreErrorCode.PERMISSION_CHECK_FAILED,
                            CoreErrorSeverity.ERROR,
                            "Exception while checking permission via LuckPerms."
                    )
                    .withContextEntry("player", player.getGameProfile().getName())
                    .withContextEntry("permission", permissionNode)
                    .withCause(ex)
                    .log();
            return false; // konservative Fail-Policy
        }
    }
}
