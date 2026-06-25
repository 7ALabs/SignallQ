---
titulo: Quirks OEM e Template de Teste de Rede em Device Real
atualizado: 2026-06-24
versao-signallq: 0.21.0
---

# Quirks OEM e Template de Teste de Rede

Referência consolidada de comportamentos específicos por fabricante ao testar diagnóstico de rede no SignallQ. Complementa `SKILL.md`.

---

## Samsung (One UI)

### Modo economia de energia agressivo (Extreme / Maximum)
- Em modo "Economia máxima de energia", o One UI pausa WorkManager de forma não-padrão: jobs agendados simplesmente não disparam, sem nenhum callback de erro.
- O `MonitoramentoWorker` (30 min) pode ficar silencioso por horas sem qualquer log de falha.
- `isPowerSaveMode()` retorna `true`, mas não distingue entre modo "economia" normal e o modo extremo.
- **Impacto no SignallQ:** monitoramento de background silenciosamente interrompido; usuário não vê erro, só ausência de dados no histórico.
- **Mitigação já no código:** `MonitorRedeAndroid` registra `registerDefaultNetworkCallback` que sobrevive ao Doze normal, mas não ao modo extremo Samsung.
- **O que verificar em teste:** com bateria de modo extremo ativo, confirmar se o WorkManager dispara via `adb shell dumpsys jobscheduler`. Se não disparar, documentar como known limitation.

### Samsung Knox e restrições de DNS em redes corporativas
- Dispositivos gerenciados pelo Samsung Knox (MDM empresarial) podem bloquear resolução DNS para servidores específicos via política de rede.
- `DnsDiagnosticEngine` pode reportar DNS-01 (muito lento) ou falha de resolução quando na verdade é restrição de Knox, não problema de rede.
- `getLinkProperties().dnsServers` retorna os servidores configurados, mas Knox pode interceptar as queries antes de chegar ao servidor listado.
- **O que verificar:** se DNS latência > 300ms e o usuário está em rede corporativa com Samsung, questionar se o device é gerenciado (Knox ativo).

### WiFi Assist — troca automática para dados móveis (Smart Network Switch)
- One UI tem "Smart Network Switch" (Assistente de Wi-Fi): quando o sinal WiFi cai abaixo de um threshold interno Samsung, o sistema pode silenciosamente preferir LTE mesmo estando "conectado" ao WiFi.
- O `MonitorRedeAndroid.callbackRede` vai reportar TRANSPORT_WIFI mesmo com o tráfego indo por CELLULAR na prática — porque a rede ativa reportada é WiFi, mas o roteamento de saída pode ser celular.
- **Sintoma:** RSSI baixo (-75 a -80 dBm) + latência inconsistente + speedtest varia muito entre tentativas no mesmo ponto.
- **O que verificar em teste:** desabilitar "Smart Network Switch" em Configurações > Conexões > WiFi > Avançado antes de testar WiFi instável. Comparar resultados.
- **Versões afetadas:** One UI 3.0+ (Android 11+). Mais agressivo no One UI 5+.

### VALIDATED delay no One UI
- One UI frequentemente dispara `onAvailable()` do NetworkCallback antes de `NET_CAPABILITY_VALIDATED` estar presente.
- O `MonitorRedeAndroid` já trata isso com retry de 600ms e fallback após 1 tentativa (ver comentário no código `#123`).
- Em testes Samsung, aguardar ~1-2 segundos após conectar ao WiFi antes de iniciar diagnóstico para garantir estado estável.

---

## Motorola

### Moto Actions e interferência com sensores de rede
- "Moto Actions" (gestos físicos: virar de frente pra baixo para silenciar, etc.) não interfere diretamente com rede.
- O que pode interferir: o modo "Attentive Display" (tela sempre ligada enquanto olha) e "Display Moto" — mas são visuais, sem impacto em ConnectivityManager ou WorkManager.
- **Conclusão:** Moto Actions não é uma fonte de bug de rede no SignallQ. Documentado aqui para fechar o loop.

### Moto G (low-end) vs Moto Edge (mid-high)
- **Moto G (G14, G34, G54):** API mínima suportada pelo SignallQ é 24. Moto G low-end pode rodar Android 8/9 sem suporte a algumas APIs:
  - `WifiInfo.wifiStandard` só está disponível em API 30+. `MonitorRedeAndroid` já faz guard `Build.VERSION.SDK_INT >= Build.VERSION_CODES.R`.
  - `NetworkCapabilities.getTransportInfo()` disponível em API 29+. Em API 24-28, o código cai no fallback de `WifiManager.connectionInfo` (deprecated mas ainda funciona).
  - Performance: em devices com <3GB RAM, o WorkManager pode ser morto pelo sistema durante pressão de memória. Não é bug do SignallQ, é limitação do hardware.
  - Throttle de CPU em modelos low-end pode afetar tempo de medição de latência de gateway (`GatewayLatencyMeasurer`).

