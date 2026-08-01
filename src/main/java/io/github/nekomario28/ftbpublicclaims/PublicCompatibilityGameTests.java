package io.github.nekomario28.ftbpublicclaims;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.api.ProtectionPolicy;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import io.github.nekomario28.ftbpublicclaims.publicclaim.FTBServerTeamBridge;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimProject;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimSavedData;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Combined runtime contract with the released BuyClaimChunks Continued JAR. */
@GameTestHolder("buyclaimchunks")
@PrefixGameTestTemplate(false)
public final class PublicCompatibilityGameTests {
    private PublicCompatibilityGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void realPersonalPurchaseDoesNotChangePublicCapacity(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();

        helper.assertTrue(dispatcher.getRoot().getChild("buyclaim") != null,
                "Expected BuyClaimChunks /buyclaim command to be registered");
        helper.assertTrue(dispatcher.getRoot().getChild("publicclaim") != null,
                "Expected FTBPublicClaims /publicclaim command to be registered");

        final ServerPlayer player;
        try {
            player = makeConnectedPlayer(helper, "public-buy-test");
        } catch (Exception exception) {
            FTBPublicClaims.LOGGER.error("Failed to create compatibility GameTest player", exception);
            helper.fail("Failed to create connected player: " + exception.getMessage());
            return;
        }

        helper.runAfterDelay(5, () -> {
            try {
                PublicClaimProject project = PublicClaimSavedData.get(server)
                        .getOrCreateGlobal(server.createCommandSourceStack().withPermission(4));
                var publicTeam = FTBServerTeamBridge.find(project.teamId()).orElse(null);
                helper.assertTrue(publicTeam != null, "Expected the global public Server Team to exist");

                var publicData = FTBChunksAPI.api().getManager().getOrCreateData(publicTeam);
                helper.assertTrue(publicData.getTeam().getTeamId().equals(project.teamId()),
                        "Public claims must use the dedicated global Server Team");
                helper.assertTrue(!publicData.getTeam().isPlayerTeam(),
                        "Public claim capacity must not use personal player data");

                int publicExtraBefore = publicData.getExtraClaimChunks();
                int publicClaimsBefore = publicData.getClaimedChunks().size();

                String playerName = player.getGameProfile().getName();
                dispatcher.execute(
                        "ftbchunks admin extra_claim_chunks " + playerName + " set 0",
                        server.createCommandSourceStack().withPermission(4).withSuppressedOutput()
                );

                var personalData = FTBChunksAPI.api().getManager().getPersonalData(player.getUUID());
                helper.assertTrue(personalData != null, "Expected personal FTB Chunks data after login");
                helper.assertValueEqual(personalData.getExtraClaimChunks(), 0,
                        "personal extra capacity before purchase");

                player.getInventory().clearContent();
                player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 4));

                int result = dispatcher.execute("buyclaim", player.createCommandSourceStack());
                helper.assertValueEqual(result, 1, "/buyclaim command result");
                helper.assertValueEqual(personalData.getExtraClaimChunks(), 1,
                        "personal extra capacity after purchase");
                helper.assertTrue(player.getInventory().getItem(0).isEmpty(),
                        "Expected the four-diamond payment to be consumed");

                helper.assertValueEqual(publicData.getExtraClaimChunks(), publicExtraBefore,
                        "public extra capacity after personal purchase");
                helper.assertValueEqual(publicData.getClaimedChunks().size(), publicClaimsBefore,
                        "public claimed chunks after personal purchase");
                helper.succeed();
            } catch (Exception exception) {
                FTBPublicClaims.LOGGER.error("BuyClaimChunks compatibility GameTest failed", exception);
                helper.fail("Compatibility GameTest failed: " + exception.getMessage());
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void differentPlayersCanClaimAndUnclaimTheSharedPublicChunk(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        final ServerPlayer first;
        final ServerPlayer second;
        try {
            first = makeConnectedPlayer(helper, "public-claimer");
            second = makeConnectedPlayer(helper, "public-unclaimer");
        } catch (Exception exception) {
            helper.fail("Failed to create connected players: " + exception.getMessage());
            return;
        }

        helper.runAfterDelay(5, () -> {
            try {
                PublicClaimProject project = PublicClaimSavedData.get(server)
                        .getOrCreateGlobal(server.createCommandSourceStack().withPermission(4));
                var publicTeam = FTBServerTeamBridge.find(project.teamId()).orElseThrow();
                var publicData = FTBChunksAPI.api().getManager().getOrCreateData(publicTeam);

                BlockPos absolutePos = helper.absolutePos(BlockPos.ZERO);
                ChunkPos chunk = new ChunkPos(absolutePos);
                ChunkDimPos dimensionChunk = new ChunkDimPos(helper.getLevel().dimension(), chunk);
                clearExistingClaim(server, dimensionChunk);

                var claimResult = publicData.claim(first.createCommandSourceStack(), dimensionChunk, false);
                helper.assertTrue(claimResult.isSuccess(),
                        "First world participant must be able to create the shared public claim: "
                                + claimResult.getResultId());

                var claimed = FTBChunksAPI.api().getManager().getChunk(dimensionChunk);
                helper.assertTrue(claimed != null, "Expected the public chunk to be claimed");
                helper.assertTrue(claimed.getTeamData().getTeam().getTeamId().equals(project.teamId()),
                        "The claimed chunk must belong to the global public Server Team");

                var unclaimResult = publicData.unclaim(second.createCommandSourceStack(), dimensionChunk, false, false);
                helper.assertTrue(unclaimResult.isSuccess(),
                        "Another world participant must be able to unclaim the shared public chunk: "
                                + unclaimResult.getResultId());
                helper.assertTrue(FTBChunksAPI.api().getManager().getChunk(dimensionChunk) == null,
                        "Expected the shared public chunk to be unclaimed");
                helper.succeed();
            } catch (Exception exception) {
                FTBPublicClaims.LOGGER.error("Cross-player public claim GameTest failed", exception);
                helper.fail("Cross-player public claim GameTest failed: " + exception.getMessage());
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void anyWorldParticipantCanEditAndInteractInsidePublicClaim(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        final ServerPlayer firstParticipant;
        final ServerPlayer anotherParticipant;
        try {
            firstParticipant = makeConnectedPlayer(helper, "participant-a");
            anotherParticipant = makeConnectedPlayer(helper, "participant-b");
        } catch (Exception exception) {
            helper.fail("Failed to create connected world participants: " + exception.getMessage());
            return;
        }

        helper.runAfterDelay(5, () -> {
            try {
                PublicClaimProject project = PublicClaimSavedData.get(server)
                        .getOrCreateGlobal(server.createCommandSourceStack().withPermission(4));
                var publicTeam = FTBServerTeamBridge.find(project.teamId()).orElseThrow();
                var publicData = FTBChunksAPI.api().getManager().getOrCreateData(publicTeam);

                BlockPos absolutePos = helper.absolutePos(BlockPos.ZERO);
                ChunkDimPos dimensionChunk = new ChunkDimPos(
                        helper.getLevel().dimension(), new ChunkPos(absolutePos)
                );
                clearExistingClaim(server, dimensionChunk);

                var claimResult = publicData.claim(
                        firstParticipant.createCommandSourceStack(), dimensionChunk, false
                );
                helper.assertTrue(claimResult.isSuccess(),
                        "A world participant must be able to create the public claim: "
                                + claimResult.getResultId());
                var claimed = FTBChunksAPI.api().getManager().getChunk(dimensionChunk);
                helper.assertTrue(claimed != null, "Expected claimed chunk data");

                helper.getLevel().setBlockAndUpdate(absolutePos, Blocks.STONE.defaultBlockState());
                ProtectionPolicy editPolicy = Protection.EDIT_BLOCK.getProtectionPolicy(
                        anotherParticipant, absolutePos, InteractionHand.MAIN_HAND, claimed, null
                );
                helper.assertValueEqual(editPolicy, ProtectionPolicy.ALLOW,
                        "all world participants can edit public blocks");

                helper.getLevel().setBlockAndUpdate(absolutePos, Blocks.CHEST.defaultBlockState());
                ProtectionPolicy interactPolicy = Protection.INTERACT_BLOCK.getProtectionPolicy(
                        anotherParticipant, absolutePos, InteractionHand.MAIN_HAND, claimed, null
                );
                helper.assertValueEqual(interactPolicy, ProtectionPolicy.ALLOW,
                        "all world participants can interact with public containers");

                publicData.unclaim(server.createCommandSourceStack().withPermission(4), dimensionChunk, false, true);
                helper.succeed();
            } catch (Exception exception) {
                FTBPublicClaims.LOGGER.error("Public world-participant access GameTest failed", exception);
                helper.fail("Public world-participant access GameTest failed: " + exception.getMessage());
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
