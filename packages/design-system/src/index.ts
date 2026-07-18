// Tokens
export { LK, ORB } from './tokens.js';

// Utils
export { hexA } from './utils.js';

// Primitives
export * from './primitives/index.js';

// Layout
export * from './layout/index.js';

// Animations
export * from './animations/index.js';

// Telas/fluxos NÃO fazem parte do design system — são composições de produto,
// vivem como protótipos (tobe/ + templates/ no Claude Design), não como componentes
// reutilizáveis. Ver docs_ai/design-system/DECISAO_SEPARACAO_DS_PROTOTIPOS_2026-07-18.md.
// (src/screens/ mantido por ora; export removido para não entrar no bundle do DS.)
