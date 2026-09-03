package com.blackenter.minecraftdune.worldgen.geology;

/** Static material blending around the existing actual contact, never a terrain/contact solver. */
public final class BasalSandSkirt {
    public static final int INWARD_OVERLAP = 4;
    public static final int OUTWARD_REACH = 24;
    public static final int MAX_DEPTH = 4;

    private BasalSandSkirt() {}

    public static Sample sample(boolean enabled, BasalTalusApronField.Evaluation basal,
            boolean residualY65, boolean gravelAtY65) {
        var contact = basal.actual();
        // 'found' alone also includes rejected source contacts; use the existing qualification.
        boolean qualified = contact.enabled() && contact.found() && contact.reason().equals("found");
        return shape(enabled && qualified, contact.signedDistance(), residualY65 && !gravelAtY65);
    }

    public static Sample shape(boolean qualified, double signed, boolean safeResidualY65) {
        if (!qualified || !Double.isFinite(signed) || signed > INWARD_OVERLAP || signed <= -OUTWARD_REACH) {
            return new Sample(false, signed, 0, false);
        }
        double t = Math.max(0, -signed) / OUTWARD_REACH;
        int depth = (int) Math.ceil(MAX_DEPTH * (1 - t * t * (3 - 2 * t)));
        return new Sample(depth > 0, signed, depth, safeResidualY65 && depth > 0);
    }

    public record Sample(boolean active, double signedDistance, int depth, boolean visibleY65Mantle) {
        public int bottomY() { return 65 - depth; }
        public int topY() { return visibleY65Mantle ? 65 : 64; }
        public BasalTalusApronField.Material materialAt(int y) {
            return active && (y >= bottomY() && y <= 64 || y == 65 && visibleY65Mantle)
                    ? BasalTalusApronField.Material.SAND : BasalTalusApronField.Material.NONE;
        }
    }
}
