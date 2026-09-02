# Basal remnant consistency and actual-contact talus

Implementation report, 2026-09-02. Visual acceptance is still required in a new world.

## Starting state

- Repository: `C:\Modding\Minecraft\Minecraft-Dune`.
- Branch: `refactor/0.5.14.8-hardening`.
- Starting HEAD: `64de1b2721452b6fad69b233c1d1e4cf868fcdfc` (`Tweaked Geology`).
- Working tree was clean. This pass makes local changes only: no commit, push or staging.
- The user's latest Seed-0 screenshots establish the accepted mountain-base appearance.
  No macro geometry, scarp, erosion-strength, material-hardness, fracture, fault or dune-field
  parameters were changed. No hard cliff, height deletion, BFS or world-neighbor reads were added.

## Basal remnants: exact change and limits

The previous filter exempted blocks at/below `64 + max(0, minimum_height_above_base)`
without checking base anchoring. With the active value 5, that bypassed support checks
through Y69 even though face erosion was base anchored.

Both erosion-column entry points now consult the existing serialized base-anchoring flag:

| Mode | Protected through | Behavior above protection |
| --- | --- | --- |
| False/missing `base_anchored_erosion` | Y64 plus the configured minimum (currently Y69) | Existing rules |
| True | Y64 | Existing erosion eligibility, relief and inward/lateral support rules |

The eight-cell inward chain, two-cell lateral alternative and minimum face relief of 24
are unchanged. This is removal of one exemption, not stronger erosion or a new connectivity
algorithm. Supported basal ribs and the foundation remain protected by their existing rules.

Fixed reproduction: seed `-5640511200611798902`, block **3052/68/96** is raw rock but now
fails support and is removed. The previously reported 3067/96/106 remains removed.

**Not every pictured tooth has this cause.** Seed 0 at **3050/70/190** is a surface-only
candidate with face relief **18.84**, below the unchanged **24** minimum. It is native
deepslate, not local or basal talus at that Y, and still survives. Y70 was never covered by
the old Y69 exemption. Its cause is now reproduced and reported, not silently treated as
fixed. Addressing it needs a separate decision about low-relief remnant qualification;
globally lowering that guard could damage legitimate smaller formations.

Seed 0 at **3053/65/190** still has a supported Y65 block. Its apron remains suppressed
because its fault mask is about 0.851, exceeding the preserved 0.85 cutoff. Neither result
justifies lowering the accepted mountain or raising the sand datum.

## Actual-contact talus

Old placement used structural scarp signed distance and pre-erosion macro relief. That
could leave the apron behind when carving and filtering moved the visible wall.

New placement is explicitly enabled by `lithology.talus.actual_contact_enabled=true`:

1. Evaluate macro geometry, lithology, fractures and both erosion passes into a cached
   immutable `PreTalusColumn`. Support lookup reads this raw stage, never deposits.
2. Query final filtered rock in a small band above the maximum apron height: currently
   **Y71–76** for the six-block apron. A trial that treated any Y65 rock as the wall
   incorrectly selected thin surviving floor sheets. Looking above the wedge identifies
   the exposed wall while preserving those floors. This is a **talus-only query**, not a
   cutoff applied to rock generation; shallow undercuts have a five-block tolerance.
3. Quantize the structural inward normal to one cardinal direction. An exterior candidate
   searches inward at most **32 cells** for the first non-wall/wall transition. An interior
   candidate searches outward at most `ceil(inset)+1`, capped at 32 (currently five cells).
   The immediately adjacent exterior cell has outward distance **zero**, not one.
4. Probe up to **24 cells inward** from the contact. Representative wall top is the maximum
   final filtered rock top along the connected probe. Stop at a missing wall-band sample,
   suppressed corridor or fault. Relief is that maximum minus Y64; the original 12-to-42
   relief gate and apron shape/material grading are reused.
5. Compose basal talus, existing local scree and dunes with the existing writer rules.

Structural contact supplies side, direction and a coarse eligibility area, never final
placement distance. The canonical source band is scarp width plus 64; the candidate halo
is scarp width plus 96. These are eligibility bounds, **not** 64/96-cell searches. Qualifying
the source as well as using the larger candidate halo prevents a wedge from finding a
wall whose adjacent cell is excluded by the coarse gate. A reproduced edge case at
3116/224 led to a fixed adjacent-cell regression at **3123/224 -> 3124/224**.

Corridor masks above 0.25 and fault masks above 0.85 still suppress basal talus. The path
and relief probe stop at those suppressors; opposing fault walls cannot supply relief
across the core. Fully active fault cores remain at Y64 without basal deposits.

