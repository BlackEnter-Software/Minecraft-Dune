# Macro geology prototype

## 0.5.7 scope

Version 0.5.7 introduces the first world-scale geological coordinate field for Gameplay
Arrakis. It deliberately does **not** turn each landform into a Minecraft biome yet.

The field is continuous and deterministic from the Minecraft world seed plus absolute X/Z
coordinates. Future biomes, wind exposure, sand availability, rock morphology and dune
regimes can be derived from this shared substrate.

The 0.5.6 transverse dune generator remains frozen as the v1 local dune synthesizer.

## First-region layout

The world origin `(0,0)` is the reference center of the first Gameplay Arrakis region.

- **0–1000 blocks:** hard-reserved Arrakeen / central basin. Macro rock elevation is exactly
  Y=64 here; procedural boundary warping cannot intrude into this protected radius.
- **~1000–1500:** rock transition begins.
- **~1400–3000:** main Shield Wall / massif province.
- **~2800–4000:** eroded outer margin, increasingly broken into isolated rock masses and
  sand corridors.
- **~3600–4200:** open-desert weight rises and suppresses rock generation.
- **Beyond ~4200:** open desert dominates.

The values overlap intentionally. These are environmental weights, not mutually exclusive
biome borders.

## Boundary distortion

After the protected 1000-block Arrakeen radius, a very-low-frequency seeded field offsets
the effective radial coordinate by up to roughly ±250 blocks.

A second set of low-frequency spatial fields controls rock continuity. Seeded angular lobes
break the main rock province into a broad, irregular shield / horseshoe with passes rather
than exposing a perfect circular ring centered on `(0,0)`.

## Current output fields

`MacroGeologyField.Sample` exposes:

- true radius from `(0,0)`;
- distorted/effective radius;
- boundary warp;
- central basin weight;
- rock-transition weight;
- Shield Wall / massif weight;
- eroded-margin weight;
- open-desert weight;
- rock-formation mask;
- provisional base elevation;
- dominant province label.

## Debug commands

Inspect the player's current position:

```mcfunction
/dune geology info
```

Inspect any X/Z coordinate:

```mcfunction
/dune geology sample 0 0
/dune geology sample 1200 0
/dune geology sample 2000 0
/dune geology sample 3200 0
/dune geology sample 3900 0
/dune geology sample 4500 0
```

## Terrain materialization

The normal command materializes the aligned **256 x 256** tile containing the player:

```mcfunction
/dune geology generate
```

Clear that tile with:

```mcfunction
/dune geology clear
```

### Initial 100-chunk overview

For the initial Distant Horizons / macro-scale inspection:

```mcfunction
/dune geology generate_initial
```

This generates a **100 vanilla Minecraft chunk radius** around absolute `(0,0)`. One
Minecraft chunk is 16 x 16 blocks, so the radius is 1600 blocks.

The job includes flat Arrakeen chunks because forcing the underlying chunks to exist is
useful for distant-terrain overview tools as well as for the rock-bearing belt. A radius
this large covers tens of thousands of Minecraft chunks, so the work is intentionally
spread over server ticks.

Check or cancel it with:

```mcfunction
/dune geology generation status
/dune geology generation cancel
```

Only one large-area geology job can run at a time.

### Player-centered nearest tiles

```mcfunction
/dune geology generate_nearest <1..12>
```

The argument is a radius in **256 x 256 geology tiles centered on the player's current
tile**.

Examples:

- `generate_nearest 1` -> 3 x 3 tiles = 768 x 768 blocks;
- `generate_nearest 2` -> 5 x 5 tiles = 1280 x 1280 blocks;
- `generate_nearest 3` -> 7 x 7 tiles = 1792 x 1792 blocks.

Radius 1 therefore generates the tile under the player plus one neighboring tile north,
south, east, west, and all four diagonals.

### Large-job performance behavior

Large jobs use a faster additive materialization path:

1. force/generate the underlying vanilla Arrakis Dev Minecraft chunk;
2. sample the absolute-coordinate macro field for its 16 x 16 columns;
3. add prototype stone from Y=65 to the sampled surface where rock relief exists;
4. never overwrite a non-air/non-stone block.

They deliberately **do not** scan every column from Y=240 downward to erase older prototype
stone. That cleanup behavior remains in the normal single-tile `/dune geology generate`
command. The large commands are intended primarily for fresh inspection areas.

The tick job has both a chunks-per-tick cap and an approximate time budget. Rock-heavy
chunks can therefore reduce the effective rate automatically instead of forcing the entire
100-chunk radius through one server tick.

## Provisional rock scale

The crude slab field can add up to 176 blocks above the Y=64 Arrakis Dev surface, reaching a
maximum debug surface of Y=240.

This is not a finalized Shield Wall height. It is deliberately large enough to test regional
scale before detailed geology is added.

## Planned next geology pass

Once macro placement and scale are convincing, the next layer should subdivide the rock
field into grounded landform morphology:

- massif;
- plateau;
- escarpment;
- ridge / Shield Wall;
- mesa / butte;
- depression;
- exposed rock plain.

After that, a regional wind-exposure field can be projected over the finished topography and
the frozen transverse dune synthesizer can consume local wind/sand-supply inputs.

Salt flats and other environmental surfaces should be derived later from depression,
moisture/salinity and surface fields rather than made synonymous with geological provinces.

Sable/Aeronautics integration is intentionally absent from world generation in this phase.
