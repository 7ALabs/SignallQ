---
name: paridade-plataformas
description: Compara uma feature entre Android (Kotlin/Compose) e PWA (React/TS), identificando divergências reais vs. limitações técnicas legítimas do navegador.
---

Compara a paridade de uma feature entre Android e PWA. Identifica o que diverge por erro de implementação e o que diverge por limitação real da plataforma.

Feature a comparar:

$ARGUMENTS

## Quando usar
- Ao implementar uma feature nas duas plataformas.
- Ao revisar se o PWA está prometendo algo que o browser não consegue entregar.

## Agentes
- **Renan** — análise e correção PWA.
- **Camilo** — análise e correção Android.

Renan não implementa paridade impossível: documenta a limitação com clareza.

## Projetos
- Android: `android/`
- PWA: `pwa/`
- iOS: `ios/` — ainda não coberto por esta skill. Quando entrar no escopo, replicar o mesmo método tratando o WebKit/Safari iOS e as APIs nativas iOS como uma terceira coluna na tabela delta.

## Passos
1. Usar Glob/Grep para listar os arquivos da feature em Android e PWA.
2. Para cada plataforma, descreva o comportamento atual: tela, lógica relevante, estados.
3. Preencha o checkpoint de limitações do browser (abaixo) com as APIs específicas desta feature.
4. Classifique cada funcionalidade segundo os critérios.
5. Monte a tabela delta.
6. Não implemente antes de apresentar o mapa completo de diferenças.

## Checkpoint pré-análise — limitações do browser
Antes de mapear divergências, liste as APIs Android sem equivalente no browser. Base de referência:

| API Android | Equivalente no browser | Status |
|---|---|---|
| WifiManager / WifiInfo | Nenhum (sem acesso a Wi-Fi no browser) | Impossível |
| ConnectivityManager / NetworkCallback | Network Information API (limitada) | Degradado |
| LinkProperties / DNS servers | Nenhum | Impossível |
| ForegroundService | Service Worker (limitado, sem rede contínua) | Degradado |
| TelephonyManager / cell tower | Nenhum | Impossível |
| Leitura de RSSI em tempo real | Nenhum | Impossível |
| Scan de redes Wi-Fi vizinhas | Nenhum | Impossível |
| Ping nativo (ICMP) | Nenhum (apenas roundtrip HTTP) | Impossível |

Complete a tabela com as APIs específicas da feature comparada.

## Critérios de classificação
Para cada funcionalidade:
- **Paridade total** — mesmo comportamento nas duas plataformas.
- **Paridade parcial / degradada** — funciona, mas com limitação aceitável. Documentar.
- **Impossível no browser** — API nativa sem equivalente web. Documentar com justificativa.
- **PWA prometendo o impossível** — remover ou substituir por alternativa honesta.

## Entregue
1. Como funciona no Android — comportamento, tela e lógica.
2. Como funciona no PWA — comportamento, tela e lógica.
3. Diferenças justificadas por limitação técnica — o que o navegador não permite, com explicação.
4. Diferenças que parecem erro — divergências sem justificativa técnica.
5. O que deve ser ajustado — lista priorizada de correções.
6. Arquivos prováveis — caminhos reais nos dois projetos.
7. Plano de correção — passos para alinhar os dois lados.

## Tabela delta
Ao final, entregue:

| Feature | Android | PWA | Divergência | Tipo |
|---|---|---|---|---|
| [feature] | [impl. Android] | [impl. PWA] | [diferença] | total / parcial / erro / impossível |

## Limites
- Esta skill classifica e recomenda — não implementa.
- Correção Android → Camilo. Correção PWA → Renan.
