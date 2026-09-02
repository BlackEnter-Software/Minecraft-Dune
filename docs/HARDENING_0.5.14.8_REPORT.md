# 0.5.14.8 hardening report

Date: 2026-09-02. This is a local, uncommitted hardening pass, not a new terrain release.

## Repository state and scope

- Repository: `C:\Modding\Minecraft\Minecraft-Dune`.
- Branch: `refactor/0.5.14.8-hardening`.
- Starting HEAD: `9789ea8f3ad682752a99bf3110894151a145c53e`.
- Original .8 base: `04a09caa6dd2e8a3a97236aa48817c79c3dcdd66`.
- Working tree initially clean; no commit or push performed.
- The starting branch already contained base anchoring and eight-block inward support.
  Those are morphology changes relative to original .8, not changes introduced here.
- No macro, scarp, fault, fracture, dune-prototype calibration or preset values changed.
  No BFS, hard-cliff conversion or terrain replacement pass was added.

## Findings and decisions

### Face exposure: narrow inconsistency confirmed, broader diagnosis corrected

The old low-height guard was conditional, not unconditional:
`top <= Y66 && rockFormationMask <= 0.015`. The change uses Y64 when the serialized
`base_anchored_erosion` flag is true and retains Y66 otherwise. The rock-mask conjunction,
corridor suppression and province/fault permissions remain intact. Upper columns use the
same equations. This is an intentional face-eligibility change, distinct from the refactor.

It is **not** a complete fix for the photographed step. At seed 0, block 3053/65/190, the
rock mask already passed the old guard. The shared evaluator reports original top 71,
pre-filter top 70 and filtered top 65. The Y65 deepslate block survives existing erosion
and the basal filter protection through Y69. Surface erosion also retains its <=2-block
original-height exclusion. Changing those policies is deferred rather than disguised
as a refactor or a global sand-height adjustment.

### Orphan support

The filter checks a continuous same-Y inward chain along a cardinal direction selected
from the outward normal, with a bounded contiguous lateral alternative. It does not jump
air gaps. Tests cover isolated removal, solid inward retention, lateral retention, broken
inward/lateral paths, and protected low blocks. Inward support depth stays 8; no BFS was
justified in this pass. Depths 4/6/8 are candidates for a later controlled visual comparison,
not silent settings changes. The known photographed block 3067/96/106 on the random seed
is raw rock but is removed by the existing filter; that does not prove all remnants are gone.

### Talus is structural-contact anchored

`BasalTalusApronField` reads `ScarpMorphologyField.nearestMassifLowSideContact` and inward
macro relief, before final erosion and orphan occupancy. It cannot follow a retreating
surviving face. The random-seed Z173 transect places the apron at X3038..3048 and the main
surviving wall at X3065..3068 under the documented height criteria. A seed-0 bounded survey
finds up to 42 intervening steps from apron to surviving wall. At the specific seed-0 step,
fault suppression instead prevents the apron entirely (0.851 mask versus 0.85 cutoff).

The evidence implicates structural placement plus erosion/filtering and local suppressors,
not one universal off-by-one. Rooting and pre-filter deposit substrate heights are preserved
and must be considered in a contact redesign. Re-anchoring safely exceeds a small contained
edit, so the work document's deferral rule applies. See [measured follow-up design](TALUS_CONTACT_FOLLOWUP.md).

### Chunk API audit contradicts the generic threading suggestion

Inspected the installed Minecraft 1.21.1 / NeoForge 21.1.248 source, specifically
`net.minecraft.server.level.ServerChunkCache#getChunkFuture`, in the local NeoForm
`sourcesAndCompiledWithNeoForge` artifact. On the server thread it calls the main-thread
request and `mainThreadProcessor.managedBlock(...)`. Its off-thread branch schedules that
request on `mainThreadProcessor` and composes the future without that managed-block wait.

Therefore replacing the existing background entry with a direct tick-thread call would
introduce blocking. Both managers keep the thread-aware entry point but capture the chunk
source on the server thread first; completion stays on the server executor. Geology jobs
are now per-server, matching prototype operations. Both maps are synchronized identity
maps, and stale completion callbacks return before touching canceled operation state.
Stopping/canceling removes ownership before canceling pending futures.

