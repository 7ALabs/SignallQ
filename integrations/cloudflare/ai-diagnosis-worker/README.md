# Linka AI Diagnosis Worker

Endpoint unico para explicar (via IA) um diagnostico de rede local produzido
pelo app Linka. O cliente Kotlin **nunca** chama Cloudflare AI direto — ele
sempre passa por este Worker, que monta o prompt, chama o binding `AI`, parseia
a saida e devolve um JSON em **schema v2**.

## Endpoint

- `POST /api/ai/diagnostico-conexao`

Resposta `200`: JSON em `AiDiagnosisResult` (schema v2).
Erros possiveis:

| Codigo | HTTP | Quando |
|--------|------|--------|
| `not_found`            | 404 | Path diferente do unico permitido |
| `method_not_allowed`   | 405 | Metodo diferente de POST |
| `payload_invalid`      | 400 | JSON invalido ou body > 64 KB |
| `ai_json_parse_failed` | 502 | A IA respondeu, mas o texto nao e JSON parseavel |
| `internal_error`       | 500 | Falha generica |

## Variaveis

- Binding: `AI` (Cloudflare Workers AI)
- `AI_MODEL` (opcional): id do modelo. Default: `@cf/google/gemma-4-26b-a4b-it`
  (Gemma 4 26B, MoE 26B total / 4B ativo, derivado da pesquisa do Gemini 3).
  **Politica:** Llama/Meta nao deve ser configurado aqui — nem como padrao,
  nem como fallback cloud. Em caso de falha do modelo cloud, o cliente Kotlin
  cai no fallback local (sem IA externa).

## Schema de saida (v2)

Campos novos em relacao a v1:

- `schemaVersion`: agora `"2"`. O Worker sobrescreve este campo apos o parse.
- `modeloIa`: bloco com nome comercial do motor (`familia`, `versao`, `tamanho`,
  `nomeExibicao`, `nomeCompletoComercial`, `descricaoComercial`, `textoRodape`).
  O Worker SEMPRE sobrescreve este bloco apos o parse — a IA nao decide qual
  modelo esta sendo usado. O id interno `@cf/...` fica em `idInterno` e NAO
  deve ser exibido ao usuario.
- `classificacaoTecnica`: separa explicitamente `velocidade`, `estabilidade`,
  `wifi`, `dns`, `fibra`. Cada item tem `avaliacao` (`boa|regular|ruim|inconclusiva`)
  e `justificativa`.
- `problemaPrincipal.tipo` aceita: `velocidade|estabilidade|wifi|roteador|isp|dns|fibra|dispositivo|historico|sem_problema|inconclusivo`.
- `hipotesesDescartadas[]`: hipoteses descartadas com `motivo`.
- `acoesRecomendadas[].tipo`: `validacao_local|ajuste_roteador|ajuste_dispositivo|contato_isp|observacao|reteste`.
- `acoesRecomendadas[].executavelNoApp`: indica se a acao pode ser feita pelo
  proprio app (ex.: re-rodar speedtest).
- `perguntasContextuais[]`: perguntas curtas com `opcoes[]` para o usuario
  refinar o diagnostico.

## Mapeamento comercial do modelo

A funcao `getCommercialModelInfo(model)` traduz o id interno em:

| id interno (regex)            | familia | nomeExibicao        | textoRodape                                |
|-------------------------------|---------|---------------------|--------------------------------------------|
| `gemma-4.*26b` (PADRAO)       | Gemma   | Gemma 4 26B         | Motor de análise: Linka IA — Gemma 4 26B   |
| `gemma*` (generico)           | Gemma   | Gemma               | Motor de análise: Linka IA — Gemma         |
| `llama*` (apenas retrocompat) | Llama   | Linka IA            | Motor de análise: Linka IA                 |
| outros / desconhecido         | Outro   | IA                  | Motor de análise: Linka IA                 |

Para todos os casos vindos do Worker, `provedor = "cloudflare_workers_ai"`.
O fallback local do cliente Kotlin (`ModeloIa.localFallback()`) emite
`provedor = "local"`. O parser Kotlin aceita ambos os valores sem normalizacao
extra.

## Sobrescrita defensiva pos-parse

Apos parsear a saida da IA, o Worker forca:

```ts
parsed.schemaVersion = "2";
parsed.source = "cloudflare_ai";
parsed.generatedAt = Date.now();
parsed.modeloIa = modelInfo;
```

Isso evita que a IA invente metadados ou que o cliente confie em campos que a
IA pode ter omitido/divergido.

## Por que NAO usamos `response_format: json_schema` agora

