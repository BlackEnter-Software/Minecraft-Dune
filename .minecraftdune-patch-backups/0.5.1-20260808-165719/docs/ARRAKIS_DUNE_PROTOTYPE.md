# Arrakis Dev dune prototype

## Status

Version 0.5.0 adds the first executable terrain prototype for Arrakis. It is a command-
driven development tool operating on the existing flat Arrakis Dev preset. It is not
registered as a chunk generator and does not run during normal chunk generation.

## Commands

```mcfunction
/minecraftdune dunes generate transverse
/minecraftdune dunes generate barchan
/minecraftdune dunes info
/minecraftdune dunes clear
```

All commands require permission level 2.

## Region model

- Output region: 128 x 128 Minecraft blocks
- Simulation grid: 64 x 64 cells
- Cell footprint: 2 x 2 Minecraft blocks
- Base surface: Y=64
- Maximum added sand: 20 blocks
- Region alignment: multiples of 128 in X and Z
- Wind direction: 24 degrees toward positive X and positive Z

The region seed is a deterministic hash of:

- the Minecraft world seed;
- the aligned region X and Z coordinates;
- a separate salt for each dune mode.

## Simulation pipeline

1. **Initial sand field**
   - Transverse mode begins with a high-supply warped ridge field.
   - Barchan mode begins with a sparse set of oriented crescent-shaped deposits.
2. **Directional transport**
   - Every occupied cell may lift a small fraction of its sand.
   - The lifted amount travels several cells along the fixed wind vector.
   - A deterministic coordinate hash supplies hop-length and crosswind variation.
3. **Wind-shadow approximation**
   - A cell with substantially higher sand directly upwind experiences reduced lifting.
   - This is a reduced lee-side retention model, not a computational fluid simulation.
4. **Slope stabilization**
   - Cells compare their height with surrounding cells.
   - Material above the stable slope threshold cascades toward the lowest neighbor.
   - Two cascade passes follow each transport pass.
5. **Minecraft conversion**
   - The continuous sand field is percentile-scaled into the mode's height range.
   - Cell values are bilinearly interpolated over the 128 x 128 block output.
   - A smooth boundary mask returns the added height to zero at region edges.
6. **Block placement**
   - Sand is added above the existing Y=64 surface.
   - Old prototype sand above the new height is removed.
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

The directional stochastic hop mechanism is also informed by the compact MATLAB ripple
experiment supplied during development. That experiment is treated as a visual and
algorithmic reference, not copied as the final world-generation system.

## Known limitations

- The command executes synchronously on the server thread.
- The finite simulation uses periodic internal transport before boundary blending.
- There is no underlying bedrock-height or obstacle field yet.
- There is no multi-scale terrain-projected wind field.
- There are no structure reservation masks.
- The output does not cross region boundaries.
- Dune families are seeded before simulation, so this version is not a fully emergent
  dune morphology solver.
- Sand does not migrate during weather or Coriolis storms.
- No region result is cached or persisted independently of placed blocks.

## Test checklist

1. Create a new Arrakis Dev world with cheats enabled.
2. Run `/minecraftdune dunes info` near the origin and note the selected region.
3. Run `/minecraftdune dunes generate transverse`.
4. Verify long ridges run broadly perpendicular to the reported wind direction.
5. Run the same command again and verify the resulting shape does not change.
6. Run `/minecraftdune dunes clear` and verify the Y=64 base surface remains intact.
7. Run `/minecraftdune dunes generate barchan`.
8. Inspect the field from above and verify isolated crescents and downwind horns.
9. Place a non-sand marker block inside the test region and regenerate.
10. Verify the marker block remains present.
11. Run `clean build` and test both integrated and dedicated server startup.

## Planned successor

The next world-generation stage should move the simulation behind a region cache and add
an underlying rock/bedrock field plus a generation-time wind field. Obstacles and rock
slopes can then affect shadowing, climbing dunes, and echo dunes before the solver is
connected to the Gameplay Arrakis chunk generator.
