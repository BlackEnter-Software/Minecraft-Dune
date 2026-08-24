# Final Cliff Foot & Contact Talus — 0.5.14.12

## Confirmed ordering failure

The 0.5.14.11 cutoff ran in `MacroGeologyField`, before the generator created fractures and
applied major erosion, surface erosion and orphan-remnant filtering. It therefore could not
classify the rock height that would actually be written:

```text
macro height 15 -> passes minimum 10
erosion        -> final height 6
old result     -> six-block native-rock skirt survives
```

## Authoritative final footprint

Profile 51412 keeps the earlier macro cleanup as a preliminary optimization and builds one
cached `finalPreTalusRockTopY` model in `ArrakisChunkGenerator` around the shared
`FinalCliffFootField` resolver:

```text
macro composition and preliminary cleanup
  -> fault carving
  -> fractures
  -> major erosion
  -> surface erosion
  -> orphan-remnant filtering
  -> highest filtered surviving rock
  -> final Shield-Wall cliff-foot cull
  -> local scree / unified wall probe / basal talus
  -> dunes
```

The final top is cached only from seed, serialized settings and absolute coordinates. A result
of Y64 means the complete native-rock column is absent, including the normally foundation-rooted
lithology below the visible surface. `getBaseHeight`, `getBaseColumn`, `fillFromNoise`, talus
contact sampling and material support all use the same resolved value.

The cull remains limited to the warped physical Shield-Wall contact and retains the existing
10-block minimum and 16-block width. Regional-fault carve geometry is excluded.

## Contact and material changes

Basal contact occupancy now searches Y65-Y76 with the active settings. This catches low eroded
or undercut wall rock without allowing arbitrary high floating blocks to become talus sources.
Every contact and inward wall-probe sample reads the post-cull analytical footprint.

Horizontal placement is unchanged: the empty cell immediately adjacent to rock has
`outwardDistance=0`. Material selection instead changes to an 80% outward-distance and 20%
inverse-height blend. Gravel therefore remains visually distinct at the cliff while sand grows
toward the distal, lower toe. This remains gravity-driven colluvium, not aeolian deposition.

## Compatibility

The final cull, low-wall interval and contact-coarse grading require profile 51412. Serialized
profiles at 51411 or below retain the previous post-erosion occupancy, exact-Y65 contact test
and material formula. No JSON fields or terrain dimensions were added or retuned.
