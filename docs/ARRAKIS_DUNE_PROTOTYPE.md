# Arrakis Dev dune prototype

## Status

Version 0.5.3 retains the transverse morphology introduced in 0.5.2 and adds fixed-camera
screenshot tooling around the laboratory. The simulation remains a fixed 64 x 64 grid,
with `cell_size` capped at 8 so the largest synchronous test footprint is 512 x 512
Minecraft blocks.

The main changes in 0.5.2 are:

- transverse dune wavelength is controlled directly in Minecraft blocks;
- ridge regularity, ridge width, and flat interdune floor can be tuned independently;
- `stable_slope` has been replaced by the physically meaningful `repose_angle`;
- cascade stabilization now runs after the sand field is mapped into Minecraft-scale
  heights, so its result is not stretched back out by later percentile normalization;
- cascade range is increased to 0-64 passes;
- the barchan prototype is intentionally left unchanged while transverse dunes are tuned.

## Core commands

```mcfunction
/dune dunes generate transverse
/dune dunes generate barchan
/dune dunes info
/dune dunes clear
/dune dunes clear <cell_size>
```

All commands require permission level 2.

`/dune` is the canonical command root. `/minecraftdune` remains a compatibility alias for
older scripts and notes.

## Live settings

Show the active settings:

```mcfunction
/dune dunes settings
```

Reset to the 0.5.2 transverse-oriented defaults:

```mcfunction
/dune dunes settings reset
```

Change one value at a time:

```mcfunction
/dune dunes settings cell_size <1..8>
/dune dunes settings max_height <0..32>
/dune dunes settings dune_spacing <32..512>
/dune dunes settings spacing_variation <0.0..0.50>
/dune dunes settings ridge_sharpness <1.0..8.0>
/dune dunes settings valley_cutoff <0.0..0.80>
/dune dunes settings repose_angle <10..45>
/dune dunes settings cascade_passes <0..64>
/dune dunes settings iterations <0..1000>
/dune dunes settings wind_angle <-360..360>
/dune dunes settings edge_blend <0..32>
/dune dunes settings transport_strength <0.0..4.0>
```

`max_height 0` uses the selected dune mode's built-in maximum height: 18 for transverse
and 20 for barchan. `iterations 0` similarly uses the mode defaults: 180 and 220.
Wind angles are normalized to 0-360 degrees after entry.

The settings are in-memory development state and reset when the Minecraft process restarts.

## 0.5.2 defaults and parameter behavior

| Setting | Range | Default | Behavior |
|---|---:|---:|---|
| `cell_size` | 1-8 blocks | 8 | Minecraft blocks represented by one simulation cell. Region width is `64 * cell_size`. It changes the output footprint and the physical distance over which neighboring simulation heights interpolate. |
| `max_height` | 0-32 blocks | 0 (= mode default) | Maximum added dune height. For transverse, `0` means 18 blocks. Higher values make the same horizontal form steeper and make repose cascading more likely. |
| `dune_spacing` | 32-512 blocks | 100 | Target transverse crest-to-crest wavelength in Minecraft blocks. It is independent of `cell_size`; 100 should remain approximately 100 blocks at any horizontal scale. |
| `spacing_variation` | 0.0-0.50 | 0.18 | Strength of deterministic phase warping. `0` produces very regular parallel spacing; larger values bend, compress, and stretch ridges locally. It does not add random per-block noise. |
| `ridge_sharpness` | 1.0-8.0 | 4.0 | Shapes the seeded ridge profile. Low values create broad rolling ridges. High values concentrate sand near crests, producing narrower dune bodies and wider low areas. |
| `valley_cutoff` | 0.0-0.80 | 0.20 | Removes the low end of the transverse height field before block placement. Higher values create more genuinely flat Y=64 interdune ground. Too high a value can break ridges into disconnected islands. |
| `repose_angle` | 10-45 degrees | 33 | Maximum stable coarse-grid slope used by the final cascade solver. Lower angles force gentler slopes and spread sand farther; higher angles permit steeper faces. 33 degrees is the physical reference value used by the research-inspired prototype. |
| `cascade_passes` | 0-64 | 16 | Number of block-scale slope-relaxation passes after transport and height mapping. `0` disables final cascading. More passes converge farther toward `repose_angle`; changes usually diminish after the field stabilizes. |
| `iterations` | 0-1000 | 0 (= mode default) | Number of directional transport steps. Transverse default is 180. Higher values allow more migration and distortion but increase simulation time. |
| `wind_angle` | -360..360 degrees | 24 | Prototype wind direction in the X/Z plane. 0 degrees is +X and 90 degrees is +Z. The value is normalized internally to 0-360. |
| `edge_blend` | 0-32 cells | 7 | Width of the artificial test-region boundary fade. `0` disables the fade. This is only a laboratory seam treatment, not part of the eventual Gameplay Arrakis regional generator. |
| `transport_strength` | 0.0-4.0 | 1.0 | Multiplier for saltation-like lifting. `0` disables wind transport while retaining the seeded transverse field and final cascade. |

