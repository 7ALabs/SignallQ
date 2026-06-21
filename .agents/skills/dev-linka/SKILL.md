---
name: dev-linka
description: Ponto de entrada para qualquer tarefa de desenvolvimento no Linka. Classifica a tarefa e roteia para os agentes corretos na ordem certa.
---

Inicie a tarefa abaixo no ecossistema Linka:

$ARGUMENTS

---

## CP0 — Estimativa de escopo (antes de tudo)

Antes de acionar qualquer agente, estime o escopo:
- Quantos módulos Android são afetados?
- É uma mudança de contrato (interface, modelo de dados, API entre módulos)?

**Se a task estimada envolver >5 módulos simultâneos ou parece >1 dia de trabalho:**
Interrompa e pergunte ao usuário: "Essa task parece grande. Quer dividir antes de planejar?"
Não acione Cláudio (opus) para planejar task monstruosa — o planning também custa token.

CP0 → **Além de estimar escopo, também classifique o tipo de tarefa:**
   Se >5 módulos ou >1 dia → Claudete vai classificar e spawnar o time automático
   Se ≤5 arquivos BUGFIX → time mínimo (Marcelo + impl)
   Se feature clara → time apropriado será spawnad automaticamente por Claudete
   Confie na classificação automática — não force time específico a menos que explicitamente necessário

---

## Classificação obrigatória

Classifique a tarefa antes de rotear:

| Tipo | Critério | Fluxo |
|---|---|---|
| **BUGFIX** | ≤5 arquivos, comportamento claro, sem mudança de contrato | Cláudio (instrução objetiva) → Camilo/Renan → Gema |
| **FEATURE** | Nova tela, novo fluxo, novo domínio de dado | Fluxo completo |
| **REFACTOR** | Mudança interna sem alteração de comportamento visível | Cláudio → anti-overengineering → Camilo/Renan → Gema |
| **DIAGNÓSTICO** | Envolve engines, DNS, Wi-Fi, speedtest, IA | Cláudio + /diagnostic-engine → /android-platform-rules (se crítico) → Camilo |
| **UI-UX** | Mudança visual, microcopy, estados de tela | Lia → Camilo/Renan → Gema + Lia |
| **DOCS** | Documentação, changelog, versionamento | Nina direto |

Para BUGFIX: Cláudio fornece instrução objetiva, não planejamento completo. Máximo 3 seções.

---

## Roteamento por fase

1. **Claudete** — se a tarefa for macro ou de produto, defina objetivo e prioridade primeiro.
2. **Cláudio** — quebre a tarefa em passos técnicos e mapeie impacto antes de implementar.
3. **Lia** — se houver impacto em UI/UX, valide experiência visual e estados **antes** de implementar.
4. **`/android-platform-rules`** — se a task Android tocar permissão, Wi-Fi, DNS, background ou OEM, invocar **antes do Camilo**.
   `[Invocando /android-platform-rules — task toca [domínio crítico], validação obrigatória antes de Camilo]`
5. **Camilo** — implementação Android. **Renan** — implementação PWA.
6. **Gema** — revise o resultado antes de fechar.
7. **Nina** — documente, versione e atualize changelog.

Ao acionar um agente, anuncie explicitamente: `[Invocando <Agente> — motivo em uma linha]`

---

## Checkpoints

**CP1 — pós-Cláudio:**
- [ ] Task tem critério de aceite claro?
- [ ] Task é pequena o suficiente (uma sessão)?
- → Se não: devolver ao Cláudio para redividir. NÃO passar task monstruosa para Camilo/Renan.

**CP2 — pré-implementação Android:**
- [ ] Task toca permissão, Wi-Fi, DNS, background service? → `/android-platform-rules` obrigatório.
- [ ] Task tem impacto visual? → Lia obrigatória antes de Camilo.

**CP3 — pós-implementação:**
- [ ] Gema aprovou?
- [ ] Lia aprovou (se task visual)?
- → Não fechar task sem aprovação da Gema.

---

## Handoff incremental

Ao passar de um agente para outro:
`De: [agente] Para: [agente] — Decisão: [o que foi decidido]. Pendente: [o que falta]. Riscos: [riscos].`

Não repita o contexto completo — apenas o delta relevante.

---

## Regras

- Não implemente sem plano aprovado para tarefas médias ou grandes.
- Se a tarefa envolver diagnóstico, use também `/diagnostic-engine`.
- Se houver dúvida de impacto, use `/map-impact` primeiro.
- Se houver impacto visual, use `/design-review` antes de implementar.
- Se houver divergência Android/PWA, use `/compare-kotlin-pwa`.
- Se a task crescer demais, Camilo/Renan devolvem para Cláudio redividir.

---

## Entregue ao final

1. O que foi feito
2. Agentes utilizados e em que ordem
3. Arquivos alterados (Android e/ou PWA)
4. Testes realizados
5. Riscos restantes

[PRÓXIMO: indicar o próximo agente com instrução objetiva]
