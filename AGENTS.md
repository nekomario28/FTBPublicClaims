# Agent Engineering Entry Point

FTBPublicClaims is an independent public-claim addon with a long-lived NeoForge 1.21.1 port in active review. Preserve claim ownership/capacity isolation, FTB API/ABI compatibility, persistence, client/server behavior, and the distinction between default-branch history and active port carriers.

## Read first

1. `README.md` on the branch being changed for the exact claim model and supported runtime.
2. Live GitHub PR state before choosing a base. The NeoForge port and its stacked fixes may be substantially newer than the repository default branch.
3. Existing GameTests, RCON/client evidence, JAR audit, and compatibility notes before changing runtime or dependency policy.
4. Exact FTB Chunks / FTB Teams / FTB Library API or ABI evidence when touching map integration or team ownership internals.

## Product authority

- Public claims and personal/party claims are distinct ownership/capacity domains. Do not merge or convert one into the other implicitly.
- Public capacity, public claimed chunks, and the global public ownership container must not alter `/buyclaim` personal capacity or ownership semantics.
- Existing claim ownership must not be overwritten during migration/recovery merely to simplify legacy conversion.
- Server-side protection, client map target selection, networking, persistence, and restart behavior are one product contract; compile-only success is insufficient.

## Version and compatibility discipline

- Follow the exact dependency policy recorded on the active port rather than copying stale version pins from an older branch.
- Where integration depends on private/internal FTB ABI, isolate that dependency and keep executable/API evidence tied to the exact supported range.
- Do not widen compatibility metadata beyond versions that the runtime/JAR/client evidence actually supports.
- Keep distributable JAR audits fail-closed for stale metadata, leaked test classes, invalid language resources, and dependency-range drift.

## Active-carrier discipline

- Do not fold temporary Actions-storage pilots or other project-incubator execution lanes into product history; those PRs are evidence transports and intentionally unmerged.
- Do not rewrite the large active NeoForge port carrier for an unrelated docs/agent migration. Check its exact head, base, and stacked PRs before product work.
- Treat this root entrypoint on the default branch as routing guidance; live port PRs remain authoritative for current implementation state until the port is integrated.

## Validation

For runtime changes, preserve the strongest applicable gates already established by the active port:

- clean Java/NeoForge build and distributable-JAR audit;
- combined runtime with BuyClaimChunks when capacity-isolation semantics are affected;
- real command/server-state assertions;
- GameTests for claim/protection rules;
- save/stop/second-JVM persistence where durable state changes;
- real client/map interaction where UI/network claim targeting changes.

A successful isolated helper/unit test does not authorize weakening ownership, protection, persistence, or cross-mod capacity boundaries.
