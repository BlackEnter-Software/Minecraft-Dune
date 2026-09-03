package com.blackenter.minecraftdune.worldgen.arrakis;

import com.blackenter.minecraftdune.worldgen.geology.*;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Function;
import java.util.function.Predicate;

/** Authoritative profile-6000 composition, shared by chunk and base-column writers. */
public record BuriedTerrainColumn(long seed, int x, int z, RawRockSurfaceField.Sample raw,
        SedimentSurfaceField.Sample sediment, RockErosionField.Sample erosion,
        MassifFractureField.Sample fracture, LithologyField.Column lithology,
        TalusColluviumField.Sample talus, int compactionDepth) {
    public int rockTopY() { return (int) Math.floor(erosion.rockTop()); }
    public double sedimentThickness() { return Math.max(0, sediment.surfaceY() - erosion.rockTop()); }
    public double externalSurface() { return Math.max(rockTopY(), sediment.surfaceY()); }
    public double finalSurface() { return talus.active() ? Math.max(externalSurface(), talus.topY()) : externalSurface(); }
    public int highestOccupiedY() { return Math.max(Math.max(rockTopY(), sediment.highestY()), talus.topY()); }

    /** Minecraft heightmap predicates differ: a thin layer need not block motion. */
    public int baseHeight(int minimumY, int maximumYExclusive, Predicate<Cell> opaque) {
        for (int y = Math.min(highestOccupiedY(), maximumYExclusive - 1); y >= minimumY; y--) {
            if (opaque.test(cellAt(y, minimumY))) return y + 1;
        }
        return minimumY;
    }

    public Cell cellAt(int y, int worldBottom) {
        if (y < worldBottom) return Cell.AIR;
        // Keep the existing bottom bedrock layer. Geology starts above it, not at Y65.
        if (y == worldBottom) return Cell.BEDROCK;
        if (y <= rockTopY()) return new Cell(Kind.ROCK, lithology.sample(y).material(), 0);
        if (talus.active() && y >= talus.bottomY() && y <= talus.topY()) {
            if (TalusColluviumField.isDistalSand(seed, x, y, z, talus)) return Cell.SAND;
            return new Cell(Kind.TALUS, TalusColluviumField.materialAt(seed, x, y, z, talus), 0);
        }
        if (y <= sediment.fullTopY()) {
            return sediment.fullTopY() - y >= compactionDepth ? Cell.SANDSTONE : Cell.SAND;
        }
        if (y == sediment.fullTopY() + 1 && sediment.partialLayers() > 0) {
            return new Cell(Kind.SAND_LAYER, null, sediment.partialLayers());
        }
        return Cell.AIR;
    }

    /** Includes air to replace the temporary FlatLevelSource substrate completely. */
    public void compose(int minimumY, int maximumYExclusive, CellWriter writer) {
        for (int y = minimumY; y < maximumYExclusive; y++) writer.set(y, cellAt(y, minimumY));
    }

    /** FlatLevelSource's buffer is only its configured substrate depth; allocate the full world. */
    public NoiseColumn toNoiseColumn(int minimumY, int maximumYExclusive, Function<Cell, BlockState> palette) {
        var column = new NoiseColumn(minimumY, new BlockState[maximumYExclusive - minimumY]);
        compose(minimumY, maximumYExclusive, (y, cell) -> column.setBlock(y, palette.apply(cell)));
        return column;
    }

    @FunctionalInterface public interface CellWriter { void set(int y, Cell cell); }
    public enum Kind { AIR, BEDROCK, ROCK, SAND, SANDSTONE, SAND_LAYER, TALUS }
    public record Cell(Kind kind, LithologyField.Material material, int layers) {
        public static final Cell AIR = new Cell(Kind.AIR, null, 0);
        public static final Cell BEDROCK = new Cell(Kind.BEDROCK, null, 0);
        public static final Cell SAND = new Cell(Kind.SAND, null, 0);
        public static final Cell SANDSTONE = new Cell(Kind.SANDSTONE, null, 0);
    }
}
