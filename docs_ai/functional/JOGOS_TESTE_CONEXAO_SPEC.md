# Especificação funcional — Teste de conexão para jogos online

> Registrado em 2026-07-14 (Claudete), a partir de especificação completa do Luiz. Implementa a
> Fase 6 (Jogos) do plano MD3 To-Be (`docs_ai/technical/TOBE_MD3_APP_PLANO_IMPLEMENTACAO.md`,
> issue #935), substituindo a versão "lista curada estática" originalmente prevista por um fluxo
> completo de teste direcionado por jogo.

## Objetivo

Permitir que o usuário selecione sua plataforma e um jogo específico para avaliar se a conexão
atual apresenta condições adequadas para jogar. O sistema não testa todos os jogos simultaneamente
— só o jogo selecionado.

A escolha do jogo determina: perfil de sensibilidade, região de teste, estratégia de endpoint,
critérios de avaliação, textos do resultado e recomendações específicas.

## Regra principal

1. Medição da conexão atual.
2. Medição direcionada ao perfil do jogo selecionado.
3. Avaliação conjunta de latência, jitter, perda de pacotes e estabilidade.

Nunca avaliar o jogo só pela velocidade de download. Nunca pingar site institucional/loja/login do
jogo ou publicadora (ex.: `fortnite.com`, `ea.com`, `activision.com`, `playstation.com`, `xbox.com`)
— esses endereços não representam a infraestrutura usada durante uma partida.

## Fluxo

**Etapa 1 — Plataforma:** PC / PS5 / Xbox Series, seleção única, filtra a lista de jogos.

**Etapa 2 — Jogo:** lista com busca, seleção única. Ao selecionar, mostra nome, plataforma,
categoria de sensibilidade e região estimada (ex. "Fortnite · PlayStation 5 · Teste estimado para
servidores na América do Sul").

**Etapa 3 — Executar:** botão "Testar conexão para {jogo}" (nome dinâmico). Teste nunca inicia
automático ao selecionar o jogo.

**Etapa 4 — Progresso (10-15s):** "Verificando a conexão" → "Medindo o tempo de resposta" →
"Avaliando estabilidade" → "Preparando o resultado". Sem jargão técnico durante o loading.

**Etapa 5 — Resultado:** card único com título de veredito, texto, e métricas (latência, jitter,
perda, estabilidade, região testada, tipo de conexão atual). Botões "Testar novamente" e "Escolher
outro jogo".

## Catálogo inicial

### PC + PS5 + Xbox

| Jogo | Perfil | Estratégia |
|---|---|---|
| Fortnite | Competitivo | Sonda regional América do Sul |
| Call of Duty: Warzone | Competitivo | Sonda regional América do Sul |
| Apex Legends | Competitivo | Sonda regional América do Sul |
| Rocket League | Competitivo extremo | Sonda regional, critérios rigorosos |
| Overwatch | Competitivo | Sonda regional América do Sul |
| Rainbow Six Siege | Competitivo extremo | Sonda regional, critérios rigorosos |
| EA Sports FC | Esporte competitivo | Sonda regional, foco jitter/perda |
| Marvel Rivals | Competitivo | Sonda regional América do Sul |
| PUBG: Battlegrounds | Competitivo | Sonda regional América do Sul |
| Dead by Daylight | Multiplayer moderado | Sonda regional América do Sul |
| THE FINALS | Competitivo | Sonda regional América do Sul |
| Destiny 2 | Cooperativo competitivo | Sonda regional América do Sul |

### Exclusivos/prioritários PC

| Jogo | Perfil | Estratégia |
|---|---|---|
| VALORANT | Competitivo extremo | Rede Riot BR, fallback regional |
| League of Legends | Competitivo extremo | Rede Riot BR, fallback regional |
| Counter-Strike 2 | Competitivo extremo | Steam Datagram Relay quando detectável, fallback regional |
| Dota 2 | Competitivo | Rede Valve ou fallback regional |

## Perfis de sensibilidade (thresholds)

### Competitivo extremo
VALORANT, CS2, Rocket League, Rainbow Six Siege, League of Legends.

| Métrica | Excelente | Boa | Atenção | Ruim |
|---|---:|---:|---:|---:|
| Latência | até 30ms | 31-50ms | 51-80ms | acima 80ms |
| Jitter | até 5ms | 6-10ms | 11-20ms | acima 20ms |
| Perda | 0% | até 0,5% | até 1% | acima 1% |

### Competitivo
Fortnite, Warzone, Apex, Overwatch, Marvel Rivals, PUBG, THE FINALS, Dota 2.

| Métrica | Excelente | Boa | Atenção | Ruim |
|---|---:|---:|---:|---:|
| Latência | até 50ms | 51-80ms | 81-120ms | acima 120ms |
| Jitter | até 10ms | 11-20ms | 21-30ms | acima 30ms |
| Perda | 0% | até 0,5% | até 1% | acima 1% |

### Esporte competitivo
EA Sports FC. Penaliza fortemente jitter/perda/variação mesmo com latência aceitável.

| Métrica | Excelente | Boa | Atenção | Ruim |
|---|---:|---:|---:|---:|
| Latência | até 40ms | 41-70ms | 71-100ms | acima 100ms |
| Jitter | até 5ms | 6-10ms | 11-20ms | acima 20ms |
| Perda | 0% | até 0,5% | até 1% | acima 1% |

### Multiplayer moderado
Dead by Daylight, Destiny 2.

| Métrica | Excelente | Boa | Atenção | Ruim |
|---|---:|---:|---:|---:|
| Latência | até 60ms | 61-100ms | 101-150ms | acima 150ms |
| Jitter | até 10ms | 11-20ms | 21-30ms | acima 30ms |
| Perda | 0% | até 0,5% | até 1% | acima 1% |

## Estratégia de endpoint

Cada jogo tem uma config de teste (estrutura conceitual):

```json
{
  "gameId": "fortnite",
  "name": "Fortnite",
  "profile": "COMPETITIVE",
  "platforms": ["PC", "PS5", "XBOX"],
  "testStrategy": "REGIONAL_ESTIMATE",
  "region": "SOUTH_AMERICA",
  "target": "udp-game-sp.signallq.com",
  "fallbackTarget": "tcp-game-sp.signallq.com",
  "resultLabel": "Estimativa para Fortnite"
}
```

**`PROVIDER_NETWORK`** — alvo estável na rede da própria desenvolvedora/publicadora (VALORANT, LoL,
CS2 quando a rede Valve for descoberta). Resultado: "Latência até a rede da Riot".

**`REGIONAL_ESTIMATE`** — sonda controlada pelo SignallQ na região mais provável do servidor,
quando o jogo não expõe endpoint público estável. Resultado: "Estimativa para {jogo}". Nunca
apresentar como ping real da partida.

**`SESSION_DISCOVERY`** — identificação dinâmica do servidor/relay real durante sessão. **Fora do
MVP**, item futuro.

### Decisão técnica de implementação (2026-07-14, Claudete)

O `target` conceitual `udp-game-sp.signallq.com` (UDP puro) exigiria Cloudflare Spectrum (produto
pago novo, fora da infra atual) — não implementado no MVP para não introduzir custo novo sem
aprovação explícita. `REGIONAL_ESTIMATE` é implementado via probe TCP/HTTPS contra um Worker
Cloudflare leve (infra já existente no projeto, mesmo padrão de `ai-diagnosis-worker`/
`signallq-admin-worker`), preservando a semântica exata do spec (latência/jitter/perda contra sonda
controlada, nunca "ping real"). `PROVIDER_NETWORK` (Riot/Valve) é melhor esforço — quando o servidor
regional não for identificável com confiança, cai em `REGIONAL_ESTIMATE` automaticamente, nunca
inventa dado.

## Dados medidos

Latência mín/média/máx/p95, jitter, perda de pacotes, rajadas de perda, variação na amostra, falhas
de conexão, IPv4/IPv6, banda Wi-Fi atual, intensidade de sinal, impacto de tráfego concorrente
(quando teste de carga disponível). Velocidade de download/upload é contexto, nunca determina a
nota sozinha.

## Avaliação final

Prioridade: 1) perda de pacotes, 2) jitter, 3) estabilidade, 4) latência, 5) qualidade Wi-Fi local,
6) velocidade disponível. Perda de pacotes pesa mais que diferença pequena de latência (ex.: 25ms
com 3% de perda = ruim; 55ms com 0% de perda e jitter baixo = boa).

