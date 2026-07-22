package io.github.nekomario28.ftbpublicclaims.network.packet;

import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimSavedData;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public record PublicChunkChangePacket(UUID projectId, boolean claim, Set<ChunkPos> chunks) {
    public PublicChunkChangePacket {
        LinkedHashSet<ChunkPos> limited = new LinkedHashSet<>();
        chunks.stream().limit(PublicClaimService.MAX_CHANGES_PER_PACKET).forEach(limited::add);
        chunks = Collections.unmodifiableSet(limited);
    }

    public static void encode(PublicChunkChangePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.projectId);
        buffer.writeBoolean(packet.claim);
        buffer.writeVarInt(packet.chunks.size());
        packet.chunks.forEach(pos -> {
            buffer.writeVarInt(pos.x);
            buffer.writeVarInt(pos.z);
        });
    }

    public static PublicChunkChangePacket decode(FriendlyByteBuf buffer) {
        UUID projectId = buffer.readUUID();
        boolean claim = buffer.readBoolean();
        int size = buffer.readVarInt();
        if (size < 0 || size > PublicClaimService.MAX_CHANGES_PER_PACKET) {
            throw new IllegalArgumentException("Invalid public chunk count: " + size);
        }
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) {
            chunks.add(new ChunkPos(buffer.readVarInt(), buffer.readVarInt()));
        }
        return new PublicChunkChangePacket(projectId, claim, chunks);
    }

    public static void handle(PublicChunkChangePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet.chunks.isEmpty()) {
                return;
            }
            PublicClaimSavedData.get(player.server).find(packet.projectId)
                    .ifPresent(project -> PublicClaimService.changeChunks(player, project, packet.claim, packet.chunks));
        });
        context.setPacketHandled(true);
    }
}
