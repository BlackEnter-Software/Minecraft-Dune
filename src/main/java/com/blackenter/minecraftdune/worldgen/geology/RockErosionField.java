package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.BuriedRockSettings;

/** One fixed analytical recession pass. Its result is a solid geological roof, not voxel survival. */
public final class RockErosionField {
    // Kept from the successful 0.5.14 erosion fields so the spatial weathering character survives.
    private static final long FACE_DETAIL_SALT = 0x4F92C7A63D18B5E1L;
    private static final long COARSE_SALT = 0x6F53A149D827CBE1L;
    private static final long DETAIL_SALT = 0x3C91E0AF5B6274D8L;
    private static final long VERTICAL_SALT = 0x7249D3B851AEC06FL;
    private RockErosionField() {}

    public static Sample sample(long seed, double x, double z, double rawTop, double sediment,
            RockFaceExposure.Sample face, MassifFractureField.Sample fracture, LithologyField.Column lithology,
            double faultDamage, double windAngle, BuriedRockSettings settings,
            RockFaceExposure.HeightLookup rawHeight) {
        var erosion = settings.erosion();
        if (!erosion.enabled() || rawTop < sediment) return new Sample(rawTop, 0, 0, 0, 0, 0, face);

        double exposed = GeologyNoise.smoothStep(0, 2, rawTop - sediment);
        double incision = Math.min(rawTop - settings.rockSurface().minimumY(),
                fracture.carveDepth() * erosion.incisionScale() * exposed);
        double afterIncision = rawTop - incision;
        double susceptibility = susceptibility(lithology.sample(afterIncision).resistance(), erosion);
        double reliefGate = GeologyNoise.smoothStep(erosion.minimumRelief(), erosion.minimumRelief() + 26, face.localRelief());
        double faceGate = GeologyNoise.smoothStep(erosion.edgeThreshold(),
                Math.min(1, erosion.edgeThreshold() + .34), face.exposure());
        double strength = GeologyNoise.clamp(reliefGate * faceGate * (.55 + .84 * .68), 0, 1);
        double windRadians = Math.toRadians(windAngle);
        double facing = Math.max(0, face.outwardNormalX() * Math.cos(windRadians)
                + face.outwardNormalZ() * Math.sin(windRadians));
        double shelter = .72 + .28 * (.5 + .5 * GeologyNoise.value2(seed ^ FACE_DETAIL_SALT, x / 310, z / 310));
        double wind = facing * (.62 + reliefGate * .38) * shelter;
        double proximity = Double.isFinite(fracture.distance())
                ? 1 - GeologyNoise.smoothStep(fracture.halfWidth(), fracture.halfWidth() + 14, fracture.distance()) : 0;
        double fractureWeakness = GeologyNoise.clamp(Math.max(fracture.strength(), proximity * fracture.activation())
                + fracture.intersectionStrength() * .45, 0, 1);
        double exposureResponse = .62 + wind * erosion.windStrength()
                + fractureWeakness * erosion.fractureStrength() + faultDamage * erosion.faultWeakness();
        double faceDetail = GeologyNoise.value3(seed ^ FACE_DETAIL_SALT, x / 13, rawTop / 19, z / 13) * 1.35;
        double majorDistance = GeologyNoise.clamp((erosion.maximumRecession()
                * (.34 * exposureResponse + .28 * fractureWeakness) * susceptibility - faceDetail) * strength,
                0, erosion.maximumRecession());

        double coarse = .5 + .5 * GeologyNoise.value2(seed ^ COARSE_SALT, x / erosion.coarseScale(), z / erosion.coarseScale());
        double detail = .5 + .5 * GeologyNoise.value2(seed ^ DETAIL_SALT, x / erosion.detailScale(), z / erosion.detailScale());
        double pattern = coarse * .72 + detail * .28;
        double lithologyRelief = GeologyNoise.clamp(1 + (susceptibility - 1) * .65, .2, 2);
        double surfaceStrength = erosion.surfaceStrength() * (1 + faultDamage * erosion.faultWeakness());
        double topWeathering = erosion.surfaceRetreat() * surfaceStrength * (.18 + pattern * .82)
                * (.72 + face.exposure() * .28) * lithologyRelief * exposed;
        double vertical = .5 + .5 * GeologyNoise.value3(seed ^ VERTICAL_SALT,
                x / erosion.coarseScale(), rawTop / Math.max(5, erosion.coarseScale() * .82), z / erosion.coarseScale());
        double response = GeologyNoise.smoothStep(0, .55, surfaceStrength * face.exposure() * (.52 + pattern * .48));
        double surfaceDistance = Math.min(erosion.surfaceRetreat(), erosion.surfaceRetreat()
                * response * lithologyRelief * (.52 + vertical * .48));

        // Advect the geological roof from downhill: horizontal recession becomes continuous
        // vertical lowering. There is no reconstructed hard face, Y64 floor, or repair filter.
        double afterMajor = Math.min(afterIncision, rawHeight.top(x + face.outwardNormalX() * majorDistance,
                z + face.outwardNormalZ() * majorDistance));
        double distance = majorDistance + surfaceDistance;
        double afterSurface = Math.min(afterMajor, rawHeight.top(x + face.outwardNormalX() * distance,
                z + face.outwardNormalZ() * distance)) - topWeathering;
        afterSurface = Math.max(settings.rockSurface().minimumY(), afterSurface);
        afterMajor = Math.max(afterSurface, afterMajor);
        return new Sample(afterSurface, incision, afterIncision - afterMajor, afterMajor - afterSurface,
                rawTop - afterSurface, distance, face);
    }

    public static double susceptibility(LithologyField.ResistanceClass resistance, BuriedRockSettings.Erosion settings) {
        return switch (resistance) {
            case SOFT -> settings.softMultiplier();
            case MEDIUM -> 1;
            case HARD -> settings.hardMultiplier();
            case VERY_HARD -> settings.veryHardMultiplier();
            case LOOSE -> settings.softMultiplier() * 1.15;
        };
    }

    public record Sample(double rockTop, double incision, double majorRemoval, double surfaceRemoval,
            double removedAmount, double horizontalRecession, RockFaceExposure.Sample face) {}
}
