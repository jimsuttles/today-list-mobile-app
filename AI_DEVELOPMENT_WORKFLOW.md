# AI Development Workflow

**Status:** Required engineering workflow  
**Applies to:** This repository

## Purpose

Use the fastest reliable engineering loop while preserving independent verification.

The default model is now:

**single-owner implementation, independent verification**

ChatGPT normally owns architecture, specification, implementation, first-pass tests, code review, repository updates, and documentation. The user owns local build/runtime validation. A separate adversarial verification pass is mandatory before merge and must not treat the implementation's own tests as proof of correctness.

## Default Roles

### ChatGPT

ChatGPT owns:

- architecture
- design
- requirements
- ADRs
- sprint specifications
- implementation
- application source code
- production configuration required by the implementation
- unit/UI tests
- migrations
- refactors
- build-system changes required for implementation
- first-pass defect correction
- diff/code review
- repository updates
- merge-readiness review
- release history
- portfolio/consulting documentation

ChatGPT may directly implement source and test changes when the work can be completed safely through the connected repository workflow.

### User / Local Toolchain

The user owns independent execution and product validation in the local environment, including:

- Xcode build
- simulator/device run
- physical-device validation
- App Store validation
- local command output when needed
- experiential confirmation that the implemented behavior matches the intended product behavior

### Codex / Claude

Codex or Claude is no longer the default implementation owner.

Use another implementation agent only when it provides clear leverage, such as:

- very large mechanical refactors
- work requiring local-only tooling unavailable to ChatGPT
- unusually broad repository changes where delegation is materially more efficient
- a deliberately independent review or test-design pass for higher-risk work

When used for independent verification, the secondary agent should review the implementation rather than simply repeat the same implementation assumptions.

## Hard Guardrails

1. **Do not reintroduce a mandatory ChatGPT → Codex/Claude implementation handoff.**
   ChatGPT directly implements by default.

2. **Do not merge on the strength of implementation-authored tests alone.**
   Green tests are necessary evidence, not sufficient proof.

3. **Every substantive implementation requires an independent adversarial verification pass before merge.**
   That pass must explicitly look for shared assumptions, omitted scenarios, lifecycle defects, persistence/replay issues, upgrade risks, and behavior not covered by the implementation's own tests.

4. **Physical-device/runtime validation remains independent evidence.**
   For user-visible mobile behavior, the user's Xcode/device validation is a required quality gate unless the change is purely non-runtime documentation or metadata.

5. **Do not silently waive the workflow.**
   If a task cannot meet one of these gates, state that explicitly before merge rather than treating the exception as implicit.

## Standard Delivery Loop

1. ChatGPT defines or updates the specification and acceptance criteria.
2. ChatGPT creates a dedicated feature/fix branch or verifies the active branch is appropriate.
3. ChatGPT implements the source, configuration, and tests.
4. ChatGPT reviews the actual diff against the specification.
5. ChatGPT performs a **fresh adversarial verification review** that is intentionally separate from implementation thinking. This review asks, at minimum:
   - What assumptions are shared by both the code and tests?
   - What state transitions or failure modes are untested?
   - What happens on cold launch, relaunch, retry, partial failure, stale data, missing metadata, deleted/completed records, and upgrade where relevant?
   - Are tests verifying externally observable outcomes or merely implementation details?
   - Could persistence, identity, idempotency, or migration behavior fail despite green tests?
   - Could an existing integration or production behavior regress?
6. ChatGPT adds or strengthens tests when the adversarial review identifies gaps.
7. The user builds/runs/tests locally and performs required simulator/physical-device validation.
8. ChatGPT reviews the validation evidence and any resulting defects.
9. Only after all gates pass does ChatGPT coordinate merge readiness and update release/sprint/portfolio documentation.

## Higher-Risk Verification

For changes with elevated risk, use an additional independent reviewer where useful. Examples include:

- persistence or schema migration
- App Group / entitlement changes
- identity or idempotency logic
- destructive operations
- cross-app transport
- authentication/security-sensitive behavior
- release migration from an installed production build
- broad refactors affecting multiple apps

A second model such as Codex or Claude may be used specifically as an adversarial reviewer or test designer. This is optional, not a mandatory implementation handoff.

## Repository Hygiene

- Keep GitHub current so state is recoverable after interruption.
- Use dedicated feature/fix branches.
- Do not mix unrelated changes into the active branch.
- Treat generated project files according to the repository's established generation workflow.
- Preserve test and device-validation evidence for meaningful defects and shipped behavior.
- Do not discard unrelated local changes without explicit user approval.

## Review Principle

Implementation is not complete because code was written or because its own tests passed.

Completion requires:

- successful build
- required automated tests
- independent adversarial verification
- required device/runtime validation
- final diff review against the specification
- documented merge evidence where appropriate

The operating principle is:

> **Single-owner implementation. Independent verification. No silent exceptions.**
