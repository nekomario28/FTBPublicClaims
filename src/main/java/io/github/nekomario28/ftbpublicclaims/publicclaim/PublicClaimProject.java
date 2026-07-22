package io.github.nekomario28.ftbpublicclaims.publicclaim;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PublicClaimProject {
    private final UUID id;
    private final String name;
    private final UUID teamId;
    private final UUID ownerId;
    private final Set<UUID> managers;

    private PublicClaimProject(UUID id, String name, UUID teamId, UUID ownerId, Set<UUID> managers) {
        this.id = id;
        this.name = name;
        this.teamId = teamId;
        this.ownerId = ownerId;
        this.managers = new HashSet<>(managers);
    }

    public static PublicClaimProject create(String name, UUID teamId, UUID ownerId) {
        return new PublicClaimProject(UUID.randomUUID(), name, teamId, ownerId, Set.of());
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public UUID teamId() {
        return teamId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public Set<UUID> managers() {
        return Set.copyOf(managers);
    }

    public boolean isOwner(UUID playerId) {
        return ownerId.equals(playerId);
    }

    public boolean canManage(UUID playerId) {
        return isOwner(playerId) || managers.contains(playerId);
    }

    public boolean addManager(UUID playerId) {
        return !isOwner(playerId) && managers.add(playerId);
    }

    public boolean removeManager(UUID playerId) {
        return managers.remove(playerId);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putUUID("teamId", teamId);
        tag.putUUID("ownerId", ownerId);
        ListTag managerTags = new ListTag();
        for (UUID managerId : managers) {
            CompoundTag managerTag = new CompoundTag();
            managerTag.putUUID("id", managerId);
            managerTags.add(managerTag);
        }
        tag.put("managers", managerTags);
        return tag;
    }

    public static PublicClaimProject load(CompoundTag tag) {
        Set<UUID> managers = new HashSet<>();
        ListTag managerTags = tag.getList("managers", Tag.TAG_COMPOUND);
        for (int i = 0; i < managerTags.size(); i++) {
            CompoundTag managerTag = managerTags.getCompound(i);
            if (managerTag.hasUUID("id")) {
                managers.add(managerTag.getUUID("id"));
            }
        }

        return new PublicClaimProject(
                tag.getUUID("id"),
                tag.getString("name"),
                tag.getUUID("teamId"),
                tag.getUUID("ownerId"),
                managers
        );
    }
}
