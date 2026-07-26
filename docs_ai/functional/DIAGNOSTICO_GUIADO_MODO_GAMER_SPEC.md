# Especificação funcional — Diagnóstico guiado por objetivo e modo gamer por jogo/device

- **Status:** rascunho — pronto para virar critério de aceite de tasks do Camilo, pendente de
  confirmação dos pontos assinalados na seção 12
- **Última validação:** 2026-07-26
- **Fonte de verdade:** este arquivo — spec pontual de fluxo pós-teste, complementar a
  `docs_ai/FUNCIONAL.md` RF-01 (Velocidade) e RF-02 (Diagnóstico assistido por IA), que descrevem o
  estado **atual** de `ResultadoVelocidadeScreen`. Não duplicado lá.
- **Escopo:** fluxo de produto exibido após qualquer teste de velocidade — resumo simples,
  diagnóstico guiado por objetivo, modo gamer, detalhes técnicos. Não cobre o motor de medição em
  si (Speedtest) nem o worker de diagnóstico remoto (`signallq-diagnostic-worker`), só a camada de
  apresentação/orquestração pós-teste.
- **Responsável:** Marina (spec, issue #550), Camilo (implementação Kotlin/Compose — a definir em
  tasks futuras)

> Registrado em 2026-07-26 (Marina), a partir da issue #550 (migrada de SIG-303). Segue o template
> de **Especificação Funcional** (`.claude/rules/higiene-e-padronizacao-repositorio.md`, seção 10).

---

## 1. Objetivo

Substituir o card único e denso de `ResultadoVelocidadeScreen` (que hoje despeja Grade A-D,
6 métricas, 3 vereditos de uso, entrada de IA por texto livre, recomendação e contato com
operadora na mesma tela) por um fluxo em camadas: **resumo simples** primeiro, e o usuário decide
se quer ir mais fundo — por objetivo (diagnóstico guiado), por jogo/device (modo gamer) ou por
dado bruto (detalhes técnicos).

O motor local determinístico (`core/diagnostico`, `core/network`) continua sendo a única fonte de
decisão de status. A IA (`ai-diagnosis-worker`) explica em linguagem natural o que o motor já
decidiu — nunca decide ela mesma.

## 2. Contexto e problema

**Estado atual real** (`docs_ai/FUNCIONAL.md` RF-01/RF-02, confirmado no código):
`ResultadoVelocidadeScreen` mostra, na mesma tela, sem hierarquia de profundidade: Grade A/B/C/D/?,
6 cards de métrica, seção "Experiência de uso" (3 vereditos Streaming/Gaming/Vídeo Chamada via
`VereditoUso`, calculado por `SpeedtestQualityClassifier` em `:featureSpeedtest`), chip de
contaminação, comparação ANATEL, DNS info, detalhes avançados expansíveis, `AnalisadorEntryRow`
(entrada de IA por **texto livre**, não fechado), `RecommendationCard`, `OperadoraContactCard`. É
o "concentra muitos dados técnicos após o teste" citado na issue.

**Motores já existentes e não utilizados na UI hoje** (achado desta spec, confirmado por busca no
código — nenhum consumidor em `android/app/src/main/kotlin`):

- `UsageProfileClassifier` (`core/diagnostico`) — 5 perfis de uso (Navegação/Streaming/Jogos/
  Videochamada/Trabalho), status `OK/Instavel/Comprometido`, motivo + evidências + ação
  recomendada, tudo determinístico a partir de `DiagnosticInput`. Sucessor documentado do
  `SpeedtestQualityClassifier`/`VereditoUso` hoje em uso — mais granular (5 perfis vs. 3), mesmo
  princípio de pior-faixa-vence.
- `GameReadinessClassifier` (`core/diagnostico`) — prontidão por categoria de jogo
  (`FPS_COMPETITIVO`/`CLOUD_GAMING`/`MOBILE_COMPETITIVO`), status `Bom/Atencao/Ruim`, evidência de
  NAT como dado adicional (nunca rebaixa sozinho).
