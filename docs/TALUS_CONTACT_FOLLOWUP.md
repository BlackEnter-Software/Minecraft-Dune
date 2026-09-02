# Next pass: basal remnants and talus contact

Status: design and measurements only, 2026-09-02. No new contact solver, erosion retuning,
or talus relocation was implemented in the hardening pass. Preserve the current continuous
0.5.14.8 massif and the useful upper-wall erosion pattern.

## What the evidence says

The screenshots show basal teeth, a narrow raised rock floor and talus separated from
the wall. Analytical measurements corroborate a mismatch but do not establish visual
acceptance. Measurements below use the checked-in profile and shared production evaluator.

### Seed 0: one-block step at 3053 / 65 / 190

- Original/fissure top: 71; pre-orphan top: 70; filtered top: 65.
- Y65 is raw occupied rock and survives filtering: deepslate, HARD resistance.
- The original face guard already allowed this column: rock mask 0.046 exceeds 0.015.
  Therefore the settings-aware face-guard fix does not remove this particular step.
- Major erosion is a candidate (relief 42.10, retreat 2.92); surface erosion is active
  (maximum retreat 4, face strength 0.076).
- Orphan filtering deliberately protects through Y69. Base anchoring still has a vertical
  erosion ramp, and surface erosion still excludes original rock heights of two or less.
- Fault carve mask 0.851 exceeds the basal-apron cutoff of 0.85. Local and basal talus
  are both absent here. This is not simply a misplaced active gravel wedge at this point.

Next: use exact surviving blocks to distinguish raw erosion permission, retained basal
protection, support routes and foundation writes. Do not lower the entire mountain or
raise sand to cover the step. A localized basal policy change needs its own regression
and compatibility decision; it is not an evaluator refactor.

### Random seed -5640511200611798902: Z173 transect

Along X2990..3160:

| Stage | Measured contact/interval |
| --- | --- |
| Structural low-side contact, within one block | X3044..3045 |
| Basal apron present | X3038..3048 |
| Main surviving wall reaches Y84 | Starts at X3065 |
| Main surviving wall contains rock at Y70 | Starts at X3068 |
| Raw thin elevated pieces before filtering | X3059..3062 at Y84 |
| Those same elevated pieces after filtering | Removed |

There are 16–19 intervening X columns between the apron end and these two wall criteria.
Major erosion removes much of the intervening low wall; orphan filtering removes additional
pieces. Surface erosion also modifies the lower foreground. The nominal structural
contact does not follow those changes. These intervals do not mean all intervening space
is air at every height.

### Seed 0: independent apron-to-wall survey

A grid X2900..3120, Z-100..300 at four-block spacing selected active apron samples on
the inward side and followed their structural inward normal for at most 64 steps.
For diagnosis only, a surviving wall was defined as filtered top at least Y84 and solid
pre-talus rock at Y70. Of 61 rays, 59 reached that criterion and two did not within the bound.

The largest measured separation was 42 intervening steps: from apron X3028/Z-52,
last contiguous apron step 1, to wall X3072/Z-53 at step 44. The wall's fault mask is zero.
Thus a substantial separation also occurs away from the screenshot's strongly faulted
seed-0 step. This is a bounded sample, not a global maximum and not 42 proven air columns.

## Contained implementation proposal

1. **Record a baseline before modifying geometry.** Keep raw/filtered occupancy and
   deposit snapshots for the sites above, both cliff sides, a fault core and a sand corridor.
   Identify exact coordinates of the remaining scattered teeth with `terrain inspect`.
2. **Separate final pre-talus rock from deposits.** Reuse the evaluator's lithology,
   fracture, erosion and support functions. Its current `column()` still computes a
   structurally independent apron; simply calling it recursively from a new apron solver
   would create a dependency cycle. Introduce a rock-only internal stage/cache first,
   verified against the existing occupancy fingerprint. Keep foundation rooting a distinct
   below-datum concern and define contact using rock above the datum.
3. **Design a bounded surviving-foot query.** Use structural contact as a search hint,
   not the accepted final foot. Test a fixed deterministic set of inward samples against
   surviving rock and contiguous attachment to the wall. Choose a measured search bound
   before implementation; the current 64-step diagnostic is not yet a production default.
   Distinguish a tall source cliff from detached small rocks, fault shoulders and a low
   broad foundation sheet. Handle inner and outer boundaries consistently.
4. **Attach talus to that foot.** Place the existing short gravity-driven wedge against
   surviving rock, taper outward, preserve gravel/sand grading and all corridor/fault
   suppressors. A missing source returns no apron. No forced vertical wall, structural
   ramp deletion or deposit used to conceal an invalid rock floor. Local scree stacking
   and dune substrate composition must be explicitly tested together.
5. **Handle basal teeth separately.** Reproduce each reported block and identify why it
   survives. Test a narrowly defined basal exception only if that evidence warrants it.
   Do not weaken all hard material or remove genuine supported ribs. Keep support depth 8
   initially; a later controlled 4/6/8 A/B study can quantify rib preservation versus thin
   remnants. No BFS/flood-fill in this design.

The rock-only query must never request talus, terrain writes or neighboring loaded chunks.
Support queries remain bounded and read raw analytical occupancy, so no recursive
filtered-support search is introduced. Caches stay seed/profile-bound and operation-local;
cache saturation must only cause recomputation. Avoid per-block contact searches: query
once per relevant column and measure both average and worst work before accepting it.

## Acceptance gates

- Same macro field, upper-wall shapes, fault sandy core and Y64 datum; do not import the
  failed hard-cliff algorithms from later main-branch experiments.
- Remove reproduced invalid basal teeth without creating floating material or deleting
  supported narrow ribs. Verify the one-block step explicitly, not just from a distant view.
- Talus touches its intended surviving wall without a long intervening shelf/trench;
  a fault/corridor-suppressed apron must stay suppressed.
- Deterministic fixed probes, signed coordinates, alternate cache limits, neighboring
  chunk generation orders and missing/false legacy settings all pass.
- Performance is measured on eroded walls, fissures, inner/outer contacts and open desert;
  no unbounded search or persistent cross-world terrain cache.
- Compare newly generated worlds/chunks for seeds 0 and -5640511200611798902 from matching
  viewpoints. No visual success claim until those comparisons are reviewed.
- Decide how any new morphology setting is serialized before release. A future
  0.5.14.8.1 / 51481 gate is a proposal, not a version change made in this pass.

Run `./gradlew.bat diagnoseArrakisContact` to reproduce the measurements. Use
`/dune terrain inspect x y z` for a copyable in-game prediction; it does not replace
inspection of actual chunks or reconstruct old-world generation settings.