- **Moto Edge (Edge 30, Edge 40, Edge 50):** comportamento próximo ao AOSP. Menos quirks. APIs modernas disponíveis. WorkManager estável.
  - Em testes, priorize Moto Edge para validar o happy path e Moto G para validar edge cases de API level e memória.

### Background execution no Motorola
- Motorola segue AOSP de perto. `Doze Mode` e `App Standby` são padrão Google.
- Não tem lista negra proprietária de apps (diferente de Xiaomi/Samsung).
- WorkManager funciona de forma confiável em Motorola em Doze mode — janelas de manutenção respeitadas normalmente.
- **Known difference:** alguns Moto G com Android 11 apresentam `onLost()` disparando 500ms antes do `onAvailable()` em handoff WiFi → 4G. O debounce de 2000ms já em `MonitorRedeAndroid` resolve isso.

---

## Xiaomi (MIUI / HyperOS)

### AutoStart — obrigatório para WorkManager funcionar
- MIUI e HyperOS têm uma lista negra de apps que não podem iniciar automaticamente. Por padrão, apps instalados via APK (não Play Store) entram na lista negra.
- **Efeito:** `MonitoramentoWorker` agendado com WorkManager simplesmente nunca dispara após reinicialização do device ou após o sistema "limpar" a memória.
- **Como verificar:** Ajustes > Apps > Gerenciar apps > SignallQ > Início automático: deve estar ON.
- **Impacto no SignallQ:** histórico de diagnóstico pode aparecer com lacunas de horas; o usuário acha que o app não funciona.
- **Mitigation atual:** não há mitigação programática confiável — é setting do usuário. Documentar no onboarding.

### DNS over HTTPS (DoH) bloqueado por padrão em algumas versões MIUI
- MIUI 12 e 12.5 em algumas ROMs de operadora brasileira (Claro, Vivo) bloqueiam DoH na camada de sistema.
- `getLinkProperties().privateDnsServerName` retorna null (modo automático), mas consultas DoH feitas pelo app para `1.1.1.1/dns-query` ou `8.8.8.8/resolve` podem falhar silenciosamente com timeout.
- `DnsDiagnosticEngine` vai medir latência do DNS atual (via `currentDnsLatencyMs`), que é o DNS do sistema (geralmente DNS do operador) — isso funciona normalmente.
- O problema é quando o usuário tenta usar DNS privado (configurado em Ajustes > Rede > DNS Privado) em MIUI 12: pode haver comportamento imprevisível dependendo da ROM.
- **MIUI 13+ / HyperOS:** situação melhorou. DoH funciona normalmente em ROMs globais.

### Bateria otimizada que mata WorkManager
- Xiaomi tem "Bateria e desempenho" com modo "Equilibrado" e "Economia de bateria" que são mais agressivos que o Doze padrão Android.
- Em "Economia de bateria" ou com o app não na lista de "Sem restrições", WorkManager é suspenso mesmo fora do Doze window.
- **O que verificar em teste:** Ajustes > Apps > Gerenciar apps > SignallQ > Bateria > Sem restrições. Se estiver em "Otimizado", WorkManager pode não disparar.
- Diferente do Samsung (que mata no modo extremo), o Xiaomi pode matar mesmo em uso normal se o app não for aberto com frequência.

### MIUI Security App — permissões de rede
- O "Segurança" (Security app) da Xiaomi pode revogar permissões de rede para apps em background.
- Sintoma: `NetworkCallback` para de receber eventos após o app ficar em background por algumas horas.
- **Como detectar:** log `MonitorRede iniciado/encerrado` deixa de aparecer.
- **Versões mais afetadas:** MIUI 12 e 12.5. MIUI 13+ e HyperOS melhoraram.

---

## Template de Teste de Rede em Device Real

### Checklist pré-teste obrigatório

Antes de qualquer teste de diagnóstico em device real:

**Configuração do device:**
- [ ] VPN desligada (VPN altera roteamento DNS e mascarara latência real)
- [ ] Modo avião off
- [ ] Bluetooth off (pode causar interferência em 2.4 GHz em alguns modelos)
- [ ] Bateria > 20% (evitar restrições de bateria imprevistas)
- [ ] Data e hora automáticas (evitar anomalia de timestamp no histórico)
- [ ] Nenhum download ou streaming rodando em background (consome banda durante speedtest)

**Para teste WiFi:**
- [ ] Conectado na rede alvo (confirmar SSID no app)
- [ ] Localização ativada (obrigatório para SSID/BSSID em API 26+)
- [ ] Samsung: "Smart Network Switch" desligado em Configurações > WiFi > Avançado
- [ ] Xiaomi: AutoStart do SignallQ habilitado
- [ ] Xiaomi: Bateria do SignallQ em "Sem restrições"

**Para teste de dados móveis:**
- [ ] WiFi desligado completamente (não apenas desconectado de rede)
- [ ] SIM com dados habilitados e plano ativo
- [ ] Confirmar tipo de rede (4G/LTE, 3G) em status bar antes de iniciar

---

### Cenários obrigatórios

