# Casos Reais de Paridade e Regressoes PWA — SignallQ

**Criado em:** 2026-06-24
**Baseado em:** Android v0.21.0, Worker `linka-ai-diagnosis-worker`, `paridade-plataformas.md`
**Mantenedor:** Renan (Dev PWA)

Este arquivo documenta casos concretos de paridade PWA/Android e limitacoes reais de browser.
Cada caso tem: o que o Android faz, o que o PWA pode fazer, o que reportar ao usuario.

---

## Caso 1 — RSSI e scan Wi-Fi (impossivel no browser)

**O que o Android faz:**
`WifiManager.scanResults` retorna lista de redes visiveis com SSID, BSSID, RSSI (dBm), frequencia (MHz), canal.
`WifiInfo.getRssi()` retorna RSSI da rede conectada em tempo real.

**No browser:**
Nenhuma Web API expoe RSSI ou lista de redes Wi-Fi. A API `navigator.wifi` nao existe.
A tentativa via `NetworkInformation.downlink` retorna estimativa grosseira de banda (0.1–10 Mbps max), nao RSSI.

**O que mostrar no PWA:**
- Nao exibir campo de RSSI. Nao simular valores como "-72 dBm".
- Exibir apenas: tipo de conexao (Wi-Fi / celular / offline) via `navigator.onLine` + Network Info API.
- Texto sugerido: "Para ver nivel de sinal e redes visiveis, use o app Android."
- A aba Sinal no PWA deve ter escopo drasticamente reduzido — tipo de conexao e IP publico apenas.

**Regressao conhecida:**
Se qualquer tela do PWA exibir um valor de RSSI sem telo medido, isso e dado inventado. Remover imediatamente.

---

## Caso 2 — Sinal movel (RSRP, RSRQ, SINR, banda — impossivel no browser)

**O que o Android faz:**
`TelephonyManager` + `CellInfoLte` / `CellInfoNr` expoe RSRP (dBm), RSRQ (dB), SINR (dB), banda (ex.: B3, n78), tecnologia (4G/5G), operadora, SIMs ativos.
Thresholds usados pelo worker IA (do SYSTEM_PROMPT do Worker):
- RSRP: > -85 bom, -85 a -100 medio, -100 a -110 ruim, < -110 pessimo.
- SINR: > 10 bom, 0-10 medio, < 0 ruim.
- RSRQ: > -10 bom, -10 a -15 medio, < -15 ruim.

**No browser:**
`navigator.connection.effectiveType` retorna "4g" / "3g" / "2g" / "slow-2g" — estimativa do browser, nao dado da operadora.
Sem acesso a RSRP, RSRQ, SINR, banda ou numero de SIMs.

**O que mostrar no PWA:**
- Exibir `navigator.connection.effectiveType` como indicativo: "Conexao estimada: 4G". Sem numero de dBm.
- Adicionar sempre o aviso: "Esta e uma estimativa do navegador — para informacoes detalhadas de sinal, use o app Android."
- Nao criar secao de "Sinal Movel" no PWA com metricas falsas.

**Regressao conhecida:**
Exibir `effectiveType` como "sinal 4G" sem contexto pode confundir o usuario — ele pode achar que e sinal real da operadora.

---

## Caso 3 — Speedtest no browser (parcialmente possivel, com limitacoes de precisao)

**O que o Android faz:**
Download: fetch paralelo de arquivo grande no worker Cloudflare, mede throughput real com multithreading.
Upload: POST de payload no worker, mede throughput.
Ping: TCP connect para o gateway local (`GatewayLatencyMeasurer.kt`) + fetch HTTP para servidor externo.
Bufferbloat: multiplas requisicoes paralelas durante o teste, mede delta de latencia.

**No browser:**
- Download: `fetch()` de arquivo grande via Cloudflare, medir throughput com `performance.now()`. Funciona.
- Upload: `fetch()` POST com payload. Funciona.
- Ping: `fetch HEAD` para servidor de referencia + `performance.now()`. Funciona, mas e RTT HTTP, nao TCP/ICMP.
- Bufferbloat: multiplas requisicoes paralelas com `Promise.all()`. Possivel, mas sem garantia de thread real no browser (event loop unico).
- Precisao de download/upload: pode ser afetada por cache do browser, CORS preflight, e compressao de resposta.

