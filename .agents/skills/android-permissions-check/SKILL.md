---
description: Checklist de permissões Android antes de implementar qualquer feature que as utilize — ACCESS_FINE_LOCATION, FOREGROUND_SERVICE, READ_PHONE_STATE e derivadas.
---

## Quando usar
Antes de implementar ou revisar qualquer código Android que declare ou solicite permissões ao usuário.

## Checklist obrigatório

### Permissões de localização
- [ ] ACCESS_FINE_LOCATION declarado no AndroidManifest?
- [ ] Solicitação feita em momento contextual (não no cold start)?
- [ ] Rationale exibido se usuário negar na primeira vez?
- [ ] Fluxo funciona com localização negada (degradação graciosa)?
- [ ] API 29+: permissão obrigatória para Wi-Fi scan?

### FOREGROUND_SERVICE
- [ ] API 34+: tipo declarado (LOCATION, NETWORK, DATA_SYNC)?
- [ ] Notificação obrigatória exibida ao iniciar serviço?
- [ ] Serviço para corretamente em onDestroy/cancelamento?

### READ_PHONE_STATE
- [ ] Justificativa clara para Play Store?
- [ ] Alternativa sem READ_PHONE_STATE avaliada primeiro?
- [ ] Funciona sem a permissão (fallback)?

### Geral
- [ ] Toda permissão solicitada está declarada no Manifest?
- [ ] `shouldShowRequestPermissionRationale` verificado antes de re-solicitar?
- [ ] Comportamento OEM testado? (Samsung pode bloquear CHANGE_NETWORK_STATE)

## Regras Play Store
- Background location: formulário de declaração obrigatório
- READ_CALL_LOG, PROCESS_OUTGOING_CALLS: justificativa explícita
- MANAGE_EXTERNAL_STORAGE: só se absolutamente necessário

## Limites
- Esta skill orienta, não implementa.
- Implementação → Camilo.
- Comportamento incerto em OEM específico → declarar explicitamente.
