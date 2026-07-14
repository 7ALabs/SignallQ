# Plano de implementação — SignallQ App To-Be (MD3)

> Registrado em 2026-07-13 por Claudete. Fonte: protótipo "SignallQ App - Fluxo de Telas.dc.html"
> (projeto Claude Design "SignallQ Design System", `e77ea465-291f-4bf5-930c-a267680da04e`) +
> levantamento As-Is do código Android (`android/`) via Explore em 2026-07-13.
>
> Objetivo: levar o app real (código atual) ao estado To-Be documentado no protótipo, fiel tela a
> tela. Este documento é o plano — execução rastreada via GitHub Issues (ver seção final).

## 0. Decisões de produto em aberto — resolver antes/durante cada fase

| # | Questão | Por que trava | Recomendação (default até decisão contrária) |
|---|---|---|---|
| 1 | **Equipamento de Internet (5b) "universal"** — hoje só existe cliente Nokia G-1425G-B via scraping HTTP (`NokiaModemClient`); `GatewayConnectionService` genérico é mock. TR-069/TR-064/OMCI citados nas notas não existem no código. | Maior risco técnico do plano — tratar como "trocar tela" subestima o esforço em ordens de grandeza. | UI universal (composição por capacidade) com engine plugável de 1 provider real (Nokia) por trás. Documentar limitação, não bloquear lançamento com TR-069 real. |
| 2 | **Escopo exato de "SignallQ AI (7) descontinuada"** — a tela standalone (`SignallQScreen`/`SignallQPulseScreen`) já está órfã hoje; a Análise Detalhada (1a) e o chat de diagnóstico usam a mesma infra de IA. | Remover a infra errada quebra 1a, que o próprio protótipo mantém. | Remove só a tela/rota dedicada (já é dead code). Infra de IA usada em 1a/monitoramento continua. |
| 3 | **Tab "Dispositivos" dentro de Sinal** (4ª tab hoje) não existe no protótipo — lá Dispositivos só aparece em 5a (hub Ferramentas). | Ambíguo se é duplicação intencional. | Remover a 4ª tab de Sinal, centralizar em 5a. |
| 4 | **"Falar com a operadora" (1b)** — nota exige config remota por parceiro; não confirmado se `OperadoraBottomSheet` já é dinâmico. | Se hardcoded, 1b não é restyle — é integração nova. | Camilo confirma antes da Fase 2. |
| 5 | **Monitoramento (5f) vira ferramenta com tela própria** — hoje só toggles dentro de Ajustes. | Duplicar controle em dois lugares é ruído. | Single source: toggle só em 5f; Perfil/Ajustes só linka pra lá. |
| 6 | **Jogos (5g)** pede lista curada estática de games — decisão de conteúdo, não só código. | Sem a lista, a tela não tem o que mostrar. | Você/Lia define os primeiros ~10-15 títulos antes da Fase 6. |

## 1. Fases e dependências

```
Fase 0  Fundação MD3 (tema/tokens/primitivos Compose)
   │
   ├─→ Fase 1  Navegação (AppShell: troca Ajustes↔Ferramentas na tab bar, Perfil via avatar)
   │       │
   │       ├─→ Fase 2  Onboarding + Velocidade + Início        (baixo risco, restyle+extracts)
   │       ├─→ Fase 3  Sinal + Histórico                        (baixo risco, restyle)
   │       ├─→ Fase 4  Ferramentas hub + Dispositivos+DNS+Ping+Laudo (médio risco)
   │       ├─→ Fase 5  Equipamento de Internet                  (ALTO risco — decisão #1)
   │       ├─→ Fase 6  Jogos (novo)                              (decisão #6)
   │       └─→ Fase 7  Monitoramento (5f) + Perfil/Ajustes (6a-6f)
   │
   └─→ Fase 8  Cleanup/QA (dead code, contraste, checar-entrega)
```

Fases 2-7 podem rodar em paralelo entre si (dependem só de Fase 0+1). Fase 5 começa por último por
causa do risco arquitetural.

## 2. Fase 0 — Fundação MD3

- Tipografia: `SignallQTheme.kt` não tem `FontFamily` customizada hoje (Roboto/sistema). Adicionar
  Google Sans Flex — checar licenciamento antes de embutir fonte.
- Cores/tokens: `LkColors`/`LkTokens` já existem sobre `lightColorScheme`/`darkColorScheme` —
  conferir aderência aos tokens do protótipo, não recriar do zero.
- Espaçamento 8dp: formalizar escala única (`SignallQSpacing`) se não existir — hoje magic numbers
  espalhados (`HomeScreen.kt` tem 3764 linhas sozinho).
