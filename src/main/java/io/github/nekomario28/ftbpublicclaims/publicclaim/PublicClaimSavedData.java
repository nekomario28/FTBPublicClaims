package io.github.nekomario28.ftbpublicclaims.publicclaim;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbteams.data.ServerTeam;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PublicClaimSavedData extends SavedData {
    private static final String DATA_NAME = "ftbpublicclaims_public_claims";
    private static final Factory<PublicClaimSavedData> FACTORY = new Factory<>(
            PublicClaimSavedData::new,
            PublicClaimSavedData::load,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<UUID, PublicClaimProject> projects = new LinkedHashMap<>();

    public static PublicClaimSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static PublicClaimSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PublicClaimSavedData data = new PublicClaimSavedData();
        ListTag projectTags = tag.getList("projects", Tag.TAG_COMPOUND);
        for (int i = 0; i < projectTags.size(); i++) {
            PublicClaimProject project = PublicClaimProject.load(projectTags.getCompound(i));
            data.projects.put(project.id(), project);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag projectTags = new ListTag();
        projects.values().stream().findFirst().ifPresent(project -> projectTags.add(project.save()));
        tag.put("projects", projectTags);
        return tag;
    }

    public PublicClaimProject getOrCreateGlobal(CommandSourceStack source) throws CommandSyntaxException {
        PublicClaimProject existing = projects.values().stream().findFirst().orElse(null);
        if (existing != null) {
            FTBServerTeamBridge.find(existing.teamId()).ifPresent(FTBServerTeamBridge::applySharedProperties);
            if (projects.size() > 1) {
                projects.clear();
                projects.put(existing.id(), existing);
                setDirty();
            }
            return existing;
        }

        ServerTeam team = FTBServerTeamBridge.createShared(source);
        PublicClaimProject project = PublicClaimProject.create(team.getId());
        projects.put(project.id(), project);
        setDirty();
        return project;
    }

    public Optional<PublicClaimProject> global() {
        return projects.values().stream().findFirst();
    }

    public Optional<PublicClaimProject> find(UUID id) {
        return Optional.ofNullable(projects.get(id));
    }

    public List<PublicClaimProject> manageableBy(UUID playerId) {
        return global().map(List::of).orElseGet(List::of);
    }
}
