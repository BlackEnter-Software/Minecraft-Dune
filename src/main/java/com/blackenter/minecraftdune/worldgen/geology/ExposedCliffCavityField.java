package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.BuriedRockSettings.CliffCavities;

/** Sparse, exterior-connected cuts into the final eroded envelope. No voxel-air feedback. */
public final class ExposedCliffCavityField {
    public static final int CELL_SIZE = 64;
    private static final long SALT = 0x796E51A6D0CB843FL;
    private ExposedCliffCavityField() {}

    public interface SurfaceLookup {
        Surface sample(int x, int z);
        LithologyField.ResistanceClass resistance(int x, int y, int z);
    }
    public record Surface(double roof, double sediment, double massifWeight, double fracture, int depositClearance) {
        public Surface(double roof, double sediment, double massifWeight, double fracture) {
            this(roof, sediment, massifWeight, fracture, 0);
        }
        public double external() { return Math.max(roof, sediment); }
        public boolean certainlyOpen(int y) {
            return y > Math.max(Math.floor(roof), Math.ceil(sediment)) + depositClearance;
        }
    }
    public record Column(int baseY, long mask) {
        public static final Column NONE = new Column(0, 0);
        public boolean removes(int y) { int bit = y - baseY; return bit >= 0 && bit < 16 && (mask & (1L << bit)) != 0; }
        public int volume() { return Long.bitCount(mask); }
    }
    public record Feature(int cellX, int cellZ, int baseY, int height, int mouthX, int mouthZ,
            int inwardX, int inwardZ, int halfWidth, int length, long[] masks) {
        public Column column(int x, int z) {
            int lx = x - cellX * CELL_SIZE, lz = z - cellZ * CELL_SIZE;
            if (masks.length == 0 || lx < 0 || lz < 0 || lx >= CELL_SIZE || lz >= CELL_SIZE) return Column.NONE;
            return new Column(baseY, masks[lz * CELL_SIZE + lx]);
        }
        public int volume() { int sum = 0; for (long mask : masks) sum += Long.bitCount(mask); return sum; }
    }

    public static double materialCost(LithologyField.ResistanceClass resistance) {
        return switch (resistance) {
            case SOFT, LOOSE -> .8;
            case MEDIUM -> 1.15;
            case HARD -> 2.5;
            case VERY_HARD -> 4.5;
        };
    }

    public static Feature sample(long seed, int cellX, int cellZ, CliffCavities config, SurfaceLookup surfaces) {
        return sample(seed, cellX, cellZ, config, surfaces, null, null);
    }

    /** Single-column queries build only their tangent lane, with identical prefix rules. */
    public static Column column(long seed, int x, int z, CliffCavities config, SurfaceLookup surfaces) {
        return sample(seed, Math.floorDiv(x, CELL_SIZE), Math.floorDiv(z, CELL_SIZE), config, surfaces, x, z).column(x, z);
    }

