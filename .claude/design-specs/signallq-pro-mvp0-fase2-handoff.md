# Handoff de Design — SignallQ Pro MVP0, Fase 2 (Grupo 1 trimmed + Grupo 2 completo)

- **Status:** ativo
- **Última validação:** 2026-07-19
- **Fonte de verdade:** Lia (síntese) + Claudete (cross-check visual direto do protótipo real via
  DesignSync) — documento redigido por Camilo antes de implementar (issue #1161)
- **Escopo:** telas do Grupo 1 (trimmed, sem auth/assinatura) e Grupo 2 (núcleo — visita/ambiente/
  medição/diagnóstico) do SignallQ Pro
- **Responsável:** Camilo (implementação), Lia (design)
- **Documentos relacionados:** `docs_ai/pro-onhold/13_SignallQ_Pro_Arquitetura_e_Reaproveitamento_v1.md`
  §5 (mapa telas→módulo), `docs_ai/pro-onhold/09_SignallQ_Pro_Jornada_e_Fluxo_de_Telas_v5.md`

Fonte visual: protótipo Claude Design `69e53070-6aa8-485a-8d0a-5bfa36e1a08c`, design system
`77a19317-ea64-4e47-b55c-578eca776c09` (azul `#0B6CFF` + ciano `#006B76` + roxo `#6558E8`, M3, 2
temas).

Convenção de nome: PascalCase, conceito de domínio em português, sufixo estrutural em inglês
(`AtendimentoScreen.kt`, não `TelaAtendimento.kt`).

Regra transversal (Grupo 6 do protótipo — "Estados"): todo estado vazio/erro/carregando/sucesso vem
do componente `StateCard` do design system (`:pro:core:designsystem`) — nenhuma feature reimplementa
o próprio.

## Grupo 1 (trimmed — só o que entra no MVP0 sem auth/assinatura real)

- **1.1 Carregamento** — `CarregamentoScreen.kt`: logo do Pro + progress indicator M3, sem spinner
  genérico, sem placeholder fake.
- **1.2 Apresentação** — reduzida a 1 tela simples de boas-vindas (não é onboarding carrossel de
  vendas — isso pertence ao 1.9/1.10, cortados).
- **Cadastro básico do profissional** (substitui 1.5, SEM backend) — `CadastroProfissionalScreen.kt`:
  nome (obrigatório) + logo opcional (usado depois no laudo), salva local via Room
  (`:pro:core:database`). Só nome obrigatório — não exigir documento/endereço completo.
- **1.7 Permissões** — `PermissoesScreen.kt`: `ListRow` (ícone+texto+toggle) por permissão, NÃO um
  `Card` por item. Toggle chama a API real de permissão do Android, não simula estado.
- **1.8 Permissão bloqueada** — `PermissaoBloqueadaScreen.kt`: `StateCard` variante erro + botão pra
  Ajustes do sistema. Um `StateCard` só, sem duplicar com banner/ilustração.

Cortado deste grupo (fora de escopo, exige backend de auth real): 1.3 Login, 1.4 Recuperar senha,
1.5 Criar conta, 1.6 Verificar e-mail, 1.9 Por que assinar o Pro, 1.10 Escolha do plano.

## Grupo 2 — Núcleo (escopo principal desta Fase 2)

| Tela | Arquivo | Componentes DS | Estados | Observação |
|---|---|---|---|---|
| 2.1 Painel inicial | `PainelScreen.kt` (`:pro:feature:visita`) | `SyncBanner` | vazio (CTA "nova visita"), carregando, offline | **NÃO replicar os 3 cards de métrica de vaidade do protótipo** (Atendimentos hoje/Clientes/Laudos emitidos) — contraria a própria spec (doc 09 §11) e a regra contra excesso de card. Lidera com "Próximos atendimentos" (lista, divisor fino) + 4 ações rápidas em grid (ícone 32px + label, borda fina, sem elevação). Números viram no máximo 1 linha de resumo textual. |
| 2.2 Menu | `MenuScreen.kt` | `Navbar` | n/a | Componente do DS, sem navegação custom |
| 2.3 Novo cliente | `NovoClienteScreen.kt` (`:pro:feature:cliente`) | `TextField`, `Button` | erro, sucesso | Só nome obrigatório |
| 2.4 Nova visita | `NovaVisitaScreen.kt` (`:pro:feature:visita`) | `TextField`, `Button`, `StatusChip` (tipo de visita) | erro (cliente/local não selecionado) | — |
| 2.13 Modo de visita rápida | `VisitaRapidaScreen.kt` | reaproveita componentes de 2.4/2.5 | reduzido | Reduz campos de verdade, não só rótulo |
| 2.14 Checklist por tipo de serviço | `ChecklistTipoVisitaScreen.kt` | `ListRow` (item+checkbox), densidade alta | vazio, progresso parcial | `ListRow`, não 1 card por item |
| 2.5 Atendimento | `AtendimentoScreen.kt` | `TopBar`, `StatusChip`, indicador de progresso por etapa com rótulo | carregando, salvando não-bloqueante, offline (`SyncBanner`) | — |
| 2.6-2.9 Ambientes | `AmbientesScreen.kt` + `CriarAmbienteSheet.kt` + `RenomearAmbienteDialog.kt` + `ExcluirAmbienteDialog.kt` (`:pro:feature:ambiente`) | `EnvironmentCard` (uso legítimo — 1 ambiente = 1 unidade de dado real) | vazio, carregando, erro (exclusão bloqueada por medição associada) | — |
| 2.10 Medição do ambiente | `MedicaoAmbienteScreen.kt` (`:pro:feature:medicao-diagnostico`, reaproveita `ExecutorSpeedtest`/`ExecutorSpeedtestCloudflare` de `:featureSpeedtest`) | `QualityGauge` (métrica dominante) + `ListRow` expansível pras secundárias | medindo, erro (medição inválida, não salva como resultado válido), sucesso | Não fazer grid de 6-7 cards de métrica — 1 gauge em destaque + lista expansível |
| 2.11 Walk Test | `WalkTestScreen.kt` | `QualityGauge` + gráfico de linha (série temporal do trajeto) | medindo, erro (perda de conexão, pausar sem apagar contexto) | Gráfico de linha, não número parado |
| 2.12 Evidências | `EvidenciasScreen.kt` | `EvidenceChip`, grid de miniaturas | vazio, erro (foto indisponível — nota textual como alternativa) | Evidência visual não pode ser obrigatória |
| 2.15 Diagnóstico (medindo) | `DiagnosticoMedindoScreen.kt` (`:core:diagnostico` via `DiagnosticRunner`) | indicador de progresso dedicado | medindo, erro (timeout) | Explicar previamente o que será medido |
| 2.16 Diagnóstico (resultado) | `DiagnosticoResultadoScreen.kt` | `RecommendationBlock` (problema/impacto/ação/prioridade) | sucesso, vazio (diagnóstico "adequado" precisa de estado positivo claro) | — |

Fora de escopo desta issue: Grupo 3 (Entrega/Financeiro — laudo PDF, Pix, conclusão de visita),
Fase 3, parcialmente bloqueada por #1160.

## Riscos gerais de "cara de IA" a vigiar

- Tela 2.1 (Painel) já endereçada acima (sem cards de vaidade).
- Nenhum ícone fora de Material Symbols Outlined.
- Nenhuma copy traduzida literalmente do inglês.
- Badge/chip só nos usos semânticos reais listados acima (nunca decorativo).

## Módulos Gradle desta fase

Convenção hierárquica (`.claude/rules/higiene-e-padronizacao-repositorio.md` §5, aplicada a módulo
novo):

- `:pro:core:designsystem` (`android/pro/core/designsystem`) — componentes compartilhados
  (`StateCard`, `ListRow`, `StatusChip`, `VisitCard`, `EnvironmentCard`, `QualityGauge`,
  `EvidenceChip`, `SyncBanner`, `RecommendationBlock`, `Navbar`). Kotlin/Compose puro, sem Hilt.
- `:pro:core:database` (`android/pro/core/database`) — Room greenfield: `Profissional`, `Cliente`,
  `Visita`, `Ambiente`, `Medicao`, `DiagnosticoResultado`, `Evidencia`, `ChecklistItem`. Repositórios
  finos por agregado, expostos via Hilt (`ProDatabaseModule`).
- `:pro:feature:auth` — Carregamento, Apresentação, Cadastro do profissional, Permissões, Permissão
  bloqueada.
- `:pro:feature:cliente` — Novo cliente.
- `:pro:feature:visita` — Painel, Menu, Nova visita, Visita rápida, Checklist, Atendimento.
- `:pro:feature:ambiente` — Ambientes (lista/criar/renomear/excluir).
- `:pro:feature:medicao-diagnostico` — Medição, Walk Test, Evidências, Diagnóstico (medindo/
  resultado).

Cada feature module tem seu próprio grafo de navegação (`<Feature>NavGraph.kt`, extension de
`NavGraphBuilder`) e seus próprios ViewModels (`hiltViewModel()` resolvido no local de uso) — sem
god ViewModel, sem feature dependendo de feature. `:pro:app` só compõe os grafos em `ProNavHost.kt`.
