# Documentação — SignallQ Android

- **Status:** ativo
- **Última validação:** 2026-07-16
- **Fonte de verdade:** este arquivo é só um índice — não repete conteúdo de nenhum documento
  listado abaixo. Para os fatos em si, abra o documento apontado.
- **Escopo:** ponto de entrada de toda a documentação viva do monorepo SignallQ
  (`7ALabs/SignallQ`)
- **Responsável:** Claudete (dono do processo de documentação viva), Rhodolfo (manutenção/QA)

> Nota de marca: a UI e a documentação usam **SignallQ**. O package/namespace atual é
> `io.signallq.app` (renomeado de `io.veloo.app` em 2026-06-28). Outros identificadores técnicos
> permanecem por compatibilidade de infra: repo GitHub `7ALabs/SignallQ`, worker Cloudflare
> `linka-ai-diagnosis-worker`, banco DataStore `linkaPreferencias`.

---

## Documentos centrais (raiz)

| Documento | Conteúdo |
|---|---|
| [`FUNCIONAL.md`](./FUNCIONAL.md) | O que o app faz — navegação, telas, funcionalidades por domínio, permissões, feature flags visíveis ao usuário, limitações conhecidas |
| [`TECNICO.md`](./TECNICO.md) | Como o app funciona — stack, build, Workers Cloudflare, persistência, analytics, segurança, release |
| [`DESIGN_SYSTEM.md`](./DESIGN_SYSTEM.md) | Cores, tipografia, espaçamento, raios, componentes, tokens — Android |
| [`ARQUITETURA/README.md`](./ARQUITETURA/README.md) | Visão de sistema, componentes, fluxo de dados, diagrama de dependências entre módulos, riscos arquiteturais |
| [`ARQUITETURA/MODULOS/`](./ARQUITETURA/MODULOS/) | Um documento por módulo Gradle real (16) — responsabilidade, dependências, consumidores, riscos |
| [`CONTRATOS/openapi/`](./CONTRATOS/openapi/) | Contrato OpenAPI 3.0 de cada um dos 5 Workers Cloudflare |
| [`CONTRATOS/schemas/README.md`](./CONTRATOS/schemas/README.md) | Índice de schemas reais (Room, D1, analytics) — referencia a origem, não copia |
| [`RELEASES.md`](./RELEASES.md) | Histórico de releases a partir do git log real |

---

## Outras pastas (ficam onde estão — não force conteúdo funcional/técnico/design para dentro delas)

| Pasta | Conteúdo |
|---|---|
| `ai/` | Fluxo de trabalho dos agentes de IA (resumos apontadores de `.claude/CLAUDE.md`/`.claude/agents/`) |
| `plataforma/` | **Visão-alvo** consolidada do ecossistema (pacote **v5**): SignallQ + SignallQ Pro + SignallQ Admin (+ Portal + Nethal), monorepo-alvo `signallq-platform`. É **proposta**, não o estado do código — cada doc marca ATUAL vs ALVO. Começar por [`LEIA-ME_v5.md`](./plataforma/LEIA-ME_v5.md) + [`00_CANONICO_v5.md`](./plataforma/00_CANONICO_v5.md) (fonte única de nomes/eventos/tabelas/paleta). Fonte da validação doc-vs-código: `00_CHANGELOG_e_Validacao_Cruzada_v5.md` |
| `decisions/` | ADRs — decisões arquiteturais registradas (`ADR-001` a `ADR-007`) |
| `functional/` | Specs funcionais que não migraram para `FUNCIONAL.md`: `FEATURE_FLAGS.md` (metade painel Admin), `JOGOS_TESTE_CONEXAO_SPEC.md` (spec de referência do domínio Jogos) |
| `technical/` | Docs técnicos pontuais que não migraram para `TECNICO.md`/`ARQUITETURA/`: contratos em prosa (`admin-api-schema.md`, `analytics-events.md`, `analytics-events-schema.md`), auditorias de OpenAPI (`AUTHENTICATION_OPENAPI.md`, `FIREBASE_INTEGRATION_OPENAPI.md`, `OPENAPI_VALIDATION.md`), mapas de campo de equipamento (`INTELBRAS_RX1500_FIELD_MAP.md`, `NOKIA_GPON_FIELD_MAP.md`, `TPLINK_ARCHER_ROUTER_FIELD_MAP.md`), planos ativos (`PLANO_UNIFICACAO_TOPOLOGIA_WIFI_2026-07-15.md`, `TOBE_MD3_APP_PLANO_IMPLEMENTACAO.md`, `MIRO_PUBLICATION_PLAN_SIG172.md`, `PATH_CONSOLIDATION_SIG168.md`), doc de componente (`PING_EXECUTOR_ARCHITECTURE.md`), convenções de código (`CODE_PATTERNS.md`), doc de feature técnica (`MONITORAMENTO_PASSIVO.md`, `NOTIFICACOES.md` — mecanismo depreciado), mapa de telas (`SCREEN_MAP.md`), fluxo de IA (`AI_FLOW.md`) |
| `operations/` | Release, deploy, versionamento, ambientes, runbooks, custos, matriz de dispositivos, processo do squad |
| `legal/` | Termos de uso, política de privacidade |
| `design-system/` | Histórico — conteúdo vigente consolidado em `DESIGN_SYSTEM.md`; `DECISAO_*` mantidos aqui como registro de decisão |
| `testing/` | `firebase-test-cases.yaml` |
| `_archive/` | Material histórico — **nunca** usar como fonte de verdade atual |

---

## Como usar este índice

1. Pergunta é "o que o app faz"? → `FUNCIONAL.md`.
2. Pergunta é "como o app é construído/integrado"? → `TECNICO.md`, depois `ARQUITETURA/` para
   detalhe por módulo.
3. Pergunta é "qual o contrato de uma API"? → `CONTRATOS/openapi/`.
4. Pergunta é "qual o valor de um token de design"? → `DESIGN_SYSTEM.md`.
5. Pergunta é "por que essa decisão foi tomada"? → `decisions/` (ADRs).
6. Pergunta é "como faço deploy/release"? → `operations/`.
7. Nenhum desses documentos responde? Busque por símbolo (Grep) no código antes de assumir que a
   documentação está completa — código é sempre a fonte de verdade final (ver
   `.claude/rules/higiene-e-padronizacao-repositorio.md`, seção 3).
