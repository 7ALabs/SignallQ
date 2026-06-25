# Contrato do SpeedTest Web

## Princípio

O SpeedTest do PWA precisa buscar a mesma precisão e consistência do SpeedTest Android.

A diferença de plataforma não autoriza simplificar cálculo, reduzir qualidade ou inventar uma métrica mais fraca.

O PWA deve usar a mesma metodologia do Android sempre que possível: endpoint, payload, amostragem, cálculo, classificação e critérios de descarte.

Este contrato segue a paridade definida em `pwa/docs/parity.md`.

## Status de paridade

SpeedTest no PWA é `paridade-metodologica`.

O objetivo é entregar resultado comparável ao Android para:

- download;
- upload;
- latência HTTP;
- jitter;
- bufferbloat, quando entrar no modo completo;
- classificação qualitativa.

A ressalva é apenas terminológica e técnica: browser não mede ICMP real. Se a métrica for baseada em requisição HTTP, a UI deve chamar de latência HTTP ou latência de conexão, não de ping ICMP.

## Requisitos de precisão

Antes de implementar, confirmar ou reproduzir a metodologia Android:

- endpoint de download;
- endpoint de upload;
- quantidade de conexões paralelas;
- tamanho de payload;
- tempo mínimo/máximo de janela de medição;
- regra de aquecimento/descarte de amostras;
- fórmula de Mbps;
- cálculo de latência;
- cálculo de jitter;
- cálculo de bufferbloat, se aplicável;
- thresholds de classificação.

Se qualquer item divergir do Android, a divergência deve ser documentada no PR com justificativa técnica.

## Download

Status: obrigatório para M1.

Estratégia inicial:

- usar endpoint controlado, preferencialmente o mesmo contrato Cloudflare usado pelo Android;
- medir tempo total e/ou janela útil conforme metodologia definida;
- calcular Mbps com a mesma fórmula de referência;
- descartar amostra inválida;
- usar múltiplas amostras ou conexões quando a metodologia Android exigir.

```ts
type DownloadMetric = {
  mbps: number | null;
  durationMs: number;
  bytes: number;
  samples: number;
  status: 'measured' | 'failed' | 'not_supported';
};
```

## Upload

Status: obrigatório para paridade, desde que exista endpoint controlado.

Não aceitar upload como “não disponível” sem antes validar a existência de endpoint compatível com Android ou Worker dedicado.

Se o endpoint ainda não existir, isso deve virar bloqueio/pendência explícita, não simplificação silenciosa.

```ts
type UploadMetric = {
  mbps: number | null;
  durationMs?: number;
  bytes?: number;
  samples?: number;
  status: 'measured' | 'failed' | 'not_available';
};
```

## Latência

Status: obrigatório para M1.

Estratégia:

- requisições leves para endpoint controlado;
- medição com `performance.now()`;
- usar mediana ou média robusta conforme metodologia definida;
- não chamar de ping ICMP.

```ts
type LatencyMetric = {
  ms: number | null;
  samples: number;
  status: 'measured' | 'failed';
  method: 'http_timing';
};
```

## Jitter

Status: obrigatório para resultado de estabilidade quando houver amostras suficientes.

Estratégia:

- calcular variação entre amostras de latência;
- seguir a mesma fórmula definida para Android ou documentar divergência;
- se não houver amostras suficientes, marcar como não medido e reduzir confiança.

```ts
type JitterMetric = {
  ms: number | null;
  samples: number;
  status: 'measured' | 'insufficient_samples' | 'failed';
};
```

## Bufferbloat

Status: futuro, mas deve preservar paridade se entrar no modo completo.

Pode ser estimado com múltiplas requisições paralelas e comparação de latência sob carga.

Não implementar como métrica principal sem validar metodologia e UX.

```ts
type BufferbloatMetric = {
  ms: number | null;
  severity: 'none' | 'light' | 'moderate' | 'severe' | 'unknown';
  status: 'measured' | 'not_measured' | 'failed';
};
```

## Perda percebida

Status: inferida, não real.

No navegador, não há medição confiável de perda de pacote via ICMP.

Pode ser inferida por falhas HTTP, timeouts ou indisponibilidade.

```ts
type AvailabilityMetric = {
  failedRequests: number;
  totalRequests: number;
  perceivedLossPercent: number | null;
  status: 'inferred' | 'not_measured';
};
```

## Payload mínimo

```ts
type SpeedTestResult = {
  id: string;
  measuredAt: string;
  download: DownloadMetric;
  upload: UploadMetric;
  latency: LatencyMetric;
  jitter: JitterMetric;
  availability: AvailabilityMetric;
  bufferbloat?: BufferbloatMetric;
  browser: BrowserInfo;
  connection: BrowserConnectionInfo;
  limitations: string[];
};
```

## Browser info

```ts
type BrowserInfo = {
  userAgent?: string;
  platform?: string;
  language?: string;
  viewport?: {
    width: number;
    height: number;
  };
};
```

## Connection info

```ts
type BrowserConnectionInfo = {
  effectiveType?: string;
  downlink?: number;
  rtt?: number;
  saveData?: boolean;
  source: 'network_information_api' | 'unavailable';
};
```

## Limitações obrigatórias

Registrar quando aplicável:

- `http_latency_not_icmp_ping`;
- `network_information_api_unavailable`;
- `packet_loss_not_directly_measured`;
- `browser_measurement_may_vary`;
- `wifi_signal_not_available_on_web`.

`upload_endpoint_unavailable` só deve aparecer se a ausência do endpoint for confirmada e documentada.

## Critérios de aceite

- Download não pode ser valor falso.
- Latência não pode ser chamada de ping ICMP real.
- Upload precisa ser implementado quando houver endpoint compatível.
- Jitter só aparece como medido se houver amostras suficientes.
- Perda de pacote deve ser comunicada como inferência.
- Resultado parcial deve ser permitido em caso de erro, mas com confiança menor.
- A UI deve diferenciar velocidade, estabilidade e limitações do browser.
- O PR de implementação deve declarar como manteve paridade com o Android.

## Fora do escopo do SpeedTest

- ICMP real.
- Sinal Wi-Fi/RSSI.
- Scan de rede.
- Identificação de roteador.
- Medição nativa de operadora.
- DNS benchmark real.

Essas limitações não reduzem a obrigação de precisão do SpeedTest. Apenas delimitam o que não pertence à medição web.

## Texto de limitação para UI

“Este teste mede a velocidade e estabilidade da conexão pelo navegador usando metodologia equivalente ao app sempre que possível. Algumas medições nativas, como sinal Wi-Fi real ou ping ICMP, não estão disponíveis na versão web.”
