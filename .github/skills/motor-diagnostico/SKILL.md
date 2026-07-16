---
name: motor-diagnostico
description: Use para diagnóstico de rede, speedtest (download/upload/latência/jitter/perda), IA de diagnóstico e revisão da jornada do usuário no fluxo de diagnóstico do SignallQ.
---

Skill consolidada para tudo que envolve o diagnóstico do SignallQ: o motor (engine/orchestrator/use cases), o fluxo de speedtest e a jornada do usuário. Cobre Wi-Fi, DNS, latência, jitter, perda de pacotes, IA de diagnóstico e fluxo guiado.

Thresholds de qualidade e padrões técnicos brasileiros: consulte sempre `/regras-diagnostico-rede`. **Não duplicar thresholds aqui.**

Agentes recomendados por fase:
- **Claudete** — planeja e mapeia impacto
- **`/regras-android`** — valida comportamento real em device (DNS, Wi-Fi, NetworkCallback, OEM quirks)
- **Camilo** — implementa Android
- **Lia** — valida impacto visual, estados de UI e jornada (modo Sonnet em decisão de produto)
- **Rhodolfo** — revisão final

---

## 1. Motor / Engine

### Checkpoint de engine existente — OBRIGATÓRIO antes de criar
Antes de propor nova engine, orchestrator ou use case de diagnóstico:
1. `Grep` por "Engine", "Orchestrator", "UseCase", "Decision" nos módulos `:featureDiagnostico`, `:featureSpeedtest`, `:featureWifi`, `:featureDns`
2. Liste as engines encontradas
3. Confirme que não existe engine equivalente — não crie duplicata
4. Se engine existente cobrir >70% do caso, prefira estender a criar nova

### Gatilho `/regras-android` — OBRIGATÓRIO
Se a tarefa envolver qualquer um destes, **consultar `/regras-android` é obrigatório**:
- DNS real (não mock) — resolução, servidores, privateDns
- Wi-Fi scan, RSSI, frequência, padrão de conexão, NetworkCapabilities
- NetworkCallback, ConnectivityManager
- Comportamento de rede em background/Doze mode
- Restrições de permissão (ACCESS_FINE_LOCATION para Wi-Fi)

→ Invocar `/regras-android` antes de Camilo implementar.

`[PRÓXIMO: /regras-android — task envolve [DNS/Wi-Fi/NetworkCallback], validação obrigatória antes de Camilo]`

### Regras obrigatórias do motor
1. Localize os modelos de entrada e saída do diagnóstico antes de propor mudança.
2. Localize engines, orchestrators, use cases e decision engines existentes.
3. Identifique thresholds atuais (em `/regras-diagnostico-rede`) antes de propor novos valores.
4. Separe claramente: coleta de dados → classificação técnica → mensagem ao usuário.
5. A resposta final ao usuário deve conter ação prática, não só explicação.
6. Preserve compatibilidade entre diagnóstico inicial automático e complementos posteriores.
7. Não rode novo speedtest em complemento de contexto se já houver resultado salvo — salvo pedido explícito.
8. Quando faltar contexto, gere perguntas guiadas em vez de adivinhar.
9. Não esconda etapas de coleta — a UI deve refletir o que está acontecendo: `coletando dados`, `testando download`, `analisando estabilidade` ou similar.

---

## 2. Fluxo de Speedtest

Antes de implementar ou alterar qualquer parte do fluxo de speedtest no Android (`:featureSpeedtest`, `:coreNetwork`).

### Arquitetura do fluxo
```
UI (SpeedtestScreen) → SpeedtestViewModel → SpeedtestUseCase → NetworkEngine → Results
```
- ViewModel expõe `StateFlow<SpeedtestUiState>` — nunca valores crus.
- Engine roda em `Dispatchers.IO` — nunca na Main thread.
- Resultados persistidos no Room (`:coreDatabase`) após conclusão.

### Estados obrigatórios da UI
| Estado | O que exibir |
|---|---|
| Idle | Botão de início, última medição |
| Preparing | "Preparando teste..." + spinner |
| DownloadRunning | Velocidade atual em tempo real + barra de progresso |
| UploadRunning | Velocidade atual em tempo real + barra de progresso |
| LatencyRunning | "Medindo latência..." |
| Completed | Resultado completo (down/up/ping/jitter/loss) |
| Error | Mensagem do erro + botão retry |

### Métricas obrigatórias
| Métrica | Unidade | Precisão |
|---|---|---|
| Download | Mbps | 2 casas decimais |
| Upload | Mbps | 2 casas decimais |
| Latência | ms | inteiro |
| Jitter | ms | 1 casa decimal |
| Perda de pacotes | % | 1 casa decimal |

Thresholds de resultado (Brasil): consulte `/regras-diagnostico-rede`.

### Anti-padrões
- Bloquear Main thread durante medição.
- Exibir Mbps em vez de Mbps/s durante medição em tempo real.
- Salvar resultado parcial no banco (apenas resultado final completo).
- Omitir estado de erro — sempre oferecer retry.
- Calcular jitter sem mínimo de 10 amostras.

---

## 3. Jornada do Usuário

Ao modificar qualquer parte do fluxo de diagnóstico, revise pela perspectiva do usuário. Lia executa em modo Sonnet (decisão de produto).

### Passos
1. Mapear estados do fluxo atual: idle → iniciando → coletando → analisando → resultado.
2. Verificar transições: cada estado tem microcopy claro e duração previsível.
3. Verificar o resultado: o usuário entende o problema e sabe o que fazer.
4. Verificar fallbacks: o que acontece se o diagnóstico falha ou fica incompleto.

### Checklist
- [ ] Cada estado de loading comunica o que está acontecendo.
- [ ] Resultado explica o problema em linguagem não-técnica.
- [ ] Resultado oferece ação clara (não só "seu sinal está fraco").
- [ ] Estado de erro explica o que falhou e o que o usuário pode tentar.
- [ ] Animações de progresso não bloqueiam leitura de informação.

---

## Entregue
1. **Diagnóstico do impacto** — o que a tarefa afeta no fluxo atual
2. **Engines existentes encontradas** — lista com caminhos reais
3. **Regras existentes** — thresholds (de `/regras-diagnostico-rede`) e lógicas atuais relevantes
4. **Plano de alteração** — passos ordenados e seguros
5. **Riscos** — o que pode regredir no diagnóstico ou na experiência do usuário
6. **Testes necessários** — cenários de rede que devem ser validados
7. **Avaliação da jornada** — por estado, com problemas encontrados e sugestão de melhoria

---

## Limites
- Esta skill orienta, não implementa. Implementação → Camilo.

[PRÓXIMO: /regras-diagnostico-rede (thresholds) | /regras-android (se domínio crítico) | Claudete (planejamento) | Camilo (implementação)]
