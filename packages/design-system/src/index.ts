// Tokens
export { LK, LK_DARK, SPACE, STATE, ORB } from './tokens.js';

// Utils
export { hexA } from './utils.js';

// Primitives
export * from './primitives/index.js';

// Layout
export * from './layout/index.js';

// Animations
export * from './animations/index.js';

// Brand — marca oficial (símbolo). Fundação de marca do DS.
export * from './brand/index.js';

// Controls — primitivos interativos (Button, IconButton, TextField, Switch, Checkbox,
// Chip, SegmentedControl, Tabs, Dialog).
export * from './controls/index.js';

// Telas/fluxos NÃO fazem parte do design system — são composições de produto,
// vivem como protótipos (tobe/ + templates/ no Claude Design), não como componentes
// reutilizáveis. Ver docs_ai/design-system/DECISAO_SEPARACAO_DS_PROTOTIPOS_2026-07-18.md.
// (src/screens/ mantido por ora; export removido para não entrar no bundle do DS.)
