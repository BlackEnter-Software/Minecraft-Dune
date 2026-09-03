package com.blackenter.minecraftdune.worldgen.geology;

/** Legacy profiles only (through 5148): static blending around actual contact. Never used by 6000. */
public final class BasalSandSkirt {
    public static final int INWARD_OVERLAP = 4;
    public static final int OUTWARD_REACH = 24;
    public static final int MAX_DEPTH = 4;

    private BasalSandSkirt() {}

    public static Sample sample(boolean enabled, BasalTalusApronField.Evaluation basal,
            boolean residualY65, boolean gravelAtY65) {
        return sample(enabled, basal, residualY65, gravelAtY65, OUTWARD_REACH);
    }

    public static Sample sample(boolean enabled, BasalTalusApronField.Evaluation basal,
            boolean residualY65, boolean gravelAtY65, double outwardReach) {
        var contact = basal.actual();
        // 'found' alone also includes rejected source contacts; use the existing qualification.
        boolean qualified = contact.enabled() && contact.found() && contact.reason().equals("found");
        return shape(enabled && qualified, contact.signedDistance(), residualY65 && !gravelAtY65, outwardReach);
    }

    public static Sample shape(boolean qualified, double signed, boolean safeResidualY65) {
        return shape(qualified, signed, safeResidualY65, OUTWARD_REACH);
    }

    public static Sample shape(boolean qualified, double signed, boolean safeResidualY65, double outwardReach) {
        double reach = Math.max(1, Math.min(OUTWARD_REACH, outwardReach));
        if (!qualified || !Double.isFinite(signed) || signed > INWARD_OVERLAP || signed <= -reach) {
            return new Sample(false, signed, 0, false, reach);
        }
        double t = Math.max(0, -signed) / reach;
        int depth = (int) Math.ceil(MAX_DEPTH * (1 - t * t * (3 - 2 * t)));
        return new Sample(depth > 0, signed, depth, safeResidualY65 && depth > 0, reach);
    }

    public record Sample(boolean active, double signedDistance, int depth, boolean visibleY65Mantle, double outwardReach) {
        public Sample(boolean active, double signedDistance, int depth, boolean visibleY65Mantle) {
            this(active, signedDistance, depth, visibleY65Mantle, OUTWARD_REACH);
        }
        public int bottomY() { return 65 - depth; }
        public int topY() { return visibleY65Mantle ? 65 : 64; }
        public BasalTalusApronField.Material materialAt(int y) {
            return active && (y >= bottomY() && y <= 64 || y == 65 && visibleY65Mantle)
                    ? BasalTalusApronField.Material.SAND : BasalTalusApronField.Material.NONE;
        }
    }
}
