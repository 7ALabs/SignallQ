---
name: regras-android
description: Regras de plataforma Android (API levels, APIs deprecated, OEM quirks, restrições Play Store) e checklist de permissões do SignallQ. Consultar antes de implementar permissões, Wi-Fi, DNS, background service ou conectividade.
---

Consulte as regras Android relevantes para a tarefa abaixo:

$ARGUMENTS

**Antes de qualquer implementação crítica, consulte esta skill e o arquivo `oem-quirks-e-testes-device.md`.**

---

## API Levels suportados pelo SignallQ

- **Mínimo:** Android 7.0 (API 24)
- **Target:** Android 16 (API 36)
- **Compilação:** Android 16+ (API 37)

---

## Permissões

### Restrições por versão

| Permissão | Mudança | API |
|---|---|---|
| ACCESS_FINE_LOCATION | Obrigatória para Wi-Fi scan | 26+ |
| ACCESS_FINE_LOCATION | Agora inclui ACCESS_COARSE | 29+ |
| ACCESS_BACKGROUND_LOCATION | Permissão separada obrigatória para acesso em background | 29+ |
| FOREGROUND_SERVICE | Declaração obrigatória no manifest | 28+ |
| FOREGROUND_SERVICE_CONNECTED_DEVICE | Tipo obrigatório para serviços de rede | 34+ |
| READ_PHONE_STATE | Necessita permissão runtime separada para TelephonyManager | 26+ |
| SCHEDULE_EXACT_ALARM | Necessita permissão explícita para alarmes exatos | 31+ |

### Quando usar o checklist

Antes de implementar ou revisar qualquer código Android que declare ou solicite permissões ao usuário.

### Checklist obrigatório

**Permissões de localização**
- [ ] ACCESS_FINE_LOCATION declarado no AndroidManifest?
- [ ] Solicitação feita em momento contextual (não no cold start)?
- [ ] Rationale exibido se usuário negar na primeira vez?
- [ ] Fluxo funciona com localização negada (degradação graciosa)?
- [ ] API 29+: permissão obrigatória para Wi-Fi scan?

**FOREGROUND_SERVICE**
- [ ] API 34+: tipo declarado (LOCATION, NETWORK, DATA_SYNC)?
- [ ] Notificação obrigatória exibida ao iniciar serviço?
- [ ] Serviço para corretamente em onDestroy/cancelamento?

**READ_PHONE_STATE**
- [ ] Justificativa clara para Play Store?
- [ ] Alternativa sem READ_PHONE_STATE avaliada primeiro?
- [ ] Funciona sem a permissão (fallback)?

**Geral**
- [ ] Toda permissão solicitada está declarada no Manifest?
- [ ] `shouldShowRequestPermissionRationale` verificado antes de re-solicitar?
- [ ] Comportamento OEM testado? (Samsung pode bloquear CHANGE_NETWORK_STATE)

---

## Wi-Fi — restrições e comportamento

| Comportamento | Restrição | API |
|---|---|---|
| WifiInfo.getSSID() | Retorna null sem ACCESS_FINE_LOCATION | 26+ |
| WifiManager.getScanResults() | Throttled: 4 scans/2min foreground, 1/30min background | 28+ |
| WifiManager.getConnectionInfo() | Deprecated — usar NetworkCapabilities/WifiInfo via NetworkCallback | 31+ |
| WifiInfo via NetworkCallback | Forma recomendada para obter info do Wi-Fi ativo | 29+ |
| NetworkCapabilities.getTransportInfo() | API para WifiInfo em NetworkCallback | 29+ |

**OEM Wi-Fi quirks:**
- Samsung One UI 5+: pode bloquear scan de background sem configuração manual do usuário
- MIUI 12+: gestão agressiva de background pode interromper NetworkCallback
- Moto: comportamento padrão Android AOSP — menos quirks

---

## DNS — comportamento real

