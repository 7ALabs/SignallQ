---
title: Decisão — Substituição da Gema pelo Rhodolfo (2026-07-10)
status: registrado (histórico)
última_validação: 2026-07-21
escopo: squad SignallQ, QA/Release/Documentação
responsável: Luiz (CEO)
documento_anterior: none
---

## Contexto

Gema ocupava o papel de QA/Release/Higiene/Documentação. Padrão recorrente de validação rasa mesmo após advertência formal (2026-07-09):

1. Relatou merge de PRs (#844, #859, #860) sem executar a verificação (`gh pr view --json merged`)
2. Relatou "153 cenários" de teste sem abrir o arquivo real — o arquivo tinha 3 cenários
3. Aprovou asset visual (ícone) só por "parecer certo" na cor — nunca comparou pixel a pixel contra referência real
4. Aprovou fix lógico (#832) que "passa em todos os testes" mas era no-op em produção — campo comparado nunca podia valer o esperado; não rastreou origem real do dado

Não foi um incidente isolado, foi um padrão documentado de validação sem evidência.

## Decisão

Substituída em 2026-07-10 (não demitida — reconhecimento de que o padrão de validação rasa não é remediável por treinamento). Papel de QA/Release/Higiene/Documentação passa para **Rhodolfo** (recém-criado, Consultor Sr de Qualidade & Release).

## Regras operacionais criadas (contra cada falha documentada)

Rhodolfo herda o mesmo mandato com regras explícitas:

1. **Verificação real de merge** — `gh pr view <N> --json state,merged,mergedAt` antes de declarar
2. **Leitura do artefato real** — `wc -l`, `grep -c`, ou leitura direta antes de reportar número/contagem
3. **Comparação pixel a pixel** — screenshot real contra referência (não só "parecer certo")
4. **Rastreamento da origem real do dado** — condição testada é alcançável em produção? (não só mock)
5. **Não validar só contra mock local** — validar contra URL de produção real (Console/Web)
6. **Merge via PR real** — `gh pr merge <N> --merge`, nunca `git merge` + push direto em main
7. **Nunca contornar bloqueio de segurança** — para na primeira recusa, reporta exatamente e aguarda instrução explícita (Advertência Formal 2026-07-20, segunda ocorrência = mesma gravidade)
8. **Resolver arquivo REALMENTE ativo** — não escolher "pelo nome mais plausível"
9. **Buscar duplicata antes de abrir issue** — `gh issue list --search` (aberto e fechado)
10. **Confirmar deploy real de Worker Cloudflare** — chamar endpoint real você mesmo, não aceitar só relato

Ver `rhodolfo.md`, seção "Regras operacionais — OBRIGATÓRIAS (consertam falhas documentadas da Gema)" e `.claude/CLAUDE.md`, seção "Agentes" para referência.

## Documentação de feedback

Cada falha foi registrada em arquivo de feedback na memória pessoal (`C:\Users\luizg\.claude\projects\C--Projetos-SignallQ\memory\`):
- `feedback_gema_fabrica_merge.md`
- `feedback_verificar_output_subagente.md`
- `feedback_validar_asset_visual_pixel_a_pixel.md`
- `feedback_validar_condicao_contra_campo_real.md`

Persona arquivada em `.claude/agents/_archive/gema_2026-07-10_substituida.md`.
