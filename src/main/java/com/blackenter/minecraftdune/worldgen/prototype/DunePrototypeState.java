package com.blackenter.minecraftdune.worldgen.prototype;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent, dimension-local settings and ownership for the dune prototype laboratory. */
final class DunePrototypeState extends SavedData {
    private static final String DATA_NAME = "minecraftdune_dune_prototype";
    private static final Factory<DunePrototypeState> FACTORY = new Factory<>(
            DunePrototypeState::new,
            DunePrototypeState::load
    );

    private DuneSimulation.Settings settings = DuneSimulation.Settings.defaults();
    private DuneSurfaceResolution surfaceResolution = DuneSurfaceResolution.SIXTEENTH;
    private final Long2ObjectOpenHashMap<OwnedColumn> ownedColumns =
            new Long2ObjectOpenHashMap<>();

    static DunePrototypeState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    DuneSimulation.Settings settings() {
        return settings;
    }

    void settings(DuneSimulation.Settings settings) {
        this.settings = settings;
        setDirty();
    }

    DuneSurfaceResolution surfaceResolution() {
        return surfaceResolution;
    }

    void surfaceResolution(DuneSurfaceResolution surfaceResolution) {
        this.surfaceResolution = surfaceResolution;
        setDirty();
    }

    OwnedColumn ownership(int worldX, int worldZ) {
        return ownedColumns.getOrDefault(columnKey(worldX, worldZ), OwnedColumn.EMPTY);
    }

