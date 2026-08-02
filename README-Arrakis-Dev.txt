Minecraft: Dune — Arrakis Dev World Patch
===============================================

Purpose
-------
Adds a selectable "Arrakis Dev" world type to the Minecraft 1.21.1 NeoForge project.

Overworld layers
----------------
Y  65 and above : Air
Y  55 to 64     : Sand (10 blocks)
Y  45 to 54     : Sandstone (10 blocks)
Y   0 to 44     : Stone (45 blocks)
Y -63 to -1     : Deepslate (63 blocks)
Y -64           : Bedrock (1 block)

The surface block is at Y=64, one block above vanilla sea level Y=63.

Generation settings
-------------------
Biome: minecraft:desert
Biome features: disabled
Lakes: disabled
Structures: disabled
Caves: none, because this is a flat generator
Nether and End: normal vanilla generation

Installation
------------
1. Copy apply-arrakis-dev-world.ps1 into the root of the Minecraft-Dune project.
2. Open PowerShell in that directory.
3. Run:

   powershell -ExecutionPolicy Bypass -File .\apply-arrakis-dev-world.ps1

   Or, from an already-open PowerShell prompt:

   .\apply-arrakis-dev-world.ps1

4. Run the client:

   .\gradlew runClient

5. Create a new world and cycle the World Type button until "Arrakis Dev" appears.

Notes
-----
- The script safely merges the custom preset into an existing normal world-preset tag.
- The script safely adds the English translation to an existing en_us.json.
- Re-running the script is safe and does not duplicate the preset tag.
- Existing worlds are not modified. Create a new world after applying the patch.