| Comportamento | Detalhe |
|---|---|
| LinkProperties.getDnsServers() | Retorna servidores DNS ativos do link atual |
| DNS Privado (DoT) | privateDnsServerName em ConnectivityManager — API 28+ |
| InetAddress.getByName() | Bloqueante — nunca chamar na Main thread |
| DNS resolution timeout | Varia por OEM — Samsung pode ter timeout mais agressivo |
| DNS em Wi-Fi vs LTE | LinkProperties diferente por network — verificar network ativa antes |
| getPrivateDnsServerName() | Retorna null se DNS Privado estiver em modo automático | API 28+ |

---

## Background Execution — limites reais

| Restrição | Impacto | API |
|---|---|---|
| Doze Mode | Janelas de manutenção para rede e CPU — WorkManager respeita | 23+ |
| App Standby | Apps inativos têm acesso restrito à rede | 23+ |
| Background Location | Permissão separada + justificativa obrigatória no Play Store | 29+ |
| Exact Alarms | SCHEDULE_EXACT_ALARM obrigatório para alarmes exatos | 31+ |
| ForegroundService obrigatório | Para acesso a localização ou rede em background contínuo | 26+ |
| Battery Saver | Pode pausar WorkManager — verificar isPowerSaveMode() | 23+ |
| Background network access | Restrito sem ForegroundService ativo | 26+ |

---

## ConnectivityManager — boas práticas

| Prática | Motivo |
|---|---|
| Sempre usar NetworkCallback em vez de polling | Eficiente, respeita Doze mode |
| Verificar network.hasCapability(NET_CAPABILITY_INTERNET) | Confirma acesso real, não apenas conectividade física |
| Usar getLinkProperties(network) com a network ativa | Evitar dados desatualizados |
| Unregister callback ao destruir ViewModel/Fragment | Evitar leak de callback |

---

## Restrições Play Store relevantes ao SignallQ

| Restrição | Impacto |
|---|---|
| Permissão de localização em background | Exige justificativa real no Play Console; apps de diagnóstico de rede podem ser aprovados mas deve ser documentado. Formulário de declaração obrigatório |
| FOREGROUND_SERVICE_CONNECTED_DEVICE | Para serviços que acessam rede — declarar tipo correto API 34+ |
| Acesso a IMEI/Device ID | Fortemente restrito API 29+ — alternativas: Android ID ou Firebase Install ID |
| REQUEST_INSTALL_PACKAGES | Não usar sem necessidade explícita |
| READ_CALL_LOG, PROCESS_OUTGOING_CALLS | Justificativa explícita obrigatória |
| MANAGE_EXTERNAL_STORAGE | Só se absolutamente necessário |

---

## Compose Lifecycle — pitfalls

| Pitfall | Problema | Solução |
|---|---|---|
| LaunchedEffect com key errada | Re-executa desnecessariamente | Usar key correta (Unit para uma vez, valor para reacao) |
| rememberCoroutineScope em composição | Scope não cancelado corretamente | Preferir viewModelScope para operações longas |
| collectAsState sem lifecycle | Coleta em background mesmo quando inativo | Usar collectAsStateWithLifecycle |
| Side effect fora de remember/LaunchedEffect | Executa a cada recomposição | Mover para efeito adequado |

---

## Regra de consulta

Antes de implementar qualquer feature nas categorias acima, o agente **deve**:

1. Verificar a tabela de restrições correspondente nesta skill.
2. Confirmar qual API level mínimo afeta o comportamento.
3. Consultar `oem-quirks-e-testes-device.md` se houver dúvida de comportamento real em device.

**Documentação oficial não equivale a comportamento OEM real.** Consultar quirks documentados e testar em device real antes de implementar Wi-Fi, DNS, permissão ou background service.

## Limites

- Esta skill orienta, não implementa.
- Implementação → Camilo.
- Comportamento incerto em OEM específico → declarar explicitamente.
