package dev.huntertagog.coresystem.fabric.common.friends.follow;

import dev.huntertagog.coresystem.fabric.common.permission.PermissionService;
import net.minecraft.server.command.ServerCommandSource;

public final class FollowLimitResolver {

    private static final int DEFAULT_LIMIT = 3;

    private FollowLimitResolver() {
    }

    public static int resolveDailyLimit(PermissionService perms, ServerCommandSource src) {
        // Bypass → effectively unlimited
        if (perms.has(src, "coresystem.friend.follow.bypass")) {
            return Integer.MAX_VALUE;
        }

        // Suche nach limit.<N> (du kannst das auch härter machen, wenn du Permission-API anders hast)
        // Minimalistisch: check in absteigender Reihenfolge
        int[] candidates = {50, 25, 10, 5, 3, 1};
        for (int n : candidates) {
            if (perms.has(src, "coresystem.friend.follow.limit." + n)) {
                return n;
            }
        }
        return DEFAULT_LIMIT;
    }
}
