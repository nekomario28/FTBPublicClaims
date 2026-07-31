package io.github.nekomario28.ftbpublicclaims.network.packet;

import io.github.nekomario28.ftbpublicclaims.FTBPublicClaims;
import io.github.nekomario28.ftbpublicclaims.client.ClientClaimContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.DistExecutor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record SelectProjectPacket(UUID projectId) implements CustomPacketPayload {
    public static final Type<SelectProjectPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FTBPublicClaims.MOD_ID, "select_project")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectProjectPacket> STREAM_CODEC =
            StreamCodec.ofMember(SelectProjectPacket::encode, SelectProjectPacket::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(projectId != null);
        if (projectId != null) {
            buffer.writeUUID(projectId);
        }
    }

    private static SelectProjectPacket decode(RegistryFriendlyByteBuf buffer) {
        return new SelectProjectPacket(buffer.readBoolean() ? buffer.readUUID() : null);
    }

    public static void handle(SelectProjectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientClaimContext.select(packet.projectId)
        ));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
