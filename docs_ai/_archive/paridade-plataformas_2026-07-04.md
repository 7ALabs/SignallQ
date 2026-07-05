# Paridade Android / PWA — SignallQ

**Última atualização:** 2026-06-24 (Android v0.21.0 · PWA: não inicializado)
**Fonte Android:** `AppShell.kt`, telas em `app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/`
**Fonte PWA:** `pwa/` — projeto em estado de setup (sem código de feature ainda)

---

## Status geral do PWA

O PWA SignallQ ainda não foi inicializado com código de feature. Existe apenas a estrutura de repositório (`pwa/CLAUDE.md`, `pwa/README.md`). Stack definida: React + TypeScript + Vite + Tailwind + Cloudflare Pages.

Todas as features abaixo têm status **ausente** no PWA por omissão. O propósito deste documento é servir de contrato para quando a implementação começar.

---

## Legenda de status

| Status | Significado |
|---|---|
| implementado | Feature completa no PWA com paridade funcional |
| parcial | Feature existe no PWA mas com escopo reduzido ou degradação conhecida |
| ausente | Feature existe no Android, ainda não implementada no PWA |
| n/a-browser | Feature impossível no browser por limitação de plataforma — não será implementada |
| n/a-design | Feature faz sentido apenas em app nativo (ex.: haptic feedback) — omissão intencional |

---

## Navegacao e Shell

### Bottom Navigation (5 abas)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Tab Inicio | implementado | ausente | |
| Tab Velocidade | implementado | ausente | |
| Tab Sinal | implementado | ausente | |
| Tab Historico | implementado | ausente | |
| Tab Ajustes | implementado | ausente | |
| Badge pulsante na aba Velocidade durante teste | implementado | ausente | Requer estado global de teste ativo |
| Ocultar navbar durante teste em execucao | implementado | ausente | |
| Back handler: Historico volta para Home | implementado | ausente | No PWA equivale a navegacao de rota |

---

## Tela: Inicio (HomeScreen)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Status da conexao em tempo real (Wi-Fi / movel / offline) | implementado | ausente | `navigator.onLine` + Network Info API (degradado: sem RSSI) |
| IP local e IP publico | implementado | ausente | IP publico: fetch em worker externo. IP local: impossivel no browser — ver nota abaixo |
| Info de ISP / operadora | implementado | ausente | Obter via API de geolocalização de IP publico |
| Snapshot de velocidade mais recente | implementado | ausente | Depende de Historico implementado |
| Atalho para Dispositivos | implementado | ausente | Depende de Dispositivos |
| Atalho para Sinal (redes Wi-Fi) | implementado | ausente | |
| Atalho para DNS | implementado | ausente | |
| Atalho para Ping | implementado | ausente | |
| Atalho para Diagnostico IA | implementado | ausente | |
| Banner Anatel dismissivel | implementado | ausente | |
| Perfil do usuario (avatar, nome) | implementado | ausente | Foto de perfil: File API disponivel no browser |
| Onboarding (primeira execucao) | implementado | ausente | Usar localStorage como flag |

**Limitacao de browser — IP local:** `RTCPeerConnection` pode vazar IP local em algumas situacoes, mas o resultado e inconsistente entre browsers e bloqueado por politica de privacidade em Firefox e Safari. Nao implementar: exibir "N/A" ou omitir campo.

---

## Tela: Velocidade (SpeedTestScreen + VelocidadeScreen)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Teste de download | implementado | ausente | Fetch de arquivo grande via Cloudflare, medir throughput |
| Teste de upload | implementado | ausente | POST de payload via Fetch API |
| Ping / latencia | implementado | ausente | Fetch HEAD para servidor de referencia; sem ICMP no browser |
| Bufferbloat (modo completo) | implementado | ausente | Possivel via multiplas requisicoes paralelas |
| Modo completo vs. modo rapido | implementado | ausente | |
| Grafico de velocidade ao vivo (barras) | implementado | ausente | |
| Localizacao do servidor | implementado | ausente | Header de resposta do worker Cloudflare |
| Aviso de uso de dados moveis | implementado | ausente | Network Info API (`connection.type`): suporte parcial (Chrome/Android), ausente em Safari/Firefox |
| Contador de MB consumidos no mes | implementado | ausente | Requer persistencia local (localStorage / IndexedDB) |
| Resultado automatico apos teste | implementado | ausente | |
| Compartilhar resultado (screenshot) | implementado | ausente | `navigator.share` disponivel no browser — implementavel |
| Haptic feedback ao concluir | implementado | n/a-browser | `navigator.vibrate` existe mas suporte inconsistente; nao e parte da experiencia central |

