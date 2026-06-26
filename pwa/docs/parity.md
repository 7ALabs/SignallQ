# Paridade Android ↔ PWA

## Objetivo

Definir como o SignallQ PWA deve tratar paridade com o Android.

Este documento deriva do contrato técnico existente em:

`docs_ai/technical/paridade-plataformas.md`

## Regra principal

Paridade não significa copiar tudo do Android.

Paridade no PWA significa entregar a mesma promessa central quando o navegador permitir, e declarar degradação ou impossibilidade quando o browser não permitir.

**Exceção importante:** SpeedTest é promessa central do produto e deve buscar paridade metodológica com o Android. Não tratar SpeedTest como versão inferior por padrão.

## Status de paridade

Usar estas classificações:

| Status | Significado |
|---|---|
| `equivalente` | O PWA entrega comportamento funcional comparável ao Android. |
| `paridade-metodologica` | O PWA usa o mesmo endpoint, amostragem, cálculo e classificação do Android quando a plataforma permite. |
| `degradado` | O PWA entrega parte do valor, com limitação conhecida. |
| `ausente` | Ainda não implementado no PWA. |
| `n/a-browser` | Impossível no browser por limitação técnica. |
| `n/a-design` | Faz sentido apenas em app nativo ou foi omitido por decisão de produto. |

## Mapa crítico

### Implementável no PWA

- App shell.
- Home simples.
- SpeedTest por HTTP com paridade metodológica.
- Latência HTTP.
- Jitter por amostras HTTP.
- Histórico local com IndexedDB.
- Exportação futura por Blob/arquivo.
- Compartilhamento via Web Share API quando disponível.
- Tema claro/escuro.
- Onboarding local.
- Diagnóstico local.
- Diagnóstico IA via Worker.

### Degradado no PWA

- Tipo de conexão: depende de Network Information API, ausente em Safari/Firefox.
- Ping ICMP real: indisponível, mas SpeedTest deve usar a mesma latência HTTP/endpoint de referência definido para o produto.
- Upload: depende de endpoint controlado, que deve ser o mesmo contrato usado pelo Android sempre que possível.
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

Status alvo: `paridade-metodologica`.

O PWA deve buscar precisão equivalente ao Android para as métricas que fazem parte do SpeedTest:

- download;
- upload;
- latência HTTP;
- jitter;
- bufferbloat, se entrar no modo completo;
- classificação qualitativa.

Regras:

- usar o mesmo endpoint/worker de medição do Android quando possível;
- usar o mesmo tamanho de payload ou uma justificativa documentada;
- usar a mesma regra de amostragem ou uma justificativa documentada;
- usar a mesma fórmula de cálculo de Mbps;
- usar a mesma classificação qualitativa;
- registrar limitação apenas quando a diferença for de plataforma, não por simplificação arbitrária.

A única ressalva obrigatória é terminológica: browser não faz ICMP real. Se a métrica for latência HTTP, a UI deve chamar de latência, não de ping ICMP.

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

SpeedTest só pode ser aceito quando houver paridade metodológica documentada ou justificativa técnica explícita para divergência.