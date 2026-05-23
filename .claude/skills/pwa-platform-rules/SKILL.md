---
description: Regras de plataforma PWA para o Linka SpeedTest — APIs disponíveis no browser, limitações reais vs. Android, Service Worker, Cloudflare Pages e compatibilidade entre navegadores.
---

## Quando usar
Antes de implementar qualquer feature no PWA que use APIs de rede, geolocalização, armazenamento, notificações ou funcionalidades nativas.

## O que é possível no browser (2024-2025)

### Rede e conectividade
- `navigator.onLine` — detecta online/offline (binário, sem qualidade)
- `navigator.connection` (Network Information API) — effectiveType (4g/3g/2g), downlink estimado. **Não disponível no Safari/iOS.**
- `fetch` com AbortController — chamadas HTTP customizadas para speedtest
- WebSocket — conexão persistente para medições em tempo real
- WebRTC — pode ser usado para medir RTT (complexo, cuidado)

### Armazenamento
- `localStorage` — simples, síncrono, limite ~5MB
- `IndexedDB` — assíncrono, estruturado, capacidade maior
- Cache API (Service Worker) — assets offline

### Notificações
- Push API + Service Worker — notificações push (requer permissão do usuário)
- Notification API — notificações locais (requer permissão)

### Geolocalização
- `navigator.geolocation` — funciona, requer permissão. Menos preciso que Android nativo.

## O que NÃO é possível no browser

| Funcionalidade | Motivo |
|---|---|
| Scan de redes Wi-Fi vizinhas | API não exposta ao browser |
| RSSI em tempo real | Acesso ao hardware bloqueado |
| Foreground service persistente | Sem suporte fora de PWA instalada + SW |
| Cell tower info (RSRP/RSRQ/SINR) | API exclusivamente nativa |
| ARP scan / mDNS / UPnP | Bloqueados por segurança |
| DNS personalizado por request | Browser controla resolução DNS |
| MAC address de dispositivos | Bloqueado por privacidade |
| Interface de rede atual (Wi-Fi vs mobile) | Parcial via Network Information API |

## Compatibilidade de navegadores (crítica)

| API | Chrome | Firefox | Safari | Samsung Internet |
|---|---|---|---|---|
| Network Information | ✅ | ❌ | ❌ | ✅ |
| Service Worker | ✅ | ✅ | ✅ (14+) | ✅ |
| Push Notifications | ✅ | ✅ | ⚠️ iOS 16.4+ | ✅ |
| WebRTC | ✅ | ✅ | ✅ | ✅ |
| IndexedDB | ✅ | ✅ | ✅ | ✅ |

## Cloudflare Pages

- Deploy automático a cada push na branch configurada
- Edge Functions (Workers) para lógica server-side
- KV para armazenamento edge simples
- Sem estado entre requests em Edge Functions

## Limites
- Esta skill orienta, não implementa.
- Implementação → Renan.
- Qualquer funcionalidade classificada como "impossível no browser" → não implementar ou documentar limitação honesta ao usuário.
