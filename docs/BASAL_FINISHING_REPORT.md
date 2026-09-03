# Basal finishing replacement — 2026-09-03

Historical report for the first replacement. The subsequent
[fault-edge/ravine finishing report](FAULT_EDGE_RAVINE_FINISHING_REPORT.md) supersedes its
blanket fault-exclusion policy, contact coverage, cache policy and active fingerprint.
The original sand skirt dimensions and connected-component thresholds remain unchanged.

## Starting state and scope

- Repository: `C:\Modding\Minecraft\Minecraft-Dune`.
- Branch: `refactor/0.5.14.8-hardening`.
- Starting HEAD: `e9b9fb4dd53b8ed93b80a9f11afa7270931f7f1a` (`Talus fixed 95%`).
- The working tree already contained the ineffective, uncommitted finishing pass.
  Work resumed with the replacement partly applied; this report supersedes the old report.
- The user's `gradlew.bat` fix and untracked `Mine-Dune-Fix.zip` were preserved.
- No staging, commit or push. Existing worlds/regions were not edited or deleted.
- MacroGeologyField, RockFaceExposure, erosion strengths, fractures, lithology/hardness,
  fault geometry, dunes, actual-contact search and wall-relief probe are unchanged.
- No hard-cliff conversion, scarp deletion, macro height cutoff or aeolian deposition.

## Replaced code

Removed the experimental `BasalMicroRemnantTrim` and nested `SandConcealment` in
`BasalTalusApronField`. Replaced their evaluator, writer, inspector and validation hooks.
The previous radius-two/three-block-height test and exterior-only 20-block underlay are
not retained as an extra stage. `BasalTalusApronField.java` itself now matches HEAD.

## Exact bounded-component rule

Pipeline: raw erosion -> existing orphan filter -> bounded component cleanup -> existing
actual contact/wall probe -> talus and sand skirt. Support never queries cleaned rock,
contact, deposits, or neighboring chunks, so there is no recursive dependency.

Eligibility requires both orphan and component flags, base-anchored erosion, major/surface
erosion activity, an exposed face with low Y <=70 and relief >12, physical massif weight
>0.18, corridor mask <=0.25, and fault-carve mask exactly zero. Any fault influence excludes
the component stage, including toes outside the strong central-core mask.

- Horizontal Chebyshev search radius: **3 blocks**.
- Maximum removable component: **4 columns**; a fifth connected column retains it.
- Edges: neighboring columns, including diagonals, sharing any occupied Y >=66 in the
  POST-orphan result. Scan the whole remaining vertical interval, not just the foot.
- Y64 substrate and the residual Y65 floor do not connect otherwise detached posts.
- All vertical fragments in one column form one conservative graph node. This can retain
  ambiguous remnants but protects high ribs/ledges connecting to the massif.
- A connected protected-context column, a search-edge contact, or component span >=3 in X
  or Z retains the component. The span guard prevents deleting middle cells of an elongated
  four-column component while preserving its endpoints.
- Only a closed component within the thresholds is removed. No height/protrusion exemption:
  a tall isolated post is removed through its post-orphan top, including its Y65 root.
  Y64 and below are never deleted. A Y65-only floor is not a pillar candidate.
- Discard local scree sourced by a removed component so it cannot float at the old raw top.
  Other local scree is unchanged.

Queue storage is bounded to four nodes plus a larger-body witness. Operation-local column
caches store decisions and lazily sampled post-orphan occupancy; cache saturation only
recomputes. No global cache, unbounded flood fill or generation-order dependency was added.

## Exact sand skirt

Uses the existing qualified actual contact (`enabled`, `found`, reason `found`). It also
inherits apron-enabled, structural-side, source, path, corridor and fault exclusions.
The current contact search and connected wall-relief probe are not changed.

