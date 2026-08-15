---
title: "Prancha visual — SignallQ Design System 2.0"
description: "Referência navegável de foundations e componentes centrais do Design System 2.0."
type: "funcional"
status: "draft"
owner: "Claudete"
last_updated: "2026-08-15"
version: "0.1.0"
---

# Prancha visual — SignallQ Design System 2.0

Artefato estático para validar a primeira versão do sistema visual antes da migração Android.

## Escopo

- foundations: cor, tipografia, espaçamento, forma, estados e movimento;
- componentes centrais: botões, campos, chips, badges, banners, listas, resultado e estados de tela;
- temas claro e escuro;
- comportamento responsivo básico.

A prancha incorpora localmente os mesmos arquivos Google Sans Flex usados pelo Android, nos pesos
400, 500, 600 e 700. O carregamento não depende de a fonte estar instalada no computador.

Não representa código de produção nem confirma que os tokens-alvo já foram migrados para Compose.

## Abrir

Abra `index.html` diretamente no navegador. O seletor no cabeçalho alterna entre os temas.

`brand.html` apresenta a direção premium da expressão de marca: posicionamento, uso do símbolo,
voz, paleta e exemplos de aplicação. Ela preserva os ativos oficiais sem redesenhá-los.

## Fontes

- [`../../design-system/SIGNALLQ_DESIGN_SYSTEM_2_SPEC.md`](../../design-system/SIGNALLQ_DESIGN_SYSTEM_2_SPEC.md)
- [`../../../DESIGN_SYSTEM.md`](../../../DESIGN_SYSTEM.md)
- `foundations.css`, cópia da skill canônica `SignallQ-design` no momento de criação da prancha;
  `styles.css` aplica os overrides-alvo do 2.0.