    private static Feature sample(long seed, int cellX, int cellZ, CliffCavities config, SurfaceLookup surfaces,
            Integer targetX, Integer targetZ) {
        var none = new Feature(cellX, cellZ, 0, 0, 0, 0, 0, 0, 0, 0, new long[0]);
        if (!config.enabled() || config.strength() == 0 || config.frequency() == 0) return none;
        long featureSeed = GeologyNoise.cellSeed(seed, cellX, cellZ, SALT);
        int cx = cellX * CELL_SIZE + 32 + (int) (GeologyNoise.signed(featureSeed, 1) * 6);
        int cz = cellZ * CELL_SIZE + 32 + (int) (GeologyNoise.signed(featureSeed, 2) * 6);
        var center = surfaces.sample(cx, cz);
        if (center.massifWeight() < .3 || center.roof() < center.sediment() + 16) return none;
        if (GeologyNoise.unit(featureSeed, 3) > Math.min(1, config.frequency() * (1 + .3 * center.fracture()))) return none;
        double dx = surfaces.sample(cx + 16, cz).external() - surfaces.sample(cx - 16, cz).external();
        double dz = surfaces.sample(cx, cz + 16).external() - surfaces.sample(cx, cz - 16).external();
        int nx = Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? 1 : -1) : 0;
        int nz = nx == 0 ? (dz >= 0 ? 1 : -1) : 0;
        int mouthX = cx - nx * 20, mouthZ = cz - nz * 20;
        var outside = surfaces.sample(mouthX, mouthZ);
        var inside = surfaces.sample(cx + nx * 20, cz + nz * 20);
        double relief = inside.roof() - outside.external();
        if (relief < 18) return none;
        boolean large = GeologyNoise.unit(featureSeed, 4) < .15;
        int height = Math.min(config.maximumHeight(), large ? config.maximumHeight() : 4 + (int) (GeologyNoise.unit(featureSeed, 5) * 4));
        int baseY = (int) Math.floor(outside.external() + 3 + relief * (.2 + .3 * GeologyNoise.unit(featureSeed, 6)));
        if (baseY + height + config.minimumRoofThickness() >= inside.roof()) return none;
        int halfWidth = large ? 11 : 5 + (int) (GeologyNoise.unit(featureSeed, 7) * 3);
        int length = 40;
        int firstSide = -halfWidth, lastSide = halfWidth;
        if (targetX != null) {
            int depth = (targetX - mouthX) * nx + (targetZ - mouthZ) * nz;
            firstSide = lastSide = -(targetX - mouthX) * nz + (targetZ - mouthZ) * nx;
            if (depth < 1 || depth >= length - 3 || Math.abs(firstSide) > halfWidth) return none;
        }
        long[] masks = new long[CELL_SIZE * CELL_SIZE];
        // Each tangent/height lane is a PREFIX beginning in original exterior air.
        // A cumulative resistance budget can stop a cut, but never restart it behind rock.
        for (int side = firstSide; side <= lastSide; side++) {
            int x0 = mouthX - nz * side, z0 = mouthZ + nx * side;
            Surface[] line = new Surface[length + 1];
            double[] supportedRoof = new double[length + 1];
            for (int d = length; d >= 0; d--) {
                line[d] = surfaces.sample(x0 + nx * d, z0 + nz * d);
                supportedRoof[d] = d == length ? Math.floor(line[d].roof())
                        : Math.min(Math.floor(line[d].roof()), supportedRoof[d + 1]);
            }
            for (int bit = 0; bit < height; bit++) {
                int y = baseY + bit;
                double ellipse = Math.pow((double) side / halfWidth, 2)
                        + Math.pow((bit + .5 - height * .5) / (height * .5), 2);
                if (ellipse >= 1 || !line[0].certainlyOpen(y)) continue;
                double coherent = .8 + .2 * GeologyNoise.value3(featureSeed,
                        x0 / 22.0, y / 12.0, z0 / 22.0);
                double budget = config.maximumPenetration() * config.strength() * Math.sqrt(1 - ellipse)
                        * coherent * (large ? 1 : .75) * (1 + .2 * center.fracture());
                int penetration = 0;
                boolean entered = false;
                for (int d = 1; d < length - 3; d++) {
                    int x = x0 + nx * d, z = z0 + nz * d;
                    if (y <= line[d].sediment() + 2) break;
                    if (y > Math.floor(line[d].roof())) {
                        if (entered || !line[d].certainlyOpen(y)) break;
                        continue;
                    }
                    entered = true;
                    // Retain a continuous roof at this Y extending to an uncarved back wall.
                    // Disjoint cell interiors also retain at least ten-block side supports.
                    if (supportedRoof[d] < baseY + height + config.minimumRoofThickness()
                            || ++penetration > config.maximumPenetration()) break;
                    budget -= materialCost(surfaces.resistance(x, y, z));
                    if (budget < 0) break;
                    int lx = x - cellX * CELL_SIZE, lz = z - cellZ * CELL_SIZE;
                    if (lx < 5 || lz < 5 || lx >= CELL_SIZE - 5 || lz >= CELL_SIZE - 5) break;
                    masks[lz * CELL_SIZE + lx] |= 1L << bit;
                }
            }
        }
        return new Feature(cellX, cellZ, baseY, height, mouthX, mouthZ, nx, nz, halfWidth, length, masks);
    }
}
