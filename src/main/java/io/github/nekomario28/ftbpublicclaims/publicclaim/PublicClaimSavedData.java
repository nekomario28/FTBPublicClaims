package io.github.nekomario28.ftbpublicclaims.publicclaim;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
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
        projects.values().forEach(project -> projectTags.add(project.save()));
        tag.put("projects", projectTags);
        return tag;
    }

    public Optional<PublicClaimProject> find(UUID id) {
        return Optional.ofNullable(projects.get(id));
    }

    public Optional<PublicClaimProject> findByName(String name) {
        return projects.values().stream().filter(project -> project.name().equalsIgnoreCase(name)).findFirst();
    }

    public List<PublicClaimProject> manageableBy(UUID playerId) {
        List<PublicClaimProject> result = new ArrayList<>();
        for (PublicClaimProject project : projects.values()) {
            if (project.canManage(playerId)) {
                result.add(project);
            }
        }
        result.sort(Comparator.comparing(PublicClaimProject::name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public long countOwnedBy(UUID playerId) {
        return projects.values().stream().filter(project -> project.isOwner(playerId)).count();
    }

    public void add(PublicClaimProject project) {
        projects.put(project.id(), project);
        setDirty();
    }

    public boolean remove(UUID id) {
        if (projects.remove(id) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public void changed() {
        setDirty();
    }
}
