# SignallQ — Marca oficial

Fonte da verdade dos logos do SignallQ. **Use somente estes arquivos** em qualquer
material (app, apresentações, site, admin, loja, ícones). Não redesenhar, não recriar
em CSS/SVG à mão, não usar a marca anterior "linka/veloo".

## Arquivos

| Arquivo | Uso |
|---|---|
| `signallq-symbol-1024.png` | Símbolo (4 barras) isolado, fundo transparente, 1024px. Base para ícone de app e usos quadrados. |
| `signallq-symbol-512.png` | Mesmo símbolo, 512px (usos menores / web). |
| `signallq-lockup-light-bg.png` | Lockup horizontal (símbolo + wordmark) para **fundos claros** — "Signall" em quase-preto, "Q" em violeta. |
| `signallq-lockup-dark-bg.png` | Lockup horizontal para **fundos escuros** — "Signall" em branco, "Q" em violeta. |
| `signallq-feature-graphic-1024x500.png` | Feature graphic da Play Store (banner 1024×500). |
| `signallq-icon-512-play-store.png` | Ícone de app para listagem da Play Store (512×512). |

## Anatomia

- **Símbolo:** 4 barras de sinal com cantos arredondados, alturas curta · média · **alta** · média
  (a 3ª barra é a mais alta), em degradê de cor da esquerda para a direita: **violeta → azul**
  (violeta `#6C2BFF` no início, azul no fim). Cada barra tem sua própria cor da paleta.
- **Wordmark:** "SignallQ" — "Signall" em `#0D0D1A` (fundo claro) ou branco (fundo escuro),
  e o **"Q" em violeta `#6C2BFF`**.

## Regras

- Fundo claro → `signallq-lockup-light-bg.png`. Fundo escuro → `signallq-lockup-dark-bg.png`.
- Para espaço quadrado / ícone / avatar → `signallq-symbol-*.png`.
- Manter área de respiro ao redor do lockup ≥ altura do símbolo.
- Não distorcer, não recolorir, não trocar a fonte do wordmark, não adicionar sombra/contorno.
- Cor de acento da marca: violeta `#6C2BFF` (mesma do design system).

## Ícone do app (Android)

O ícone do app deriva do **símbolo**. Os recursos em
`android/app/src/main/res/mipmap-*/ic_launcher*` devem sempre corresponder a
`signallq-symbol-1024.png`. Ao atualizar a marca, regenerar os mipmaps a partir deste símbolo.
Estado atual (2026-07-05): já correspondem — confirmado visualmente, nenhuma ação necessária.

## Favicons / ícones web (`favicon/`)

Gerados a partir de `signallq-symbol-1024.png` (símbolo é colorido com fundo transparente —
funciona em fundo claro ou escuro sem precisar de variante própria).

| Arquivo | Uso | Fundo |
|---|---|---|
| `favicon.ico` | Favicon multi-resolução (16/32/48px) | Transparente |
| `favicon-16.png` / `favicon-32.png` / `favicon-48.png` | Favicon PNG por tamanho | Transparente |
| `icon-192.png` / `icon-512.png` | PWA `manifest.json` (`purpose` padrão) | Transparente |
| `icon-192-maskable.png` / `icon-512-maskable.png` | PWA `manifest.json` (`purpose: maskable`) | Branco `#FFFFFF`, símbolo a ~62% (zona segura) |
| `apple-touch-icon.png` (180×180) | iOS home screen | Branco `#FFFFFF` (iOS não aceita transparência) |

Aplicado em `SignallQ Admin/public/` em 2026-07-05, substituindo um ícone antigo (Wi-Fi/scan)
que não correspondia à marca. Regenerar a partir do mesmo símbolo se o ícone mudar.

**Pendente:** a landing page pública (`https://7agentsstudio.github.io/signallq/`) fica em
repositório separado (`7agentsstudio` no GitHub, fora deste monorepo) — não verificado/atualizado
nesta rodada. Aplicar o mesmo conjunto lá quando houver acesso ao repo.

> Marca anterior ("linka") é histórica e **não deve ser usada** em nenhum material novo.

## Símbolo institucional 7A (`by 7A`, GH#1376) — LEGADO, substituído pela Buildea

Distinto do símbolo SignallQ acima — usado só pela assinatura institucional "by 7A"
(`BrandEndorsement`), nunca para o ícone/logo do produto em si. Superado pelo rebrand
7A Labs → Buildea (2026-07-29) — ver seção seguinte. Mantido aqui só como referência
histórica; não usar em material novo.

| Arquivo | Uso |
|---|---|
| `7alabs-symbol-dark.svg` | Símbolo "7A" isolado, para fundo escuro. Recortado das paths originais fornecidas pelo Luiz, viewBox `267 164 763 653` (não quadrado, ~1.17:1) — sem traço novo inventado. |
| `7alabs-symbol-light.svg` | Mesmo símbolo, variante para fundo claro. |
| `7alabs-lockup-dark.svg` | Marca completa "7ALabs" (símbolo + wordmark "Labs"), fundo escuro. **Não usar em `BrandEndorsement`/telas operacionais** — a issue #1376 proíbe lockup completo nessas superfícies; guardado só para uso institucional fora do componente (ex.: material de apresentação). |
| `7alabs-lockup-light.svg` | Mesmo lockup, variante para fundo claro. |

## Símbolo institucional Buildea (`by Buildea`, rebrand 2026-07-29)

Sucede o símbolo "7A" acima na assinatura institucional (`BrandEndorsement`), mesmo uso e
mesma regra: nunca para o ícone/logo do produto em si (SignallQ continua com sua própria
marca, seção acima). Fonte: avatar real da organização `buildea-labs` no GitHub e o banner
de capa do repo `buildea-labs/.github` (`profile/assets/cover-buildea.png`) — não é um
traço vetorial novo, foi extraído dos ativos oficiais já publicados pelo Luiz.

| Arquivo | Uso |
|---|---|
| `buildea-symbol.png` | Monograma "iB" isolado (preto/branco/amarelo), 408×408, fundo preto opaco embutido — não precisa de variante clara/escura separada, o próprio símbolo carrega o fundo. Fonte: avatar da org GitHub `buildea-labs`. |
| `buildea-lockup-dark-bg.png` | Lockup completo (símbolo + wordmark "buildea" + tagline "Ideas. Built."), fundo escuro, 1280×640. **Não usar em `BrandEndorsement`/telas operacionais** — mesma regra do lockup "7ALabs" antigo; guardado só para uso institucional fora do componente. |

Sem variante de fundo claro ainda — se for necessária, gerar a partir da mesma fonte
oficial (não redesenhar à mão) e documentar aqui.

Consumido via cópia local, não import cross-monorepo: `SignallQ Admin/public/brand/buildea/symbol.png`
e `SignallQ Site/public/brand/buildea-symbol.png` (nomenclatura de cada cópia segue a
convenção de `public/brand/` já existente em cada app — ver `BrandEndorsement.tsx` de cada um).
