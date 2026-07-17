> **Arquivado em 2026-07-16.** Consolidacao de documentacao docs_ai/ (branch docs/consolidacao-documentacao-docs-ai). Conteudo absorvido por: FUNCIONAL.md secao 4.3 (Wi-Fi). Preservado apenas como referencia historica -- nao reflete o estado atual.

---

# WIFI_FEATURES.md — Documentação Funcional: Feature Wi-Fi Android

**Módulo:** `:featureWifi`
**Plataforma:** Android exclusivo
**Atualizado em:** 2026-07-05 (v0.23.0 — versionCode 56; conteúdo técnico de APIs Wi-Fi revalidado)
**Público-alvo:** IA (Claude) — autocontido, sem dependência de sessão anterior

---

## 1. O que é a Feature Wi-Fi

O módulo `:featureWifi` fornece diagnóstico e análise de redes Wi-Fi no SignallQ Android. Seu papel é coletar informações sobre a rede conectada e redes vizinhas para:

- Exibir resumo da conexão atual (banda, canal, sinal, velocidade de link)
- Escanear redes Wi-Fi próximas e exibir lista ordenada por sinal
- Alimentar o engine de diagnóstico de Wi-Fi (`:featureDiagnostico`) com dados de contexto
- Suportar features avançadas em desenvolvimento: identificação de banda ativa, taxa PHY, MIMO e detecção de nó mesh

O módulo NÃO implementa speedtest — isso é responsabilidade de `:featureSpeedtest`.

---

## 2. Arquivos do Módulo

**Localização:** `linkaAndroidKotlin/signallq-android-kotlin/featureWifi/src/main/kotlin/io/signallq/app/kotlin/feature/wifi/`

| Arquivo | Responsabilidade |
|---|---|
| `ResumoWifi.kt` | Data class — resumo da conexão atual (título + detalhe) |
| `MontarResumoWifiUseCase.kt` | Use case — gera ResumoWifi a partir do SnapshotRede |
| `RedeVizinha.kt` | Data class — rede Wi-Fi detectada no scan; calcula banda e canal |
| `ScannerRedesWifi.kt` | Scanner — realiza scan de redes via WifiManager; expõe StateFlow |
| `SnapshotScanWifi.kt` | Data class — estado do scan (idle/scanning/concluido/erro) + lista de redes |
| `FeatureWifiModulo.kt` | Módulo de injeção de dependência |

---

## 3. Contratos de Dados

### ResumoWifi

```kotlin
data class ResumoWifi(
    val titulo: String,   // ex: "WiFi conectado", "Sem conexao"
    val detalhe: String,  // ex: "ssid=MyNet bssid=XX rssi=-58 link=600 freq=5180"
)
```

Gerado por `MontarResumoWifiUseCase.executar(snapshotRede: SnapshotRede)`. Cobre 5 estados de conexão: wifi, movel, ethernet, desconectado, desconhecido.

### RedeVizinha

```kotlin
data class RedeVizinha(
    val ssid: String?,              // pode ser null (SSID oculto)
    val bssid: String,              // MAC do AP
    val rssiDbm: Int,               // sinal em dBm (negativo, ex: -58)
    val frequenciaMhz: Int,         // frequência central em MHz
    val seguranca: SegurancaWifi,   // aberta/wep/wpa/wpa2/wpa3/desconhecida
    val larguraCanalMhz: Int?,      // 20/40/80/160 MHz ou null
) {
    val banda: String   // "2.4GHz" | "5GHz" | "6GHz"
    val canal: Int?     // canal calculado a partir da frequência
}
```

### SnapshotScanWifi

```kotlin
enum class EstadoScanWifi { idle, scanning, concluido, erro }

data class SnapshotScanWifi(
    val estado: EstadoScanWifi,
    val redes: List<RedeVizinha>,   // ordenadas por RSSI desc
    val erroMensagem: String?,      // "semPermissaoLocalizacao" | "erroScanWifi" | null
)
```

---

## 4. Comportamento do Scanner

`ScannerRedesWifi` usa `WifiManager` e expõe `snapshotFlow: StateFlow<SnapshotScanWifi>`.

**Fluxo de scan:**
1. Emite estado `scanning`
2. Registra `BroadcastReceiver` para `SCAN_RESULTS_AVAILABLE_ACTION`
3. Chama `wifiManager.startScan()` (deprecated mas ainda funcional até API 33+)
4. Timeout de 10 segundos — se o receiver não disparar, usa `scanResults` cached
5. Ordena resultados por RSSI descendente (sinal mais forte primeiro)
6. Emite estado `concluido` com lista de `RedeVizinha`
7. Em caso de erro: emite estado `erro` com chave de mensagem

