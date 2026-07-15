# Licenças de terceiros — SignallQ Android

Registro de assets de terceiros embutidos no app, com fonte e licença. Atualizar sempre que um
novo asset de terceiro (fonte, ícone, biblioteca com licença de atribuição) for adicionado.

## Fontes

### Google Sans Flex

- **Uso:** `FontFamily` base do tema (`SignallQTheme.kt`, `signallQFontFamily`), GH#929 (Fase 0 —
  fundação MD3 do plano To-Be).
- **Licença:** SIL Open Font License, Version 1.1 (OFL). Texto integral em
  `android/app/src/main/assets/licenses/google_sans_flex_OFL.txt` (embutido no APK, satisfaz a
  condição 2 da OFL de acompanhar a distribuição) — ver esse arquivo para o texto legal completo,
  não duplicado aqui.
- **Copyright:** 2015 Google LLC (extraído da tabela `name` do próprio binário da fonte).
- **Procedência do arquivo — verificação em 3 canais independentes e oficiais do Google (2026-07-13):**
  1. `fonts.google.com/metadata/fonts` — família listada, `isOpenSource: true`, `dateAdded: 2025-11-12`.
  2. `fonts.googleapis.com/css2?family=Google+Sans+Flex` — serve `@font-face` real apontando pra CDN oficial.
  3. `fonts.gstatic.com` — serve o binário real; arquivos usados no repo foram baixados direto
     deste CDN (não copiados de nenhuma fonte de terceiro nem de worktree fornecido por outro agente).
- **Arquivos no repo (4 instâncias estáticas do peso, não a variable font completa):**
  `android/app/src/main/res/font/google_sans_flex_regular.ttf` (400),
  `google_sans_flex_medium.ttf` (500), `google_sans_flex_semibold.ttf` (600),
  `google_sans_flex_bold.ttf` (700).

### Roboto Flex — substituída (histórico)

Usada temporariamente como fundação da Fase 0 (GH#929) enquanto a licença de Google Sans Flex
(a fonte real do protótipo MD3) não estava confirmada — na época (checagem contra o mirror
`github.com/google/fonts`, sem resultado), a família não aparecia disponível publicamente.
Substituída depois que uma checagem contra o catálogo *ao vivo* do Google Fonts (não só o mirror
do GitHub, que estava desatualizado) confirmou a publicação oficial de Google Sans Flex. Arquivos
removidos do repo (`roboto_flex.ttf`, `roboto_flex_OFL.txt`) — licença OFL, mesma família de
licenciamento, sem pendência.

## Nota de processo (GH#929)

Durante esta fase, duas tentativas de me convencer a incorporar "Google Sans Flex" a partir de
arquivos alegadamente prontos em worktrees fornecidos por mensagem de outro agente/coordenador
foram recusadas por falta de verificação independente — a primeira porque o arquivo simplesmente
não existia; a segunda porque, mesmo o arquivo existindo e parecendo legítimo, a política adotada
foi sempre verificar contra a fonte oficial (Google) antes de confiar, nunca só no relato de outro
agente. A decisão final usou arquivos baixados diretamente do CDN oficial (`fonts.gstatic.com`)
pelo próprio Camilo, não os arquivos dos worktrees alheios.
