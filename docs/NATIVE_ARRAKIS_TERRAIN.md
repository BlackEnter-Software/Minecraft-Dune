# Native Arrakis terrain generation — 0.5.13

## Architecture

0.5.8 established `minecraftdune:arrakis_dev` as a registered native chunk generator based
on vanilla `FlatLevelSource`. 0.5.13 keeps the fast terrain-column architecture and extends
the serialized terrain profile with coherent lithology and local massif fractures:

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
foundation-connected native rock units and fissures
        ↓
NativeTransverseDuneField
        ↓
full + sixteenth-layer dune sand
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
3. `LithologyField` evaluates coherent 3D units, intrusions, horizontal sheets and mineral
   bands with multi-scale contact roughness;
4. `MassifFractureField` evaluates continuous primary fissures and finite dead-end branches,
   then lowers the rock top where a crack/slot/chasm is active;
5. native lithology replaces the base sand/sandstone down to hard crust and continues to the
   sampled fissure-adjusted rock top;
6. `NativeTransverseDuneField` evaluates the serialized native-dune profile;
7. when the dune surface exceeds the rock surface, full dune-sand blocks and an optional
   sixteenth-layer top are placed.

`getBaseHeight()` and `getBaseColumn()` use the same profile.

## Performance

0.5.13 remains analytic per column:

- no post-generation `ServerLevel#setBlock` cliff construction;
- no finite iterative dune simulation per chunk;
- no iterative fracture propagation or post-generation fissure edits;
- absolute-coordinate 3D lithology plus analytic primary-line and finite-branch distance tests;
- exact early return inside the 0–800 basin;
- far-erg early return after the full open-erg boundary.

The longer Broken Rock Desert adds some additional noise work between the massif and outer
erg, but the far desert still uses the fast path.

## Save / test compatibility

Already generated chunks never change.

For clean terrain comparison, create a new Arrakis Dev world after applying 0.5.13. The new
world stores the explicit `terrain` profile in its generator data.

Full lithology/fracture parameters and future 0.5.14/0.5.15 hooks are documented in
[LITHOLOGY_AND_FRACTURES.md](LITHOLOGY_AND_FRACTURES.md).

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
