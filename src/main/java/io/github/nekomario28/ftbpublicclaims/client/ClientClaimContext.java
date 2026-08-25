package io.github.nekomario28.ftbpublicclaims.client;

import io.github.nekomario28.ftbpublicclaims.network.packet.ProjectSyncPacket;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ClientClaimContext {
    private static List<ProjectSyncPacket.ProjectSummary> projects = List.of();
    private static UUID selectedProjectId;

    private ClientClaimContext() {
    }

    public static void updateProjects(List<ProjectSyncPacket.ProjectSummary> newProjects) {
        projects = List.copyOf(newProjects);
        if (selectedProjectId != null && find(selectedProjectId).isEmpty()) {
            selectedProjectId = null;
        }
    }

    public static void select(UUID projectId) {
        selectedProjectId = projectId != null && find(projectId).isPresent() ? projectId : null;
    }

    public static Optional<ProjectSyncPacket.ProjectSummary> selectedProject() {
        return selectedProjectId == null ? Optional.empty() : find(selectedProjectId);
    }

    public static void cycle() {
        if (projects.isEmpty()) {
            selectedProjectId = null;
            return;
        }
        if (selectedProjectId == null) {
            selectedProjectId = projects.get(0).id();
            return;
        }
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).id().equals(selectedProjectId)) {
                selectedProjectId = i + 1 < projects.size() ? projects.get(i + 1).id() : null;
                return;
            }
        }
        selectedProjectId = null;
    }

    public static Component buttonLabel() {
        return selectedProject().isPresent()
                ? Component.translatable("ftbpublicclaims.public.target_public")
                : Component.translatable("ftbpublicclaims.public.target_personal_short");
    }

    private static Optional<ProjectSyncPacket.ProjectSummary> find(UUID id) {
        return projects.stream().filter(project -> project.id().equals(id)).findFirst();
    }
}
