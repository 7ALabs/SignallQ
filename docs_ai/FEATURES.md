# Linka Android — Features Implementadas

> Gerado automaticamente. Atualizar sempre que uma feature for adicionada, alterada ou removida.
> Versão de referência: **v0.7.3** (versionCode 18) — build 19/05/2026

---

## Features de Tela (módulos `:feature*`)

| FEATURE | DESCRIÇÃO | STATUS | VERSÃO |
|---|---|---|---|
| **Onboarding** | Tela de primeira execução com 3 slides (produto, privacidade, indicadores de qualidade), dots animados, HorizontalPager, swipe e controle por DataStore (`onboarding_concluido`) | Implementado | v0.7.2 |
| **Home** | Tela inicial com resumo de conexão, sinal Wi-Fi, velocidade, diagnóstico rápido, gateway info e monitoramento passivo | Implementado | v0.6.2 |
| **Wi-Fi** | Scan de redes, análise de canais (2.4/5/6 GHz), congestionamento de espectro, PHY (802.11a/b/g/n/ac/ax/be), RSSI, detecção de tipo de gateway (mesh/roteador/extensor), MU-MIMO; exibição adaptativa por tipo de conexão (Wi-Fi/Móvel/Cabo/Desconhecido), sinal móvel RSRP/RSRQ/SINR | Implementado | v0.7.3 |
| **Dispositivos** | Scan de dispositivos na rede (mDNS/ARP), classificação por tipo, base OUI com +50 fabricantes (Nokia, ZTE, Sagemcom, Ubiquiti, Dell, HP…) | Implementado | v0.6.2 |
| **Speedtest** | Medição de download/upload/latência/jitter, fases de execução (aquecimento, principal, resfriamento), classificação de qualidade, suporte Cloudflare, detecção de bufferbloat, taxa de qualidade por uso (gaming/streaming/videochamada); compartilhamento de resultado como PNG via share sheet | Implementado | v0.7.3 |
| **DNS** | Benchmark de resolvedores DNS e DoH, análise de coerência (private DNS), resolvedores customizados, avaliador de qualidade com grade A–D | Implementado | v0.6.2 |
| **Fibra** | Diagnóstico de modems GPON (Nokia, Intelbras, TP-Link), extração de dados (RX dBm, SNR, status WAN/PPP), presets com credenciais pré-preenchidas, suporte a criptografia de modem | Implementado | v0.7.0 |
| **Diagnóstico IA** | Motor de diagnóstico multi-camada (Wi-Fi, internet, mobile, fibra, DNS, histórico), geração de recomendações, pergunta-resposta dinâmica (Pulse/Orbit), contexto acumulado de sessão; card de contato da operadora quando diagnóstico aponta ISP | Implementado | v0.7.3 |
| **Histórico** | Histórico de medições (Room), cálculo de uptime, narrativa de degradação, exportação CSV/PDF, gráfico de uptime, detector de tendências (melhora/piora/estável) | Implementado | v0.6.2 |
| **Ajustes** | Configurações do app, seletor de ISP (18 operadoras brasileiras), ajustes de monitoramento passivo, notificações granulares por tipo, perfil do usuário | Implementado | v0.6.2 |

---

## Módulos Core (infraestrutura sem tela)

| MÓDULO | DESCRIÇÃO | STATUS | VERSÃO |
|---|---|---|---|
| **coreNetwork** | Monitor de estado de rede (ConnectivityManager/NetworkCallback), medição de latência do gateway (ICMP), snapshot de Wi-Fi/celular, estado de conexão em tempo real | Implementado | v0.6.2 |
| **coreDatabase** | Room database, DAOs para histórico de medições e apelidos de dispositivos, migrations | Implementado | v0.6.2 |
| **coreDatastore** | DataStore Preferences — persistência de configurações do app (tema, monitoramento, modem, onboarding, ISP, alertas) | Implementado | v0.6.2 |
| **corePermissions** | Requisição e validação de permissões em runtime (ACCESS_FINE_LOCATION, NEARBY_WIFI_DEVICES, POST_NOTIFICATIONS, CHANGE_NETWORK_STATE) | Implementado | v0.6.2 |
| **coreTelephony** | Monitor de sinal de rede móvel (TelephonyManager), coleta de RSRP/RSRQ/SINR, operadora, tecnologia (4G/5G), banda — ativado apenas quando connectionType = mobile | Implementado | v0.6.2 |

