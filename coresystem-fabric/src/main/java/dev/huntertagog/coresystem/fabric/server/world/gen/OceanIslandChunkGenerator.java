package dev.huntertagog.coresystem.fabric.server.world.gen;

import com.mojang.serialization.MapCodec;
import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.fabric.server.world.island.IslandHeightmap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OceanIslandChunkGenerator extends ChunkGenerator {

    private static final Logger LOG = LoggerFactory.get("OceanIslandChunkGen");

    private final int minY;
    private final int worldHeight;
    private final int seaLevel;

    private final IslandHeightmap heightmap;

    // Vanilla-Template-Generator (für Caves/Carver/Fine-Tuning)
    private final ChunkGenerator templateGenerator;

    private static final BlockState STONE = Blocks.STONE.getDefaultState();
    private static final BlockState DIRT = Blocks.DIRT.getDefaultState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.getDefaultState();
    private static final BlockState WATER = Blocks.WATER.getDefaultState();

    // --- Dummy-CODEC nur für Persistenz ---
    public static final MapCodec<OceanIslandChunkGenerator> CODEC =
            MapCodec.unit(() -> {
                throw new UnsupportedOperationException(
                        "OceanIslandChunkGenerator should not be created via Codec (runtime-only)"
                );
            });

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    public OceanIslandChunkGenerator(ChunkGenerator templateWorld,
                                     ServerWorld world,
                                     IslandHeightmap heightmap) {
        super(
                new FixedBiomeSource(world.getBiomeAccess().getBiome(BlockPos.ORIGIN)),
                templateWorld::getGenerationSettings
        );

        this.templateGenerator = templateWorld;
        this.minY = world.getBottomY();
        this.worldHeight = world.getHeight();
        this.seaLevel = world.getSeaLevel();
        this.heightmap = heightmap;
    }

    // -------------------------------------------------
    // Carving: Vanilla-Caves + Custom Underwater-Tunnel
    // -------------------------------------------------
    @Override
    public void carve(ChunkRegion region,
                      long seed,
                      NoiseConfig noiseConfig,
                      BiomeAccess biomeAccess,
                      StructureAccessor structureAccessor,
                      Chunk chunk,
                      GenerationStep.Carver carverStep) {

        ChunkPos pos = chunk.getPos();

        try {
            // 1) Vanilla-Carver laufen lassen (Caves, Aquifers etc.)
            templateGenerator.carve(region, seed, noiseConfig, biomeAccess, structureAccessor, chunk, carverStep);

            // 2) Unsere Underwater-Tunnel nur im AIR-Step
            if (carverStep == GenerationStep.Carver.AIR) {
                carveUnderwaterTunnels(chunk);
            }
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.ERROR,
                            "Exception during carve in OceanIslandChunkGenerator"
                    )
                    .withCause(e)
                    .withContextEntry("chunkX", pos.x)
                    .withContextEntry("chunkZ", pos.z)
                    .withContextEntry("carverStep", carverStep.name());

            LOG.error(error.toLogString(), e);
        }
    }

    @Override
    public void buildSurface(ChunkRegion region,
                             StructureAccessor structures,
                             NoiseConfig noiseConfig,
                             Chunk chunk) {
        // Terrain kommt vollständig aus populateNoise
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Entities werden über Biome / späteren Pass gesteuert
    }

    @Override
    public int getWorldHeight() {
        return this.worldHeight;
    }

    // ------------------------------------
    // Kern: Terrain + Cliff-Caves / Spikes
    // ------------------------------------
    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender,
                                                  NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor,
                                                  Chunk chunk) {

        ChunkPos pos = chunk.getPos();

        try {
            int startX = pos.getStartX();
            int startZ = pos.getStartZ();
            int endX = pos.getEndX();
            int endZ = pos.getEndZ();

            for (int x = startX; x <= endX; x++) {
                for (int z = startZ; z <= endZ; z++) {

                    int localX = x - startX;
                    int localZ = z - startZ;

                    // genau EIN mal abfragen, danach nur noch weiterreichen
                    int terrainY = heightmap.sampleHeight(x, z);
                    int floorY = heightmap.sampleSeafloorHeight(x, z);

                    // --- reiner Ozean (keine Insel) ---
                    if (terrainY < 0) {
                        // Seafloor → STONE
                        for (int y = minY; y <= floorY; y++) {
                            setBlock(chunk, localX, y, localZ, STONE);
                        }
                        // darüber Wasser bis Meeresspiegel
                        for (int y = floorY + 1; y <= seaLevel; y++) {
                            setBlock(chunk, localX, y, localZ, WATER);
                        }
                        continue;
                    }

                    // -----------------------------
                    // 1. Solider Unterbau: Stone
                    // -----------------------------
                    int groundHeight = Math.max(minY + 1, terrainY - 5);
                    for (int y = minY; y < groundHeight; y++) {
                        setBlock(chunk, localX, y, localZ, STONE);
                    }

                    // -----------------------------
                    // 2. Erdschicht (Dirt)
                    // -----------------------------
                    for (int y = groundHeight; y < terrainY; y++) {
                        setBlock(chunk, localX, y, localZ, DIRT);
                    }

                    // -----------------------------
                    // 3. Oberfläche (Grass)
                    // -----------------------------
                    setBlock(chunk, localX, terrainY, localZ, GRASS);

                    // -----------------------------
                    // 4. Wasser auffüllen, falls Insel im Wasser
                    // -----------------------------
                    for (int y = terrainY + 1; y <= seaLevel; y++) {
                        setBlock(chunk, localX, y, localZ, WATER);
                    }

                    // --------------------------------
                    // 5. Cliff-Caves & Spikes an Wand
                    // --------------------------------
                    if (terrainY > seaLevel && floorY < seaLevel - 3) {

                        // 5.1 Risse/Höhlen in der Wand
                        for (int y = floorY + 2; y <= terrainY - 3; y++) {
                            if (heightmap.isCliffCavity(x, y, z)) {
                                setBlock(chunk, localX, y, localZ, WATER);
                            }
                        }

                        // 5.2 Spikes / Felsnasen aus Boden/Wand
                        for (int y = floorY + 1; y <= seaLevel - 2; y++) {
                            if (heightmap.isCliffSpike(x, y, z)) {
                                BlockState current = chunk.getBlockState(new BlockPos(localX, y, localZ));
                                if (current.isOf(Blocks.WATER)) {
                                    setBlock(chunk, localX, y, localZ, STONE);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            CoreError error = CoreError.of(
                            CoreErrorCode.MESSAGE_ERROR,
                            CoreErrorSeverity.ERROR,
                            "Exception during populateNoise in OceanIslandChunkGenerator"
                    )
                    .withCause(e)
                    .withContextEntry("chunkX", pos.x)
                    .withContextEntry("chunkZ", pos.z);

            LOG.error(error.toLogString(), e);
        }

        // Im Fehlerfall: Chunk trotzdem zurückgeben (teilweise generiert)
        return CompletableFuture.completedFuture(chunk);
    }

    private void setBlock(Chunk chunk, int localX, int y, int localZ, BlockState state) {
        if (state == null) return;
        int relY = y - minY;
        if (relY < 0 || relY >= worldHeight) return;
        chunk.setBlockState(new BlockPos(localX, y, localZ), state, false);
    }

    @Override
    public int getSeaLevel() {
        return this.seaLevel;
    }

    @Override
    public int getMinimumY() {
        return this.minY;
    }

    @Override
    public int getHeight(int x,
                         int z,
                         Heightmap.Type heightmapType,
                         HeightLimitView world,
                         NoiseConfig noiseConfig) {
        int h = heightmap.sampleHeight(x, z);
        return h < 0 ? minY : h;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x,
                                               int z,
                                               HeightLimitView world,
                                               NoiseConfig noiseConfig) {
        int h = heightmap.sampleHeight(x, z);
        if (h < 0) {
            return new VerticalBlockSample(minY, new BlockState[0]);
        }
        BlockState[] states = new BlockState[h - minY + 1];
        for (int i = 0; i < states.length; i++) {
            states[i] = (i == states.length - 1) ? GRASS : STONE;
        }
        return new VerticalBlockSample(minY, states);
    }

    @Override
    public void getDebugHudText(List<String> text,
                                NoiseConfig noiseConfig,
                                BlockPos pos) {
        text.add("Generator: OceanIslandChunkGenerator");
    }

    // -------------------------------------------------
    // Custom Underwater-Tunnel (Cave-Feld aus Heightmap)
    // -------------------------------------------------
    private void carveUnderwaterTunnels(Chunk chunk) {
        ChunkPos pos = chunk.getPos();
        int startX = pos.getStartX();
        int startZ = pos.getStartZ();

        for (int localX = 0; localX < 16; localX++) {
            int x = startX + localX;

            for (int localZ = 0; localZ < 16; localZ++) {
                int z = startZ + localZ;

                int seafloorY = heightmap.sampleSeafloorHeight(x, z);
                int minY = Math.max(this.minY + 1, seafloorY + 2);
                int maxY = this.seaLevel - 3;

                if (maxY <= minY) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    if (!heightmap.isUnderwaterCaveAt(x, y, z)) {
                        continue;
                    }

                    BlockPos p = new BlockPos(localX, y, localZ);
                    BlockState current = chunk.getBlockState(p);

                    if (!current.isAir() && current.getBlock() != Blocks.WATER) {
                        chunk.setBlockState(p, WATER, false);
                    }
                }
            }
        }
    }
}
