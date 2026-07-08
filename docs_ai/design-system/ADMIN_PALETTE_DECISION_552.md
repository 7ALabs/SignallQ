---
issue: gmmattey/linka-android#552 / #746 (item 1)
tela: SignallQ Admin — decisão de token de design (paleta)
autor: Felipe (Admin & Dados)
data: 2026-07-08
status: aplicado (Fase 3)
---

# Decisão: paleta semântica real no SignallQ Admin

## Contexto

A paleta original do Admin (`SignallQ Admin/src/index.css`, SIG-156) era deliberadamente
monocromática: `--success` e `--attention` apontavam para `--text-primary` (cinza) em todos os
temas (dark padrão, dark explícito, light, `prefers-color-scheme: light`). Só `--error` tinha cor
própria (`#FF4D4F`).

Efeito colateral encontrado na revisão da Lia (PR #745, issue #746 item 1): como o token não
funcionava, várias telas já contornavam isso hardcodando classes Tailwind direto no componente
(`text-emerald-400`, `text-amber-500`, `text-red-400` em `ErrorsPage.tsx`, `DiagnosticsPage.tsx`)
em vez de usar `var(--success)` / `var(--attention)`. Sintoma de token quebrado, não de estilo
intencional.

O Luiz pediu, para a Fase 3 (telas com contrato de dados novo), visual de dashboard corporativo
tipo Google Analytics / Google Cloud Console — densidade de informação alta, cards limpos,
semântica de cor real. Paleta monocromática é o oposto disso: qualquer veredito (Bom/Regular/Ruim)
fica visualmente idêntico, o que contraria a regra do design system de "métrica crua sempre
acompanhada de veredito humano" — o veredito perde força se não tem cor.

## Decisão

1. **`--success` e `--attention` deixam de apontar para `--text-primary`** e passam a ter cor
   própria em todos os temas:
   - Dark (padrão e explícito): `--success: #34D399` (emerald-400), `--attention: #F59E0B`
     (amber-500) — mesmos tons que já estavam hardcoded ad hoc no código, agora tokenizados.
   - Light: `--success: #1E8E3E`, `--attention: #B06000`, `--error: #D93025` — tons alinhados à
     paleta de status do Google Material (Analytics/Cloud Console usam essa faixa de verde/âmbar/
     vermelho em fundo claro), ajustados para contraste AA em texto pequeno sobre `--bg-surface`
     branco.
2. **`--info` deixa de ser um alias de `--text-secondary`** e ganha cor própria (`#60A5FA` dark /
   `#1A73E8` light — mesmo azul que já existia solto em `--sq-accent-blue`), para permitir usar
   informação neutra-mas-visível (ex.: badge "info", série secundária de gráfico) sem confundir
   com texto secundário comum.
3. **`--chart-line-primary/secondary/tertiary`** deixam de ser 3 tons de cinza e passam a
   `--primary` (violeta), `--info` (azul) e `--success` (verde) — necessário para os gráficos
   multi-série do wireframe (ex.: Centro de Controle sobrepõe volume × erro × latência; IA & Custos
   sobrepõe custo × volume por provider). Com 3 tons de cinza essas séries eram ilegíveis lado a
   lado.
4. **`--primary` (`#6C2BFF`, violeta) não muda.** É o acento de marca do SignallQ, mantido —
   a direção "visual Google" é sobre densidade/clareza de dashboard e semântica de status real, não
   sobre trocar a cor de marca por azul do Google.
5. **`--focus-ring`** passa de `--text-secondary` para `--primary` — acessibilidade de foco visível
   ganha prioridade sobre o efeito discreto anterior (relacionado ao item 6 do #746, ainda não
   resolvido nesta task).

## Por que não foi pedida aprovação prévia

O Luiz já autorizou esse ajuste explicitamente ao encomendar a Fase 3 ("resolva a paleta antes de
construir as 4 telas novas... já está autorizado"). Este documento registra o quê mudou e por quê,
conforme pedido.

## Não incluído nesta decisão

- Radius, espaçamento e tipografia dos cards — mantidos como estão (SIG-156), fora do escopo pedido
  (que era especificamente paleta/token de cor).
- Item 6 do #746 (acessibilidade de foco em `<select>` do `GlobalFilters.tsx`) — registrado, mas
  não é bloqueante desta decisão de paleta; deve virar task separada de a11y.
- Itens 2, 3, 4, 5 do #746 (excesso de gráficos em Redes & Provedores, gráfico ausente em Releases,
  10 KPIs no Centro de Controle, uso inconsistente de `MetricCard.verdict`) — fora do escopo desta
  task (Fase 3 cobre 4 telas específicas: Diagnósticos, Uso do App, IA & Custos, Saúde do Sistema).