O Workers AI suporta `response_format` em alguns modelos, mas o suporte ainda
e inconsistente entre modelos do binding `[ai]`. Habilitar JSON Mode antes de
validacao especifica para Gemma 4 26B (modelo padrao atual) pode quebrar o
fluxo (resposta vazia, truncada ou nao-JSON). Optamos por:

- Schema hint forte no prompt do usuario.
- `temperature: 0.2`, `max_tokens: 2048`.
- Extracao tolerante (`extractJson`) que lida com markdown fences e texto antes/depois.
- Sobrescrita defensiva pos-parse (`schemaVersion`, `source`, `generatedAt`, `modeloIa`).

Quando houver validacao clara de que JSON Mode nao quebra Gemma/Workers AI,
trocar para `response_format: { type: "json_schema", json_schema: ... }`.

## Regras inegociaveis

- Nao retornar texto solto.
- Nao vazar prompt ou stacktrace.
- Rejeitar qualquer metodo que nao seja `POST`.
- Body limitado a 64 KB.
- O cliente Kotlin recebe sempre `application/json`, mesmo em erro.

## Como testar manualmente

```bash
curl -X POST https://linka-ai-diagnosis-worker.<seu-subdominio>.workers.dev/api/ai/diagnostico-conexao \
  -H "content-type: application/json" \
  -d '{
    "schemaVersion": "2",
    "generatedAtEpochMs": 1700000000000,
    "connectionType": "wifi",
    "decisaoId": "TEST",
    "decisaoStatus": "attention",
    "decisaoTitulo": "Teste",
    "decisaoMensagem": "Teste",
    "metricasAtuais": {
      "downloadMbps": 294,
      "uploadMbps": 411,
      "latenciaMs": 101,
      "jitterMs": 25.1,
      "perdaPacotesPercentual": 0
    },
    "classificacaoLocal": {
      "velocidade": "boa",
      "estabilidade": "ruim",
      "problemaProvavel": "estabilidade",
      "naoEhProblemaDeBanda": true
    },
    "evidencias": [],
    "recomendacoesLocais": [],
    "limitesDaAnalise": []
  }'
```

A resposta deve:

- Identificar o problema como `estabilidade` (nao "internet lenta").
- Trazer `hipotesesDescartadas[]` com "velocidade insuficiente".
- Trazer `modeloIa.textoRodape = "Motor de análise: Linka IA — Gemma 4 26B"`.

## Regras de titulo (PROIBICAO DE "Internet lenta")

A regra 16 do `SYSTEM_PROMPT` proibe titulos genericos quando as metricas
permitem identificar o problema real:

- Download/upload bons + latencia/jitter altos -> titulo deve refletir
  estabilidade (ex.: "Conexao instavel", "Velocidade boa, estabilidade ruim").
- Velocidade abaixo do esperado + latencia normal -> titulo deve apontar
  velocidade (ex.: "Velocidade abaixo do contratado", "Banda insuficiente").
- Perda de pacotes significativa -> titulo deve mencionar perda
  (ex.: "Perda de pacotes detectada").
- `"Internet lenta"` so e aceitavel quando NAO ha dados de
  latencia/jitter/perda E download/upload estao de fato baixos.

A regra 17 reforca que o objeto `modeloIa` e sobrescrito pelo Worker — a IA
nao precisa preencher (e nao deve inventar).

### Exemplo obrigatorio

Para o payload abaixo (download 294 Mbps, upload 411 Mbps, latencia 101 ms,
jitter 25,1 ms) a resposta DEVE conter:

- `classificacaoTecnica.velocidade.avaliacao = "boa"`
- `classificacaoTecnica.estabilidade.avaliacao = "ruim"`
- `problemaPrincipal.tipo = "estabilidade"`
- `resumo`: "A velocidade está boa, mas a conexão está instável."
- `titulo`: algo como "Conexao instavel" ou "Velocidade boa, estabilidade
  ruim". **NUNCA "Internet lenta".**
- `hipotesesDescartadas` inclui
  `{"hipotese": "velocidade insuficiente", "motivo": "download de 294 Mbps e upload de 411 Mbps indicam boa banda"}`.
- `impacto.jogos`: `"Alta latencia"` ou `"Instavel"` (NAO `"OK"`).
- `impacto.streaming`: `"OK"` (download alto sustenta streaming).
- `impacto.videochamada`: `"Comprometida"` ou `"Instavel"`.

Esse exemplo esta replicado no `SYSTEM_PROMPT` (secao `EXEMPLO OBRIGATORIO`)
e e usado como caso de regressao em `AiDiagnosisRepositoryTest` (cenario 1).