This hardens bookkeeping; cancellation does not promise to undo already-started native
chunk generation. No second geology `setBlock` pass exists. In-game cancel/restart and
integrated-server stop/start remain manual tests, not claimed runtime verification.

## Changed files and behavior classification

Java package paths below are relative to `src/main/java/com/blackenter/minecraftdune/worldgen/`;
test paths are relative to the corresponding `src/test/java/.../worldgen/` directory.

| File | Purpose | Classification |
| --- | --- | --- |
| `arrakis/ArrakisTerrainEvaluator.java` (new) | Column composition, raw/filtered occupancy, heights, bounded FastUtil cache | Behavior-preserving extraction |
| `arrakis/ArrakisChunkGenerator.java` | Delegate analysis; retain hooks, seed, palette, foundation and native writers | Behavior-preserving refactor |
| `arrakis/ArrakisTerrainSettingsValidator.java` (new) | Existing semantic bounds/resource/order validation | Behavior-preserving extraction |
| `arrakis/ArrakisTerrainSettings.java` | Keep records/codecs/defaults; delegate validation | Serialized schema and defaults unchanged |
| `arrakis/ArrakisTerrainCommand.java` (new) | Read-only operator inspect command with clipboard report | New diagnostics, no terrain mutation |
| `arrakis/TerrainGenerationMetrics.java` | Hit ratio, evaluations/chunk, saturation/bypasses and worst time | Opt-in observation only |
| `geology/RockFaceExposure.java` | Settings-aware weak-rock basal guard | Intentional eligibility correction |
| `geology/MacroGeologyCommandRegistration.java` | Register `/dune terrain inspect` | New operator command |
| `geology/MacroGeologyGenerationManager.java` | Per-server lifecycle, source capture, stale callback guard | Runtime hardening, no terrain math change |
| `prototype/DunePrototypeOperationManager.java` | Source capture, synchronized ownership, stale callback guard | Runtime hardening; state/simulation unchanged |
| Test `arrakis/ArrakisTerrainEvaluatorValidation.java` (new) | Production fingerprints, cache/order checks, inspect regressions | Build-blocking invariants |
| Test `arrakis/ArrakisContactDiagnostics.java` (new) | Timings, transects, bounded contact survey, outer-contact suggestion | Optional diagnostic only |
| Test `geology/ArrakisProfileValidation.java` (new) | Codec compatibility, rejection tests, current round-trip | Build-blocking; extracted and extended |
| Test `geology/EscarpmentErosionValidation.java` | Preserve invariants/seams/history; add guard/support/fixed-fault tests | Test restructuring and coverage |
| Test `geology/ArrakisVisualRegressionDiagnostics.java` (new) | Coordinate discovery and remnant population scans | Moved out of `check` |
| Root `build.gradle` | Wire evaluator validation into check; separate diagnostic tasks | Build/test wiring |
| Root `.gitignore` | Ignore `logs/` | Repository hygiene |
| Root `logs/debug.log`, `logs/latest.log` | Remove from Git index only | Staged deletions; physical files retained |
| Root `README.md`, `PATCH_NOTES.md` | Current hardening status, inspect usage and known limits | Documentation; historical notes preserved |
| `docs/PROJECT_STRUCTURE.md` | Evaluator/writer responsibilities and validation layout | Documentation |
| `docs/TALUS_CONTACT_FOLLOWUP.md` (new) | Measurements, bounded design and acceptance gates | Deferred-work design, no implementation |
| This report (new) | Evidence, compatibility, validation and handoff | Documentation |

The generator no longer owns analytical field composition, cache implementation, orphan
support composition or final analytical height scanning. Its native writers remain together;
no speculative writer abstraction was introduced. The evaluator's public rock query covers
pre-talus native rock above Y64; foundation writes below that remain generator responsibilities.

## Terrain compatibility and hash accounting

