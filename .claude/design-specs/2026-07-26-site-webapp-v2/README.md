# SignallQ — Protótipo (2 páginas)

Protótipo visual do site/PWA SignallQ, reduzido a duas páginas. Conteúdo 1:1 ao que
estava implementado no site; a medição é **mockada** (nenhum teste real roda).

```
Desktop.dc.html   → /         site em largura desktop
Webapp.dc.html    → /webapp   mesmo conteúdo dentro de um moldura iPhone 17 (402×874)
shared/mock-test.js          dados fixos + reprodução visual das fases da medição
_ds/                         bundle do SignallQ Design System (não editar)
```

Fora de escopo neste protótipo: Android/Google Play, SignallQ PRO, Admin, histórico,
páginas institucionais.

## Executar localmente

```bash
npx serve .
```

Não abra com `file://` — `shared/mock-test.js` é um módulo ES e exige http(s).

## Números mostrados

Resultado fixo (Download 92.4 Mbps · Upload 41.2 Mbps · Latência 14 ms · Jitter 3.2 ms),
com vereditos derivados das mesmas faixas de `shared/classification.js`. Para mudar o
resultado exibido, edite `RESULT` / `RESULT_VIEW` em `shared/mock-test.js`.
