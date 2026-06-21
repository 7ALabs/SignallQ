# Handoff: Linka Design System → Claude Code

## Overview

Este pacote transforma o **Linka Design System** em uma **Claude Code Skill** pronta para
ser instalada no seu repositório. O objetivo não é implementar telas novas (elas **já
existem** no app Android `io.linka.app.kotlin`), e sim garantir que **todo trabalho futuro de
UI feito com o Claude Code siga o design system perfeitamente** — mesmos tokens, mesma voz,
mesmos componentes.

Uma vez instalada, sempre que você pedir uma tela, um componente ou um ajuste de UI, o
Claude Code reconhece a skill pela sua `description`, carrega as regras e os tokens, e gera
código já dentro do padrão Linka.

---

## Sobre os arquivos deste pacote

A pasta `.claude/skills/linka-design/` contém o design system completo, no formato que o
Claude Code reconhece automaticamente. **Não é código de produção para copiar e colar** — é a
**fonte de verdade de design** que o agente consulta:

| Arquivo / pasta | Conteúdo |
|---|---|
| `SKILL.md` | Manifesto da skill (frontmatter `name` / `description` / `user-invocable`). É isso que o Claude Code lê para decidir quando ativar a skill. |
| `README.md` | Fundamentos completos: contexto do produto, voz/conteúdo, fundamentos visuais, iconografia, índice. |
| `colors_and_type.css` | Todos os design tokens como CSS vars + classes de tipografia M3. **A fonte de verdade dos valores.** |
| `assets/` | Marca "linka" (`ic_launcher.png`, `ic_launcher_foreground.png`). |
| `preview/` | Cards de referência do DS (cores, tipo, espaçamento, componentes) — abra no navegador para visualizar. |
| `ui_kits/android/` | Recriação React fiel do app (`index.html` interativo + componentes JSX). `chrome.jsx` carrega os tokens `LK` e os primitivos compartilhados. |

> Os tokens aqui foram **engenharia-reversa do próprio codebase Android** (`LinkaTheme.kt` →
> `LkColors`, `LkSpacing`, `LkRadius`, `linkaTypography`). Os nomes batem com o que já está no
> seu código Kotlin/Compose — use os valores abaixo como tabela de equivalência.

---

## Fidelidade

**Alta fidelidade (hi-fi).** Cores, tipografia, espaçamento, raios e semântica de status são
os valores finais e exatos, extraídos do app real. Qualquer UI nova deve bater pixel-a-token
com esta referência.

---

## Instalação (3 passos)

### 1. Copie a skill para o seu repositório

Arraste a pasta `.claude/` deste pacote para a **raiz do seu projeto**:

```
seu-projeto/
└── .claude/
    └── skills/
        └── linka-design/
            ├── SKILL.md
            ├── README.md
            ├── colors_and_type.css
            ├── assets/
            ├── preview/
            └── ui_kits/android/
```

- `.claude/skills/` → skill **do projeto**: vai pro Git, todo o time herda. **Recomendado**,
  já que você quer que *o projeto* siga o DS.
- `~/.claude/skills/` → skill **pessoal**: só na sua máquina.

### 2. (Recomendado) Reforce no `CLAUDE.md`

Skills são carregadas sob demanda. Para deixar o DS **sempre presente**, adicione ao
`CLAUDE.md` da raiz do projeto:

```md
## Design System
Toda UI deste projeto segue o Linka Design System.
Antes de criar ou editar telas/componentes, consulte a skill `linka-design`
(.claude/skills/linka-design/README.md) e use os tokens de colors_and_type.css /
LinkaTheme.kt como fonte de verdade.
Não-negociáveis: Material 3 claro, acento violeta #6C2BFF, semântica de status
verde/âmbar/vermelho, ícones Material Symbols (Outlined), tipo Roboto, grid 8dp,
card radius 16dp e flat (sem sombras pesadas), superfícies Orbit (IA) sempre escuras,
copy em PT-BR com "você" e SEM emoji.
```

### 3. Use

- **Automático:** peça qualquer coisa de UI ("crie a tela de configurações de DNS") e o
  Claude Code ativa a skill pela `description`.
- **Explícito:** "use a skill `linka-design` para …".

Para confirmar que está instalada, rode `/doctor` ou liste skills no Claude Code.

---

## Como manter as telas existentes alinhadas

