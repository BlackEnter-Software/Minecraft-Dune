# Fault-edge and ravine finishing pass — 2026-09-03

## Scope and starting state

- Branch: `refactor/0.5.14.8-hardening`.
- HEAD: `e9b9fb4dd53b8ed93b80a9f11afa7270931f7f1a`.
- Working tree was already dirty with the preceding component/skirt implementation.
  Those changes were continued, not reset. The user's `gradlew.bat` fix and
  `Mine-Dune-Fix.zip` were preserved.
- No commit, staging or push. No game launch, UI interaction, world migration or in-game
  testing in this pass, as requested. All new measurements below are analytical.
- No changes to MacroGeologyField, RockFaceExposure, raw erosion strengths, lithology,
  fractures, regional fault geometry, dunes, Y64 datum or the chunk material writer in
  this pass. No hard-cliff conversion, scarp deletion or macro-geometry intervention.

## Fault-edge remnants

The existing post-orphan bounded component classifier is now optionally admitted on
weak/medium fault shoulders, instead of excluding every nonzero fault carve mask.
The new `fault_edge_cleanup_enabled` flag requires the existing component/orphan flags.

All other eligibility checks remain: base-anchored erosion, active major/surface erosion,
exposed face with low Y <=70 and relief >12, physical massif weight >0.18, sand corridor
mask <=0.25. Fault carve >0.85 remains protected. Neighbors outside this context still
protect a connected component.

The actual removal rule is UNCHANGED:

- Radius 3; at most four connected columns; no pillar-height exemption.
- Any same-height connection at Y>=66, including diagonal neighbors and high ledges,
  joins columns. The Y64 substrate and thin residual Y65 floor do not join posts.
- A fifth column, protected connection, search boundary, or component span >=3 retains
  the body. Closed small components may lose Y65 through their remaining rock top.
- Broad toes, ribs and buttresses are not cut merely because their tips look small.
- The full Y64 base is never removed. Local scree from a removed component remains disabled.

### Important unresolved photographed tooth

At Seed 0 **3050/70/190**, the previous result was not a candidate (fault mask 0.710).
The new result is a candidate, but `removed=false`, `reaches-support=true`, reason
`protected-connection`. Its neighbor topology connects into the broader low toe. It is
not a closed <=4-column orphan under the agreed rules; no special coordinate exception
or broader cutting rule was added to erase it.

An optional two-seed, 24,010-column survey around the earlier known regions found the
same two removable columns as before (3005/442 and 3005/443, Seed 0, fault mask zero).
Thus this pass improves fault-shoulder eligibility but does NOT claim additional actual
removals in that survey or claim the photographed connected tooth fixed. The survey is
not a build-blocking assertion that artifacts must exist or be removed.

## Ravine talus

The previous diagnosis at **3200/65/200**, Seed 0, detected a 147.64-block exposed face,
but the massif search returned `outside-scarp-search-band`. The old saved profile still
reproduces that result.

With `ravine_contact_enabled`, the original massif evaluation runs first. If it already
has a qualified contact, its result is kept unchanged. Only an unqualified query on a
fault shoulder (0<fault<=0.85, physical massif weight>0.18, corridor<=0.25) may fall back.

The fallback:

1. Traces four cardinal raster directions against final pre-talus occupancy.
2. Uses the existing exterior search bound of 32 blocks; interior search remains
   `ceil(inset)+1`, capped at 32 (five steps with the current inset of four).
3. Reuses the existing foot query band Y71..76 and connected 24-block wall-relief probe.
   Neither imposes a rock-generation cutoff.
4. Requires a source on an allowed fault shoulder with an exposed basal face (low<=70,
   relief>12) and physical massif weight>0.18. Disconnected low debris cannot borrow
   wall relief across an air gap.
5. Chooses the nearest qualified actual foot. Equal distances break by source X, then Z.
   Relief is never summed across competing/opposing walls.

Core/pass suppression applies to the deposit query, every ray cell, every wall-probe
cell, and the source. A ray stops before it can cross a protected central channel.

The contact feeds the existing apron and sand-skirt material paths. The gravel shape is
unchanged (current maximum height six, spread twelve, inset four); the skirt remains
four blocks inward, 24 outward, depth four, smoothstep tapered:
`ceil(4 * (1 - t*t*(3-2*t)))`, `t=max(0,-signedDistance)/24`.

Real cliff rock wins over deposits; visible gravel/local coarse talus wins over the skirt.
The existing ownership-confirmed Y65 residue mantle is reused, not broadened. No Y65
layer is added over empty desert or valid native rock. Strong fault cores receive no
talus/skirt and remain open at Y64.

### Fixed analytical results

| Seed-0 location | Result |
| --- | --- |
| 3200/200 | Ravine source 3200/203, signed distance -2, wall top 211, relief 147; apron height 6, gravel at Y70, sand at Y64, skirt depth 4 |
| 3200/202 | Same source, exterior contact distance zero |
| 3204/125 | Opposite ravine wall: source 3204/124, distance zero, near-wall gravel |
| 3200/180..190 | Full fault core remains Y64, no active skirt/apron |
| 3001/464 and 4086/0 | Original working inner/outer massif contacts and skirts unchanged |
| 2991/464 and 2988/464 | Existing safe erosion-residue mantle versus retained native foreland rock unchanged |

