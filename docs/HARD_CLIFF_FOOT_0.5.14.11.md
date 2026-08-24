# Hard Cliff Foot & Unified Wall Talus — 0.5.14.11

## Root cause

The Shield Wall is primarily a smooth height field. Even after 0.5.14.10 assigned overlapping
foreland, broken-rock and transition terrain away from the basal contact, the final composition
could still contain a small positive massif height:

```text
finalCandidateHeight = max(massifHeight, clearedNonMassifHeight)
```

A 1-9-block candidate is visually much heavier than its surface relief suggests because native
rock replaces the soft flat Arrakis layers down to the hard foundation. The result was a broad,
continuous rock skirt rather than an abrupt desert escarpment.

## Generation order

Profile 51411 applies a hard cliff-foot cutoff at the final macro composition boundary:

```text
macro province fields
  -> massif and overlapping non-massif height fields
  -> 0.5.14.10 contact-ownership clearance
  -> final candidate native-rock height
  -> 0.5.14.11 hard cliff-foot cutoff
  -> sand-corridor and regional-fault carving
  -> fractures
  -> major and surface erosion
  -> orphan-remnant filtering
  -> final surviving pre-talus rock
  -> gravity-driven talus
  -> dunes
```

The active values are a strict minimum height of 10 blocks and a 16-block cut width around the
existing warped physical inner/outer Shield-Wall contacts. A positive candidate below 10 is
changed to zero together with its rock mask. Taller cliffs remain untouched, as do low rock
formations outside that contact band.

## Unified wall relief and actual placement

The actual final surviving rock footprint remains authoritative for talus placement. The first
empty low-side cell touching rock has zero outward distance, so no intentional one-block gap is
introduced.

Only the relief measurement changes. Both massif and regional-fault contacts now use one helper
that samples deterministic surviving pre-talus columns 16-24 blocks inward and selects their
maximum top Y. Massif probes follow the inward normal of the warped physical scarp; fault probes
keep the actual contact direction from 0.5.14.10. A shallow toe can therefore inherit the relief
of a tall wall immediately behind it without moving the contact or using talus to conceal an
incorrect rock footprint.

The opposing-apron distance cap, absolute Y=64 fault floor, protected central sandy channel,
talus dimensions and gravel-to-sand grading are unchanged.

## Serialized compatibility

`minimum_cliff_foot_height` and `cliff_foot_cut_width` live in `base_alignment`, whose codec
defaults both fields to zero. Missing keys therefore disable the cutoff. The behavior is also
gated at profile 51411, while prior fault/talus branches retain their existing version gates.
