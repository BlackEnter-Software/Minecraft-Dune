package com.blackenter.minecraftdune.worldgen.geology;

/**
 * Bounded horizontal component classification over post-orphan rock, before deposits.
 * The universal Y64 substrate and residual Y65 floor do not connect otherwise detached
 * posts. Above that floor, ANY same-height connection (including diagonal columns) counts.
 * Multiple vertical pieces in one column are conservatively treated as one node: this can
 * retain an ambiguous artifact but cannot sever a high rib/ledge connecting to the massif.
 */
public final class BoundedBasalComponentCleanup {
    public static final int SEARCH_RADIUS = 3;
    public static final int MAX_COMPONENT_COLUMNS = 4;
    public static final int CONNECTION_MIN_Y = 66;

    private BoundedBasalComponentCleanup() {}

    public interface RockLookup {
        int topY(int x, int z);
        boolean occupied(int x, int y, int z);
        /** False for protected fault cores, sand corridors, or rock outside the basal context. */
        boolean cleanupAllowed(int x, int z);
    }

    public static Sample sample(int x, int z, RockLookup rock) {
        if (!rock.cleanupAllowed(x, z)) return Sample.none("protected-or-outside-basal-context");
        int top = rock.topY(x, z);
        if (top < CONNECTION_MIN_Y) return Sample.none("substrate-only");
        // At most four removable nodes plus one witness of a larger connected body.
        int[] xs = new int[MAX_COMPONENT_COLUMNS + 1], zs = new int[xs.length];
        xs[0] = x; zs[0] = z;
        int count = 1, minX = x, maxX = x, minZ = z, maxZ = z;
        for (int head = 0; head < count; head++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dz == 0) continue;
                    int nx = xs[head] + dx, nz = zs[head] + dz;
                    boolean visited = false;
                    for (int i = 0; i < count; i++) visited |= xs[i] == nx && zs[i] == nz;
                    if (visited || !connected(xs[head], zs[head], nx, nz, rock)) continue;
                    minX = Math.min(minX, nx); maxX = Math.max(maxX, nx);
                    minZ = Math.min(minZ, nz); maxZ = Math.max(maxZ, nz);
                    // Span guard makes the conservative edge decision identical for EVERY
                    // member, not just members whose query-centered window hits the edge.
                    if (Math.abs(nx - x) >= SEARCH_RADIUS || Math.abs(nz - z) >= SEARCH_RADIUS
                            || maxX - minX >= SEARCH_RADIUS || maxZ - minZ >= SEARCH_RADIUS) {
                        return new Sample(true, false, count + 1, false, true, top, "search-boundary");
                    }
                    if (!rock.cleanupAllowed(nx, nz)) {
                        return new Sample(true, false, count + 1, true, false, top, "protected-connection");
                    }
                    if (count == MAX_COMPONENT_COLUMNS) {
                        return new Sample(true, false, count + 1, true, false, top, "larger-connected-body");
                    }
                    xs[count] = nx; zs[count++] = nz;
                }
            }
        }
        return new Sample(true, true, count, false, false, top, "closed-small-component");
    }

    private static boolean connected(int ax, int az, int bx, int bz, RockLookup rock) {
        int top = Math.min(rock.topY(ax, az), rock.topY(bx, bz));
        for (int y = top; y >= CONNECTION_MIN_Y; y--) {
            if (rock.occupied(ax, y, az) && rock.occupied(bx, y, bz)) return true;
        }
        return false;
    }

    public record Sample(boolean candidate, boolean removed, int componentColumns,
            boolean reachesSupport, boolean reachesSearchBoundary, int topY, String reason) {
        public static Sample none(String reason) { return new Sample(false, false, 0, false, false, 64, reason); }
        public boolean removesY(int y) { return removed && y >= 65 && y <= topY; }
    }
}
