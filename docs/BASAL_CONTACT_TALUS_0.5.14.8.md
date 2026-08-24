# Basal Contact & Talus Apron - 0.5.14.8–0.5.14.9

0.5.14.8 fixes the remaining raised Shield-Wall foot at its source, then adds a short
gravity-driven debris wedge at the corrected rock/sand contact.

## Structural contact fix

The previous massif formula retained a constant basal contribution:

```text
(30 + 105 * physicalEnvelope + 30 * relief) * shape
```

At a partially active scarp this could leave several blocks of rock above the Y=64 sand
datum even with the 0.5.14.7 `massif_vertical_offset = -4`.

0.5.14.8 changes the structural term to:

```text
105 * physicalEnvelope
+ (30 + 30 * relief) * basalGate
```

`basalGate` fades from zero to one across the low physical-envelope range. Deep inside the
massif the gate is one, so full Shield-Wall height is retained. Near the sand contact the
constant/relief contribution disappears and the existing -4 alignment can clamp the toe to
the sand datum.

## Basal talus apron

0.5.14.8 introduced `BasalTalusApronField` using the same warped inner/outer scarp boundaries
as the massif. Seed-0 testing showed that this structural boundary was not the final rock
foot after the rest of terrain shaping, so it could leave a visible sand strip before the
debris began.

0.5.14.9 retains the structural boundary only as a bounded search-eligibility optimization.
Placement now comes exclusively from the actual surviving rock contact after macro geology,
fractures, major erosion, surface erosion and orphan-remnant filtering, but before basal
talus or dunes. Each low-side column searches fixed absolute-coordinate directions for the
nearest surviving foundation-contact rock column. A directly adjacent rock column has zero
outward distance, so the first talus column touches it without a manufactured gap. The apron
follows local footprint irregularity instead of reproducing a smooth nominal ring.

The same sampler recognizes regional-fault floor columns and can source debris from either
actual fault wall. Where both walls are near, the wall-adjacent cells and at least one central
cell are reserved before 43% of the remaining interior span is assigned to either apron.
This leaves a sandy channel rather than bridging or refilling the fault. Deposition begins
above Y=64 and never replaces a surviving rock column.

The lookup is a pure, cached absolute-coordinate query of final rock occupancy before talus.
Its orphan test reads raw pre-orphan neighbors, and the contact query never asks for another
apron result. No generated chunk or neighboring block state is inspected, so generation
remains deterministic, recursion-free and independent of chunk order.

Active source settings:

```json
"talus": {
  "local_scree_enabled": true,
  "minimum_fracture_strength": 0.44,
  "maximum_thickness": 7,
  "spread": 18.0,
  "basal_apron_enabled": true,
  "basal_apron_max_height": 6,
  "basal_apron_spread": 12.0,
  "basal_apron_inset": 4.0,
  "basal_apron_sand_start": 0.62
}
```

The 0.5.14.9 wedge:
- reaches at most about 6 blocks high;
- extends about 12 blocks onto the sand side;
- begins immediately outside surviving rock and never overwrites it;
- is gravel-dominant near the wall;
- grades toward sand at its lower/distal toe;
- is suppressed in strong non-fault sand corridors;
- forms independently from both regional-fault walls while retaining a sandy center.

`basal_apron_inset` remains serialized and still controls the legacy 0.5.14.8 inward-overlap
path. For profile 5149 it contributes only to the safe structural search band; actual rock
contact is authoritative and cannot be replaced to force an inset.

Existing localized scree remains separate and stacks above the basal apron where both occur.

## Not aeolian deposition

This is colluvial/gravity debris. It has no prevailing-wind logic.

Wind-driven sand piling against obstacles remains reserved for approximately 0.5.16 after
the 0.5.15 subsurface-geology pass.

## Compatibility

The five new `lithology.talus` fields are optional. Old serialized worlds that omit them
decode `basal_apron_enabled = false`, preserving their previous terrain behavior. Serialized
profiles below 5149 that contain the fields retain 0.5.14.8 nominal-scarp targeting; profile
5149 and later use actual surviving contact.