The extracted evaluator and validator keep existing mathematics and semantic validation.
Field names/defaults and save formats are unchanged. `mod_version=0.5.14.8` and profile
`5148` remain. Profiles omitting base anchoring still decode false and use the old guard;
explicit false profiles also preserve it. Existing generated chunks are not rewritten.

For profiles already explicitly opting into true, face eligibility at original tops Y65–66
can change. That is a correction to the current experimental opt-in behavior, not a universal
claim of identical future chunks for every custom true profile. No new version gate was
added because legacy behavior already has the false/missing gate and no new parameters or
defaults were introduced. A released morphology redesign needs a separate compatibility decision.

The old regression hash `10918fcc46909c98` includes face/erosion metadata; it is not a pure
macro-height or block-output hash. Reconstructing only the pre-fix face guard still produces
that exact historical hash. With the correction, eight sampled face descriptions change
within Y65–66 and zero raw occupied blocks change in that three-seed sweep; the new combined
metadata hash is `8eddb8f453b631f6`. The old baseline is still asserted, not simply discarded.

The previous field harness did not include all production additional-material/basal-apron
composition. New shared-evaluator checks cover production material/occupancy and deposit
inputs, height results, below-datum rooting queries and cache/order effects. A temporary
verbatim pre-extraction evaluator was used to establish exact agreement before recording
fingerprint `624f66b5a25a22a3`; that duplicate was removed. The fixture covers three seeds
and sixteen sites, with full vertical occupancy checks; an additional 512-column adjacent-
chunk sweep compares forward/reverse order with different cache limits. These are analytical
tests, not a live Minecraft chunk-write or image-comparison test and not exhaustive world coverage.

## Performance and hot-path review

Cache keys pack full signed X/Z into a long, without dividing by chunk size. Each evaluator
has a fixed immutable seed/profile and operation-local lifetime; limits stay 64/query and
1024/chunk. On saturation columns are recomputed, never replaced with defaults. Tests compare
limits 0, 1, 64 and 1024, including deliberate saturation and extreme signed-key distinctions.
Metrics-disabled and metrics-enabled validation produce the same fingerprint.

Representative **analytical-only** run, after a small warmup (not server world-generation
latency; excludes chunk writes, lighting, I/O and scheduling):

| Seed | Chunk X/Z | Time ms | Column evaluations | Hit ratio | Cached | Bypasses |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| 0 | 190/12 | 4.59 | 256 | 0.00% | 256 | 0 |
| -5640511200611798902 | 191/6 | 24.46 | 291 | 99.31% | 291 | 0 |
| -5640511200611798902 | 193/10 | 44.50 | 256 | 99.41% | 256 | 0 |
| 0 | 41/206 | 82.30 | 397 | 99.78% | 397 | 0 |
| 0 | 159/106 | 17.47 | 256 | 0.00% | 256 | 0 |
| 0 | 0/0 | 0.63 | 256 | 0.00% | 256 | 0 |
| 0 | 409/0 | 0.93 | 256 | 0.00% | 256 | 0 |

No sample reached 1024; this does not establish a global maximum. Zero hits simply means
that workload did not request repeated support columns. The slow eroded-wall case still
makes 180,263 cache-hit calls; caching limits expensive column reconstruction but does not
eliminate per-block support/material work. Repeated runs varied, with the slowest case about
82–96 ms. No speedup is claimed from extraction and no cache retuning was justified.

No boxed coordinate map or global terrain cache was introduced. Existing per-block material
sampling and bounded support amplification remain potential measured optimization targets.
New column records retain references to already-computed geology/face results for inspection.
Metrics use bounded counters, not terrain state, and are enabled only with the game JVM
option `-Dminecraftdune.terrainMetrics=true`; slow logging remains rate-limited.

## Validation

Final results:

- `./gradlew.bat clean build`: **BUILD SUCCESSFUL in 14s**, exit 0;
  11 actionable tasks, nine executed and two restored from cache. Both terrain harnesses
  and prototype-state validation passed. Terrain checks reported 768 seam columns and
  12,288 order columns; hashes are recorded above. No assemble-only substitute was used.
