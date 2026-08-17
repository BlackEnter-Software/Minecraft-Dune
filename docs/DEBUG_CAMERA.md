# Minecraft: Dune debug camera and screenshot commands

Version 0.5.2 adds client-side camera presets for repeatable terrain screenshots.
The preferred mod command root is `/dune`. The old `/minecraftdune` root remains as a
server-side compatibility alias for the existing dune laboratory commands.

Camera presets store the exact dimension, player position, yaw, and pitch in:

```text
config/minecraftdune/debug-cameras.json
```

The file is local to the Minecraft client and is reused between development sessions.

## Camera commands

Show the current camera transform:

```mcfunction
/dune camera info
```

Save the current position and view direction:

```mcfunction
/dune camera save A
/dune camera save B
/dune camera save C
```

List saved cameras:

```mcfunction
/dune camera list
```

Return to a saved camera:

```mcfunction
/dune camera goto A
```

Delete a saved camera:

```mcfunction
/dune camera delete A
```

Teleport directly to an exact position, yaw, and pitch in the current dimension:

```mcfunction
/dune camera tp <x> <y> <z> <yaw> <pitch>
```

Example:

```mcfunction
/dune camera tp 1200.5 190 -850.5 -135 12
```

The teleport itself is sent to the server, so these commands require operator/cheat permission
level 2 just like the existing dune laboratory commands.

## Screenshots

Take one named screenshot on the next fully rendered frame:

```mcfunction
/dune screenshot transverse_spacing400
```

The file is written to the normal Minecraft `screenshots` directory as:

```text
dune_transverse_spacing400.png
```

If that filename already exists, `_2`, `_3`, and so on are appended rather than overwriting it.

## Batch screenshots

Save all desired viewpoints once, then capture every saved camera in alphabetical camera-name
order:

```mcfunction
/dune screenshot batch transverse_spacing400
```

By default, the runner waits 40 client ticks after arriving at each camera before capturing the
frame. A different settle period can be specified from 0 to 200 ticks:

```mcfunction
/dune screenshot batch transverse_spacing400 60
```

Example files:

```text
dune_transverse_spacing400_A.png
dune_transverse_spacing400_B.png
dune_transverse_spacing400_C.png
```

Cancel an active batch with:

```mcfunction
/dune screenshot batch cancel
```

During a batch, the debug runner:

1. sends an authoritative cross-dimension teleport to the saved camera;
2. waits until the client has actually reached the requested dimension and position;
3. holds the exact saved position, yaw, and pitch during the settle period and immediately
   before each rendered frame, including the previous-frame transform used for interpolation;
4. temporarily hides the HUD;
5. captures the rendered framebuffer with an explicit filename;
6. advances to the next camera;
7. restores the previous HUD visibility setting after completion, cancellation, or timeout.

The runner times out a camera if its teleport has not completed within 200 ticks.

## Recommended dune comparison workflow

Create stable viewpoints once:

```mcfunction
/dune camera save A
/dune camera save B
/dune camera save C
/dune camera save D
```

Generate one terrain profile and capture it:

```mcfunction
/dune dunes settings dune_spacing 200
/dune dunes generate transverse
/dune screenshot batch spacing200
```

Change only the parameter under test, regenerate the same deterministic region, and run the same
camera set again:

```mcfunction
/dune dunes settings dune_spacing 400
/dune dunes generate transverse
/dune screenshot batch spacing400
```

Keep FOV, perspective, render distance, resource packs/shaders, time, and weather unchanged when
they matter to the comparison. The batch runner currently normalizes camera transform and HUD
visibility; it deliberately does not modify the rest of the user's graphics or world settings.