**Limitacoes tecnicas:**
- Sem controle de protocolo de baixo nivel (TCP window, congestion control).
- O event loop unico do JavaScript pode suavizar picos de jitter — jitter medido no PWA tende a ser menor que o real.
- CORS: o worker Cloudflare precisa ter `Access-Control-Allow-Origin: *` para o PWA poder chamar. Ja configurado no worker existente.
- Service Workers: nao interferem no speedtest se nao houver cache de URLs do teste. Garantir que as URLs de teste nunca sejam cacheadas pelo SW.

**Mitigacoes:**
- Usar arquivos diferentes por requisicao (timestamp no query string) para evitar cache.
- Rodar pelo menos 3 medicoes e usar a mediana.
- Documentar na UI: "Medicao feita via navegador — pode variar em relacao a medidores dedicados."

**Regressao conhecida:**
Bufferbloat medido via browser em single-thread pode dar resultados inconsistentes. Alertar o usuario quando bufferbloat parecer anormal, mas nao usar como diagnostico definitivo sem evidencia adicional.

---

## Caso 4 — Latencia DNS no browser (parcialmente possivel)

**O que o Android faz:**
`DnsDiagnosticEngine.kt` mede latencia de resolucao DNS via `InetAddress.getByName()` com timer.
Thresholds reais do codigo:
- > 300ms: DNS muito lento (critico, id DNS-01)
- > 150ms: DNS lento (atencao, id DNS-02)
- > 50ms: DNS acima do ideal (info, id DNS-03)
- <= 50ms: saudavel (sem alerta)

**No browser:**
`dns.resolve()` nao existe no browser. O unico caminho e medir o RTT total de uma requisicao HTTP e comparar com uma requisicao a IP diretamente — a diferenca e aproximadamente o tempo de resolucao DNS.

Tecnica:
```typescript
// Mede RTT com resolucao DNS
const t0 = performance.now();
await fetch('https://one.one.one.one/dns-query?name=example.com', { cache: 'no-store' });
const rttComDns = performance.now() - t0;

// Mede RTT sem resolucao DNS (IP direto)
const t1 = performance.now();
await fetch('https://1.1.1.1/dns-query?name=example.com', { cache: 'no-store' });
const rttSemDns = performance.now() - t1;

const estimativaDns = rttComDns - rttSemDns;
```

Limitacoes:
- Resultado inclui latencia HTTP + TLS handshake, nao apenas DNS puro.
- `performance.now()` tem resolucao reduzida em alguns browsers por protecao a Spectre (granularidade de 1ms no Firefox; 0.1ms em outros).
- O browser reutiliza conexoes persistentes (keep-alive) — a segunda requisicao ao mesmo host pode nao resolver DNS de novo.
- Comparacao entre servidores DNS (benchmark) nao e viavel de forma confiavel no PWA.

**O que fazer:**
- Nao implementar benchmark de DNS no PWA por ora — resultado nao e confiavel.
- Se implementar algo, mostrar apenas latencia estimada com disclaimer claro.
- Usar os mesmos thresholds do Android quando houver medicao (> 300ms critico, > 150ms atencao).

---

## Caso 5 — Deteccao de tipo de conexao (parcialmente possivel, sem Safari/iOS)

**O que o Android faz:**
`ConnectivityManager` + `NetworkCapabilities` determina se e Wi-Fi, celular, ethernet ou VPN.
`WifiInfo` fornece SSID, BSSID, frequencia.

**No browser:**
`navigator.connection` (Network Information API) — `type` e `effectiveType`.

Suporte real:
- Chrome desktop/Android: `connection.type` retorna "wifi" | "cellular" | "ethernet" | "none" | "other"
- Firefox: API nao existe (`navigator.connection === undefined`)
- Safari / iOS (todos os browsers no iOS): API nao existe

