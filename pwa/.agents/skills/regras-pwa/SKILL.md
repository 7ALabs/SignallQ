---
name: regras-pwa
description: Use para qualquer decisão ou implementação do SignallQ PWA que dependa de APIs de navegador, limites web, métricas de rede, instalação PWA, service worker, histórico local ou diagnóstico de conectividade no browser. Não use para Android nativo.
---

# Regras PWA — SignallQ

## Objetivo

Garantir que o SignallQ PWA implemente apenas o que navegador permite, sem prometer capacidade nativa Android.

## Pode ser feito no browser

- Download via HTTP.
- Upload via HTTP quando houver endpoint adequado.
- Latência aproximada via `fetch` ou Performance API.
- Jitter aproximado a partir de múltiplas amostras.
- Histórico local com IndexedDB ou localStorage.
- Service Worker e cache.
- Manifest PWA e instalação quando suportado.
- Estado online/offline.
- Tipo de conexão via Network Information API quando disponível.
- Informações básicas de navegador/dispositivo sem invasão.

## Parcial ou degradado

- Tipo de conexão pode não existir em Safari ou browsers específicos.
- Latência via HTTP não é ping ICMP real.
- Perda de pacote não é medição real; pode ser inferência limitada.
- Upload depende de endpoint próprio ou estratégia controlada.
- Background sync é limitado e inconsistente entre browsers.

## Não prometer no PWA

- Scan de Wi-Fi.
- RSSI, RSRP, RSRQ, SINR.
- MAC address.
- ARP scan.
- Dispositivos conectados na rede.
- Cell tower ID.
- ICMP ping real.
- DNS custom nativo por request.
- Logs de sistema.
- Foreground service persistente.

## Regra de diagnóstico

Se o navegador não mediu, o app deve informar que não mediu.

Nunca invente valor técnico para preencher tela.

## Antes de implementar

Responda:

- A API existe no browser?
- Funciona em Chrome mobile?
- Funciona em Safari/iOS?
- Precisa de permissão?
- Existe fallback honesto?
- A informação pode ser explicada para usuário comum?

## Saída esperada

Ao usar esta skill, entregue:

- classificação: possível, degradado ou impossível;
- impacto na experiência;
- recomendação de implementação;
- texto simples para comunicar limitação ao usuário, se necessário.
