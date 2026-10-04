# AI Development Workflow

**Status:** Required engineering workflow  
**Applies to:** This repository

## Purpose

Preserve a consistent division of responsibility between ChatGPT, implementation agents, and local validation so architecture, implementation, and review do not collapse into one opaque step.

## Default Roles

### ChatGPT

ChatGPT owns:

- architecture
- design
- requirements
- ADRs
- sprint specifications
- test strategy
- implementation handoff instructions
- code/diff review
- defect analysis
- merge-readiness review
- release history
- portfolio/consulting documentation

ChatGPT may directly edit documentation and planning artifacts.

### Codex / Claude

Codex or Claude owns application implementation unless the user explicitly directs ChatGPT to implement a specific code change.

Implementation work includes:

- application source code
- production configuration that changes executable behavior
- unit/UI tests
- migrations
- refactors
- build-system changes required for implementation

### User / Local Toolchain

The user owns execution and validation in the local development environment, including:

- Xcode build
- simulator/device run
- physical-device validation
- App Store validation
- local command output when needed

## Hard Guardrail

ChatGPT must not directly modify application source code or test code merely because repository-write tools are available.

Before any source-code write, ChatGPT must verify that the user explicitly requested ChatGPT itself to implement that specific change.

If not, ChatGPT must instead produce a Codex/Claude handoff.

## Standard Delivery Loop

1. ChatGPT defines or updates the implementation specification.
2. ChatGPT creates an implementation handoff containing:
   - repository
   - branch
   - files/areas expected to change
   - functional requirements
   - architecture constraints
   - compatibility constraints
   - required tests
   - acceptance criteria
3. Codex/Claude implements the change.
4. User builds/runs/tests locally.
5. ChatGPT reviews:
   - diff
   - test results
   - runtime behavior
   - regressions
6. If defects exist, ChatGPT writes corrective implementation instructions for Codex/Claude.
7. Once validated, ChatGPT may update release, sprint, ADR, and portfolio documentation and coordinate merge readiness.

## Exceptions

ChatGPT may implement source code only when the user explicitly asks ChatGPT to do so for that task.

An earlier general statement such as "continue" or "move forward" is not sufficient authorization to bypass this workflow.

## Repository Hygiene

- Keep GitHub current so state is recoverable after interruption.
- Use dedicated feature/fix branches.
- Do not mix unrelated changes into the active branch.
- Treat generated project files according to the repository's established generation workflow.
- Preserve test and device-validation evidence for meaningful defects and shipped behavior.

## Review Principle

Implementation is not considered complete because code was written.

Completion requires:

- successful build
- required automated tests
- required device/runtime validation
- ChatGPT review against the sprint or defect specification
- documented merge evidence where appropriate
