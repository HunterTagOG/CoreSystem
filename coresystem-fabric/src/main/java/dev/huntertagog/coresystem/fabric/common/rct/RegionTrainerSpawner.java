package dev.huntertagog.coresystem.fabric.common.rct;

import com.gitlab.srcmc.rctapi.api.RCTApi;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import dev.huntertagog.coresystem.common.rct.RctRegionSpawnStore;
import dev.huntertagog.coresystem.fabric.CoresystemCommon;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class RegionTrainerSpawner {

    private static final String MOD_ID = CoresystemCommon.MOD_ID;

    private RegionTrainerSpawner() {
    }

    /**
     * Spawn exactly once globally for (worldId, regionId).
     */
    public static void ensureSpawned(ServerWorld world,
                                     String regionId,
                                     String trainerId,
                                     BlockPos spawnPos) {

        Identifier wid = world.getRegistryKey().getValue();
        String worldId = wid.toString();

        if (RctRegionSpawnStore.isSpawned(worldId, regionId)) {
            return;
        }

        // Spawn entity (example: villager)
        VillagerEntity villager = EntityType.VILLAGER.create(world);
        if (villager == null) return;

        villager.refreshPositionAndAngles(
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                0f,
                0f
        );
        villager.setPersistent();
        villager.setCustomNameVisible(true);

        // (Optional) name from your trainer model or config:
        villager.setCustomName(net.minecraft.text.Text.literal(trainerId));

        world.spawnEntity(villager);

        // Attach RCT trainer to this entity
        RCTApi api = RCTApi.getInstance(MOD_ID);
        TrainerNPC trainer = api.getTrainerRegistry().getById(trainerId, TrainerNPC.class);
        if (trainer != null) {
            trainer.setEntity(villager); // important
        }

        RctRegionSpawnStore.markSpawned(worldId, regionId);
    }
}
