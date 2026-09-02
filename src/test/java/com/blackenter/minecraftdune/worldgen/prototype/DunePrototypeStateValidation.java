package com.blackenter.minecraftdune.worldgen.prototype;

import net.minecraft.nbt.CompoundTag;

/** Dependency-free persistence checks for dimension-local prototype state. */
public final class DunePrototypeStateValidation {
    private DunePrototypeStateValidation() {
    }

    public static void main(String[] args) {
        DunePrototypeState state = new DunePrototypeState();
        DuneSimulation.Settings expectedSettings = DuneSimulation.Settings.defaults()
                .withCellSize(4)
                .withMaximumHeightOverride(17)
                .withWindAngleDegrees(-45.0);
        state.settings(expectedSettings);
        state.surfaceResolution(DuneSurfaceResolution.EIGHTH);

        DunePrototypeState.OwnedColumn expectedOwnership =
                DunePrototypeState.OwnedColumn.EMPTY
                        .withFullBlock(0, true)
                        .withFullBlock(7, true)
                        .withPartial(8, 6);
        state.ownership(1234, -987, expectedOwnership);

        CompoundTag saved = state.save(new CompoundTag(), null);
        DunePrototypeState restored = DunePrototypeState.load(saved, null);
        require(restored.settings().equals(expectedSettings), "prototype settings did not round-trip");
        require(restored.surfaceResolution() == DuneSurfaceResolution.EIGHTH,
                "surface resolution did not round-trip");
        require(restored.ownership(1234, -987).equals(expectedOwnership),
                "prototype ownership did not round-trip");
        require(restored.ownership(1235, -987).isEmpty(),
                "ownership leaked into an adjacent column");

        DunePrototypeState.OwnedColumn changed = expectedOwnership
                .withFullBlock(7, false)
                .withPartial(-1, 0);
        require(changed.ownsFullBlock(0), "clearing one bit removed another owned block");
        require(!changed.ownsFullBlock(7), "owned block bit was not cleared");
        require(changed.partialLayers() == 0, "partial ownership was not cleared");

        System.out.println("Dune prototype state validation passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
