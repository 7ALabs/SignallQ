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

### Velocidade

- download abaixo de 10 Mbps: `slow`;
- download entre 10 e 50 Mbps: `ok`;
- download acima de 50 Mbps: `fast`;
- sem download medido: `unknown`.

### Estabilidade

- latência acima de 150 ms: atenção;
- jitter acima de 40 ms: instável;
- falhas HTTP relevantes: instável;
- sem amostras suficientes: `unknown`.

### Qualidade geral

- `bad` se velocidade ou estabilidade estiver ruim;
- `attention` se houver alerta relevante;
- `good` se velocidade e estabilidade estiverem boas;
- `unknown` se faltarem métricas principais.

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
