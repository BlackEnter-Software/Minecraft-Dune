# Native transverse dunes — 0.5.9

## Why a second dune implementation exists

The existing `DuneSimulation` is a useful development laboratory. It operates on a fixed
64 × 64 reduced grid, performs iterative saltation-like transport and cascading, then
upscales that finite field into Minecraft blocks.

That architecture is intentionally retained for experiments, but it is not ideal for normal
world generation because independently requested chunks must not depend on having simulated
a neighboring 64 × 64 laboratory region first.

0.5.9 therefore adds `NativeTransverseDuneField`: an **analytic coordinate field** that
extracts the calibrated morphology from the laboratory and evaluates it directly from
absolute X/Z coordinates.

## Calibrated parameters carried into native generation

The first native far-erg profile uses the established transverse baseline:

```text
maximum height       = 30 blocks
dune spacing         = 350 blocks
spacing variation    = 0.18
ridge sharpness      = 3.0
valley cutoff        = 0.20
slope asymmetry      = 0.82
wind angle           = 24 degrees
surface resolution   = 1/16 block
```

The fixed 24-degree wind is temporary. A future regional wind field should replace it after
rock topography, shelter, passes and sand supply are stable.

## Analytic morphology

For every absolute X/Z sample the field:

1. projects the coordinate onto along-wind and crosswind axes;
2. computes a nominal 350-block transverse phase;
3. warps that phase at several larger crosswind/along-wind wavelengths using the calibrated
   0.18 spacing variation;
4. evaluates the same asymmetric ridge concept used by the laboratory;
5. normalizes the 0.82-asymmetry profile so a full-suitability crest can still reach the
   configured 30-block envelope;
6. applies ridge sharpness and the 0.20 valley cutoff;
7. applies a very mild crosswind crest-height modulation;
8. multiplies by `MacroGeologyField.duneSuitability()`;
9. quantizes to sixteenth-block surface units.

No iterative transport, toroidal wrapping, cascade passes or finite-region edge blending is
run during native chunk generation.

## Sand material

The base Arrakis Dev flat layer remains vanilla sand through Y=64. Native dune material above
that base uses the mod's existing blocks:

- full `minecraftdune:sand` blocks;
- at most one `minecraftdune:sand_layer` top block with 1–15 sixteenth layers.

If a low rock remnant occupies part of the same column, the rock is generated first. Sand is
only placed above that rock when the analytic dune surface is physically higher, allowing
low remnants to become partially buried while tall formations remain exposed.

## Geographic activation

Native transverse dunes are not enabled merely because a coordinate is far from `(0,0)`.
They consume the geological `duneSuitability` field:

- Central Basin: zero;
- Inner Rock Foreland: zero;
- Main Shield Wall: effectively zero;
- Broken Rock Desert: low between exposed rock;
- Sand–Rock Transition: increasingly strong;
- Open Erg: full strength.

Tall rock suppresses local dune suitability, so isolated remnants can stand above dune
fields without being uniformly coated in sand.

## Relationship to the laboratory simulation

The two systems have different jobs:

```text
DuneSimulation
    finite development laboratory
    iterative transport/cascade experiments
    morphology research and calibration

NativeTransverseDuneField
    infinite coordinate field
    fast native chunk generation
    deterministic and chunk-order independent
```

Future versions can migrate additional validated physical behavior into regional fields,
but 0.5.9 does not discard or rewrite the laboratory implementation.
