package com.blackenter.minecraftdune.client.dev;

import com.blackenter.minecraftdune.MinecraftDune;
import com.blackenter.minecraftdune.worldgen.arrakis.ArrakisRuntimeValidation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Explicitly enabled only by runTerrainValidationClient; never runs in normal worlds. */
@EventBusSubscriber(modid = MinecraftDune.MOD_ID, value = Dist.CLIENT)
public final class ArrakisValidationClient {
    private static final String PHASE = System.getProperty("minecraftdune.validationPhase", "");
    private static final String FOLDER = System.getProperty("minecraftdune.validationFolder", "");
    private static final long STARTED = System.nanoTime();
    private static final Camera[] CAMERAS = {
            new Camera("inner-wall", 2990.5, 210, 240.5, -129, 23, 3060, 145, 150),
            new Camera("cliff-foot", 3030.5, 95, 180.5, -135, -12, 3060, 105, 150),
            new Camera("shoulder", 3068.5, 250, 200.5, -147, 32, 3100, 200, 150),
            new Camera("outer-wall", 4200.5, 175, 80.5, 128, 18, 4096, 100, 0)
    };
    private static CompletableFuture<Void> checking;
    private static int cameraIndex = -1, settleTicks, stableTicks;
    private static boolean finished, readyToCapture;

    private ArrakisValidationClient() {}

    private static Path output() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("validation").resolve(FOLDER);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (PHASE.isEmpty() || finished) return;
        var minecraft = Minecraft.getInstance();
        try {
            if (!PHASE.equals("generate") && !PHASE.equals("reload")) throw new IllegalArgumentException("Invalid validation phase");
            if (!FOLDER.matches("Arrakis-validation_[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Invalid validation save folder");
            minecraft.options.pauseOnLostFocus = false;
            if ((System.nanoTime() - STARTED) / 1e9 > 600) throw new IllegalStateException("Runtime validation timed out");
            // The only dialog automatically accepted belongs to this disposable validation
            // save. Never bypass version downgrades, recovery, datapack errors or low disk warnings.
            if (PHASE.equals("reload") && minecraft.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen screen
                    && screen.getTitle().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents title
                    && title.getKey().equals("selectWorld.backupQuestion.experimental")) {
                for (var child : screen.children()) {
                    if (child instanceof net.minecraft.client.gui.components.Button button
                            && button.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents label
                            && label.getKey().equals("selectWorld.backupJoinSkipButton")) {
                        button.onPress();
                        return;
                    }
                }
            }
            var server = minecraft.getSingleplayerServer();
            if (minecraft.level == null || minecraft.player == null || server == null) return;
            if (checking == null) {
                minecraft.options.renderDistance().set(12);
                minecraft.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                minecraft.options.hideGui = true;
                minecraft.setScreen(null);
                Files.createDirectories(output());
                var manifest = new com.google.gson.JsonObject();
                manifest.addProperty("mod_version", net.neoforged.fml.ModList.get()
                        .getModContainerById(MinecraftDune.MOD_ID).orElseThrow().getModInfo().getVersion().toString());
                manifest.addProperty("render_distance", 12);
                manifest.addProperty("clouds", "off");
                manifest.addProperty("day_time", 6000);
                manifest.add("cameras", new com.google.gson.Gson().toJsonTree(CAMERAS));
                manifest.add("loaded_mods", new com.google.gson.Gson().toJsonTree(net.neoforged.fml.ModList.get()
                        .getMods().stream().map(mod -> mod.getModId() + "@" + mod.getVersion()).toList()));
                Files.writeString(output().resolve(PHASE + "-capture-manifest.json"),
                        new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(manifest));
                checking = server.submit(() -> {
                    try { ArrakisRuntimeValidation.run(server, output(), PHASE.equals("reload"), FOLDER); }
                    catch (Exception exception) { throw new RuntimeException(exception); }
                });
                return;
            }
            if (!checking.isDone()) return;
            checking.join();
            if (PHASE.equals("reload")) { finish(); return; }
            if (cameraIndex < 0) { nextCamera(); return; }
            if (readyToCapture) return;
            var camera = CAMERAS[cameraIndex];
            if (++settleTicks > 2400) throw new IllegalStateException("Camera did not load: " + camera.name);
            if (minecraft.player.distanceToSqr(camera.x, camera.y, camera.z) > 1) return;
            minecraft.player.setYRot(camera.yaw);
            minecraft.player.setXRot(camera.pitch);
            boolean loaded = true;
            int cx = ((int) Math.floor(camera.x)) >> 4, cz = ((int) Math.floor(camera.z)) >> 4;
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
                if (!minecraft.level.hasChunk(cx + dx, cz + dz)) loaded = false;
            }
            if (loaded && minecraft.levelRenderer.hasRenderedAllSections()
                    && minecraft.levelRenderer.isSectionCompiled(new BlockPos(camera.targetX, camera.targetY, camera.targetZ))) {
                stableTicks++;
            } else stableTicks = 0;
            if (settleTicks >= 240 && stableTicks >= 40) readyToCapture = true;
        } catch (Exception exception) { fail(exception); }
    }

    private static void nextCamera() {
        if (++cameraIndex >= CAMERAS.length) { finish(); return; }
        settleTicks = 0;
        stableTicks = 0;
        var minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        var camera = CAMERAS[cameraIndex];
        var playerId = minecraft.player.getUUID();
        server.execute(() -> {
            var level = server.overworld();
            level.setDayTime(6000);
            level.setWeatherParameters(6000, 0, false, false);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_SPECTATORSGENERATECHUNKS).set(true, server);
            var player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.setGameMode(GameType.SPECTATOR);
                player.teleportTo(level, camera.x, camera.y, camera.z, camera.yaw, camera.pitch);
            }
        });
    }

    @SubscribeEvent
    public static void rendered(RenderFrameEvent.Post event) {
        if (!readyToCapture || finished) return;
        readyToCapture = false;
        try (var screenshot = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            screenshot.writeToFile(output().resolve(CAMERAS[cameraIndex].name + ".png"));
            nextCamera();
        } catch (Exception exception) { fail(exception); }
    }

    private static void finish() {
        try {
            Files.writeString(output().resolve(PHASE + ".passed"), "Runtime validation passed; screenshots require visual review.\n");
            finished = true;
            Minecraft.getInstance().stop();
        } catch (Exception exception) { fail(exception); }
    }

    private static void fail(Exception exception) {
        finished = true;
        MinecraftDune.LOGGER.error("Arrakis runtime validation failed", exception);
        try {
            Files.createDirectories(output());
            Files.writeString(output().resolve(PHASE + ".failed"), exception.toString());
        } catch (Exception writeFailure) { MinecraftDune.LOGGER.error("Cannot write validation failure", writeFailure); }
        Minecraft.getInstance().stop();
    }

    private record Camera(String name, double x, double y, double z, float yaw, float pitch,
            int targetX, int targetY, int targetZ) {}
}
