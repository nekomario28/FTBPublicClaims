package io.github.nekomario28.ftbpublicclaims.network.packet;

import io.github.nekomario28.ftbpublicclaims.FTBPublicClaims;
import io.github.nekomario28.ftbpublicclaims.client.ClientClaimContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ProjectSyncPacket(List<ProjectSummary> projects) implements CustomPacketPayload {
    public static final Type<ProjectSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FTBPublicClaims.MOD_ID, "project_sync")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ProjectSyncPacket> STREAM_CODEC =
            StreamCodec.ofMember(ProjectSyncPacket::encode, ProjectSyncPacket::decode);

    public ProjectSyncPacket {
        projects = List.copyOf(projects);
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(projects.size());
        for (ProjectSummary project : projects) {
            buffer.writeUUID(project.id());
            buffer.writeUtf(project.name(), 32);
        }
    }

    private static ProjectSyncPacket decode(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > 1024) {
            throw new IllegalArgumentException("Invalid public project count: " + size);
        }
        List<ProjectSummary> projects = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            projects.add(new ProjectSummary(buffer.readUUID(), buffer.readUtf(32)));
        }
        return new ProjectSyncPacket(projects);
    }

    public static void handle(ProjectSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientClaimContext.updateProjects(packet.projects));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record ProjectSummary(UUID id, String name) {
    }
}
