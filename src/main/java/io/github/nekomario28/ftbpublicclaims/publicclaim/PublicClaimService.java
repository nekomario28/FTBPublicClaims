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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
        Set<ChunkPos> pending = new LinkedHashSet<>(positions);
        Map<String, Integer> problems = new HashMap<>();
        int changed = 0;

        // FTB Chunks 2101 deliberately exempts server teams from its normal
        // claim-power limit. The public realm therefore enforces its own cap
        // before calling the public ChunkTeamData claim API.
        long maxChunks = (long) Config.getMaxPublicChunks() + teamData.getExtraClaimChunks();
        int existing = teamData.getClaimedChunks().size();
        long existingInDimension = teamData.getClaimedChunks().stream()
                .map(ClaimedChunk::getPos)
                .filter(pos -> pos.dimension().equals(player.level().dimension()))
                .count();

        while (!pending.isEmpty() && existing + changed < maxChunks) {
            boolean progressed = false;
            for (ChunkPos pos : new ArrayList<>(pending)) {
                // Adjacency forms one connected public region per dimension.
                // A claim in the Overworld must not prevent the first claim in
                // the Nether or End from establishing that dimension's region.
                boolean firstClaimInDimension = existingInDimension + changed == 0;
                String validation = validateClaim(player, teamData, pos, firstClaimInDimension);
                if ("wait_for_adjacent".equals(validation)) {
                    continue;
                }
                pending.remove(pos);
                progressed = true;

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
            if (!progressed) {
                pending.forEach(pos -> addProblem(problems, "not_adjacent"));
                pending.clear();
            }
        }

        if (!pending.isEmpty()) {
            pending.forEach(pos -> addProblem(problems, "realm_limit"));
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

    private static String validateClaim(ServerPlayer player, ChunkTeamData teamData, ChunkPos pos, boolean firstClaim) {
        if (!withinRange(player, pos)) {
            return "too_far";
        }

        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        if (manager.getChunk(new ChunkDimPos(player.level().dimension(), pos)) != null) {
            return "already_claimed";
        }

        if (Config.requirePublicClaimAdjacency() && !firstClaim && !touchesPublicClaim(teamData, player, pos)) {
            return "wait_for_adjacent";
        }
        return null;
    }

    private static boolean touchesPublicClaim(ChunkTeamData teamData, ServerPlayer player, ChunkPos pos) {
        for (ClaimedChunk chunk : teamData.getClaimedChunks()) {
            ChunkDimPos claimedPos = chunk.getPos();
            if (claimedPos.dimension().equals(player.level().dimension())
                    && Math.abs(claimedPos.x() - pos.x) + Math.abs(claimedPos.z() - pos.z) == 1) {
                return true;
            }
        }
        return false;
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
