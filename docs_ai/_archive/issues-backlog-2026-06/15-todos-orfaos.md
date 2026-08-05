## Contexto
TODOs/FIXMEs espalhados sem issue vinculada acumulam dívida invisível. Cada TODO deve virar issue rastreável ou ser removido.

## Evidência
- `featureHistory/src/main/kotlin/io/signallq/app/kotlin/feature/history/ExportadorHistoricoPDF.kt:15-16` — "TODO v2.0: Renderizar HTML/CSS via WebView"
- `featureHistory/src/main/kotlin/io/signallq/app/kotlin/feature/history/UptimeNarrativaEngine.kt:15-17` — 3 TODOs
- `app/src/main/kotlin/io/signallq/app/kotlin/ui/screen/SinalScreen.kt:497` — "TODO: integrar com TopologiaWifiEngine"

## Critério de aceite
- [ ] Inventário de TODOs/FIXMEs gerado: `Get-ChildItem -Recurse -Include *.kt | Select-String -Pattern 'TODO|FIXME'`
- [ ] Cada TODO classificado: vira issue (vincular nº) OU removido OU rebatizado como `// NOTE:` com justificativa
- [ ] Regra Detekt `ForbiddenComment` ativa exigindo formato `TODO(#nnn):`

## Como verificar
```powershell
Get-ChildItem -Recurse -Include *.kt -Path .\app\src\main,.\core*\src\main,.\feature*\src\main | Select-String -Pattern 'TODO[^(]|FIXME'
# qualquer hit deve estar no formato TODO(#nnn):
```

## Notas para o agente
- Skills: `signallq-arch`
- Dependências: facilitado por #5 (Detekt configurado)
