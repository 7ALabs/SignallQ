---
title: Decisão — Demissão da Lia, contratação da Marina (2026-07-25)
status: registrado (histórico)
última_validação: 2026-07-25
escopo: squad SignallQ (Frontend/Design — Console, Site, SignallQ Nethal), portfólio 7ALabs
responsável: Luiz (CEO)
documento_anterior: none
---

## Contexto

Lia ocupava o papel de Especialista de Produto, UX & Frontend — design em todos os produtos, e
implementação React/TS/Tailwind do SignallQ Console e Site. Padrão apontado pelo Luiz: em pedidos
de paridade 1:1 com protótipo, reaproveitava código/estrutura por comodidade em vez de refazer do
zero quando a instrução exigia — não pela primeira vez.

## Decisão

Demitida em 2026-07-25. Substituída pela **Marina** (nova, `~/.claude/agents/marina.md`), perfil
que soma UX/design de produto com marketing e implementação HTML/TypeScript. Herda integralmente a
frente de Frontend/Design que era da Lia (Console, Site, design no SignallQ Nethal).

## Ressalva sobre a entrega que motivou a decisão nesta sessão

Na sessão em que a decisão foi tomada, a squad estava em meio à reconstrução 1:1 do SignallQ
Site/PWA contra um protótipo novo (issue #1402 e desdobramentos). Uma das entregas atribuídas à
Lia (Fase 4 — páginas institucionais, incluindo Bufferbloat/CGNAT) foi inicialmente **reprovada**
pelo Rhodolfo sob a alegação de que ela havia reescrito o copy técnico de SEO já validado
(issues #1399/#1374), que deveria ter sido preservado.

A Claudete conferiu por conta própria com `git diff origin/main <branch-da-lia> -- BufferbloatPage.tsx CgnatPage.tsx`
antes de repassar qualquer veredito, e confirmou que o conteúdo textual (título, introdução, texto
de cada seção) estava **byte-a-byte idêntico** ao que já existia em produção — a única mudança real
era estrutural (nomes de campo e composição JSX, exatamente o que a tarefa pedia: só a moldura
visual muda). O Rhodolfo havia comparado o PR contra o protótipo (que tem uma versão resumida de
exemplo) em vez de contra a base real (`main`) — erro de metodologia dele, não da Lia. O veredito
foi revertido e o PR mergeado sem alterações no texto.

Isso não reverte a decisão do Luiz — que se baseia num padrão mais amplo, de sessões anteriores a
esta — mas fica registrado para não distorcer o histórico factual dessa entrega específica: nesta
ocorrência pontual, a acusação de "reescrita sem instrução" não se sustentou.

## Consequências estruturais

- **Frontend/Design (Console, Site, SignallQ Nethal)** → absorvido pela **Marina**, com regra
  operacional explícita de fidelidade 1:1 (ver `marina.md`, seção "REGRA CRÍTICA — Fidelidade
  1:1").
- Persona da Lia arquivada em `~/.claude/agents/_archive/lia_2026-07-25_demitida.md`.
- Tabelas de agentes atualizadas em `.claude/CLAUDE.md` (SignallQ), `C:\Projetos\CLAUDE.md`
  (workspace raiz) e `SignallQ Nethal/CLAUDE.md`.

## Regra operacional criada para todo o squad (não só a Marina)

**Fidelidade 1:1 a protótipo deve ser validada byte a byte, por todo agente que tocar o assunto —
não só por quem implementou.** Isso inclui, no mínimo:
- Quem implementa: compara sua própria entrega contra o protótipo/fonte real antes de reportar
  como pronta.
- Quem revisa (QA/gate de Done): compara contra a **base real** (o código em produção antes da
  mudança, quando aplicável — não só contra o protótipo isoladamente), usando `git diff` real, não
  impressão visual ou memória.
- Quando nenhum agente tiver acesso real ao protótipo ao vivo (limitação conhecida: a tool
  DesignSync não propaga para sessões de subagente, só para a sessão principal), a Claudete assume
  pessoalmente a comparação byte a byte antes de aceitar qualquer entrega como fiel.

Ver `.claude/CLAUDE.md`, seção "Design System", para o texto integrado desta regra.

## Nota de higiene — perda de working-dir change por checkout compartilhado (2026-07-25)

Este documento e as edições correspondentes em `.claude/CLAUDE.md` foram perdidos uma vez antes de
serem commitados — provavelmente um subagente (Rhodolfo ou Marina) rodando sem isolamento de
worktree executou uma operação de checkout/reset no working dir compartilhado do repo, descartando
mudanças não commitadas. Reconstituído e commitado imediatamente após a descoberta, para não
depender de working-dir change sobrevivendo entre invocações paralelas de agente. Reforça a
memória `feedback_parallel_agents_worktree.md`: mudanças de documentação feitas pela sessão
principal devem ser commitadas assim que possível, não deixadas como diff pendente enquanto
subagentes seguem operando no mesmo checkout.
