# Minecraft: Dune patch notes

## 0.5.5 â€” Transverse dune morphology pass

- Promoted the tested Arrakis Dev transverse profile to the development defaults:
  - `cell_size=8`;
  - `surface_resolution=sixteenth`;
  - transverse mode maximum height 30 blocks;
  - `dune_spacing=350`;
  - `spacing_variation=0.18`;
  - `ridge_sharpness=3.0`;
  - `valley_cutoff=0.20`;
  - `repose_angle=33`;
  - `cascade_passes=25`;
  - transverse transport default 180 iterations;
  - `transport_strength=1.0`;
  - `wind_angle=24`;
  - `edge_blend=7`.
- Added `slope_asymmetry` (0.0-1.0, default 0.60). The transverse seed now blends from
  the old symmetric profile toward a wind-oriented profile with a longer windward/stoss
  ramp and shorter lee face before transport and cascading.
- Added `interdune_cleanup` (0.0-1.0, default 0.30). Cleanup is based on local dune support
  rather than a second global cutoff: low dune toes near a major body are retained while
  weak isolated remnants in broad interdune plains are reduced.
- Added mild low-relief transverse transport attenuation to reduce stochastic micro-islands
  on nearly flat sand while preserving transport deformation on the dune body.
- Added a low-height positive-detail attenuation pass before support-aware cleanup.
- Kept `valley_cutoff=0.20`; screenshot tests showed that 0.18 retains too much basin clutter
  while 0.28 cleans the basin at the cost of visibly clipping useful dune-foot material.
- Kept the 0.5.4 sixteenth-layer sand renderer, block/model/texture assets, 0.5.3 camera and
  screenshot tools, 33-degree repose cascade, and barchan prototype otherwise unchanged.
- Kept NeoForge at 21.1.248.
- Updated project version metadata and active dune documentation to 0.5.5.
## 0.5.4 — Fractional dune surfaces

- Upgraded NeoForge from 21.1.244 to 21.1.248.
- Added the registered `minecraftdune:sand` falling block and its block item, model,
  localization, loot table, sand tag, and shovel-mining tag.
- Added `minecraftdune:sand_layer`, a custom 1-15 layer surface block with sixteenth-block
  visual shapes, snow-like stacking, support checks, collision, block item, models,
  localization, loot table, and shovel-mining tag. Placing one more layer on a 15/16 block
  converts it to a full `minecraftdune:sand` block.
- Added `/dune dunes settings surface_resolution whole|eighth|sixteenth`.
- Made `sixteenth` the 0.5.4 default. `eighth` uses even-numbered layer states, and `whole`
  retains the 0.5.3 nearest-whole-block output for direct comparisons.
- Preserved the 0.5.3 simulation, transverse/barchan morphology settings, regional seeds,
  camera presets, fixed-camera teleport behavior, and named/batch screenshot tools.
- Changed only the renderer boundary: the same interpolated physical height is quantized at
  the selected resolution, with full `minecraftdune:sand` below and at most one partial
  `minecraftdune:sand_layer` at the top of each column.
- Generation and clearing recognize old vanilla prototype sand as well as both new custom
  blocks, so an existing 0.5.3 test region can be regenerated or cleared in place.
- Updated project version metadata, README files, and the Arrakis dune prototype guide.

## 0.5.3 — Debug camera and repeatable screenshots

- Made `/dune` the canonical command root for the existing dune laboratory and the new
  camera tools.
- Retained `/minecraftdune` as a compatibility alias, so existing commands and test notes
  continue to work unchanged.
- Added client-side fixed-camera commands:
  - `/dune camera info`;
  - `/dune camera list`;
  - `/dune camera save <name>`;
  - `/dune camera goto <name>`;
  - `/dune camera delete <name>`;
  - `/dune camera tp <x> <y> <z> <yaw> <pitch>`.
- Saved camera presets include dimension, exact position, yaw, and pitch, and persist in
  `config/minecraftdune/debug-cameras.json`.
