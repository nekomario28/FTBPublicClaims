package io.github.nekomario28.ftbpublicclaims.network;

import io.github.nekomario28.ftbpublicclaims.FTBPublicClaims;
import io.github.nekomario28.ftbpublicclaims.network.packet.ProjectSyncPacket;
import io.github.nekomario28.ftbpublicclaims.network.packet.PublicChunkChangePacket;
import io.github.nekomario28.ftbpublicclaims.network.packet.SelectProjectPacket;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimProject;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.Optional;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(FTBPublicClaims.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, PublicChunkChangePacket.class,
                PublicChunkChangePacket::encode, PublicChunkChangePacket::decode, PublicChunkChangePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, ProjectSyncPacket.class,
                ProjectSyncPacket::encode, ProjectSyncPacket::decode, ProjectSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id, SelectProjectPacket.class,
                SelectProjectPacket::encode, SelectProjectPacket::decode, SelectProjectPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendProjects(ServerPlayer player) {
        List<ProjectSyncPacket.ProjectSummary> projects = PublicClaimSavedData.get(player.server)
                .manageableBy(player.getUUID())
                .stream()
                .map(project -> new ProjectSyncPacket.ProjectSummary(project.id(), project.name()))
                .toList();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ProjectSyncPacket(projects));
    }

    public static void selectProject(ServerPlayer player, PublicClaimProject project) {
        sendProjects(player);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SelectProjectPacket(project.id()));
    }

    public static void selectPersonal(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SelectProjectPacket(null));
    }
}
