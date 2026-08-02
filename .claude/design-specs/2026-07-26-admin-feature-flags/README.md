# SignallQ Admin — Firebase > Feature Flags (protótipo)

Protótipo da issue [#1479](https://github.com/7ALabs/SignallQ/issues/1479) (Feature 3 do épico
[#1347](https://github.com/7ALabs/SignallQ/issues/1347) — Governar o Consumer por Firebase Remote
Config). Design apenas — a implementação em React real (F3 de código) só acontece depois do backend
(F2, #1478) estar pronto.

```
index.html   protótipo navegável, autocontido (sem build step)
```

## Como abrir

Arquivo estático puro — abrir direto no navegador (`file://` funciona, sem módulo ES nem servidor).

## Por que HTML estático em vez de Claude Design

Marina (esta sessão) não teve acesso à tool DesignSync — limitação conhecida documentada no perfil
do agente: só a sessão principal (Claudete) propaga a sessão da tool. Em vez de aproximar de memória
o protótipo "SignallQ — Protótipos" (Claude Design, fonte viva do Admin) ou assumir estar certo sem
poder comparar, optei por construir o protótipo ancorado nos artefatos reais e verificáveis do
repositório:

- **Tokens de cor/tipografia/espaçamento/radius**: copiados literalmente de
  `SignallQ Admin/src/index.css` (tema dark, padrão do Console).
- **Vocabulário de componente**: catálogo de `SignallQ Admin/DESIGN.md` (SectionIntro, badges de
  estado com dot+texto, regra "nunca cor sozinha", regra de profundidade nível 1/2/3, regra
  "The One Accent Rule").
- **Chrome de navegação**: réplica simplificada do padrão Nav Drawer (300px) documentado em
  `DESIGN.md` seção 7, para não entregar tela solta fora do contexto real do app (ver
  `feedback_prototipo_nao_slideshow`).
- **Catálogo de flags**: chaves e módulos copiados literalmente do corpo de #1347 (seção "Modelo de
  chaves" e "Módulos Android que devem entrar") — nenhuma chave inventada. Criticidade, owner,
  rollout e timestamps são dados de exemplo do protótipo, não valores reais de produção.

Pendência explícita: comparar este protótipo pixel-a-pixel contra o projeto Claude Design
"SignallQ — Protótipos" quando a Claudete (sessão principal, com acesso real à DesignSync) puder
validar — não declarar isto como 1:1 confirmado até essa comparação acontecer.

## Fluxo coberto (navegável, não slideshow)

1. **Lista** — agrupada por módulo (colapsável), busca, filtro "só críticas/kill switch", filtro
   "só com rascunho", banner de rascunho pendente com atalho para comparar/publicar.
2. **Detalhe/edição** (drawer lateral, sem sair da lista) — 3 abas: Editar (toggle, rollout,
   versão mín/máx, segmentação, mensagem de indisponibilidade, motivo obrigatório), Comparar
   (publicado × rascunho campo a campo), Histórico (versões + ação de restaurar).
3. **Publicar** (modal) — resumo de todas as flags alteradas no rascunho, comentário obrigatório,
   reforço extra quando há flag crítica na leva.
4. **Rollback** (modal) — diff da versão-alvo, motivo obrigatório, publica como nova versão.

## Decisão de UX — destaque de kill switch sem alarme falso

Erro comum aqui seria pintar a linha inteira de vermelho ou usar uma borda lateral grossa colorida
("side-tab accent border") — o hook de design (`impeccable`) sinalizou exatamente essa borda como a
marca mais reconhecível de UI gerada por IA, e ela também contraria a regra do próprio `DESIGN.md`
("alerta tratado como informação objetiva, nunca decoração/drama visual", "nunca cor sozinha —
sempre dot + texto + label"). Removi a borda lateral e o destaque de crítica ficou só em:

- ícone `⛔` inline ao lado da chave (não fica isolado, sempre acompanhado de texto);
- badge `Crítica · kill switch` (cor + texto, padrão `StatusBadge` do sistema);
- na aba Editar do drawer, uma caixa "Zona de risco" (não "PERIGO" em caixa alta) — explica o
  efeito, e exige um checkbox de confirmação explícita antes de liberar "Adicionar ao rascunho";
- no modal de publicação, a mesma caixa reaparece só quando a leva inclui pelo menos uma flag
  crítica, com o botão "Publicar agora" desabilitado até o checkbox de confirmação.

O resultado: quem está mexendo numa flag crítica não tem como não perceber (bloqueio funcional real,
não só visual), mas a tela não parece quebrada nem grita — é o mesmo tom sóbrio do resto do Console.

## Achados do hook `impeccable` e como tratei

- **`side-tab` (borda lateral grossa colorida em linha crítica)** — achado real, corrigido: removida,
  substituída por ícone+badge (ver seção acima).
- **`overused-font` / `design-system-font` (Roboto, Roboto Mono)** — falso positivo por sidecar
  desatualizado; `DESIGN.md` documenta explicitamente Roboto (display/body) e Roboto Mono (mono) como
  as fontes oficiais do Console. Não suprimido via config porque não é drift, é o valor documentado.
- **`design-system-radius` 23px (nav item do drawer)** — falso positivo pela mesma causa: `DESIGN.md`
  documenta `nav-item-drawer: 23px` literalmente no frontmatter.
- **`design-system-radius` 10px (logo/brand mark do protótipo)** — achado real e trivial, corrigido
  para `12px` (token `card`, mais próximo do documentado).
- **`design-system-color` (~20 ocorrências, variações alpha de `rgba(125,219,147,*)`,
  `rgba(255,185,85,*)`, `rgba(207,188,255,*)`)** — falso positivo pela mesma causa raiz (sidecar
  desatualizado): são exatamente `--success`/`--attention`/`--primary` documentados em alpha
  10%/20%, o padrão descrito no `DESIGN.md` seção 7 para `StatusBadge` ("fill da cor semântica a 10%
  de alpha, borda a 20% de alpha"). Não roda `ignore-value` individual pra cada uma (arquivo de
  protótipo fora de `src/`, não código de produção) — decisão registrada aqui em vez de 20 comandos
  de supressão.

## Pendências para quando F3 (implementação) começar

- Validar contra o Claude Design real (ver seção acima).
- Confirmar com Camilo/Bruno (F2, backend) os nomes exatos de campo retornados pela API antes de
  fixar os nomes usados aqui (`draftEnabled`, `draftRollout` etc. são nomes de protótipo).
- `.impeccable/design.json` deveria ser regenerado (`/impeccable document`) antes da implementação
  real entrar em `src/` — os falsos positivos acima não devem se repetir em código de produção.
