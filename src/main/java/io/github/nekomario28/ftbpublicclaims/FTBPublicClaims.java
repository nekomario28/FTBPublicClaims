package io.github.nekomario28.ftbpublicclaims;

import com.mojang.logging.LogUtils;
import io.github.nekomario28.ftbpublicclaims.network.ModNetwork;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(FTBPublicClaims.MOD_ID)
public class FTBPublicClaims {
    public static final String MOD_ID = "ftbpublicclaims";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FTBPublicClaims(FMLJavaModLoadingContext context) {
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(
                ModConfig.Type.COMMON,
                Config.SPEC
        );

        ModNetwork.register();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        PublicClaimCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.sendProjects(player);
        }
    }
}
