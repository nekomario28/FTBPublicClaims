package io.github.nekomario28.ftbpublicclaims;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import io.github.nekomario28.ftbpublicclaims.publicclaim.FTBServerTeamBridge;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimProject;
import io.github.nekomario28.ftbpublicclaims.publicclaim.PublicClaimSavedData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FTBPublicClaims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PublicCompatibilityGameTests {
    private PublicCompatibilityGameTests() {
    }

    @GameTest(template = "buyclaimchunks:empty", timeoutTicks = 200)
    public static void buyClaimChunksAndPublicClaimsRemainSeparate(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();

        helper.assertTrue(dispatcher.getRoot().getChild("buyclaim") != null,
                "Expected BuyClaimChunks /buyclaim command to be registered");
        helper.assertTrue(dispatcher.getRoot().getChild("publicclaim") != null,
                "Expected FTBPublicClaims /publicclaim command to be registered");

        try {
            PublicClaimProject project = PublicClaimSavedData.get(server)
                    .getOrCreateGlobal(server.createCommandSourceStack().withPermission(4));
            var publicTeam = FTBServerTeamBridge.find(project.teamId()).orElse(null);
            helper.assertTrue(publicTeam != null, "Expected the global public Server Team to exist");

            var publicData = FTBChunksAPI.api().getManager().getOrCreateData(publicTeam);
            helper.assertTrue(publicData.getTeam().getTeamId().equals(project.teamId()),
                    "Public claims must use the dedicated global Server Team");
            helper.assertTrue(!publicData.getTeam().isPlayerTeam(),
                    "Public claim capacity must not use personal player data");
            helper.succeed();
        } catch (Exception exception) {
            FTBPublicClaims.LOGGER.error("BuyClaimChunks compatibility GameTest failed", exception);
            helper.fail("Compatibility GameTest failed: " + exception.getMessage());
        }
    }
}
