# Paridade Android ↔ PWA

## Objetivo

Definir como o SignallQ PWA deve tratar paridade com o Android.

Este documento deriva do contrato técnico existente em:

`docs_ai/technical/paridade-plataformas.md`

## Regra principal

Paridade não significa copiar tudo do Android.

Paridade no PWA significa entregar a mesma promessa central quando o navegador permitir, e declarar degradação ou impossibilidade quando o browser não permitir.

## Status de paridade

Usar estas classificações:

| Status | Significado |
|---|---|
| `equivalente` | O PWA entrega comportamento funcional comparável ao Android. |
| `degradado` | O PWA entrega parte do valor, com limitação conhecida. |
| `ausente` | Ainda não implementado no PWA. |
| `n/a-browser` | Impossível no browser por limitação técnica. |
| `n/a-design` | Faz sentido apenas em app nativo ou foi omitido por decisão de produto. |

## Mapa crítico

### Implementável no PWA

- App shell.
- Home simples.
- SpeedTest por HTTP.
- Latência HTTP.
- Jitter estimado por amostras HTTP.
- Histórico local com IndexedDB.
- Exportação futura por Blob/arquivo.
- Compartilhamento via Web Share API quando disponível.
- Tema claro/escuro.
- Onboarding local.
- Diagnóstico local.
- Diagnóstico IA via Worker.

### Degradado no PWA

- Tipo de conexão: depende de Network Information API, ausente em Safari/Firefox.
- Ping: apenas latência HTTP, não ICMP.
- Upload: depende de endpoint controlado.
- Bufferbloat: possível com múltiplas requisições, mas deve ser tratado como métrica avançada futura.
- Notificações: Web Push é possível, mas sem RSSI real e com suporte desigual.
- Monitoramento passivo: apenas com app aberto ou Periodic Background Sync onde disponível.

### Permanente `n/a-browser`

- RSSI Wi-Fi.
- Scan de redes Wi-Fi.
- Canal/frequência Wi-Fi.
- Análise real de interferência Wi-Fi.
- Sinal móvel RSRP/RSRQ/SINR.
- Dados de SIM/cell ID.
- ARP scan.
- mDNS/SSDP scan direto.
- MAC/BSSID confiável.
- Diagnóstico direto de modem local/fibra.
- GPON/TR-064 direto pelo browser.
- DNS benchmark real sem proxy dedicado.
- ICMP ping real.
- Monitoramento contínuo em background como WorkManager.

## Decisões por área

### SpeedTest

Status alvo: `equivalente-degradado`.

O PWA pode medir download, upload e latência por HTTP. A UI deve comunicar que latência é HTTP e não ICMP.

### DNS

Status alvo inicial: `n/a-browser` para benchmark real.

Pode existir no futuro como checagem indireta ou via Worker/proxy dedicado, mas não deve ser prometido como DNS benchmark real no MVP.

### Sinal

Status alvo inicial: `degradado`.

O PWA pode mostrar conectividade geral e talvez tipo estimado de conexão, mas não pode mostrar RSSI, scan, canal, frequência ou sinal móvel real.

### Dispositivos

Status alvo: `n/a-browser`.

Scan de dispositivos por ARP/mDNS/SSDP direto no browser é inviável.

### Fibra/modem

Status alvo: `n/a-browser` para PWA público.

Acesso direto a modem local tem bloqueios de CORS, mixed content e rede privada. Só seria viável com proxy, extensão ou ambiente controlado.

### Histórico

Status alvo: `equivalente-degradado`.

PWA usa IndexedDB/local. Não há Room, sincronização nativa ou migração Android.

### IA

Status alvo: `equivalente` se o Worker aceitar payload web e retornar contrato compatível.

Falha de IA deve usar fallback local.

## Regra de manutenção

Quando o Android adicionar feature nova relevante para PWA, revisar este documento.

Quando o PWA implementar ou rejeitar feature por limitação do browser, atualizar este documento no mesmo PR.

## Critério de aceite

Nenhuma feature nativa pode aparecer como promessa do PWA sem classificação explícita.

Se for impossível no browser, deve ser marcada como `n/a-browser` e refletida na UI ou no escopo.