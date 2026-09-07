# Architecture Plan — trabalho corrente

> Use somente quando o gate arquitetural do `AGENTS.md` for acionado. Camillo mantém este artefato curto e proporcional à mudança.

## Problema

O resultado do Assist não informa se sua explicação veio de inferência de IA, do catálogo validado ou de fallback determinístico. A tela também exibe confiança, embora isso não ajude a pessoa a decidir o próximo passo.

## Comportamento esperado

O resultado mostra, de modo discreto e apenas quando conhecido: “Explicação por IA · [modelo público]”, “Explicação validada do Assist” ou “Baseado nas regras do diagnóstico”. Não mostra confiança. Resultado local só é chamado de local quando a fonte de avaliação for realmente local.

## Arquitetura atual relevante

O NDS V1 contém `explanation_source`, `fallback_used` e `ai_model_used` no módulo `ai`. O V2 responde `{raw, explanation}`, mas o Android preserva `raw` genericamente e não projeta essa procedência para `DiagnosticReport`.

## Decisão

Adicionar `explanation.provenance` opcional ao V2, com `source` fechado (`ai`, `copy_catalog`, `deterministic`) e `model_label` público opcional. O Worker deriva o rótulo por allowlist; nunca repassa `ai_model_used` bruto. Android converte V1/V2 para um tipo de domínio opcional em `DiagnosticReport`; a UI não lê JSON bruto.

## Impacto

- Módulos/repositórios: NDS, `:core:nds`, `:core:diagnostico` e `:app`.
- APIs/contratos: extensão aditiva de `explanation.provenance` no V2.
- Fluxo de dados: NDS decide procedência → resposta V1/V2 → mapper tipado → `DiagnosticReport` → indicador opcional.
- Persistência/migração: nenhuma.
- Segurança/privacidade: `model_label` allowlisted; não expor configuração, erro, tokens ou nome bruto do modelo.
- Compatibilidade/fallback: clientes antigos ignoram o campo; ausência não renderiza indicador; fallback remoto não é tratado como aparelho.

## Testes e validação

NDS: contratos V2 para IA, catálogo e fallback, com rótulo desconhecido omitido; OpenAPI regenerado. Android: parser/mapper V1/V2, ausência/malformação segura e composição do indicador; regressão de falha remota do Assist. Validação visual em dispositivo quando disponível.

## Riscos

Nomes de modelos podem expor detalhe operacional ou mudar. O Worker usa allowlist e o campo é opcional. Rollout parcial não quebra clientes porque a procedência é aditiva e sua ausência é silenciosa.

## Não-objetivos

Não muda motor determinístico, severidade, recomendação, persistência de histórico nem a exigência de resposta remota no Assist.

## Modo gamer — validade da medição e reteste

### Problema e decisão

Hoje `ModoGamerViewModel` mede somente o ping específico e avalia o restante contra o `DiagnosticInput` disponível. Esse input prefere o resultado em memória, mas pode cair silenciosamente na última `MedicaoEntity`, sem idade ou identidade de rede. Além disso, `ModoGamerScreen` chama `analisarProblema()` ao entrar no resultado.

O veredito gamer só poderá usar uma medição de speedtest concluída, não contaminada, de no máximo 15 minutos e com `networkId` igual ao da rede atual. O ping específico da rota vale no máximo 2 minutos e pertence à mesma tentativa gamer. Sem esses requisitos, não há veredito: a tela mostra o CTA “Fazer um teste rápido para jogar” e, ao toque, inicia o speedtest automaticamente; conserva jogo e aparelho e retorna ao resultado apenas depois da nova medição e do ping específico. A IA não será disparada automaticamente nesse fluxo.

### Arquitetura e responsabilidades

- `:core:database`: reutilizar `MedicaoEntity.timestampEpochMs`, `status` e `networkId`; adicionar uma consulta específica, limitada por tempo/rede/status, em vez de fazer o Modo gamer escolher `observarUltimas(1)`.
- `:app`/`MainViewModel`: oferecer uma operação gamer explícita que resolve a elegibilidade e aciona o pipeline canônico `reiniciarSuite(ModoSpeedtest.fast)`. Não chamar `solicitarDiagnostico()`: ela só reavalia os dados disponíveis e não inicia um speedtest.
- `:app`/`AppShell` e `ModoGamerScreen`: transportar o estado da medição gamer e a ação do CTA sem navegar para a aba Velocidade; o `ModoGamerViewModel` mantém a seleção atual durante espera, falha e retorno.
- `:app`/`ModoGamerViewModel`: receber uma evidência gamer tipada (medição-base identificada + ping com timestamp), nunca um `DiagnosticInput` sem proveniência temporal. A IA deixa de ser dependência do resultado gamer; o veredito vem do `ModoGamerEngine`.

### Falhas, compatibilidade e testes

`networkId` nulo, mudança de rede, medição vencida, contaminada, parcial/inconclusiva ou falha/cancelamento do speedtest não produzem “Bom pra jogar”; preservam jogo/aparelho e oferecem tentar de novo. A confirmação já existente para rede medida continua antes de qualquer teste que possa consumir dados. Nenhuma migração é necessária: medições antigas com `networkId = null` simplesmente não são reutilizáveis.

Davi implementa UI, navegação de estado e testes do ViewModel; Ramon define/valida a elegibilidade da evidência e a integração com o speedtest. Cobrir: 14:59 vs 15:00, mesma rede vs rede trocada/nula, status inválidos, ping com 1:59 vs 2:00, CTA que mantém seleção, término/falha/cancelamento e ausência de chamada à IA. Breno valida em aparelho Wi-Fi e rede medida, incluindo troca de rede durante o teste.

### Riscos e não-objetivos

O principal risco é disparar só o ping ou só uma reavaliação e rotulá-los como novo speedtest; a operação gamer deve aguardar uma execução nova identificada antes de concluir. Não mudamos os thresholds do `ModoGamerEngine`, o histórico como recurso de consulta, nem o contrato NDS/Worker; esta fatia apenas impede seu uso silencioso como veredito atual.
