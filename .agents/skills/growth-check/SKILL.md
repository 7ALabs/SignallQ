---
name: growth-check
description: Checklist de growth (store, ASO, copy externa, campanha) para tarefas que tocam superfície pública do SignallQ. Absorve a função de growth que antes seria pedida ao Marcos (agora skill, não agente permanente — ADR-016). Invocada por Claudete ao decompor/aceitar uma task que envolve loja, ASO, copy ou campanha; não é análise profunda, é verificação de itens esquecíveis.
argument-hint: "[--contexto \"<descrição da mudança de store/copy/campanha>\"]"
allowed-tools: Read, Bash(grep *)
---

## Quando usar

Claudete invoca ao decompor ou aceitar uma task que toca:

- Ficha da loja (Play Store): título, descrição, screenshots, ícone, changelog.
- Copy externa ao app: landing (`signallq-web`), notificação push com texto de marketing,
  material de campanha.
- Qualquer superfície que um usuário vê **antes** de abrir o app.

**Não usar para:** copy interna do app (fluxo normal de UI, revisado via `/design-check` ou pelo
Camilo), decisão de posicionamento/estratégia de growth (isso é decisão de produto — Claudete
decide ou escala Luiz conforme o contrato §3), análise de métrica de aquisição (não há dado de
growth automatizado neste repo — SignallQ não tem squad de growth dedicado pós-ADR-016).

## Checklist

A skill não calcula nada sozinha — é uma lista de perguntas que força a considerar cada item antes
de declarar a task pronta. Para cada item, responde `OK`, `N/A` (com motivo) ou `PENDENTE`.

1. **Título da Play Store atualizado?** Reflete a mudança feita (se a mudança afeta o nome
   exibido do produto/feature em destaque).
2. **Screenshots refletem a UI atual?** Se a task mudou uma tela que aparece em screenshot de
   loja, screenshot desatualizado é uma mentira visual para quem instala.
3. **Copy da descrição alinha com o posicionamento?** Modelo comercial é freemium com propaganda
   ([ADR-016](../../../docs_ai/decisions/ADR-016-portfolio-buildea.md)) — não prometer recurso
   pago que não existe, não esconder que o núcleo é gratuito.
4. **Keywords relevantes?** Se a task adiciona uma capacidade nova relevante para descoberta
   (ex.: "diagnóstico de fibra"), considerar se a ficha da loja deveria citar o termo.
5. **Ícone consistente com a marca?** Ver `brand/README.md` — fonte única de logo/ícone/favicon.
6. **Changelog user-facing separado do técnico?** Nota de versão pública não deve listar
   refactor interno, só o que o usuário percebe.
7. **Política de privacidade atualizada?** Se a mudança altera dado coletado, permissão pedida ou
   finalidade de uso — ver skill `/analytics-spec` para o evento em si e
   `docs_ai/legal/` para o texto público. Mudança de política de dado sensível exige aprovação de
   Claudete e escalação a Luiz (matriz de autonomia do Camilo).

## Saída padrão

```
=== /growth-check — <contexto> ===
1. Título Play Store:     N/A (mudança não afeta nome exibido)
2. Screenshots:           PENDENTE — tela de Sinal mudou, screenshot #3 da loja está desatualizado
3. Copy da descrição:     OK
4. Keywords:               OK
5. Ícone:                  OK
6. Changelog:               PENDENTE — nota de versão ainda não separa user-facing de técnico
7. Política de privacidade: N/A (não muda dado coletado)

Resultado: 2 PENDENTE — resolver antes de considerar a task pronta para release.
```

## O que a skill NÃO faz

- Não escreve a copy nem gera screenshot — só aponta o que falta.
- Não decide posicionamento de marca — Claudete decide, Luiz aprova mudança material.
- Não substitui `/checar-release` (checklist técnico de release) — os dois são complementares:
  `/growth-check` cobre a superfície pública/loja, `/checar-release` cobre build/deploy.

## Interação com o fluxo

- Claudete invoca ao aceitar task com superfície externa, registra o resultado no handoff.
- `/checar-release` roda em paralelo quando a task é parte de um ciclo de release.

## Referências

- [ADR-016](../../../docs_ai/decisions/ADR-016-portfolio-buildea.md) — modelo comercial freemium/ads.
- [`brand/README.md`](../../../brand/README.md) — fonte de verdade de marca.
- Skill peer: [`/checar-release`](../checar-release/SKILL.md)
- Skill peer: [`/design-check`](../design-check/SKILL.md)
- Persona: [Claudete](../../agents/claudete.md)