Como as telas já existem, o ganho real vem de garantir que elas usem os **tokens** como
fonte de verdade (e não valores hardcoded). Antes de gerar UI nova, peça ao Claude Code para:

1. Verificar se `LinkaTheme.kt` (`LkColors` / `LkSpacing` / `LkRadius` / `linkaTypography`) é
   de fato a origem de cores, espaçamentos e tipos das telas atuais.
2. Substituir qualquer cor/spacing hardcoded pelos tokens equivalentes (tabela abaixo).
3. Reaproveitar os componentes existentes (`…/ui/component/*.kt`) em vez de recriar.

---

## Design Tokens (referência rápida)

> CSS var (artefatos web) → valor → equivalente Compose (`LinkaTheme.kt`).

### Marca
| Token | Valor | Uso |
|---|---|---|
| `--accent` | `#6C2BFF` | CTA primário, highlights, seleção, nav ativa |
| `--accent-blue` | `#2563EB` | Links/badges informativos, dados "Móvel", gradiente do avatar |

### Status (semáforo)
| Token | Valor | Uso |
|---|---|---|
| `--success` | `#22C55E` | Conexão boa, testes OK |
| `--warning` | `#F5A623` | Alertas moderados |
| `--error` | `#FF4D4F` | Erros críticos, falhas |

### Fases do SpeedTest
| Token | Valor |
|---|---|
| `--phase-latencia` | `#60A5FA` |
| `--phase-download` | `#34D399` |
| `--phase-upload` | `#FBBF24` |

### Superfícies — Light (padrão)
| Token | Valor |
|---|---|
| `--bg-primary` | `#FFFFFF` |
| `--bg-secondary` | `#F3F4F6` |
| `--bg-card` | `#FFFFFF` |
| `--text-primary` | `#0D0D1A` |
| `--text-secondary` | `#6B7280` |
| `--text-tertiary` | `#9CA3AF` |
| `--border` | `#E5E7EB` |

### Superfícies — Dark
| Token | Valor |
|---|---|
| `--bg-primary` | `#000000` |
| `--bg-secondary` | `#1A1A1A` |
| `--bg-card` | `#111111` |
| `--text-primary` | `#F3F4F6` |
| `--border` | `#2A2A2A` |

### Orbit (IA — sempre escuro, não adapta ao tema)
| Token | Valor |
|---|---|
| `--linka-black` | `#0D0D1A` (background) |
| `--linka-dark-surface` | `#1A0B2E` |
| `--linka-dark-card` | `#1E1130` |
| `--linka-text-on-dark` | `#F3F4F6` |

### Containers semânticos (Light)
| Token | Valor |
|---|---|
| `--warning-container` / `--on-warning-container` | `#FFF3CD` / `#7A4E00` |
| `--amber-surface` | `#FFF8E6` |
| `--success-container` / `--on-success-container` | `#D1FAE5` / `#065F46` |

### Espaçamento — grid 8dp (`LkSpacing`)
`--space-xs` 4 · `--space-sm` 8 *(unidade base)* · `--space-md` 12 · `--space-lg` 16
*(padding de tela + card)* · `--space-xl` 24 · `--space-xxl` 32. Toque mínimo: **56dp**.

### Raios (`LkRadius`)
`--radius-card` 16 · `--radius-button` / `--radius-input` 12 · `--radius-pill` 999.
Ícones em chip circular e avatares: totalmente redondos (36–44dp típico).

### Tipografia — Roboto, escala Material 3 (`linkaTypography`)
Sem fonte customizada — Compose usa o default do sistema (Roboto). Classes em
`colors_and_type.css`: `display-large` (700/34) · `headline-large` (600/24) ·
`headline-medium` (600/20) · `headline-small` (600/18) · `title-large` (500/16) ·
`title-medium` (500/15) · `title-small` (500/14) · `body-large` (400/16) ·
`body-medium` (400/14) · `body-small` (400/12) · `label-*` (500/400, 14–11) ·
`overline` (600/11, UPPERCASE, +0.3px letter-spacing).

### Alpha (convenção do codebase)
Tints são cor-em-alpha por sufixo hex: `1A`=10% · `1F`=12% · `26`=15% · `33`=20% ·
`40`=25%. Ex.: card de Wi-Fi conectado = `success @12%` fill; seleção/IA = `accent @8–12%`
fill + `accent @25–30%` borda; banner de alerta = `warning @12%` fill.

---

