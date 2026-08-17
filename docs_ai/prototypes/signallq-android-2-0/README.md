---
title: "Protótipo navegável — Jornada Android 2.0"
description: "Referência visual e de navegação do épico #1647, versionada no repositório para não depender de máquina."
type: "funcional"
status: "ativo"
owner: "Claudete"
last_updated: "2026-08-16"
---

# Protótipo navegável — Jornada Android 2.0

Referência visual e de navegação do épico [#1647](https://github.com/buildea-labs/signallq/issues/1647).
Cada fatia da Jornada Android 2.0 é comparada com este protótipo antes de ser aprovada.

## Como abrir

Abra `index.html` no navegador. Não precisa de servidor — é autocontido.

```bash
open docs_ai/prototypes/signallq-android-2-0/index.html
```

## Por que está aqui

Até 2026-08-16 o protótipo existia **apenas** em
`/Users/giammattey/GitHub Projects/design_system/signallq-android-2-0-4b44/`, fora de qualquer
repositório, numa única máquina — e o épico #1647 e todas as issues filhas citavam esse caminho
absoluto como referência canônica.

Consequência prática: quem abrisse a issue em outra máquina lia "compare com o protótipo" e não
tinha a pasta. Foi por isso que a comparação com o protótipo, na validação em emulador de
2026-08-16, ficou apenas visual.

## Fontes e assets — o que foi deduplicado

O `assets/` original tinha 11 MB, e **todo ele já existia no repositório**, byte a byte (conferido
por `shasum -a256`):

| Arquivo | Já versionado em |
|---|---|
| `material-symbols-outlined.ttf` (10 MB) | `android/app/src/main/res/font/material_symbols_outlined.ttf` |
| `google-sans-flex-400/500/600/700.ttf` | `android/app/src/main/res/font/google_sans_flex_*.ttf` |
| `signallq-lockup-*.png`, `signallq-symbol-*.png` | `brand/` |

`assets/fonts/material-symbols-outlined.ttf` é um **symlink** para a cópia do app — os 10 MB não
foram duplicados. Se esse arquivo for movido em `android/app/src/main/res/font/`, os ícones do
protótipo deixam de renderizar; atualize o symlink junto.

**No Windows:** o Git só materializa symlinks com `core.symlinks=true` (não é o padrão em toda
instalação). Sem isso, o arquivo vira um texto com o caminho dentro e os ícones não renderizam —
falha silenciosa, o resto do protótipo continua funcionando. Para resolver, ative o suporte
(`git config core.symlinks true` e clone de novo) ou copie o arquivo por cima:

```
copy android\app\src\main\res\font\material_symbols_outlined.ttf ^
     docs_ai\prototypes\signallq-android-2-0\assets\fonts\material-symbols-outlined.ttf
```

Os demais assets foram mantidos como cópia (≈1 MB) para o protótipo continuar autocontido, seguindo
a convenção do `signallq-design-system-2-board/`, que também traz as próprias fontes.

## Divergências em relação ao original

- Os `.md` deste pacote ganharam frontmatter YAML, exigido pelo `docs-ci` para qualquer `.md` sob
  `docs_ai/` (ver `.claude/rules/politica-documentacao-viva.md`, §2.1). Conteúdo inalterado.
- Metadados de ferramenta (`.od-skills/`, `.open-design/`) não foram versionados.

## Documentos do pacote

- [`COVERAGE.md`](COVERAGE.md) — quais telas e fluxos o protótipo cobre, e o que ficou de fora.
- [`NAVIGATION_AUDIT.md`](NAVIGATION_AUDIT.md) — auditoria dos caminhos de navegação.
- [`brand-spec.md`](brand-spec.md) — tokens de marca, cor e tipografia aplicados.

## Limite

O protótipo é referência de **direção visual e de navegação**, não de comportamento. Regras de
negócio, motores de diagnóstico, telemetria e integrações continuam definidos pelo código e pelas
specs canônicas — [`SIGNALLQ_DESIGN_SYSTEM_2_SPEC.md`](../../design-system/SIGNALLQ_DESIGN_SYSTEM_2_SPEC.md)
e [`JORNADA_ANDROID_GUIADA_2_SPEC.md`](../../functional/JORNADA_ANDROID_GUIADA_2_SPEC.md).
