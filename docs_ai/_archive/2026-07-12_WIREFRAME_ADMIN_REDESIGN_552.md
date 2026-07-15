---
issue: gmmattey/linka-android#552 (SIG-294, migrada do Linear)
tela: SignallQ Admin — redesenho completo (9 telas + navegação)
tipo: wireframe baixa fidelidade — estrutura/hierarquia de informação, não pixel-perfect
status: proposto, aguardando validação do Luiz antes de virar issue de implementação por tela
autor: Felipe (Admin & Dados)
data: 2026-07-08
---

# Redesenhar Painel Admin como console operacional claro e narrativo

## Princípio central (recap da issue)

Cada tela responde a **uma pergunta principal**. O que não ajuda a responder essa pergunta sai da
tela ou muda de contexto. Nenhuma tela existe "porque cabia um menu ali".

## Padrão visual obrigatório (aplicado nas 9 telas abaixo, sem exceção)

```
Pergunta principal (H1, sempre visível, não decorativa)
   ↓
Filtros globais (período, versão do app, OS, região/operadora quando aplicável)
   ↓
KPIs curtos (3–5 no máximo, cada um com veredito/interpretação, não número cru)
   ↓
Gráfico principal (responde a pergunta do topo — não é "mais um gráfico")
   ↓
Blocos de explicação (o que o gráfico está dizendo, em texto — antes da tabela)
   ↓
Tabela de investigação (drill-down, só depois do contexto já dado)
   ↓
Ações claras (o que fazer com essa informação — nunca tela sem ação)
```

Regras não-negociáveis: sem cards decorativos, sem tabela antes de gráfico+explicação, sem
duplicar dado sem propósito narrativo, pergunta-guia sempre visível, todo KPI tem
interpretação/ação, todo gráfico responde pergunta específica.

---

## Novo menu de navegação (sidebar)

Mapeamento de migração das telas atuais → novas, para referência de quem for implementar
(pasta atual em `SignallQ Admin/src/features/`):

| # | Tela nova | Pergunta-guia | Telas atuais que migram para cá |
|---|---|---|---|
| 1 | Centro de Controle | O SignallQ está saudável agora? | `overview/` |
| 2 | Diagnósticos | O que os usuários estão medindo? | `diagnostics/` |
| 3 | Problemas & Incidentes | O que está prejudicando a experiência? | `errors/` |
| 4 | Redes & Provedores | Onde a qualidade varia? | `networks/` + `operators/` |
| 5 | Uso do App | As pessoas estão usando o app como esperado? | `product-analytics/` |
| 6 | Releases & Qualidade | A versão publicada está estável? | `app-versions/` |
| 7 | IA & Custos | A IA está entregando valor com custo controlado? | `ai-cost/` (expandido) |
| 8 | Saúde do Sistema | A infraestrutura está funcionando? | `system-health/` (recortado, foco infra real) |
| 9 | Configurações | O que posso controlar com segurança? | `settings/` + `feature-flags/` |

```
┌──────────────────────┐
│  SignallQ Admin       │
├──────────────────────┤
│  ● Centro de Controle │  ← landing, default ao logar
│  ○ Diagnósticos       │
│  ○ Problemas & Incid. │
│  ○ Redes & Provedores │
│  ○ Uso do App         │
│  ○ Releases & Qualid. │
│  ○ IA & Custos        │
│  ○ Saúde do Sistema   │
│  ○ Configurações      │
├──────────────────────┤
│  [Perfil admin]  [⎋]  │
└──────────────────────┘
```

Nota de implementação: nomes de rota/hash podem manter os slugs técnicos atuais
(`#overview`, `#errors` etc.) — a migração é de **rótulo, agrupamento e conteúdo da tela**, não
necessariamente de slug. Confirmar com Gema antes de renomear rotas se algo externo já linkar.

---

## 1. Centro de Controle

**Pergunta-guia:** "O SignallQ está saudável agora?"