### Measured contact fixtures

| Seed | Adjacent exterior X/Z | Contact X/Z | Result |
| --- | --- | --- | --- |
| 0 | 3001/464 | 3002/464 | Outward distance 0, height 6, gravel crest; final wall top 212 |
| 0 | 3071/-52 | 3072/-52 | Actual contact replaces the formerly separated inner apron |
| 0 | 3123/224 | 3124/224 | Coarse-gate edge regression stays attached |
| 0 | 4086/0 | 4085/0 | Outer wall, inward direction reversed |
| -5640511200611798902 | 3065/173 | 3066/173 | Outward distance 0; apron interval now X3055..3069 on Z173 |

The random-seed structural apron formerly occupied X3038..3048 on Z173. Seed-0's old
3028/-52 misplaced apron fixture is now inactive. A four-block-grid Seed-0 survey over
X2900..3120, Z-100..500 rechecked the adjacent exterior cells of **589 active samples**:
**zero attachment mismatches**. This verifies analytical contact consistency in that
sample, not a rendered image or universal absence of awkward terrain.

## Acyclic evaluator and cache behavior

Each operation owns one bounded primitive-key map (64 query / 1024 chunk entries). Entries
hold a pre-talus column plus lazy filtered top, wall-band presence and composed column.
Final column composition can query rock and support; neither rock nor support can query a
composed column. The contact search does not load chunks or inspect already-written blocks.

Tests verify that pre-talus queries compose zero columns and one final query composes only
the requested column, not its neighbors. Cache limits 0, 1, 64 and 1024, deliberate
saturation, signed coordinates and reversed adjacent-chunk evaluation produce the same
results. Saturation causes recomputation only. No global or cross-world terrain cache exists.

### Analytical-only performance

Representative paired timings with the same basal-support rules, actual-contact on/off:

| Seed / chunk | Contact on | Contact off | Cached columns on/off |
| --- | ---: | ---: | ---: |
| Random / 191,6 | 44.37 ms | 18.52 ms | 884 / 445 |
| 0 / 187,29 | 43.53 ms | 30.41 ms | 636 / 384 |
| 0 / 191,-4 | 62.91 ms | 47.59 ms | 673 / 418 |

Nine representative enabled chunks used 256–884 cached columns, with zero bypasses;
the slowest observed in this run was 75.40 ms at seed-0 chunk 41/206. Repeated timings
vary and these are not statistically controlled benchmarks. Contact has a real cost near
eroded walls; memoizing wall-band presence avoids repeating support checks along nearby rays.
These measurements exclude Minecraft writes, lighting, disk I/O and scheduling. Live
generation performance still needs testing; no broad speedup or universal cache bound is claimed.

## Inspector additions

`/dune terrain inspect [x y z]` now reports:

- Structural validity, signed distance and inward normal.
- Actual-contact mode, searched cells, found state, signed distance, coordinates, contact
  rock top and explicit exclusion/failure reason.
- Representative wall top, relief, probe count and wall query band.
- Talus height and actual outward distance; basal material and local-talus occupancy at
  the queried Y, helping distinguish native rock remnants from later deposits.
- Correct settings-aware protected-through Y, existing support settings and raw/kept state.

All values come from the shared evaluator/helpers. The command remains read-only and does
not force-load chunks. Windows CRLF is normalized to LF so carriage-return glyphs no longer
appear in the chat report. Its prediction can differ from old chunks or player edits.

## Compatibility and validation

Mod version **0.5.14.8**, terrain profile **5148** are unchanged. The new talus flag defaults
false, retaining structural placement for serialized historical profiles; the development
preset explicitly opts in. Spread/inset limits of 32 apply only to actual-contact profiles.
The base-anchoring correction intentionally affects newly generated chunks in existing
profiles that already store `base_anchored_erosion=true`; false/missing retains the old
protection. No existing chunks are rewritten. A new world is required to inherit the new
development-preset talus flag automatically.

Build-blocking coverage includes legacy/current codec round-trips and bounds, unsupported
low remnants, direct/lateral supported buttresses, air gaps, unchanged high-wall behavior,
four directional synthetic contacts, shallow-toe relief, bounded missing-contact behavior,
fault/corridor suppression, five real contact fixtures, historical apron fallback and
unchanged pre-talus rock/material/dune inputs when only contact mode changes.

The original production fingerprint **624f66b5a25a22a3** is still asserted by reconstructing
only the former basal exemption and structural apron. The intentional new fingerprint is
**4587dd069077360f**. The earlier raw-field hash **8eddb8f453b631f6** is unchanged. These
checks do not claim exhaustive generated-world or Minecraft writer coverage.

