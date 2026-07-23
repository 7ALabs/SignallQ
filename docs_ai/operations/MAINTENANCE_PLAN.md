# Plano De Atualizacao - Documentos, Agentes E Skills

- **Status:** ativo
- **Última validação:** 2026-07-23
- **Escopo:** cadência/rotina de atualização de documentação, não conteúdo técnico em si

## Rotina por mudanca

1. Atualizar codigo e testes no modulo Gradle afetado.
2. Atualizar `docs_ai/FUNCIONAL.md` quando mudar fluxo, tela ou comportamento (`docs_ai/functional/`
   só para specs pontuais que ainda não migraram — ver `.claude/rules/higiene-e-padronizacao-repositorio.md`, seção 10).
3. Atualizar `docs_ai/TECNICO.md` e `docs_ai/ARQUITETURA/` quando mudar arquitetura, modulo, servico,
   storage ou integracao (`docs_ai/technical/` idem, só residual).
4. Atualizar `docs_ai/DESIGN_SYSTEM.md` quando mudar componente visual, token, navegacao ou guideline.
5. Atualizar `docs_ai/operations/` quando mudar build, release, ambiente, script ou versionamento.
6. Registrar impacto em `CHANGELOG.md` quando a mudanca for entregavel.
7. Quando gerar APK, seguir `docs_ai/operations/APK_OUTPUT_POLICY.md`.

## Agentes

- Manter `AGENTS.md` como contrato curto de operacao.
- Manter `docs_ai/ai/AGENT_WORKFLOW.md` como fluxo detalhado.
- Quando criar novo agente, documentar objetivo, entradas, saidas, limites e validacao esperada em `docs_ai/ai/`.
- Quando remover agente, apagar referencias dos comandos em `.claude/commands/` e dos documentos em `docs_ai/ai/`.

## Skills e comandos

- Comandos Claude versionados ficam em `.claude/commands/`.
- Scripts executaveis ficam em `scripts/`, agrupados por dominio.
- Toda skill ou comando novo deve declarar:
  - quando usar;
  - arquivos que pode alterar;
  - comando de validacao;
  - documento que deve ser atualizado.

## Cadencia sugerida

- A cada PR ou pacote de mudanca: revisar docs afetadas.
- Antes de release: rodar checklist de `docs_ai/operations/RELEASE.md`.
- Mensalmente: revisar arquivos antigos, duplicados ou marcados como legacy.

## Backlog pos-migracao

- Criar um repositorio Git novo para `C:\Projetos\SignallQ Android`.
- Decidir se `integrations/cloudflare/ai-diagnosis-worker` vira workspace proprio ou permanece como integracao local.
- [feito 2026-07-04] Revisar `docs_ai` para remover mencoes residuais ao Flutter/PWA/iOS apos descontinuacao do PWA e do app iOS.
- Manter os segredos locais migrados fora do Git e validar que `.gitignore` continua cobrindo `.env`, `key.properties`, keystores, certificados e chaves.
