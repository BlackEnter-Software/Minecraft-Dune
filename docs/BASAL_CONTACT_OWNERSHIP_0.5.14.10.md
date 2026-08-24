# Basal Contact Ownership & Wall-Relief Talus — 0.5.14.10

Seed-0 testing of 0.5.14.9 exposed two independent causes of the remaining basal artifact.

## 1. Contact ownership

The Shield-Wall height was already faded to zero at its physical low-side contact, but the
final macro height was still the maximum of several overlapping terrain systems:

```text
massif
foreland
broken rock
outer transition
```

At the inner Shield-Wall contact, foreland terrain can still overlap the massif. At the outer
contact, broken-rock/transition terrain can do the same. Therefore `massifHeight == 0` did not
guarantee that final native-rock height was zero.

Profile 51410 introduces a narrow 8-block **contact-clearance band** around the warped physical
inner and outer Shield-Wall contacts. Inside that band, only the non-massif contribution fades
toward zero:

```text
finalHeight = max(
    massifHeight,
    nonMassifHeight * (1 - contactClearance)
)
```

The massif itself is unchanged. At the exact contact, non-massif ownership is zero; eight
blocks away the previous foreland/broken-rock/transition contribution is fully restored.

Profiles below 51410 retain the 0.5.14.9 height-composition path.

## 2. Talus source ownership

0.5.14.9 correctly switched apron placement to actual surviving pre-talus rock, but the
massif path still required the sampled contact rock to classify as `massifSource`. That could
reject the exact low basal shelf created by overlapping foreland/broken-rock fields.

For profile 51410, the warped physical scarp remains only a bounded search-eligibility gate.
Once inside that band, **actual surviving Y=65 rock is authoritative regardless of which
overlapping macro field supplied it**.

Profiles at 5149 retain the old `massifSource` requirement.

## 3. Fault wall relief

The nearest Y=65 rock beside a fault core is often a shallow structural toe. 0.5.14.9 used
that one contact column's top Y to decide whether the wall had enough relief for talus. A
3–6-block toe could therefore suppress colluvium even when an 80–150-block fault wall stood
immediately behind it.

Profile 51410 keeps the same actual contact position, then probes approximately 20 blocks
farther **through the contact into the wall**. The maximum surviving pre-talus rock top found
along that direction supplies the relief gate.

The opposite-wall distance cap remains unchanged, so two fault-wall aprons cannot bridge the
protected central sandy core.

## Scope

Unchanged:
- `BASE_SURFACE_Y = 64`;
- `massif_vertical_offset = -4`;
- `faults.rocky_floor_height = 0`;
- 0.5.14.6 orphan-remnant filtering;
- erosion strengths and scarp widths;
- current talus dimensions/material grading;
- dune generation.

This is still gravity-driven colluvium. Aeolian deposition remains reserved for approximately
0.5.16.
