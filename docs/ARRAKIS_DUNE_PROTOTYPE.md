# Arrakis Dev dune prototype

## Status

Version 0.5.6 freezes the calibrated transverse dune generator as the **v1 baseline**.
It keeps the deterministic 64 x 64 simulation grid, `cell_size` maximum of 8, 512 x 512
synchronous test footprint, 0.5.4 fractional-sand surface, repose-angle cascade, and
0.5.3 fixed-camera screenshot workflow.

The production terrain will eventually vary dune scale regionally. For development,
350-block crest spacing is the baseline because it gives Arrakis-scale forms while still
showing enough morphology inside one 512 x 512 laboratory region.

Barchan generation is intentionally unchanged. Geological macro-height, shield-wall,
rock-field, and terrain-projected wind work is deferred to a later release.

## Core commands

```mcfunction
/dune dunes generate transverse
/dune dunes generate barchan
/dune dunes info
/dune dunes clear
/dune dunes clear <cell_size>
/dune dunes settings
/dune dunes settings reset
```

`/minecraftdune` remains a compatibility alias.

## 0.5.6 transverse v1 baseline

| Setting | Range | Default | Purpose |
|---|---:|---:|---|
| `cell_size` | 1-8 | 8 | Horizontal simulation-cell size; 8 gives a 512 x 512 output region. |
| `max_height` | 0-32 | 0 (= mode default) | Transverse mode default is now 30 blocks; barchan remains 20. |
| `surface_resolution` | whole/eighth/sixteenth | sixteenth | Final vertical surface quantization. |
| `dune_spacing` | 32-512 | 350 | Target transverse crest-to-crest spacing in Minecraft blocks. |
| `spacing_variation` | 0.0-0.50 | 0.18 | Low-frequency deterministic phase warping. |
| `ridge_sharpness` | 1.0-8.0 | 3.0 | Broadens/narrows the seeded ridge body. |
| `valley_cutoff` | 0.0-0.80 | 0.20 | Broad low-end height threshold; kept moderate so real dune toes are not clipped. |
| `slope_asymmetry` | 0.0-1.0 | 0.82 | Moves the crest downwind in the seed profile, lengthening the windward ramp and shortening the lee face. |
| `interdune_cleanup` | 0.0-1.0 | 0.40 | Support-aware removal of weak isolated low-sand remnants in open basins. |
| `repose_angle` | 10-45 deg | 33 | Maximum stable coarse-grid slope. |
| `cascade_passes` | 0-64 | 25 | Number of post-height-map repose relaxation passes. |
| `iterations` | 0-1000 | 0 (= mode default) | Transverse default remains 180; barchan 220. |
| `wind_angle` | -360..360 deg | 24 | Generator wind direction; 0 = +X (east), 90 = +Z (south). |
| `edge_blend` | 0-32 cells | 7 | Laboratory-only fade to the flat test surface. |
| `transport_strength` | 0.0-4.0 | 1.0 | Multiplier for saltation-like lifting. |

These values define the frozen v1 local dune synthesizer. The live controls remain available
for experiments, but `/dune dunes settings reset` always restores this baseline.

## Transverse v1 morphology semantics

### Asymmetric transverse seed

The old transverse seed was fundamentally based on a symmetric sinusoidal cross-section.
The morphology pass introduced in 0.5.5 keeps the same deterministic phase field, spacing
variation, and ridge sharpness, but `slope_asymmetry` blends that profile toward a
wind-oriented cycle:

- long, gentler windward/stoss rise;
- crest shifted downwind;
- shorter lee descent.

The profile is changed before transport and repose cascading, so later processes refine the
directional dune rather than trying to create asymmetry from a symmetric hill.

### Low-relief transport attenuation

Nearly flat transverse cells now receive a mild reduction in stochastic crosswind jitter and
lifting. The reduction is intentionally small so the existing transport behavior remains
recognizable on the actual dune body.

### Support-aware interdune cleanup

`valley_cutoff` remains a global broad-shape control. The screenshot tests showed that raising
it from 0.20 toward 0.28 cleans the plain but also clips useful dune-foot material.

`interdune_cleanup` therefore uses local support instead:

- a low-height cell near a substantial dune body is retained;
- a similarly low isolated patch in an open basin is reduced;
- very low unsupported positive detail is attenuated before cleanup.

This is a morphology filter after physical height mapping, so it is not a sand-mass-conserving
transport step.

### Wind-angle orientation invariant

Wind uses world axes rather than Minecraft player/camera yaw. The validated convention is:

- `0` points toward `+X` (east);
- `90` points toward `+Z` (south);
- `180` points toward `-X` (west);
- `270` points toward `-Z` (north).

Positive angles therefore rotate from `+X` toward `+Z`. The v1 baseline preserves this
orientation behavior unchanged.

## Pipeline

1. Seed deterministic transverse ridges using spacing, variation, sharpness, and asymmetry.
2. Run directional saltation-like transport with wind-shadow approximation.
3. Mildly attenuate stochastic transport in very low-relief transverse areas.
4. Map the transported sand field into Minecraft-scale physical heights.
5. Attenuate unsupported low-height positive noise.
6. Apply support-aware interdune cleanup.
7. Run the 33-degree repose cascade for the configured number of passes.
8. Bilinearly interpolate into Minecraft columns and apply laboratory edge blending.
9. Quantize to whole/eighth/sixteenth surface units.
10. Place full `minecraftdune:sand` plus at most one `minecraftdune:sand_layer`.

## Recommended 0.5.6 baseline checks

### Baseline

```mcfunction
/dune dunes settings reset
/dune dunes generate transverse
/dune screenshot batch baseline056 60
```

### Asymmetry comparison

```mcfunction
/dune dunes settings slope_asymmetry 0.0
/dune dunes generate transverse
/dune screenshot batch asym_000 60

/dune dunes settings slope_asymmetry 0.82
/dune dunes generate transverse
/dune screenshot batch asym_082 60

/dune dunes settings slope_asymmetry 1.0
/dune dunes generate transverse
/dune screenshot batch asym_100 60
```

Keep all other values fixed.

### Interdune cleanup comparison

```mcfunction
/dune dunes settings reset
/dune dunes settings interdune_cleanup 0.0
/dune dunes generate transverse
/dune screenshot batch cleanup_000 60

/dune dunes settings interdune_cleanup 0.4
/dune dunes generate transverse
/dune screenshot batch cleanup_040 60

/dune dunes settings interdune_cleanup 0.7
/dune dunes generate transverse
/dune screenshot batch cleanup_070 60
```

The overhead and low-wind cameras are the most important for these comparisons.

## Known limitations

- Generation/clearing still execute synchronously on the server thread.
- The internal transport grid is still periodic while the final repose cascade is not.
- There is no bedrock obstacle field or terrain-projected regional wind yet.
- The transverse family is seeded rather than emerging solely from sand availability.
- Barchan initialization remains a separate sparse/additive prototype.
- Interdune cleanup is a deliberate morphology filter and does not conserve mapped height.
- Region output is not cached independently of placed blocks.
- Live tuning values reset on process restart.
- `edge_blend` remains a visible laboratory artifact from very high cameras.
- Reducing `cell_size` does not clear sand outside the new footprint; clear the old footprint first.

## Production direction

The production Arrakis generator should not use one universal wavelength. Current testing
supports roughly:

- tighter transverse fields: about 200-300 blocks;
- normal major ergs: about 300-400 blocks;
- open large ergs: about 400-500+ blocks.

The next major terrain stage after transverse morphology is stable should move the simulation
behind a regional cache and introduce the underlying rock/bedrock field and generation-time wind.
