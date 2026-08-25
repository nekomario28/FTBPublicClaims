package io.github.nekomario28.ftbpublicclaims;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue PUBLIC_CLAIMS_ENABLED;
    public static final ModConfigSpec.IntValue MAX_PUBLIC_CHUNKS;
    public static final ModConfigSpec.IntValue MAX_PUBLIC_CLAIM_DISTANCE;

    static {
        BUILDER.comment("FTB Public Claims configuration").push("publicClaims");

        PUBLIC_CLAIMS_ENABLED = BUILDER
                .comment("Enable the server-wide public claim realm")
                .define("enabled", true);

        MAX_PUBLIC_CHUNKS = BUILDER
                .comment("Base maximum number of chunks in the global public realm")
                // Retain the development config key so existing values carry forward.
                .defineInRange("maxChunksPerProject", 64, 1, 100000);

        MAX_PUBLIC_CLAIM_DISTANCE = BUILDER
                .comment("Maximum chunk distance from the player for map-based public claim changes")
                .defineInRange("maxClaimDistance", 16, 1, 128);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static boolean publicClaimsEnabled() {
        return PUBLIC_CLAIMS_ENABLED.get();
    }

    public static int getMaxPublicChunks() {
        return MAX_PUBLIC_CHUNKS.get();
    }

    public static int getMaxPublicClaimDistance() {
        return MAX_PUBLIC_CLAIM_DISTANCE.get();
    }

    private Config() {
    }
}