`dune_spacing`, `spacing_variation`, `ridge_sharpness`, and `valley_cutoff` are currently
transverse-specific controls. They are intentionally not used to repair the experimental barchan
initializer in this release.

## Repeatable camera capture

Save the useful viewpoints once, then use the same positions and view angles for each
terrain profile:

```mcfunction
/dune camera save A
/dune camera save B
/dune camera save C
/dune screenshot batch baseline
```

The batch command visits saved cameras alphabetically, waits 40 client ticks after arrival,
hides the HUD, and produces names such as `dune_baseline_A.png`. Override the delay by
supplying a final tick count, or cancel an active run:

```mcfunction
/dune screenshot batch baseline 60
/dune screenshot batch cancel
```

## Recommended screenshot profiles

### A — 100-block Arrakis baseline

This is the new 0.5.2 default target: approximately 100 blocks between transverse crests,
moderate irregularity, and some flat interdune floor.

```mcfunction
/dune dunes settings reset
/dune dunes settings cell_size 8
/dune dunes settings max_height 18
/dune dunes settings dune_spacing 100
/dune dunes settings spacing_variation 0.18
/dune dunes settings ridge_sharpness 4.0
/dune dunes settings valley_cutoff 0.20
/dune dunes settings repose_angle 33
/dune dunes settings cascade_passes 16
/dune dunes settings iterations 180
/dune dunes generate transverse
```

### B — broad flat interdune test

This profile is intended to answer whether the desert should have visibly larger flat corridors
between narrower dune ridges.

```mcfunction
/dune dunes settings reset
/dune dunes settings cell_size 8
/dune dunes settings max_height 14
/dune dunes settings dune_spacing 110
/dune dunes settings spacing_variation 0.22
/dune dunes settings ridge_sharpness 6.0
/dune dunes settings valley_cutoff 0.35
/dune dunes settings repose_angle 33
/dune dunes settings cascade_passes 16
/dune dunes settings iterations 180
/dune dunes generate transverse
```

### C — cascade stress comparison

Generate this once with `cascade_passes 0`, screenshot it, then change only the pass count to
48 and regenerate the same region. This deliberately uses tall, close dunes and a low repose
angle so the cascade difference should be obvious.

```mcfunction
/dune dunes settings reset
/dune dunes settings cell_size 8
/dune dunes settings max_height 30
/dune dunes settings dune_spacing 80
/dune dunes settings spacing_variation 0.12
/dune dunes settings ridge_sharpness 5.0
/dune dunes settings valley_cutoff 0.25
/dune dunes settings repose_angle 20
/dune dunes settings cascade_passes 0
/dune dunes settings iterations 180
/dune dunes generate transverse
```

Then:

```mcfunction
/dune dunes settings cascade_passes 48
/dune dunes generate transverse
```

## Region model

