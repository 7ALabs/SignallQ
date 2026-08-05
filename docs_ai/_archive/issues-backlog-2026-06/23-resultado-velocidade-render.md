## Contexto
`ResultadoVelocidadeScreen` tem um `Column().verticalScroll()` com ~40 composables e usa `Canvas` em `OrbitSymbolSmall()` sem `remember` para valores caros. Em devices mid-tier isso causa drop de frames ao abrir o resultado.

## Evidência
- `app/.../ui/screen/ResultadoVelocidadeScreen.kt:185-527` — Column com verticalScroll grande
- `app/.../ui/screen/ResultadoVelocidadeScreen.kt:600-605` — Canvas em `OrbitSymbolSmall()` sem `remember`

## Critério de aceite
- [ ] `OrbitSymbolSmall` usa `remember` para offsets/cores; ideal: substituir Canvas por `ImageVector` pré-renderizada se a animação for trivial
- [ ] Seções de lista dinâmica (>10 itens) migradas para `LazyColumn` com `key`
- [ ] Lambdas dentro de `Modifier.clickable {}` extraídas para `remember(...)` quando capturam estado
- [ ] Frame time em scroll do resultado ≤ 16ms em Pixel 4a / Moto G7
- [ ] UX visualmente idêntica

## Como verificar
- `adb shell dumpsys gfxinfo io.veloo.app framestats` antes/depois
- Macrobenchmark `FrameTimingMetric` no fluxo Speedtest → Resultado

## Notas para o agente
- Skills: `signallq-design`, `signallq-arch`
- Não mexer em estética; só em estrutura/perf
- Dependências: facilitado por #9 (Baseline Profile mede no mesmo fluxo)
