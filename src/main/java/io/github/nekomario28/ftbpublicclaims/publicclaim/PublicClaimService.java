package io.github.nekomario28.ftbpublicclaims.publicclaim;

import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.data.ServerTeam;
import io.github.nekomario28.ftbpublicclaims.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PublicClaimService {
    public static final int MAX_CHANGES_PER_PACKET = 64;

    private PublicClaimService() {
    }

    public static void changeChunks(ServerPlayer player, PublicClaimProject project, boolean claim, Set<ChunkPos> requested) {
        if (!Config.publicClaimsEnabled()) {
            player.sendSystemMessage(Component.translatable("ftbpublicclaims.public.disabled").withStyle(ChatFormatting.RED));
            return;
        }

        ServerTeam team = FTBServerTeamBridge.find(project.teamId()).orElse(null);
        if (team == null) {
            player.sendSystemMessage(Component.translatable("ftbpublicclaims.public.missing_team").withStyle(ChatFormatting.RED));
            return;
        }

        List<ChunkPos> positions = requested.stream()
                .sorted(Comparator.comparingInt((ChunkPos pos) -> distance(player.chunkPosition(), pos)))
                .toList();

        ChunkTeamData teamData = FTBChunksAPI.api().getManager().getOrCreateData(team);
        ChangeSummary summary = claim
                ? claimChunks(player, teamData, positions)
                : unclaimChunks(player, teamData, positions);

        player.sendSystemMessage(Component.translatable(
                claim ? "ftbpublicclaims.public.claim_result" : "ftbpublicclaims.public.unclaim_result",
                summary.changed(), positions.size() - summary.changed()
        ).withStyle(summary.changed() > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));

        if (!summary.problems().isEmpty()) {
            String details = summary.problems().entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .sorted()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            player.sendSystemMessage(Component.literal(details).withStyle(ChatFormatting.GRAY));
        }
    }

    private static ChangeSummary claimChunks(ServerPlayer player, ChunkTeamData teamData, List<ChunkPos> positions) {
        Map<String, Integer> problems = new HashMap<>();
        int changed = 0;

        // FTB Chunks exempts server teams from its normal claim-power limit,
        // so the shared public realm enforces its own cap before claiming.
        long maxChunks = (long) Config.getMaxPublicChunks() + teamData.getExtraClaimChunks();
        int existing = teamData.getClaimedChunks().size();

        for (ChunkPos pos : positions) {
            if (existing + changed >= maxChunks) {
                addProblem(problems, "realm_limit");
                continue;
            }

            String validation = validateClaim(player, pos);
            if (validation != null) {
                addProblem(problems, validation);
                continue;
            }

            ChunkDimPos dimPos = new ChunkDimPos(player.level().dimension(), pos);
            ClaimResult result = teamData.claim(player.createCommandSourceStack(), dimPos, false);
            if (result.isSuccess()) {
                changed++;
            } else {
                addProblem(problems, result.getResultId());
            }
        }

        return new ChangeSummary(changed, problems);
    }

    private static ChangeSummary unclaimChunks(ServerPlayer player, ChunkTeamData teamData, List<ChunkPos> positions) {
        Map<String, Integer> problems = new HashMap<>();
        int changed = 0;
        for (ChunkPos pos : positions) {
            if (!withinRange(player, pos)) {
                addProblem(problems, "too_far");
                continue;
            }
            ChunkDimPos dimPos = new ChunkDimPos(player.level().dimension(), pos);
            ClaimedChunk chunk = FTBChunksAPI.api().getManager().getChunk(dimPos);
            if (chunk == null || !chunk.getTeamData().getTeam().getTeamId().equals(teamData.getTeam().getTeamId())) {
                addProblem(problems, "not_public_claim");
                continue;
            }
            ClaimResult result = teamData.unclaim(player.createCommandSourceStack(), dimPos, false, false);
            if (result.isSuccess()) {
                changed++;
            } else {
                addProblem(problems, result.getResultId());
            }
        }
        return new ChangeSummary(changed, problems);
    }

    private static String validateClaim(ServerPlayer player, ChunkPos pos) {
        if (!withinRange(player, pos)) {
            return "too_far";
        }

        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        if (manager.getChunk(new ChunkDimPos(player.level().dimension(), pos)) != null) {
            return "already_claimed";
        }
        return null;
    }

    private static boolean withinRange(ServerPlayer player, ChunkPos pos) {
        return distance(player.chunkPosition(), pos) <= Config.getMaxPublicClaimDistance();
    }

    private static int distance(ChunkPos left, ChunkPos right) {
        return Math.max(Math.abs(left.x - right.x), Math.abs(left.z - right.z));
    }

    private static void addProblem(Map<String, Integer> problems, String id) {
        problems.merge(id, 1, Integer::sum);
    }

    private record ChangeSummary(int changed, Map<String, Integer> problems) {
    }
}