## Regras não-negociáveis (resumo)

**Visual**
- Material Design 3 claro, brilhante, neutro. Um único acento violeta `#6C2BFF` para
  destaque; status verde/âmbar/vermelho carregam o significado.
- **Sem** imagens fotográficas, hero full-bleed, padrões/texturas ou gradientes decorativos.
  Gradiente existe em exatamente 2 lugares: avatar de perfil e header de Diagnóstico/IA
  (linear `accent → accent-blue`).
- Cards: raio 16dp, fundo `--bg-card`, hairline `1px --border`, **flat** (elevação por tom de
  superfície, não por sombra). Sombra suave só no pill de segmento ativo e no botão de
  speedtest.
- Cards "status" tingidos: cor semântica em alpha baixo (fill) + ~25–30% (borda).
- Ícone em chip circular preenchido com a cor semântica ~10% é o motivo recorrente.
- Barras de sinal: glyph vertical de 4 barras (alturas 6/9/12/16dp, largura 3dp, raio 1dp),
  cor pela qualidade (verde Forte / âmbar Regular / vermelho Fraco), vazias em `--border`.

**Conteúdo / voz**
- Português do Brasil, sempre. Fala **com** o usuário usando "você", a partir do mundo dele
  ("Sua internet por fibra").
- Títulos e botões em **sentence case**; overlines/labels de seção em **UPPERCASE** com
  letter-spacing leve.
- Métrica crua **sempre** acompanhada de um veredito humano (`Excelente`, `Bom`, `Regular`,
  `Fraco`, `Forte`). Separador inline: ponto médio `·`.
- **Sem emoji.** Significado vem de ícones Material + cor semântica. Único glyph decorativo: o
  `✓` dentro do badge "Conectado".

**Sistemas**
- Ícones: **Material Symbols / Material Icons (Outlined)** apenas. Na web:
  `<span class="material-symbols-outlined">wifi</span>`.
- Superfícies **Orbit** (IA) são **sempre escuras** (`#0D0D1A` / `#1A0B2E` / `#1E1130`),
  independentes do tema.
- Movimento contido e funcional: fades/offsets rápidos, ripple M3, sem bounce nem floreio.
  Nav inferior some no scroll-down e durante teste em andamento.

**Layout fixo**
- `CenterAlignedTopAppBar` (título centralizado, `ProfileAvatarButton` à esquerda, ação
  contextual à direita) + `NavigationBar` de 5 abas (Início · Velocidade · Sinal · Histórico ·
  Ajustes). Conteúdo rola em `LazyColumn`. Telas secundárias sobrepõem as abas; fluxos
  profundos ganham seta de voltar. `ModalBottomSheet` é o padrão para permissões, análise de
  topologia e pickers.

---

## Telas / Superfícies do app (referência)

As superfícies centrais — recriadas em alta fidelidade em `ui_kits/android/`:

- **Início (Home):** estado da conexão, cards de resumo, atalhos.
- **Velocidade (SpeedTest):** seletor segmentado Rápido/Completo/Triplo, botão violeta de
  iniciar com glow, gauge ao vivo por fase (latência/download/upload) → **Resultado**.
- **Sinal (Wi-Fi):** card de rede conectada, chips de filtro de banda (Todos / 2.4 / 5 / 6
  GHz), lista de redes com barras de sinal e métricas cruas + veredito.
- **Histórico:** resultados anteriores.
- **Diagnóstico / Orbit (IA):** superfície escura, bolhas de chat com TypewriterText, bubble
  "pensando" pulsante.

Veja `_ref/` (capturas do app real, **não** para shipping) e `preview/` para os cards do DS.

---

## Arquivos para referência

- `colors_and_type.css` — tokens + classes de tipo (importe em qualquer artefato web).
- `README.md` (dentro da skill) — fundamentos completos de conteúdo, visual e iconografia.
- `ui_kits/android/index.html` — protótipo click-through; `chrome.jsx` tem os tokens `LK` e
  primitivos compartilhados; `screens.jsx`, `speedtest.jsx`, `orbit.jsx`, `app.jsx`.
- `preview/*.html` — cards de cores, tipo, espaçamento, raios e componentes.
- No seu codebase: `LinkaTheme.kt` (tokens), `…/ui/component/*.kt` (25 composables),
  `…/ui/screen/*.kt` (telas), `docs_ai/design-system/*.md` (docs originais).
