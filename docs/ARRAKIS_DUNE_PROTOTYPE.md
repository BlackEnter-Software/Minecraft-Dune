# Arrakis Dev dune prototype

## Status

Version 0.5.1 keeps the command-driven dune laboratory introduced in 0.5.0 and exposes
the main simulation/output parameters as live commands. The prototype operates on the
flat Arrakis Dev preset and is still intentionally separate from normal chunk generation.

## Core commands

```mcfunction
/minecraftdune dunes generate transverse
/minecraftdune dunes generate barchan
/minecraftdune dunes info
/minecraftdune dunes clear
/minecraftdune dunes clear <cell_size>
```

All commands require permission level 2.

## Live settings

Show the active settings:

```mcfunction
/minecraftdune dunes settings
```

Reset to the original 0.5.0 behavior:

```mcfunction
/minecraftdune dunes settings reset
```

Change one value at a time:

```mcfunction
/minecraftdune dunes settings cell_size <1..8>
/minecraftdune dunes settings max_height <0..32>
/minecraftdune dunes settings stable_slope <0.10..4.0>
/minecraftdune dunes settings cascade_passes <0..8>
/minecraftdune dunes settings iterations <0..1000>
/minecraftdune dunes settings wind_angle <-360..360>
/minecraftdune dunes settings edge_blend <0..32>
/minecraftdune dunes settings transport_strength <0.0..4.0>
```

`max_height 0` restores the selected dune mode's built-in height (18 for transverse,
20 for barchan). `iterations 0` similarly restores the mode defaults (180 and 220).
Wind angles are normalized to 0-360 degrees after entry.

The settings are in-memory development state. They intentionally reset when the
Minecraft server/client process restarts.

## What the parameters do

| Setting | Original | Effect |
|---|---:|---|
| `cell_size` | 2 | Minecraft blocks per simulation cell. Region width is `64 * cell_size`. Larger values make the same simulated forms physically wider and therefore gentler in block-space. |
| `max_height` | mode default | Maximum added blocks above Y=64. Lower this directly to reduce peak height. |
| `stable_slope` | 1.15 | Simulation-space height difference tolerated before sand cascades. Lower values trigger cascading sooner. |
| `cascade_passes` | 2 | Relaxation passes after every transport step. More passes generally produce a more stabilized field. |
| `iterations` | mode default | Number of wind-transport iterations. Higher values allow more migration but increase command time. |
| `wind_angle` | 24° | Direction of the fixed prototype wind in the X/Z plane. 0° is +X and 90° is +Z. |
| `edge_blend` | 7 | Width, in simulation cells, used to blend generated height back to the flat Y=64 boundary. |
| `transport_strength` | 1.0 | Multiplier applied to each saltation-like lifted amount. 0 disables transport while retaining the seeded field and cascade behavior. |

### Recommended first size/steepness test

The original 0.5.0 profile generates 128 x 128 blocks and allows 18-20 block peaks.
For a first wider and lower comparison:

```mcfunction
/minecraftdune dunes settings reset
/minecraftdune dunes settings cell_size 4
/minecraftdune dunes settings max_height 10
/minecraftdune dunes settings stable_slope 0.75
/minecraftdune dunes settings cascade_passes 4
/minecraftdune dunes generate transverse
```

This produces a 256 x 256 block footprint from the same 64 x 64 simulation resolution.
The larger `cell_size` is particularly important: it increases test area without making
the solver itself four times larger, and it stretches each simulation height transition
over more Minecraft blocks.

For a 512 x 512 test, set `cell_size 8`. Block placement and chunk preloading become much
more expensive at that size, so expect a longer synchronous pause.

## Region model

- Simulation grid: fixed 64 x 64 cells.
- Cell footprint: configurable 1 x 1 through 8 x 8 Minecraft blocks.
- Output region: 64 x 64 through 512 x 512 Minecraft blocks.
- Base surface: Y=64.
- Maximum configurable added sand: 32 blocks.
- Region alignment: multiples of the current output region size in X and Z.
- Default wind direction: 24 degrees toward positive X and positive Z.

