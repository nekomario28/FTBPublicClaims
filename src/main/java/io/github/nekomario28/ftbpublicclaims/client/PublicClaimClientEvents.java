package io.github.nekomario28.ftbpublicclaims.client;

import dev.ftb.mods.ftbchunks.client.gui.ChunkScreen;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.ScreenWrapper;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import io.github.nekomario28.ftbpublicclaims.FTBPublicClaims;
import io.github.nekomario28.ftbpublicclaims.network.ModNetwork;
import io.github.nekomario28.ftbpublicclaims.network.packet.PublicChunkChangePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = FTBPublicClaims.MOD_ID, value = Dist.CLIENT)
public final class PublicClaimClientEvents {
    private static final Field SELECTED_CHUNKS = findSelectedChunksField();

    private PublicClaimClientEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ScreenWrapper wrapper) || !(wrapper.getGui() instanceof ChunkScreen chunkScreen)) {
            return;
        }

        ClaimTargetButton targetButton = new ClaimTargetButton(chunkScreen);
        targetButton.setPosAndSize(chunkScreen.width / 2 - 100, 6, 200, 20);
        chunkScreen.add(targetButton);
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        var selectedProject = ClientClaimContext.selectedProject();
        if (selectedProject.isEmpty()
                || !(event.getScreen() instanceof ScreenWrapper wrapper)
                || !(wrapper.getGui() instanceof ChunkScreen chunkScreen)) {
            return;
        }

        Set<XZ> selectedChunks = getSelectedChunks(chunkScreen);
        if (selectedChunks.isEmpty()) {
            return;
        }

        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            selectedChunks.clear();
            event.setCanceled(true);
            return;
        }

        if (event.getScreen().hasShiftDown()) {
            selectedChunks.clear();
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                        Component.translatable("ftbpublicclaims.public.no_force_load").withStyle(ChatFormatting.RED)
                );
            }
            event.setCanceled(true);
            return;
        }

        Set<ChunkPos> positions = new LinkedHashSet<>();
        selectedChunks.forEach(pos -> positions.add(new ChunkPos(pos.x(), pos.z())));
        ModNetwork.CHANNEL.sendToServer(new PublicChunkChangePacket(
                selectedProject.get().id(),
                event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT,
                positions
        ));
        selectedChunks.clear();
        event.setCanceled(true);
    }

    @SuppressWarnings("unchecked")
    private static Set<XZ> getSelectedChunks(ChunkScreen screen) {
        if (SELECTED_CHUNKS == null) {
            return Set.of();
        }
        try {
            return (Set<XZ>) SELECTED_CHUNKS.get(screen);
        } catch (IllegalAccessException exception) {
            FTBPublicClaims.LOGGER.error("Could not read FTB Chunks map selection", exception);
            return Set.of();
        }
    }

    private static Field findSelectedChunksField() {
        try {
            Field field = ChunkScreen.class.getDeclaredField("selectedChunks");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            FTBPublicClaims.LOGGER.error("FTB Chunks 2001.3.6 map integration is unavailable", exception);
            return null;
        }
    }

    private static final class ClaimTargetButton extends SimpleTextButton {
        private ClaimTargetButton(Panel panel) {
            super(panel, ClientClaimContext.buttonLabel(), Icons.FRIENDS);
        }

        @Override
        public void onClicked(MouseButton button) {
            ClientClaimContext.cycle();
            setTitle(ClientClaimContext.buttonLabel());
            setWidth(200);
            playClickSound();
        }

        @Override
        public boolean renderTitleInCenter() {
            return true;
        }
    }
}
