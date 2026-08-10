package com.blackenter.minecraftdune.client.debug;

import com.blackenter.minecraftdune.MinecraftDune;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Client-side fixed-camera and screenshot tools for repeatable terrain comparisons.
 */
@EventBusSubscriber(modid = MinecraftDune.MOD_ID, value = Dist.CLIENT)
public final class DebugCameraClientCommands {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CAMERA_FILE = FMLPaths.CONFIGDIR.get()
            .resolve(MinecraftDune.MOD_ID)
            .resolve("debug-cameras.json");
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Comparator<String> CAMERA_NAME_ORDER = String.CASE_INSENSITIVE_ORDER
            .thenComparing(Comparator.naturalOrder());
    private static final int DEFAULT_SETTLE_TICKS = 40;
    private static final int MAXIMUM_SETTLE_TICKS = 1200;
    private static final int MAXIMUM_TELEPORT_WAIT_TICKS = 600;
    private static final double ARRIVAL_DISTANCE_SQUARED = 0.25 * 0.25;

    private static final Map<String, CameraPreset> CAMERAS = new LinkedHashMap<>();
    private static final Set<String> RESERVED_SCREENSHOT_NAMES = new HashSet<>();
    private static BatchRun activeBatch;

    private DebugCameraClientCommands() {
    }

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        loadCameras();
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dune")
                        .then(Commands.literal("camera")
                                .then(Commands.literal("info")
                                        .executes(DebugCameraClientCommands::cameraInfo))
                                .then(Commands.literal("list")
                                        .executes(DebugCameraClientCommands::listCameras))
                                .then(Commands.literal("save")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(DebugCameraClientCommands::saveCamera)))
                                .then(Commands.literal("goto")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(DebugCameraClientCommands::goToCamera)))
                                .then(Commands.literal("delete")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(DebugCameraClientCommands::deleteCamera)))
                                .then(Commands.literal("tp")
                                        .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                                .then(Commands.argument("yaw", FloatArgumentType.floatArg())
                                                                        .then(Commands.argument(
                                                                                        "pitch",
                                                                                        FloatArgumentType.floatArg(-90.0F, 90.0F)
                                                                                )
                                                                                .executes(DebugCameraClientCommands::teleportCamera))))))))
                        .then(Commands.literal("screenshot")
                                .then(Commands.literal("batch")
                                        .then(Commands.literal("cancel")
                                                .executes(DebugCameraClientCommands::cancelBatchCommand))
                                        .then(Commands.argument("label", StringArgumentType.word())
                                                .executes(context -> startBatch(context, DEFAULT_SETTLE_TICKS))
                                                .then(Commands.argument(
                                                                "settle_ticks",
                                                                IntegerArgumentType.integer(0, MAXIMUM_SETTLE_TICKS)
                                                        )
                                                        .executes(context -> startBatch(
                                                                context,
                                                                IntegerArgumentType.getInteger(context, "settle_ticks")
                                                        )))))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(DebugCameraClientCommands::takeNamedScreenshot)))
        );

        CommandNode<CommandSourceStack> duneRoot = dispatcher.getRoot().getChild("dune");
        if (duneRoot != null) {
            dispatcher.register(Commands.literal("minecraftdune").redirect(duneRoot));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        BatchRun batch = activeBatch;
        if (batch == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.getConnection() == null) {
            stopBatch(Component.literal("Screenshot batch stopped because the client disconnected."));
            return;
        }

        NamedCamera target = batch.currentCamera();
        switch (batch.phase) {
            case WAITING_FOR_TELEPORT -> {
                batch.phaseTicks++;
                if (hasArrived(player, target.preset())) {
                    lockCamera(player, target.preset());
                    batch.remainingSettleTicks = batch.settleTicks;
                    batch.phase = batch.remainingSettleTicks == 0
                            ? BatchPhase.READY_TO_RENDER
                            : BatchPhase.SETTLING;
                } else if (batch.phaseTicks > MAXIMUM_TELEPORT_WAIT_TICKS) {
                    stopBatch(Component.literal(
                            "Screenshot batch timed out while travelling to camera " + target.name() + "."
                    ));
                }
            }
            case SETTLING -> {
                if (!sameDimension(player, target.preset())) {
                    stopBatch(Component.literal(
                            "Screenshot batch left camera dimension while settling " + target.name() + "."
                    ));
                    return;
                }
                lockCamera(player, target.preset());
                if (--batch.remainingSettleTicks <= 0) {
                    batch.phase = BatchPhase.READY_TO_RENDER;
                }
            }
            case READY_TO_RENDER, CAPTURE_AFTER_RENDER -> lockCamera(player, target.preset());
        }
    }

    @SubscribeEvent
    public static void beforeRender(RenderFrameEvent.Pre event) {
        BatchRun batch = activeBatch;
        if (batch == null || batch.phase != BatchPhase.READY_TO_RENDER) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            stopBatch(Component.literal("Screenshot batch stopped because no player was available."));
            return;
        }

        lockCamera(player, batch.currentCamera().preset());
        batch.phase = BatchPhase.CAPTURE_AFTER_RENDER;
    }

    @SubscribeEvent
    public static void afterRender(RenderFrameEvent.Post event) {
        BatchRun batch = activeBatch;
        if (batch == null || batch.phase != BatchPhase.CAPTURE_AFTER_RENDER) {
            return;
        }

        NamedCamera camera = batch.currentCamera();
        captureScreenshot(batch.label + "_" + camera.name());
        message(Component.literal(
                "Captured " + (batch.cameraIndex + 1) + "/" + batch.cameras.size()
                        + ": " + camera.name() + "."
        ));

        batch.cameraIndex++;
        if (batch.cameraIndex >= batch.cameras.size()) {
            stopBatch(Component.literal(
                    "Screenshot batch " + batch.label + " completed: "
                            + batch.cameras.size() + " camera(s)."
            ));
            return;
        }

        beginCurrentTeleport(batch);
    }

    private static int cameraInfo(CommandContext<CommandSourceStack> context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            message(Component.literal("No local player is available."));
            return 0;
        }

        message(Component.literal(String.format(
                Locale.ROOT,
                "Camera: dimension=%s, x=%.6f, y=%.6f, z=%.6f, yaw=%.3f, pitch=%.3f.",
                player.level().dimension().location(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        )));
        return 1;
    }

    private static int listCameras(CommandContext<CommandSourceStack> context) {
        if (CAMERAS.isEmpty()) {
            message(Component.literal("No debug cameras are saved."));
            return 0;
        }

        List<String> names = sortedCameraNames();
        message(Component.literal("Saved debug cameras (" + names.size() + "): " + String.join(", ", names)));
        return names.size();
    }

    private static int saveCamera(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        if (!isSafeName(name, "Camera")) {
            return 0;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            message(Component.literal("No local player is available."));
            return 0;
        }

        CAMERAS.put(name, CameraPreset.from(player));
        if (!saveCameras()) {
            return 0;
        }
        message(Component.literal("Saved debug camera " + name + "."));
        return 1;
    }

    private static int goToCamera(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CameraPreset preset = CAMERAS.get(name);
        if (preset == null) {
            message(Component.literal("Unknown debug camera: " + name + "."));
            return 0;
        }

        cancelActiveBatchForManualTeleport();
        if (!requestTeleport(preset)) {
            return 0;
        }
        message(Component.literal("Travelling to debug camera " + name + "."));
        return 1;
    }

    private static int deleteCamera(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        if (CAMERAS.remove(name) == null) {
            message(Component.literal("Unknown debug camera: " + name + "."));
            return 0;
        }
        if (!saveCameras()) {
            return 0;
        }
        message(Component.literal("Deleted debug camera " + name + "."));
        return 1;
    }

    private static int teleportCamera(CommandContext<CommandSourceStack> context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            message(Component.literal("No local player is available."));
            return 0;
        }

        CameraPreset preset = new CameraPreset(
                player.level().dimension().location().toString(),
                DoubleArgumentType.getDouble(context, "x"),
                DoubleArgumentType.getDouble(context, "y"),
                DoubleArgumentType.getDouble(context, "z"),
                FloatArgumentType.getFloat(context, "yaw"),
                FloatArgumentType.getFloat(context, "pitch")
        );
        cancelActiveBatchForManualTeleport();
        return requestTeleport(preset) ? 1 : 0;
    }

    private static int takeNamedScreenshot(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        if (!isSafeName(name, "Screenshot")) {
            return 0;
        }
        captureScreenshot(name);
        return 1;
    }

    private static int startBatch(CommandContext<CommandSourceStack> context, int settleTicks) {
        String label = StringArgumentType.getString(context, "label");
        if (!isSafeName(label, "Batch label")) {
            return 0;
        }
        if (activeBatch != null) {
            message(Component.literal("A screenshot batch is already running. Cancel it first."));
            return 0;
        }
        if (CAMERAS.isEmpty()) {
            message(Component.literal("Save at least one debug camera before starting a batch."));
            return 0;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            message(Component.literal("A connected local player is required for a screenshot batch."));
            return 0;
        }

        List<NamedCamera> cameras = new ArrayList<>();
        for (String name : sortedCameraNames()) {
            cameras.add(new NamedCamera(name, CAMERAS.get(name)));
        }

        activeBatch = new BatchRun(cameras, label, settleTicks, minecraft.options.hideGui);
        minecraft.options.hideGui = true;
        message(Component.literal(
                "Started screenshot batch " + label + " with " + cameras.size()
                        + " camera(s) and " + settleTicks + " settle tick(s) per camera."
        ));
        beginCurrentTeleport(activeBatch);
        return cameras.size();
    }

    private static int cancelBatchCommand(CommandContext<CommandSourceStack> context) {
        if (activeBatch == null) {
            message(Component.literal("No screenshot batch is running."));
            return 0;
        }
        stopBatch(Component.literal("Screenshot batch cancelled."));
        return 1;
    }

    private static void beginCurrentTeleport(BatchRun batch) {
        batch.phase = BatchPhase.WAITING_FOR_TELEPORT;
        batch.phaseTicks = 0;
        batch.remainingSettleTicks = 0;
        NamedCamera camera = batch.currentCamera();
        if (!requestTeleport(camera.preset())) {
            stopBatch(Component.literal(
                    "Screenshot batch could not travel to camera " + camera.name() + "."
            ));
        }
    }

    private static boolean requestTeleport(CameraPreset preset) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            message(Component.literal("No server connection is available for teleporting."));
            return false;
        }
        if (!preset.hasValidDimension()) {
            message(Component.literal("Camera has an invalid dimension: " + preset.dimension + "."));
            return false;
        }

        String command = "execute in " + preset.dimension
                + " run teleport @s "
                + Double.toString(preset.x) + " "
                + Double.toString(preset.y) + " "
                + Double.toString(preset.z) + " "
                + Float.toString(preset.yaw) + " "
                + Float.toString(preset.pitch);
        minecraft.getConnection().sendCommand(command);
        return true;
    }

    private static boolean hasArrived(LocalPlayer player, CameraPreset preset) {
        if (!sameDimension(player, preset)) {
            return false;
        }
        double deltaX = player.getX() - preset.x;
        double deltaY = player.getY() - preset.y;
        double deltaZ = player.getZ() - preset.z;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= ARRIVAL_DISTANCE_SQUARED;
    }

    private static boolean sameDimension(LocalPlayer player, CameraPreset preset) {
        return player.level().dimension().location().toString().equals(preset.dimension);
    }

    private static void lockCamera(LocalPlayer player, CameraPreset preset) {
        player.moveTo(preset.x, preset.y, preset.z, preset.yaw, preset.pitch);
        player.setYHeadRot(preset.yaw);
        player.setYBodyRot(preset.yaw);
        player.setDeltaMovement(Vec3.ZERO);
        player.setOldPosAndRot();
    }

    private static void captureScreenshot(String requestedName) {
        Minecraft minecraft = Minecraft.getInstance();
        String fileName = uniqueScreenshotName(minecraft, "dune_" + sanitizeFilePart(requestedName));
        Screenshot.grab(
                minecraft.gameDirectory,
                fileName,
                minecraft.getMainRenderTarget(),
                component -> minecraft.execute(() -> minecraft.gui.getChat().addMessage(component))
        );
    }

    private static String uniqueScreenshotName(Minecraft minecraft, String baseName) {
        Path directory = minecraft.gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR);
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            MinecraftDune.LOGGER.warn("Could not create screenshot directory {}", directory, exception);
        }

        String candidate = baseName + ".png";
        int suffix = 2;
        while (Files.exists(directory.resolve(candidate))
                || !RESERVED_SCREENSHOT_NAMES.add(candidate.toLowerCase(Locale.ROOT))) {
            candidate = baseName + "_" + suffix + ".png";
            suffix++;
        }
        return candidate;
    }

    private static String sanitizeFilePart(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static void cancelActiveBatchForManualTeleport() {
        if (activeBatch != null) {
            stopBatch(Component.literal("Screenshot batch cancelled by manual camera teleport."));
        }
    }

    private static void stopBatch(Component finalMessage) {
        BatchRun batch = activeBatch;
        if (batch != null) {
            Minecraft.getInstance().options.hideGui = batch.previousHideGui;
            activeBatch = null;
        }
        message(finalMessage);
    }

    private static void message(Component component) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.getChat().addMessage(component));
    }

    private static boolean isSafeName(String value, String kind) {
        if (SAFE_NAME.matcher(value).matches()) {
            return true;
        }
        message(Component.literal(
                kind + " must be 1-64 characters using only letters, numbers, dot, underscore, or hyphen."
        ));
        return false;
    }

    private static List<String> sortedCameraNames() {
        List<String> names = new ArrayList<>(CAMERAS.keySet());
        names.sort(CAMERA_NAME_ORDER);
        return names;
    }

    private static void loadCameras() {
        CAMERAS.clear();
        if (!Files.isRegularFile(CAMERA_FILE)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(CAMERA_FILE, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new IOException("Camera file root is not an object");
            }
            JsonObject cameras = parsed.getAsJsonObject().getAsJsonObject("cameras");
            if (cameras == null) {
                return;
            }

            for (Map.Entry<String, JsonElement> entry : cameras.entrySet()) {
                if (!SAFE_NAME.matcher(entry.getKey()).matches() || !entry.getValue().isJsonObject()) {
                    continue;
                }
                try {
                    CameraPreset preset = GSON.fromJson(entry.getValue(), CameraPreset.class);
                    if (preset != null && preset.isFinite() && preset.hasValidDimension()) {
                        CAMERAS.put(entry.getKey(), preset);
                    }
                } catch (RuntimeException exception) {
                    MinecraftDune.LOGGER.warn("Skipping invalid debug camera {}", entry.getKey(), exception);
                }
            }
        } catch (IOException | RuntimeException exception) {
            MinecraftDune.LOGGER.warn("Could not load debug cameras from {}", CAMERA_FILE, exception);
            message(Component.literal("Could not load saved debug cameras; see the log for details."));
        }
    }

    private static boolean saveCameras() {
        Path temporaryFile = CAMERA_FILE.resolveSibling(CAMERA_FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(CAMERA_FILE.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("schema", 1);
            JsonObject cameras = new JsonObject();
            for (String name : sortedCameraNames()) {
                cameras.add(name, GSON.toJsonTree(CAMERAS.get(name)));
            }
            root.add("cameras", cameras);

            try (Writer writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            try {
                Files.move(
                        temporaryFile,
                        CAMERA_FILE,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, CAMERA_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            MinecraftDune.LOGGER.warn("Could not save debug cameras to {}", CAMERA_FILE, exception);
            message(Component.literal("Could not save debug cameras; see the log for details."));
            return false;
        }
    }

    private enum BatchPhase {
        WAITING_FOR_TELEPORT,
        SETTLING,
        READY_TO_RENDER,
        CAPTURE_AFTER_RENDER
    }

    private record CameraPreset(
            String dimension,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        static CameraPreset from(LocalPlayer player) {
            return new CameraPreset(
                    player.level().dimension().location().toString(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getYRot(),
                    player.getXRot()
            );
        }

        boolean hasValidDimension() {
            return dimension != null && net.minecraft.resources.ResourceLocation.tryParse(dimension) != null;
        }

        boolean isFinite() {
            return Double.isFinite(x)
                    && Double.isFinite(y)
                    && Double.isFinite(z)
                    && Float.isFinite(yaw)
                    && Float.isFinite(pitch)
                    && pitch >= -90.0F
                    && pitch <= 90.0F;
        }
    }

    private record NamedCamera(String name, CameraPreset preset) {
    }

    private static final class BatchRun {
        private final List<NamedCamera> cameras;
        private final String label;
        private final int settleTicks;
        private final boolean previousHideGui;
        private int cameraIndex;
        private int phaseTicks;
        private int remainingSettleTicks;
        private BatchPhase phase = BatchPhase.WAITING_FOR_TELEPORT;

        private BatchRun(
                List<NamedCamera> cameras,
                String label,
                int settleTicks,
                boolean previousHideGui
        ) {
            this.cameras = List.copyOf(cameras);
            this.label = label;
            this.settleTicks = settleTicks;
            this.previousHideGui = previousHideGui;
        }

        private NamedCamera currentCamera() {
            return cameras.get(cameraIndex);
        }
    }
}
