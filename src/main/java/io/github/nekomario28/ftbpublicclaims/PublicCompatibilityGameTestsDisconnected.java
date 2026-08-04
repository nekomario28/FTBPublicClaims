package io.github.nekomario28.ftbpublicclaims;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import io.github.nekomario28.ftbpublicclaims.publicclaim.FTBServerTeamBridge;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimProject;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimSavedData;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimService;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

@GameTestHolder("buyclaimchunks")
@PrefixGameTestTemplate(false)
public final class PublicCompatibilityGameTestsDisconnected {
    private PublicCompatibilityGameTestsDisconnected() {
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void disconnectedChunksCanJoinTheSharedPublicRealm(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        final ServerPlayer player;
        try {
            player = makeConnectedPlayer(helper, "public-disconnected-claimer");
        } catch (Exception exception) {
            helper.fail("Failed to create connected player: " + exception.getMessage());
            return;
        }

        helper.runAfterDelay(5, () -> {
            try {
                PublicClaimProject project = PublicClaimSavedData.get(server)
                        .getOrCreateGlobal(server.createCommandSourceStack().withPermission(4));
                var publicTeam = FTBServerTeamBridge.find(project.teamId()).orElseThrow();
                var publicData = FTBChunksAPI.api().getManager().getOrCreateData(publicTeam);

                ChunkPos playerChunk = player.chunkPosition();
                ChunkPos first = new ChunkPos(playerChunk.x + 2, playerChunk.z);
                ChunkPos disconnected = new ChunkPos(playerChunk.x + 8, playerChunk.z + 8);
                ChunkDimPos firstDim = new ChunkDimPos(helper.getLevel().dimension(), first);
                ChunkDimPos disconnectedDim = new ChunkDimPos(helper.getLevel().dimension(), disconnected);

                clearExistingClaim(server, firstDim);
                clearExistingClaim(server, disconnectedDim);

                PublicClaimService.changeChunks(player, project, true, Set.of(first, disconnected));

                var firstClaim = FTBChunksAPI.api().getManager().getChunk(firstDim);
                var disconnectedClaim = FTBChunksAPI.api().getManager().getChunk(disconnectedDim);
                helper.assertTrue(firstClaim != null,
                        "Expected the first public chunk to be claimed");
                helper.assertTrue(disconnectedClaim != null,
                        "Expected a disconnected public chunk to be claimed without adjacency");
                helper.assertTrue(firstClaim.getTeamData().getTeam().getTeamId().equals(project.teamId()),
                        "First chunk must belong to the global public team");
                helper.assertTrue(disconnectedClaim.getTeamData().getTeam().getTeamId().equals(project.teamId()),
                        "Disconnected chunk must belong to the global public team");

                publicData.unclaim(server.createCommandSourceStack().withPermission(4), firstDim, false, true);
                publicData.unclaim(server.createCommandSourceStack().withPermission(4), disconnectedDim, false, true);
                helper.succeed();
            } catch (Exception exception) {
                FTBPublicClaims.LOGGER.error("Disconnected public claim GameTest failed", exception);
                helper.fail("Disconnected public claim GameTest failed: " + exception.getMessage());
            }
        });
    }

    private static void clearExistingClaim(net.minecraft.server.MinecraftServer server, ChunkDimPos dimensionChunk) {
        var existing = FTBChunksAPI.api().getManager().getChunk(dimensionChunk);
        if (existing != null) {
            existing.getTeamData().unclaim(
                    server.createCommandSourceStack().withPermission(4), dimensionChunk, false, true
            );
        }
    }

    private static ServerPlayer makeConnectedPlayer(GameTestHelper helper, String name) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), name), false
        );
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                cookie.gameProfile(),
                cookie.clientInformation()
        );

        Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public void tick() {
                super.tick();
                player.resetLastActionTime();
            }

            @Override
            public boolean isMemoryConnection() {
                return true;
            }

            @Override
            public void send(Packet<?> packet, @Nullable PacketSendListener listener, boolean flush) {
                super.send(packet, listener, flush);
                if (packet instanceof ClientboundKeepAlivePacket keepAlive) {
                    player.connection.handleKeepAlive(new ServerboundKeepAlivePacket(keepAlive.getId()));
                }
            }
        };

        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        var server = helper.getLevel().getServer();
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        server.getConnection().getConnections().add(connection);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.connection.chunkSender.sendNextChunks(player);
        player.connection.chunkSender.onChunkBatchReceivedByClient(64.0F);
        return player;
    }
}
