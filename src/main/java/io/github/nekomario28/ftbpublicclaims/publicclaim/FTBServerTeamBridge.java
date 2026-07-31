package io.github.nekomario28.ftbpublicclaims.publicclaim;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.property.PrivacyMode;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.data.ServerTeam;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Version boundary for the FTB Teams 2101.1.9 and FTB Chunks 2101.1.20 APIs.
 *
 * <p>Server-team creation is public in FTB Teams 2101, so this bridge no longer
 * reaches into private fields. Implementation casts remain limited to explicit
 * save and client-sync operations that are not exposed by the public APIs.</p>
 */
public final class FTBServerTeamBridge {
    private static final String DESCRIPTION = "Managed public claim created by FTBPublicClaims";

    private FTBServerTeamBridge() {
    }

    public static ServerTeam create(CommandSourceStack source, String projectName, UUID ownerId)
            throws CommandSyntaxException {
        TeamManager manager = requireManager();
        Team created = manager.createServerTeam(
                source.withSuppressedOutput(),
                "Public: " + projectName,
                DESCRIPTION,
                null
        );
        if (!(created instanceof ServerTeam team)) {
            throw new IllegalStateException("FTB Teams returned a non-server team for a server-team request");
        }

        team.setProperty(TeamProperties.DESCRIPTION, DESCRIPTION);
        team.setProperty(FTBChunksProperties.BLOCK_EDIT_MODE, PrivacyMode.ALLIES);
        team.setProperty(FTBChunksProperties.BLOCK_INTERACT_MODE, PrivacyMode.PUBLIC);
        team.setProperty(FTBChunksProperties.ENTITY_INTERACT_MODE, PrivacyMode.PUBLIC);
        team.setProperty(FTBChunksProperties.NONLIVING_ENTITY_ATTACK_MODE, PrivacyMode.ALLIES);
        team.setProperty(FTBChunksProperties.CLAIM_VISIBILITY, PrivacyMode.PUBLIC);
        team.setProperty(FTBChunksProperties.ALLOW_EXPLOSIONS, false);
        team.setProperty(FTBChunksProperties.ALLOW_MOB_GRIEFING, false);
        team.setProperty(FTBChunksProperties.ALLOW_PVP, false);
        team.setProperty(FTBChunksProperties.ALLOW_ALL_FAKE_PLAYERS, false);
        team.setProperty(FTBChunksProperties.ALLOW_NAMED_FAKE_PLAYERS, List.of());
        team.setProperty(FTBChunksProperties.ALLOW_FAKE_PLAYERS_BY_ID, false);

        team.addMember(ownerId, TeamRank.OWNER);
        team.markDirty();
        syncTeam(team);
        FTBChunksAPI.api().getManager().getOrCreateData(team);
        return team;
    }

    public static Optional<ServerTeam> find(UUID teamId) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return Optional.empty();
        }
        return FTBTeamsAPI.api().getManager().getTeamByID(teamId)
                .filter(ServerTeam.class::isInstance)
                .map(ServerTeam.class::cast);
    }

    public static void setManager(ServerTeam team, UUID playerId, boolean enabled) {
        if (enabled) {
            team.addMember(playerId, TeamRank.OFFICER);
        } else {
            team.removeMember(playerId);
        }
        team.markDirty();
        syncTeam(team);
    }

    public static boolean addExtraClaimChunks(ServerTeam team, int amount, int maximum) {
        ChunkTeamData data = FTBChunksAPI.api().getManager().getOrCreateData(team);
        long updated = (long) data.getExtraClaimChunks() + amount;
        if (amount < 1 || updated > maximum || updated > Integer.MAX_VALUE) {
            return false;
        }

        data.setExtraClaimChunks((int) updated);
        if (data instanceof ChunkTeamDataImpl implementation) {
            implementation.markDirty();
        } else {
            throw new IllegalStateException("Unsupported FTB Chunks team-data implementation: " + data.getClass().getName());
        }
        return true;
    }

    public static void delete(CommandSourceStack source, ServerTeam team) {
        ChunkTeamData chunkData = FTBChunksAPI.api().getManager().getOrCreateData(team);
        new ArrayList<>(chunkData.getClaimedChunks())
                .forEach(chunk -> chunk.unclaim(source.withSuppressedOutput(), true));

        new ArrayList<>(team.getMembers()).forEach(team::removeMember);
        team.markDirty();

        ClaimedChunkManagerImpl manager = ClaimedChunkManagerImpl.getInstance();
        if (manager != null) {
            manager.deleteTeam(team);
        }
        team.delete(source.withSuppressedOutput());
    }

    private static TeamManager requireManager() {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            throw new IllegalStateException("FTB Teams manager is not ready");
        }
        return FTBTeamsAPI.api().getManager();
    }

    private static void syncTeam(ServerTeam team) {
        TeamManager manager = requireManager();
        if (manager instanceof TeamManagerImpl implementation) {
            implementation.syncToAll(team);
        } else {
            throw new IllegalStateException("Unsupported FTB Teams manager implementation: " + manager.getClass().getName());
        }
    }
}
