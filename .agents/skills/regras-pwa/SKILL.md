---
name: regras-pwa
description: Regras de plataforma PWA do SignallQ — APIs do browser, Service Worker, Cloudflare Pages e compatibilidade entre navegadores — e as limitações reais do browser vs. Android, com a forma honesta de comunicá-las ao usuário.
---

## Quando usar
Antes de implementar ou avaliar qualquer feature do PWA que use rede, geolocalização, armazenamento, notificações ou funcionalidades nativas — e sempre que precisar decidir se uma feature é viável no browser.

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

## Possível sem restrição (speedtest)
- Latência (ping) via fetch timing
- Download speed via fetch com blob
- Upload speed via fetch POST
- Histórico local via IndexedDB
- Offline básico via Service Worker cache

## Parcialmente possível (implementar com ressalva)

| Funcionalidade | O que funciona | Limitação |
|---|---|---|
| Tipo de conexão | `navigator.connection.effectiveType` | Estimativa, não técnico. Sem Safari/iOS |
| Velocidade da conexão | `navigator.connection.downlink` | Estimativa grosseira (0.1–10 Mbps max reportado) |
| Interface de rede atual (Wi-Fi vs mobile) | Network Information API | Parcial, sem detalhe técnico |
| Geolocalização | `navigator.geolocation` | Menos precisa, requer permissão |
| Notificações push | Push API + Service Worker | Requer permissão, pode ser bloqueado pelo usuário |
| Speedtest real | `fetch` com body grande + timer | Funciona, mas sem controle de protocolo baixo nível |

## Impossível no browser (não implementar)

| Funcionalidade | Por quê |
|---|---|
| Scan de redes Wi-Fi vizinhas | API não exposta — bloqueio de segurança do SO |
| RSSI/RSRP/RSRQ/SINR em tempo real | Acesso ao hardware bloqueado |
| Cell tower info | API exclusivamente nativa (Android TelephonyManager) |
| ARP scan de rede local | Raw sockets bloqueados |
| MAC address de dispositivos | Privacidade — bloqueado desde 2019 |
| mDNS / UPnP discovery | Sem acesso ao multicast local |
| DNS customizado por request | Browser controla o resolver |
| Leitura de logs do sistema | Sandbox do browser |
| Foreground service persistente | Sem suporte sem instalação + Service Worker |

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

## Como comunicar a limitação ao usuário

Quando uma funcionalidade é impossível ou parcial, a UI deve:
1. Explicar a limitação em linguagem simples ("Para ver redes Wi-Fi vizinhas, use o app Android")
2. Não simular dados que não existem
3. Oferecer alternativa quando possível (deeplink para app, instrução manual)
4. Nunca exibir métrica nativa como "Wi-Fi Signal: -72dBm" sem ter essa informação real

## Casos reais documentados

Ver `CASOS-REAIS.md` nesta mesma pasta — 13 casos com thresholds reais do Android, codigo de referencia e o que comunicar ao usuario. Consultar antes de implementar qualquer feature de rede.

Casos cobertos:
1. RSSI e scan Wi-Fi — impossivel
2. Sinal movel (RSRP/RSRQ/SINR) — impossivel
3. Speedtest no browser — limitacoes de precisao e mitigacoes
4. Latencia DNS — tecnica de estimativa via fetch e limitacoes
5. Deteccao de tipo de conexao — Network Info API, suporte por browser, fallbacks
6. Latencia (ping) sem ICMP — como fazer com fetch, o que reportar
7. Thresholds reais de veredicto — todos os valores do Android (dl, ul, jitter, DNS, bufferbloat, RSSI)
8. Background monitoring — impossivel de forma continua, alternativas
9. Deploy Cloudflare Pages — variaveis de ambiente, checklist pre-deploy
10. CORS em diagnostico de modem/IP local — por que falha, que erro aparece
11. IP local — RTCPeerConnection inconsistente, nao implementar
12. Veredicto qualitativo — mapeamento status IA → cor e texto na UI
13. Compartilhar resultado — navigator.share, restricoes e fallback

## Limites
- Esta skill orienta e classifica, nao implementa.
- Implementacao → Renan.
- Qualquer funcionalidade "impossivel no browser" → nao implementar ou documentar limitacao honesta ao usuario.
- Para paridade Android/PWA detalhada → `/paridade-plataformas`.
- Para casos reais com codigo de referencia → `CASOS-REAIS.md`.
