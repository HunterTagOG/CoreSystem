package dev.huntertagog.coresystem.fabric.server.region;

import dev.huntertagog.coresystem.common.provider.ServiceProvider;
import dev.huntertagog.coresystem.common.region.RegionFlag;
import dev.huntertagog.coresystem.common.text.CoreMessage;
import dev.huntertagog.coresystem.fabric.common.region.RegionService;
import dev.huntertagog.coresystem.fabric.common.text.Messages;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public final class RegionGuardHooks {

    private RegionGuardHooks() {
    }

    public static void register() {

        /* ---------------------------------------------------
         * 1) BLOCK BREAK
         * --------------------------------------------------- */
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {

            if (!(world instanceof ServerWorld serverWorld)) return true;

            if (!(player instanceof ServerPlayerEntity spe)) {
                return true;
            }

            RegionService regions = ServiceProvider.getService(RegionService.class);
            if (regions == null) return true;

            if (regions.hasFlagAt(serverWorld, pos, RegionFlag.BLOCK_BREAK)) {
                deny(player, CoreMessage.REGION_BLOCK_BREAK_DENY);
                return false;
            }

            if (!RegionPermissionUtil.canBreak(serverWorld, pos, spe)) {
                deny(spe, CoreMessage.REGION_BLOCK_BREAK_DENY);
                return false;
            }

            return true;
        });


        /* ---------------------------------------------------
         * 2) BLOCK PLACE  (UseBlockCallback)
         * --------------------------------------------------- */
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            if (!(world instanceof ServerWorld serverWorld)) return ActionResult.PASS;

            if (!(player instanceof ServerPlayerEntity spe)) {
                return ActionResult.PASS;
            }

            RegionService regions = ServiceProvider.getService(RegionService.class);
            if (regions == null) return ActionResult.PASS;

            // Player möchte am Block pos einen Block setzen
            BlockPos pos = hitResult.getBlockPos().offset(hitResult.getSide());

            if (regions.hasFlagAt(serverWorld, pos, RegionFlag.BLOCK_PLACE)) {
                deny(player, CoreMessage.REGION_BLOCK_PLACE_DENY);
                return ActionResult.FAIL; // blockiert Block-Place
            }

            if (!RegionPermissionUtil.canPlace(serverWorld, pos, spe)) {
                deny(spe, CoreMessage.REGION_BLOCK_PLACE_DENY);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }


    private static void deny(PlayerEntity player, CoreMessage msg) {
        if (!(player instanceof ServerPlayerEntity serverPlayerEntity)) return;
        Messages.send(serverPlayerEntity, msg);

        // Visuelles Feedback
        player.swingHand(player.getActiveHand(), true);
    }
}
