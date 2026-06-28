# Changelog — SignallQ Android

Todas as mudanças notáveis do app Android serão documentadas aqui.

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/).

> Histórico de marca: Linka (até 0.14.x) → Veloo (0.15.0) → **SignallQ** (0.16.0+).
> Identificadores técnicos (`io.signallq.app`, repo `linka-android`) permanecem por compatibilidade.

---

## [Unreleased]

### Adicionado
- Preparação para v1.0.0 — beta fechado via Play Store

---

## [0.21.0] — 2026-06-22

### Adicionado
- Novo ícone do app em todas as densidades com ícone adaptativo (SIG-7/SIG-8)
- Worker de privacidade para conformidade de dados pessoais
- CI com Ktlint, Detekt, testes unitários e build APK em cada PR (SIG-28/SIG-37)

### Corrigido
- Painel admin: erro de telemetria no Overview agora exibe mensagem clara
- Painel admin: dados de teste vazando para produção removidos (SIG-9)

### Alterado
- Formatação Ktlint corrigida em ~30 arquivos
- Toolchain: AGP 9.1.1, Gradle 9.3.1, compileSdk 37, Kotlin 2.2.20

---

## [0.20.0] — 2026-06-18

### Adicionado
- Painel administrativo SignallQ (React/TS) com login, métricas e gestão
- Ingest de diagnósticos no D1 via worker Cloudflare
- Integração Firebase Analytics com GA4
- Multi-provider de IA (Qwen3 30B + fallback Gemini Flash)
- Laudo de diagnóstico como resultado primário, IA como explicador
- HiltWorkerFactory para injeção de dependência nos Workers

### Alterado
- Diagnóstico reformulado: laudo estruturado em vez de chat livre

---

## [0.19.0] — 2026-06-15

### Alterado
- Refatoração completa da arquitetura: decomposição de telas em composables independentes
- ViewModels extraídos por funcionalidade (Dispositivos, Speedtest, Diagnóstico)
- Módulos de feature sem dependências cruzadas
- Orquestrador de diagnóstico movido para `featureDiagnostico`
- Cache de IA com expiração (TTL 5 min)
- OkHttpClient singleton para UPnP e scan de dispositivos
- Overlay de diagnóstico antigo removido

---

## [0.16.0] — 2026-06-01

### Alterado — Rebrand para SignallQ
- Toda a UI, copy e documentação renomeados de Linka/Veloo/Orbit para **SignallQ**
- Assistente de IA passa a se chamar SignallQ (não mais "Orbit")
- Identificadores técnicos preservados por compatibilidade de infraestrutura

### Adicionado
- Tela de dispositivos: nomes via jmDNS + SSDP, scan concorrente, detecção por MAC

---

## [0.15.1] — 2026-06-12

### Corrigido
- Nó "Roteador" na trilha de rede mesh exibia dado vazio em vez do placeholder "—"

---

## [0.15.0] — 2026-05-30

### Alterado — Rebrand Linka → Veloo
- Identidade visual, package name e configurações Firebase atualizados para Veloo
- Tela de novidades v0.15.0

---

## [0.14.0] — 2026-05-29

### Adicionado
- Novo fluxo de diagnóstico por laudo (setup → análise → resultado) em vez de chat
- Assistente de IA em tela standalone com chat livre sobre conexão
- 8 componentes UI novos para o laudo (veredito, causa-raiz, impacto, recomendações, métricas)
- Seleção de sinais para análise direcionada (velocidade, Wi-Fi, latência, fibra, DNS)

### Alterado
- Botão "Diagnóstico IA" na Home agora abre o fluxo de laudo
- Chat inline removido da tela de diagnóstico

---

## [0.13.3] — 2026-05-28

### Corrigido
- Fibra: abertura automática da tela ao iniciar o app removida
- IP público do Wi-Fi não aparece mais na sheet de rede móvel
- Detecção de 5G NSA corrigida com fallbacks adicionais

---

## [0.13.2] — 2026-05-28

### Corrigido
- Nome da operadora agora vem do snapshot móvel, não do ISP externo
- Card SIM duplicado removido em dispositivos single-SIM
- SSID Wi-Fi nulo no Android 12+ corrigido
- Filtro de banda 2.4GHz exibe banner correto quando conectado em 5GHz

### Alterado
- SimCard redesenhado com operadora em destaque e badge "EM USO"
- Cards de contexto removidos da tela de velocidade

---

## [0.13.0] — 2026-05-28

### Alterado — Mockup v2 UI
- Home: ordem fixa dos cards, sinal com qualidade em palavra (Forte/Regular/Fraco), chips de SIM compactos
- Sinal: 3 abas fixas (Wi-Fi, Canal, Móvel) sempre visíveis
- Velocidade: subtítulo com plano contratado, seletor de modo neutro, métricas em 3 colunas
- Resultado: nota em círculo removida, avaliação em texto direto

---

## [0.12.0] — 2026-05-28

