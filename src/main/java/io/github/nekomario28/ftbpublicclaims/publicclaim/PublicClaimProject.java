package io.github.nekomario28.ftbpublicclaims.publicclaim;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public final class PublicClaimProject {
    public static final String GLOBAL_NAME = "public";

    private final UUID id;
    private final UUID teamId;

    private PublicClaimProject(UUID id, UUID teamId) {
        this.id = id;
        this.teamId = teamId;
    }

    public static PublicClaimProject create(UUID teamId) {
        return new PublicClaimProject(UUID.randomUUID(), teamId);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return GLOBAL_NAME;
    }

    public UUID teamId() {
        return teamId;
    }

    public boolean isOwner(UUID playerId) {
        return false;
    }

    public boolean canManage(UUID playerId) {
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putUUID("teamId", teamId);
        return tag;
    }

    public static PublicClaimProject load(CompoundTag tag) {
        return new PublicClaimProject(tag.getUUID("id"), tag.getUUID("teamId"));
    }
}
