# Contrato de Diagnóstico

## Objetivo

Definir o contrato mínimo do diagnóstico do SignallQ PWA, separando coleta, classificação local e resposta de IA.

## Princípio

O diagnóstico não pode inventar o que o navegador não mediu.

O diagnóstico deve ser curto, simples e acionável.

IA é melhoria de laudo, não pré-requisito para entregar resultado.

## Integração IA

A chamada de IA deve passar por Cloudflare Worker intermediário.

Antes de implementar a integração real, validar:

- endpoint correto;
- schema de request;
- schema de response;
- timeout;
- formato de erro;
- fallback local.

Não chamar serviço de IA direto do browser.

## Entrada

```ts
type DiagnosisInput = {
  speedTest: SpeedTestResult;
  userContext?: {
    declaredProblem?: 'slow' | 'unstable' | 'video' | 'gaming' | 'work_call' | 'unknown';
    usageIntent?: 'general' | 'streaming' | 'gaming' | 'video_call' | 'work' | 'unknown';
  };
};
```

## Saída

```ts
type DiagnosisResult = {
  id: string;
  generatedAt: string;
  source: 'local' | 'ai' | 'fallback';
  summary: string;
  quality: QualityClassification;
  speed: SpeedClassification;
  stability: StabilityClassification;
  experience: ExperienceClassification[];
  actions: RecommendedAction[];
  limitations: DiagnosisLimitation[];
  confidence: 'high' | 'medium' | 'low';
};
```

## Classificações

```ts
type QualityClassification = 'good' | 'attention' | 'bad' | 'unknown';
type SpeedClassification = 'fast' | 'ok' | 'slow' | 'unknown';
type StabilityClassification = 'stable' | 'unstable' | 'unknown';
```

## Experiência provável

```ts
type ExperienceClassification = {
  type: 'browsing' | 'streaming' | 'video_call' | 'gaming' | 'download';
  status: 'good' | 'attention' | 'bad' | 'unknown';
  reason: string;
};
```

## Ações recomendadas

```ts
type RecommendedAction = {
  priority: 1 | 2 | 3;
  title: string;
  description: string;
  category: 'router' | 'wifi' | 'device' | 'provider' | 'retry' | 'unknown';
};
```

## Limitações

```ts
type DiagnosisLimitation = {
  code: string;
  message: string;
};
```

Códigos iniciais:

- `upload_not_measured`;
- `jitter_not_measured`;
- `network_type_unavailable`;
- `packet_loss_not_directly_measured`;
- `http_latency_not_icmp_ping`;
- `browser_limited_measurement`;
- `native_signal_unavailable_on_web`;
- `native_scan_unavailable_on_web`;
- `dns_benchmark_unavailable_on_web`.

## Regras locais mínimas

Antes de IA, o PWA deve classificar de forma local e simples.

**Thresholds alinhados ao motor Android** (`InternetDiagnosticEngine.kt` +
`MetricClassifier.kt`, fonte `/regras-diagnostico-rede`) desde GH#438 — não
inventar novos números aqui sem atualizar os dois lados. Antes disso o PWA
usava uma régua própria (10/50 Mbps, 150/40 ms), divergente do Android, o que
gerava veredito diferente para o mesmo cenário de rede.

### Velocidade

- download abaixo de 25 Mbps: `slow` (Android IN-NORMAL-03 "Download Baixo");
- download entre 25 e 100 Mbps: `ok`;
- download acima de 100 Mbps: `fast` (faixa só de exibição no PWA — Android não
  distingue acima do mínimo saudável);
- sem download medido: `unknown`.

### Estabilidade

- latência acima de 100 ms: instável (Android IN-NORMAL-05, referência Anatel RQUAL);
- jitter acima de 20 ms: instável (Android IN-NORMAL-06);
- sem amostras de latência e jitter: `unknown`.

### Upload e perda de pacotes (entram na qualidade geral, fora do par velocidade/estabilidade)

- upload = 0 Mbps: crítico (Android IN-NORMAL-04Z "Upload Zerado");
- upload entre 0 e 5 Mbps: atenção (Android IN-NORMAL-04 "Upload Baixo");
- perda de pacotes ≥ 3%: crítico (Android IN-NORMAL-07);
- perda de pacotes entre 1% e 3%: atenção (Android IN-NORMAL-07b).

Bufferbloat (IN-NORMAL-09 no Android) **não é replicado no PWA**: exigiria medir
latência sob carga simultânea ao download, fase que o speedtest web atual não
implementa. Ver limitação `bufferbloat_not_measured`.

### Qualidade geral

- `bad` se download não foi medido, upload = 0 Mbps ou perda de pacotes ≥ 3%
  (equivalente aos casos `critical` do Android);
- `attention` se velocidade for `slow`, estabilidade for `unstable`, upload
  estiver entre 0 e 5 Mbps, perda de pacotes estiver entre 1% e 3%, ou latência/
  jitter não tiverem sido medidos;
- `good` se nenhuma das condições acima ocorrer;
- `unknown` se não houver nenhuma métrica principal (download, latência e
  jitter todos ausentes).

## Regras de texto

O diagnóstico deve ter:

- resumo de até 2 frases;
- no máximo 3 ações principais;
- linguagem PT-BR simples;
- sem termos técnicos sem explicação;
- sem tom alarmista;
- sem prometer correção garantida.

## Fallback

Se a IA falhar:

- usar diagnóstico local;
- informar que a análise avançada não está disponível;
- não bloquear o resultado do teste.

## Critérios de aceite

- Diagnóstico funciona sem IA.
- Diagnóstico nunca inventa métrica ausente.
- Diagnóstico separa velocidade de estabilidade.
- Resultado é compreensível para usuário comum.
- Resposta de IA, quando existir, respeita o contrato.
- Falha de IA não quebra o fluxo.
- Limitações web aparecem no diagnóstico quando afetam a conclusão.