- Camera recall uses a server-authoritative teleport and supports returning to presets in
  another dimension.
- Added `/dune screenshot <name>` for named captures such as `dune_testG.png`.
- Added `/dune screenshot batch <label> [settle_ticks]` to visit every saved camera in
  alphabetical order and capture consistently named comparison images. The default delay
  is 40 client ticks after arrival at each camera.
- Added `/dune screenshot batch cancel`.
- Batch capture waits for the client to arrive, locks exact current and previous-frame
  position/rotation values, hides the HUD, captures after a fully rendered locked frame,
  and restores the previous HUD state on completion or cancellation.
- Screenshot files are never intentionally overwritten; repeated names receive `_2`,
  `_3`, and later numeric suffixes.
- Aligned the active dune documentation with the current source limit of 512 blocks for
  `dune_spacing` (the original 0.5.2 notes retain the earlier 256-block release limit).
- Updated the project version and human-facing build documentation to `0.5.3`. The
  generated `neoforge.mods.toml` continues to obtain its version from `mod_version` in
  `gradle.properties`.

## 0.5.2 — Transverse dune morphology and physical cascade

- Focused the Arrakis Dev dune laboratory on transverse-dune tuning while leaving the
  experimental barchan initializer unchanged for a later pass.
- Kept the simulation at 64 x 64 cells and capped `cell_size` at 8, so the largest
  synchronous test footprint remains 512 x 512 blocks.
- Changed the 0.5.2 default `cell_size` from 2 to 8 for a 512 x 512 transverse test field.
- Added transverse-specific live controls:
  - `dune_spacing` (32-256 blocks; default 100);
  - `spacing_variation` (0.0-0.50; default 0.18);
  - `ridge_sharpness` (1.0-8.0; default 4.0);
  - `valley_cutoff` (0.0-0.80; default 0.20).
- Removed the abstract `stable_slope` control and replaced it with `repose_angle`
  (10-45 degrees; default 33 degrees).
- Reworked cascade stabilization so it operates on Minecraft-scale coarse heights after
  the transported sand field is mapped to vertical blocks. No percentile normalization is
  performed after cascading, fixing the 0.5.1 behavior where cascade changes were largely
  stretched back toward the original profile.
- Reworked the slope test to account for `cell_size`: the permitted vertical difference
  between neighboring simulation samples is derived from the requested repose angle and
  their physical horizontal separation.
- Made the final cascade non-toroidal so a slope cannot avalanche across the visible
  laboratory region boundary.
- Increased `cascade_passes` from 0-8 to 0-64. Cascading now occurs after transport rather
  than after every transport iteration, making the control cheaper and easier to compare.
- Moved the artificial `edge_blend` fade to the final interpolation stage so it does not
  participate in physical repose stabilization.
- Reduced transverse per-cell seed noise to avoid amplifying low-level contour islands.
- Added documented baseline, flat-interdune, and cascade-stress screenshot profiles.
- Updated project metadata to version `0.5.2`.

## 0.5.1 — Arrakis Dev dune tuning commands

- Added live operator controls for the dune prototype so terrain can be tuned without
  recompiling or restarting the client.
- Added `/minecraftdune dunes settings` and `/minecraftdune dunes settings reset`.
- Exposed these simulation parameters:
  - `cell_size` (1-8);
  - `max_height` (0-32, where 0 uses the dune-mode default);
  - `stable_slope` (0.10-4.0);
  - `cascade_passes` (0-8);
  - `iterations` (0-1000, where 0 uses the dune-mode default);
  - `wind_angle` (-360 to 360 degrees, normalized internally);
  - `edge_blend` (0-32 simulation cells);
  - `transport_strength` (0.0-4.0).
- Made the output region size dynamic: the fixed 64 x 64 simulation grid now expands to
  `64 * cell_size` Minecraft blocks per side.
- Made wind direction, slope stabilization, cascade count, transport strength, output
  height, and interpolation footprint runtime parameters.
