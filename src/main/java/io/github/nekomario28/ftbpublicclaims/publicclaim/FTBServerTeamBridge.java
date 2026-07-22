package io.github.nekomario28.ftbpublicclaims.publicclaim;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
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
 * Version-specific bridge for the FTB Teams 2001.3.1 server-team implementation.
 * FTB Teams exposes server teams to commands, but not through its public creation API.
 */
public final class FTBServerTeamBridge {
    private FTBServerTeamBridge() {
    }

    public static ServerTeam create(CommandSourceStack source, String projectName, UUID ownerId) throws CommandSyntaxException {
        TeamManagerImpl manager = requireManager();
        ServerTeam team = manager.createServer(source.withSuppressedOutput(), "Public: " + projectName).getRight();

        team.setProperty(TeamProperties.DESCRIPTION, "Managed public claim created by FTBPublicClaims");
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
        manager.syncToAll(team);
        FTBChunksAPI.api().getManager().getOrCreateData(team);
        return team;
    }

    public static Optional<ServerTeam> find(UUID teamId) {
        TeamManagerImpl manager = TeamManagerImpl.INSTANCE;
        if (manager == null) {
            return Optional.empty();
        }
        return manager.getTeamByID(teamId).filter(ServerTeam.class::isInstance).map(ServerTeam.class::cast);
    }

    public static void setManager(ServerTeam team, UUID playerId, boolean enabled) {
        if (enabled) {
            team.addMember(playerId, TeamRank.OFFICER);
        } else {
            team.removeMember(playerId);
        }
        team.markDirty();
        requireManager().syncToAll(team);
    }

    public static boolean addExtraClaimChunks(ServerTeam team, int amount, int maximum) {
        var data = ClaimedChunkManagerImpl.getInstance().getOrCreateData(team);
        long updated = (long) data.getExtraClaimChunks() + amount;
        if (amount < 1 || updated > maximum) {
            return false;
        }

        data.setExtraClaimChunks((int) updated);
        data.markDirty();
        return true;
    }

    public static void delete(CommandSourceStack source, ServerTeam team) {
        ChunkTeamData chunkData = FTBChunksAPI.api().getManager().getOrCreateData(team);
        new ArrayList<>(chunkData.getClaimedChunks()).forEach(chunk -> chunk.unclaim(source, true));

        new ArrayList<>(team.getMembers()).forEach(team::removeMember);

        ClaimedChunkManagerImpl.getInstance().deleteTeam(team);
        team.delete(source.withSuppressedOutput());
    }

    private static TeamManagerImpl requireManager() {
        TeamManagerImpl manager = TeamManagerImpl.INSTANCE;
        if (manager == null) {
            throw new IllegalStateException("FTB Teams manager is not ready");
        }
        return manager;
    }
}