- Componentes base a alinhar (`Card`, `Badge`, `TopBar`, `BottomNav`, `SignalBars`, `Overline`):
  auditar equivalentes hoje espalhados dentro de `AppShell.kt`/telas, extrair se não existirem como
  componentes reutilizáveis.
- Estado: nenhum — puramente visual/tema.

## 3. Fase 1 — Navegação

- `AppShell.kt`: trocar 5ª tab de `Ajustes` para `Ferramentas`. Manter entrada em Velocidade
  (já correto).
- Criar acesso a Perfil/Ajustes fora da tab bar, via avatar do TopBar (reaproveitar avatar já
  existente na Home).
- Adicionar ao enum `Overlay`: `EquipamentoInternet` (substitui `Fibra`), sub-telas de Ferramentas,
  `Jogos`, `Perfil`, `Dns` (se sair de sheet — ver Fase 4).
- Se decisão #2 confirmar: remover `SignallQScreen.kt`, `SignallQPulseScreen.kt`,
  `SignallQPulseUiState.kt`, `SignallQUiState.kt`, `component/SignallQPulseIcon.kt` e campos órfãos
  de `AppShellSignallQState`, exceto os usados por 1a e chat de diagnóstico.

## 4. Fase 2 — Onboarding, Velocidade, Início

- **Onboarding (0):** restyle puro. `first_launch` (`PreferenciasAppRepository.onboardingConcluidoFlow`)
  já correto.
- **Velocidade (1):** `SpeedTestScreen.kt`, `VelocidadeScreen.kt`, `ResultadoVelocidadeScreen.kt` —
  restyle dos 3 estados (idle/running/result). Banner nativo: confirmar guard de "só idle + inventário
  disponível". **1a Análise detalhada:** extrair de inline (`AnalisadorState` em
  `ResultadoVelocidadeScreen`) para bottom sheet dedicado — estados `Inativo/Analisando/Resultado/Erro`
  já mapeiam 1:1 para loading/bom/ruim. **1b Falar com operadora:** restyle de `OperadoraBottomSheet.kt`,
  condicionado à decisão #4.
- **Início (2):** `HomeScreen.kt`, trilha `PathNode` já dinâmica por `ConnectionNodeType` — restyle.
  Nota exige motor de topologia compartilhado com 5b — hoje Home e Fibra não compartilham engine;
  dependência real da Fase 5, sinalizar a Camilo. 2a-2e: sheets existentes, restyle + confirmar
  reuso do executor de speedtest em 2e (não duplicar lógica).

## 5. Fase 3 — Sinal, Histórico

