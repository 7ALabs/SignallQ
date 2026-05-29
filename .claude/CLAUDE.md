# Project Memory

Instructions here apply to this project and are shared with team members.

## Design System

Toda UI deste projeto segue o **Linka Design System** (`.claude/skills/linka-design/`).
Antes de criar ou editar telas/componentes, consulte a skill `linka-design` e use os tokens de `colors_and_type.css` / `LinkaTheme.kt` como fonte de verdade.

Não-negociáveis:
- Material 3 claro, acento violeta `#6C2BFF`, semântica de status verde/âmbar/vermelho
- Ícones Material Symbols (Outlined), tipo Roboto, grid 8dp, card radius 16dp, flat (sem sombras pesadas)
- Superfícies Orbit (IA) sempre escuras (`#0D0D1A` / `#1A0B2E` / `#1E1130`)
- Copy em PT-BR com "você", sentence case em títulos, UPPERCASE em overlines, SEM emoji
- Métrica crua sempre acompanhada de veredito humano (Excelente/Bom/Regular/Fraco/Forte)
- Separador inline: ponto médio `·`

Referência rápida de tokens: ver `.claude/skills/linka-design/HANDOFF_README.md` (tabela completa de cores, espaçamento, raios e tipografia).

## Release Process

Quando o usuário pedir para subir/deploy/publicar no Firebase, seguir OBRIGATORIAMENTE nesta ordem:

1. **Commit** — stage todos os arquivos modificados, commit com mensagem descritiva
2. **Push** — `git push origin main` para sincronizar GitHub
3. **Clean build** — `./gradlew clean assembleRelease --no-build-cache` (NUNCA usar cache em release)
4. **Upload** — `./gradlew appDistributionUploadRelease`

Nunca pular etapas. Nunca fazer assembleRelease sem clean + --no-build-cache antes. O cache do Gradle já causou builds desatualizados no Firebase.

Worker Cloudflare: quando houver mudanças em `integrations/cloudflare/ai-diagnosis-worker/src/`, fazer `npx wrangler deploy` ANTES do commit.

## Context