---

## Engines de Diagnóstico (dentro de `:featureDiagnostico`)

| ENGINE | DESCRIÇÃO | STATUS |
|---|---|---|
| `WifiSignalQualityEngine` | Análise de sinal Wi-Fi: RSSI, PHY, capacidade estimada | Implementado |
| `InternetDiagnosticEngine` | Speedtest, latência, qualidade de uso por perfil | Implementado |
| `MobileSignalDiagnosticEngine` | Sinal 4G/5G: RSRP, RSRQ, SINR, operadora | Implementado |
| `FibraSignalQualityEngine` | GPON: RX power dBm, SNR, status WAN/PPP | Implementado |
| `DnsDiagnosticEngine` | Validação de DNS, coerência com private DNS | Implementado |
| `WifiChannelDiagnosticEngine` | Análise de canal Wi-Fi, congestionamento, interferência | Implementado |
| `HistoricalDegradationEngine` | Tendências de uptime e degradação ao longo do tempo | Implementado |

---

## Funcionalidades Transversais

| FUNCIONALIDADE | DESCRIÇÃO | VERSÃO |
|---|---|---|
| **Monitoramento Passivo** | WorkManager 30 min — mede latência HTTP, DNS, RSSI Wi-Fi em background | v0.7.0 |
| **Notificações Inteligentes** | 4 tipos de alerta (latência alta, DNS lento, sinal fraco, sem internet) com cooldown por tipo e controles granulares do usuário | v0.7.0 |
| **Detecção de Tipo de Gateway** | Heurística dupla (SSID + BSSID) para identificar mesh, extensor ou roteador | v0.7.0 |
| **ISP Selector** | 18 operadoras brasileiras pré-definidas + opção "Outra/ISP Local" customizável | v0.7.1 |
| **OUI Database** | Base de fabricantes com +50 entradas (Nokia, ZTE, Sagemcom, Ubiquiti, Tenda, Dell, HP…) | v0.6.2 |
| **Exportação CSV/PDF** | Histórico exportável em dois formatos | v0.6.2 |
| **Presets Fibra** | 3 botões de acesso rápido (Nokia/ONT, Intelbras, TP-Link) com credenciais pré-preenchidas | v0.7.0 |
| **Banco de Operadoras** | 16 ISPs brasileiros com SAC e WhatsApp (Vivo, Claro, TIM, Oi, Sky, Algar, Brisanet, Ligga, Sumicity, Desktop/Nextel, Unifique, Mob, Giga+, Eletronet, Vogel, Tely), detecção por substring matching, fallback Anatel (1331) | v0.7.3 |

---

## Notas de Atualização

| DATA | VERSÃO | O QUE MUDOU |
|---|---|---|
| 19/05/2026 | v0.7.3 | SinalScreen adaptativa por tipo de conexão (Wi-Fi/Móvel/Cabo/Desconhecido) com RSRP/RSRQ/SINR em móvel; compartilhamento de resultado speedtest como PNG; banco de operadoras brasileiras com SAC e WhatsApp |
| 18/05/2026 | v0.7.2 | Onboarding implementado conforme spec: 3 slides, novo conteúdo, dots animados (pill 8→22dp), assinatura unificada `onConcluir`, acessibilidade completa |
| 16/05/2026 | v0.7.1 | ISP Selector com 18 operadoras brasileiras |
| — | v0.7.0 | Fibra GPON, monitoramento passivo, notificações inteligentes, detecção de tipo de gateway, presets de modem |
| — | v0.6.2 | Baseline da maioria das features de tela e engines de diagnóstico |

---

*Para atualizar: edite a tabela correspondente e adicione uma linha em "Notas de Atualização" com data, versão e descrição da mudança.*
