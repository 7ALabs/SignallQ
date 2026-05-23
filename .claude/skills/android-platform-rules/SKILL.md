---
description: Centraliza regras de plataforma Android por API level, OEM quirks (Samsung, Xiaomi, Moto), restrições Play Store e comportamentos críticos de device. Substitui o agente Otávio. Consultar antes de implementar permissões, Wi-Fi, DNS, background service ou conectividade.
---

## Quando usar
Antes de implementar qualquer feature Android que envolva: permissões, Wi-Fi, DNS, background service, ConnectivityManager, VPN ou comportamento OEM-específico.

## Regras por domínio

### Permissões
- ACCESS_FINE_LOCATION: obrigatório para Wi-Fi scan a partir de API 29. Perguntar ao usuário no momento certo (não no cold start).
- FOREGROUND_SERVICE_*: a partir de API 34, especificar o tipo (LOCATION, NETWORK, etc).
- CHANGE_NETWORK_STATE: não exige permissão de usuário, mas pode ser bloqueado por OEM.
- READ_PHONE_STATE: Play Store exige justificativa desde 2022. Usar com cautela.

### Wi-Fi
- WifiInfo.getRssi(): retorna dBm. Em API 31+, pode retornar valores menos precisos por privacidade.
- WifiManager.startScan(): deprecated em API 28. Usar passivo scan ou NetworkCallback.
- ScanResults: requer ACCESS_FINE_LOCATION + Wi-Fi habilitado. Samsung pode limitar frequência.
- WifiNetworkSuggestion: API 29+. Aprovação do usuário obrigatória.

### DNS
- LinkProperties.getDnsServers(): funciona, mas OEMs podem cachear valores antigos.
- Private DNS (privateDnsServerName): API 28+. Pode estar bloqueado por perfil de trabalho.

### Background / Foreground Service
- Doze mode: suspende jobs a partir de API 23. WorkManager é o caminho correto para background.
- Battery Saver: pode matar foreground service em OEMs (especialmente Xiaomi MIUI e Samsung One UI).
- ForegroundService com network: exige FOREGROUND_SERVICE_NETWORK_BINDING em API 34+.

### OEM Quirks
- **Samsung One UI**: pode limitar scan Wi-Fi a 1x por minuto. RSSI reports podem ter offset de ±2dBm.
- **Xiaomi MIUI**: mata processos agressivamente em background. Testar com "Battery -> No restrictions".
- **Motorola**: geralmente se comporta próximo ao AOSP. Menos quirks.

### Play Store
- Evitar PROCESS_OUTGOING_CALLS, READ_CALL_LOG sem justificativa clara.
- Location em background exige formulário de declaração de uso.
- Não usar MANAGE_EXTERNAL_STORAGE sem necessidade real.

## Comportamento incerto
Se o comportamento for incerto em um OEM específico → declarar explicitamente: "Comportamento incerto em [OEM] — recomendo teste em device real."

## Limites
- Esta skill não implementa — apenas orienta.
- Implementação → Camilo.