- Inward overlap: **4 blocks**, including signed contact distance +4.
- Outward reach: **24 blocks**, none at or beyond 24.
- Burial: maximum **4 layers**, Y61–64.
- For signed distance `s`, active domain is `-24 < s <= 4`.
- Let `t = max(0,-s)/24`; `depth = ceil(4 * (1 - t*t*(3-2*t)))`.
- Contact/inward depth 4; outward distance 12 depth 2; distance 23 depth 1; distance 24 off.
- The native-grid stair steps follow a smoothstep envelope; there is no new raised terrain
  ramp. Only eligible pre-existing Y65 residue can receive the optional visible mantle.

Material order in the existing basal writer: real final cliff rock, then visible basal/local
gravel or coarse talus, then skirt, then existing desert substrate. At/below Y64 the skirt
can conceal native foundation roots, including the requested inward overlap. At Y65 and
above real cliff/buttress rock is preserved. The ordinary dune writer remains unchanged.

### Y65 ownership check (not guessed from screenshots)

Analytical queries were made at the user's fixed regions before enabling a mantle:

| Seed 0 X/Z | Pre-skirt Y65 owner | Result |
| --- | --- | --- |
| 3001/464 | Deepslate erosion floor, original top 144 -> final 65 | Sand under gravel; depth 4 |
| 2991/464 | Blackstone erosion floor, original top 75 -> final 65 | Safe Y65 sand; depth 3 |
| 2988/464 | Native blackstone foreland, original AND final top 65 | Y65 rock retained; buried depth 2 |
| 2987/464 | No native Y65 rock | No visible mantle added over empty desert |
| 3005/464 | Real cliff, final top 174, signed distance +4 | Roots Y61–64 concealed; cliff Y65 retained |
| 2963/615 | Real deepslate body, final top 127 | Rock retained, not blanket-covered |
| 3050/254 | Deepslate erosion floor, original top 93 -> final 65 | Safe mantle outside strong core |
| 3043/200 | Full fault core; no native Y65 rock | Skirt suppressed; Y64 sand remains open |

A Y65 mantle is permitted only where the original native top was >65, an exposed basal
face has low Y <=65, erosion is active, final filtered top is exactly 65, and native rock
still occupies Y65. The existing actual-contact qualification must also succeed. Gravel
and local talus still win. This is a one-layer material replacement, never new Y65 height
over air, and never a blanket covering of low native foreland formations.

The distinction at 2988/464 is intentional: dark color alone is not proof of an artifact.
It remains visible under the user's real-rock-first constraint. The mantle handles genuine
erosion-floor rims, not every dark block along the desert edge.

## Fault protection

Component cleanup rejects any nonzero fault carve. The skirt uses the unchanged contact
gates: fault carve >0.85 or corridor >0.25 suppress it; contact/probe paths stop on protected
cells and disconnected air gaps. Consequently no new apron bridges opposing walls or
raises/refills the full Y64 sandy core. Existing weaker-fault talus is preserved.

## Inspector

`/dune terrain inspect` reads shared evaluator/helper results, not duplicate terrain math:

- Component: candidate, removed, radius, component columns, supported/boundary, reason.
- Skirt: active, signed actual-contact distance, inward/outward bounds, local depth,
  optional Y65 mantle, material at the query.
- Pre-skirt ownership at Y64, Y65 and queried Y; existing final basal-material line includes
  rock/talus precedence, so raw skirt eligibility is distinguishable from a material write.

## Compatibility

Version **0.5.14.8**, profile **5148** unchanged, following the repository's existing explicit
opt-in convention. New optional booleans default false; the development preset sets true:

- `erosion.orphan_remnants.basal_component_cleanup_enabled`
- `lithology.talus.basal_sand_skirt_enabled`

Pre-finishing saved profiles without these flags retain their generation fingerprint.
The retired experimental keys `micro_trim_enabled` / `sand_concealment_enabled` are ignored,
not aliases. Experimental worlds still decode, but newly generated chunks no longer
reproduce those discarded finishing systems. Use a NEW Arrakis Dev world for this pass;
existing generated chunks are never migrated. No 51411–51414 semantics, 0.5.15 geology or
0.5.16 wind/sand-banking behavior was introduced.

