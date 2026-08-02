package io.github.nekomario28.ftbpublicclaims.client;

import dev.ftb.mods.ftbchunks.client.gui.ChunkScreen;
import dev.ftb.mods.ftbchunks.client.gui.ChunkScreenPanel;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

@EventBusSubscriber(modid = FTBPublicClaims.MOD_ID, value = Dist.CLIENT)
public final class PublicClaimClientEvents {
    private static final Field SELECTED_CHUNKS = findSelectedChunksField();
    private static final boolean UI_PROBE = Boolean.getBoolean("ftbpublicclaims.uiProbe");
    private static int probeTicks;
    private static boolean probeMapOpened;

    private PublicClaimClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!UI_PROBE || probeMapOpened) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            probeTicks = 0;
            return;
        }
        if (++probeTicks >= 40) {
            probeMapOpened = true;
            ChunkScreen.openChunkScreen();
            probe("automatic-map-open");
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ScreenWrapper wrapper) || !(wrapper.getGui() instanceof ChunkScreen chunkScreen)) {
            return;
        }

        ClaimTargetButton targetButton = new ClaimTargetButton(chunkScreen);
        targetButton.setPosAndSize(chunkScreen.width / 2 - 100, 6, 200, 20);
        chunkScreen.add(targetButton);
        probe("map-screen-init"
                + " screenWidth=" + event.getScreen().width
                + " screenHeight=" + event.getScreen().height
                + " chunkWidth=" + chunkScreen.width
                + " chunkHeight=" + chunkScreen.height
                + " buttonCenterX=" + (targetButton.getX() + targetButton.getWidth() / 2)
                + " buttonCenterY=" + (targetButton.getY() + targetButton.getHeight() / 2));
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
            probe("selection-canceled unsupported-button=" + event.getButton());
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
            probe("selection-canceled force-load");
            return;
        }

        Set<ChunkPos> positions = new LinkedHashSet<>();
        selectedChunks.forEach(pos -> positions.add(new ChunkPos(pos.x(), pos.z())));
        boolean claim = event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        ModNetwork.sendChunkChange(new PublicChunkChangePacket(
                selectedProject.get().id(),
                claim,
                positions
        ));
        selectedChunks.clear();
        event.setCanceled(true);
        probe("public-chunk-packet claim=" + claim + " chunks=" + positions.size()
                + " canceled=" + event.isCanceled());
    }

    @SuppressWarnings("unchecked")
    private static Set<XZ> getSelectedChunks(ChunkScreen screen) {
        if (SELECTED_CHUNKS == null) {
            return Set.of();
        }
        try {
            return (Set<XZ>) SELECTED_CHUNKS.get(screen.getChunkScreen());
        } catch (IllegalAccessException exception) {
            FTBPublicClaims.LOGGER.error("Could not read FTB Chunks map selection", exception);
            return Set.of();
        }
    }

    private static Field findSelectedChunksField() {
        try {
            Field field = ChunkScreenPanel.class.getDeclaredField("selectedChunks");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            FTBPublicClaims.LOGGER.error("FTB Chunks 2101 map integration is unavailable", exception);
            return null;
        }
    }

    private static void probe(String message) {
        if (UI_PROBE) {
            FTBPublicClaims.LOGGER.info("FTBPublicClaims UI probe: {}", message);
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
            probe("target-cycle public=" + ClientClaimContext.selectedProject().isPresent());
        }

        @Override
        public boolean renderTitleInCenter() {
            return true;
        }
    }
}
