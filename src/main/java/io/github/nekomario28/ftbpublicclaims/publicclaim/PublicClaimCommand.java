package io.github.nekomario28.ftbpublicclaims.publicclaim;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.data.ServerTeam;
import io.github.nekomario28.ftbpublicclaims.Config;
import io.github.nekomario28.ftbpublicclaims.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class PublicClaimCommand {
    private static final Pattern PROJECT_NAME = Pattern.compile("[A-Za-z0-9_-]{3,24}");

    private PublicClaimCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("publicclaim")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> create(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "name")
                                ))))
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(Commands.literal("select")
                        .then(Commands.literal("personal").executes(context -> selectPersonal(context.getSource())))
                        .then(projectArgument().executes(context -> select(
                                context.getSource(),
                                StringArgumentType.getString(context, "project")
                        ))))
                .then(Commands.literal("delete")
                        .then(projectArgument()
                                .then(Commands.literal("confirm").executes(context -> delete(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "project")
                                )))))
                .then(Commands.literal("manager")
                        .then(projectArgument()
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> setManager(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "project"),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        true
                                                ))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> setManager(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "project"),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        false
                                                ))))))
                .then(Commands.literal("internal")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.literal("add_extra")
                                .then(projectArgument()
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("maximum", IntegerArgumentType.integer(1))
                                                        .executes(context -> addExtraClaimChunks(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "project"),
                                                                IntegerArgumentType.getInteger(context, "amount"),
                                                                IntegerArgumentType.getInteger(context, "maximum")
                                                        ))))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> projectArgument() {
        return Commands.argument("project", StringArgumentType.word()).suggests((context, builder) -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player != null) {
                PublicClaimSavedData.get(player.server).manageableBy(player.getUUID())
                        .forEach(project -> builder.suggest(project.name()));
            }
            return builder.buildFuture();
        });
    }

    private static int create(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!Config.publicClaimsEnabled()) {
            return fail(source, "ftbpublicclaims.public.disabled");
        }
        if (!PROJECT_NAME.matcher(name).matches()) {
            return fail(source, "ftbpublicclaims.public.invalid_name");
        }

        PublicClaimSavedData data = PublicClaimSavedData.get(player.server);
        if (data.findByName(name).isPresent()) {
            return fail(source, "ftbpublicclaims.public.name_taken");
        }
        if (data.countOwnedBy(player.getUUID()) >= Config.getMaxPublicProjectsPerPlayer()) {
            return fail(source, "ftbpublicclaims.public.project_limit");
        }

        ServerTeam team = FTBServerTeamBridge.create(source, name, player.getUUID());
        PublicClaimProject project = PublicClaimProject.create(name, team.getId(), player.getUUID());
        data.add(project);
        ModNetwork.selectProject(player, project);
        source.sendSuccess(() -> Component.translatable("ftbpublicclaims.public.created", name).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<PublicClaimProject> projects = PublicClaimSavedData.get(player.server).manageableBy(player.getUUID());
        if (projects.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("ftbpublicclaims.public.list_empty").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        source.sendSuccess(() -> Component.translatable("ftbpublicclaims.public.list_header").withStyle(ChatFormatting.AQUA), false);
        for (PublicClaimProject project : projects) {
            source.sendSuccess(() -> Component.literal("- " + project.name())
                    .append(project.isOwner(player.getUUID())
                            ? Component.translatable("ftbpublicclaims.public.owner_suffix")
                            : Component.translatable("ftbpublicclaims.public.manager_suffix"))
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return projects.size();
    }

    private static int selectPersonal(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ModNetwork.selectPersonal(player);
        source.sendSuccess(() -> Component.translatable("ftbpublicclaims.public.selected_personal").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int select(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PublicClaimProject project = findProject(source, name).orElse(null);
        if (project == null) {
            return 0;
        }
        if (!project.canManage(player.getUUID())) {
            return fail(source, "ftbpublicclaims.public.not_manager");
        }

        ModNetwork.selectProject(player, project);
        source.sendSuccess(() -> Component.translatable("ftbpublicclaims.public.selected_project", project.name())
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int delete(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PublicClaimProject project = findProject(source, name).orElse(null);
        if (project == null) {
            return 0;
        }
        if (!project.isOwner(player.getUUID())) {
            return fail(source, "ftbpublicclaims.public.owner_only");
        }

        FTBServerTeamBridge.find(project.teamId()).ifPresent(team -> FTBServerTeamBridge.delete(source, team));
        PublicClaimSavedData.get(player.server).remove(project.id());
        ModNetwork.selectPersonal(player);
        syncAffectedPlayers(player, project);
        source.sendSuccess(() -> Component.translatable("ftbpublicclaims.public.deleted", project.name()).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int setManager(CommandSourceStack source, String name, ServerPlayer target, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PublicClaimProject project = findProject(source, name).orElse(null);
        if (project == null) {
            return 0;
        }
        if (!project.isOwner(player.getUUID())) {
            return fail(source, "ftbpublicclaims.public.owner_only");
        }
        if (project.isOwner(target.getUUID())) {
            return fail(source, "ftbpublicclaims.public.owner_is_manager");
        }

        boolean shouldChange = enabled
                ? !project.managers().contains(target.getUUID())
                : project.managers().contains(target.getUUID());
        if (!shouldChange) {
            return fail(source, enabled
                    ? "ftbpublicclaims.public.already_manager"
                    : "ftbpublicclaims.public.not_manager");
        }

        ServerTeam team = FTBServerTeamBridge.find(project.teamId()).orElse(null);
        if (team == null) {
            return fail(source, "ftbpublicclaims.public.missing_team");
        }
        FTBServerTeamBridge.setManager(team, target.getUUID(), enabled);
        if (enabled) {
            project.addManager(target.getUUID());
        } else {
            project.removeManager(target.getUUID());
        }
        PublicClaimSavedData.get(player.server).changed();
        ModNetwork.sendProjects(player);
        ModNetwork.sendProjects(target);

        source.sendSuccess(() -> Component.translatable(
                enabled ? "ftbpublicclaims.public.manager_added" : "ftbpublicclaims.public.manager_removed",
                target.getDisplayName(), project.name()
        ).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int addExtraClaimChunks(CommandSourceStack source, String name, int amount, int maximum)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!Config.publicClaimsEnabled()) {
            return fail(source, "ftbpublicclaims.public.disabled");
        }

        PublicClaimProject project = findProject(source, name).orElse(null);
        if (project == null) {
            return 0;
        }
        if (!project.canManage(player.getUUID())) {
            return fail(source, "ftbpublicclaims.public.not_manager");
        }

        ServerTeam team = FTBServerTeamBridge.find(project.teamId()).orElse(null);
        if (team == null) {
            return fail(source, "ftbpublicclaims.public.missing_team");
        }
        return FTBServerTeamBridge.addExtraClaimChunks(team, amount, maximum) ? 1 : 0;
    }

    private static Optional<PublicClaimProject> findProject(CommandSourceStack source, String name) {
        Optional<PublicClaimProject> project = PublicClaimSavedData.get(source.getServer()).findByName(name);
        if (project.isEmpty()) {
            source.sendFailure(Component.translatable("ftbpublicclaims.public.not_found", name).withStyle(ChatFormatting.RED));
        }
        return project;
    }

    private static void syncAffectedPlayers(ServerPlayer owner, PublicClaimProject project) {
        ModNetwork.sendProjects(owner);
        for (UUID managerId : project.managers()) {
            ServerPlayer manager = owner.server.getPlayerList().getPlayer(managerId);
            if (manager != null) {
                ModNetwork.sendProjects(manager);
            }
        }
    }

    private static int fail(CommandSourceStack source, String translationKey) {
        source.sendFailure(Component.translatable(translationKey).withStyle(ChatFormatting.RED));
        return 0;
    }
}
