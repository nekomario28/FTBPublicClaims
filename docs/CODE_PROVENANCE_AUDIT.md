# Code provenance audit

Status: **APPLICATION SOURCE NO_ACTION AT CURRENT EVIDENCE BOUNDARY**

Audit date: 2026-08-25
Audited head: `8caaab65266a94e7bdedc6ad2f66030c7e394edf`
Application source boundary: initial commit `ffa938af196e1f81cc3d23b1362938e18c91a1f3`

## Scope

This record distinguishes three provenance categories that should not be collapsed into one Git Author decision:

1. FTBPublicClaims-specific application code;
2. code that integrates with version-pinned FTB APIs/internals;
3. standard Forge MDK / Gradle scaffolding and framework idioms.

The current default branch is exactly one commit ahead of the initial implementation, and that later commit adds only `AGENTS.md`. Therefore all current production Java application code is unchanged from the initial application boundary and can be audited against one stable snapshot.

## Application source inventory

The audited tree contains 13 production Java files:

- `Config.java`
- `FTBPublicClaims.java`
- `client/ClientClaimContext.java`
- `client/PublicClaimClientEvents.java`
- `network/ModNetwork.java`
- `network/packet/ProjectSyncPacket.java`
- `network/packet/PublicChunkChangePacket.java`
- `network/packet/SelectProjectPacket.java`
- `publicclaim/FTBServerTeamBridge.java`
- `publicclaim/PublicClaimCommand.java`
- `publicclaim/PublicClaimProject.java`
- `publicclaim/PublicClaimSavedData.java`
- `publicclaim/PublicClaimService.java`

## Explicit upstream/API sources

The repository README pins the compatibility/source references used by the implementation:

- FTB Chunks `v2001.3.6`
- FTB Teams `v2001.3.1`
- FTB Library `v2001.2.12`
- Architectury API `9.1.12`

The code uses those projects as APIs and, for the isolated FTB Teams bridge, selected version-specific implementation details. API use and source consultation are not by themselves evidence that implementation bodies were copied.

## FTB-facing source comparisons

Two highest-risk FTB-facing surfaces were already compared directly against the pinned upstream implementations:

### `FTBServerTeamBridge`

The bridge uses FTB Teams implementation classes/private state because the pinned release does not expose the required server-team creation operation as a suitable public API.

A source-level comparison against the pinned FTB Teams server-team creation implementation found no class-level or method-level body copy. The local bridge implements its own property configuration, manager/member handling, claim cleanup, lookup, deletion and extra-claim behavior around the pinned internal surface.

Classification: **API/internal integration, no direct implementation copy established.**

### `PublicClaimClientEvents`

The client integration was compared with the pinned FTB Chunks `ChunkScreen` surface. It interacts with the existing screen and FTB behavior but does not reproduce the inspected upstream class implementation.

Classification: **UI/API integration, no direct implementation copy established.**

## Remaining application-code audit

The other application classes were inspected as a complete current-source set.

The domain model, command tree, persistence model, project/manager ownership, packet schemas, bounded claim-change protocol, adjacency/range policy and claim-service control flow are FTBPublicClaims-specific. Representative distinctive symbols were searched across indexed GitHub code:

- `touchesProjectClaim`
- `PublicClaimSavedData`
- `MAX_PUBLIC_CHUNKS_PER_PROJECT`
- `PublicChunkChangePacket`
- `PublicClaimCommand`

The material results resolve to this repository and its own internal references; no external source implementation matching these project-specific surfaces was identified.

`PublicClaimService` is particularly important because it calls FTB Chunks APIs directly. Its batching, range ordering, adjacency progression, problem aggregation and project-team ownership checks are local orchestration around those APIs rather than a copied FTB source class found in the pinned upstream relation.

`PublicClaimProject` and `PublicClaimSavedData` use ordinary Minecraft NBT/SavedData APIs with a project-specific schema (`id`, `name`, `teamId`, `ownerId`, `managers`, `projects`). Framework API shape is not treated as evidence of copied implementation.

The packet classes use standard Forge `SimpleChannel` / `NetworkEvent.Context` idioms, but their payloads, limits and project-selection semantics are local to this mod.

## Forge MDK / scaffold boundary

The initial repository also contains standard Forge/Gradle scaffolding. This must not be mistaken for independently invented application code, and it also must not be used to assign the entire initial commit to a Forge/Gradle contributor.

Direct comparison with `MinecraftForge/MDKExamples` confirms shared template/framework surfaces such as:

- `ForgeConfigSpec.Builder` config organization;
- `@Mod(...)` entrypoint structure;
- `LogUtils.getLogger()`;
- `FMLJavaModLoadingContext` / `registerConfig` usage;
- conventional ForgeGradle build/run configuration and Gradle wrapper material.

The local `Config` values, mod events and FTBPublicClaims domain behavior differ materially from the example application. These are best classified as **template/framework-derived scaffolding and idioms**, not a transplanted FTB application implementation.

Generated or distributed toolchain material (for example the Gradle wrapper) has its own upstream/tool provenance. Its presence inside a mixed initial bootstrap does not justify rewriting the bootstrap commit Author away from the author of the project-specific application work.

## Git Author decision

Initial implementation commit:

- local commit: `ffa938af196e1f81cc3d23b1362938e18c91a1f3`
- local Author: `Yuu <206304251+nekomario28@users.noreply.github.com>`

Classification: **NO_ACTION for Git Author correction at the current evidence boundary.**

Reasons:

1. no material FTB implementation body with a different verified source Author was found in the 13-file application inventory;
2. the two highest-risk FTB-specific internal/UI surfaces were directly compared and classified as integration rather than source transplantation;
3. project-specific class/method identifiers and control flow do not resolve to an external copied implementation in the bounded public-code search;
4. Forge/Gradle template scaffolding is mixed with local application code in the root commit, so assigning that whole commit to a template contributor would create a false Author statement;
5. framework idioms, generated/tooling files and application authorship require separate provenance records.

This is a bounded evidence conclusion, not proof that no unindexed/private historical source ever existed. Reopen the audit if a concrete donor repository/revision/path is later identified for a material application fragment.

## Future integration rule

For future direct imports into this repository:

- preserve identifiable upstream Git Author/AuthorDate when substantially unchanged code is transplanted;
- keep pure import and local adaptation commits separate where practical;
- record exact source repository, revision and path;
- treat templates/generated scaffolding separately from application-code authorship;
- do not infer an Author solely from API similarity or a dependency name.

## License boundary

This document records Git/source provenance. It is not a substitute for dependency, template or redistribution license review. Existing dependency licenses and notices must continue to be handled according to the actual upstream terms.
