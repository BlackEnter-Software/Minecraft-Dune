package com.blackenter.minecraftdune.worldgen.prototype;

/**
 * Initial dune families supported by the Arrakis Dev prototype.
 */
public enum DuneMode {
    TRANSVERSE("transverse", 180, 18, 0x6A09E667F3BCC909L),
    BARCHAN("barchan", 220, 20, 0xBB67AE8584CAA73BL);

    private final String commandName;
    private final int transportIterations;
    private final int maximumHeight;
    private final long seedSalt;

    DuneMode(
            String commandName,
            int transportIterations,
            int maximumHeight,
            long seedSalt
    ) {
        this.commandName = commandName;
        this.transportIterations = transportIterations;
        this.maximumHeight = maximumHeight;
        this.seedSalt = seedSalt;
    }

    public String commandName() {
        return commandName;
    }

    public int transportIterations() {
        return transportIterations;
    }

    public int maximumHeight() {
        return maximumHeight;
    }

    public long seedSalt() {
        return seedSalt;
    }
}
