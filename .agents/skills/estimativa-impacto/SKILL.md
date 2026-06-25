---
name: estimativa-impacto
description: Framework executável para avaliar tamanho, risco e milestone de uma issue antes do breakdown em tasks. Produz uma linha de decisão: tamanho + risco + milestone + recomendação.
---

Avalie o impacto da issue abaixo antes de qualquer breakdown:

$ARGUMENTS

---

## Passo 1 — Classificação de Tamanho

Responda cada critério com SIM ou NÃO e some os pontos.

| Critério | Peso |
|---|---|
| Toca 1 módulo Gradle | 0 |
| Toca 2–3 módulos Gradle | +1 |
| Toca 4+ módulos Gradle | +2 |
| Toca `AppShell.kt` (navegação raiz) | +2 |
| Toca `AppModule.kt` ou `DiagnosticoModule.kt` (DI global) | +2 |
| Requer migration de schema Room | +3 |
| Requer mudança no Worker Cloudflare (`linka-ai-diagnosis-worker`) | +2 |
| Adiciona permissão nova no Android | +2 |
| Exige paridade obrigatória Android ↔ PWA | +1 |
| Envolve mudança de contrato de API pública (Worker ou endpoint) | +2 |

**Escala:**

| Pontos | Tamanho | Modo de operação |
|---|---|---|
| 0–1 | Pequena | Piloto automático |
| 2–4 | Média | Planejar, executar, registrar |
| 5–8 | Grande | Propor plano, pedir aprovação antes |
| 9+ ou qualquer critério sensível | Sensível | Parar — escalar para o Luiz |

**Critérios que tornam Sensível automaticamente (independente de pontos):**
- Mudança de package (`io.veloo.app`)
- Mudança de marca ou rebrand
- Custo novo ou integração paga
- Publicação em loja (Play Console)
- Exclusão destrutiva irreversível

---

## Passo 2 — Mapeamento de Risco

Responda cada pergunta. Qualquer NÃO é um risco ativo — registre e decida antes de executar.

**Definição**
- [ ] Tem critério de aceite claro e verificável?
  - NÃO → refinar antes de estimar. Não avançar.
- [ ] Existe design aprovado pela Lia (se a issue tem impacto visual)?
  - NÃO → bloquear até ter. Consultar `/linka-design` como referência enquanto aguarda.

**Arquitetura**
- [ ] Afeta `DiagnosticOrchestrator` ou o fluxo central de diagnóstico?
  - SIM → risco ALTO. Adicionar task de smoke test pós-merge.
- [ ] Afeta persistência Room (schema, DAO, queries)?
  - SIM → risco de migration. Verificar versão atual do banco (`SignallQDatabase`, versão 10) e planejar migration explícita.
- [ ] Afeta `MonitoramentoWorker` (background, alarme, WorkManager)?
  - SIM → risco de comportamento silencioso. Testar em device real com Doze Mode.

**Infra e Custo**
- [ ] Afeta o AI Worker Cloudflare (`linka-ai-diagnosis-worker`)?
  - SIM → risco de custo (modelo Qwen3 30B). Estimar volume de chamadas antes de implementar. Fazer `npx wrangler deploy` ANTES do commit.
- [ ] Adiciona permissão nova no Android?
  - SIM → verificar `/regras-android` para restrições de API level e Play Store.

**Qualidade**
- [ ] Tem cobertura de teste unitário prevista ou já existente?
  - NÃO → adicionar ao escopo da issue antes de fechar estimativa. Projeto tem ~37 classes de teste em `android/*/src/test/`.
- [ ] Há dependência de outra issue em andamento (bloqueio cruzado)?
  - SIM → registrar dependência no Linear antes de iniciar.

**Nível de Risco Consolidado:**

| Resultado | Nível |
|---|---|
| Todos SIM, sem afetação de componentes críticos | Baixo |
| 1–2 NÃO ou afeta 1 componente crítico | Médio |
| 3+ NÃO ou afeta DiagnosticOrchestrator ou Room migration | Alto |
| Qualquer critério Sensível acionado | Crítico — escalar |

---

## Passo 3 — Cruzamento com Milestone

Datas de referência (hoje: verificar `currentDate` no contexto):

| Milestone | Data alvo |
|---|---|
| M0 — Fundação e Setup | 27/06/2026 |
| M1 — App pronto para Beta | 17/07/2026 |
| M2 — Beta Fechado | 31/07/2026 |
| M3 — Lançamento Play Store | 07/08/2026 |

**Regra de capacidade por tamanho:**

| Tamanho | Dias úteis estimados | Cabe em M1 se iniciada até | Cabe em M2 se iniciada até |
|---|---|---|---|
| Pequena | 1–2 | 15/07/2026 | 29/07/2026 |
| Média | 3–5 | 10/07/2026 | 24/07/2026 |
| Grande | 6–10 | 03/07/2026 | 17/07/2026 |
| Sensível | indeterminado | Não programar sem aprovação | Não programar sem aprovação |

Se a issue não cabe no milestone atual dado o tamanho e a data de hoje → mover para próximo milestone ou colocar em backlog. Não forçar entrega incompleta.

---

## Passo 4 — Output de Decisão

Ao final dos 3 passos, produza exatamente esta linha:

```
IMPACTO: [Tamanho] · Risco [Nível] · Milestone recomendado: [M0/M1/M2/M3/Backlog] · Recomendação: [executar agora / refinar primeiro / mover para próximo cycle / escalar para o Luiz]
```

Seguida de até 3 bullets com os riscos ativos identificados (se houver). Se não há riscos: omitir os bullets.

**Exemplos de output válido:**

```
IMPACTO: Pequena · Risco Baixo · Milestone recomendado: M1 · Recomendação: executar agora
```

```
IMPACTO: Média · Risco Médio · Milestone recomendado: M1 · Recomendação: refinar primeiro
- Sem design aprovado: bloquear até Lia validar estados visuais.
- Afeta Room DAO: planejar migration antes de iniciar.
```

```
IMPACTO: Grande · Risco Alto · Milestone recomendado: M2 · Recomendação: mover para próximo cycle
- Afeta DiagnosticOrchestrator: risco de regressão em todo fluxo de diagnóstico.
- Sem critério de aceite claro: refinar com o Luiz antes de qualquer estimativa de prazo.
- Sem cobertura de teste: adicionar ao escopo antes de fechar.
```

---

## Quando usar esta skill

- Antes de qualquer breakdown de issue em tasks (obrigatório para Média/Grande/Sensível).
- Ao receber feature bruta sem critério de aceite definido.
- Ao avaliar se uma issue cabe no cycle/milestone atual.
- Ao identificar conflito de prioridade entre issues concorrentes.

## Quando não usar

- BUGFIXes simples (≤5 arquivos, sem mudança de contrato) — Camilo/Renan direto.
- Ajustes de copy ou cor que não afetam lógica — Camilo direto com `/linka-design`.
