package dev.huntertagog.coresystem.fabric.server.islands;

import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.List;

public final class PrivateIslandWorldSchematicLoader {

    private static final Logger LOG = LoggerFactory.get("IslandSchematic");

    private static final List<Identifier> ISLAND_TEMPLATES = List.of(
            Identifier.of("coresystem", "starter_island_1"),
            Identifier.of("coresystem", "starter_island_2"),
            Identifier.of("coresystem", "starter_island_3")
    );

    private static final BlockRotation[] ROTATIONS = new BlockRotation[]{
            BlockRotation.NONE,
            BlockRotation.CLOCKWISE_90,
            BlockRotation.CLOCKWISE_180,
            BlockRotation.COUNTERCLOCKWISE_90
    };

    private PrivateIslandWorldSchematicLoader() {
    }

    public static void placeRandomStarterIsland(ServerWorld world) {
        // ✅ One-Time Gate (persistiert)

        var psm = world.getPersistentStateManager();

        // Type bereitstellen (dein TYPE mit Supplier + fromNbt)
        PrivateIslandInitState state = psm.getOrCreate(
                PrivateIslandInitState.TYPE,
                PrivateIslandInitState.KEY
        );

        if (state.isInitialized()) {
            // Already done -> kein Double-Placement
            return;
        }

        StructureTemplateManager stm = world.getStructureTemplateManager();
        Random random = world.getRandom();

        Identifier templateId = ISLAND_TEMPLATES.get(random.nextInt(ISLAND_TEMPLATES.size()));
        StructureTemplate template = stm.getTemplate(templateId).orElse(null);
        if (template == null) {
            LOG.warn("Missing starter template: {} (world={})", templateId, world.getRegistryKey().getValue());
            return;
        }

        BlockRotation rotation = ROTATIONS[random.nextInt(ROTATIONS.length)];
        BlockMirror mirror = BlockMirror.NONE;

        // ✅ “Business Default”: Spawn als Referenz (statt hart 0/64/0)
        BlockPos origin = world.getSpawnPos();
        // Optional: auf “Bodenhöhe” korrigieren, wenn dein Spawn nur ein Marker ist
        // origin = new BlockPos(origin.getX(), Math.max(origin.getY(), world.getSeaLevel() + 3), origin.getZ());

        // ✅ Chunk sicher laden (billig, vermeidet “silent fail”)
        world.getChunk(origin.getX() >> 4, origin.getZ() >> 4);

        StructurePlacementData placementData = new StructurePlacementData()
                .setRotation(rotation)
                .setMirror(mirror)
                // Performance/Consistency: Entities im Template in 99% der Fälle nicht gewünscht
                .setIgnoreEntities(true);

        // flags=2 ist ok (Block updates). Wenn du Redstone/Physics vermeiden willst: 18/34 je nach Bedarf.
        template.place(world, origin, origin, placementData, random, 2);

        // ✅ Marker setzen (idempotent + auditierbar)
        state.markInitialized();

        LOG.info("Placed starter island template={} rotation={} world={}",
                templateId, rotation, world.getRegistryKey().getValue());
    }
}
