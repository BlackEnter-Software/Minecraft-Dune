package com.blackenter.minecraftdune.worldgen.geology;

/** Continuous, seed-stable patch variation; distances still come from the actual rock foot. */
public final class TalusShapeVariation {
    private TalusShapeVariation() {}

    public static Sample sample(long seed, double x, double z) {
        double broad = unit(seed ^ 0x296AE517DA892B3FL, x / 48, z / 48);
        double detail = unit(seed ^ 0x43B61CFAD57E0829L, x / 12, z / 12);
        double width = unit(seed ^ 0x57CAD612038EF49BL, x / 36, z / 36);
        return new Sample(0.55 + 0.45 * (0.65 * broad + 0.35 * detail),
                0.85 + 0.30 * width, 16 + 4 * width, (broad - 0.5) * 0.16);
    }

    private static double unit(long seed, double x, double z) {
        return GeologyNoise.clamp(0.5 + 0.5 * GeologyNoise.value2(seed, x, z), 0, 1);
    }

    public record Sample(double heightScale, double spreadScale, double skirtReach, double sandStartOffset) {}
}
