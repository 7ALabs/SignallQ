# Decisão: fonte de verdade de cor do SignallQ Console — `index.css` vence o protótipo `signallq-admin-md3-tobe`

**Data:** 2026-07-16
**Responsável:** Lia (design do Console)
**Status:** REVOGADA em 2026-07-16 — ver seção "Correção 2026-07-16" no final do documento. Mantida
como registro histórico da investigação; a conclusão original abaixo **não vale mais**.
**Última validação:** 2026-07-16
**Escopo:** SignallQ Console (Admin) — apenas cor. Não afeta o design system Android
(`.claude/skills/SignallQ-design/`, ver `DECISAO_ALINHAMENTO_TOBE_2026-07-13.md`, que trata de um
documento e produto diferentes).

## O que aconteceu

Existe um protótipo Claude Design (`templates/signallq-admin-md3-tobe`, componentes
`Md3Screen00Login`, `Md3Screen01Overview`, `Md3DashboardContent`/`Md3DashboardContentMobile`,
`Md3NavRail`) com paleta M3 baseline roxa. Esse protótipo **não está versionado no repositório**
(é artefato do Claude Design, não arquivo em `SignallQ Admin/` nem em `.claude/design-specs/`) e
**nunca foi adotado** como fonte de cor do Console.

A fonte de cor real e já implementada do Console é `SignallQ Admin/src/index.css` — tokens
`--primary` (`#6C2BFF`), `--success`/`--attention`/`--error`/`--info`, superfícies `--bg-base`/
`--bg-surface`/`--bg-content`, texto `--text-primary`/`--secondary`/`--tertiary`, mais os aliases
legados `--sq-*`. Essa paleta é a que está de fato renderizando em produção (`AppLayout.tsx`,
`Sidebar.tsx`, `Topbar.tsx`, `OverviewPage.tsx`, `LoginPage.tsx` etc. — todos referenciam essas
CSS vars, não hex do protótipo).

`SignallQ Admin/DESIGN.md` formaliza esse mesmo sistema (North Star "The Operator's Console") e é
consistente com `index.css`, não com o protótipo `md3-tobe`.

## Decisão

`SignallQ Admin/src/index.css` é a **fonte de verdade de cor** do Console. O protótipo
`signallq-admin-md3-tobe` fica com uso restrito a **referência de layout, estrutura e hierarquia
de navegação** (ex.: `Md3NavRail` como demonstração de padrão de nav colapsada, composição de
grid do dashboard) — nunca como fonte de hex, tokens semânticos ou paleta.

Qualquer divergência de cor entre o protótipo e o Console real (ex.: roxo M3 baseline do
protótipo vs. `--primary: #6C2BFF` real) **não é bug** — é o protótipo estar desatualizado/nunca
adotado, não o código.

## Por que não just adota a paleta do protótipo

- `index.css` já está em produção, testado, com contraste AA verificado (ver comentário GH#552 no
  próprio arquivo) e com fallback de compatibilidade (Safari < 16.2, `color-mix()` com fallback
  `rgba()`).
- Trocar a paleta agora seria mudança de marca/visual ampla do Console sem gatilho de produto —
  fora do escopo de uma auditoria de conformidade.
