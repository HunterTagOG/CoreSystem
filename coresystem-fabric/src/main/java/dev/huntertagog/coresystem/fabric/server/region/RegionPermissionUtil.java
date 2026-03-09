package dev.huntertagog.coresystem.fabric.server.region;

import dev.huntertagog.coresystem.common.player.PlayerRelationshipUtil;
import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.region.RegionFlag;
import dev.huntertagog.coresystem.fabric.common.region.RegionService;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Region-Permissions inkl. Friends + Clans.
 * <p>
 * Regel:
 * - keine Region -> erlaubt
 * - Owner -> erlaubt
 * - Friend / Clan-Manager des Owners -> erlaubt
 * - Flag nicht gesetzt -> erlaubt
 * - Flag gesetzt und nicht trusted -> deny
 */
public final class RegionPermissionUtil {

    private RegionPermissionUtil() {
    }

    public static boolean canBreak(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        return canModify(world, pos, player, RegionFlag.BLOCK_BREAK);
    }

    public static boolean canPlace(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        return canModify(world, pos, player, RegionFlag.BLOCK_PLACE);
    }

    private static boolean canModify(ServerWorld world,
                                     BlockPos pos,
                                     ServerPlayerEntity player,
                                     RegionFlag flag) {

        RegionService regions = ServiceProvider.getService(RegionService.class);
        if (regions == null) {
            return true; // kein Region-System aktiv → kein Guard
        }

        List<RegionDefinition> regOpt = regions.findRegionsAt(world, pos);
        if (regOpt.isEmpty()) {
            return true; // keine Region an Pos → kein Guard
        }

        RegionDefinition region = regOpt.getFirst(); // die oberste Region
        UUID ownerId = region.ownerId();
        UUID actorId = player.getUuid();

        // Owner / Friends / Clan-Manager dürfen immer
        if (PlayerRelationshipUtil.isTrustedForOwner(ownerId, actorId)) {
            return true;
        }

        // Region ohne dieses Flag -> nichts zu tun
        return !region.flags().contains(flag);
    }
}