## Estados de resultado

- **Excelente para jogar** — resposta rápida, baixa variação, nenhuma perda relevante.
- **Boa para jogar** — adequada, pequenas variações não devem prejudicar a maioria das partidas.
- **Pode apresentar atraso** — variações que podem causar atraso nos comandos/travamentos rápidos.
- **Conexão ruim para este jogo** — perda, atraso elevado ou muita variação.

## Recomendações condicionais

- **2,4GHz** → "Use a rede de 5GHz".
- **Sinal fraco** → "Aproxime-se do roteador".
- **Jitter alto** → "Sua conexão está variando".
- **Perda de pacotes** → "Parte dos dados não chegou ao destino".
- **Bufferbloat** → "Outros usos da internet podem afetar a partida".
- **Latência alta + conexão estável** → "O servidor está distante".

## Avisos

Padrão: "Este teste é uma estimativa feita a partir da conexão atual e da região de servidores mais
provável. O resultado dentro da partida pode variar conforme o servidor, a rota, o horário e a
plataforma."

Quando `PROVIDER_NETWORK`: "O teste foi realizado contra a rede do fornecedor, mas o servidor
específico da partida pode ser diferente."

Sobre o dispositivo (teste rodado no celular, avaliando PC/console): "Para uma estimativa mais
próxima, conecte o celular à mesma rede e permaneça próximo do local onde o PC ou console é
utilizado." Quando por cabo: "O desempenho do dispositivo conectado por cabo pode ser melhor que o
resultado medido pelo celular no Wi-Fi."