    void ownership(int worldX, int worldZ, OwnedColumn ownership) {
        long key = columnKey(worldX, worldZ);
        if (ownership.isEmpty()) {
            ownedColumns.remove(key);
        } else {
            ownedColumns.put(key, ownership);
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("cell_size", settings.cellSize());
        tag.putInt("maximum_height", settings.maximumHeightOverride());
        tag.putDouble("dune_spacing", settings.duneSpacingBlocks());
        tag.putDouble("spacing_variation", settings.spacingVariation());
        tag.putDouble("ridge_sharpness", settings.ridgeSharpness());
        tag.putDouble("valley_cutoff", settings.valleyCutoff());
        tag.putDouble("slope_asymmetry", settings.slopeAsymmetry());
        tag.putDouble("interdune_cleanup", settings.interduneCleanup());
        tag.putDouble("repose_angle", settings.reposeAngleDegrees());
        tag.putInt("cascade_passes", settings.cascadePasses());
        tag.putInt("transport_iterations", settings.transportIterationsOverride());
        tag.putDouble("wind_angle", settings.windAngleDegrees());
        tag.putInt("edge_blend", settings.edgeBlendCells());
        tag.putDouble("transport_strength", settings.transportStrength());
        tag.putString("surface_resolution", surfaceResolution.commandName());

        long[] keys = new long[ownedColumns.size()];
        long[] masks = new long[ownedColumns.size()];
        int[] partialData = new int[ownedColumns.size()];
        int index = 0;
        for (Long2ObjectMap.Entry<OwnedColumn> entry : ownedColumns.long2ObjectEntrySet()) {
            keys[index] = entry.getLongKey();
            OwnedColumn column = entry.getValue();
            masks[index] = column.fullBlockMask();
            partialData[index] = column.packedPartial();
            index++;
        }
        tag.putLongArray("owned_column_keys", keys);
        tag.putLongArray("owned_full_masks", masks);
        tag.putIntArray("owned_partial_data", partialData);
        return tag;
    }

    static DunePrototypeState load(CompoundTag tag, HolderLookup.Provider registries) {
        DunePrototypeState state = new DunePrototypeState();
        DuneSimulation.Settings defaults = DuneSimulation.Settings.defaults();
        state.settings = new DuneSimulation.Settings(
                boundedInt(tag, "cell_size", defaults.cellSize(),
                        DuneSimulation.Settings.MINIMUM_CELL_SIZE,
                        DuneSimulation.Settings.MAXIMUM_CELL_SIZE),
                boundedInt(tag, "maximum_height", defaults.maximumHeightOverride(),
                        0, DuneSimulation.Settings.MAXIMUM_ALLOWED_HEIGHT),
                boundedDouble(tag, "dune_spacing", defaults.duneSpacingBlocks(),
                        DuneSimulation.Settings.MINIMUM_DUNE_SPACING,
                        DuneSimulation.Settings.MAXIMUM_DUNE_SPACING),
                boundedDouble(tag, "spacing_variation", defaults.spacingVariation(),
                        DuneSimulation.Settings.MINIMUM_SPACING_VARIATION,
                        DuneSimulation.Settings.MAXIMUM_SPACING_VARIATION),
                boundedDouble(tag, "ridge_sharpness", defaults.ridgeSharpness(),
                        DuneSimulation.Settings.MINIMUM_RIDGE_SHARPNESS,
                        DuneSimulation.Settings.MAXIMUM_RIDGE_SHARPNESS),
                boundedDouble(tag, "valley_cutoff", defaults.valleyCutoff(),
                        DuneSimulation.Settings.MINIMUM_VALLEY_CUTOFF,
                        DuneSimulation.Settings.MAXIMUM_VALLEY_CUTOFF),
                boundedDouble(tag, "slope_asymmetry", defaults.slopeAsymmetry(),
                        DuneSimulation.Settings.MINIMUM_SLOPE_ASYMMETRY,
                        DuneSimulation.Settings.MAXIMUM_SLOPE_ASYMMETRY),
                boundedDouble(tag, "interdune_cleanup", defaults.interduneCleanup(),
                        DuneSimulation.Settings.MINIMUM_INTERDUNE_CLEANUP,
                        DuneSimulation.Settings.MAXIMUM_INTERDUNE_CLEANUP),
                boundedDouble(tag, "repose_angle", defaults.reposeAngleDegrees(),
                        DuneSimulation.Settings.MINIMUM_REPOSE_ANGLE,
                        DuneSimulation.Settings.MAXIMUM_REPOSE_ANGLE),
                boundedInt(tag, "cascade_passes", defaults.cascadePasses(),
                        0, DuneSimulation.Settings.MAXIMUM_CASCADE_PASSES),
                boundedInt(tag, "transport_iterations", defaults.transportIterationsOverride(),
                        0, DuneSimulation.Settings.MAXIMUM_TRANSPORT_ITERATIONS),
                boundedDouble(tag, "wind_angle", defaults.windAngleDegrees(), 0.0, 360.0),
                boundedInt(tag, "edge_blend", defaults.edgeBlendCells(),
                        0, DuneSimulation.Settings.MAXIMUM_EDGE_BLEND_CELLS),
                boundedDouble(tag, "transport_strength", defaults.transportStrength(),
                        DuneSimulation.Settings.MINIMUM_TRANSPORT_STRENGTH,
                        DuneSimulation.Settings.MAXIMUM_TRANSPORT_STRENGTH)
        );

        String resolutionName = tag.getString("surface_resolution");
        for (DuneSurfaceResolution candidate : DuneSurfaceResolution.values()) {
            if (candidate.commandName().equals(resolutionName)) {
                state.surfaceResolution = candidate;
                break;
            }
        }

        long[] keys = tag.getLongArray("owned_column_keys");
        long[] masks = tag.getLongArray("owned_full_masks");
        int[] partialData = tag.getIntArray("owned_partial_data");
        int count = Math.min(keys.length, Math.min(masks.length, partialData.length));
        for (int index = 0; index < count; index++) {
            OwnedColumn column = OwnedColumn.fromPacked(masks[index], partialData[index]);
            if (!column.isEmpty()) {
                state.ownedColumns.put(keys[index], column);
            }
        }
        return state;
    }

    private static int boundedInt(CompoundTag tag, String key, int fallback, int minimum, int maximum) {
        return tag.contains(key) ? Mth.clamp(tag.getInt(key), minimum, maximum) : fallback;
    }

    private static double boundedDouble(
            CompoundTag tag,
            String key,
            double fallback,
            double minimum,
            double maximum
    ) {
        if (!tag.contains(key)) {
            return fallback;
        }
        double value = tag.getDouble(key);
        return Double.isFinite(value) ? Mth.clamp(value, minimum, maximum) : fallback;
    }

    private static long columnKey(int worldX, int worldZ) {
        return ChunkPos.asLong(worldX, worldZ);
    }

    record OwnedColumn(long fullBlockMask, int partialY, int partialLayers) {
        static final OwnedColumn EMPTY = new OwnedColumn(0L, -1, 0);

        boolean ownsFullBlock(int yOffset) {
            return yOffset >= 0 && yOffset < 32 && (fullBlockMask & (1L << yOffset)) != 0L;
        }

        OwnedColumn withFullBlock(int yOffset, boolean owned) {
            if (yOffset < 0 || yOffset >= 32) {
                return this;
            }
            long bit = 1L << yOffset;
            return new OwnedColumn(
                    owned ? fullBlockMask | bit : fullBlockMask & ~bit,
                    partialY,
                    partialLayers
            );
        }

        OwnedColumn withPartial(int yOffset, int layers) {
            return layers <= 0
                    ? new OwnedColumn(fullBlockMask, -1, 0)
                    : new OwnedColumn(fullBlockMask, yOffset, layers);
        }

        boolean isEmpty() {
            return fullBlockMask == 0L && partialLayers <= 0;
        }

        int packedPartial() {
            return partialLayers <= 0 ? 0 : ((partialY + 1) << 4) | partialLayers;
        }

        static OwnedColumn fromPacked(long fullBlockMask, int packedPartial) {
            int layers = packedPartial & 0xF;
            int yOffset = (packedPartial >>> 4) - 1;
            if (layers < 1 || layers > 15 || yOffset < 0 || yOffset >= 32) {
                return new OwnedColumn(fullBlockMask, -1, 0);
            }
            return new OwnedColumn(fullBlockMask, yOffset, layers);
        }
    }
}