Fallback obrigatorio:
```typescript
const tipoConexao = (() => {
  if (!navigator.onLine) return 'offline';
  const conn = (navigator as any).connection;
  if (!conn) return 'desconhecido'; // Safari, Firefox
  return conn.type ?? conn.effectiveType ?? 'desconhecido';
})();
```

**Regressao conhecida:**
Se o PWA exibir "Wi-Fi" como tipo de conexao sem verificar suporte a `navigator.connection`, usuarios de Safari/iOS verao "Wi-Fi" baseado em dado inventado. Sempre usar o fallback acima.

**O que reportar ao usuario em Safari/iOS:**
"Tipo de conexao: nao detectado (limitacao do navegador Safari)."

---

## Caso 6 — Latencia (ping) no browser — sem ICMP, como fazer corretamente

**O que o Android faz:**
`GatewayLatencyMeasurer.kt`: TCP connect para o gateway local (192.168.x.1) na porta 80/443.
RTT externo: fetch HTTP HEAD para servidor de referencia (ex.: `8.8.8.8`, servidor Cloudflare).

**No browser:**
ICMP (`ping`) e impossivel — raw sockets sao bloqueados.
TCP connect direto tambem e impossivel — nao existe Socket API no browser.

Unica opcao: `fetch` com metodo HEAD ou GET pequeno, medir com `performance.now()`.

Implementacao correta:
```typescript
async function medirLatencia(url: string, tentativas = 5): Promise<number | null> {
  const resultados: number[] = [];
  for (let i = 0; i < tentativas; i++) {
    const t0 = performance.now();
    try {
      await fetch(`${url}?t=${Date.now()}`, {
        method: 'HEAD',
        cache: 'no-store',
        signal: AbortSignal.timeout(3000),
      });
      resultados.push(performance.now() - t0);
    } catch {
      // timeout ou erro de rede — ignorar esta tentativa
    }
  }
  if (resultados.length === 0) return null;
  resultados.sort((a, b) => a - b);
  return resultados[Math.floor(resultados.length / 2)]; // mediana
}
```

Limitacoes declaradas:
- O RTT medido inclui: resolucao DNS + TLS handshake + HTTP processing + transporte.
- Um ICMP ping real mede apenas transporte (camada 3). O fetch mede camadas 3+4+7.
- Resultado tipicamente 10-30ms maior que ICMP para o mesmo servidor.
- O que reportar: "Latencia HTTP (aproximacao de ping)" — nunca chamar de "ping ICMP".

**Paridade de thresholds com Android:**
Android usa > 100ms como latencia alta (`InternetDiagnosticEngine.kt`, id IN-NORMAL-05, ref. Anatel RQUAL).
PWA deve usar o mesmo threshold: > 100ms = atencao. Declarar que e RTT HTTP, nao ICMP.

---

## Caso 7 — Paridade de veredicto: thresholds reais do Android

Ao classificar resultados no PWA, usar obrigatoriamente os mesmos thresholds do Android.
Fontes: `WifiSignalQualityEngine.kt`, `InternetDiagnosticEngine.kt`, `DnsDiagnosticEngine.kt`.

### Download (InternetDiagnosticEngine.kt)
| Threshold | Classificacao | ID Android |
|---|---|---|
| < 25 Mbps | Baixo (atencao) | IN-NORMAL-03 |
| >= 25 Mbps | Saudavel | IN-NORMAL-02 |

### Upload (InternetDiagnosticEngine.kt)
| Threshold | Classificacao | ID Android |
|---|---|---|
| 0 Mbps (exato) | Upload zerado (critico) | IN-NORMAL-04Z |
| > 0 e < 5 Mbps | Upload baixo (atencao) | IN-NORMAL-04 |
| >= 5 Mbps | Saudavel | — |

### Latencia HTTP (InternetDiagnosticEngine.kt)
| Threshold | Classificacao | ID Android | Referencia |
|---|---|---|---|
| > 100ms | Alta (atencao) | IN-NORMAL-05 | Anatel RQUAL |

