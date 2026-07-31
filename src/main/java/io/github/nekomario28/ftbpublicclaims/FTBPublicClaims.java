package io.github.nekomario28.ftbpublicclaims;

import com.mojang.logging.LogUtils;
import io.github.nekomario28.ftbpublicclaims.network.ModNetwork;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimCommand;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

@Mod(FTBPublicClaims.MOD_ID)
public class FTBPublicClaims {
    public static final String MOD_ID = "ftbpublicclaims";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FTBPublicClaims(IEventBus modBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modBus.addListener(ModNetwork::registerPayloads);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        PublicClaimCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                PublicClaimSavedData.get(player.getServer()).getOrCreateGlobal(player.createCommandSourceStack());
                ModNetwork.sendProjects(player);
            } catch (Exception exception) {
                LOGGER.error("Failed to initialize global public claims", exception);
            }
        }
    }
}