```
┌───────────────────────────────────────────────────────────┐
│  O SignallQ está saudável agora?                            │  H1 fixo
├───────────────────────────────────────────────────────────┤
│  [Período: 24h ▾] [Versão: todas ▾] [OS: todos ▾]           │  filtros globais
├───────────────────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐        │
│  │Diagnóst. │ │Crash rate│ │Latência  │ │Custo IA  │        │  KPIs (4, com
│  │  1.284   │ │  0.6%    │ │ IA P95   │ │ hoje     │        │  veredito, não
│  │ ↑ 12% dia│ │  ✓ Bom   │ │ 3.2s ✓   │ │ R$ 41,20 │        │  número cru)
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘        │
├───────────────────────────────────────────────────────────┤
│  Gráfico principal: linha do tempo (24h) sobrepondo         │
│  volume de diagnósticos x taxa de erro x latência da IA     │
│  [ gráfico de linha, 3 séries, eixo Y duplo ]                │
├───────────────────────────────────────────────────────────┤
│  Bloco de explicação:                                       │
│  "Pico de diagnósticos às 19h coincide com leve alta de     │
│   latência da IA (3.8s), mas dentro do aceitável (<5s P95)." │
├───────────────────────────────────────────────────────────┤
│  Tabela de investigação: últimos 10 incidentes/alertas       │
│  automáticos (timestamp, tipo, severidade, status)           │
├───────────────────────────────────────────────────────────┤
│  Ações: [Ver Problemas & Incidentes] [Ver IA & Custos]       │
└───────────────────────────────────────────────────────────┘
```

Racional: essa é a tela de "semáforo geral" — 4 KPIs cobrem volume, estabilidade, IA e custo.
Qualquer coisa fora desses 4 eixos pertence a uma tela específica, não aqui.

---

## 2. Diagnósticos

**Pergunta-guia:** "O que os usuários estão medindo?"

```
┌───────────────────────────────────────────────────────────┐
│  O que os usuários estão medindo?                           │
├───────────────────────────────────────────────────────────┤
│  [Período ▾] [Versão ▾] [Tipo: Wi-Fi/Móvel/Fibra ▾]          │
├───────────────────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐        │
│  │Total     │ │Taxa concl│ │Duração   │ │Tipo mais │        │
│  │diagnóst. │ │  92%     │ │média     │ │comum     │        │
│  │  3.941   │ │ ✓ Saudáv.│ │  8.4s    │ │ Wi-Fi 61%│        │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘        │
├───────────────────────────────────────────────────────────┤
│  Gráfico principal: distribuição de diagnósticos por tipo    │
│  de rede ao longo do tempo (barras empilhadas, diário)       │
├───────────────────────────────────────────────────────────┤
│  Bloco de explicação:                                        │
│  "Diagnósticos de Fibra caíram 8% na semana — coincide com   │
│   queda de instalações na região Sul (ver Redes&Provedores)."│
├───────────────────────────────────────────────────────────┤
│  Tabela: funil de diagnóstico (iniciado → concluído →        │
│  laudo gerado), com taxa de abandono por etapa                │
├───────────────────────────────────────────────────────────┤
│  Ações: [Exportar CSV] [Ver funil completo] [Filtrar erros    │
│  desse funil em Problemas & Incidentes]                       │
└───────────────────────────────────────────────────────────┘
```

Racional: essa tela é sobre **volume e composição** de uso da feature core, não sobre falhas
(isso é a tela 3) nem sobre qualidade de rede em si (isso é a tela 4).

---

## 3. Problemas & Incidentes

**Pergunta-guia:** "O que está prejudicando a experiência?"

```
┌───────────────────────────────────────────────────────────┐
│  O que está prejudicando a experiência?                      │
├───────────────────────────────────────────────────────────┤
│  [Período ▾] [Versão ▾] [Severidade: Crítico/Alerta/Info ▾]  │
├───────────────────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                     │
│  │Crash rate│ │ANR rate  │ │Timeouts  │                     │  KPIs (3, cada
│  │  0.6%    │ │ 0.3%     │ │de IA     │                     │  com limiar de
│  │ ✓ Aceit. │ │ ✓ Aceit. │ │  1.9%    │                     │  mercado exposto)
│  └──────────┘ └──────────┘ └──────────┘                     │
├───────────────────────────────────────────────────────────┤
│  Gráfico principal: crash rate + ANR por versão de app       │
│  (barras agrupadas, últimas 5 versões, com linha de limiar    │
│  1% e 0.47% pontilhadas)                                      │
├───────────────────────────────────────────────────────────┤
│  Bloco de explicação:                                        │
│  "v0.24.1 está 2.1x acima do limiar de crash rate. Maior      │
│   concentração em Android 8 — correlação já reportada ao      │
│   Camilo (ver issue vinculada)."                               │
├───────────────────────────────────────────────────────────┤
│  Tabela: stack traces agrupados por assinatura, contagem,     │
│  versão, dispositivo/OS mais afetado, primeira/última          │
│  ocorrência                                                    │
├───────────────────────────────────────────────────────────┤
│  Ações: [Abrir issue no GitHub] [Marcar como conhecido]        │
│  [Ver detalhe no Crashlytics]                                  │
└───────────────────────────────────────────────────────────┘
```

