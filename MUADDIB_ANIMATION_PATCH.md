# Muad'dib model and animation patch

This patch updates the standalone NeoForge 1.21.1 project with the extensively
edited kangaroo-mouse model supplied in `muaddib_mouse.bbmodel`.

## Replaced

- Runtime Java geometry
- Entity texture
- Editable Blockbench source
- Entity animation state handling

## Added

- `MuaddibMouseAnimations.java`
- A looping coordinated hop animation
- A subtle looping idle/balance animation
- Random playback of the supplied `wiggle_head` action
- Random playback of the supplied `sniff_ground` action

## Runtime behavior

The hop is driven through `animateWalk`, so it follows actual entity movement.
Both hind legs extend together, the body rises and pitches, the front legs tuck,
and the three-part tail counterbalances the jump.

The two authored one-shot actions play only while the mouse is stationary and
on the ground. They are client-side visual actions and require no network packet.

## Apply

Extract this patch archive into the project root and allow replacement of files.
Then run:

```powershell
.\gradlew.bat clean build
```

For live testing:

```powershell
.\gradlew.bat runClient
```
