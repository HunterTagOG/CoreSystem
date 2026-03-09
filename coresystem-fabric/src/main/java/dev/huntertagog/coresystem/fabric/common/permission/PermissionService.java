package dev.huntertagog.coresystem.fabric.common.permission;

import dev.huntertagog.coresystem.common.permission.CorePermission;
import dev.huntertagog.coresystem.platform.provider.Service;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public interface PermissionService extends Service {

    /**
     * Permission-Check über CorePermission-Enum.
     */
    boolean has(ServerCommandSource source, CorePermission perm);

    /**
     * Permission-Check über reinen Node-String.
     */
    boolean has(ServerCommandSource source, String permissionNode);

    // ---------------------------------------------------------------------
    // Convenience-Overloads für direkte Player-Nutzung
    // ---------------------------------------------------------------------

    default boolean has(ServerPlayerEntity player, CorePermission perm) {
        if (player == null) {
            return false;
        }
        return has(player.getCommandSource(), perm);
    }

    default boolean has(ServerPlayerEntity player, String permissionNode) {
        if (player == null) {
            return false;
        }
        return has(player.getCommandSource(), permissionNode);
    }
}