Racional: absorve `errors/` na íntegra. Diferencial do redesenho: KPIs sempre mostram o
**limiar de mercado**, não só o número — decisão consciente para não repetir erro de "crash rate:
3.2%" sem contexto (o time já cometeu isso antes).

---

## 4. Redes & Provedores

**Pergunta-guia:** "Onde a qualidade varia?"

```
┌───────────────────────────────────────────────────────────┐
│  Onde a qualidade varia?                                     │
├───────────────────────────────────────────────────────────┤
│  [Período ▾] [Operadora ▾] [Região/UF ▾] [Tipo rede ▾]        │
├───────────────────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐         │
│  │Vel. média│ │Latência  │ │Operadora │ │Região com│         │
│  │download  │ │média     │ │pior sinal│ │mais falha│         │
│  │ 62 Mbps  │ │ 34ms     │ │ Oi (2.1★)│ │ Norte    │         │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘         │
├───────────────────────────────────────────────────────────┤
│  Gráfico principal: mapa de calor ou barras comparativas      │
│  de qualidade (RSRP/velocidade) por operadora x região         │
├───────────────────────────────────────────────────────────┤
│  Bloco de explicação:                                         │
│  "Vivo e Claro têm cobertura equivalente no Sudeste, mas       │
│   Claro cai 40% de qualidade no Norte — padrão consistente     │
│   nas últimas 4 semanas, não é ruído."                          │
├───────────────────────────────────────────────────────────┤
│  Tabela: operadora, região, amostras, sinal médio,             │
│  velocidade média, taxa de falha de conexão                    │
├───────────────────────────────────────────────────────────┤
│  Ações: [Exportar relatório por operadora] [Filtrar             │
│  diagnósticos dessa região]                                     │
└───────────────────────────────────────────────────────────┘
```

Racional: fusão deliberada de `networks/` + `operators/` — ambos respondiam fragmentos da mesma
pergunta ("onde a rede é ruim"). Separados, forçavam o usuário a cruzar mentalmente duas telas.

---

## 5. Uso do App

**Pergunta-guia:** "As pessoas estão usando o app como esperado?"

```
┌───────────────────────────────────────────────────────────┐
│  As pessoas estão usando o app como esperado?                 │
├───────────────────────────────────────────────────────────┤
│  [Período ▾] [Versão ▾] [Plataforma: Android/iOS ▾]            │
├───────────────────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │DAU/MAU   │ │Retenção  │ │Retenção  │ │Sessões/  │          │
│  │  22%     │ │D1        │ │D7        │ │usuário   │          │
│  │ ✓ Saudáv.│ │ 31% ✓Bom │ │ 14% ✓ Ok │ │  2.3/dia │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
├───────────────────────────────────────────────────────────┤
│  Gráfico principal: funil de engajamento — abertura →         │
│  diagnóstico iniciado → concluído → laudo gerado                │
│  (funil horizontal, com % de queda por etapa)                   │
├───────────────────────────────────────────────────────────┤
│  Bloco de explicação:                                          │
│  "Retenção D1 de 31% está na faixa saudável de mercado          │
│   (25-40% para utilitários). Maior queda do funil é entre       │
│   'diagnóstico iniciado' e 'concluído' — 18% de abandono,        │
│   investigar se é timeout de IA (ver IA & Custos)."              │
├───────────────────────────────────────────────────────────┤
│  Tabela: telas mais visitadas (screen_view), tempo médio,       │
│  taxa de saída por tela                                          │
├───────────────────────────────────────────────────────────┤
│  Ações: [Ver eventos brutos no Firebase] [Comparar cohort        │
│  por versão]                                                     │
└───────────────────────────────────────────────────────────┘
```

Racional: renomeia `product-analytics/` para algo que qualquer stakeholder não-técnico entende à
primeira leitura. KPIs sempre comparados a benchmark de mercado — sem isso, retenção é só número
solto.

---

