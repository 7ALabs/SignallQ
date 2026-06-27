# Design Sync — Notas de Sincronização

## Re-sync

Para re-sincronizar após mudanças nos componentes:

```sh
cd packages/design-system
npm run build
node ../../.ds-sync/package-build.mjs
```

O `_ds_sync.json` serve como âncora de diff — componentes sem mudança de hash são pulados.

## Riscos conhecidos

- **Render check desativado**: Playwright não instalado neste setup. Previews foram
  inspecionados manualmente via código-fonte, não por screenshot. Ao re-sincronizar,
  considere `npm install -D playwright` + `npx playwright install chromium` para
  ativar o `--render-check`.

- **Material Symbols**: As classes de ícone (ex: `wifi`, `signal_cellular_alt`) dependem
  da fonte `Material Symbols Outlined` carregada pelo `styles.css`. Se algum preview
  mostrar texto em vez de ícone, verificar se o CDN do Google Fonts está acessível.

- **ORB surfaces**: Os componentes `SignallQScreen` e `Thinking` usam fundo escuro via
  tokens `ORB` (`#0D0D1A`). O agente de design deve usar esses tokens, não `LK.bgPrimary`,
  nestas superfícies específicas.

- **Tokens CSS vs JS**: `styles/tokens.css` e `src/tokens.ts` devem permanecer em sync
  manual. Alterar um sem alterar o outro quebra paridade entre uso CSS e uso inline.

## Componentes fora do escopo inicial

Os seguintes componentes existem na fonte Android (`ui_kits/android/`) mas não foram
incluídos nesta sincronização por não terem equivalente no pacote React ainda:
- `DiagnosticoOverlay` (overlay de diagnóstico IA — usa `SignallQScreen` como base)
- `SpeedGauge` (animação de gauge — parcialmente em `SpeedFlow`)
