# Orphan Remnant Suppression - 0.5.14.6

0.5.14.6 is a narrow cleanup pass for a specific exposed-cliff artifact:

```text
top
███████
████████
███████
████████
███████
███████    █  <- detached vertical remnant
████████████
base layer
```

The extra column can survive because the erosion fields operate on deterministic per-Y
occupancy and lithological resistance. It is vertically supported by the base, but no longer
connected laterally to the main cliff at that height.

## Support rule

The new `OrphanRemnantFilter` is applied only after both existing erosion fields say that a
rock block survives.

For exposed high-relief erosion candidates above the protected base, a surviving block must:

1. have a contiguous same-Y rock chain inward toward the massif/fault body; or
2. reach such an inward-supported block through a short contiguous lateral path.

The source profile starts with:

```json
"orphan_remnants": {
  "enabled": true,
  "inward_support_depth": 1,
  "lateral_search_radius": 2,
  "minimum_height_above_base": 5,
  "minimum_face_relief": 24.0
}
```

`inward_support_depth = 1` is intentionally conservative: an immediately attached rib,
overhang or resistant projection survives. An air gap between the remnant and the cliff does
not count as support.

`lateral_search_radius = 2` rescues ledges that connect into the body slightly to either side
of the local face normal.

`minimum_height_above_base = 5` keeps the structural base/fault-floor region outside this
cleanup.

`minimum_face_relief = 24` restricts the pass to major exposed faces rather than small
foreland/broken-rock formations.

The support lookup always samples the raw deterministic erosion result, never the already
filtered result. This avoids recursion and makes chunk order irrelevant.

## Additional native materials

The active 0.5.14.6 profile also enables three vanilla lithologies:

- `minecraft:smooth_basalt` — HARD
- `minecraft:red_sandstone` — SOFT
- `minecraft:terracotta` — MEDIUM

They are stored in the new top-level `additional_materials` settings group so the existing
16-field material-palette codec does not exceed DataFixerUpper's RecordCodecBuilder arity
limit.

Smooth basalt forms coherent hard margins on very-hard basalt sheets. Red sandstone appears
as coherent variants of soft sandstone units. Terracotta appears as medium-strength
sedimentary/altered host bodies. None are per-block decorative speckle.

Old serialized profiles omit `additional_materials` and `orphan_remnants`; both therefore
decode disabled and retain their previous terrain/material behavior.