- `PerfilThresholds.kt` (`:app`, pacote `jogos`) + catálogo de 16 jogos (`GameCatalog.kt`, citado
  em `JOGOS_TESTE_CONEXAO_SPEC.md`) — já implementado e em produção (`JogosScreen`, issue #935),
  cobre seleção jogo→plataforma (PC/PS5/Xbox Series) com 4 perfis de sensibilidade.

Esta spec **não inventa motor novo** — orquestra os três acima na apresentação pós-teste, conforme
regra explícita da issue #550.

## 3. Personas e casos de uso

- **Usuário apressado** — só quer saber "minha internet está boa?" sem entender métrica nenhuma.
  Atendido pelo resumo simples.
- **Usuário com problema específico** — "meu Zoom trava", "meu jogo está com lag" — não sabe o
  nome técnico da causa, mas sabe descrever o sintoma. Atendido pelo diagnóstico guiado.
- **Jogador** — quer saber se a conexão serve para o jogo/device específico dele, quer salvar essa
  preferência para consultar de novo sem reconfigurar. Atendido pelo modo gamer.
- **Usuário técnico/avançado** — quer os números crus (latência, jitter, perda, RSSI). Atendido
  pelos detalhes técnicos (o card atual de `ResultadoVelocidadeScreen`, reaproveitado como estava).

## 4. Histórias de usuário

- Como usuário, quero um resumo curto logo após o teste, para entender o estado geral sem precisar
  interpretar números.
- Como usuário com um problema específico, quero escolher entre opções fechadas ("meus vídeos
  travam", "minhas chamadas congelam"), para chegar num diagnóstico sem precisar descrever em
  texto livre.
- Como jogador, quero escolher meu jogo e meu device, para saber se a conexão atual serve para
  aquela combinação específica, com recomendação prática se não servir.
- Como jogador recorrente, quero salvar jogo+device como padrão, para não reconfigurar toda vez que
  abrir o app.
- Como usuário, quero ter certeza de que a IA está só explicando o que o motor mediu, nunca
  inventando causa ou me empurrando compra sem eu ter reclamado do mesmo problema mais de uma vez.

## 5. Fluxo principal

### Tela 1 — Resumo simples (substitui o topo de `ResultadoVelocidadeScreen`)

Aparece imediatamente após o teste concluir (mesmo gatilho atual). Mostra **no máximo 5 blocos**,
um por dimensão, cada um com selo de severidade (vocabulário canônico do design system:
excelente/bom/regular/ruim/crítico/inconclusivo — `.claude/CLAUDE.md`, seção Design System):

| Bloco | Fonte do dado |
|---|---|
| Velocidade | `downloadMbps`/`uploadMbps` do resultado do speedtest (já existe) |
| Latência | `latencyMs` (já existe) |
| Wi-Fi | `WifiSignalQualityEngine` (já existe, `core/diagnostico`) |
| DNS | `DnsDiagnosticEngine` (já existe) |
| Estabilidade | jitter + perda de pacotes (`InternetDiagnosticEngine`, já existe) |

Sem grade A-D, sem detalhes avançados, sem `AnalisadorEntryRow` nesta tela — tudo isso migra para
"Ver detalhes técnicos" (tela 4).

**CTAs (3 botões, mesma hierarquia visual, nenhum é padrão pré-selecionado):**
1. "Iniciar diagnóstico guiado"
2. "Modo gamer"
3. "Ver detalhes técnicos"

**Estados da tela 1:** carregando (skeleton dos 5 blocos), sucesso (5 blocos preenchidos), parcial
(bloco individual mostra "inconclusivo" quando o dado daquela dimensão está ausente — nunca
esconde o bloco inteiro), erro (motor local falhou — fallback: mostra só os blocos com dado
disponível, nunca tela em branco).

### Tela 2 — Diagnóstico guiado (novo)

**Etapa 1 — Objetivo:** lista de 7 objetivos fechados (cards com ícone + label, seleção única, ver
árvore completa na seção 6):
1. Minha internet cai ou oscila
2. Meus vídeos travam
3. Meus jogos têm lag
4. Minhas chamadas congelam
5. Meus sites demoram para carregar
6. Minha velocidade não chega ao contratado
7. Não sei se o problema é Wi-Fi ou operadora

**Etapa 2 — Perguntas fechadas:** 2-4 perguntas de múltipla escolha específicas do objetivo
escolhido (ver seção 6 para o conjunto exato por objetivo). Nenhuma pergunta é campo de texto
livre — todas têm opções pré-definidas. Perguntas servem só para **refinar contexto** (ex.: "isso
acontece sempre ou só em horário de pico?") — nunca para o usuário informar a causa, que é sempre
decidida pelo motor.

**Etapa 3 — Resultado do objetivo:** card com status (do vocabulário canônico), motivo em
linguagem simples (gerado pelo motor local — `UsageProfileClassifier` ou engine específico
mapeado, ver seção 6), evidências (2-3 métricas que embasam o motivo), ação recomendada quando
aplicável. Texto da IA, quando disponível, aparece **abaixo** do resultado do motor, claramente
identificado como explicação adicional (ex.: rótulo "Explicação" separado visualmente do bloco de
status) — nunca substitui nem precede o veredito do motor.

Botões: "Escolher outro objetivo", "Ver detalhes técnicos", "Voltar ao resumo".

### Tela 3 — Modo gamer (novo, ver fluxo detalhado na seção 7)

### Tela 4 — Detalhes técnicos

Reaproveita o card atual de `ResultadoVelocidadeScreen` como está hoje (Grade A-D, 6 métricas,
comparação ANATEL, DNS info, detalhes avançados) — **não é reescrito por esta spec**, só deixa de
ser a primeira coisa que o usuário vê. `AnalisadorEntryRow` (entrada de IA por texto livre) e
`RecommendationCard` continuam existindo aqui, sem mudança de comportamento — ver ponto de
confirmação 12.3 sobre o que muda ou não nessa camada.

## 6. Árvore de objetivos fechados

Cada objetivo mapeia para o motor/engine determinístico que já calcula aquela dimensão — nenhum
objetivo cria classificação nova.

| # | Objetivo | Perguntas fechadas (opções) | Métrica priorizada | Motor/engine |
|---|---|---|---|---|
| 1 | Internet cai ou oscila | "Isso acontece: sempre / só às vezes / só em horário de pico?" · "Acontece mais no Wi-Fi ou também com cabo?" | Perda de pacotes, histórico de degradação | `HistoricalDegradationEngine` + `InternetDiagnosticEngine` (perda) |
| 2 | Vídeos travam | "Qual serviço: Streaming (Netflix/YouTube) ou chamada de vídeo?" · "Em qual qualidade costuma travar: SD / HD / 4K?" | Download, bufferbloat, jitter | `UsageProfileClassifier.STREAMING` (se streaming) ou `.VIDEOCHAMADA` (se chamada) |
| 3 | Jogos têm lag | "Isso acontece num jogo específico ou em qualquer jogo?" — se específico, direciona para modo gamer (tela 3); se genérico, segue aqui | Latência, jitter, perda | `UsageProfileClassifier.JOGOS` (genérico) |
| 4 | Chamadas congelam | "Qual serviço: Zoom/Meet/Teams ou WhatsApp/Discord?" (contexto, não muda cálculo) · "Só sua imagem trava ou a do outro lado também?" | Upload, jitter, perda | `UsageProfileClassifier.VIDEOCHAMADA` |
| 5 | Sites demoram | "Sites em geral ou um site específico?" · "Só na primeira vez que abre ou sempre?" | DNS, latência | `UsageProfileClassifier.NAVEGACAO` (foco DNS) |
| 6 | Velocidade não chega ao contratado | "Testou por Wi-Fi ou cabo?" · "Sabe o valor contratado?" (se souber, habilita comparação ANATEL) | Download/upload vs. plano contratado | Comparação ANATEL já existente (RF-01) + `downloadMbps`/`uploadMbps` |
| 7 | Wi-Fi vs. operadora | "O problema muda quando você desliga o Wi-Fi e usa dados móveis?" | RSSI, banda Wi-Fi vs. métricas de rede móvel | `WifiSignalQualityEngine` vs. `MobileSignalDiagnosticEngine` (comparação lado a lado) |

Objetivo 3 é o único que se ramifica para outro fluxo (modo gamer) dentro do próprio diagnóstico
guiado — decisão deliberada para não duplicar a lógica de seleção jogo/device em dois lugares.

## 7. Fluxo do modo gamer

**Etapa 1 — Jogo:** lista com busca, reaproveita o catálogo já existente e implementado
(`GameCatalog.kt`, 16 jogos, `JOGOS_TESTE_CONEXAO_SPEC.md`) como base — ver ponto de confirmação
12.1 sobre jogos fora do catálogo atual.

**Etapa 2 — Device:** PS5, PS4, Xbox, PC, Android, iPhone, Switch, TV/cloud (streaming de jogo via
Smart TV ou box, ex. Xbox Cloud Gaming/GeForce NOW na TV). Filtra ou ajusta a classificação
conforme a tabela de mapeamento abaixo.

**Etapa 3 — Uso único ou salvar como padrão:** toggle simples. Uso único não persiste; salvar como
padrão grava jogo+device em preferência local (DataStore, mesmo padrão de outras preferências do
app — `linkaPreferencias`) e, em testes futuros, oferece atalho "Testar para {jogo} no {device}"
direto da tela de resumo, sem repassar pelas etapas 1-2. Nenhuma sincronização remota — dado fica
só no device.

**Etapa 4 — Resultado por jogo/device:**

| Device | Categoria/motor aplicado | Observação |
|---|---|---|
| PS5, PS4, Xbox, PC | Se o jogo estiver no catálogo `GameCatalog` com perfil competitivo (Valorant, CS2, Rocket League, R6 Siege, LoL → `PerfilThresholds.COMPETITIVO_EXTREMO`), usa `GameReadinessClassifier.FPS_COMPETITIVO` quando aplicável (FPS competitivo especificamente: COD/Warzone, Valorant, CS2, Apex, R6) | Jogo fora dessas 5 categorias específicas cai no perfil de sensibilidade genérico já calculado por `PerfilThresholds` (4 perfis, fluxo já implementado em `JogosScreen`) |
| Android, iPhone | `GameReadinessClassifier.MOBILE_COMPETITIVO` quando o jogo é um dos mobile-competitivos nativos (COD Mobile, Free Fire, PUBG Mobile, Wild Rift) | Jogo mobile fora dessa lista cai em fallback (`UsageProfileClassifier.JOGOS`, genérico) |
| TV/cloud | `GameReadinessClassifier.CLOUD_GAMING` sempre (Xbox Cloud Gaming, GeForce NOW, PS Remote Play, Steam Link — é a definição da categoria) | — |
| Switch | Sem categoria específica em `GameReadinessClassifier` nem no catálogo `GameCatalog` hoje | Fallback: `UsageProfileClassifier.JOGOS` genérico — cumpre a AC "jogo específico pode cair em categoria fallback" da issue #550 |

Resultado exibido: status (Bom/Atenção/Ruim do `GameReadinessClassifier`, ou o vocabulário
canônico equivalente quando cai no fallback genérico), evidências (métricas específicas da
categoria, incluindo NAT como evidência adicional nunca-decisiva quando disponível), recomendação
condicional (ex. "priorize 5GHz perto do roteador"), e explicitação textual de que é estimativa —
mesmo aviso padrão já usado em `JOGOS_TESTE_CONEXAO_SPEC.md` RF-08 ("resultado dentro da partida
pode variar").

Botões: "Salvar como padrão" / "Usar uma vez e continuar", "Trocar jogo", "Trocar device".

## 8. Guardrails de IA

Aplicam-se a qualquer explicação de IA nas telas 2 e 3 (diagnóstico guiado e modo gamer), além do
que já vale hoje para `AnalisadorEntryRow`/laudo automático (RF-02, `AI_FLOW.md`).

**A IA PODE:**
- Explicar em linguagem natural o motivo/evidências que o motor local já calculou.
- Organizar/resumir várias evidências numa frase coesa.
- Fazer perguntas de esclarecimento **dentro do conjunto fechado já definido** na árvore de
  objetivos (seção 6) — nunca perguntas abertas novas geradas por ela.
- Sugerir a próxima ação **entre as ações condicionais já cadastradas** no motor (ex.:
  `acaoRecomendadaPara` de `UsageProfileClassifier`, `recomendacao` de `GameReadinessClassifier`).

**A IA NÃO PODE:**
- Alterar o status técnico decidido pelo motor local (Bom/Atenção/Ruim, OK/Instável/Comprometido,
  excelente/bom/regular/ruim/crítico) — o status exibido é sempre o do motor, nunca reescrito ou
  "amaciado"/"piorado" pela camada de explicação.
- Inventar causa sem evidência mensurada — toda causa citada precisa corresponder a uma métrica
  real presente no `DiagnosticInput` (mesmo princípio de `Provenance` já aplicado no motor: dado
  estimado é rotulado como estimado, dado ausente nunca vira afirmação).
- Sugerir compra (equipamento, plano, upgrade) sem recorrência comprovada — critério objetivo:
  mesmo objetivo/sintoma relatado ou mesma métrica em faixa Ruim/Comprometido em pelo menos 2
  execuções (usar `HistoricalDegradationEngine`/médias 7d-30d já existentes como fonte de
  recorrência, não estado de sessão único).
- Responder livre fora do conjunto de objetivos/perguntas fechadas — não há campo de texto livre
  nas telas 2 e 3 desta spec (diferente de `AnalisadorEntryRow`, que continua existindo só na tela
  4, detalhes técnicos, sem mudança de comportamento).

Regra-síntese, citada literalmente da issue #550: **"Motor local mede, classifica e decide. IA
explica, organiza e conversa de forma guiada."**

## 9. Requisitos não funcionais

- Resumo simples nunca mostra número cru sem selo de severidade ao lado (nunca "47ms" sozinho,
  sempre "47ms · bom").
- Nenhuma das 3 telas de entrada (resumo, diagnóstico guiado, modo gamer) pode conter texto livre
  de usuário — só seleção fechada.
- Card de resultado do diagnóstico guiado e do modo gamer nunca omite jitter/perda "em detalhes
  técnicos" — evidências relevantes aparecem no próprio card, resumidas.
- Ação recomendada nunca promete ausência de problema ("sem lag garantido") — sempre linguagem
  condicional ("deve reduzir", "pode melhorar").
- Modo gamer nunca reexecuta teste automaticamente ao trocar jogo/device — exige ação explícita
  (mesmo princípio de RF-09 do `JOGOS_TESTE_CONEXAO_SPEC.md`).

## 10. Critérios de aceite

- [ ] Resumo simples com os 5 blocos (Velocidade/Latência/Wi-Fi/DNS/Estabilidade), sem grade A-D
      nem detalhes avançados nessa camada.
- [ ] 3 CTAs no resumo: "Iniciar diagnóstico guiado", "Modo gamer", "Ver detalhes técnicos".
- [ ] 7 objetivos fechados navegáveis, cada um com 2-4 perguntas fechadas (nenhum campo de texto
      livre) e mapeamento explícito para motor/engine determinístico existente.
- [ ] Objetivo "jogos têm lag" (específico) direciona para o modo gamer sem duplicar lógica de
      seleção.
- [ ] Modo gamer: seleção jogo → device (8 opções: PS5/PS4/Xbox/PC/Android/iPhone/Switch/TV-cloud)
      → uso único ou salvar como padrão → resultado com status/evidências/recomendação.
- [ ] Jogo sem categoria específica em `GameReadinessClassifier` cai no fallback documentado
      (`PerfilThresholds` genérico ou `UsageProfileClassifier.JOGOS`), nunca em erro/tela vazia.
- [ ] IA nunca altera status técnico do motor nas telas 2 e 3.
- [ ] Nenhuma recomendação de compra sem recorrência (≥2 execuções) comprovada por
      `HistoricalDegradationEngine`.
- [ ] Detalhes técnicos preserva o comportamento atual de `ResultadoVelocidadeScreen` sem
      regressão.

## 11. Fora de escopo

- Reescrita do card de detalhes técnicos (`ResultadoVelocidadeScreen` como está hoje) — só deixa de
  ser a tela padrão pós-teste, conteúdo interno não muda nesta spec.
- Novo motor de classificação — toda decisão vem de `UsageProfileClassifier`,
  `GameReadinessClassifier` ou `PerfilThresholds`/`GameCatalog` já existentes.
- Chat livre completo (mantido só como está hoje em `AnalisadorEntryRow`, na tela de detalhes
  técnicos).
- Loja/monetização patrocinada, ranking comercial de roteadores (SIG-291 vem depois, conforme a
  issue original).
- Lista infinita de jogos — catálogo continua o já existente (16 jogos) até nova decisão de
  ampliação.
- Integração real com servidores dos jogos, medição oficial de ping por servidor.
- Categorias adicionais de `GameReadinessClassifier` além das 3 já implementadas (MOBA, luta,
  corrida, RPG online, battle royale não-FPS) — item já registrado como escopo futuro no próprio
  kdoc da classe.
- Persistência remota da preferência "jogo/device padrão" — fica local (DataStore) nesta spec.

## 12. Pontos a confirmar antes de virar task (não decididos silenciosamente)

**12.1 — Cobertura de device vs. catálogo de jogos atual.** O catálogo `GameCatalog` (16 jogos) foi
construído para plataformas PC/PS5/Xbox Series (`JOGOS_TESTE_CONEXAO_SPEC.md` RF-01). A issue #550
pede 8 devices, incluindo PS4, Android, iPhone, Switch e TV/cloud — nenhum jogo do catálogo atual
declara essas plataformas explicitamente. Proposta desta spec: manter o catálogo como filtro de
jogo quando o device for PC/PS5/Xbox, e usar as categorias genéricas de `GameReadinessClassifier`
(mobile/cloud) ou o fallback `UsageProfileClassifier.JOGOS` para os demais devices, sem exigir
expansão do catálogo agora. **Decidido (Claudete, 2026-07-26): fallback via categoria genérica é aceitável para o MVP; expansão do catálogo fica para iteração futura, fora de escopo desta entrega.**

**12.2 — Objetivo 3 ("jogos têm lag") desviar para o modo gamer.** Proposta: só desvia quando o
usuário responde que o lag é num jogo específico; resposta "qualquer jogo" segue no diagnóstico
guiado genérico (`UsageProfileClassifier.JOGOS`). **Decidido (Claudete, 2026-07-26): desvio só quando jogo específico é identificado; os dois fluxos (diagnóstico guiado genérico vs. modo gamer) continuam separados.**

**12.3 — O que muda em `AnalisadorEntryRow` (entrada de IA por texto livre) na tela de detalhes
técnicos.** Esta spec assume que ela continua existindo sem alteração (só deixou de ser a primeira
coisa vista). Como o "Fora de escopo" da issue #550 cita "chat livre completo" como não coberto —
não fica claro se isso significa "não mexer" ou "não criar um novo". **Decidido (Claudete, 2026-07-26): `AnalisadorEntryRow` fica intocada na camada de detalhes técnicos por enquanto; issue #550 não pede mudança ali, fora de escopo nesta entrega.**

## 13. Métricas de sucesso

`[a confirmar]` — não encontrada meta formal de produto (ex.: % de usuários que usam diagnóstico
guiado vs. vão direto a detalhes técnicos, taxa de conclusão do fluxo de objetivo) em código ou doc
ativa nesta revisão. Mesma lacuna já registrada em `JOGOS_TESTE_CONEXAO_SPEC.md` seção 11.