- **Sinal (3):** `SinalScreen.kt`, tabs Wi-Fi/Canal/Móvel restyle. Remover 4ª tab "Dispositivos"
  (decisão #3). Sheets 3a-3d: extrair de `ModalBottomSheet` inline para componentes dedicados se
  ainda não existirem como tal. 3e/3f (permissões): já existem com lógica de rationale e
  "bloqueada permanentemente" completa — puro restyle.
- **Histórico (4):** `HistoricoScreen.kt` restyle. 4a: checar se reaproveita `LaudoScreen` ou
  precisa de sheet dedicado (`HistoricoDetailSheet`). 4b: `ExportHistoricoBottomSheet.kt` já
  existe (CSV+PDF) — restyle.

## 6. Fase 4 — Ferramentas hub + Dispositivos, DNS, Ping, Laudo

- **Ferramentas (5):** tela nova do zero, grid estático de 7 atalhos (5a-5g), sem chamada de rede
  própria.
- **5a Dispositivos:** `DispositivosScreen.kt` já implementa ARP+mDNS+OUI — restyle + decidir
  pontos de entrada (Home e/ou Ferramentas).
- **5c Ping:** já é overlay — restyle.
- **5d DNS:** mudança estrutural — hoje `ModalBottomSheet` (`DnsSheetContent`, atrás de
  `FeatureFlags.DNS_SCREEN`), protótipo modela como tela cheia. Migrar de sheet para
  overlay/tela roteada própria.
- **5e Laudo:** já é overlay full-screen — restyle, já bate estruturalmente.

## 7. Fase 5 — Equipamento de internet (maior risco)

Substitui `FibraModemScreen.kt` (Nokia-only). 8 cenários de referência no protótipo (gpon-ok,
gpon-bad, router-only, multi-device, partial, access-error, loading, restart-dialog).

- Criar `EquipamentoInternetScreen.kt` — composição por capacidade a partir de um enum único de
  "Acesso ao equipamento" (não existe centralizado hoje; criar e reutilizar também em 2b).
- Manter `NokiaModemClient` como único provider real por trás da engine plugável (decisão #1).
  `ClassificadorSaudeGpon` já reaproveitável para alertas de sinal óptico.
- Regra nova: Double NAT só sinalizado com evidência real (ONT fora de bridge + roteador
  adicional fazendo NAT) — não existe essa checagem hoje.
- Estados a cobrir: leitura completa, leitura parcial, somente identificação, gerenciamento
  disponível, sessão expirada, credenciais necessárias, carregando, diálogo de confirmação de
  reiniciar (com tratamento do período de indisponibilidade pós-reboot).

## 8. Fase 6 — Jogos (novo)

Superado pelo spec completo do Luiz (`docs_ai/functional/JOGOS_TESTE_CONEXAO_SPEC.md`,
2026-07-14) — decisão #6 (lista curada) foi substituída por um catálogo de 16 jogos com 4 perfis
de sensibilidade e fluxo de teste direcionado, implementado na issue #935 (ver "Status de
implementação" no fim do spec). `GameReadinessClassifier.kt` **não** foi reaproveitado aqui —
continua servindo exclusivamente a aba 10 do diagnóstico completo (`DiagnosticRunner`), com
vocabulário e thresholds próprios, incompatíveis com o catálogo/perfis do spec de Jogos.

## 9. Fase 7 — Monitoramento (5f) + Perfil/Ajustes (6a-6f)

- **5f Monitoramento:** hoje só toggles dentro de `AjustesScreen.kt`. Criar sheet novo
  (`DiagnosticoSheet`), movendo os toggles pra lá (decisão #5).
- **Perfil/Ajustes:** split de `AjustesScreen.kt` (2206 linhas):
  - 6a Meu perfil → `PerfilEditSheet.kt` (já existe)
  - 6b Minha conexão → extrair bloco Provedor/ISP inline hoje para `MinhaConexaoSheet` (código novo)
  - 6c Dados e privacidade → `DadosLocaisSheet.kt` (já existe)
  - 6d Privacidade → `PrivacidadeScreen.kt` (já existe)
  - 6e Novidades → `NovidadesScreen.kt` (já existe)
  - 6f Sobre → extrair versão/licenças para `SimpleInfoSheet` dedicado (novo)

## 10. Fase 8 — Cleanup/QA

- Remover dead code confirmado (SignallQ standalone, se decisão #2 confirmar).
- Rodar skill `auditar-ux` (contraste WCAG claro/escuro) e `checar-entrega` antes de fechar cada PR.
- Validar em device real (Rhodolfo) cada tela restruturada — comportamento, não só visual
  (skill `verify`), principalmente Fase 5.

## 11. Riscos técnicos gerais

- `HomeScreen.kt` (3764 linhas) e `AjustesScreen.kt` (2206 linhas) já são monolitos — o split em
  Fase 7 é oportunidade de reduzir dívida, mas é retrabalho real, não só restyle.
- Não existe Compose Navigation real hoje — navegação é `selectedTab` + `overlayStack` (enum
  `Overlay`) dentro de `AppShell.kt`. Sustentável até certo ponto; se o número de overlays dobrar
  (o que este plano faz), avaliar migração para NavHost como item técnico separado, fora deste
  plano.
- `GameReadinessClassifier` e as telas `SignallQ*` já são código não-wireado hoje — padrão
  recorrente de features "meio construídas" no repo; handoff explícito Camilo→Rhodolfo em cada
  fase para não repetir.

## 12. Rastreamento

Execução via GitHub Issues (`gmmattey/linka-android`), squad SignallQ em modo piloto automático
(Camilo implementa, Lia valida design, Rhodolfo faz QA/gate de Done, Claudete coordena). Epic e
tasks por fase referenciadas abaixo (preenchido após criação):

- Epic: [#928](https://github.com/gmmattey/linka-android/issues/928)
- Fase 0: [#929](https://github.com/gmmattey/linka-android/issues/929)
- Fase 1: [#930](https://github.com/gmmattey/linka-android/issues/930)
- Fase 2: [#931](https://github.com/gmmattey/linka-android/issues/931)
- Fase 3: [#932](https://github.com/gmmattey/linka-android/issues/932)
- Fase 4: [#933](https://github.com/gmmattey/linka-android/issues/933)
- Fase 5: [#934](https://github.com/gmmattey/linka-android/issues/934)
- Fase 6: [#935](https://github.com/gmmattey/linka-android/issues/935)
- Fase 7: [#936](https://github.com/gmmattey/linka-android/issues/936)
- Fase 8: [#937](https://github.com/gmmattey/linka-android/issues/937)
