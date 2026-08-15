---
title: "Módulo :featureFibra"
description: "Driver de leitura autenticada da ONT Nokia GPON G-1425G-B e normalização do snapshot para o contrato de dispositivo local."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-06"
---

# `:featureFibra`

- **Caminho físico:** `android/feature/fibra/` (alias flat legado, remapeado por `projectDir` em `android/settings.gradle.kts`)
- **Namespace:** `io.signallq.app.feature.fibra`

## Responsabilidade

Driver de equipamento local: autentica na interface web da ONT GPON do usuário, faz scraping das páginas `.cgi`, extrai o estado óptico/WAN/PPP/Wi-Fi/LAN e a lista de clientes conectados, e normaliza tudo para o contrato compartilhado `LocalNetworkDeviceSnapshot` de `:coreNetwork`. Também classifica o nível de sinal óptico RX/TX contra o perfil normativo da classe GPON B+.

Não é dele: a UI de "Equipamento de Internet" (vive em `:app` — o módulo não tem nenhum `@Composable`), a decisão de qual host consultar (`MainViewModel` resolve o gateway por scan de rede), o armazenamento de credenciais, o motor genérico de saúde GPON (`ClassificadorSaudeGpon`, em `:coreNetwork`, ainda não wireado a este perfil) e qualquer diagnóstico de causa raiz.

## Dependências

Extraídas de `android/feature/fibra/build.gradle.kts`.

| Dependência | Configuração | Observação |
|---|---|---|
| `:coreNetwork` | `implementation` | Contratos `localdevice`: `LocalNetworkDeviceSnapshot`, `DeviceType`, `SupportLevel`, `TipoConexaoFisica` etc. |
| `libs.androidx.core.ktx` | `implementation` | |
| `libs.kotlinx.coroutines.android` | `implementation` | |
| `libs.timber` | `implementation` | |
| `libs.junit` | `testImplementation` | |
| `libs.androidx.junit`, `libs.androidx.espresso.core` | `androidTestImplementation` | |

Sem Hilt, sem OkHttp, sem Room. O HTTP é feito com `java.net.HttpURLConnection` puro e a criptografia com `javax.crypto`/`java.security` do JDK.

## Consumidores

`grep -rn 'project(":featureFibra")' --include=*.kts .`

| Consumidor | Local |
|---|---|
| `:app` | `android/app/build.gradle.kts:317` |

Nenhum outro módulo depende deste. Em `:app`, o wiring acontece em `di/AppModule.kt` (`FeatureFibraModulo.criarExecutor()`), `MainViewModel.kt`, `ui/screen/EquipamentoInternetScreen.kt`, `EquipamentoPanelMapper.kt` e `FibraModemUiState.kt`.

## Equipamentos suportados

**Único equipamento com driver de produção: Nokia G-1425G-B** (ONT GPON classe B+, série ALCL / Alcatel-Lucent, OUI `F82229`, chipset MediaTek MTK7528H). Todo o código do módulo (`NokiaModemClient`, `NokiaModemCrypto`, `NokiaModemParser`, `NokiaG1425GBProfile`, `NokiaLocalDeviceMapper`) é específico deste modelo. `NokiaLocalDeviceMapper` publica o snapshot com `supportLevel = SupportLevel.LAB_VALIDATED`.

Os mapas de campo vivem em `docs_ai/technical/` e são documentos de **reconhecimento**, não de produto:

| Documento | Equipamento | Método de levantamento | Existe driver? |
|---|---|---|---|
| `docs_ai/technical/NOKIA_GPON_FIELD_MAP.md` | Nokia G-1425G-B (ONT GPON) | Acesso HTTP ao vivo ao equipamento real (2026-07-08), login RSA+AES replicando o `crypto_page.js` da própria ONT | **Sim** — este módulo |
| `docs_ai/technical/TPLINK_ARCHER_ROUTER_FIELD_MAP.md` | TP-Link Archer C6/A6v2 (família `tplink-stok-luci`) | Acesso HTTP ao vivo (`192.168.0.1`, 2026-07-08/09), handshake `form=keys` → `form=auth` → `form=login` replicado em Node.js | Não |
| `docs_ai/technical/INTELBRAS_RX1500_FIELD_MAP.md` | Intelbras RX1500/RAX1500 | Análise **estática/offline** do arquivo de firmware `.aes` (2026-07-09), sem acesso ao equipamento; extração do `rootfs` bloqueada | Não — "Intelbras" só aparece no código como fabricante no catálogo OUI |

