package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.MinecraftDune;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisTerrainCommand;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Merges the geology branch into the canonical /dune command root.
 *
 * <p>0.5.8 attempted to look up the already-created root and mutate it. In actual native
 * world testing that branch was not visible. Registering another literal builder with the
 * same root name lets Brigadier merge the child tree into the existing /dune node. LOWEST
 * priority keeps the main dune prototype registration first, and the /minecraftdune redirect
 * therefore continues to point at the merged node.</p>
 */
@EventBusSubscriber(modid = MinecraftDune.MOD_ID)
public final class MacroGeologyCommandRegistration {
    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    private MacroGeologyCommandRegistration() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("dune")
                        .requires(source -> source.hasPermission(REQUIRED_PERMISSION_LEVEL))
                        .then(MacroGeologyCommand.build())
                        .then(ArrakisTerrainCommand.build())
        );
    }
}
