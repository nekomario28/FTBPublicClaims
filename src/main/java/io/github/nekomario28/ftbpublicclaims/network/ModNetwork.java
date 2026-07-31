package io.github.nekomario28.ftbpublicclaims.network;

import io.github.nekomario28.ftbpublicclaims.FTBPublicClaims;
import io.github.nekomario28.ftbpublicclaims.network.packet.ProjectSyncPacket;
import io.github.nekomario28.ftbpublicclaims.network.packet.PublicChunkChangePacket;
import io.github.nekomario28.ftbpublicclaims.network.packet.SelectProjectPacket;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimProject;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

@EventBusSubscriber(modid = FTBPublicClaims.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    /**
     * Kept as an explicit entrypoint for callers from the legacy port. Payload
     * registration itself is driven by the NeoForge mod-bus event below.
     */
    public static void register() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                PublicChunkChangePacket.TYPE,
                PublicChunkChangePacket.STREAM_CODEC,
                PublicChunkChangePacket::handle
        );
        registrar.playToClient(
                ProjectSyncPacket.TYPE,
                ProjectSyncPacket.STREAM_CODEC,
                ProjectSyncPacket::handle
        );
        registrar.playToClient(
                SelectProjectPacket.TYPE,
                SelectProjectPacket.STREAM_CODEC,
                SelectProjectPacket::handle
        );
    }

    public static void sendChunkChange(PublicChunkChangePacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendProjects(ServerPlayer player) {
        List<ProjectSyncPacket.ProjectSummary> projects = PublicClaimSavedData.get(player.getServer())
                .manageableBy(player.getUUID())
                .stream()
                .map(project -> new ProjectSyncPacket.ProjectSummary(project.id(), project.name()))
                .toList();
        PacketDistributor.sendToPlayer(player, new ProjectSyncPacket(projects));
    }

    public static void selectProject(ServerPlayer player, PublicClaimProject project) {
        sendProjects(player);
        PacketDistributor.sendToPlayer(player, new SelectProjectPacket(project.id()));
    }

    public static void selectPersonal(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SelectProjectPacket(null));
    }
}
