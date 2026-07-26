# Spec — Componente `BrandEndorsement` ("by 7A")

- **Status:** parcialmente implementado (Admin/React feito; Android pendente — ver seção 6)
- **Última validação:** 2026-07-26
- **Fonte de verdade:** este arquivo
- **Escopo:** componente de assinatura institucional "by 7A", superfícies do SignallQ Admin
  (implementado), SignallQ Consumer e SignallQ PRO (spec para o Camilo implementar em Compose)
- **Responsável:** Marina (spec + implementação Admin), Camilo (implementação Android)
- **Issue:** [#1376](https://github.com/7ALabs/SignallQ/issues/1376)
- **Decisão de marca de origem:** `7ALabs/.github/BRAND.md` (decisão já aprovada pelo Luiz,
  citada na própria issue — não reaberta aqui)

## 1. Regras de conteúdo (não mudam, herdadas da issue/BRAND.md)

- Texto: `by 7A` — "by" em caixa baixa, peso normal; "7A" com peso maior (bold), mesmo tamanho de
  fonte que "by" (não é um lockup com tamanhos diferentes).
- Nunca o lockup completo "7A Labs" em tela operacional — só em texto legal/institucional corrido
  (ex.: "SignallQ é um produto da 7A Labs."), fora do escopo deste componente.
- Não repetir em todas as telas — só superfícies institucionais explicitamente listadas abaixo.
- Nunca em: Home, navegação principal, todos os cards, durante o Speedtest (Consumer); dashboards,
  tabelas e telas operacionais (Admin).

## 2. Achado bloqueante — símbolo 7A não existe como asset

A issue e o `BRAND.md` pedem "uso preferencial do símbolo 7A existente", mas **não existe hoje
nenhum arquivo de símbolo 7A** — nem em `brand/` (que só tem os assets do símbolo **SignallQ**,
4 barras, ver `brand/README.md`), nem em `7ALabs/.github` (que só tem `BRAND.md`, texto, sem
pasta de assets). Não fabriquei/desenhei um símbolo — isso violaria a regra de "não redesenhar,
não recriar em CSS/SVG à mão" que já vale para os assets SignallQ e vale por analogia para
qualquer marca 7A Labs.

**Decisão tomada:** implementar e shippar apenas a variante **somente texto** agora. A variante
**símbolo + texto** existe na API do componente (prop `variant="symbol-text"` + `symbolSrc`), mas
sem `symbolSrc` informado ela cai automaticamente para texto — nunca renderiza um símbolo
inventado.

**Pendência real, não decisão minha:** o Luiz (ou quem gerar o asset) precisa produzir o símbolo
7A vetorial (SVG preferencialmente) e colocá-lo em `brand/7a/` (novo diretório, mesmo padrão de
`brand/README.md`) antes que a variante símbolo+texto possa ser usada em qualquer produto.

## 3. Componente React/TS (implementado)

`SignallQ Admin/src/components/ui/BrandEndorsement.tsx`

```ts
interface BrandEndorsementProps {
  variant?: "text" | "symbol-text";   // default "text"
  size?: "compact" | "default";        // default "default"
  symbolSrc?: string;                  // caminho do símbolo 7A, quando existir
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
- **Acessibilidade:** quando `symbolSrc` está presente, o `<img>` leva `alt=""` +
  `aria-hidden="true"` (decorativo — o texto ao lado já carrega o significado). O texto "by 7A"
  nunca é ocultado de leitor de tela — é o próprio conteúdo informativo, não decoração.
- Sem dependência nova — só `React`, consistente com o requisito da issue de não adicionar
  dependência externa só para a assinatura.

Teste: `BrandEndorsement.test.tsx` (4 casos — render texto, fallback de symbol-text sem
`symbolSrc`, símbolo decorativo com `symbolSrc`, `id`/`className` customizados).

## 4. Onde entra no Admin (implementado)

| Superfície | Arquivo | Tamanho | Observação |
|---|---|---|---|
| Login | `src/auth/LoginPage.tsx` | `default` | Abaixo do formulário, `mt-10`, hierarquia baixa — não compete com o wordmark "SignallQ Admin" do topo. |
| Rodapé institucional | `src/components/layout/Sidebar.tsx` | `compact` | Instância única, dentro do bloco de rodapé já existente (usuário + tema), separada por `border-top`. Chrome persistente (não é conteúdo de tela, não "repete por tela" no sentido da regra da issue). Só existe hoje no breakpoint desktop (`Sidebar`, `lg:`+) — ver pendência na seção 5. |

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
- **Bloqueio compartilhado:** o mesmo problema da seção 2 vale aqui — sem o asset do símbolo 7A,
  Consumer e PRO também só podem shippar a variante texto por enquanto.

## 7. Critérios de aceitação da issue — status desta entrega (só Admin)

- [x] Componente documentado no Design System (este arquivo).
- [x] Usa só o texto "by 7A" — não introduz símbolo/identidade nova (bloqueio da seção 2 impede
      a variante símbolo+texto por enquanto, decisão consciente, não gap silencioso).
- [ ] SignallQ Consumer — fora de escopo deste dispatch (Camilo).
- [ ] SignallQ PRO — fora de escopo deste dispatch (Camilo).
- [x] SignallQ Admin exibe a assinatura no login e no rodapé sem poluir dashboards/tabelas.
      Seção "Sobre/versão" pendente — ver seção 5 (decisão de produto necessária antes de
      implementar, não é um "esqueci").
- [x] Temas claro e escuro — usa token já theme-aware (`--sq-text-tertiary`), validado lendo
      `index.css` (blocos `:root`/`.light`), não foi possível captura visual ao vivo nesta sessão
      (sem servidor rodando) — recomendo o Rhodolfo confirmar visualmente no gate de QA.
- [x] Acessibilidade — símbolo decorativo oculto de leitor de tela quando presente; texto sempre
      acessível. Coberto por teste automatizado.
- [x] Sem aumento de tempo de inicialização — componente é `span`/`img` estático, sem
      fetch/efeito.
- [x] Sem dependência externa nova.
- [ ] Testes e evidências visuais anexados à PR — testes automatizados incluídos; evidência visual
      (screenshot) pendente, recomendo o Rhodolfo capturar no ambiente de preview antes do merge
      final da issue (quando a parte Android também estiver pronta).
- [x] `BRAND.md` continua coerente — nenhuma mudança feita nele.
