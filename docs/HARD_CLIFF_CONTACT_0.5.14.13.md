# 0.5.14.13 — Hard Cliff Contact

0.5.14.12 proved that the remaining Shield-Wall base was not only an erosion-order issue.

The structural massif itself still contained a smooth physical scarp transition. Even when
the final low-height cutoff worked, any column at or above the 10-block threshold remained a
fully rooted native-rock column. Because the old contact was defined at the **zero-height**
edge of the scarp, the rest of the 36-block inner transition / 48-block outer transition could
still appear as a broad basal terrace.

0.5.14.13 changes the meaning of the active Shield-Wall contact for new profile 51413:

```text
0.5.14.12 structural transition

desert                                  massif
0  2  5  10  18  30  50  ...  full wall
|--------------------------------------|
^ old low-side contact

0.5.14.13

desert                        HARD CLIFF
                              |
0  0  0  0  0  0  0          | full wall...
|-----------------------------|
      removed structural ramp
```

For the inner Shield Wall, the hard contact is at:

```text
start_radius + warped_offset + inner_scarp_width
```

For the outer Shield Wall it is at:

```text
outer_start_radius + warped_offset
```

These are the high-rock ends of the existing physical scarp transitions.

The whole desert-side structural ramp is removed, regardless of whether an individual column
is 3, 10, or 30 blocks high. The existing 10-block final cliff-foot test remains useful on the
rock side of the new contact for eroded low remnants.

The cull is bounded to one configured structural scarp width, so unrelated foreland and
broken-rock formations farther from the Shield Wall remain legal.

The same hard contact is used for:
- non-massif contact ownership clearance;
- macro structural cleanup;
- the authoritative post-erosion/pre-talus final cull;
- massif talus search gating and inward wall-relief direction.

Regional faults keep their existing 0.5.14.12 geometry and are excluded from the Shield-Wall
final cull.

No new JSON controls are introduced. Existing `minimum_cliff_foot_height=10`,
`cliff_foot_cut_width=16`, `massif_vertical_offset=-4`, talus dimensions, erosion and dune
settings remain unchanged.

Profiles below 51413 retain the previous contact semantics.
