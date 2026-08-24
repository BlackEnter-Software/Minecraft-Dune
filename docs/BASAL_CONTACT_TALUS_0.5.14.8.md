# Basal Contact & Talus Apron - 0.5.14.8

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

A new `BasalTalusApronField` uses the same warped inner/outer scarp boundaries as the massif.

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

The wedge:
- reaches at most about 6 blocks high;
- extends about 12 blocks onto the sand side;
- can occupy/replace the lowest 4 blocks just inside the contact;
- is gravel-dominant near the wall;
- grades toward sand at its lower/distal toe;
- is suppressed in strong sand corridors and full regional-fault cores.

Existing localized scree remains separate and stacks above the basal apron where both occur.

## Not aeolian deposition

This is colluvial/gravity debris. It has no prevailing-wind logic.

Wind-driven sand piling against obstacles remains reserved for approximately 0.5.16 after
the 0.5.15 subsurface-geology pass.

## Compatibility

The five new `lithology.talus` fields are optional. Old serialized worlds that omit them
decode `basal_apron_enabled = false`, preserving their previous terrain behavior.
