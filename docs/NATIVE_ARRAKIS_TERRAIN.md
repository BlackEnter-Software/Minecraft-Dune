# Native Arrakis terrain generation — 0.5.10

## Architecture

0.5.8 established `minecraftdune:arrakis_dev` as a registered native chunk generator based
on vanilla `FlatLevelSource`. 0.5.9 added native far-erg dunes. 0.5.10 keeps the same fast
terrain-column architecture and adds a serialized terrain profile:

```text
FlatLevelGeneratorSettings
        +
ArrakisTerrainSettings
        ↓
ArrakisChunkGenerator
        ↓
MacroGeologyField
        ↓
provisional native rock
        ↓
NativeTransverseDuneField
        ↓
full + sixteenth-layer dune sand
```

Everything remains deterministic from the actual world seed, the serialized generator
profile, and absolute coordinates.

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
3. rock is written from Y=65 to the sampled rock top;
4. `NativeTransverseDuneField` evaluates the serialized native-dune profile;
5. when the dune surface exceeds the rock surface, full dune-sand blocks and an optional
   sixteenth-layer top are placed.

`getBaseHeight()` and `getBaseColumn()` use the same profile.

## Performance

0.5.10 remains analytic per column:

- no post-generation `ServerLevel#setBlock` cliff construction;
- no finite iterative dune simulation per chunk;
- exact early return inside the 0–800 basin;
- far-erg early return after the full open-erg boundary.

The longer Broken Rock Desert adds some additional noise work between the massif and outer
erg, but the far desert still uses the fast path.

## Save / test compatibility

Already generated chunks never change.

For clean terrain comparison, create a new Arrakis Dev world after applying 0.5.10. The new
world stores the explicit `terrain` profile in its generator data.

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
