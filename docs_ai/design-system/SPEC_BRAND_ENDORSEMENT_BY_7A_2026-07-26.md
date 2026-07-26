# Spec — Componente `BrandEndorsement` ("by 7A")

- **Status:** parcialmente implementado (Admin/React e Site/React feitos, com símbolo real desde
  2026-07-26; Android pendente — ver seção 6)
- **Última validação:** 2026-07-26 (atualizado por Bruno — wiring do símbolo real recebido do Luiz)
- **Fonte de verdade:** este arquivo
- **Escopo:** componente de assinatura institucional "by 7A", superfícies do SignallQ Admin
  (implementado), SignallQ Site (implementado), SignallQ Consumer e SignallQ PRO (spec para o
  Camilo implementar em Compose)
- **Responsável:** Marina (spec + implementação Admin), Bruno (implementação Site + wiring do
  símbolo real em Admin e Site, reforço pontual — ver `.claude/CLAUDE.md`, "Bruno emprestado do
  Agente Virtual"), Camilo (implementação Android)
- **Issue:** [#1376](https://github.com/7ALabs/SignallQ/issues/1376)
- **Decisão de marca de origem:** `7ALabs/.github/BRAND.md` (decisão já aprovada pelo Luiz,
  citada na própria issue — não reaberta aqui)
- **Nota de escopo:** a issue original não lista o SignallQ Site como superfície-alvo (só
  Consumer/PRO/Admin/Agente) — a cobertura do Site foi incluída por instrução direta da dispatch
  de reforço que gerou esta atualização, por ser a única superfície pública institucional que
  ainda faltava. Não amplia o escopo dos outros produtos.

## 1. Regras de conteúdo (não mudam, herdadas da issue/BRAND.md)

- Texto: `by 7A` — "by" em caixa baixa, peso normal; "7A" com peso maior (bold), mesmo tamanho de
  fonte que "by" (não é um lockup com tamanhos diferentes).
- Nunca o lockup completo "7A Labs" em tela operacional — só em texto legal/institucional corrido
  (ex.: "SignallQ é um produto da 7A Labs."), fora do escopo deste componente.
- Não repetir em todas as telas — só superfícies institucionais explicitamente listadas abaixo.
- Nunca em: Home, navegação principal, todos os cards, durante o Speedtest (Consumer); dashboards,
  tabelas e telas operacionais (Admin).

## 2. Símbolo 7A — resolvido em 2026-07-26

Bloqueio original: a issue e o `BRAND.md` pedem "uso preferencial do símbolo 7A existente", mas até
2026-07-25 não existia nenhum arquivo de símbolo 7A no repo nem em `7ALabs/.github`. Não foi
fabricado/desenhado um símbolo à mão nesse período — só a variante somente texto foi shippada
inicialmente.

**Resolvido:** o Luiz forneceu o asset vetorial real (`brand/7alabs-symbol-dark.svg` /
`7alabs-symbol-light.svg`, viewBox `267 164 763 653`, ~1.17:1 — não quadrado). Ver
`brand/README.md`, seção "Símbolo institucional 7A". Cópias locais em
`SignallQ Admin/public/brand/7a/symbol-{dark,light}.svg` e
`SignallQ Site/public/brand/7alabs-symbol-{dark,light}.svg` (nomenclatura de cada cópia segue a
convenção de `public/brand/` já usada por cada app).

`BrandEndorsement` agora resolve `symbolSrc` sozinho por tema (default), sem exigir que quem chama
passe o caminho manualmente — `symbolSrc` continua existindo só como override explícito. Detalhe
por app nas seções 3 e 4b.

## 3. Componente React/TS (implementado)

`SignallQ Admin/src/components/ui/BrandEndorsement.tsx`

```ts
interface BrandEndorsementProps {
  variant?: "text" | "symbol-text";   // default "text"
  size?: "compact" | "default";        // default "default"
  theme?: "dark" | "light";            // default: detectado sozinho (data-theme do doc, fallback matchMedia)
  symbolSrc?: string;                  // override explícito do caminho padrão, opcional
  className?: string;
  id?: string;
}
```

- **Tamanhos:** `compact` = `10px`; `default` = `11px` (fonte da assinatura, sempre menor que o
  corpo de texto padrão do Console, que é `13-15px` nas telas existentes).
- **Cor:** `var(--sq-text-tertiary)` — já é o token de menor hierarquia do Design System do
  Admin, já theme-aware (muda sozinho entre claro/escuro, confirmado em `index.css`), garante
  contraste discreto sem precisar de hex hardcoded por tema.
- **Peso:** "by" = 400 (normal), "7A" = 700 (bold) + `letter-spacing: 0.02em`.
- **Símbolo (2026-07-26):** quando `variant="symbol-text"`, resolve `symbolSrc` sozinho a partir
  de `theme` — mesma convenção de prop `theme: "dark" | "light"` já usada por
  `Sidebar`/`NavRail`/`BottomNav`/`Topbar` (vem de `useTheme()` em `App.tsx`). Se `theme` não for
  passado, detecta via `document.documentElement.getAttribute("data-theme")` (já aplicado por
  `useTheme()`) com fallback para `prefers-color-scheme`. `symbolSrc` continua existindo só como
  override explícito de um caminho específico.
- **Proporção do símbolo:** viewBox real `763x653` (~1.17:1, não quadrado) — o `<img>` fixa
  `height` por tamanho (`12px`/`14px`) e usa `width: "auto"`, deixando o navegador preservar a
  proporção intrínseca do SVG em vez de forçar quadrado.
- **Acessibilidade:** o `<img>` do símbolo leva `alt=""` + `aria-hidden="true"` (decorativo — o
  texto ao lado já carrega o significado). O texto "by 7A" nunca é ocultado de leitor de tela — é
  o próprio conteúdo informativo, não decoração.
- Sem dependência nova — só `React`, consistente com o requisito da issue de não adicionar
  dependência externa só para a assinatura.

Teste: `BrandEndorsement.test.tsx` (7 casos — render texto, resolução do símbolo real por tema
["dark"/"light"], proporção não quadrada, símbolo decorativo, override de `symbolSrc`,
`id`/`className` customizados).

## 4. Onde entra no Admin (implementado)

| Superfície | Arquivo | Tamanho | Observação |
|---|---|---|---|
| Login | `src/auth/LoginPage.tsx` | `default` | Abaixo do formulário, `mt-10`, hierarquia baixa — não compete com o wordmark "SignallQ Admin" do topo. `variant="symbol-text"` desde 2026-07-26; `theme` recebido de `App.tsx` (mesmo state de `useTheme()`, adicionado como prop nova de `LoginPage` — antes não era repassado). |
| Rodapé institucional | `src/components/layout/Sidebar.tsx` | `compact` | Instância única, dentro do bloco de rodapé já existente (usuário + tema), separada por `border-top`. Chrome persistente (não é conteúdo de tela, não "repete por tela" no sentido da regra da issue). Só existe hoje no breakpoint desktop (`Sidebar`, `lg:`+) — ver pendência na seção 5. `variant="symbol-text"` desde 2026-07-26; `theme` já existia como prop de `Sidebar`, só passado adiante. |

## 4b. Componente React/TS no Site (implementado, Bruno)

`SignallQ Site/src/components/BrandEndorsement.tsx` — mesmo contrato de props que o do Admin
(`variant`/`size`/`symbolSrc`/`className`/`id`), reimplementado localmente em vez de compartilhado
via pacote: o Site já tinha decidido consumir o design system via CSS puro (`tokens.css`), não via
componentes React de outro app (ver `SignallQ Site/CLAUDE.md`, "Decisões técnicas relevantes") —
importar o componente do Admin criaria uma dependência cross-app fora desse padrão, decisão maior
do que cabe a este dispatch pontual. Usa os tokens equivalentes do Site (`var(--text-tertiary)`,
`var(--font-sans)`, ambos vindos do mesmo `packages/design-system/styles/tokens.css` importado por
`src/index.css`), garantindo o mesmo comportamento theme-aware do Admin.

Teste: `BrandEndorsement.test.tsx` (mesmos 7 casos do Admin, adaptados à prop `isDark: boolean` em
vez de `theme: "dark" | "light"` — ver nota de convenção abaixo).

**Convenção de tema divergente do Admin, intencional:** o Site já tinha o padrão `isDark: boolean`
estabelecido por `Logo.tsx` (recebe o booleano já resolvido do chamador, que por sua vez chama
`useSystemTheme()` uma única vez) — `BrandEndorsement` do Site segue essa mesma convenção em vez de
`theme: "dark" | "light"` do Admin, para não introduzir um segundo mecanismo de tema no mesmo app.
Default `isDark = false` (claro), igual ao default de `Logo`.

**Onde entra:** rodapé institucional (`src/components/SiteFooter.tsx`), nas 3 densidades
responsivas (mobile/compact/full) — já existia texto plano "by 7A" na linha de copyright
(`COPYRIGHT`), substituído pelo componente porque o texto corrido não expressava a hierarquia
visual aprovada ("by" peso normal, "7A" peso maior). Tamanho `compact`, chrome persistente (rodapé
de todas as páginas do site), não é "repetir em toda tela" no sentido da regra da issue — é a
mesma lógica já aceita para o rodapé do Sidebar no Admin. `variant="symbol-text"` + `isDark`
(reaproveitando o `isDark` que `SiteFooter` já calcula via `useSystemTheme()` para o `Logo`) desde
2026-07-26.

Testes ajustados: `SiteFooter.test.tsx` — a asserção de texto corrido do copyright foi dividida em
4 fragmentos (`getAllByText` para prefixo, "by", "7A" e sufixo), já que o texto passou a ser
composto por elementos diferentes em vez de uma única string.

## 5. Pendência real — "seção Sobre/versão" do Admin não existe hoje

A issue pede a assinatura também em "seção Sobre / versão" do Admin. **Não implementei isso** —
não existe hoje nenhuma tela "Sobre" no Admin. O candidato mais próximo,
`src/features/app-versions/VersionsTab.tsx`, é uma tela **operacional** (dashboard de métricas de
release, crash rate, Play Console) — não uma superfície institucional, e a issue proíbe
explicitamente repetir a assinatura em dashboards. `SettingsPage.tsx` também não serve: é
construído em paridade 1:1 com um mockup do Luiz já validado (comentário `paridade com mockup do
Luiz` no próprio arquivo) — adicionar uma seção nova ali seria inventar conteúdo fora do que foi
aprovado, o que meu papel proíbe fazer por conta própria.

**Proposta para a Claudete decidir (não decidi sozinha):**
1. Criar uma tela "Sobre" nova e pequena (versão do build, ambiente, link para changelog, "by 7A")
   — precisa de protótipo/aprovação antes, não algo para eu inventar layout sozinha; ou
2. Considerar a assinatura já presente no rodapé da Sidebar (item 4 acima, visível em toda
   navegação autenticada) como cobertura suficiente de "versão/institucional" para o Admin, sem
   tela dedicada nova — mais simples, sem risco de invenção de conteúdo.

Também pendente: NavRail (tablet) e BottomNav (mobile) não têm rodapé — a assinatura no Admin
hoje só aparece no breakpoint desktop (`Sidebar`). Replicar para os outros dois breakpoints é
possível, mas não fiz porque ambos são navegação compacta (ícone-only/bottom bar) sem espaço de
chrome equivalente hoje — decisão de onde encaixar também caberia à Claudete/Camilo se for
considerado necessário.

## 6. O que falta para o Camilo (Android — Consumer + PRO)

Fora do escopo deste dispatch (Marina só fez a parte React/TS). Tokens e nomes-alvo sugeridos para
o Composable, para reduzir ambiguidade na implementação:

- **Nome sugerido:** `BrandEndorsement` (mesmo nome do React, por consistência de vocabulário
  entre plataformas) — parâmetros equivalentes: `variant: BrandEndorsementVariant` (`Text` /
  `SymbolText`), `size: BrandEndorsementSize` (`Compact` / `Default`), `symbolRes: Int? = null`.
- **Cor:** token de texto terciário/mais baixo já existente no M3 theme de cada produto (Consumer
  usa paleta violeta, PRO usa paleta azul — **não misturar as duas**, ver `.claude/CLAUDE.md`
  seção "Produtos e Superfícies").
- **Tamanho de fonte:** replicar a mesma relação usada no Admin — `compact` menor que `default`,
  ambos abaixo do corpo de texto padrão do produto.
- **Acessibilidade Compose:** símbolo decorativo usa `Image(..., contentDescription = null)`
  (equivalente ao `alt=""` + `aria-hidden` do React); o texto "by 7A" nunca fica com
  `contentDescription = null` — é informativo.
- **Onde entra (Consumer):** tela Ajustes/Sobre, informações de versão/créditos/licenças,
  eventualmente splash final de onboarding se não gerar ruído — todos pendentes de localizar o
  arquivo real (`AjustesScreen.kt`, 771 linhas, já é dívida técnica documentada em
  `.claude/rules/higiene-e-padronizacao-repositorio.md` seção 4.4 — extrair como sub-seção
  própria, não inchar o arquivo).
- **Onde entra (PRO):** tela Ajustes/Sobre, créditos/versão, rodapé do laudo PDF com hierarquia
  abaixo da marca/dados do profissional — formulação sugerida pela issue: `Gerado com SignallQ
  PRO · by 7A`.
- **Asset do símbolo já existe (seção 2 resolvida):** Consumer e PRO podem usar o mesmo
  `brand/7alabs-symbol-{dark,light}.svg` — falta só o Composable e o wiring, não o asset.

## 7. Critérios de aceitação da issue — status desta entrega (Admin + Site)

- [x] Componente documentado no Design System (este arquivo).
- [x] Símbolo real 7A implementado em Admin e Site (variante `symbol-text` ativa em login, rodapé
      do Sidebar e rodapé do Site) — resolvido sozinho por tema, sem exigir caminho manual do
      chamador (seção 2).
- [ ] SignallQ Consumer — fora de escopo deste dispatch (Camilo).
- [ ] SignallQ PRO — fora de escopo deste dispatch (Camilo).
- [x] SignallQ Admin exibe a assinatura no login e no rodapé sem poluir dashboards/tabelas.
      Seção "Sobre/versão" pendente — ver seção 5 (decisão de produto necessária antes de
      implementar, não é um "esqueci").
- [x] Temas claro e escuro — cor do texto usa token já theme-aware (`--sq-text-tertiary`); símbolo
      troca de arquivo por tema (`theme`/`isDark`), coberto por teste automatizado nos dois apps.
      Sem ambiente de browser disponível nesta sessão para captura de screenshot ao vivo —
      recomendo o Rhodolfo confirmar visualmente no gate de QA.
- [x] Acessibilidade — símbolo decorativo oculto de leitor de tela quando presente; texto sempre
      acessível. Coberto por teste automatizado.
- [x] Sem aumento de tempo de inicialização — componente é `span`/`img` estático, sem
      fetch/efeito.
- [x] Sem dependência externa nova.
- [ ] Testes e evidências visuais anexados à PR — testes automatizados incluídos; evidência visual
      (screenshot) pendente, recomendo o Rhodolfo capturar no ambiente de preview antes do merge
      final da issue (quando a parte Android também estiver pronta).
- [x] `BRAND.md` continua coerente — nenhuma mudança feita nele.
