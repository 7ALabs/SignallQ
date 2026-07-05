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

> Marca anterior ("linka") é histórica e **não deve ser usada** em nenhum material novo.