Final command results:

- `./gradlew.bat clean build`: **BUILD SUCCESSFUL in 23s**, exit 0; 11 actionable tasks,
  nine executed and two restored from cache. Both terrain harnesses and prototype-state
  validation passed, including 768 seam columns and 12,288 order columns.
- `./gradlew.bat validateArrakisEvaluator -PterrainMetrics=true`: **BUILD SUCCESSFUL in
  20s**, same `4587dd069077360f` fingerprint; all five real contact fixtures passed.
- `./gradlew.bat diagnoseArrakisContact`: successful, 589 sampled attachments and zero
  adjacent-cell mismatches. Optional diagnostics are not build gates.
- `git diff --check`: exit 0, no whitespace errors. New files also checked for trailing
  whitespace separately. Git's LF/CRLF notices are conversion warnings, not failures.
- `git status`: 17 tracked files modified and four new files. Nothing staged; branch/HEAD
  unchanged. No files were removed in this pass.
- `git diff --stat`: 17 tracked files, 511 insertions and 85 deletions; excludes the four
  untracked files listed in status. No commit or push.
- Artifact: `build/libs/minecraftdune-0.5.14.8.jar`, 325,667 bytes.

The existing Windows wrapper's leading-backslash warning, Gradle deprecation warning and
test-console terminal warning remain unrelated to this pass. None prevented the build.

## Files changed

Production paths are under `src/main/java/com/blackenter/minecraftdune/worldgen/`:

- `geology/OrphanRemnantFilter.java`: settings-aware basal exemption.
- `geology/BasalTalusApronField.java`: legacy shape reuse, bounded final-contact search,
  connected relief and source eligibility diagnostics.
- `arrakis/ArrakisTerrainEvaluator.java`: pre-talus/composed stages and bounded lazy caching.
- `arrakis/ArrakisTerrainCommand.java`: shared contact/support/deposit report and LF formatting.
- `arrakis/ArrakisTerrainSettings.java`: optional actual-contact flag, default false.
- `arrakis/ArrakisTerrainSettingsValidator.java`: search-compatible bounds for opt-in profiles.
- `src/main/resources/data/minecraftdune/worldgen/world_preset/arrakis_dev.json`: opt in.

Tests under `src/test/java/com/blackenter/minecraftdune/worldgen/`:

- New `geology/BasalRemnantValidation.java`, `geology/ActualTalusContactValidation.java`
  and `arrakis/BasalContactPipelineValidation.java` provide the focused regressions.
- `geology/ArrakisProfileValidation.java`, `geology/EscarpmentErosionValidation.java`
  and `arrakis/ArrakisTerrainEvaluatorValidation.java` retain earlier invariants and wire
  in the new checks; the production hash change is explicitly accounted for.
- `arrakis/ArrakisContactDiagnostics.java`: attachment survey, exact reports, optional
  `--trace` details and paired performance measurements. Its legacy radial metric is
  available with `--legacy-gap` and clearly labeled as a different heuristic.

Documentation: `README.md`, `PATCH_NOTES.md`, `docs/PROJECT_STRUCTURE.md`,
`docs/ARRAKIS_TERRAIN_PROFILE.md`, links from the earlier hardening/design reports, and
this report. Earlier measurements remain historical, not rewritten as new results.

## Fresh-world visual handoff

Create a **new Arrakis Dev world with seed 0**, keeping the accepted massif silhouette and
base appearance as the reference. Confirm `Actual contact: enabled=true` in inspection.

Useful commands:

```text
/dune terrain inspect 3001 70 464
/dune terrain inspect 3071 70 -52
/dune terrain inspect 3123 70 224
/dune terrain inspect 4086 70 0
/dune terrain inspect 3042 64 199
/dune terrain inspect 3050 70 190
```

Compare the view near **2973/78/464** to the latest screenshot: the gravel ridge should
now lie against the eroded wall rather than across a strip of sand. Recheck **3043/72/200**
for remaining teeth and preserved base appearance. Also inspect the eroded wall near
657/222/3306, fissure near 2553/238/1706, and the small-rock area X2980..3053/Z190.
Check both cliff sides, intact ribs, sandy fault core, native dunes and performance.

On a new world with seed `-5640511200611798902`, inspect **3052/68/96** (newly removed low
remnant), **3067/96/106** (earlier removed remnant) and **3065/70/173** (attached talus).

No in-game visual success is claimed. The known low-relief Y70 tooth is intentionally
still listed as open; its qualification rule should be considered separately after review.
