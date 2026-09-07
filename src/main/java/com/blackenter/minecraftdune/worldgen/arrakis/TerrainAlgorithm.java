package com.blackenter.minecraftdune.worldgen.arrakis;

/** Persisted algorithm identities, independent of mod releases and numeric tuning. */
public final class TerrainAlgorithm {
    public static final int DEV1 = 1;
    public static final int CURRENT = 2;

    private TerrainAlgorithm() {}

    /** Only unambiguous pre-revision dev.1 settings may acquire an identity automatically. */
    public static ArrakisTerrainSettings resolve(ArrakisTerrainSettings settings) {
        if (settings.isBuriedRock() && settings.terrainAlgorithmRevision() == 0) {
            if (settings.buriedRock().erosion().morphology().enabled()
                    || settings.buriedRock().talus().coherentSources()) {
                throw new IllegalArgumentException("Unversioned Arrakis dev.2 terrain is ambiguous: initial and corrected "
                        + "dev.2 used the same profile. Open it with its original mod build or create a fresh world. "
                        + "Do not relabel the save's terrain_algorithm_revision.");
            }
            settings = settings.withAlgorithmRevision(DEV1);
        }
        requireSupported(settings);
        return settings;
    }

    public static void requireSupported(ArrakisTerrainSettings settings) {
        int revision = settings.terrainAlgorithmRevision();
        if (!settings.isBuriedRock()) {
            if (revision != 0) throw new IllegalArgumentException("Legacy terrain requires algorithm revision 0");
            return;
        }
        if (revision != DEV1 && revision != CURRENT) {
            throw new IllegalArgumentException("Unsupported Arrakis terrain_algorithm_revision=" + revision
                    + "; this build supports 1 (dev.1) and 2 (corrected dev.2). Use a compatible mod build.");
        }
        if (revision == DEV1 && (settings.buriedRock().erosion().morphology().enabled()
                || settings.buriedRock().talus().coherentSources())) {
            throw new IllegalArgumentException("Terrain algorithm 1 cannot enable dev.2 morphology or coherent talus");
        }
        // Revisions 1 and 2 share the frozen evaluator. Revision 1's two disabled switches
        // select the preserved dev.1 path. Output changes require a NEW revision and either
        // an explicitly retained old implementation or rejection of that old revision.
    }
}