## 6. Releases & Qualidade

**Pergunta-guia:** "A versão publicada está estável?"

```
┌───────────────────────────────────────────────────────────┐
│  A versão publicada está estável?                             │
├───────────────────────────────────────────────────────────┤
│  [Versão foco: v0.24.2 ▾] [Comparar com: v0.24.1 ▾]            │
├───────────────────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │Rollout   │ │Rating    │ │Crash rate│ │Reviews   │          │
│  │  38%     │ │  4.2 ★   │ │  0.5%    │ │negativos │          │
│  │ em curso │ │ ✓ Saudáv.│ │ ✓ Aceit. │ │  3 novos │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
├───────────────────────────────────────────────────────────┤
│  Gráfico principal: crash rate + rating por versão, lado a     │
│  lado (últimas 6 versões), destacando a versão em foco          │
├───────────────────────────────────────────────────────────┤
│  Bloco de explicação:                                           │
│  "v0.24.2 está estável até aqui — crash rate 0.5%, abaixo        │
│   da v0.24.1 (0.9%). Rollout gradual recomendado continuar        │
│   até 100% sem necessidade de pausa."                              │
├───────────────────────────────────────────────────────────┤
│  Tabela: reviews recentes (texto, nota, versão, data),            │
│  ordenados por relevância/negativos primeiro                      │
├───────────────────────────────────────────────────────────┤
│  Ações: [Avançar rollout] [Pausar rollout] [Responder review]     │
└───────────────────────────────────────────────────────────┘
```

Racional: absorve `app-versions/`, mas o eixo organizador vira "uma versão de cada vez", não
"lista de todas as versões" — decisão operacional acontece por release, não por tabela genérica.

---

## 7. IA & Custos

**Pergunta-guia:** "A IA está entregando valor com custo controlado?"

```
┌───────────────────────────────────────────────────────────┐
│  A IA está entregando valor com custo controlado?              │
├───────────────────────────────────────────────────────────┤
│  [Período ▾] [Provider: Gemini/Qwen3 ▾] [Modelo ▾]              │
├───────────────────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │Custo     │ │Latência  │ │Taxa de   │ │Taxa de   │           │
│  │no período│ │P95       │ │fallback  │ │falha     │           │
│  │ R$ 287,40│ │  3.2s ✓  │ │  4% ✓    │ │  1.2%    │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
├───────────────────────────────────────────────────────────┤
│  Gráfico principal: custo diário por provider (Gemini vs         │
│  Qwen3 fallback), sobreposto com volume de sessões                │
├───────────────────────────────────────────────────────────┤
│  Bloco de explicação:                                             │
│  "Fallback para Qwen3 subiu de 4% para 9% nos últimos 3 dias —     │
│   sugere instabilidade momentânea do Gemini, não aumento de         │
│   custo real (Qwen3 é mais barato por token). Sem impacto            │
│   negativo no orçamento, mas latência P95 do fallback é 40%          │
│   maior — monitorar se piora a experiência."                          │
├───────────────────────────────────────────────────────────┤
│  Tabela de auditoria: sessões com maior consumo de tokens,            │
│  outliers de latência, retries, custo por sessão                      │
├───────────────────────────────────────────────────────────┤
│  Ações: [Ver prompt da sessão outlier] [Exportar relatório de          │
│  custo] [Configurar alerta de orçamento]                                │
└───────────────────────────────────────────────────────────┘
```

Racional: tela nova, não é só o `ai-cost/` renomeado — issue pede explicitamente
custo+qualidade+modelo+falhas+auditoria juntos. Auditoria de sessão outlier é o gancho de ação
mais concreto dessa tela (identificar prompt ineficiente ou loop de retry).

---

## 8. Saúde do Sistema

**Pergunta-guia:** "A infraestrutura está funcionando?"