- Added `/minecraftdune dunes clear <cell_size>` for clearing a previously generated
  footprint after changing horizontal scale.
- Generation now clears prototype sand up to the hard development ceiling (Y=96) inside
  the active footprint so lowering `max_height` removes old peaks immediately.
- Updated dune documentation and README with a recommended first gentle/wide test profile.
- Corrected the project version metadata to `0.5.1`.

## 0.5.0 — Arrakis Dev dune prototype

- Added deterministic prototype dune generation for the Arrakis Dev world.
- Added operator commands:
  - `/minecraftdune dunes generate transverse`
  - `/minecraftdune dunes generate barchan`
  - `/minecraftdune dunes info`
  - `/minecraftdune dunes clear`
- Added a reduced 64 x 64 sand-thickness simulation rendered into a 128 x 128 block
  region.
- Added saltation-like directional transport, lee-side erosion dampening, and repeated
  slope stabilization.
- Added deterministic region seeds derived from the world seed, region coordinates,
  and dune mode.
- Added edge blending so generated test regions return to the original Y=64 surface.
- Added documentation of the simulation architecture, limitations, and test procedure.
- Updated the mod version from `0.1.0` to `0.5.0`.

## 0.4.0 — Arrakis Dev flat world

- Added the selectable `minecraftdune:arrakis_dev` world preset.
- Added a desert surface at Y=64 with ten sand layers, ten sandstone layers, stone,
  deepslate, and bedrock.
- Disabled overworld biome features, lakes, structures, and caves for deterministic
  testing.
- Retained normal vanilla Nether and End generation.

Public commit: `e24d3d5` — `Added Arrakis-dev flatbiome for testing`.

## 0.3.0 — Muad'dib redesign

- Replaced the original entity geometry with the revised tiny kangaroo-mouse design.
- Updated the runtime Java model, texture, and editable Blockbench source.
- Added the articulated body, ears, front legs, hind legs, and segmented tail used by
  the current animations.

## 0.2.1 — Muad'dib animation correction

- Corrected the animation/model integration produced during the initial animation patch.
- Aligned the revised hierarchy with the runtime model and movement-driven animation.
- Retained stationary random one-shot actions without adding network synchronization.

## 0.2.0 — Muad'dib animation update

- Added `MuaddibMouseAnimations.java`.
- Added coordinated hopping driven by actual entity movement.
- Added a subtle idle and balance loop.
- Added random stationary `wiggle_head` and `sniff_ground` actions.
- Added client-side animation state handling to the Muad'dib entity.

The public repository combines the 0.2.0, 0.2.1, and 0.3.0 development milestones in
commit `16b56a9` — `updated muaddib model`.

## 0.1.1 — NeoForge 1.21.1 compatibility fix

- Replaced the unavailable newer `DeferredRegister.Entities` API with the generic
  NeoForge 1.21.1 entity registry.
- Registered entity types through `Registries.ENTITY_TYPE` and `DeferredHolder`.
- Passed the registered resource identifier to `EntityType.Builder.build(String)`.
- Removed the deprecated explicit event-bus selector from `ClientModEvents`.
- Added the Gradle wrapper JAR.

Public commit: `0e36dd6` — `fixed`.

## 0.1.0 — Initial development build

- Created the standalone Minecraft 1.21.1 NeoForge project.
- Added the Muad'dib desert mouse entity as a rabbit-derived test mob.
- Added the spawn egg, client renderer, initial model and texture, project structure,
  build configuration, and Blockbench workflow.

Public commit: `5574dfa` — `initial`.

## Repository history note

The public repository currently contains four historical commits:

1. `5574dfa` — initial
2. `0e36dd6` — fixed
3. `16b56a9` — updated muaddib model
4. `e24d3d5` — Added Arrakis-dev flatbiome for testing

The semantic versions above describe the project milestones rather than claiming a
one-to-one release tag for every public commit.
