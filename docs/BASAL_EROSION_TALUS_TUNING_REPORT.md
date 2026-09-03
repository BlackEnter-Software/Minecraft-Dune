# 0.5.14.8.1 - Remnants: basal erosion and organic talus tuning

## Scope and compatibility

Started from clean `main` at `47874b96701afbf0be1cf5e6142b79f4a7fecf16`.
Work is on `tuning/0.5.14.8-basal-erosion-talus`. After the tuning and support-depth follow-up,
the user authorized committing and pushing this branch as **0.5.14.8.1 - Remnants**.
The user approved Y60 as the new basal erosion limit, with the desert datum still Y64.
The mod version is `0.5.14.8.1`; terrain profile `5148` is unchanged. New optional serialized settings opt in
to the changes; missing settings reproduce the previous hardening profile exactly.

No changes to MacroGeologyField, RockFaceExposure, lithology, fractures, dune behavior,
fault centerlines, actual-contact searches or wall-relief probes. No slab/hard-cliff
conversion, nominal-radius deposit placement or destructive edits to existing worlds.

## Remove the shelf before deposits

`erosion.surface.basal_erosion_depth=4` moves the two existing basal erosion fade limits
from Y64..66 to Y60..62. Y60 remains protected; Y61 is transitional, with full strength
from Y62. Only raw occupancy at Y61..65 changes. Noise coordinates, erosion strengths and
raw erosion at/above Y66 are unchanged. This is not a blanket deletion of all low rock:
rock that still survives the existing erosion equations remains.

The native writer already tests shared filtered occupancy above the existing hard
foundation. Newly omitted native root material leaves the normal sand/substrate below
Y65, not an air trench. The Y65 residue skirt remains available for genuine single-layer
erosion residue, but is no longer needed at several formerly exposed shelf locations.

## Remnant reach

The existing inward raw-rock support chain extends **8 → 10 → 12 blocks**. The final
two-block extension follows the user's visual review of the tuned inner/outer walls and
ravine. That follow-up changes only the preset support depth: component radius remains 5,
erosion stays at Y60, and talus shape/noise/sand-skirt settings are untouched. Its contiguous
lateral support search stays **2**; increasing that instead could rescue more remnants.
The post-orphan component window extends **radius 3 → 5**, while the removable component
limit stays **four columns**. This now resolves a closed four-column linear component
which previously hit the radius-three conservative boundary. Tall disconnected components
remain removable. Larger bodies, ribs/ledges connected at any height, protected fault
connections and ambiguous boundary cases remain protected. No new cleanup stage is added.

## Smaller, curved and varied deposits

All distances are measured from the existing actual final pre-talus contact. The wall
detection band stays Y71..76, independent of the reduced deposit height.

The preset changes apron maximum height **6 → 4**, base spread **12 → 10**, and sand-start
fraction **0.62 → 0.80**. Smooth coordinate/seed-based value-noise patches modulate:

- height by 0.55–1.00 (48-block broad + 12-block detail noise, weighted 65%/35%);
- spread by 0.85–1.15 (36-block patches), yielding 8.5–11.5 nominal blocks;
- skirt reach over 16–20 blocks using that same width field, previously fixed at 24;
- coarse/sand transition threshold by ±0.08 using the broad field.

For outward distance `d` and local spread `w`, height scales with `(1-d/w)^1.8`.
This is steepest near contact and gentler toward the toe. It also uses the existing
wall-relief qualification and a smooth inward reduction (minimum 0.45 within the four-block
inset). Height is rounded to nearest block; traces below 0.45 disappear beyond the first
outward block rather than producing an endless one-block rail. Minecraft's block grid
still produces steps; the visible toe is generally shorter than the nominal spread.

For local skirt reach `r`, burial depth is `ceil(4*(1-t*t*(3-2*t)))`,
where `t=max(0,-signedContactDistance)/r`. It is four layers at contact/inward, two halfway
out, and none at/beyond `r`. Inward overlap remains four blocks. The visible Y65 mantle
still requires proven one-layer erosion residue; it never covers a valid cliff or merely
adds a raised layer over empty desert. Final cliff rock and local scree take precedence;
basal gravel/colluvium takes precedence over the sand skirt.

Full fault cores and protected sand passes reject both deposits and component cleanup.
Ravine contact rays still stop at core barriers: opposing aprons cannot bridge the channel.

## Analytical observations (Seed 0)

These are evaluator results, not claims of in-game visual acceptance:

| X / Z | Final pre-deposit rock top, before → after | Native Y64 root, after | Apron height, before → after |
| --- | --- | --- | --- |
| 3001 / 464 | 65 → 64 | absent | 6 → 3 |
| 2991 / 464 | 65 → 64 | absent | 1 → 0 |
| 3053 / 190 | 65 → 64 | absent | 0 → 0 |
| 3050 / 254 | 65 → 64 | absent | 4 → 1 |
| 3200 / 200 (ravine) | 65 → 64 | absent | 6 → 2 |
| 4086 / 0 (outer wall) | 171 → 171 | retained | 6 → 3 |

The originally one-layer native foreland at 2988/464 is retained, as is the supported
fault-edge toe at 3050/70/190. The latter is connected, not a closed small component.
This pass deliberately does not delete every low rock feature or excavate the whole mountain.

## Diagnostics and validation

`/dune terrain inspect` now reports erosion floor/depth, native-root presence at Y64,
final rock at Y65, organic-apron opt-in, configured component radius and local skirt reach.
The offline `diagnoseArrakisContact --args=--basal-tuning` mode compares fixed known points.

Validation retains the previous raw-erosion fingerprint and four historical production
fingerprints using an explicit pre-tuning serialized-profile fixture. New active-profile
checks cover removal-only Y61..65 changes, identical upper raw walls and upstream fields,
the observed shelf/root removal, longer support reach, bounded component retention/removal,
noise bounds/continuity/seed variation, curved slope, skirt taper, gravel precedence,
both ravine sides, open Y64 fault core and cache/query/chunk-order independence.

`./gradlew.bat clean build` passed, including terrain/profile/prototype and active tuning
validation. The fixed multi-seed probes found 56 additional raw basal voxels removed,
with no raw-erosion changes outside Y61..65. `git diff --check` passed. Git may print its
normal LF-to-CRLF conversion warnings on this Windows checkout; these are not whitespace
errors. No visual acceptance has been claimed from the analytical checks.

## Next visual check

Create a **new Seed-0 Arrakis Dev world**. Check 3001/464, 3043/200, 3050/254 and 2963/615;
also check 4086/0 on the outer wall, the retained toe at 3050/190, and both ravine sides
around 3200/200 and 3204/125 with the open central channel at 3200/180..190.
No in-game testing was performed during this pass.
