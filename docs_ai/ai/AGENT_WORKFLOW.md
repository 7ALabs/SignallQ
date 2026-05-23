# Agent Workflow

## Agent Roles

- **CAIO_ENGINEER**: Implementation, `.orbit/agents/camilo/AGENT.md`
- **CLAUDIO_TECH_LEAD**: Architecture, `.orbit/agents/claudio/AGENT.md`
- **LIA_UX_LEAD**: Design/UX, `.orbit/agents/lia/AGENT.md`

## Workflow Stages

1. **Understand**: Gather context from `docs_ai/`
2. **Research**: `grep`, `read`, explore codebase
3. **Plan**: `ai/TASK_BREAKDOWN.md` for decomposition
4. **Implement**: Edit code/docs, run builds
5. **Verify**: `./gradlew build`, tests, linting
6. **Handoff**: Follow `ai/HANDOFF_RULES.md`

## Key Paths

- Agent definitions: `.orbit/agents/[name]/`
- Skills: `.orbit/skills/`
- Workflows: `.orbit/workflow/`
- Documentation: `docs_ai/ai/`

## Build/Verify

- Gradle: `./gradlew build`
- Lint: `./gradlew lint`
- APK: `./gradlew assembleDebug`

See `technical/BUILD_SYSTEM.md` for details
