# Fontes dos logos oficiais de operadoras (SIG-292)

Data de coleta: **2026-07-04**.

Este documento registra, para cada operadora do catalogo (`OperadoraLogoCatalog.kt` /
`BancoOperadoras.kt`), a origem exata do asset visual usado em
`android/app/src/main/res/drawable` (VectorDrawable) ou
`android/app/src/main/res/drawable-nodpi` (WebP), quando disponivel. Todo asset e
**bundled localmente** — nenhuma chamada de rede em runtime para exibir logo de operadora.

| Operadora | id | Recurso Android | Arquivo | Fonte (URL exata) | Tipo de asset | Risco |
|---|---|---|---|---|---|---|
| Claro | `claro_net` | `R.drawable.operator_claro_net` | `drawable-nodpi/operator_claro_net.webp` | `https://www.claro.com.br/files/104379/300x300/787be71e56/logo-claro.webp?sq=100` (footer do site oficial claro.com.br) | WebP oficial extraido do site (raster, sem SVG disponivel no header) | Baixo |
| Vivo | `vivo_fibra` | — (fallback cor+monograma) | — | `https://vivo.com.br/` | **Fallback manual** | — |
| TIM | `tim_live` | `R.drawable.operator_tim_live` | `drawable/operator_tim_live.xml` | `https://www.tim.com.br/themes/custom/timbrasil/logo.svg` (header do site oficial) | SVG oficial convertido — usado **apenas o icone (4 barras vermelhas)**, sem o wordmark branco | Baixo |
| Oi | `oi_fibra` | `R.drawable.operator_oi_fibra` | `drawable/operator_oi_fibra.xml` | `https://www.oi.com.br/lumis/portal/file/fileDownload.jsp?fileId=8ABAB6AE9A4E6BF5019A4E74A617026D` (referenciado no JSON-LD `logo` da propria oi.com.br) | SVG oficial convertido (gradiente verde->amarelo + "oi" branco) | Baixo |
| Nio | `nio` | `R.drawable.operator_nio` | `drawable/operator_nio.xml` | `https://www.niointernet.com.br/lumis-theme/br/com/nio/theme/nio/assets/icons/default-logo.svg` (header do site oficial) | SVG oficial convertido | Baixo |
| Algar Telecom | `algar` | `R.drawable.operator_algar` | `drawable-nodpi/operator_algar.webp` | `https://algar.com.br/img/new-logo-algar.png` (alt="Logomarca Algar", institucional/index.html) | WebP oficial extraido do site (raster, resolucao baixa 113x41 — pode perder nitidez em densidades muito altas) | Baixo |
| Unifique | `unifique` | `R.drawable.operator_unifique` | `drawable/operator_unifique.xml` | `https://unifique.com.br/assets/imgs/logo.svg` | SVG oficial convertido | Baixo |
| Brisanet | `brisanet` | — (fallback cor+monograma) | — | `https://www.brisanet.com.br/` | **Fallback manual** — SVG inline no header renderizou com corrupcao visual de path ao extrair via HTML estatico (provavel efeito de hidratacao client-side/Next.js); sem ambiente de browser real neste agente para validar visualmente, optamos por nao arriscar fidelidade da marca | — |
| Desktop | `desktop` | `R.drawable.operator_desktop` | `drawable/operator_desktop.xml` | `https://www.desktop.com.br/` (SVG inline no `<header class="component main-header">`) | SVG oficial convertido — cor fixada em cinza-escuro (`#1A1A1A`); o SVG original usa `fill: var(--custom-brand-color, ...)` (tema claro/escuro dinamico via CSS custom properties) que nao e resolvido fora de um browser real | Medio (cor monocromatica assumida, nao confirmada visualmente no site) |
| Ligga Telecom | `ligga` | `R.drawable.operator_ligga` | `drawable-nodpi/operator_ligga.webp` | `https://liggavc.com.br/wp-content/uploads/2025/04/logo-2025.png` (campo `logo` do JSON-LD schema.org da propria liggavc.com.br) | WebP oficial extraido do site (raster 310x310) | Baixo |
| Vero | `vero` | `R.drawable.operator_vero` | `drawable/operator_vero.xml` | `https://querovero.com.br/icons/vero-logo-v2.svg` | SVG oficial convertido (variante colorida — a variante `vero-logo.svg` padrao e branca, ilegivel em fundo claro) | Baixo |
| Giga+ Fibra | `giga_mais` | `R.drawable.operator_giga_mais` | `drawable-nodpi/operator_giga_mais.webp` | `https://www.gigamaisfibra.com.br/wp-content/uploads/2023/10/GIGA_SUMI_PRINC_COLOR_DIG-1-e1698429006245.png` (campo `logo` do JSON-LD schema.org da propria gigamaisfibra.com.br) | WebP oficial extraido do site (raster 137x40) | Baixo |
| Generico (fallback) | — | `R.drawable.operator_generic` | `drawable/operator_generic.xml` | N/A — icone abstrato desenhado para este app (barras de sinal neutras, sem semelhanca com marca real) | Fallback manual (icone proprio) | — |

## Observacoes gerais

- **Vivo**: o site vivo.com.br carrega o wordmark oficial via sprite SVG injetado por
  JavaScript no cliente (`<svg><use xlink:href="#logo-vivo-vivinho"></use></svg>`, sem
  `<symbol>` presente no HTML estatico). Alem disso, o CDN de assets estaticos
  (`vivo.com.br/content/dam/...`) retornou `403 Access Denied` para todas as tentativas de
  download programatico testadas (inclusive o favicon), indicativo de protecao anti-bot
  (WAF/Akamai) no nivel de edge, independente da URL exata. Sem um navegador real para
  renderizar o JS e sem acesso ao CDN de assets, nao foi possivel extrair um asset oficial
  de forma licita. Mantido no fallback cor+monograma.
- **Brisanet**: o wordmark esta embutido como SVG inline no HTML (nao depende de CDN
  bloqueado), mas o path data extraido renderizou com sobreposicao/corrupcao visual ao ser
  rasterizado (testado com ImageMagick/librsvg). Nao foi possivel confirmar visualmente a
  causa (minificacao, hidratacao client-side alterando o DOM, ou erro de escaping na
  extracao) sem um browser completo. Mantido no fallback cor+monograma por seguranca de
  fidelidade visual.
- **TIM**: a pagina `tim.com.br/sobre-a-tim` (com a suposta secao "Nossa marca" e brand kit
  oficial) nao apresentou, no momento da coleta, nenhum link de download de brand kit/guia
  de marca — apenas o header padrao do site, de onde foi extraido o logo.
- **Claro**: a fonte prioritaria `mondrian.claro.com.br` e um portal interno de design ops
  (SPA React, "DesignOps da Claro"), nao expõe o asset de marca publicamente; usado o
  logo do proprio `claro.com.br` (fonte secundaria da spec) como fallback, conforme previsto.
- **Desktop**: a cor exata do logo depende de CSS custom properties resolvidas em tempo de
  execucao no browser (`--custom-brand-color-1`/`-2` com fallback para `--change-full-color`).
  Sem esse contexto de runtime, foi assumida cor monocromatica escura (segura para fundo
  claro/cards do design system), mas isso e uma inferencia, nao uma confirmacao visual do
  site ao vivo — recomenda-se validacao visual por Lia antes do proximo release.

## Nota legal

As marcas e logos pertencem às respectivas operadoras. O uso no SignallQ é apenas
identificativo, para exibir a operadora detectada ou informada pelo usuário. O app não
deve sugerir parceria, patrocínio, endosso ou vínculo oficial com essas empresas.
