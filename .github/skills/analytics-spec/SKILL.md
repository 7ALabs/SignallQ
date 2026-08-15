---
name: analytics-spec
description: Especifica um evento de telemetria antes da implementação — nome, propriedades tipadas, gatilho, anti-gatilho, dados sensíveis a evitar e consumidor esperado. Absorve a função de dados/telemetria que antes seria pedida ao Gustavo (agora skill, não agente permanente — ADR-016). Invocada por Claudete ou Camilo antes de codificar comportamento novo que precisa ser medido.
argument-hint: "<nome-da-feature-ou-fluxo>"
allowed-tools: Bash(grep *), Read
---

## Quando usar

- Claudete, ao decompor uma issue que menciona "medir", "acompanhar", "saber se o usuário...".
- Camilo, antes de implementar um fluxo novo que deveria gerar evento e ainda não tem spec.
- Antes de `/check-done` — se a PR muda comportamento observável e não referencia evento nenhum,
  isso é sinal de telemetria esquecida (ver critério 4 do check-done, documentação necessária).

**Não usar para:** analisar dado já coletado (não há dashboard nem ferramenta de análise neste
repo — SignallQ não tem squad de dados dedicado pós-ADR-016), decidir o que medir do zero sem
contexto de produto (isso é decisão de Claudete, a skill só formata a especificação).

## Convenção real de nomenclatura

**Fonte de verdade:** código existente, não convenção teórica. `android/app/.../analytics/` usa
snake_case em português, **por domínio da ação, sem prefixo de app redundante** — o Firebase
Analytics já escopa por app, prefixar `signallq_` em todo evento é ruído. Exemplos confirmados em
`FirebaseAnalyticsHelper.kt`: `app_aberto`, `speedtest_iniciado`, `speedtest_concluido`.

```bash
# Ver eventos já existentes antes de propor um novo nome (evitar duplicata/inconsistência)
grep -rhoE '"[a-z][a-z0-9_]+"' android/app/src/main/kotlin/*/analytics/*.kt 2>/dev/null \
  | sort -u
```

Padrão do nome: `<dominio>_<acao_no_particípio_ou_gerundio>` — ex. `dispositivo_selecionado`,
`fibra_diagnostico_concluido`. Sem CamelCase, sem espaço, sem verbo no infinitivo.

## Estrutura da especificação

Para cada evento novo, a skill produz:

1. **Nome do evento** — seguindo a convenção acima; checa contra a lista de eventos existentes
   (grep acima) para evitar duplicata com nome diferente para o mesmo fato.
2. **Propriedades esperadas** — nome + tipo (String/Long/Double/Boolean), cada uma com 1 linha de
   propósito. Máximo de propriedades que o Firebase Analytics aceita por evento: 25 — sinalizar se
   a proposta ultrapassar.
3. **Quando dispara** — gatilho exato (ação do usuário ou transição de estado), sem ambiguidade
   ("ao concluir o teste", não "durante o teste").
4. **Quando NÃO dispara** — casos que parecem o gatilho mas não deveriam contar (ex.: retry
   automático, teste cancelado pelo usuário, execução em background sem interação).
5. **Propriedades sensíveis a evitar** — checklist: identificador de dispositivo bruto, endereço
   IP, SSID de Wi-Fi do usuário, localização, qualquer PII/PLI. Ver
   `docs_ai/legal/PRIVACY_POLICY.md` para o que já está declarado como coletado — evento novo com
   dado não declarado ali é bloqueador (mudança de política de dado sensível exige aprovação de
   Claudete + escalação a Luiz).
6. **Consumidor esperado** — Firebase Analytics (default), ou também Crashlytics (se for evento de
   erro), ou Worker Cloudflare via `docs_ai/CONTRATOS/openapi/signallq-analytics-events.yaml`
   (se o evento precisa ir além do SDK do app).

## Saída padrão

```
=== /analytics-spec — fluxo "diagnóstico de fibra" ===

Evento: fibra_diagnostico_concluido
Propriedades:
  - resultado: String        (Excelente|Bom|Regular|Fraco)
  - duracao_ms: Long         (tempo total do diagnóstico)
  - topologia: String        (roteador_direto|duplo_nat|cgnat)
Dispara quando: diagnóstico de fibra chega ao veredito final e é exibido ao usuário.
Não dispara quando: usuário sai da tela antes do veredito, diagnóstico falha por erro técnico
  (usar fibra_diagnostico_erro para esse caso, evento separado).
Sensível a evitar: nenhuma propriedade proposta é PII/PLI. Confirmado contra PRIVACY_POLICY.md.
Consumidor: Firebase Analytics (FirebaseAnalyticsHelper.kt).

Nome já existe na base? Não (grep confirmou).
```

## O que a skill NÃO faz

- Não implementa o `logEvent(...)` — só especifica.
- Não analisa dado já coletado — não há acesso a dashboard de analytics neste repo.
- Não aprova mudança de política de dado sensível — sinaliza e escala.

## Interação com o fluxo

- Claudete/Camilo invocam antes de implementar comportamento que precisa ser medido.
- A especificação resultante vira comentário na issue ou trecho do corpo da PR — `/check-done`
  espera essa evidência quando a mudança adiciona comportamento observável novo.

## Referências

- [`docs_ai/CONTRATOS/openapi/signallq-analytics-events.yaml`](../../../docs_ai/CONTRATOS/openapi/signallq-analytics-events.yaml)
- [`docs_ai/legal/PRIVACY_POLICY.md`](../../../docs_ai/legal/PRIVACY_POLICY.md)
- Código real: `android/app/src/main/kotlin/*/analytics/FirebaseAnalyticsHelper.kt`
- Persona: [Claudete](../../agents/claudete.md), [Camilo](../../agents/camilo.md)