- `git diff --check`: passed, no whitespace errors. Git emits LF-to-CRLF conversion
  notices for modified files; these are not test failures.
- `git status`: same branch/HEAD; 13 tracked files modified, two staged log untracking
  deletions, nine new source/test/documentation files (one test directory is collapsed in
  short status). Changes remain local. The two empty local log files still exist.
- `git diff --stat`: 13 tracked unstaged files, 308 insertions and 1,095 deletions.
  This command excludes the nine new files and the two staged log removals. The separate
  cached diff reports the two empty log deletions, with zero content lines changed.
- Output artifact: `build/libs/minecraftdune-0.5.14.8.jar`, 313,817 bytes.

The Windows wrapper already has a stray leading backslash that prints a command warning;
the build still exits successfully. Gradle also reports deprecated features and the test
logger warns that advanced terminal features are unavailable. These pre-existing tooling
warnings were not folded into this terrain refactor.

Additional completed checks:

- `./gradlew.bat check`: passed during implementation, including terrain and prototype state.
- `./gradlew.bat validateArrakisEvaluator -PterrainMetrics=true`: passed; fingerprint unchanged.
- `./gradlew.bat diagnoseArrakisContact diagnoseArrakisTerrain`: passed. These are optional
  analytical diagnostics, not evidence of visual acceptance.
- New basal-exposure regression was first verified to fail against the old guard.
- Tests retain legacy missing-field compatibility, invalid-profile rejection, prototype
  persistence, deterministic hashes, seam/order checks, fault floors and erosion/support
  invariants. Broad candidate existence/population assertions are no longer build gates;
  explicit fixed fault/fissure/support probes remain build-blocking.
- No custom networking work applies to this pass. Touched common code has no client-only
  imports. Registry/palette behavior and `DunePrototypeState` format remain unchanged.

## Manual acceptance checklist

Use newly generated Arrakis Dev worlds with the checked-in profile for matching comparisons.
Do not delete valuable saves or infer a new result from already-generated chunks. Inspect
commands below are read-only; teleport separately in a disposable creative/spectator world.

| Seed | Feature | Position or transect |
| --- | --- | --- |
| 0 | Inner basal step | `/dune terrain inspect 3053 65 190` |
| 0 | Inner talus/cliff gap | Apron X3028/Z-52 to surviving wall X3072/Z-53, inspect near Y70 |
| 0 | Outer Shield-Wall contact | `/dune terrain inspect 4132 67 0`; measured apron height 3, filtered rock top 64; compare inward wall |
| 0 | Representative eroded wall | Around X657/Z3306, Y222; compare relief and surviving ribs |
| 0 | Fissure | Around X2553/Z1706, Y238; inspect width, depth and calcite |
| 0 | Regional fault floor | `/dune terrain inspect 3042 64 199`; macro top must stay Y64 |
| 0 | Broken low-rock remnants | Low mound and basal teeth around X2980..3053/Z190, compare the latest seed-0 views |
| -5640511200611798902 | Known removed block | `/dune terrain inspect 3067 96 106`: prediction raw=true, kept=false |
| -5640511200611798902 | Current screenshot contact | X3038..3068/Z173; view from around 3089/71/173 |

Also exercise `/dune terrain inspect` with no coordinates, copying the report, an unloaded
target, a non-Arrakis dimension and insufficient permissions. Verify command output against
actual blocks; it reports a prediction from current code and stored settings, not the old
history of the chunk. The foundation line reports only an observed hard layer at/below Y64
from an already-loaded chunk, not the substrate before native writes.

For lifecycle acceptance, start/cancel/restart a bounded pregeneration operation, then stop
and reopen the integrated server while requests are pending; repeat applicable checks on a
dedicated server. These runtime tests have not been performed in this pass.

Remaining visual issues are deliberately open. The next implementation should follow the
[contact/remnant design](TALUS_CONTACT_FOLLOWUP.md), not reintroduce the failed later-main
cliff algorithms or hide the problem under raised sand.