### Adicionado
- Chat de diagnóstico IA com estilo conversa moderna (Material 3)
- 3 opções rápidas: analisar último teste, executar novo teste, analisar histórico
- Streaming SSE de respostas da IA com auto-scroll
- Persistência de sessões de chat via Room (criar, apagar, renomear)
- Cota diária de IA com janela rolling de 24h e banner de renovação
- Mensagens humanizadas para 5 cenários de erro
- TopBar contextual com SSID/operadora na Home
- Card "Sua Conexão" destacado na tela Sinal
- Mini-cards DNS, Ping e Diagnóstico na Home
- Seletor Android/Roteador na aba Canal
- Chip de segurança Wi-Fi (WPA3/WPA2/WPA/Open)
- Card de rede móvel com suporte a dual SIM

### Corrigido
- Cards de informação movidos da Home para a tela de Resultado
- Operadora nula em medições via chat corrigida

---

## [0.11.4] — 2026-05-27

### Corrigido
- DNS do provedor não reconhecia Registro.br e CETIC.br no comparativo
- Filtro de latência excluía DNS rápidos de operadora em redes 5G/4G

---

## [0.11.3] — 2026-05-27

### Corrigido
- Trilha de rede mostrava "sem internet" enquanto buscava IP público

---

## [0.11.2] — 2026-05-27

### Corrigido
- Crash na abertura do app (NullPointerException) por ordem de inicialização

---

## [0.11.1] — 2026-05-27

### Corrigido
- Detecção de 5G NSA em Samsung Exynos e Xiaomi MIUI 14+
- Histórico vazio ao filtrar por rede móvel
- Botão voltar fechava o app na aba Histórico
- Trilha de rede intermitente durante handoff Wi-Fi → Móvel

---

## [0.11.0] — 2026-05-26

### Adicionado
- Tela Fibra/Modem com análise de RX/TX, temperatura e status óptico
- Tela DNS com benchmark de 7 provedores, recomendação e guia de configuração
- Onboarding com aceite obrigatório de termos e cards de permissão
- Mensagens humanizadas para estados vazios

### Corrigido
- MAC address parcialmente mascarado na tela de dispositivos (privacidade)

---

## [0.10.0] — 2026-05-26

### Adicionado
- Chat inline com IA na tela de diagnóstico (Gemma 4 26B via Cloudflare Workers)
- Histórico com gráfico de testes e cards de velocidade média
- Diagnóstico redesenhado com 5 cards (status, problema, ações, evidências, chat)
- Classificação automática de topologia Wi-Fi (roteador, mesh, repetidor)
- Streaming SSE no chat com cursor pulsante em tempo real
- Detecção de padrões de uptime (horários recorrentes, interrupções longas, tendência)
- Exportação de histórico em PDF com paginação automática

---

## [0.9.0] — 2026-05-20

### Adicionado
- Central de Testes com grid de ferramentas (DNS, Ping, Diagnóstico)
- Ping/Latência: 20 amostras ICMP com mediana, jitter e perda de pacotes
- DNS Benchmark: Registro.br e CETIC.br adicionados (7 provedores total)
- Proteção de dados móveis antes de testes pesados
- Preferência para sempre permitir testes em dados móveis
- Consumo mensal de dados em testes

### Alterado
- Separador visual com ponto médio no DNS Benchmark
- Ferramentas movidas de bottom sheet para grid na tela de Velocidade

---

## [0.8.4] — 2026-05-19

### Corrigido
- App mostrava "4G" em redes 5G NSA por falha na detecção por OEM

---

## [0.8.3] — 2026-05-19

### Alterado
- Sinal móvel redesenhado com gauge de RSRP, textos humanizados e diagnóstico
- Canal Wi-Fi com destaque e recomendação em linguagem natural
- Monitoramento de uptime com lista de eventos por dia
- Nó mesh na trilha de rede com ícone Hub

### Corrigido
- "Diagnóstico Inteligente" oculto em release via feature flag
- Label de internet duplicada em conexão móvel
- Nó móvel sem IP com tecnologia como sublabel
- Operadora correta no perfil em conexão móvel
- Item duplicado "Dados usados" removido dos Ajustes

---

## [0.8.2] — 2026-05-19

### Corrigido
- Navbar fixa (animação de scroll-hide causava espaço vazio)
- Trilha de rede proporcional à tela
- "Modo Gamer" renomeado para "Jogar Online"

### Alterado
- Avatar do perfil exibe dados de conexão em vez de nome do aparelho
- Gráfico de monitoramento reorientado para leitura horizontal

---

## [0.8.1] — 2026-05-19

### Corrigido
- 10 correções visuais/UX (modem dinâmico, títulos, cores, labels, acessibilidade)
- Classificação de sinal Wi-Fi distingue 2.4GHz de 5GHz
- Tipo de conexão real enviado para IA (era Wi-Fi fixo)
- Limiar de RX Power alinhado ao padrão ITU-T G.984

---

## [0.8.0] — 2026-05-18

### Adicionado
- Tela Fibra/Modem visível em produção
- DNS Benchmark visível em produção
- Chat com IA disponível apenas em debug

### Corrigido
- Botão "Testar Novamente" duplicado removido
- Overlays protegidos por feature flags

---

Versões anteriores a 0.8.0 não possuem registro neste arquivo.