```
┌───────────────────────────────────────────────────────────┐
│  A infraestrutura está funcionando?                             │
├───────────────────────────────────────────────────────────┤
│  [Período ▾] [Serviço: Worker IA/Admin API/DB ▾]                 │
├───────────────────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │Uptime    │ │Erro 5xx  │ │Latência  │ │Requests  │            │
│  │  99.94%  │ │  0.08%   │ │p95 API   │ │/min       │            │
│  │ ✓ Saudáv.│ │ ✓ Aceit. │ │ 210ms ✓  │ │  340       │            │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘            │
├───────────────────────────────────────────────────────────┤
│  Gráfico principal: disponibilidade + latência por serviço        │
│  Cloudflare (Worker IA, Admin API, D1) ao longo do tempo            │
├───────────────────────────────────────────────────────────┤
│  Bloco de explicação:                                              │
│  "Pico de latência no Worker IA às 14h coincide com deploy          │
│   registrado no GitHub — comportamento esperado, sem incidente."      │
├───────────────────────────────────────────────────────────┤
│  Tabela: incidentes de infra (timestamp, serviço, duração,           │
│  causa raiz se conhecida, status)                                     │
├───────────────────────────────────────────────────────────┤
│  Ações: [Ver logs no Cloudflare] [Criar issue de incidente]           │
└───────────────────────────────────────────────────────────┘
```

Racional: recorte deliberado — a issue pede foco em "infra real" (Cloudflare/Workers/DB), não em
métricas de produto que já vivem em outras telas. Isso remove a sobreposição que o
`system-health/` atual tinha com `overview/`.

---

## 9. Configurações

**Pergunta-guia:** "O que posso controlar com segurança?"

```
┌───────────────────────────────────────────────────────────┐
│  O que posso controlar com segurança?                            │
├───────────────────────────────────────────────────────────┤
│  [Categoria: Feature Flags / Conta / Notificações ▾]              │
├───────────────────────────────────────────────────────────┤
│  (sem KPIs — tela de controle, não de análise; pula direto        │
│   para blocos de ação, conforme o padrão permite quando a          │
│   pergunta-guia não é analítica)                                    │
├───────────────────────────────────────────────────────────┤
│  Bloco: Feature Flags ativas                                       │
│  [ toggle ] IA fallback automático         [ON]                     │
│  [ toggle ] Diagnóstico de Fibra (beta)    [OFF]                    │
│  [ toggle ] Novo funil de onboarding       [ON — 20% rollout]        │
├───────────────────────────────────────────────────────────┤
│  Bloco: Conta e acesso                                               │
│  [ Admins com acesso: 3 ]  [ Convidar novo admin ]                    │
├───────────────────────────────────────────────────────────┤
│  Bloco: Notificações e alertas                                        │
│  [ Alerta de crash rate > 1% ]        [ configurar ]                   │
│  [ Alerta de custo IA > R$ 500/dia ]  [ configurar ]                    │
├───────────────────────────────────────────────────────────┤
│  Tabela: log de auditoria — quem mudou o quê, quando                    │
├───────────────────────────────────────────────────────────┤
│  Ações: [Salvar alterações] [Reverter para padrão]                      │
└───────────────────────────────────────────────────────────┘
```

Racional: fusão de `settings/` + `feature-flags/` — única tela sem KPI/gráfico por natureza (é
controle, não análise), mas mantém tabela de investigação (log de auditoria) e ações claras,
preservando o padrão onde faz sentido.

---

## Observações gerais de implementação (para quando isso virar issues por tela)

- Todos os filtros globais (período, versão, OS) devem ser **componente compartilhado único**,
  não reimplementado por tela — evita duplicação de estado e comportamento inconsistente entre
  abas.
- KPIs devem seguir o padrão já estabelecido no design system: métrica crua + veredito humano
  (Excelente/Bom/Regular/Fraco/Forte), nunca número solto — mesma regra do app mobile, aplicada
  aqui ao painel.
- Cada tela precisa de contrato de dados próprio antes de qualquer UI — isso vira task de mock
  realista por tela, seguindo distribuições plausíveis (nunca dado "redondo demais").
- Esse wireframe não define biblioteca de gráficos, paleta exata ou breakpoints — isso é
  responsabilidade da implementação, respeitando os tokens já definidos em
  `.claude/skills/SignallQ-design/HANDOFF_README.md`.
- Sugestão de ordem de implementação (a validar com Claudete/Luiz): Centro de Controle primeiro
  (é a landing, maior visibilidade), depois Problemas & Incidentes e IA & Custos (maior valor
  operacional imediato), resto em sequência.

## Fora de escopo deste wireframe

- Não define os contratos de dados/mock por tela — isso é a próxima etapa, tela por tela.
- Não implementa nenhum componente React — é proposta estrutural, aguardando validação antes de
  virar código.
- Não decide biblioteca de gráficos nem substitui Recharts se já em uso — decisão técnica futura.