The region seed is a deterministic hash of:

- the Minecraft world seed;
- the aligned region X and Z coordinates;
- a separate salt for each dune mode.

Changing `cell_size` can change the region alignment away from the origin and therefore
can also change the regional seed. Keep position and cell size fixed when comparing only
shape parameters.

## Simulation pipeline

1. **Initial sand field**
   - Transverse mode begins with a high-supply warped ridge field.
   - Barchan mode begins with a sparse set of oriented crescent-shaped deposits.
2. **Directional transport**
   - Every occupied cell may lift a small fraction of its sand.
   - The lifted amount travels several cells along the active wind vector.
   - A deterministic coordinate hash supplies hop-length and crosswind variation.
   - `transport_strength` multiplies the lifted amount.
3. **Wind-shadow approximation**
   - A cell with substantially higher sand directly upwind experiences reduced lifting.
   - This is a reduced lee-side retention model, not a computational fluid simulation.
4. **Slope stabilization**
   - Cells compare their height with surrounding cells.
   - Material above `stable_slope` cascades toward the lowest neighbor.
   - `cascade_passes` controls how many relaxation passes follow each transport pass.
5. **Minecraft conversion**
   - The continuous sand field is percentile-scaled into the requested height range.
   - Cell values are bilinearly interpolated across the configured block footprint.
   - A smooth boundary mask returns the added height to zero at region edges.
6. **Block placement**
   - Sand is added above the existing Y=64 surface.
   - Prototype sand up to Y=96 above the new target height is removed.
   - Non-sand blocks are preserved.

## Research basis

The architecture is inspired by the layered and process-oriented treatment described in:

- Axel Paris, Adrien Peytavie, Eric Guérin, Oscar Argudo, and Eric Galin,
  *Desertscape Simulation*, Computer Graphics Forum 38(7), 2019/2020,
  DOI `10.1111/cgf.13815`.
- Brennen Taylor and John Keyser, *Real-Time Sand Dune Simulation*,
  Proceedings of the ACM on Computer Graphics and Interactive Techniques 6(1), 2023,
  DOI `10.1145/3585510`.

The prototype adopts their conceptual separation of sand transport and avalanching but
is intentionally much smaller and simpler. It does not claim physical equivalence to the
published simulations.

## Known limitations

- The command executes synchronously on the server thread.
- A 512 x 512 output can require substantial chunk loading and block placement time.
- The finite simulation uses periodic internal transport before boundary blending.
- There is no underlying bedrock-height or obstacle field yet.
- There is no multi-scale terrain-projected wind field.
- There are no structure reservation masks.
- Dune families are seeded before simulation, so this is not yet a fully emergent dune
  morphology solver.
- Sand does not migrate during weather or Coriolis storms.
- No region result is cached or persisted independently of placed blocks.
- Live tuning values are not saved across process restarts.
- Reducing `cell_size` does not automatically clear sand outside the new smaller region.
  Use `/minecraftdune dunes clear <old_cell_size>` before shrinking the footprint.

## Test procedure

1. Create or open an Arrakis Dev world with cheats enabled.
2. Stand near the center/origin of an empty test region.
3. Run `/minecraftdune dunes settings` and record the original profile.
4. Generate one dune mode and take a screenshot or note crest height/spacing.
5. Change only one parameter.
6. Regenerate the same mode in the same region.
7. Repeat until the effect of that parameter is understood.
8. For size and steepness, test `cell_size`, `max_height`, `stable_slope`, and
   `cascade_passes` first.
9. Run the same settings twice and verify the result is deterministic.
10. Place a non-sand marker inside the region and regenerate to verify it is preserved.
11. Run `clean build` and test both integrated and dedicated server startup before
    committing a chosen profile.

## Planned successor

After choosing convincing dune proportions, the next stage should move the simulation
behind a region cache and add an underlying rock/bedrock field plus a generation-time wind
field. Obstacles and rock slopes can then affect shadowing, climbing dunes, and echo dunes
before the solver is connected to the Gameplay Arrakis chunk generator.
