package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainSettings;

/**
 * Legacy profiles only (through 5148); bypassed by profile 6000.
 * Two bounded, removal-only passes over the post-orphan Shield-Wall front shell.
 *
 * <p>The structural scarp supplies only orientation and a narrow ownership band. Actual
 * post-filter rock occupancy decides which columns are exposed to the desert. Pass two reads
 * the result of pass one, while neither pass reads itself, deposits, chunks, or world blocks.</p>
 */
public final class ShieldWallFrontShellCleanup {
    private ShieldWallFrontShellCleanup() {
    }

    public static Sample disabled(int topY) {
        return new Sample(
                false,
                Wall.NONE,
                false,
                false,
                false,
                Double.POSITIVE_INFINITY,
                0.0,
                0.0,
                false,
                false,
                false,
                false,
                topY,
                topY,
                "disabled"
        );
    }

    public static Sample sample(
            int worldX,
            int worldZ,
            ArrakisTerrainSettings.FrontShellCleanupSettings settings,
            ColumnLookup columns
    ) {
        Column target = columns.sample(worldX, worldZ);
        boolean enabled = settings.enabled();
        int pass1Depth = Math.max(0, Math.min(4, settings.pass1Depth()));
        int pass2Depth = Math.max(0, Math.min(4 - pass1Depth, settings.pass2Depth()));
        boolean pass1Eligible = enabled && eligible(target);
        boolean pass1Removed = pass1Eligible && removesInPass1(
                worldX,
                worldZ,
                target,
                pass1Depth,
                columns
        );
        boolean pass2Eligible = pass1Eligible && !pass1Removed;
        boolean pass2Removed = pass2Eligible && removesInPass2(
                worldX,
                worldZ,
                target,
                pass2Depth,
                pass1Depth,
                columns
        );
        boolean removed = pass1Removed || pass2Removed;
        return new Sample(
                enabled,
                target.wall(),
                target.massifOwned(),
                target.faultOwned(),
                target.otherOwned(),
                target.signedStructuralDistance(),
                -target.inwardNormalX(),
                -target.inwardNormalZ(),
                pass1Eligible,
                pass1Removed,
                pass2Eligible,
                pass2Removed,
                target.topY(),
                removed ? MacroGeologyField.BASE_SURFACE_Y : target.topY(),
                reason(enabled, target, pass1Removed, pass2Removed)
        );
    }

    private static boolean eligible(Column column) {
        return column.rockPresent()
                && column.inFrontShellBand()
                && column.wall() != Wall.NONE
                && column.massifOwned()
                && !column.faultOwned()
                && !column.otherOwned();
    }

    private static boolean removesInPass1(
            int worldX,
            int worldZ,
            Column target,
            int depth,
            ColumnLookup columns
    ) {
        Direction inward = direction(target);
        if (!inward.valid()) {
            return false;
        }
        int boundedDepth = Math.max(0, Math.min(4, depth));
        for (int step = 1; step <= boundedDepth; step++) {
            int outwardX = worldX - inward.x() * step;
            int outwardZ = worldZ - inward.z() * step;
            if (!columns.sample(outwardX, outwardZ).rockPresent()) {
                return true;
            }
        }
        return false;
    }

    private static boolean removesInPass2(
            int worldX,
            int worldZ,
            Column target,
            int depth,
            int pass1Depth,
            ColumnLookup columns
    ) {
        Direction inward = direction(target);
        if (!inward.valid()) {
            return false;
        }
        int boundedDepth = Math.max(0, Math.min(4, depth));
        for (int step = 1; step <= boundedDepth; step++) {
            int outwardX = worldX - inward.x() * step;
            int outwardZ = worldZ - inward.z() * step;
            if (!presentAfterPass1(outwardX, outwardZ, pass1Depth, columns)) {
                return true;
            }
        }
        return false;
    }

    private static boolean presentAfterPass1(
            int worldX,
            int worldZ,
            int pass1Depth,
            ColumnLookup columns
    ) {
        Column column = columns.sample(worldX, worldZ);
        return column.rockPresent()
                && !(eligible(column) && removesInPass1(
                        worldX,
                        worldZ,
                        column,
                        pass1Depth,
                        columns
                ));
    }

    private static Direction direction(Column column) {
        double absX = Math.abs(column.inwardNormalX());
        double absZ = Math.abs(column.inwardNormalZ());
        if (absX < 0.01 && absZ < 0.01) {
            return Direction.NONE;
        }
        if (absX >= absZ) {
            return new Direction(column.inwardNormalX() >= 0.0 ? 1 : -1, 0, true);
        }
        return new Direction(0, column.inwardNormalZ() >= 0.0 ? 1 : -1, true);
    }

    private static String reason(
            boolean enabled,
            Column column,
            boolean pass1Removed,
            boolean pass2Removed
    ) {
        if (!enabled) return "disabled";
        if (!column.rockPresent()) return "no-post-orphan-rock";
        if (column.wall() == Wall.NONE) return "no-shield-wall-orientation";
        if (!column.inFrontShellBand()) return "outside-front-shell-band";
        if (!column.massifOwned()) return "not-massif-owned";
        if (column.faultOwned()) return "fault-owned";
        if (column.otherOwned()) return "other-formation-owned";
        if (pass1Removed) return "removed-pass-1";
        if (pass2Removed) return "removed-pass-2";
        return "behind-two-pass-depth";
    }

    @FunctionalInterface
    public interface ColumnLookup {
        Column sample(int worldX, int worldZ);
    }

    public enum Wall {
        NONE,
        INNER,
        OUTER
    }

    public record Column(
            boolean rockPresent,
            int topY,
            boolean inFrontShellBand,
            Wall wall,
            boolean massifOwned,
            boolean faultOwned,
            boolean otherOwned,
            double signedStructuralDistance,
            double inwardNormalX,
            double inwardNormalZ
    ) {
    }

    public record Sample(
            boolean enabled,
            Wall wall,
            boolean massifOwned,
            boolean faultOwned,
            boolean otherOwned,
            double signedStructuralDistance,
            double outwardNormalX,
            double outwardNormalZ,
            boolean pass1Eligible,
            boolean pass1Removed,
            boolean pass2Eligible,
            boolean pass2Removed,
            int preCleanTopY,
            int postCleanTopY,
            String reason
    ) {
        public boolean removed() {
            return pass1Removed || pass2Removed;
        }
    }

    private record Direction(int x, int z, boolean valid) {
        private static final Direction NONE = new Direction(0, 0, false);
    }
}
