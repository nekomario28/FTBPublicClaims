package io.github.nekomario28.ftbpublicclaims;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue PUBLIC_CLAIMS_ENABLED;
    public static final ForgeConfigSpec.IntValue MAX_PUBLIC_PROJECTS_PER_PLAYER;
    public static final ForgeConfigSpec.IntValue MAX_PUBLIC_CHUNKS_PER_PROJECT;
    public static final ForgeConfigSpec.IntValue MAX_PUBLIC_CLAIM_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_PUBLIC_CLAIM_ADJACENCY;

    static {
        BUILDER.comment("FTB Public Claims configuration").push("publicClaims");

        PUBLIC_CLAIMS_ENABLED = BUILDER
                .comment("Allow non-operator players to create and manage public claim projects")
                .define("enabled", true);

        MAX_PUBLIC_PROJECTS_PER_PLAYER = BUILDER
                .comment("Maximum number of public projects owned by one player")
                .defineInRange("maxProjectsPerPlayer", 1, 0, 100);

        MAX_PUBLIC_CHUNKS_PER_PROJECT = BUILDER
                .comment("Maximum claimed chunks in one public project")
                .defineInRange("maxChunksPerProject", 64, 1, 100000);

        MAX_PUBLIC_CLAIM_DISTANCE = BUILDER
                .comment("Maximum chunk distance from the player for map-based public claim changes")
                .defineInRange("maxClaimDistance", 16, 1, 128);

        REQUIRE_PUBLIC_CLAIM_ADJACENCY = BUILDER
                .comment("Require new public chunks after the first one to touch an existing project chunk")
                .define("requireAdjacency", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static boolean publicClaimsEnabled() {
        return PUBLIC_CLAIMS_ENABLED.get();
    }

    public static int getMaxPublicProjectsPerPlayer() {
        return MAX_PUBLIC_PROJECTS_PER_PLAYER.get();
    }

    public static int getMaxPublicChunksPerProject() {
        return MAX_PUBLIC_CHUNKS_PER_PROJECT.get();
    }

    public static int getMaxPublicClaimDistance() {
        return MAX_PUBLIC_CLAIM_DISTANCE.get();
    }

    public static boolean requirePublicClaimAdjacency() {
        return REQUIRE_PUBLIC_CLAIM_ADJACENCY.get();
    }

    private Config() {
    }
}
