---
name: diagnostic-engine
description: Use quando a tarefa envolver diagnóstico de rede, speedtest, Wi-Fi, DNS, latência, jitter, IA de diagnóstico ou fluxo guiado do Linka.
---

Ao trabalhar com diagnóstico no Linka:

$ARGUMENTS

Agentes recomendados por fase:
- **Cláudio** — planeja e mapeia impacto
- **Otávio** — valida comportamento real em device (DNS, Wi-Fi, NetworkCallback)
- **Camilo** — implementa Android
- **Lia** — valida impacto visual e estados de UI
- **Gema** — revisão final

---

## Checkpoint de engine existente — OBRIGATÓRIO antes de criar

Antes de propor nova engine, orchestrator ou use case de diagnóstico:

1. `Grep` por "Engine", "Orchestrator", "UseCase", "Decision" nos módulos `:featureDiagnostico`, `:featureSpeedtest`, `:featureWifi`, `:featureDns`
2. Liste as engines encontradas
3. Confirme que não existe engine equivalente — não crie duplicata
4. Se engine existente cobrir >70% do caso, prefira estender a criar nova

---

## Gatilho Otávio — OBRIGATÓRIO

Se a tarefa de diagnóstico envolver qualquer um destes, **Otávio é obrigatório**:

- DNS real (não mock) — resolução, servidores, privateDns
- Wi-Fi scan, RSSI, frequência, padrão de conexão, NetworkCapabilities
- NetworkCallback, ConnectivityManager
- Comportamento de rede em background/Doze mode
- Restrições de permissão (ACCESS_FINE_LOCATION para Wi-Fi)

→ Acionar **Otávio** antes de Camilo implementar.

`[PRÓXIMO: Otávio — task envolve [DNS/Wi-Fi/NetworkCallback], validação de device obrigatória antes de Camilo]`

---

## Regras obrigatórias

1. Localize os modelos de entrada e saída do diagnóstico antes de propor mudança.
2. Localize engines, orchestrators, use cases e decision engines existentes.
3. Identifique thresholds atuais antes de propor novos valores.
4. Separe claramente: coleta de dados → classificação técnica → mensagem ao usuário.
5. A resposta final ao usuário deve conter ação prática, não só explicação.
6. Preserve compatibilidade entre diagnóstico inicial automático e complementos posteriores.
7. Não rode novo speedtest em complemento de contexto se já houver resultado salvo — salvo pedido explícito.
8. Quando faltar contexto, gere perguntas guiadas em vez de adivinhar.
9. Não esconda etapas de coleta — a UI deve refletir o que está acontecendo: `coletando dados`, `testando download`, `analisando estabilidade` ou similar.

---

## Entregue

1. **Diagnóstico do impacto** — o que a tarefa afeta no fluxo atual
2. **Engines existentes encontradas** — lista com caminhos reais
3. **Regras existentes** — thresholds e lógicas atuais relevantes
4. **Plano de alteração** — passos ordenados e seguros
5. **Riscos** — o que pode regredir no diagnóstico ou na experiência do usuário
6. **Testes necessários** — cenários de rede que devem ser validados

[PRÓXIMO: Otávio (se domínio crítico) | Cláudio (planejamento) | Camilo (implementação)]
