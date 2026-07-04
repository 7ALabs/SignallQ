# Changelog — SignallQ PWA

## Nao lancado

### Fix
- **Icones nao renderizam no Safari/iOS (GitHub #365).** O componente `Icon`
  (`src/design-system/components/Icon/`) usava ligadura tipografica do
  Material Symbols (`<span class="material-symbols-outlined">nome</span>`),
  dependente de `font-feature-settings: 'liga'`. O WebKit/Safari nao aplica
  essa ligadura de forma confiavel — o nome literal do icone (ex: `arrow_back`)
  aparecia como texto em vez do glifo. Tentativas anteriores (forcar `liga` via
  CSS, liberar Google Fonts no CSP) nao resolveram porque o problema e de
  suporte do motor de renderizacao, nao de carregamento de fonte.

  **Solucao:** migrado para SVG inline. Os paths dos 33 icones usados no app
  (conjunto fechado, todos vindos de literais no codigo — nenhum vem cru do
  worker de IA) foram extraidos do pacote `@material-symbols/svg-400` v0.45.5
  (outlined, weight 400) e do catalogo oficial `fonts.google.com/icons`
  (para `auto_awesome` e `install_mobile`, ausentes desse pacote especifico) e
  bundled em `src/design-system/components/Icon/iconPaths.ts`. Sem fetch
  externo em runtime, sem CDN, sem dependencia de fonte — funciona offline e
  em qualquer navegador. A API do componente (`name`, `size`, `className`,
  `style`, `aria-hidden`) nao mudou; os 46 pontos de uso existentes (incluindo
  os mapas `VERDICT_ICON`/`LEVEL_ICON`) continuam funcionando sem alteracao.

  Removido: regra `.material-symbols-outlined` (`src/styles/tokens.css`),
  regra duplicada em `src/design-system/styles.css`, `<link>` da fonte
  Material Symbols Outlined no `index.html`, e a dependencia de dev
  `@material-symbols/svg-400` (usada so para extrair os paths, nao roda em
  producao). Adicionada regra base `.sq-icon` para alinhamento vertical do
  SVG. Fonte Roboto (texto) mantida — nao relacionada ao bug.
