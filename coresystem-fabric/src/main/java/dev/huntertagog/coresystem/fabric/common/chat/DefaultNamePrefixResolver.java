package dev.huntertagog.coresystem.fabric.common.chat;

import dev.huntertagog.coresystem.common.permission.CorePermission;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.fabric.common.permission.PermissionService;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

public final class DefaultNamePrefixResolver implements NamePrefixResolver {

    private final PermissionService perms;

    public DefaultNamePrefixResolver() {
        this.perms = ServiceProvider.getService(PermissionService.class);
    }

    @Override
    public @Nullable Text resolvePrefix(ServerPlayerEntity player) {
        if (perms == null) {
            return null;
        }

        // Beispiel-Logik – justierbar:
        if (perms.has(player.getCommandSource(), CorePermission.STAFF_ADMIN)) {
            return Text.literal("[Admin] ").formatted(Formatting.RED, Formatting.BOLD);
        }
        if (perms.has(player.getCommandSource(), CorePermission.STAFF_MOD)) {
            return Text.literal("[Mod] ").formatted(Formatting.BLUE, Formatting.BOLD);
        }
        if (perms.has(player.getCommandSource(), CorePermission.VIP)) {
            return Text.literal("[VIP] ").formatted(Formatting.GOLD);
        }

        return null; // kein Prefix
    }
}
