# Native Arrakis terrain generation — 0.5.14

## Architecture

0.5.8 established `minecraftdune:arrakis_dev` as a registered native chunk generator based
on vanilla `FlatLevelSource`. Version 0.5.14 retains the analytic terrain-column architecture
and adds lithology-aware three-dimensional escarpment occupancy after the 0.5.13 lithology and
local-fracture fields:

```text
FlatLevelGeneratorSettings
        +
ArrakisTerrainSettings
        ↓
ArrakisChunkGenerator
        ↓
MacroGeologyField
        ↓
LithologyField + MassifFractureField
        ↓
EscarpmentErosionField per-Y occupancy
        ↓
foundation-connected surviving rock
        ↓
NativeTransverseDuneField
        ↓
full-block dune base where present
        ↓
localized talus above rock / full dune blocks
        ↓
optional sixteenth-layer dune sand where talus does not occupy that Y
```

Everything remains deterministic from the actual world seed, the serialized generator
profile, and absolute coordinates.

## Arrakis biome and fauna

New worlds use `minecraftdune:arrakis_desert`, a featureless biome whose natural spawn table
contains only Muad'dib and Desert Hare. No monster, ambient, cave-water, water-creature,
water-ambient, or axolotl entries are defined. Both entities use on-ground Rabbit placement
rules, and Dune Sand plus layered Dune Sand are valid spawn surfaces.

Older Arrakis worlds may retain a serialized `minecraft:desert` biome holder. Generator-
scoped placement and finalization checks therefore reject all other natural and autonomous
entity types whenever the active chunk generator is `ArrakisChunkGenerator`, including patrol,
event, structure and spawner paths. Commands, spawn eggs, buckets, dispensers, and breeding
remain available for deliberate player/test activity.

## Generator codec

The custom generator codec now serializes two objects:

```json
{
  "type": "minecraftdune:arrakis_dev",
  "settings": {
    "...": "vanilla flat Arrakis base settings"
  },
  "terrain": {
    "...": "ArrakisTerrainSettings"
  }
}
```

The `terrain` field is optional when decoding and falls back to the current default profile.
This keeps old generator data decodable while allowing new worlds to explicitly store their
terrain parameters.

## Terrain-column order

For each X/Z column:

1. `FlatLevelSource` creates the base bedrock/deepslate/stone/sandstone/sand layers;
2. `MacroGeologyField` evaluates the serialized geological profile;
3. `NativeTransverseDuneField` samples independent dune surface units from the macro dune
   suitability while the column data is being assembled;
4. `LithologyField` evaluates coherent 3D units, intrusions, horizontal sheets and mineral
   bands with multi-scale contact roughness;
5. `MassifFractureField` evaluates continuous primary fissures and finite dead-end branches,
   then lowers the rock top where a crack/slot/chasm is active;
6. `EscarpmentErosionField` uses four coarse face probes, lithology, fractures and the current
   wind direction to select an escarpment and evaluate rock occupancy independently at each Y;
7. surviving native lithology replaces the base sand/sandstone down to hard crust and continues
   upward through the fissure-adjusted envelope; the lowest native-rock layers are retained;
8. final column composition retains all sampled full dune-sand blocks, starts localized low-side
   gravel/source-clast talus above both rock and the top full dune block, and emits the optional
   sixteenth-layer dune top only when talus does not occupy that same Y.

This ordering prevents unsupported gravel above a fractional dune layer: overlapping talus
replaces only the partial layer, never the supporting full dune blocks. The erosion occupancy
also unconditionally retains the first two native-rock blocks above Y64, preserving shallow
one- and two-block outcrops and the hard-crust connection.

`getBaseHeight()` and `getBaseColumn()` use the same profile.

## Performance

0.5.14 remains analytic per column:

- no post-generation `ServerLevel#setBlock` cliff construction;
- no finite iterative dune simulation per chunk;
- no iterative fracture propagation, erosion simulation or post-generation terrain edits;
- absolute-coordinate 3D lithology plus analytic primary-line and finite-branch distance tests;
- four fixed-distance macro probes only for plausible escarpment candidates;
- per-Y occupancy restricted to native rock-bearing columns and bounded by the existing rock
  envelope;
- fracture-driven face retreat fades below each fissure's design depth, while intersections
  only modestly strengthen/deepen overlapping traces;
- exact early return inside the 0–1500 pure-sand basin;
- far-erg early return after the full open-erg boundary at radius 9000.

Eligible cliff columns are more expensive than the 0.5.13 height-only path, but the basin,
fault/sand-pass, province, mask and relief gates return early. The far desert still uses the
fast path. All block writes remain direct to `ChunkAccess`, without lighting updates, and
`getBaseHeight()` / `getBaseColumn()` share the same deterministic terrain-column result.

## Save / test compatibility

Already generated chunks never change.

For clean terrain comparison, create a new Arrakis Dev world after applying 0.5.14. The new
world stores the explicit profile version 514 and enabled `erosion` settings. A serialized
0.5.13 generator that omits `erosion` decodes with the pass disabled, preventing an automatic
morphology seam where that older world next generates chunks.

Lithology/fracture parameters remain documented in
[LITHOLOGY_AND_FRACTURES.md](LITHOLOGY_AND_FRACTURES.md). The 3D face rules, complete erosion
JSON guide, talus behavior, performance approach and limitations are in
[ESCARPMENT_EROSION.md](ESCARPMENT_EROSION.md).

## Pregeneration

Pregeneration commands are unchanged:

```mcfunction
/dune geology generate
/dune geology generate_initial
/dune geology generate_nearest <1..12>
/dune geology generation status
/dune geology generation cancel
```

Distant Horizons can continue generating the native terrain independently.