**Extração de SSID por API level:**
- API 33+ (TIRAMISU): usa `ScanResult.wifiSsid.toString()` — string tipada
- API < 33: usa `ScanResult.SSID` (deprecated) — string com aspas que são removidas

**Cálculo de canal a partir de frequência:**
- 2.4GHz: canal 1 = 2412 MHz, incrementos de 5 MHz (canal 14 = 2484 MHz especial)
- 5GHz: canal = (freq - 5000) / 5 para frequências 5160-5885
- 6GHz: canal = (freq - 5955) / 5 + 1 para frequências >= 5925

---

## 5. Telas e Estados Visuais

> Nota: A tela de Wi-Fi no Android (`SinalScreen` / `featureWifi`) não tem Composables no módulo atual. Os Composables de exibição vivem no `:app` ou em módulo de feature de UI. O módulo `:featureWifi` fornece apenas dados — use cases, data classes e scanner.

### Estados esperados na UI (baseados no modelo de dados)

| Estado | O que o usuário vê |
|---|---|
| `idle` | Tela ainda não iniciou o scan |
| `scanning` | Indicador de carregamento, lista vazia ou congelada |
| `concluido` | Lista de redes ordenadas por sinal. Para cada rede: SSID (ou "Rede oculta"), RSSI em dBm, banda (2.4/5/6GHz), canal, segurança, largura de canal |
| `erro — semPermissaoLocalizacao` | Mensagem de permissão negada com ação para abrir configurações |
| `erro — erroScanWifi` | Mensagem de erro genérico com botão de retry |

### Campos exibidos por rede

A feature Wi-Fi na tela principal do SignallQ (ResumoPrincipal) exibe:
- SSID da rede conectada
- RSSI em dBm
- Banda (2.4GHz / 5GHz / 6GHz)
- Velocidade de link (Mbps)
- Frequência central (MHz)

---

## 6. Status por ORB

### ORB-195 — Fundação Wi-Fi (Implementado)

**Status:** Implementação completa. Aguarda confirmação `done` no Paperclip.

**O que entregou:**
- `ScannerRedesWifi` com scan via WifiManager + BroadcastReceiver
- `RedeVizinha` com cálculo de banda e canal
- `MontarResumoWifiUseCase` cobrindo todos os estados de conexão
- `SnapshotScanWifi` com estados idle/scanning/concluido/erro
- Suporte a SSID oculto (null) e extração por API level (< 33 e >= 33)
- Log de scan com contagem por banda 2.4 vs 5GHz

**Arquivos criados:** `ResumoWifi.kt`, `MontarResumoWifiUseCase.kt`, `RedeVizinha.kt`, `ScannerRedesWifi.kt`, `SnapshotScanWifi.kt`, `FeatureWifiModulo.kt`

---

### ORB-202 — Empty State por Banda (Implementado)

**Status:** Implementação completa. Aguarda confirmação `done` no Paperclip.

**O que entregou:**
- Empty state específico quando nenhuma rede da banda selecionada é encontrada
- Comportamento: ao filtrar por banda (2.4GHz ou 5GHz), se a lista ficar vazia, exibe estado vazio com mensagem adequada ("Nenhuma rede 5GHz encontrada nas proximidades")
- Filtro de banda na lista de redes vizinhas

**Depende de:** ORB-195 (fundação)

---

### ORB-203 — PHY Rate Display (Bloqueado)

**Status:** Bloqueado por ORB-195. ORB-195 está concluído — ORB-203 está pronto para implementação quando desbloqueado.

**O que vai entregar:**
- Exibição da taxa PHY (Physical Layer) em Mbps na tela de Wi-Fi
- A taxa PHY representa a velocidade teórica máxima da camada física (ex: 866 Mbps em Wi-Fi AC, 1200 Mbps em Wi-Fi 6)
- API Android: `WifiInfo.getLinkSpeed()` retorna taxa de link em Mbps (proxy da taxa PHY); `WifiInfo.getTxLinkSpeedMbps()` / `getRxLinkSpeedMbps()` disponíveis a partir de API 29
- A taxa PHY diferencia-se da velocidade de internet medida no speedtest — é a velocidade do link local dispositivo ↔ roteador

**Restrição de API:**
- `WifiInfo.getTxLinkSpeedMbps()` e `getRxLinkSpeedMbps()`: API 29+
- Em API < 29: fallback para `WifiInfo.getLinkSpeed()`
- Requer permissão `ACCESS_FINE_LOCATION` (já exigida pelo scan)

---

### ORB-204 — MIMO Label (Bloqueado)

**Status:** Bloqueado por ORB-195. Pronto para implementação quando desbloqueado.

