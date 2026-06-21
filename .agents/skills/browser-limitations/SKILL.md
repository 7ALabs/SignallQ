---
description: Referência rápida das limitações do browser para o PWA Linka — o que é impossível, o que é parcial, e como comunicar honestamente ao usuário.
---

## Quando usar
Ao avaliar se uma feature proposta para o PWA é tecnicamente viável no browser.

## Impossível no browser (não implementar)

| Funcionalidade | Por quê |
|---|---|
| Scan de redes Wi-Fi vizinhas | API não exposta — bloqueio de segurança do SO |
| RSSI/RSRP/RSRQ/SINR em tempo real | Hardware sem acesso via browser |
| Cell tower info | API exclusivamente nativa (Android TelephonyManager) |
| ARP scan de rede local | Raw sockets bloqueados |
| MAC address de dispositivos | Privacidade — bloqueado desde 2019 |
| mDNS / UPnP discovery | Sem acesso ao multicast local |
| DNS customizado por request | Browser controla o resolver |
| Leitura de logs do sistema | Sandbox do browser |
| Foreground service persistente | Sem suporte sem instalação + Service Worker |

## Parcialmente possível (implementar com ressalva)

| Funcionalidade | O que funciona | Limitação |
|---|---|---|
| Tipo de conexão | `navigator.connection.effectiveType` | Estimativa, não técnico. Sem Safari/iOS |
| Velocidade da conexão | `navigator.connection.downlink` | Estimativa grosseira (0.1–10 Mbps max reportado) |
| Geolocalização | `navigator.geolocation` | Menos precisa, requer permissão |
| Notificações push | Push API + Service Worker | Requer permissão, pode ser bloqueado por usuário |
| Speedtest real | `fetch` com body grande + timer | Funciona, mas sem controle de protocolo baixo nível |

## Possível sem restrição

- Latência (ping) via fetch timing
- Download speed via fetch com blob
- Upload speed via fetch POST
- Histórico local via IndexedDB
- Offline básico via Service Worker cache

## Como comunicar ao usuário

Quando uma funcionalidade é limitada, a UI deve:
1. Explicar a limitação em linguagem simples ("Para ver redes Wi-Fi vizinhas, use o app Android")
2. Não simular dados que não existem
3. Oferecer alternativa quando possível (deeplink para app, instrução manual)
4. Nunca exibir métricas nativas como "Wi-Fi Signal: -72dBm" sem ter essa informação real

## Limites
- Esta skill não implementa — apenas classifica.
- Implementação → Renan.
- Para paridade Android/PWA detalhada → `/android-pwa-parity`.