## Componentes principais

| Arquivo / classe | Responsabilidade |
|---|---|
| `.../fibra/ExecutorFibra.kt` (248 linhas) | Ponto de entrada. Expõe `snapshotFlow: StateFlow<SnapshotFibra>`. `executar(host, username, password)` reaproveita a sessão HTTP em cache antes de tentar login novo (#894 — o firmware rejeita login concorrente com `err_t=0`), com até 3 tentativas e backoff de 1 s×tentativa. Erro permanente (host inválido, CSRF) interrompe o retry. `reiniciar()` e `marcarSemRede()` completam a API |
| `.../fibra/NokiaModemClient.kt` (275 linhas) | Sessão HTTP com o modem. Protocolo: `GET` da página de login → extração do material cripto → `POST /login.cgi` → uso de `sid`/`X-SID` nas demais páginas. `fetchPage(path)`, `reboot()` (`/reboot.cgi`). `internal`; valida o host no `init` |
| `.../fibra/NokiaModemCrypto.kt` (77 linhas) | Replica o `crypto_page.js` do equipamento: AES-CBC com padding ISO/IEC 7816-4 + RSA PKCS#1 v1.5. Extrai `pubkey`, `nonce` e `csrf_token` do HTML por regex |
| `.../fibra/NokiaModemParser.kt` (494 linhas) | Parser de todas as páginas. `parseGpon`, `parseWan`, `parsePpp`, `parseDeviceInfo`, `parseWifi`, `parseLan`, `parseClientes`, `normalizarTipoConexaoFisica`. Trata particularidades do firmware: typo `SupplyVottage`, `TransceiverTemperature` em Q8.8 (`raw/256`), `LaserCurrent` em 0,5 µA (`raw/500`), tensão em `raw/10000` |
| `.../fibra/ValidadorHostEquipamento.kt` (49 linhas) | GH#1213 item 2 — só aceita **IP literal privado/local** (RFC1918, `169.254/16`, loopback, `::1`, `fe80::`, ULA `fc00::/7`). Nunca resolve hostname por DNS, para não abrir janela de DNS rebinding. Falha rápido antes de qualquer credencial sair do aparelho |
| `.../fibra/NokiaG1425GBProfile.kt` (84 linhas) | Perfil óptico versionado (ITU-T G.984.2 Amd.1 classe B+): RX operacional −27,0 a −8,0 dBm; TX 0,5 a 5,0 dBm; margens de atenção −25,0/−10,0 dBm (heurística de produto, **não** normativa). Contém `NivelSinalOpticoRx` (4 estados), `NivelSinalOpticoTx` (3 estados) e o `object ClassificadorOpticoNokiaG1425GB` (funções puras) |
| `.../fibra/NokiaLocalDeviceMapper.kt` (114 linhas) | Converte `SnapshotFibra` → `LocalNetworkDeviceSnapshot` (`DeviceType.ONT_GPON`, `SupportLevel.LAB_VALIDATED`). Só mapeia leituras `EstadoFibra.concluido` com dado óptico presente; caso contrário devolve `null` |
| `.../fibra/RebootLabFlags.kt` (16 linhas) | `HABILITADO_SEM_VALIDACAO_HARDWARE = false` — o reboot nunca foi validado contra hardware físico, então fica indisponível em produção (critério de aceite da #1213 item 12) |
| Modelos: `SnapshotFibra.kt` (20), `GponStatus.kt` (14), `WanStatus.kt` (25), `WifiStatus.kt` (23), `LanStatus.kt` (15), `PppStatus.kt` (9), `DeviceInfoFibra.kt` (23), `ClienteFibra.kt` (18), `EstadoFibra.kt` (3), `GponSaudeStatus.kt` (4) | Data classes do snapshot bruto do equipamento |
| `.../fibra/FeatureFibraModulo.kt` (5 linhas) | Fachada mínima: `criarExecutor(): ExecutorFibra` |

### Páginas consumidas do equipamento

Lidas por `ExecutorFibra.buscarSnapshot` a cada leitura:

| Path | Conteúdo | Criticidade |
|---|---|---|
| `/login.cgi` | Autenticação (POST) | Crítico |
| `/wan_status.cgi?gpon` | Estado GPON, RX/TX, temperatura, serial, tensão, corrente do laser | Crítico |
| `/show_wan_status.cgi?ipv4` | Conexões WAN (`wan_conns`) | Crítico |
| `/index.cgi?getppp` | Estado PPPoE (JSON) | Crítico |
| `/device_status.cgi` | Modelo, fabricante, firmware, hardware, uptime | Crítico |
| `/lan_status.cgi?lan` | LAN + objeto `wlan_status` (rádios Wi-Fi, canal, segurança, potência) | Best-effort |
| `/lan_ipv4.cgi` | Configuração IPv4 da LAN | Best-effort |
| `/lan_status.cgi?wlan` | `device_cfg` + `alias_cfg` — lista de clientes conectados | Best-effort |
| `/reboot.cgi` | Reinício do equipamento | Desligado por `RebootLabFlags` |

O comentário no código registra a correção de 2026-07-10: `wlan_status` vive em `lan_status.cgi?lan`, não em `?wlan` — revalidado contra equipamento real.

## Riscos e dívidas

- **Path físico alinhado ao package `io.signallq.app.*`** — migração de `io/signallq/app/kotlin/` concluída em 2026-08-15 (#1645).
- **Dependência entre features:** nenhuma. O módulo depende só de `:coreNetwork` — está em conformidade com a regra.
- **Regra de negócio em Composable:** não aplicável — 0 `@Composable` no módulo (verificado por grep). A classificação óptica está em `object` de funções puras, testado isoladamente.
- **Arquivos acima de 800 linhas:** nenhum. O maior arquivo é `NokiaModemParser.kt` com **494 linhas** — acima do limiar de "revisar coesão" (400) da §7, mas abaixo do de extração obrigatória. Segundo maior: `NokiaModemClient.kt`, 275 linhas.
- **Acoplamento a um único equipamento.** O módulo inteiro é o driver de um modelo. Não há abstração de família de driver nem registry — adicionar um segundo equipamento (TP-Link ou Intelbras, ambos já mapeados em `docs_ai/technical/`) exigiria extrair a interface hoje implícita. Enquanto isso, o app só lê ONT para esse hardware específico.
- **Fragilidade estrutural do scraping.** Parser por regex sobre HTML/JS do firmware, e a criptografia replica byte a byte o `crypto_page.js`. O comentário em `NokiaModemCrypto` é explícito: "qualquer alteração pode quebrar o login silenciosamente (modem retorna `err_t=[0]`)". Uma atualização de firmware do fabricante quebra a leitura sem nenhum sinal de compilação ou teste.
- **`NokiaG1425GBProfile` no lugar errado.** O próprio kdoc registra: o perfil é candidato a ser consumido pelo motor canônico (#1228) mas vive em `feature/fibra` porque ainda não foi wireado ao `ClassificadorSaudeGpon` genérico de `:coreNetwork`. Há hoje duas noções de saúde óptica no repo.
- **Falta de teste.** Com teste: `NokiaModemParser`, `NokiaLocalDeviceMapper`, `ClassificadorOpticoNokiaG1425GB`, `ValidadorHostEquipamento`, `chaveErroFibra`, `normalizarTipoConexaoFisica`, e um teste mínimo de `NokiaModemClient` (23 linhas — cobre só a validação de host do `init`). **Sem teste:** `ExecutorFibra` (248 linhas — retry, cache de sessão, best-effort por página, `reiniciar()`) e `NokiaModemCrypto` (77 linhas — o handshake que quebra silenciosamente). São exatamente os dois pontos de maior risco operacional do módulo.
- **`reboot()` implementado mas não validado** contra hardware físico; permanece desligado por flag. Código vivo não exercitado.
