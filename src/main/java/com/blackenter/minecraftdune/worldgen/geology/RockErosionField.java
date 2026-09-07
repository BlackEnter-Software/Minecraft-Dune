package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.worldgen.arrakis.BuriedRockSettings;
import java.util.function.DoubleUnaryOperator;

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
        return sample(seed, x, z, rawTop, sediment, face, fracture, lithology, faultDamage,
                windAngle, settings, rawHeight, WallErosionMorphology.Sample.NONE);
    }

    public static Sample sample(long seed, double x, double z, double rawTop, double sediment,
            RockFaceExposure.Sample face, MassifFractureField.Sample fracture, LithologyField.Column lithology,
            double faultDamage, double windAngle, BuriedRockSettings settings,
            RockFaceExposure.HeightLookup rawHeight, WallErosionMorphology.Sample morphology) {
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
        if (settings.erosion().morphology().enabled() && morphology.exposureGate() > 0) {
            double faultBoost = .35 * faultDamage * erosion.faultWeakness();
            double fractureBoost = .55 * fractureWeakness * erosion.fractureStrength();
            var config = erosion.morphology();
            double sector = Math.min(config.sectorRecession(), morphology.sectorRecession()
                    * (.85 + .3 * wind + faultBoost));
            double meso = Math.min(config.mesoscaleRecession(), morphology.mesoscaleRecession()
                    * (1 + fractureBoost + faultBoost));
            double totalDistance = distance + sector + meso;
            double endpointLoss = Math.max(0, afterSurface - rawHeight.top(
                    x + face.outwardNormalX() * totalDistance, z + face.outwardNormalZ() * totalDistance));
            // The short offset alone barely changes a shoulder until it crosses the toe.
            // Secants across the downhill profile distribute coherent recession over that
            // shoulder as well. Sampling three distances also sees shorter downhill slopes
            // followed by rising ground; a single far endpoint would miss those entirely.
            // A flat envelope supplies no slope or removal.
            double profileReach = face.farProbeDistance();
            double profileSlope = 0;
            for (int probe = 1; probe <= 3; probe++) {
                double reach = profileReach * probe / 3;
                double drop = Math.max(0, rawTop - rawHeight.top(
                        x + face.outwardNormalX() * reach, z + face.outwardNormalZ() * reach));
                profileSlope += drop / reach * (probe == 1 ? .2 : probe == 2 ? .3 : .5);
            }
            double recessionWork = Math.max(endpointLoss, (sector + meso) * profileSlope);
            double gullyWork = morphology.gullyDepth() * (1 + fractureBoost + faultBoost);
            double work = recessionWork + gullyWork;
            double roof = erodeThroughStrata(afterSurface, settings.rockSurface().minimumY(), work,
                    y -> susceptibility(lithology.sample(y).resistance(), erosion));
            double removal = afterSurface - roof;
            double resistance = work > 0 ? removal / work : 1;
            // Attribute the one integrated removal to its two causes; this is not another pass.
            double gully = work > 0 ? removal * gullyWork / work : 0;
            double recession = removal - gully;
            return new Sample(roof, incision, afterIncision - afterMajor + recession,
                    afterMajor - afterSurface, rawTop - roof, totalDistance, face,
                    new MorphologyBreakdown(sector, meso, gully, resistance, fractureBoost, faultBoost, recession));
        }
        return new Sample(afterSurface, incision, afterIncision - afterMajor, afterMajor - afterSurface,
                rawTop - afterSurface, distance, face);
    }

    /** Integrate erosion work through the material actually reached. At most 88 four-block
     * intervals cover the configured geological range. Interpolated cost and a continuous
     * inverse give a fractional roof, never occupancy holes or mandatory stratum-height steps.
     * The caller supplies displaced lithology; no terrain/exposure is recomputed here. */
    public static double erodeThroughStrata(double top, double minimum, double work,
            DoubleUnaryOperator susceptibilityAt) {
        if (work <= 0 || top <= minimum) return top;
        double roof = top;
        double cost = erosionCost(susceptibilityAt.applyAsDouble(roof));
        int intervals = (int) Math.ceil((top - minimum) / 4);
        for (int i = 0; i < intervals; i++) {
            double depth = Math.min(4, roof - minimum);
            double nextCost = erosionCost(susceptibilityAt.applyAsDouble(roof - depth));
            double needed = depth * (cost + nextCost) * .5;
            if (work <= needed) {
                double gradient = (nextCost - cost) / depth;
                double reached = 2 * work / (cost + Math.sqrt(Math.max(0, cost * cost + 2 * gradient * work)));
                return Math.max(minimum, roof - Math.min(depth, reached));
            }
            work -= needed;
            roof -= depth;
            cost = nextCost;
        }
        return minimum;
    }

    private static double erosionCost(double susceptibility) {
        return 1 / GeologyNoise.clamp(susceptibility, .05, 1.6);
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
            double removedAmount, double horizontalRecession, RockFaceExposure.Sample face, MorphologyBreakdown morphology) {
        public Sample(double rockTop, double incision, double majorRemoval, double surfaceRemoval,
                double removedAmount, double horizontalRecession, RockFaceExposure.Sample face) {
            this(rockTop, incision, majorRemoval, surfaceRemoval, removedAmount, horizontalRecession, face, MorphologyBreakdown.NONE);
        }
    }
    public record MorphologyBreakdown(double sectorRecession, double mesoscaleRecession, double gullyIncision,
            double lithologyResponse, double fractureBoost, double faultBoost, double recessionRemoval) {
        public static final MorphologyBreakdown NONE = new MorphologyBreakdown(0, 0, 0, 1, 0, 0, 0);
    }
}
