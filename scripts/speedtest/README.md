# Calibrador isolado de Speedtest — Cloudflare

Ferramenta **fora do app Linka** para descobrir empiricamente qual configuração de medição própria fica entre **5 % e 10 %** da diferença em relação ao Cloudflare Speedtest oficial. Não toca em `source/app/lib/`.

## Por que existe

O app Linka usa exclusivamente o provedor Cloudflare desde 2026-04-26 (SIMET removido). O fix de payload multi-tier resolveu o subdimensionamento em fibra ≥100 Mbps, mas **não há baseline empírico registrado** confirmando convergência ±5 a 10 %. Este calibrador fecha essa lacuna **sem alterar o app**.

## Fluxo

| Fase | O que faz | Tráfego |
|---|---|---|
| **A** | Roda `@cloudflare/speedtest` (mesmo motor do site) e mostra o resultado oficial. | ~1 GB |
| **B** | Varredura física: 9 combinações de payload×streams × N rodadas, salvando samples brutos por intervalo de 250 ms. | ~2 GB com 2 rodadas |
| **C** | **Reagrega** os samples já coletados em pós-processamento — varia método de cálculo (média/p90/p95), warmup (0/15/30 %) e slow-start guard (0/2/4 s) **sem novas chamadas de rede**. | 0 |
| **D** | Gera CSVs prontos para Excel + relatório Markdown narrativo. | 0 |

Volume total esperado: ~2 a 3 GB por execução.

## Pré-requisitos

- Python 3.11+
- Node.js 20+ (npm)
- Conexão estável (idealmente Ethernet, sem outros downloads concorrentes)
- Notebook plugado à energia

## Instalação

```bash
cd scripts/speedtest
npm install
python -m pip install -r requirements.txt
```

## Uso

### Smoke test (~2 min, valida o pipeline)

```bash
python calibradorCloudflareIsolado.py --smoke --auto-confirmar
```

### Execução completa (~50 a 90 min)

```bash
python calibradorCloudflareIsolado.py --rodadas 2
```

A Fase A apresenta a referência Cloudflare e **espera Enter** antes de iniciar a Fase B (passe `--auto-confirmar` para automatizar).

### Flags úteis

| Flag | Efeito |
|---|---|
| `--smoke` | Só 1 combinação física, 1 rodada, grade reduzida. ~2 min. |
| `--debug` | Inclui eventos `sample` e `conexao` no JSONL (volumoso). |
| `--csv-internacional` | CSVs com `,` separador e `.` decimal (pandas/EUA). Default é Excel pt-BR. |
| `--rodadas N` | Rodadas por combinação física (default 2). |
| `--saida PASTA` | Pasta de saída (default `resultados`). |
| `--auto-confirmar` | Pula confirmação manual entre Fase A e B. |

## Saída

Após cada execução, em `resultados/`:

```
medicoesConsolidadas_<timestamp>.csv  ← Excel pt-BR — TABELA COMPLETA
ranking_<timestamp>.csv               ← Excel pt-BR — VENCEDORA + top
refCloudflareOficial_<timestamp>.csv  ← Referência oficial isolada
relatorio_<timestamp>.md              ← Relatório narrativo
log_<timestamp>.jsonl                 ← Log forense estruturado
contexto_<timestamp>.json             ← Ambiente da execução
refCloudflare_<timestamp>.json        ← JSON cru do SDK oficial
samples/                              ← Samples brutos por (combo × rodada × direção)
  DL100_UL25_S4_CV10_r1_download.csv
  DL100_UL25_S4_CV10_r1_upload.csv
  ...
```

### Como ler

1. Abra **`ranking_*.csv`** no Excel — primeira linha = configuração com menor `diffMaxPctMedio`.
2. Filtre `dentroFaixa5a10 = sim` para configurações na faixa-alvo.
3. `medicoesConsolidadas_*.csv` tem cada rodada individual (uma linha por `combo × metodo × warmup × slowStart × rodada`).
4. Em caso de falha, abra `log_*.jsonl` e procure linhas com `"tipo":"erro"` ou `"tipo":"backoff"`.

## Estratégia anti-bloqueio

- **Reagregação no lugar de re-medição:** método/warmup/slow-start são pós-processamento. Cai de ~300 medições para ~18.
- **Pausa de 20 s** entre medições físicas.
- **User-Agent realista** (Chrome desktop pt-BR).
- **Smoke pré-execução** com 10 MB para validar que o IP não está limitado.
- **Backoff exponencial** em 429/403/503: 60 s → 5 min → 30 min. Aborta após 3 falhas seguidas.
- Endpoints públicos `https://speed.cloudflare.com/__down` e `__up` — os mesmos do site oficial.

