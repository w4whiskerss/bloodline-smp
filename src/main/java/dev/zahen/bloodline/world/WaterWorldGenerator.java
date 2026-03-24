package dev.zahen.bloodline.world;

import java.util.Random;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

public final class WaterWorldGenerator extends ChunkGenerator {

    private static final int FLOOR_Y = 60;
    private static final int WATER_TOP_Y = 64;

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        int minY = chunkData.getMinHeight();
        int maxY = chunkData.getMaxHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunkData.setBlock(x, minY, z, Material.BEDROCK);
                for (int y = minY + 1; y < Math.min(FLOOR_Y - 2, maxY); y++) {
                    chunkData.setBlock(x, y, z, Material.STONE);
                }
                chunkData.setBlock(x, FLOOR_Y - 2, z, Material.DEEPSLATE);
                chunkData.setBlock(x, FLOOR_Y - 1, z, Material.SANDSTONE);
                chunkData.setBlock(x, FLOOR_Y, z, Material.SAND);
                for (int y = FLOOR_Y + 1; y <= Math.min(WATER_TOP_Y, maxY - 1); y++) {
                    chunkData.setBlock(x, y, z, Material.WATER);
                }
            }
        }
    }

    @Override
    public int getBaseHeight(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull HeightMap heightMap) {
        return WATER_TOP_Y;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }
}
