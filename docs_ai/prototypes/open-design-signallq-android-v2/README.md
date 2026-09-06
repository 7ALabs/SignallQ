---
title: "Pacote Open Design — SignallQ Android 2.0"
description: "Instruções e insumos para prototipar a jornada guiada do SignallQ no Open Design"
type: "funcional"
status: "draft"
owner: "Claudete"
last_updated: "2026-08-15"
version: "draft"
---

# Pacote Open Design — SignallQ Android 2.0

Este pacote prepara a primeira rodada de prototipação do reposicionamento do Android. Ele não
instala o Open Design nem substitui as especificações canônicas.

## Como abrir

Abra a **raiz deste repositório** no Open Design. Assim, a ferramenta poderá detectar
automaticamente `PRODUCT.md` e `DESIGN.md`. Não abra somente esta subpasta.

## Fontes obrigatórias

Leia antes de gerar:

1. `PRODUCT.md`
2. `DESIGN.md`
3. `docs_ai/POSICIONAMENTO_PRODUTO.md`
4. `docs_ai/design-system/SIGNALLQ_DESIGN_SYSTEM_2_SPEC.md`
5. `docs_ai/functional/JORNADA_ANDROID_GUIADA_2_SPEC.md`

Em caso de divergência, as três fontes em `docs_ai/` prevalecem sobre o material do protótipo.

## Assets

Referencie os arquivos oficiais diretamente, sem duplicá-los:

- `brand/signallq-symbol-1024.png`
- `brand/signallq-symbol-512.png`
- `brand/signallq-lockup-light-bg.png`
- `brand/signallq-lockup-dark-bg.png`

Fontes disponíveis no projeto:

- `android/app/src/main/res/font/google_sans_flex_regular.ttf`
- `android/app/src/main/res/font/google_sans_flex_medium.ttf`
- `android/app/src/main/res/font/google_sans_flex_semibold.ttf`
- `android/app/src/main/res/font/google_sans_flex_bold.ttf`
- `android/app/src/main/res/font/material_symbols_outlined.ttf`

## Configuração recomendada

- Projeto: `SignallQ Android 2.0`
- Skill: `mobile-app`
- Agente: Codex
- Referência de viewport: Google Pixel recente
- Entrega: protótipo HTML navegável
- Idioma da interface: português do Brasil
- Design system: SignallQ 2.0 draft.6
- Expressão: composição aberta, profundidade tonal e violeta como único acento ativo

## Escopo da primeira rodada

Um único fluxo ponta a ponta:

**Home → “Vídeos ou chamadas travam” → contexto → análise → resultado de instabilidade sob carga →
orientação → nova verificação → comparação antes/depois.**

Incluir apenas acessos básicos a Histórico e Mais. Não expandir todas as ferramentas, onboarding,
login, monetização ou configurações.

## Execução

1. Inicie o Open Design conforme a documentação oficial.
2. Abra a raiz do repositório.
3. Crie o projeto com a configuração recomendada.
4. Cole o conteúdo de `PROMPT_INICIAL.md`.
5. Gere uma primeira versão navegável nos temas claro e escuro.
6. Revise com `CHECKLIST_REVISAO.md`.
7. Registre decisões aprovadas nas especificações canônicas antes de implementar.

## Saída e versionamento

Salve o HTML exportado em uma pasta versionada própria, sem sobrescrever especificações. Arquivos
gerados são artefatos de exploração até aprovação explícita do Luiz.

## Limites

- Não alterar código Android a partir do protótipo sem aprovação.
- Não inventar capacidades técnicas não confirmadas no repositório.
- Não representar hipótese diagnóstica como certeza.
- Não usar o protótipo como fonte canônica de tokens ou regras de produto.
