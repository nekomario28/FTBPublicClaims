package io.github.nekomario28.ftbpublicclaims.network.packet;

import io.github.nekomario28.ftbpublicclaims.client.ClientClaimContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SelectProjectPacket(UUID projectId) {
    public static void encode(SelectProjectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.projectId != null);
        if (packet.projectId != null) {
            buffer.writeUUID(packet.projectId);
        }
    }

    public static SelectProjectPacket decode(FriendlyByteBuf buffer) {
        return new SelectProjectPacket(buffer.readBoolean() ? buffer.readUUID() : null);
    }

    public static void handle(SelectProjectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientClaimContext.select(packet.projectId)
        ));
        context.setPacketHandled(true);
    }
}
