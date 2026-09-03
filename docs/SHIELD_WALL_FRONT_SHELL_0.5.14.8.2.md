# 0.5.14.8.2 - Shield-Wall front-shell cleanup

## Finding

The regular inner-wall pillars were not independent orphan components. The existing
post-orphan component classifier showed them reaching protected/supporting rock, while their
columns followed the curved structural massif ramp. In code, `MacroGeologyField` supplies the
smooth 36/48-block physical scarp, the two erosion fields remove parts of it, and the remaining
connected columns survive both remnant filters. They are therefore surviving pieces of the
original desert-facing massif shell.

The fix does not alter that smooth input. It adds one small analytical stage:

```text
MacroGeologyField
-> fractures
-> major erosion
-> surface erosion
-> existing orphan filter
-> existing bounded-component filter
-> ShieldWallFrontShellCleanup
-> final pre-talus rock
-> actual contact and wall-relief probe
-> talus
-> dunes
```

## Direction and two-pass rule

`ScarpMorphologyField.nearestMassifLowSideContact` defines positive signed distance as the
massif side of either low-side contact. Its inward normal points into the massif:

- inner wall: radially away from the central basin;
- outer wall: radially toward the massif center.

The desert-facing outward normal is its negation. The cleanup quantizes the local inward
normal to its dominant block-grid axis, then evaluates actual surviving columns on the
outward side. Pass 1 peels at most two horizontal blocks. Pass 2 reads the pass-1 result and
peels at most two more. The combined retreat is validated at no more than four blocks.
This is not a global radius subtraction and the retained wall remains irregular.

Removal is column-wide. If the selected shell column contains native rock from its foundation
through a top ten or a hundred blocks higher, all of that native column is omitted. The
ordinary sand/sandstone/stone substrate remains; no world blocks are excavated after generation.

## Ownership and compatibility

The shared terrain evaluator derives eligibility from its already-sampled geology:

- physical massif and final rock masks must own the column;
- any regional-fault or sand-corridor influence protects it;
- a stronger explicit foreland, Broken Rock, or sand-rock-transition weight protects it;
- the target must be within the appropriate structural scarp ramp and actual post-filter rock
  must exist.

This avoids duplicate geology and prevents the peel from cutting across intersecting structures.
The top-level `front_shell_cleanup` group defaults to disabled. The active profile stores
`enabled=true`, `pass1_depth=2`, and `pass2_depth=2`. The terrain profile remains `5148`; mod
version is `0.5.14.8.2`. Missing settings retain previous terrain behavior.

No `MacroGeologyField`, scarp width, massif offset, fracture, erosion, orphan, actual-contact,
wall-relief, talus, skirt, or gravel-grading setting changed.

## Diagnostics and deterministic fixtures

`/dune terrain inspect` reports wall orientation, ownership exclusions, signed structural
distance, outward normal, both pass decisions, and pre/post-clean tops from the production
evaluator.

Build-blocking fixtures cover first- and second-pass inner columns, a >10-block-tall column,
the retained wall behind four blocks, a buttress outside the band, fault and non-massif
intersections, outer-wall direction, disabled profiles, and cache/query-order independence.
Fixed Seed-0 production probes include:

| X/Z | Expected result |
|---:|---|
| `3057/150` | inner wall, pass 1, top Y127 removed whole |
| `3059/150` | inner wall, pass 2, top Y127 removed whole |
| `3060/150` | inner main wall retained at Y178 |
| `3050/190` | fault-owned connected toe retained at Y70 |
| `4098/0` | outer wall, pass 1, top Y171 removed whole |
| `4096/0` | outer wall, pass 2, top Y171 removed whole |
| `4095/0` | outer main wall retained at Y171 |

Use a new Arrakis Dev Seed-0 world or regenerate closed-world region files for visual testing;
existing chunks are never rewritten.