### Jitter (InternetDiagnosticEngine.kt)
| Threshold | Classificacao | ID Android |
|---|---|---|
| > 20ms | Elevado (atencao) | IN-NORMAL-06 |

### Perda de pacotes (InternetDiagnosticEngine.kt)
| Threshold | Classificacao | ID Android |
|---|---|---|
| >= 1.0% | Moderada (atencao) | IN-NORMAL-07b |
| >= 3.0% | Alta (critico) | IN-NORMAL-07 |

### Bufferbloat (InternetDiagnosticEngine.kt — thresholds DSLReports/waveform)
| Threshold | Classificacao | ID Android |
|---|---|---|
| < 5ms | Nenhum (ok) | — |
| 5-30ms | Leve (nao reportar) | — |
| > 30ms | Elevado (atencao) | IN-NORMAL-09b |
| > 100ms | Critico | IN-NORMAL-09 |

### RSSI Wi-Fi (WifiSignalQualityEngine.kt) — Android only, nao disponivel no browser
| Threshold | Classificacao | ID Android |
|---|---|---|
| > -60 dBm | Excelente | WIFI-01 |
| >= -67 dBm | Bom | WIFI-02 |
| >= -75 dBm | Fraco (atencao) | WIFI-03 |
| < -75 dBm | Muito fraco (critico) | WIFI-04 |

### DNS (DnsDiagnosticEngine.kt)
| Threshold | Classificacao | ID Android |
|---|---|---|
| > 300ms | Muito lento (critico) | DNS-01 |
| > 150ms | Lento (atencao) | DNS-02 |
| > 50ms | Acima do ideal (info) | DNS-03 |
| <= 50ms | Saudavel | — |

**Regra:** qualquer veredito de qualidade no PWA deve ser rastreavel a esses thresholds.
Se um threshold mudar no Android, atualizar este arquivo e o codigo PWA na mesma PR.

---

## Caso 8 — Background monitoring (impossivel no browser de forma continua)

**O que o Android faz:**
`MonitoramentoWorker` (WorkManager) executa a cada 30 minutos em background, mede latencia e conectividade, persiste no Room, dispara notificacao se limite excedido.

**No browser:**
- `Periodic Background Sync API`: disponivel apenas no Chrome/Android (nao Safari, nao Firefox desktop). Frequencia minima nao garantida pelo browser (pode ser horas). Requer `periodicSync` permission e o app estar instalado como PWA.
- `Background Sync API`: executa apenas uma vez quando a conexao volta — nao serve para monitoramento periodico.
- Service Worker com `setInterval`: nao existe. SW e event-driven, nao tem loop persistente.

**Conclusao de plataforma:**
Monitoramento passivo continuo e **impossivel** no browser com comportamento equivalente ao Android.

**O que implementar no PWA:**
1. Monitoramento ativo: executar medicoes enquanto o usuario esta com o app aberto. Usar `setInterval` no contexto da pagina (nao SW).
2. Periodic Background Sync (opcional, Chrome/Android only): tentar registrar sync para medicao ocasional quando o app nao esta aberto. Declarar na UI que funciona apenas no Chrome e pode nao ser executado regularmente.
3. O que comunicar ao usuario: "Monitoramento em segundo plano disponivel apenas no app Android. No navegador, as medicoes ocorrem apenas com o app aberto."

**Regressao conhecida:**
Se o PWA exibir "Monitoramento ativo" quando o usuario fecha o app, e mentira. Nao implementar essa comunicacao falsa.

---

## Caso 9 — Deploy Cloudflare Pages: variaveis de ambiente e checklist

### Variaveis de ambiente obrigatorias no PWA

O PWA SignallQ ainda nao foi inicializado (estado atual: apenas `pwa/README.md` e `pwa/CLAUDE.md`).
Quando a implementacao comecar, estas variaveis serao necessarias:

| Variavel | Onde definir | Descricao |
|---|---|---|
| `VITE_AI_WORKER_URL` | Cloudflare Pages > Settings > Environment Variables | URL do worker de diagnostico IA. Ex.: `https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev` |
| `VITE_SPEEDTEST_WORKER_URL` | Cloudflare Pages > Settings > Environment Variables | URL do worker de speedtest (quando criado) |

**Como verificar a URL do worker existente:**
O worker `linka-ai-diagnosis-worker` ja esta deployado. Endpoint: `POST /api/ai/diagnostico-conexao`.
Prefixo de URL disponivel no `wrangler.toml` do worker ou via `npx wrangler list`.

**Variaveis opcionais (ingestao admin):**
`ADMIN_WORKER_URL` e `ADMIN_SECRET` sao do lado do worker, nao do PWA. O PWA nao acessa o admin diretamente.

### Checklist pre-deploy Cloudflare Pages

1. `npm run build` sem erros de TypeScript e sem warnings de build.
2. Verificar que nenhuma URL de API esta hardcoded no codigo — usar `import.meta.env.VITE_*`.
3. Confirmar que as variaveis de ambiente estao configuradas no dashboard Cloudflare Pages (Production e Preview separadamente).
4. Testar o build local com `npx wrangler pages dev dist/` antes de fazer push.
5. Verificar que o Service Worker nao esta cacheando rotas de API (adicionar ao `workbox-config` ou equivalente).
6. CORS: o worker IA ja tem `Access-Control-Allow-Origin: *` — nao e necessario configurar nada adicional no PWA.
7. Apos deploy: verificar no Cloudflare dashboard se o build passou (Actions > Latest deployment).

### Diferenca entre preview e producao
Cloudflare Pages cria um ambiente preview automatico para cada branch diferente de `main`.
Variaveis de ambiente de Preview e Production sao configuradas separadamente no dashboard.
Nao usar `VITE_AI_WORKER_URL` do ambiente de producao no ambiente de preview — pode gerar cota inesperada.

---

## Caso 10 — CORS em diagnostico de modem/IP local (impossivel em HTTPS)

**O que o Android faz:**
`featureFibra` conecta ao modem Nokia via HTTP local (ex.: `http://192.168.1.1/`) usando OkHttp diretamente, sem restricao de CORS. Le status GPON, PPPoE, potencia optica.

**No browser:**
O browser bloqueia requisicoes para IPs privados (RFC 1918: 10.x.x.x, 172.16-31.x.x, 192.168.x.x) a partir de paginas HTTPS. Erro resultante:

```
Access to fetch at 'http://192.168.1.1/' from origin 'https://signallq.pages.dev'
has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present...
```

Mesmo se o modem tivesse CORS configurado (o que nao tem), a pagina HTTPS nao pode chamar recursos HTTP — violacao de Mixed Content:

```
Mixed Content: The page at 'https://...' was loaded over HTTPS, but requested an
insecure resource 'http://192.168.1.1/'. This request has been blocked.
```

**Chrome Private Network Access (2023+):**
Chrome adicionou bloqueio adicional de Private Network Access que requer um header `Access-Control-Allow-Private-Network: true` do servidor. Modems domesticos nao implementam esse header.

**Conclusao:**
Diagnostico de modem local e **impossivel** no PWA sem:
a) Proxy intermediario (requer que o usuario instale algo localmente), ou
b) Extensao de browser (nao e PWA publica).

**O que fazer no PWA:**
- Nao implementar a aba de Fibra/Modem no PWA.
- Se o usuario tentar acessar essa funcionalidade, exibir: "O diagnostico do modem requer acesso direto a rede local, disponivel apenas no app Android."
- Nao tentar a requisicao e capturar o erro — isso gera exception feia no console e confunde o usuario.

**Regressao conhecida:**
Se qualquer componente PWA fizer `fetch('http://192.168.x.x/...')`, isso vai falhar silenciosamente ou com erro de CORS no console. Nao implementar.

---

## Caso 11 — IP local: impossivel obter com confiabilidade

