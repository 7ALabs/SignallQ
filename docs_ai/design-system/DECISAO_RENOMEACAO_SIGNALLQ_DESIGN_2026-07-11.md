# Decisão: renomeação linka-design → SignallQ-design + adoção do manual MD3 estrito

**Data:** 2026-07-11
**Responsáveis:** Lia (conteúdo da skill + protótipo), Claudete (varredura de referências e registro)

## O que mudou

1. A skill de design system do projeto foi renomeada de `.claude/skills/linka-design/` para
   `.claude/skills/SignallQ-design/`. Estrutura interna preservada (`colors_and_type.css`,
   `HANDOFF_README.md`, `README.md`, `SKILL.md`, `preview/`, `ui_kits/`, `assets/`).
2. O conteúdo da skill foi migrado para **Material Design 3 estrito**, com base no manual
   "Especificação: Migração para Material Design 3 (estrito)" (Claude Design, projeto
   `e77ea465-291f-4bf5-930c-a267680da04e`, arquivo `templates/md3-migration-spec/Md3MigrationSpec.dc.html`).
   Principais mudanças de token:
   - Tipografia: Google Sans Flex (display/headline/title) + Roboto (body/label), escala MD3 completa (15 estilos).
   - Cor: tríade tonal HCT (Primary/Secondary/Tertiary) derivada de `#6C2BFF`, com correção de contraste
     no tema escuro (tom sobe para tone80 em vez de reusar o tom base do claro — AA→AAA).
   - Elevação: 5 níveis tonais (0/1/3/6/8dp) via tint de superfície, não sombra dura isolada.
   - Forma: escala de 7 tokens (None/XS/SM/MD/LG/XL/Full); card passa de 16dp para 12dp (16dp fica
     reservado a sheets/dialogs).
   - State layers: overlay de opacidade fixa sobre texto/ícone (hover 8% / focus 10% / pressed 12% /
     dragged 16%), aplicado a card clicável, itens de lista/sheet, tabs do BottomNav, ações de TopBar.
   - Motion: easing `emphasized`/`standard` (`cubic-bezier(.2,0,0,1)`) + durações 100/200/300/400ms.
   - Nomenclatura de tokens remapeada para o padrão `--md-sys-color-*` (`role/onRole/roleContainer/onRoleContainer`).
   - Tokens antigos (`--accent`, `--bg-card`, `--text-primary` etc.) viram aliases deprecados apontando
     para os novos, para não quebrar `ui_kits/android/*.jsx` (fora do escopo desta migração).
3. Itens explicitamente fora do escopo do MD3 (decisão de produto, não gap de conformidade) — continuam
   iguais: copy em PT-BR/sentence case/separador "·", superfície sempre-escura do SignallQ AI (ORB),
   ausência de emoji.

## Lacunas sinalizadas pela Lia (não inventadas — precisam de decisão/validação futura)

- `surface-dim` (5º nível de surface container): o manual só dá hex para os 4 níveis de elevação, não
  para o 5º. Valor atual é inferido, não do manual.
- Elevação tonal no tema escuro: manual só especifica valores no tema claro; dark é estimativa.
- `outline` / `outline-variant`: manual pede o token mas não dá valor hex exato.
- Indicador "atualizando ao vivo" (usado no protótipo do #893): **não é componente MD3 padrão** — o
  manual não cobre esse padrão. Lia aplicou as regras que o manual dá (cor tertiary container, motion
  `standard`, state layer no flash de atualização), mas a existência do badge em si é decisão de produto,
  não do MD3, e deve ser validada antes de virar convenção fixa da skill.

## Decisão registrada (2026-07-13): Google Sans Flex confirmada; esclarecimento sobre distinção de fonte

**Contexto anterior (2026-07-11):** o documento inicial sinalizou rejeição a "Google Sans" porque o manual
pede embutição via link do Google Fonts (`fonts.googleapis.com/css2?family=Google+Sans...`). **Google Sans
não é um webfont público** — é fonte proprietária de uso interno do Google (Pixel, apps próprios), não
distribuída pra terceiros via CDN. Essa objeção era válida naquela data.

**Distinção crítica:** o que foi implementado em PR #939 não é "Google Sans", mas **"Google Sans Flex"**
— uma família diferente, com licença SIL OFL genuína (confirmada no catálogo ao vivo do Google Fonts em
2026-07-12). A implementação em #939:
- Baixou os arquivos `.ttf` reais do CDN oficial (fonts.gstatic.com) — não depende de pre-instalação
- Embutiu os 4 pesos (400/500/600/700) como arquivos binários no APK (`android/app/src/main/res/font/google_sans_flex_*.ttf`)
- Confirmou licença SIL OFL 1.1 via `android/app/src/main/assets/licenses/google_sans_flex_OFL.txt` (metadados extraídos do binário)

A objeção original (Google Sans via CDN, só em Pixel) **não se aplica** a Google Sans Flex embutida.

**Decisão do Luiz (2026-07-13):** **Google Sans Flex fica** — é a fonte oficial da tipografia MD3 do SignallQ,
já implementada e mergeada em main via PR #939. A escala tipográfica (15 estilos, pesos/line-height conforme
manual MD3) usa Google Sans Flex para display/headline/title e Roboto para body/label conforme o manual.

## Escopo desta renomeação (o que foi e não foi tocado)

- Atualizado: `.claude/skills/SignallQ-design/` (conteúdo + rename), `.claude/CLAUDE.md` (seção Design
  System + lista de identificadores técnicos), `.claude/agents/{camilo,lia,claudete}.md`,
  `.claude/skills/{auditar-ux,estimativa-impacto}/SKILL.md`, `.claude/commands/linka.md`,
  `docs_ai/ai/UX_FLOW.md`, `docs_ai/AGENTS_QUICK_REFERENCE.md`,
  `docs_ai/design-system/WIREFRAME_ADMIN_REDESIGN_552.md`, `SignallQ Admin/src/types/metrics.ts`.
- **Não tocado, propositalmente:**
  - `.claude/agents/rhodolfo.md` — não tinha referência a `linka-design` antes da mudança.
  - `.claude/agents/_archive/*.md` e `android/CHANGELOG.md` — histórico, não é reescrito.
  - `.agents/skills/linka-design/` — mirror separado e dessincronizado do `.claude/skills/`, fora do
    escopo desta tarefa. Ainda referencia o nome antigo.
  - `.codex/agents/*.toml` (camilo, claudete, claudio, felipe, gema, lia) — configuração de um agente
    Codex paralelo, também ainda referencia `linka-design` e cita agentes já arquivados no squad Claude
    (Cláudio, Felipe, Gema). Parece infraestrutura órfã do squad atual.
  - `.issues/_assign-agents.ps1` — referencia `linka-design` e um agente "Bras" que não existe no squad
    atual.
  - `docs_ai/design-system/{MD3_GUIDELINES,DESIGN_TOKENS,COLORS,TYPOGRAPHY,SPACING,COMPONENTS_ANDROID}.md`
    — não confirmado se já refletem os valores desta migração; podem estar desatualizados em relação à
    skill nova.

**Recomendação:** os quatro pontos acima (`_archive` à parte, que é histórico por design) são candidatos
a uma rodada de `/higiene` — decidir se `.codex/`, o mirror `.agents/skills/`, e `.issues/_assign-agents.ps1`
ainda são infraestrutura viva ou lixo de uma geração anterior de tooling, e sincronizar os docs de
`docs_ai/design-system/` com o novo manual MD3 se ainda estiverem descrevendo o sistema antigo.
