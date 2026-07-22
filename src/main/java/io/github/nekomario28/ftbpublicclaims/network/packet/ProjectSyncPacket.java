package io.github.nekomario28.ftbpublicclaims.network.packet;

import io.github.nekomario28.ftbpublicclaims.client.ClientClaimContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record ProjectSyncPacket(List<ProjectSummary> projects) {
    public ProjectSyncPacket {
        projects = List.copyOf(projects);
    }

    public static void encode(ProjectSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.projects.size());
        for (ProjectSummary project : packet.projects) {
            buffer.writeUUID(project.id());
            buffer.writeUtf(project.name(), 32);
        }
    }

    public static ProjectSyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0) {
            throw new IllegalArgumentException("Negative public project count");
        }
        List<ProjectSummary> projects = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            projects.add(new ProjectSummary(buffer.readUUID(), buffer.readUtf(32)));
        }
        return new ProjectSyncPacket(projects);
    }

    public static void handle(ProjectSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientClaimContext.updateProjects(packet.projects)
        ));
        context.setPacketHandled(true);
    }

    public record ProjectSummary(UUID id, String name) {
    }
}