**O que vai entregar:**
- Label indicando configuração MIMO (ex: 2x2 MIMO, 3x3 MIMO)
- MIMO (Multiple Input Multiple Output) indica número de antenas usadas na comunicação
- Estimativa via `WifiInfo.getLinkSpeed()` vs largura de canal e banda — inferência indireta, não exposição direta de API Android
- Alternativa: `ScanResult.numColumns` (não exposto publicamente no SDK até API 33+)
- Label é estimativa, não medição direta — deve ser sinalizado como "aprox." na UI

**Restrição de API:** Não há API pública direta para número de streams MIMO em versões atuais do Android SDK. Implementação requer heurística ou uso de reflexão (não recomendado).

---

### ORB-205 — Mesh Node Identification (Pronto para Implementar)

**Status:** Bloqueador (ORB-195) resolvido. Pronto para implementação.

**O que vai entregar:**
- Identificação de nós mesh na rede doméstica
- Distingue roteador principal de satélites/nós mesh pelo BSSID e RSSI
- Lógica: múltiplos BSSIDs com mesmo SSID indicam rede mesh; o nó com maior RSSI é o mais próximo
- Exibição: lista de nós detectados com indicação "Nó principal" / "Satélite" e qualidade do sinal por nó
- Limitação: redes mesh que usam BSSIDs diferentes por banda (tri-band) podem aparecer como redes separadas

---

## 7. Limitações e Restrições de API Android por Feature

### Restrições gerais — todas as features Wi-Fi

| Permissão | Motivo |
|---|---|
| `ACCESS_FINE_LOCATION` | Obrigatória para qualquer acesso a `WifiInfo`, `ScanResult.SSID` e resultados de scan a partir de API 29 |
| `CHANGE_WIFI_STATE` | Necessária para `startScan()` |

Sem `ACCESS_FINE_LOCATION`: SSID retorna `<unknown ssid>` (API 29+). Scan retorna lista vazia ou lança `SecurityException`. A UI deve tratar o estado `erro — semPermissaoLocalizacao` com ação de redirecionamento para Settings.

### Restrições por API level

| Feature | API mínima | Comportamento em API inferior |
|---|---|---|
| Extração de SSID sem deprecated | API 33 (TIRAMISU) | Usa `ScanResult.SSID` (deprecated, funcional até remoção) |
| `getTxLinkSpeedMbps()` / `getRxLinkSpeedMbps()` | API 29 | Fallback: `getLinkSpeed()` |
| `CHANNEL_WIDTH_160MHZ` | API 30 | Constante não disponível; omitir |
| `ScanResult.wifiSsid` (tipado) | API 33 | Usa string raw `SSID` |
| `startScan()` rate limiting | API 28+ | Android limita a 4 scans por 2 minutos em foreground; em background ainda mais restrito |

### Rate limiting de scan (API 28+)

A partir do Android 9 (API 28), o sistema limita `startScan()`:
- Foreground app: 4 chamadas por 2 minutos por UID
- Background app: 1 chamada por 30 minutos
- Em caso de throttling: `startScan()` retorna `false`. O `ScannerRedesWifi` trata isso usando `scanResults` cached como fallback — o que retorna a última lista de scan disponível (pode ter segundos ou minutos de idade).
- Em devices Samsung: rate limiting pode ser mais agressivo. Em MIUI (Xiaomi): scan em background pode ser bloqueado completamente.

### Wi-Fi 6E / 6GHz (API 30+)

- Banda 6GHz detectada quando `frequenciaMhz >= 5925`
- `RedeVizinha.banda` já retorna "6GHz" para esse range
- Scan de 6GHz requer que o dispositivo tenha hardware Wi-Fi 6E — a maioria dos devices mid-range não tem
- Em devices sem 6GHz: simplesmente não aparecem redes nessa banda; sem erro

---

## 8. Testes Existentes

**Status atual:** Nenhum teste unitário cobre o módulo `:featureWifi`.

**Recomendados para criar:**
- `RedeVizinhaTest` — testar `banda` e `canal` calculados para frequências de borda (2412, 2484, 5180, 5925, 6000+)
- `MontarResumoWifiUseCaseTest` — testar os 5 estados de conexão
- `ScannerRedesWifiTest` — testar parsing de `ScanResult` para `RedeVizinha` (extração de SSID, segurança, largura de canal)

---

## 9. Referências

- ORB-195: `linkaAndroidKotlin/docs_ai/operations/.old/` (task encerrada)
- Diagnóstico de canal Wi-Fi: `WifiChannelDiagnosticEngineTest.kt` em `:featureDiagnostico`
- SnapshotRede e EstadoConexao: `:coreNetwork`
- Permissões: `:corePermissions`