**O que o Android faz:**
`WifiInfo.getIpAddress()` retorna o IP local IPv4 da interface Wi-Fi.

**No browser:**
`RTCPeerConnection` pode vazar o IP local via ICE candidates, mas:
- Firefox bloqueia por padrao (MDN Privacy Preferences).
- Safari bloqueia por padrao.
- Chrome bloqueia em modo incognito.
- Mesmo quando funciona, retorna multiplos IPs (loopback, VPN, Wi-Fi) sem indicar qual e qual.

**Conclusao:**
IP local: **nao implementar**. Exibir "N/A" ou omitir o campo.

IP publico: **implementavel** via `fetch('https://api.ipify.org?format=json')` ou equivalente.
Usar um worker Cloudflare proprio para isso evita dependencia de terceiros e garante CORS.

---

## Caso 12 — Paridade de veredicto qualitativo: Excelente / Bom / Regular / Fraco

**O que o Android faz:**
`SpeedtestQualityClassifier` e `DiagnosticDecisionEngine` produzem veredictos como "Conexao Sem Problemas", "Problema na Internet", "Sinal Fraco". Esses veredictos sao acompanhados de ID (ex.: DECISAO-04, WIFI-03) e status (`ok`, `attention`, `critical`, `inconclusive`).

**No PWA:**
A IA (Worker Cloudflare) retorna `status` com valores: "excelente" | "bom" | "regular" | "ruim" | "critico" | "inconclusivo".

**Mapeamento obrigatorio PWA → UI:**
| Status da IA | Cor no PWA | Texto de veredicto |
|---|---|---|
| excelente | verde (#2E7D32 / token success) | Excelente |
| bom | verde (#2E7D32 / token success) | Bom |
| regular | ambar (#F57F17 / token warning) | Regular |
| ruim | vermelho (#C62828 / token error) | Fraco |
| critico | vermelho (#C62828 / token error) | Critico |
| inconclusivo | cinza neutro | Inconclusivo |

Tokens de cor vem do design system (`colors_and_type.css`). Nao usar hex direto — usar tokens.

**Regra de paridade:**
O veredicto qualitativo do PWA deve ser semanticamente equivalente ao Android.
"Sinal Fraco" no Android (RSSI < -75 dBm, WIFI-03) = `ruim` ou `regular` no status da IA.
Nao existe mapeamento automatico perfeito — a IA gera o status baseada nos dados. O PWA confia no status da IA.

---

## Caso 13 — Compartilhar resultado: navigator.share disponivel, mas com restricoes

**O que o Android faz:**
`Intent.ACTION_SEND` com screenshot do resultado. Abre seletor nativo de apps.

**No browser:**
`navigator.share()` esta disponivel no Chrome/Android e Safari/iOS. No Firefox desktop e Chrome desktop (sem mobile), o suporte e inconsistente.

Verificar suporte antes de usar:
```typescript
if (navigator.share) {
  await navigator.share({
    title: 'Resultado SignallQ',
    text: `Download: ${dl} Mbps | Upload: ${ul} Mbps | Ping: ${ping} ms`,
    url: window.location.href,
  });
} else {
  // fallback: copiar para clipboard
  await navigator.clipboard.writeText(`Download: ${dl} Mbps ...`);
}
```

`navigator.share` com `files` (para compartilhar screenshot) requer `navigator.canShare({ files })` antes.
Screenshot via `html2canvas` ou `dom-to-image` e possivel, mas adiciona dependencia de biblioteca.

**Paridade aceitavel:** compartilhar texto com metricas e URL e suficiente para MVP.

---

## Como usar estes casos

- Antes de implementar qualquer feature de rede no PWA: checar se tem caso documentado aqui.
- Se o caso nao existir e voce descobrir uma limitacao nova: **adicionar ao final deste arquivo** antes de fechar o PR.
- Se um threshold mudar no Android: atualizar o Caso 7 e abrir issue PWA para ajustar o codigo.
- Para limitacoes permanentes (n/a-browser): nao implementar, documentar aqui e em `paridade-plataformas.md`.