- O protótipo em si é inconsistente como fonte de cor: não foi atualizado quando `index.css`
  evoluiu (paleta de status verde/âmbar/vermelho real, GH#552/#746), então usá-lo geraria
  regressão de contraste, não melhoria.

## O que isso não decide

- Não invalida usar o protótipo para achar gaps de **estrutura/navegação** (é exatamente o uso
  dado na auditoria das telas 00/01, ver `AUDITORIA_CONSOLE_TELAS_00_01_2026-07-16.md`).
- Não é decisão sobre o gêmeo digital Android (`packages/design-system/`) nem sobre
  `.claude/skills/SignallQ-design/` — sistemas e paletas diferentes, ver
  `docs_ai/design-system/DECISAO_ALINHAMENTO_TOBE_2026-07-13.md`.

## Achados registrados nesta passagem (não corrigidos por regra de escopo — Lia não edita código do Console)

- `SignallQ Admin/src/components/layout/AppLayout.tsx:88-89` — banner de staging usa Tailwind
  hardcoded (`bg-amber-500/10 border-amber-500/20 text-amber-400`) em vez do token `--attention`
  de `index.css`. Pequeno, Camilo resolve quando tocar nesse arquivo — não é issue formal agora.
- `SignallQ Admin/src/components/layout/Sidebar.tsx:249` — único ponto do Console que usa
  `material-symbols-outlined`; todo o resto (Topbar, Sidebar, MetricCard, AlertList, FilterBar
  etc.) usa `lucide-react`. `DESIGN.md:224` permite os dois sistemas juntos em tese
  ("Lucide/Material são o único sistema de ícone"), mas na prática o Console é 100% Lucide com
  essa exceção isolada — inconsistência real, não decisão deliberada. Registrado para o Camilo
  avaliar trocar por ícone Lucide equivalente (`Sun`/`Moon`) quando tocar no toggle de tema.

---

## Correção 2026-07-16 — decisão revogada por instrução direta do Luiz

**A conclusão acima está invertida. `signallq-admin-md3-tobe` é a fonte de verdade — `index.css`
é a implementação que precisa se alinhar a ele.**

### O que mudou

O Luiz interveio diretamente na conversa: "A fonte da verdade do SignallQ Admin é a que está na
espec" — referindo-se ao protótipo `signallq-admin-md3-tobe`. A Claudete investigou o histórico
antes de aceitar a decisão original acima e encontrou um documento arquivado do Felipe alegando
aprovação do Luiz para a paleta preta — escrito um dia antes de o Felipe ser demitido justamente
por um padrão de alegar validação sem confirmar (ver `.claude/agents/_archive/felipe_2026-07-09_demitido.md`).
Diante disso, a instrução direta e fresca do Luiz na conversa vale mais que qualquer documento ou
comentário de código histórico — inclusive mais que o raciocínio que produziu a decisão original
acima, que era internamente coerente com a evidência disponível na hora (código em produção,
`DESIGN.md` consistente com esse código), mas partia da premissa errada de que "já implementado e
testado" supera a espec quando o dono do produto diz o contrário.

### Por que a decisão original não foi simplesmente "errada"

O raciocínio de 2026-07-16 (acima) não foi descuidado — comparou o protótipo contra código real,
verificou `DESIGN.md`, buscou contraste AA documentado. O erro foi de **precedência de fonte**, não
de investigação: tratou "está implementado e testado" como critério suficiente para vencer a espec,
sem checar antes se havia decisão de produto explícita do Luiz sobre isso. Não havia — só um
registro do Felipe (agente já desligado por esse exato padrão) alegando aprovação verbal nunca
confirmada.

### Decisão corrigida

- `signallq-admin-md3-tobe` (paleta M3 baseline roxa, componentes `Md3Screen00Login`,
  `Md3Screen01Overview`, `Md3DashboardContent`/`Md3DashboardContentMobile`, `Md3NavRail`,
  `Md3BottomNav`) **é a fonte de verdade de cor, tokens e componentes de navegação** do Console.
- `SignallQ Admin/src/index.css` (paleta preta/flat atual) e `SignallQ Admin/DESIGN.md` (que a
  formaliza) **são a implementação a corrigir**, não a referência.
- As divergências antes registradas como "protótipo desatualizado, não é bug" (seção anterior deste
  documento) passam a ser **bugs reais**, listados em
  `docs_ai/design-system/FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md` (validação refeita) e
  consolidados no resumo final para o Camilo.
- `SignallQ Admin/DESIGN.md` precisa ser realinhado ao `md3-tobe` — não corrigido diretamente por
  mim (escopo maior, mesmo padrão do #1010 no app Android). Ver issue aberta pela Claudete/Lia
  cobrando esse realinhamento.

### O que isso não muda

- Não afeta o design system Android (`.claude/skills/SignallQ-design/`) nem
  `DECISAO_ALINHAMENTO_TOBE_2026-07-13.md` — sistemas e produtos diferentes.
- Não autoriza reescrever `index.css`/`DESIGN.md` agora — Lia não edita código nem esse `DESIGN.md`
  (escopo do Camilo); o trabalho desta correção é reclassificação de achados + issue, não
  implementação.