## O que NÃO está no escopo desta ferramenta

- Alterar qualquer arquivo do app Linka.
- Gerar PRs, commits ou releases.
- Substituir a lógica do `cloudflare_speed_test_service.dart` — a comparação e proposta de mudança virá em **uma rodada separada**, depois que esta ferramenta entregar a "lógica vencedora".

## webapp/ — Calibrador interativo no browser

Ferramenta complementar para iterar parâmetros do algoritmo Linka **no PC via cabo**, sem compilar o app Android.

### Por que existe

O app Linka usa Cronet (Android) com TTFB ~139 ms vs ~56 ms do Chrome desktop, e 2 streams paralelos que criam contenção no WiFi. Para isolar bugs de algoritmo da variável Cronet/rede, o WebApp roda no Chrome desktop (cabo Ethernet) e permite comparar:

- **Modo "Oficial Cloudflare"** — mesma metodologia do SDK (`@cloudflare/speedtest`)
- **Modo "Linka"** — configurável: streams, payload, rounds, método de agregação
- **Pré-sets rápidos:** `Linka Atual` (2 streams × 25 MB, median), `Fix 1` (1 stream × 25 MB), `Fix 2` (1 stream × 100 MB, P90)

### Como rodar

```bash
cd scripts/speedtest/webapp
python -m http.server 8080
```

Abrir `http://localhost:8080` no Chrome (requer HTTP — não funciona via `file://` por CORS).

### Arquivos

| Arquivo | Descrição |
|---|---|
| `index.html` | Aplicação completa (HTML + CSS + JS, arquivo único) |
| `manifest.json` | PWA manifest — permite instalar como app no Chrome |

### Log exportável

Cada execução gera um registro estruturado com todas as medições individuais. Botões no rodapé:
- **Exportar CSV** — Excel pt-BR (BOM UTF-8, separador `;`)
- **Exportar JSON** — objeto completo do run com config, measurements e results

### Pré-requisitos

- Python 3.x (qualquer versão — só para servir o arquivo)
- Chrome (não Firefox — `ReadableStream` no loop é necessário para leitura chunk-by-chunk)
- Conexão Ethernet (para isolar variáveis de WiFi)

---

## Próximos passos (fora do escopo desta rodada)

Após escolher a combinação vencedora a partir do `ranking_*.csv` ou do WebApp:

1. Abrir nova tarefa para comparar parâmetros vencedores vs. `source/app/lib/services/speedtest/`.
2. Atualizar `docs/DocumentacaoTecnicaSistema.md` §6.4.
3. Encerrar definitivamente `docs/PendenciasSanitizacaoCodigo.md` §7.0 com o baseline empírico.

## Comparador Flutter x Kotlin (paridade)

Script: `comparadorParidadeFlutterKotlin.py`

Objetivo: parear medições por modo (`fast`/`complete`) e por proximidade temporal, gerando delta percentual por métrica entre Flutter e app Kotlin nativo.

### Entradas suportadas

- Flutter JSON (`--flutter-json-dir`) no formato `docs/contratos/FormatoLogMedicao.md`
- Flutter SQLite legado (`--flutter-sqlite`) com tabela `medicao`
- Kotlin SQLite Room (`--kotlin-sqlite`) com tabela `medicao` do app nativo

### Exemplo de uso

```bash
python scripts/speedtest/comparadorParidadeFlutterKotlin.py \
  --flutter-json-dir tmp_logs/sessao_v3.3.3+12_20260507_1224 \
  --kotlin-sqlite caminho/para/linkaKotlin.db \
  --modo all \
  --janela-segundos 900
```

### Saída

- CSV detalhado em `scripts/speedtest/resultados/`
- Relatório Markdown com resumo por modo e % de pares dentro de `<=10%`

## Coleta automatizada da base Kotlin (ADB)

Script: `coletarParidadeKotlin.ps1`

Objetivo: exportar `linkaKotlin.db` do app nativo (`run-as`) e disparar o comparador no mesmo passo.

Exemplo:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/speedtest/coletarParidadeKotlin.ps1 `
  -PackageName io.linka.app.kotlin `
  -FlutterJsonDir tmp_logs/sessao_v3.3.3+12_20260507_1224 `
  -Modo all `
  -JanelaSegundos 900
```

Pré-condições:

- `adb` com dispositivo conectado
- app Kotlin instalado em build debuggable (necessário para `run-as`)