**Limitacao de browser — ICMP:** o browser nao tem acesso a sockets ICMP. Ping e medido via latencia HTTP (RTT de requisicao Fetch), nao ICMP real. Divergencia aceitavel — comportamento deve ser documentado na UI do PWA.

---

## Tela: Sinal (SinalScreen)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| RSSI / nivel do sinal Wi-Fi | implementado | n/a-browser | Nenhuma API web expoe RSSI. Exibir indicador de conectividade sem nivel numerico |
| Lista de redes Wi-Fi visiveis (scan) | implementado | n/a-browser | `navigator.wifi` nao existe. Wi-Fi scan impossivel no browser por seguranca |
| Canal Wi-Fi e frequencia (2.4/5/6 GHz) | implementado | n/a-browser | Depende de scan Wi-Fi — impossivel no browser |
| Analise de canais e interferencia | implementado | n/a-browser | Depende de scan Wi-Fi |
| Topologia de rede (tab Wi-Fi) | implementado | n/a-browser | Requer ARP/mDNS — indisponivel no browser |
| Sinal movel (RSRP, RSRQ, band, operadora) | implementado | n/a-browser | API de telefonia indisponivel no browser |
| Informacoes de SIM (simsAtivos) | implementado | n/a-browser | |
| Permissao de localizacao para scan Wi-Fi | implementado | n/a-browser | Sem scan, permissao de localizacao nao e necessaria para esta feature |
| Permissao de telefonia | implementado | n/a-browser | |

**Nota para PWA:** a aba Sinal pode existir no PWA com escopo drasticamente reduzido — exibir tipo de conexao (Wi-Fi / celular / offline) via Network Info API e IP publico detectado. Todo o conteudo nativo (RSSI, scan, canal, sinal movel) nao e implementavel.

---

## Tela: Historico (HistoricoScreen)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Lista de medicoes persistidas | implementado | ausente | No PWA: IndexedDB ou localStorage |
| Filtro por tipo de conexao (Wi-Fi / movel / todos) | implementado | ausente | |
| Filtro por operadora | implementado | ausente | |
| Grafico de uptime (UptimeGridChart) | implementado | ausente | Canvas API disponivel |
| Narrativa textual de uptime | implementado | ausente | Gerada localmente ou via worker |
| Resumo do historico (tendencias) | implementado | ausente | |
| Exportar CSV | implementado | ausente | `Blob` + `URL.createObjectURL` — implementavel no browser |
| Exportar PDF | implementado | ausente | Possivel via `window.print()` com estilos dedicados ou biblioteca jsPDF |
| Diagnostico IA no detalhe do teste | implementado | ausente | Depende de integracao com worker IA |

---

## Tela: Ajustes (AjustesScreen)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Perfil do usuario (nome, foto) | implementado | ausente | File API disponivel; foto salva em localStorage / IndexedDB |
| Dados do provedor (operadora, plano, regiao) | implementado | ausente | Salvar em localStorage |
| Velocidade contratada (Minha Conexao) | implementado | ausente | |
| Tema claro / escuro | implementado | ausente | `prefers-color-scheme` + localStorage para override |
| Monitoramento passivo (WorkManager) | implementado | n/a-browser | Service Worker pode agendar sync, mas sem acesso a rede em background real. Ver nota abaixo |
| Notificacoes de latencia / DNS / RSSI / sem internet | implementado | parcial | Notificacoes via Web Push API — requer permissao; conteudo limitado (sem RSSI real) |
| Limite de alerta de velocidade (Mbps) | implementado | ausente | Configuravel; alerta via notificacao web push se houver monitoramento |
| Configuracao de modem Fibra (host, user, senha) | implementado | ausente | Requer CORS do modem ou proxy — provavelmente n/a por restricao de rede local |
| Analise avancada (flag) | implementado | ausente | |
| Consumo de dados moveis no mes | implementado | ausente | |
| Limpar historico | implementado | ausente | Limpar IndexedDB / localStorage |
| Apagar dados locais | implementado | ausente | |
| Resetar app | implementado | ausente | |
| Politica de privacidade (PrivacidadeScreen) | implementado | ausente | |
| Novidades / changelog (NovidadesScreen) | implementado | ausente | |