- Simulation grid: fixed 64 x 64 cells.
- Cell footprint: configurable 1 x 1 through 8 x 8 Minecraft blocks.
- Output region: 64 x 64 through 512 x 512 Minecraft blocks.
- Default 0.5.2 footprint: 512 x 512 blocks.
- Base surface: Y=64.
- Maximum configurable added sand: 32 blocks.
- Region alignment: multiples of the current output region size in X and Z.
- Default wind direction: 24 degrees toward positive X and positive Z.

The region seed is a deterministic hash of the Minecraft world seed, aligned region X/Z,
and dune mode. Keep position and `cell_size` fixed when comparing only morphology controls.

## 0.5.2 transverse pipeline

1. **Seed ridge field**
   - `dune_spacing` defines the wavelength in Minecraft blocks.
   - `spacing_variation` applies low-frequency deterministic phase warping.
   - `ridge_sharpness` controls how concentrated the seeded sand is around the ridge crest.
2. **Directional transport**
   - Occupied cells lift a small sand fraction and move it several cells along the wind.
   - A deterministic coordinate hash controls hop length and crosswind jitter.
   - `transport_strength` scales the lifted amount.
3. **Wind-shadow approximation**
   - Higher upwind sand reduces lifting from the current cell.
4. **Physical height mapping**
   - The transported field is mapped once into the requested Minecraft height range.
   - `valley_cutoff` removes weak transverse sand values to create flat interdune terrain.
5. **Repose cascade**
   - Neighboring coarse cells are compared in Minecraft dimensions.
   - The allowed vertical difference is derived from `repose_angle` and `cell_size`.
   - Material above that limit moves toward the most unstable lower neighbor.
   - No percentile normalization occurs after cascading.
6. **Interpolation and boundary fade**
   - The stabilized 64 x 64 height field is bilinearly interpolated into Minecraft columns.
   - `edge_blend` returns the laboratory output to the Y=64 test surface near its boundary.
7. **Block placement**
   - Sand is added above Y=64.
   - Previous prototype sand above the new target is removed up to Y=96.
   - Non-sand blocks are preserved.

## Research basis

The architecture is inspired by the process separation in Axel Paris et al., *Desertscape
Simulation*, and Brennen Taylor & John Keyser, *Real-Time Sand Dune Simulation*. The latter
uses an approximately 33 degree sand angle of repose and repeated cascade iterations. The
Minecraft prototype is a reduced deterministic approximation and does not claim physical
identity with either published simulation.

## Known limitations

- Generation and clearing execute synchronously on the server thread.
- A 512 x 512 output requires substantial chunk loading and block placement.
- Directional transport still uses a periodic internal 64 x 64 sand grid.
- The final repose cascade no longer wraps across the region boundary.
- There is no underlying bedrock/rock obstacle field yet.
- There is no multi-scale terrain-projected wind field.
- There are no structure reservation masks.
- The transverse family is still seeded before simulation rather than emerging solely from
  sand availability and wind.
- Barchan initialization remains sparse/additive and can form clustered large masses plus
  low residual contour zones; it is intentionally deferred until transverse dunes are tuned.
- Sand does not migrate during Coriolis storms yet.
- Region results are not cached or persisted independently of placed blocks.
- Live tuning values reset when the process restarts.
- Reducing `cell_size` does not clear sand outside the new footprint. Use
  `/dune dunes clear <old_cell_size>` before shrinking it.

## Test procedure

For useful comparisons, keep world seed, region, `cell_size`, and all unrelated settings fixed.
Change one morphology parameter at a time and regenerate the same region. For cascade testing,
use the stress profile above because ordinary 18-block dunes at 100-block spacing can already be
shallower than the requested repose angle and therefore need little or no avalanche correction.

After selecting a promising transverse profile, test repeatability, non-sand marker preservation,
`clean build`, and dedicated-server startup before making it the next baseline.

## Planned successor

Once transverse morphology is convincing, the next terrain step should move the simulation
behind a regional cache and add the underlying rock/bedrock height field plus generation-time
wind. Barchan behavior can then be revisited as a low-sand-availability outcome rather than as a
separate collection of additive primitives.
