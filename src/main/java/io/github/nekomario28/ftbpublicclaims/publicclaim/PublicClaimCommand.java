package io.github.nekomario28.ftbpublicclaims.publicclaim;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.data.ServerTeam;
import io.github.nekomario28.ftbpublicclaims.Config;
import io.github.nekomario28.ftbpublicclaims.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class PublicClaimCommand {
    private PublicClaimCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("publicclaim")
                .then(Commands.literal("select")
                        .then(Commands.literal("personal")
                                .executes(context -> selectPersonal(context.getSource())))
                        .then(Commands.literal("public")
                                .executes(context -> selectPublic(context.getSource()))))
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource())))
                .then(Commands.literal("settings")
                        .then(booleanSetting("pvp", FTBServerTeamBridge::setPvp))
                        .then(booleanSetting("explosions", FTBServerTeamBridge::setExplosions))
                        .then(booleanSetting("mob_griefing", FTBServerTeamBridge::setMobGriefing)))
                .then(Commands.literal("internal")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.literal("add_extra")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("maximum", IntegerArgumentType.integer(1))
                                                .executes(context -> addExtraClaimChunks(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "amount"),
                                                        IntegerArgumentType.getInteger(context, "maximum")
                                                )))))
                        .then(Commands.literal("claim_chunk")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(context -> claimChunk(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "x"),
                                                        IntegerArgumentType.getInteger(context, "z")
                                                )))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> booleanSetting(
            String name,
            SettingUpdater updater
    ) {
        return Commands.literal(name)
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> updateSetting(
                                context.getSource(),
                                BoolArgumentType.getBool(context, "enabled"),
                                name,
                                updater
                        )));
    }

    private static int selectPersonal(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ModNetwork.selectPersonal(player);
        source.sendSuccess(
                () -> Component.translatable("ftbpublicclaims.public.selected_personal")
                        .withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int selectPublic(CommandSourceStack source) throws CommandSyntaxException {
        if (!requireEnabled(source)) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        PublicClaimProject project = getGlobal(source);
        ModNetwork.selectProject(player, project);
        source.sendSuccess(
                () -> Component.translatable("ftbpublicclaims.public.selected_public")
                        .withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        if (!Config.publicClaimsEnabled()) {
            source.sendSuccess(() -> Component.literal("FTBPublicClaims status enabled=false"), false);
            return 1;
        }
        ServerTeam team = getGlobalTeam(source);
        var data = FTBChunksAPI.api().getManager().getOrCreateData(team);
        String status = "FTBPublicClaims status"
                + " enabled=true"
                + " team=" + team.getId()
                + " pvp=" + team.getProperty(FTBChunksProperties.ALLOW_PVP)
                + " explosions=" + team.getProperty(FTBChunksProperties.ALLOW_EXPLOSIONS)
                + " mob_griefing=" + team.getProperty(FTBChunksProperties.ALLOW_MOB_GRIEFING)
                + " extra=" + data.getExtraClaimChunks()
                + " claims=" + data.getClaimedChunks().size();
        source.sendSuccess(() -> Component.literal(status), false);
        return 1;
    }

    private static int updateSetting(
            CommandSourceStack source,
            boolean enabled,
            String name,
            SettingUpdater updater
    ) throws CommandSyntaxException {
        if (!requireEnabled(source)) {
            return 0;
        }
        ServerTeam team = getGlobalTeam(source);
        updater.apply(team, enabled);
        source.sendSuccess(
                () -> Component.translatable("ftbpublicclaims.public.setting_updated", name, enabled)
                        .withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int addExtraClaimChunks(CommandSourceStack source, int amount, int maximum)
            throws CommandSyntaxException {
        if (!requireEnabled(source)) {
            return 0;
        }
        return FTBServerTeamBridge.addExtraClaimChunks(getGlobalTeam(source), amount, maximum) ? 1 : 0;
    }

    private static int claimChunk(CommandSourceStack source, int x, int z) throws CommandSyntaxException {
        if (!requireEnabled(source)) {
            return 0;
        }
        ServerTeam team = getGlobalTeam(source);
        var data = FTBChunksAPI.api().getManager().getOrCreateData(team);
        ChunkDimPos pos = new ChunkDimPos(source.getLevel().dimension(), new ChunkPos(x, z));
        var result = data.claim(source.withSuppressedOutput(), pos, false);
        if (!result.isSuccess()) {
            source.sendFailure(Component.literal("Public claim failed: " + result.getResultId()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Public chunk claimed: " + x + "," + z), false);
        return 1;
    }

    private static boolean requireEnabled(CommandSourceStack source) {
        if (Config.publicClaimsEnabled()) {
            return true;
        }
        source.sendFailure(Component.translatable("ftbpublicclaims.public.disabled").withStyle(ChatFormatting.RED));
        return false;
    }

    private static PublicClaimProject getGlobal(CommandSourceStack source) throws CommandSyntaxException {
        return PublicClaimSavedData.get(source.getServer()).getOrCreateGlobal(source);
    }

    private static ServerTeam getGlobalTeam(CommandSourceStack source) throws CommandSyntaxException {
        PublicClaimProject project = getGlobal(source);
        return FTBServerTeamBridge.find(project.teamId())
                .orElseThrow(() -> new IllegalStateException("Global public team is missing"));
    }

    @FunctionalInterface
    private interface SettingUpdater {
        void apply(ServerTeam team, boolean enabled);
    }
}
