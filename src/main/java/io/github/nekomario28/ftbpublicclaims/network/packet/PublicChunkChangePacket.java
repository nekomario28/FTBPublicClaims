package io.github.nekomario28.ftbpublicclaims.network.packet;

import io.github.nekomario28.ftbpublicclaims.FTBPublicClaims;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimSavedData;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public record PublicChunkChangePacket(UUID projectId, boolean claim, Set<ChunkPos> chunks)
        implements CustomPacketPayload {
    public static final Type<PublicChunkChangePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FTBPublicClaims.MOD_ID, "public_chunk_change")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PublicChunkChangePacket> STREAM_CODEC =
            StreamCodec.ofMember(PublicChunkChangePacket::encode, PublicChunkChangePacket::decode);

    public PublicChunkChangePacket {
        LinkedHashSet<ChunkPos> limited = new LinkedHashSet<>();
        chunks.stream().limit(PublicClaimService.MAX_CHANGES_PER_PACKET).forEach(limited::add);
        chunks = Collections.unmodifiableSet(limited);
    }

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(projectId);
        buffer.writeBoolean(claim);
        buffer.writeVarInt(chunks.size());
        chunks.forEach(pos -> {
            buffer.writeVarInt(pos.x);
            buffer.writeVarInt(pos.z);
        });
    }

    private static PublicChunkChangePacket decode(RegistryFriendlyByteBuf buffer) {
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

    public static void handle(PublicChunkChangePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || packet.chunks.isEmpty()) {
                return;
            }
            PublicClaimSavedData.get(player.getServer()).find(packet.projectId)
                    .ifPresent(project -> PublicClaimService.changeChunks(player, project, packet.claim, packet.chunks));
        }).exceptionally(error -> {
            FTBPublicClaims.LOGGER.error("Failed to process public chunk change payload", error);
            return null;
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