#### Cenário 1: WiFi estável
- **Setup:** próximo ao roteador, sinal esperado > -60 dBm
- **O que medir:** RSSI, banda (2.4/5 GHz), link speed, download, upload, latência, jitter
- **Veredito esperado:** WIFI-01 ou WIFI-02 (sinal excelente/bom), IN-NORMAL-02 (conexão saudável)
- **O que registrar:** todos os valores brutos + veredito do diagnóstico completo

#### Cenário 2: WiFi instável / sinal fraco
- **Setup:** distância do roteador para obter RSSI entre -75 e -85 dBm, ou com paredes/obstáculos
- **O que medir:** RSSI, se o diagnóstico marca como inconclusivo (IN-NORMAL-08 path), se UI mostra loading ou erro
- **Veredito esperado:** WIFI-03 ou WIFI-04, resultados de internet marcados como `inconclusive`
- **Samsung:** verificar se Smart Network Switch troca para 4G silenciosamente (ver RSSI cair + latência mudar)

#### Cenário 3: 4G estável
- **Setup:** WiFi desligado, área com boa cobertura (4-5 barras)
- **O que medir:** tipo de rede (LTE/4G), download, upload, latência
- **Veredito esperado:** diagnóstico com tipo `movel`, métricas dentro do esperado para 4G (download 10-50 Mbps típico)
- **Xiaomi MIUI 12:** confirmar que DNS resolve normalmente (potencial bloqueio DoH)

#### Cenário 4: 4G fraco / área de borda de cobertura
- **Setup:** área com 1-2 barras de sinal, ou dentro de prédio com bloqueio parcial
- **O que medir:** se o app reporta corretamente a qualidade de sinal móvel via `MobileSignalDiagnosticEngine`
- **Veredito esperado:** sinal fraco apontado, diagnóstico inconclusivo ou com atenção

#### Cenário 5: transição WiFi → 4G
- **Setup:** iniciar conectado ao WiFi, desligar WiFi durante ou após o diagnóstico
- **O que verificar:** se `MonitorRedeAndroid` detecta a transição via `onLost()`/`onAvailable()`, se o debounce de 2000ms evita "piscar" de desconexão na UI
- **Motorola:** transição pode ser mais abrupta (ver quirk Moto G acima)
- **Samsung:** pode demorar mais para confirmar transição por causa do VALIDATED delay

---

### O que registrar por teste

Para cada cenário executado, anotar:

| Campo | Valor |
|---|---|
| Device (fabricante + modelo) | ex: Samsung Galaxy A54 |
| Versão Android | ex: Android 14 (One UI 6.1) |
| Versão SignallQ | ex: 0.21.0 (build 52) |
| Cenário executado | ex: WiFi instável |
| RSSI (dBm) | ex: -78 dBm |
| Banda WiFi | ex: 2.4 GHz |
| Download (Mbps) | ex: 12.3 Mbps |
| Upload (Mbps) | ex: 3.1 Mbps |
| Latência (ms) | ex: 45 ms |
| Jitter (ms) | ex: 8 ms |
| Veredito principal | ex: WIFI-03 (Sinal Fraco) |
| Veredito internet | ex: IN-NORMAL-02-inc (inconclusivo por sinal fraco) |
| UI de loading funcionou? | ex: Sim, spinner apareceu e desapareceu |
| UI de erro funcionou? | ex: N/A (não houve erro) |
| Quirk OEM encontrado | ex: Smart Network Switch trocou para 4G com RSSI -80 dBm |
| WorkManager disparou? | ex: Sim, confirmado em adb dumpsys jobscheduler |
| Observações | ex: Demorou 3s para estabilizar VALIDATED no Samsung |

---

### Campos para anotar quirks OEM específicos

Ao encontrar comportamento inesperado em device específico, registrar:

```
OEM: [Samsung / Motorola / Xiaomi / outro]
Modelo: 
SO / versão da UI OEM:
Quirk encontrado: [descrição do comportamento]
Condição de reprodução: [o que fazer para reproduzir]
Impacto no SignallQ: [o que quebra ou se comporta diferente]
Workaround testado: [se encontrou solução]
Issue Linear relacionada: #N
```

---

## Resumo rápido por OEM

| OEM | Principal risco | O que checar antes do teste |
|---|---|---|
| Samsung One UI | WiFi Assist troca para 4G; VALIDATED delay; modo extremo mata WorkManager | Desligar Smart Network Switch; checar modo de bateria |
| Motorola Moto G | API level baixo em modelos antigos; kill por memória em <3GB RAM | Verificar versão Android; fechar outros apps |
| Motorola Edge | Comportamento AOSP, menos quirks | Happy path confiável para baseline |
| Xiaomi MIUI | AutoStart obrigatório; bateria agressiva; DoH bloqueado em MIUI 12 | AutoStart ON; bateria em "Sem restrições" |
| Xiaomi HyperOS | Melhorou em relação ao MIUI 12; quirks residuais de bateria | Bateria em "Sem restrições" |
