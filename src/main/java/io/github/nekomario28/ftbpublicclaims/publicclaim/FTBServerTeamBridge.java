package io.github.nekomario28.ftbpublicclaims.publicclaim;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.property.PrivacyMode;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.data.ServerTeam;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import net.minecraft.commands.CommandSourceStack;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Version boundary for FTB Teams 2101.1.9 and FTB Chunks 2101.1.20. */
public final class FTBServerTeamBridge {
    private static final String TEAM_NAME = "Global Public Claims";
    private static final String DESCRIPTION = "Server-wide public claims managed by FTBPublicClaims";

    private FTBServerTeamBridge() {
    }

    public static ServerTeam createShared(CommandSourceStack source) throws CommandSyntaxException {
        Optional<ServerTeam> recovered = findShared();
        if (recovered.isPresent()) {
            ServerTeam team = recovered.get();
            applySharedAccessProperties(team);
            FTBChunksAPI.api().getManager().getOrCreateData(team);
            return team;
        }

        Team created = requireManager().createServerTeam(
                source.withSuppressedOutput(), TEAM_NAME, DESCRIPTION, null
        );
        if (!(created instanceof ServerTeam team)) {
            throw new IllegalStateException("FTB Teams returned a non-server team");
        }
        applySharedProperties(team);
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

    public static Optional<ServerTeam> findShared() {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return Optional.empty();
        }
        return FTBTeamsAPI.api().getManager().getTeams().stream()
                .filter(ServerTeam.class::isInstance)
                .map(ServerTeam.class::cast)
                .filter(team -> TEAM_NAME.equals(team.getProperty(TeamProperties.DISPLAY_NAME)))
                .min(Comparator.comparing(team -> team.getId().toString()));
    }

    public static void applySharedProperties(ServerTeam team) {
        configureSharedAccess(team);
        team.setProperty(FTBChunksProperties.ALLOW_EXPLOSIONS, false);
        team.setProperty(FTBChunksProperties.ALLOW_MOB_GRIEFING, false);
        team.setProperty(FTBChunksProperties.ALLOW_PVP, true);
        saveAndSync(team);
    }

    public static void setPvp(ServerTeam team, boolean enabled) {
        team.setProperty(FTBChunksProperties.ALLOW_PVP, enabled);
        saveAndSync(team);
    }

    public static void setExplosions(ServerTeam team, boolean enabled) {
        team.setProperty(FTBChunksProperties.ALLOW_EXPLOSIONS, enabled);
        saveAndSync(team);
    }

    public static void setMobGriefing(ServerTeam team, boolean enabled) {
        team.setProperty(FTBChunksProperties.ALLOW_MOB_GRIEFING, enabled);
        saveAndSync(team);
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
            return true;
        }
        throw new IllegalStateException("Unsupported FTB Chunks team-data implementation: " + data.getClass().getName());
    }

    private static void applySharedAccessProperties(ServerTeam team) {
        configureSharedAccess(team);
        saveAndSync(team);
    }

    private static void configureSharedAccess(ServerTeam team) {
        team.setProperty(TeamProperties.DESCRIPTION, DESCRIPTION);
        team.setProperty(FTBChunksProperties.BLOCK_EDIT_MODE, PrivacyMode.PUBLIC);
        team.setProperty(FTBChunksProperties.BLOCK_INTERACT_MODE, PrivacyMode.PUBLIC);
        team.setProperty(FTBChunksProperties.ENTITY_INTERACT_MODE, PrivacyMode.PUBLIC);
        team.setProperty(FTBChunksProperties.NONLIVING_ENTITY_ATTACK_MODE, PrivacyMode.PUBLIC);
        team.setProperty(FTBChunksProperties.CLAIM_VISIBILITY, PrivacyMode.PUBLIC);
        team.setProperty(FTBChunksProperties.ALLOW_ALL_FAKE_PLAYERS, false);
        team.setProperty(FTBChunksProperties.ALLOW_NAMED_FAKE_PLAYERS, List.of());
        team.setProperty(FTBChunksProperties.ALLOW_FAKE_PLAYERS_BY_ID, false);
    }

    private static void saveAndSync(ServerTeam team) {
        team.markDirty();
        TeamManager manager = requireManager();
        if (manager instanceof TeamManagerImpl implementation) {
            implementation.syncToAll(team);
            return;
        }
        throw new IllegalStateException("Unsupported FTB Teams manager implementation: " + manager.getClass().getName());
    }

    private static TeamManager requireManager() {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            throw new IllegalStateException("FTB Teams manager is not ready");
        }
        return FTBTeamsAPI.api().getManager();
    }
}