An optional 4,114-column, two-seed survey along X3192..3208/Z100..220 found 237 active
exterior ravine deposits, zero missing adjacent-source aprons. Seed-0 samples included
110 north-directed and 122 south-directed contacts. This is a bounded diagnostic, not
visual acceptance or a build-blocking population search.

## Bounded cache correction

The extra rays exposed a first-insertion cache problem: retaining only the first 1,024
columns made later columns recompute post-orphan/component results repeatedly per Y.
The fixed ravine chunk 199/12 took about 12,036 ms and 588,248 column evaluations.

The operation-local cache now evicts the least-recently-used entry at capacity. The
maximum stays 1,024 (64 for inspection); zero-capacity uncached operation is still
supported. No global/shared cache or mutable cross-chunk dependency was introduced.

The same offline chunk sample then took about 76 ms, 1,556 column evaluations and a
98.98% hit rate. The previous profile took about 38 ms. These are individual analytical
diagnostics, not a controlled benchmark or native game timing. The regression suite
checks deterministic eviction/reuse and terrain equality, not a wall-clock threshold.

## Compatibility and inspector

Version **0.5.14.8**, profile **5148**, unchanged. New optional booleans default false:

- `erosion.orphan_remnants.fault_edge_cleanup_enabled`
- `lithology.talus.ravine_contact_enabled`

The development preset enables both. Prior serialized worlds retain prior behavior;
use a NEW Arrakis Dev Seed-0 world next time, even if the previous test world's chunks
have not yet generated. This does not use failed 51411–51414 semantics, 0.5.15 subsurface
geology or 0.5.16 wind-driven banking.

The inspector adds `fault-edge-enabled` to component output and `source=massif` /
`source=ravine` to contact output. Reasons, wall heights, deposits and skirt continue to
come from shared production helpers, not duplicate command mathematics.

Historical fingerprints remain separately asserted:
`624f66b5a25a22a3` (historical reconstruction), `4587dd069077360f` (before finishing),
`e8504e17b09f878f` (previous component/skirt pass).
The opted-in current fingerprint is `485af85209e2da18`.
Raw erosion remains `8eddb8f453b631f6`.

## Validation and modified files

New `RavineContactValidation` covers four wall orientations, nearest-source selection,
canonical ties, wall adjacency, inward overlap, radius/probe bounds, no source across a
core, no relief borrowed across a core/air gap, and rejected sources.

New `RavineFinishingValidation` covers both fixed ravine walls, gravel/sand order, full
core Y64, connected-toe retention, unchanged existing massif contacts and skirts,
upstream geology/erosion/face/lithology/fracture/dune equality, codec opt-ins, inspector,
reverse query order across X3199/3200, caches 0/1/64/1024, and bounded LRU reuse.
Existing isolated/tall-component, rib/buttress, skirt, and fault invariants remain.

Changed in this pass:

- `ArrakisTerrainEvaluator.java`: shoulder eligibility, source qualification, bounded cache.
- `BasalTalusApronField.java`: reuse original contact solver with optional ravine fallback.
- `ArrakisTerrainSettings.java`, `arrakis_dev.json`: two serialized opt-ins.
- `ArrakisTerrainCommand.java`: shared diagnostics.
- `BoundedBasalComponentCleanup.java`: clarify protected-core contract (algorithm unchanged).
- `BasalFinishingValidation.java`, `BasalContactPipelineValidation.java`,
  `ArrakisTerrainEvaluatorValidation.java`: legacy/policy/golden assertions.
- New `RavineContactValidation.java`, `RavineFinishingValidation.java`.
- `ArrakisContactDiagnostics.java`: optional fixed-point/topology/attachment measurements.
- This report, profile guide, and a historical-report supersession notice.

Final `gradlew.bat clean build`: **PASS**, 40 seconds, including all existing and new
deterministic suites. The first build stopped at the intentionally changed active-output
fingerprint; it was updated only after the focused invariants and all three legacy hashes
passed. The cache correction was then included in the final clean build.

`git diff --check`: **PASS** (only repository line-ending conversion notices).
Status and diff statistics reviewed; index empty, no commit/push. Tracked diff totals
include prior unfinished work and the user's wrapper fix, and exclude untracked new
helpers/tests/reports; they are not a count of just this pass. The protected macro/face/
erosion/fracture source files have no diff from HEAD.

## Next visual check (deferred by request)

Use the new Seed-0 profile at 3200/84/200 (south ravine wall), 3204/80/125 (opposite wall),
and central X3200/Z180..190. Recheck the working inner/outer contacts near 3001/464 and
4086/0, plus 2963/615 and 3050/254. Inspect the retained connected tooth at 3050/70/190
separately; deleting it would need a different, narrowly justified protrusion rule, not
a claim that it is disconnected. No visual success is asserted for this pass.
