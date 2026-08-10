package com.blackenter.minecraftdune.worldgen.prototype;

public enum DuneSurfaceResolution {
    WHOLE("whole", 1),
    EIGHTH("eighth", 8),
    SIXTEENTH("sixteenth", 16);

    private final String commandName;
    private final int subdivisions;

    DuneSurfaceResolution(String commandName, int subdivisions) {
        this.commandName = commandName;
        this.subdivisions = subdivisions;
    }

    public String commandName() {
        return commandName;
    }

    public int subdivisions() {
        return subdivisions;
    }

    public int quantize(double heightInBlocks) {
        return (int) Math.round(heightInBlocks * subdivisions);
    }

    public int fullBlocks(int surfaceUnits) {
        return surfaceUnits / subdivisions;
    }

    public int partialLayers(int surfaceUnits) {
        return (surfaceUnits % subdivisions) * (16 / subdivisions);
    }
}
