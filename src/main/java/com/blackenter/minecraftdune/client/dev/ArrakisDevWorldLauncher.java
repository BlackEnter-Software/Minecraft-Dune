package com.blackenter.minecraftdune.client.dev;

import com.blackenter.minecraftdune.MinecraftDune;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = MinecraftDune.MOD_ID, value = Dist.CLIENT)
public final class ArrakisDevWorldLauncher {
    private static final String WORLD_NAME_PROPERTY = "minecraftdune.devWorldName";
    private static final String WORLD_SEED_PROPERTY = "minecraftdune.devWorldSeed";

    private static final ResourceKey<WorldPreset> ARRAKIS_DEV_PRESET = ResourceKey.create(
            Registries.WORLD_PRESET,
            ResourceLocation.fromNamespaceAndPath(MinecraftDune.MOD_ID, "arrakis_dev")
    );

    private static LaunchState state = LaunchState.NOT_STARTED;

    private ArrakisDevWorldLauncher() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        String worldName = System.getProperty(WORLD_NAME_PROPERTY);
        if (worldName == null || worldName.isBlank() || state == LaunchState.DONE) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            state = LaunchState.DONE;
            return;
        }

        if (state == LaunchState.NOT_STARTED) {
            MinecraftDune.LOGGER.info("Preparing Arrakis dev world '{}'", worldName);
            CreateWorldScreen.openFresh(minecraft, minecraft.screen);
            state = LaunchState.WAITING_FOR_CREATE_SCREEN;
            return;
        }

        if (state != LaunchState.WAITING_FOR_CREATE_SCREEN
                || !(minecraft.screen instanceof CreateWorldScreen screen)) {
            return;
        }

        try {
            configureAndCreate(screen, worldName);
            state = LaunchState.DONE;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            state = LaunchState.DONE;
            MinecraftDune.LOGGER.error(
                    "Failed to create Arrakis dev world '{}'",
                    worldName,
                    exception
            );
        }
    }

    private static void configureAndCreate(
            CreateWorldScreen screen,
            String worldName
    ) throws ReflectiveOperationException {
        WorldCreationUiState uiState = screen.getUiState();
        String seed = System.getProperty(WORLD_SEED_PROPERTY, "0");

        WorldCreationUiState.WorldTypeEntry worldType = Stream.concat(
                        uiState.getNormalPresetList().stream(),
                        uiState.getAltPresetList().stream()
                )
                .filter(entry -> entry.preset() != null && entry.preset().is(ARRAKIS_DEV_PRESET))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Arrakis Dev world preset is not available"
                ));

        uiState.setName(worldName);
        uiState.setWorldType(worldType);
        uiState.setSeed(seed);
        uiState.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        uiState.setAllowCommands(true);
        uiState.setBonusChest(false);

        MinecraftDune.LOGGER.info(
                "Creating Arrakis dev world '{}' with seed {} and preset {}",
                worldName,
                seed,
                ARRAKIS_DEV_PRESET.location()
        );

        Method onCreate = CreateWorldScreen.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);

        try {
            onCreate.invoke(screen);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new ReflectiveOperationException(cause);
        }
    }

    private enum LaunchState {
        NOT_STARTED,
        WAITING_FOR_CREATE_SCREEN,
        DONE
    }
}
