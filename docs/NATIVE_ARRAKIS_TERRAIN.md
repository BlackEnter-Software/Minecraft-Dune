# Native Arrakis terrain generation — 0.5.9

## Architecture

0.5.8 established `minecraftdune:arrakis_dev` as a registered native chunk generator based
on vanilla `FlatLevelSource`. 0.5.9 keeps that architecture and expands the terrain column
from a single rock height into two coordinated native layers:

```text
flat Arrakis base column
        ↓
MacroGeologyField
        ↓
provisional native rock
        ↓
NativeTransverseDuneField (where dune suitability > 0)
        ↓
full + sixteenth-layer dune sand
```

Everything is still evaluated from the actual world seed and absolute coordinates during
normal chunk generation.

## Terrain-column order

For each X/Z column:

1. `FlatLevelSource` creates the base bedrock/deepslate/stone/sandstone/sand layers;
2. `MacroGeologyField` determines the provisional native rock top;
3. rock is written from Y=65 to that top;
4. `NativeTransverseDuneField` determines an absolute sand surface above the Y=64 base;
5. when the sand surface is above the rock surface, full dune-sand blocks are placed above
   the rock and a partial sixteenth-layer top is added when required.

This allows a low rock remnant to be buried by a dune while keeping a tall outcrop exposed.

`getBaseHeight()` and `getBaseColumn()` use the same combined terrain profile, so systems
that query the generator's base terrain see both native geology and native dunes.

## Performance

The 0.5.9 fields are analytic per-column calculations. No `ServerLevel#setBlock` terrain
materialization is performed and no iterative dune simulation is run per chunk.

The macro field also has two fast exits:

- the exact 0–800 Arrakeen basin returns immediately with no rock/dune work;
- beyond the mixed sand–rock transition, expensive geological formation/fault noise is
  skipped and only the low-frequency boundary sample plus the analytic dune field remain.

This is intended to preserve the strong 0.5.8 generation performance observed with Distant
Horizons.

## Save compatibility

0.5.8 and 0.5.9 use the same `minecraftdune:arrakis_dev` generator codec, so a 0.5.8 save is
technically loadable.

However, already-generated 0.5.8 chunks keep their old terrain. Newly generated 0.5.9 chunks
use the new province/dune model and can form obvious seams beside old chunks.

For morphology evaluation, create a **new Arrakis Dev world** or remove the relevant region
files while the world is closed.

## Pregeneration

The 0.5.8 chunk-pregeneration manager is retained unchanged:

```mcfunction
/dune geology generate
/dune geology generate_initial
/dune geology generate_nearest <1..12>
/dune geology generation status
/dune geology generation cancel
```

`generate_initial` still means a 100 vanilla-Minecraft-chunk / 1600-block radius around
absolute `(0,0)`. Distant Horizons can also generate terrain independently; the commands are
primarily useful for controlled test regions.