## Troca de jogo

Resultado anterior vira histórico, não reutiliza classificação. Novo jogo = novo perfil/critérios,
exige novo teste, atualiza texto do botão/nome/região. Nunca reexecuta sem ação do usuário.

## Histórico (opcional no MVP)

Jogo, plataforma, data/hora, região, latência, jitter, perda, classificação, tipo de conexão, banda
Wi-Fi, versão do teste. MVP mostra só o último resultado por jogo.

## Regras de interface

Não: testar todos os jogos ao mesmo tempo; mostrar ranking entre jogos; lista com dezenas de
resultados; usar só velocímetro de ping; chamar estimativa regional de "ping real"; prometer
ausência de lag; usar verde com perda relevante; esconder jitter/perda em detalhes técnicos;
exigir conhecimento técnico pra interpretar.

## Critérios de aceite

- Um jogo selecionável por teste; lista filtrada por plataforma; botão menciona o jogo escolhido;
  cada jogo tem perfil de sensibilidade; resultado considera latência+jitter+perda; sistema
  diferencia endpoint oficial de estimativa regional; interface nunca afirma estimativa como ping
  real; recomendações geradas a partir das métricas detectadas; usuário troca de jogo e roda novo
  teste; fluxo integrado visualmente à tela Jogos já em desenvolvimento (stub criado na Fase 1/4);
  protótipo cobre estados inicial, seleção, carregamento, sucesso, atenção, resultado ruim e erro.

## Status de implementação (2026-07-14, Camilo, issue #935)

Implementado: catálogo dos 16 jogos, 4 perfis de sensibilidade com thresholds exatos, fluxo de 5
etapas (`android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/JogosScreen.kt` +
`android/app/src/main/kotlin/io/veloo/app/kotlin/jogos/`), motor de avaliação (perda > jitter >
latência via worst-metric, thresholds da tabela já cobrem a prioridade de perda), recomendações e
avisos condicionais, e o Worker `game-latency-probe-worker` (deployado em
`signallq-game-latency-probe.giammattey-luiz.workers.dev`) para a estratégia `REGIONAL_ESTIMATE`
(reaproveita `PingExecutor` de `featureSpeedtest`, generalizado para aceitar URL de sonda
configurável).

Limitações conhecidas, documentadas conforme item 5 do escopo da issue:

- **`PROVIDER_NETWORK` não tem detecção real de rede Riot/Valve implementada** — os 4 jogos que
  declaram essa estratégia (VALORANT, LoL, CS2, Dota 2) caem automaticamente em
  `REGIONAL_ESTIMATE` em tempo de execução, com aviso explícito na UI de que a medição não é
  direto na rede do fornecedor. Nunca inventa dado de rede que não mediu.
- **"Estabilidade"** (item 3 da prioridade de avaliação do spec) não tem uma faixa numérica própria
  na tabela de thresholds — tratada aqui via jitter (proxy de variação da amostra) nas
  recomendações condicionais, não como quarta dimensão pontuada separada.
- **Bufferbloat não é medido neste fluxo** (10-15s, só latência/jitter/perda — não inclui
  saturação de banda). A recomendação de bufferbloat existe no motor mas só dispara se um valor for
  explicitamente fornecido no futuro.
- **Histórico não implementado** (era opcional no MVP conforme a spec).
