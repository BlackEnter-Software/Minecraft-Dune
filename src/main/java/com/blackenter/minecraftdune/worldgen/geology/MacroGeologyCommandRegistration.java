package com.blackenter.minecraftdune.worldgen.geology;

import com.blackenter.minecraftdune.MinecraftDune;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Attaches macro-geology development commands to the existing /dune command tree.
 *
 * <p>Registration runs at LOWEST priority so the 0.5.6 dune command can remain untouched.
 * The child-exists check also makes this compatible with a checkout where an earlier
 * experimental 0.5.7 patch already attached the geology branch directly.</p>
 */
@EventBusSubscriber(modid = MinecraftDune.MOD_ID)
public final class MacroGeologyCommandRegistration {
    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    private MacroGeologyCommandRegistration() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandNode<CommandSourceStack> duneRoot = dispatcher.getRoot().getChild("dune");

        if (duneRoot == null) {
            LiteralCommandNode<CommandSourceStack> fallbackDuneRoot = dispatcher.register(
                    Commands.literal("dune")
                            .requires(source -> source.hasPermission(REQUIRED_PERMISSION_LEVEL))
                            .then(MacroGeologyCommand.build())
            );

            if (dispatcher.getRoot().getChild("minecraftdune") == null) {
                dispatcher.register(
                        Commands.literal("minecraftdune")
                                .requires(source -> source.hasPermission(REQUIRED_PERMISSION_LEVEL))
                                .redirect(fallbackDuneRoot)
                );
            }
            return;
        }

        if (duneRoot.getChild("geology") == null) {
            duneRoot.addChild(MacroGeologyCommand.build().build());
        }
    }
}