## Verification

Focused deterministic tests cover single-column posts at heights 2/3/6/10/40/150; pairs;
2x2 components; broad buttresses; inward-connected ribs; a high ledge bridge; conservative
search-edge retention for every member; protected fault connections; radius bounds;
contact/inward/midpoint/distal skirt; gravel priority; native-rock priority; safe mantle
versus initially low rock/air; Y64 fault core; rejected contact paths and sources; inspector;
flag compatibility; reversed queries and cache capacities 0/1/64/1024.

The optional 24,010-column, two-seed survey found only TWO new removals: Seed 0 X3005,
Z442 and443, a disconnected two-column body with post-orphan top Y81. Those exact cells
are now fixed regression fixtures (not a candidate search in the build). This intentionally
does not erase connected remnants or protected fault teeth.

Existing historical and pre-finishing hashes remain `624f66b5a25a22a3` and
`4587dd069077360f`. Active output hash is intentionally `e8504e17b09f878f`.
Raw erosion validation hash remains `8eddb8f453b631f6`.
The optional 589-contact attachment survey reports zero mismatches.

Analytical-only chunk measurements (one diagnostic run, not a benchmark): active finishing
about 58–65 ms vs disabled 39–59 ms for three contact-heavy chunks; cached columns <=906,
no cache bypass there. Full visual/native timing is separate from analytical timing.

Final `gradlew.bat clean build`: **PASS**, 46 seconds, including the fixed Y81 component
regression and existing terrain validations. `git diff --check`: **PASS**. Status and diff
statistics were reviewed; no staged changes. Ordinary diff statistics exclude the four new
implementation/report files until tracked (and include the user's wrapper fix).
No separate `validateArrakisTerrain` invocation; it runs through `clean build` / `check`.

## Changed files

- New helpers: `geology/BoundedBasalComponentCleanup.java`, `geology/BasalSandSkirt.java`.
- Integration: `arrakis/ArrakisTerrainEvaluator.java`, `ArrakisChunkGenerator.java`,
  `ArrakisTerrainSettings.java`, `ArrakisTerrainCommand.java`.
- Preset: `src/main/resources/data/minecraftdune/worldgen/world_preset/arrakis_dev.json`.
- Tests: `BasalFinishingValidation.java` (replacement), `ArrakisTerrainEvaluatorValidation.java`,
  `BasalContactPipelineValidation.java`, `ArrakisContactDiagnostics.java`.
- Documentation: this report and `docs/ARRAKIS_TERRAIN_PROFILE.md`.
- The deleted experimental helper was untracked; its deletion does not appear in HEAD diff.
- `gradlew.bat` and `Mine-Dune-Fix.zip` are pre-existing USER changes, not this implementation.

## Visual checklist / remaining limits

Use Seed 0: (2963,615), (3001,464), (3043,200), (3050,254), plus (3005,442–443) for the
confirmed removed component; outer contact (4086,0); recess/contact (3071,-52).
Inspect 2991/464 vs2988/464 for erosion-residue mantle versus intentionally retained native
foreland rock. Fault-associated tooth 3050/70/190 remains protected, not falsely claimed fixed.

Check cliff-contact gravel, preserved ribs/buttresses, open sandy core, and no new slab
conversion. Buried depth needs a numerical/material check, not just a surface screenshot.
### Visual execution

The computer-use skill was used to create a separate Creative, Seed-0 **Arrakis Dev** world
named `Basal finishing Seed 0 - 2026-09` (the UI truncated the longer name). The existing
world was not opened or changed. A fresh in-game view near the preset spawn (3021,77,235)
showed the eroded walls and open sand corridor. The user then stopped Computer Use with
physical Escape. All game interaction stopped immediately, and the new world was left open.
The coordinate-by-coordinate visual checklist, in-game inspector commands and subsurface
material inspection were therefore NOT completed; analytical tests are not visual acceptance.