**Limitacao de browser — monitoramento passivo:** o browser nao suporta execucao de codigo em background de forma continua. Background Sync API e Periodic Background Sync (Chrome/Android) permitem execucao ocasional do Service Worker, mas: (1) frequencia minima nao garantida, (2) indisponivel em Safari e Firefox, (3) requer permissao adicional no Chrome. O monitoramento passivo no PWA sera degradado: execucao apenas com o app aberto ou via Periodic Background Sync com restricoes declaradas na UI.

---

## Overlay: Resultado de Velocidade (ResultadoVelocidadeScreen)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Download, upload, ping, bufferbloat | implementado | ausente | |
| Veredicto qualitativo (Excelente / Bom / Regular / Fraco) | implementado | ausente | |
| Localizacao do servidor e ISP | implementado | ausente | |
| Banner Anatel dismissivel | implementado | ausente | |
| Analisador de problema (SIG-113) | implementado | ausente | Requer integracao com worker IA |
| Compartilhar resultado | implementado | ausente | `navigator.share` disponivel |

---

## Overlay: Diagnostico IA / SignallQ (SignallQScreen + LaudoScreen)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Chat com IA SignallQ (Qwen3 via Cloudflare Worker) | implementado | ausente | Worker ja existe — integracao por fetch |
| Perguntas contextuais interativas | implementado | ausente | |
| Laudo gerado (resumo do diagnostico) | implementado | ausente | |
| Superficies escuras (#0D0D1A / #1A0B2E) | implementado | ausente | Tailwind com tokens do design system |
| Analise direcionada por problema (SIG-113) | implementado | ausente | |
| Chat livre com IA (LLMChatScreen) | implementado | ausente | |
| Cotas de uso da IA | implementado | ausente | Logica de cota esta no worker — verificar endpoint |

---

## Overlay: Dispositivos (DispositivosScreen)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Scan de dispositivos na rede local (ARP / mDNS / SSDP) | implementado | n/a-browser | Raw sockets indisponiveis no browser. Impossivel |
| Identificacao por OUI (fabricante) | implementado | n/a-browser | Depende de scan |
| Apelidos por dispositivo | implementado | n/a-browser | Depende de scan |
| Classificacao de dispositivo (router, TV, celular etc.) | implementado | n/a-browser | Depende de scan |

---

## Overlay: Fibra / Modem (FibraModemScreen)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Conexao com modem Nokia via HTTP local | implementado | n/a-browser | Browser bloqueia requisicoes para IPs locais (CORS + mixed content). Implementavel apenas em contexto HTTPS com proxy ou extensao de browser — nao viavel para PWA publico |
| Status GPON / PPPoE / WAN | implementado | n/a-browser | Depende de acesso ao modem |
| Potencia optica (dBm) e status de saude | implementado | n/a-browser | |

---

## Overlay: Ping (PingScreen)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Ping continuo para host configurado | implementado | ausente | Fetch HTTP RTT — sem ICMP, mas comportamento equivalente para usuario final |
| Historico de RTT ao vivo | implementado | ausente | |

---

## Overlay: DNS Benchmark (DnsScreen / bottom sheet)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Benchmark de multiplos servidores DNS (tempo de resolucao) | implementado | n/a-browser | `dns.resolve()` nao existe no browser. DNS lookup via fetch indireto e pouco confiavel para benchmark real. Funcionalidade nao e viavel no PWA sem proxy dedicado |
| Recomendacao de configuracao DNS | implementado | n/a-browser | Depende do benchmark |

---

## Overlay: Minha Conexao (MinhaConexaoScreen)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Formulario: operadora, estado, cidade, velocidade contratada | implementado | ausente | Implementavel com formulario e localStorage |

---

## Overlay: Novidades (NovidadesScreen)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Changelog da versao atual do app | implementado | ausente | Conteudo estatico ou via fetch de arquivo markdown |

---

## Overlay: Privacidade (PrivacidadeScreen)

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Politica de privacidade e opcao de apagar dados | implementado | ausente | |

---

## Onboarding

| Feature | Android | PWA | Observacao |
|---|---|---|---|
| Onboarding na primeira execucao | implementado | ausente | Flag em localStorage |

---

## Resumo consolidado

| Categoria | Implementado | Parcial | Ausente | N/A browser | N/A design |
|---|---|---|---|---|---|
| Navegacao / Shell | 0 | 0 | 8 | 0 | 0 |
| Inicio | 0 | 0 | 10 | 1 | 0 |
| Velocidade | 0 | 0 | 11 | 1 | 1 |
| Sinal | 0 | 0 | 1 | 8 | 0 |
| Historico | 0 | 0 | 9 | 0 | 0 |
| Ajustes | 0 | 1 | 12 | 2 | 1 |
| Overlay Resultado | 0 | 0 | 5 | 0 | 0 |
| Overlay Diagnostico IA | 0 | 0 | 7 | 0 | 0 |
| Overlay Dispositivos | 0 | 0 | 0 | 4 | 0 |
| Overlay Fibra | 0 | 0 | 0 | 3 | 0 |
| Overlay Ping | 0 | 0 | 2 | 0 | 0 |
| Overlay DNS | 0 | 0 | 0 | 2 | 0 |
| Overlay Minha Conexao | 0 | 0 | 1 | 0 | 0 |
| Overlay Novidades | 0 | 0 | 1 | 0 | 0 |
| Overlay Privacidade | 0 | 0 | 1 | 0 | 0 |
| Onboarding | 0 | 0 | 1 | 0 | 0 |
| **Total** | **0** | **1** | **69** | **21** | **2** |

---

## Features permanentemente N/A no browser

Lista consolidada do que nunca sera implementado no PWA e o motivo tecnico:

| Feature | Motivo |
|---|---|
| RSSI / nivel de sinal Wi-Fi | Nenhuma Web API expoe RSSI |
| Scan de redes Wi-Fi visiveis | Proibido por APIs de seguranca do browser |
| Canal e frequencia Wi-Fi | Depende de scan |
| Analise de interferencia de canal | Depende de scan |
| Sinal movel (RSRP, RSRQ, band) | API de telefonia indisponivel no browser |
| Informacoes de SIM | Idem |
| Scan de dispositivos na rede local (ARP/mDNS/SSDP) | Raw sockets indisponiveis |
| Conexao com modem local (Fibra) | CORS + mixed content bloqueiam requisicoes a IPs privados em HTTPS |
| DNS benchmark real | `dns.resolve()` nao existe; fetch indireto nao e benchmark valido |
| IP local confiavel | RTCPeerConnection inconsistente; bloqueado em Firefox e Safari |
| Monitoramento passivo continuo | Background Sync com restricoes severas; indisponivel em Safari |

---

## Como manter este documento atualizado

**Regra geral:** qualquer feature nova que entrar no Android deve ter uma entrada neste documento antes do PR ser mergeado.

### Fluxo — feature nova no Android (Camilo)

1. Ao abrir o PR com a feature, adicionar no corpo do PR a secao `## Paridade PWA` com:
   - Nome da feature
   - Se e implementavel no browser (sim / parcial / n/a com motivo tecnico)
   - Link para este documento como referencia
2. Atualizar este arquivo (`docs_ai/technical/paridade-plataformas.md`) na mesma branch ou em issue separada marcada com `area:pwa`.

### Fluxo — feature nova no PWA (Renan)

1. Ao implementar uma feature, atualizar o status neste documento de `ausente` para `implementado` ou `parcial`.
2. Se `parcial`, documentar o comportamento degradado na coluna "Observacao".
3. Atualizar a linha de "Ultima atualizacao" no cabecalho.

### Quando acionar a Lia

Se a divergencia entre Android e PWA gerar experiencia visualmente inconsistente que o usuario possa perceber como bug, acionar a Lia para definir o comportamento esperado no PWA antes de implementar.

### Revisao periodica

A Gema revisa este documento em cada Cycle Review para verificar se features marcadas como `ausente` com issue aberta estao progredindo.
