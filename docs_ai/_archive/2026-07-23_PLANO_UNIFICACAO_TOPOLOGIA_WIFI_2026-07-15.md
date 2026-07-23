# Unificação da identificação de topologia Wi-Fi e tipos de dispositivo

## Contexto

O Luiz pediu pra entender como o app identifica hoje roteador, mesh, extensor e tipos de
dispositivo conectados via Wi-Fi, e montar um plano pra melhorar essa identificação. Uma
primeira versão deste plano foi revisada e devolvida com 12 ajustes obrigatórios — esta versão
já os incorpora.

O levantamento encontrou **três motores de classificação desalinhados**, cada um com sua
própria heurística, sem compartilhar dados entre si. Não são só a Home e a tela Dispositivos
que podem divergir — a tela **Sinal → aba Redes** também consome um dos três motores
diretamente. Hoje o app pode mostrar **três vereditos diferentes pra mesma rede física, em
três telas diferentes, ao mesmo tempo**:

1. **`ClassificadorDispositivoRede`** (`feature/devices`) — classifica hosts da LAN
   (descobertos via ping/ARP/mDNS/SSDP/porta) em `TipoDispositivo`. Alimenta a tela
   "Dispositivos".
2. **`GatewayHeuristica`** (`app`) — classifica a rede conectada em `WifiRouter/WifiMesh/WifiExtender`
   só por regex de SSID + fallback de múltiplos BSSIDs/RSSI. Alimenta a Home
   (`MainViewModel.kt:~1590-1650`).
3. **`TopologiaWifiEngine` + `MeshOuiDatabase`** (`feature/wifi` + `coreNetwork`) — classifica
   por fabricante do rádio (OUI do BSSID) + agrupamento de banda. Alimenta a aba Redes de
   `SinalScreen.kt` (linhas ~1234, ~1301) e o `RecommendationEngine` de diagnóstico. **Não
   alimenta nem a Home nem a tela Dispositivos.**

Achado extra: as duas bases de OUI (`OuiDatabase` em `feature/devices` e `MeshOuiDatabase` em
`coreNetwork`) têm um conflito de curadoria não documentado — o OUI da Intelbras (`C46E1F`,
`6C5AB0`) está cadastrado simultaneamente como "nó mesh" e como "gateway ISP" em listas
paralelas, resolvido hoje por uma regra ad-hoc dentro do `TopologiaWifiEngine`.

Limitação de plataforma: o roteador central de uma malha mesh quase nunca aparece separado na
Home, porque o Android só expõe a rota IP default do nó ao qual o device está conectado — o
`GatewayInfo` só cria um nó "Roteador" quando existe uma segunda rota IP visível, o que é raro
em mesh de verdade mesmo quando o app já tem, no scan de Wi-Fi, evidência de múltiplos BSSIDs.
**Importante (ajuste #1 da revisão): essa evidência prova que existe um sistema mesh, não qual
nó específico é o roteador central** — o plano não deve fingir uma certeza que a plataforma não
permite.

**Quarto sinal, hoje isolado e subaproveitado — leitura direta do próprio equipamento.** Existe
um `ClientSnapshot` (`coreNetwork/contracts/localdevice/`) alimentado por scraping autenticado
real do gateway:
- **Só o ONT Nokia G-1425G-B tem scraper de verdade implementado** (`feature/fibra/NokiaModemClient.kt`
  + `NokiaModemParser.kt` + `ExecutorFibra.kt`) — login autenticado real (RSA/AES), parsing de
  HTML/JS do firmware.
- **TP-Link e os fabricantes mesh mais comuns (Deco, Eero, Orbi, Velop, AiMesh, Nest Wifi,
  UniFi Mesh) não têm scraper nenhum** — só fingerprint passivo (`DeviceDriverCatalog.kt`/
  `EquipmentClassifier.kt`), nunca `LAB_VALIDATED`.
- **`ClientSnapshot` não modela "isto é um nó mesh satélite"** — hoje só tem
  `mac/ip/hostname/tipoConexao`, e `tipoConexao` é **string crua do firmware**, não normalizada.
- **É 100% manual** — usuário digita host/usuário/senha do equipamento
  (`GatewayConnectionSheet.kt`); sem descoberta automática. Autoconexão só retoma sessão já
  configurada (por BSSID exato).
- **Uso hoje é só de naming** (`NamingPrioridade.resolverNomeRouterActive`), nunca de
  classificação de topologia/tipo.
- Credenciais já protegidas: `CredenciaisModemStore.kt` usa `EncryptedSharedPreferences` +
  `AndroidKeyStore` (AES-256 GCM), `allowBackup="false"` no manifest, `NokiaModemClient` só loga
  tamanho/status (nunca senha/HTML cru) — ver seção Segurança abaixo.

Nenhuma fase deste plano depende de root, sinais 802.11k/v/r, ou leitura de `/proc/net/arp`
(bloqueado desde Android 10).

## Restrição arquitetural

Por `arquitetura-android`, features não podem depender de features
(`featureDevices → featureWifi` é proibido). O motor unificado mora em **`:coreNetwork`** — já
é dependência comum de `featureDevices`, `featureWifi`/`app` e `coreRecommendation`, e já
hospeda `MeshOuiDatabase`, `RedeVizinha`, `ClientSnapshot`.

**Ajuste #8 da revisão — separação contrato/implementação:** modelos de dado (enums, data
classes de resultado) ficam em `:coreNetwork/contracts/...`; a lógica (engine, catálogo OUI,
correlator) fica em pacotes de implementação próprios dentro de `:coreNetwork`
(`:coreNetwork/topologia/engine/`, `:coreNetwork/topologia/oui/`, `:coreNetwork/topologia/correlacao/`),
não misturados com `contracts/`.

## Decisão já tomada

Catálogo OUI remoto (worker `signallq-diagnostic`) fica **fora de escopo por agora**. Migração
pra remoto vira issue separada, decidida depois.

## Modelo de resultado (ajuste #2 e #3 da revisão)

O motor unificado **não retorna um enum final sozinho**. Retorna um resultado estruturado:

```
ClassificacaoTopologia(
    papelProvavel: PapelTopologia,      // enum, ver abaixo — inclui SISTEMA_MESH_PROVAVEL
    confianca: NivelConfianca,          // ALTA / MEDIA / BAIXA
    evidencias: List<Evidencia>,        // cada uma com tipo (OUI/SSID/RSSI/ClientSnapshot/Correlacao),
                                         // valor bruto observado, e peso atribuído
    origemDados: OrigemDados,           // SCAN_WIFI_PASSIVO / SCAN_LAN_ATIVO / GATEWAY_DIRETO / CORRELACAO
    conflitos: List<ConflitoSinal>,     // quando dois sinais discordam (ex.: SSID diz mesh, OUI diz gateway ISP)
)
```

`PapelTopologia` inclui explicitamente `SISTEMA_MESH_PROVAVEL` (evidência de múltiplos nós, mas
sem identificar qual é central) além de `ROTEADOR`, `NO_MESH`, `REPETIDOR`,
`PONTO_DE_ACESSO`, `DESCONHECIDO` — **não existe `ROTEADOR_CENTRAL_INFERIDO` como fato**; quando
a Fase 2 detecta mesh só por scan Wi-Fi sem 2ª rota IP, o resultado é "sistema mesh provável,
papel de cada nó desconhecido/provável", nunca a criação de um nó "Roteador central" afirmativo.

Catálogo OUI (ajuste #3) diferencia por entrada: fabricante, **papéis possíveis** (não um só),
**nível de validação** (curado manualmente / confirmado em campo) e **especificidade** (OUI
específico de linha de produto vs. OUI genérico do fabricante que também vende outros tipos de
equipamento) — OUI sozinho nunca é veredito, é uma evidência entre outras dentro de
`evidencias`.

## Preparação (antes da Fase 0)

**Ajuste #7 e #9 da revisão — obrigatório antes de qualquer código novo:**
- Mapear se `TipoDispositivo`/`TipoTopologia`/`ConnectionNodeType` atuais são persistidos
  (Room, DataStore) e como são serializados. Se algum for persistido por `ordinal` de enum,
  **corrigir para persistência por nome/código estável antes de tocar no enum** — nunca
  adicionar/reordenar valor de enum persistido por ordinal (corrompe dado existente
  silenciosamente).
- Mapear todos os consumidores atuais dos três motores (grep completo de call sites de
  `GatewayHeuristica`, `TopologiaWifiEngine`, `ClassificadorDispositivoRede`) — a lista da seção
  "Arquivos críticos" abaixo é o ponto de partida, não a lista final.
- **Criar testes de caracterização com fixtures de rede reais antes de qualquer refactor**,
  cobrindo pelo menos: mesh real (múltiplos nós, mesmo OUI, banda repetida); roteador único
  dual-band (mesmo OUI, banda não repetida); extensor com mesmo SSID mas OUI diferente;
  múltiplos APs cabeados no mesmo switch (não é mesh, é topologia cabeada); o conflito Intelbras
  (OUI em duas listas); dispositivos do mesmo fabricante do roteador que **não são** o gateway
  (ex.: um smartphone Intelbras — evita falso positivo "mesmo fabricante = é o roteador"). Esses
  testes travam o comportamento atual como baseline antes de qualquer fase mexer em lógica.

## Fases

### Fase 0 — Contratos canônicos (modelo só, zero lógica nova)

- Criar em `:coreNetwork/contracts/topologia/`: `PapelTopologia`, `NivelConfianca`, `Evidencia`,
  `OrigemDados`, `ConflitoSinal`, `ClassificacaoTopologia` (schema da seção acima).
- Persistência: usar código estável (string/int explícito, nunca ordinal) se qualquer enum for
  gravado no Room — confirmado na fase de Preparação.
- Criar `TipoDispositivoRede` em `:coreNetwork/contracts/dispositivo/` com os valores novos da
  Fase 3 já reservados.
- Teste: compile-check + testes de mapeamento contra os testes de caracterização criados na
  Preparação (devem continuar batendo com o comportamento atual, já que nada mudou ainda).
- Critério de saída: compila, zero mudança de comportamento visível, os três motores antigos
  continuam rodando em paralelo.

### Fase 1 — Catálogo OUI único com papéis possíveis e nível de validação

- Unificar `OuiDatabase` (feature/devices) e `MeshOuiDatabase` (coreNetwork) em
  `:coreNetwork/topologia/oui/OuiCatalog.kt` (implementação) com modelo em
  `:coreNetwork/contracts/oui/OuiEntry.kt`:
  `OuiEntry(prefixo, fabricante, papeisPossiveis: Set<PapelTopologia>, nivelValidacao: NivelValidacaoOui, especificidade: EspecificidadeOui)`.
  Resolve o conflito Intelbras com um registro por OUI e papéis múltiplos declarados, não duas
  listas que colidem silenciosamente.
- `OuiDatabase.lookup(oui)` e `TopologiaWifiEngine.isMeshNo`/`isGatewayIsp` passam a delegar pro
  catálogo único; funções antigas viram wrappers finos.
- Teste: rodar os testes de caracterização da Preparação + `ClassificadorDispositivoTest`/
  `TopologiaWifiEngineTest` sem alterar assertions (zero regressão esperada) + caso novo cobrindo
  o conflito Intelbras (resultado depende do contexto — SSID único vs. agrupamento de banda —
  não de ordem de declaração) + caso "mesmo fabricante do gateway mas não é o gateway" (evidência
  fraca isolada, não deve virar veredito).
- Critério de saída: uma fonte de verdade OUI, nenhuma tela muda output ainda.

### Fase 2A — Motor de topologia novo rodando em paralelo (sem trocar telas)

- Criar `TopologiaRedeEngine` em `:coreNetwork/topologia/engine/` (implementação) retornando
  `ClassificacaoTopologia` (não um enum simples). Absorve:
  - Agrupamento por SSID + OUI + banda do `TopologiaWifiEngine`, registrando cada sinal usado
    como `Evidencia` com peso.
  - Keyword de SSID do `GatewayHeuristica` como **evidência adicional**, nunca sobrescrevendo
    sozinha um sinal de OUI/banda mais forte — quando os dois concordam, confiança sobe; quando
    divergem, vira `ConflitoSinal` registrado no resultado, com OUI priorizado (é sinal de
    hardware; SSID é configurável pelo usuário).
  - **Mesh sem 2ª rota IP**: quando o grupo de BSSIDs do SSID conectado tem evidência de mesh
    (múltiplos nós, banda repetida) mas não há confirmação de qual é o central, retorna
    `papelProvavel = SISTEMA_MESH_PROVAVEL` com os papéis dos nós individuais como
    `DESCONHECIDO`/provável — nunca sintetiza um nó "Roteador central" afirmativo (ajuste #1).
- **Roda em paralelo, sem nenhuma tela consumir ainda** — só testes comparam o output do motor
  novo contra os testes de caracterização e contra o output dos três motores antigos lado a
  lado, logando divergências pra análise manual antes de migrar qualquer consumidor.
- Teste: todos os testes de caracterização da Preparação + `InferirTipoGatewayTest.kt` (14 casos,
  incluindo falso positivo ORANGE/GRANGE/EXTERIOR e threshold -75dBm) e `TopologiaWifiEngineTest.kt`
  portados para validar o motor novo isoladamente.
- Critério de saída: motor novo existe, testado, sem nenhum consumidor real ainda — zero risco
  de regressão em produção nesta fase.

### Fase 2B — Migração gradual dos consumidores

- Trocar call sites um de cada vez, cada um em PR própria, cada um definindo **como aquele
  contexto específico traduz `ClassificacaoTopologia` pra sua necessidade** (ajuste #10 — não é
  "mostrar o mesmo texto/enum em todo lugar", é consumir o mesmo dado canônico e apresentar
  conforme o contexto da tela):
  1. **Home** (`MainViewModel.coletarInfoLocalRede()`) — hoje usa `ConnectionNodeType`; passa a
     derivar de `ClassificacaoTopologia.papelProvavel`, com UI tratando `SISTEMA_MESH_PROVAVEL`
     como rótulo "provável", nunca afirmativo.
  2. **Sinal → Redes** (`SinalScreen.kt:~1234/1301`) — hoje usa `TipoTopologia` direto; passa a
     consumir o motor novo, mantendo a granularidade por BSSID que essa tela já tem (mais
     detalhada que a Home).
  3. **Recomendação** (`RecommendationEngine.kt`) — hoje usa `MeshOuiDatabase` direto; passa a
     consumir `evidencias`/`confianca` do resultado unificado pra decidir se vale recomendar
     ação (ex.: só recomendar reposicionamento de nó mesh se `confianca != BAIXA`).
- Cada troca roda o teste correspondente + os testes de caracterização antes de avançar pra
  próxima tela.
- Critério de saída: as três telas consomem `ClassificacaoTopologia`, cada uma traduzindo pro
  seu contexto — convergência de dado canônico, não de texto idêntico.

### Fase 2C — Remoção dos classificadores paralelos

- Só depois que 2A e 2B estiverem validadas em produção por um ciclo: remover
  `GatewayHeuristica`, `TopologiaWifiEngine` (versão antiga) e as duas bases OUI antigas.
- Critério de saída: um único motor, um único catálogo, zero código morto de classificação.

### Fase 3 — Ampliar cobertura de tipos de dispositivo (com cautela de sinal — ajuste #6)

Aditivo ao `ClassificadorDispositivoRede`, cascata atual preservada (fonte gateway > nome > mDNS
> OUI/porta). Cada sinal novo entra como **evidência adicional, não veredito isolado**:

- mDNS: `_matter._tcp` identifica **dispositivo Matter genérico**, não confirma categoria
  (câmera/plug/sensor) sozinho — usar como evidência de "é smarthome", detalhar subtipo só se
  outro sinal (nome, porta, TXT record específico) corroborar.
- Smart TV: `_androidtvremote._tcp` além do match por nome já existente.
- AirPlay/RAOP: porta 7000 + `_raop` **não diferenciam com certeza** Apple TV de HomePod
  sozinhos — tratar como "dispositivo Apple AV", subtipo só com corroboração adicional (nome,
  outro serviço mDNS).
- Câmera IP: porta 554 (RTSP) **isolada não confirma câmera** — outros dispositivos abrem essa
  porta; exigir corroboração (mDNS `_rtsp._tcp`, ou nome) antes de classificar como câmera com
  confiança alta; porta sozinha vira evidência fraca, não veredito.
- Console: OUI de Nintendo (hoje cai errado em `smarthome`) é **sinal complementar, não
  definitivo** — combinar com padrão de nome/porta quando disponível; ainda assim é melhoria
  sobre o estado atual (que classifica errado com confiança implícita alta).
- Teste: expandir `ClassificadorDispositivoTest.kt` com um caso por sinal novo + casos negativos
  explícitos (porta 554 sem corroboração não vira câmera; Matter sozinho não vira subtipo
  específico).
- Critério de saída: cobertura mais granular, mas cada classificação nova carrega o nível de
  confiança real do sinal que a gerou — não finge certeza que o sinal não sustenta.

### Fase 4 — Correlação best-effort entre scan LAN e topologia Wi-Fi (ajuste #4 e #5)

**Fontes de evidência, não fontes de confirmação automática:**

- **`ClientSnapshot` (leitura direta do gateway), quando disponível** — hoje só ONT Nokia com
  credencial configurada. **`tipoConexao` deve ser normalizado por driver** para um enum
  `TipoConexaoFisica { ETHERNET, WIFI, DESCONHECIDO }` dentro do parser do driver (`NokiaModemParser`
  mapeia a string crua do firmware pro enum ali, não deixa a string vazar pro resto do app) —
  nunca consumir a string do firmware diretamente fora da camada do driver. Quando presente, é a
  evidência de maior peso (`OrigemDados.GATEWAY_DIRETO`), mas ainda entra como evidência no
  resultado estruturado, não substitui o resultado inteiro.
- **Correlação por MAC exato == BSSID exato** entre `DispositivoRede` e `RedeVizinha`: evidência
  **forte** (`Evidencia` de peso alto), rara em Android 10+.
- **Correlação por mesmo prefixo OUI (fabricante) apenas**: evidência **auxiliar/fraca** —
  nunca confirma identidade nem sobrescreve uma classificação já forte de outra fonte. Um
  smartphone Intelbras não vira "possível roteador" só por compartilhar fabricante com o gateway
  Intelbras da casa (caso de teste já coberto na Preparação).
- Quando há correlação forte (ClientSnapshot exato ou MAC exato), a tela Dispositivos herda o
  papel de topologia do nó Wi-Fi correspondente. Correlação fraca (só OUI) fica registrada como
  evidência adicional no resultado, sem forçar reclassificação.
- Estritamente aditivo e best-effort: sem correlação, tela Dispositivos funciona como hoje — sem
  dependência hard entre scans (Wi-Fi throttled 4x/2min, LAN sob demanda, gateway só se
  credencial configurada).
- Teste: unit test da função de correlação cobrindo os 4 níveis (ClientSnapshot exato / MAC
  exato / OUI fraco / sem match) + caso explícito "mesmo OUI não é o gateway" (não deve
  reclassificar) + teste garantindo que `DevicesViewModel` não regride sem scan Wi-Fi ou sem
  credencial de gateway.
- Critério de saída: em teste manual com rede mesh real, o nó mesh aparece com papel mais
  correto na tela Dispositivos; em teste com ONT Nokia configurado, com-fio vs Wi-Fi aparece
  corretamente distinguido via enum normalizado, nunca string crua do firmware.

### Fase 5 — (issues separadas por família/hardware/firmware) Scraper real de gateway mesh

**Ajuste #12 da revisão**: não é "suporte TP-Link" genérico — Archer (roteador único),
OneMesh (mesh via firmware TP-Link em produtos Archer/Deco compatíveis) e Deco (mesh dedicado)
são **arquiteturas de firmware diferentes**, cada uma precisa de investigação e parser próprios,
mesmo sendo do mesmo fabricante. Tratar como issues separadas:

- Issue própria por família de firmware (ex.: "Scraper TP-Link Archer standalone",
  "Scraper TP-Link OneMesh", "Scraper TP-Link Deco app-based") — não uma única issue "TP-Link".
- Mesmo padrão de referência do Nokia (`NokiaModemClient`/`NokiaModemParser`/`ExecutorFibra`):
  login autenticado real, parser dedicado, sem reuso forçado de lógica entre firmwares
  diferentes só por serem do mesmo fabricante.
- Modelar em `ClientSnapshot`/`LocalNetworkDeviceSnapshot` o campo que falta — "nó mesh do
  próprio sistema" vs. "cliente comum" — só quando a primeira família com suporte real (mesh)
  for implementada, não especular schema antes de ter um caso real validado.
- Mesma exigência de credencial manual dos demais — não muda UX de configuração existente.
- Deco/Eero/Orbi/Velop/AiMesh/Nest Wifi/UniFi seguem o mesmo princípio: cada um vira issue
  própria quando priorizado, não um "épico mesh genérico".

Fases 0-4 já entregam valor real (convergência entre telas, tipos mais granulares, correlação
best-effort, evidência estruturada) sem depender da Fase 5.

## Segurança (ajuste #11 da revisão)

Requisitos explícitos — a maioria já satisfeita hoje, listada aqui como checklist de
não-regressão pra qualquer código novo deste plano (especialmente Fase 4/5):

- **Credenciais em Android Keystore**: já implementado (`CredenciaisModemStore.kt`,
  `EncryptedSharedPreferences` + `AndroidKeyStore`, AES-256 GCM). Qualquer novo driver
  (Fase 5) deve usar o mesmo store, nunca DataStore plaintext ou SharedPreferences comum.
- **Nenhuma credencial ou HTML sensível em log**: já é o padrão em `NokiaModemClient.kt` (loga
  só tamanho/status, SID truncado a 8 chars). Manter o mesmo padrão em qualquer driver novo.
- **Dados brutos de clientes só locais**: já reforçado por `LocalDeviceSafeFilter` (GH#541) —
  lista crua de MAC/IP/hostname nunca vai pra IA/analytics, só contagem agregada. Qualquer
  `Evidencia`/`ClassificacaoTopologia` usada em telemetria deve passar pelo mesmo filtro.
- **Exclusão de credenciais de backup**: já é o padrão (`allowBackup="false"` +
  `data_extraction_rules.xml` no manifest) — confirmar que isso cobre também qualquer chave nova
  de driver adicionado na Fase 5.
- **Limpeza/expiração de sessão**: `ExecutorFibra` cacheia sessão HTTP em memória de processo
  (GH#894) — **verificar antes da Fase 4/5** se há TTL/expiração explícita ou se a sessão só
  morre com o processo; se não houver expiração, é item a corrigir antes de expandir pra mais
  drivers (mais superfície de sessão pendurada).

## Sequenciamento

Preparação → Fase 0 → Fase 1 → Fase 2A → Fase 2B (Home → Sinal → Recomendação) → Fase 2C →
Fase 3 → Fase 4. Fase 5 é issues separadas, priorizadas independentemente. Cada fase até 2B é
standalone e revertível; 2C só acontece depois de 2A+2B validadas em produção por um ciclo.
Rodar a cada fase: testes de caracterização da Preparação + `InferirTipoGatewayTest`,
`TopologiaWifiEngineTest`, `ClassificadorDispositivoTest`, `DispositivoRedeExtTest`,
`RecommendationEngineTest` (`feature/diagnostico` e `core/recommendation`) como gate de
não-regressão.

## Arquivos críticos

- `android/core/network/src/main/kotlin/io/veloo/app/core/network/contracts/wifi/MeshOuiDatabase.kt`
- `android/feature/devices/src/main/kotlin/io/veloo/app/kotlin/feature/devices/OuiDatabase.kt`
- `android/feature/wifi/src/main/kotlin/io/veloo/app/kotlin/feature/wifi/TopologiaWifiEngine.kt`
- `android/app/src/main/kotlin/io/veloo/app/kotlin/GatewayHeuristica.kt`
- `android/feature/devices/src/main/kotlin/io/veloo/app/kotlin/feature/devices/ClassificadorDispositivoRede.kt`
- `android/app/src/main/kotlin/io/veloo/app/kotlin/MainViewModel.kt` (`coletarInfoLocalRede`, ~linhas 1590-1650)
- `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/SinalScreen.kt` (chamadas a `TopologiaWifiEngine.classificar`, ~linhas 1234/1301)
- `android/app/src/test/kotlin/io/veloo/app/kotlin/InferirTipoGatewayTest.kt` (padrão de teste de referência)
- `android/core/network/src/main/kotlin/io/veloo/app/core/network/contracts/localdevice/ClientSnapshot.kt` (Fase 4/5)
- `android/core/datastore/src/main/kotlin/io/veloo/app/kotlin/core/datastore/CredenciaisModemStore.kt` (padrão de segurança a seguir em drivers novos)
- `android/feature/fibra/src/main/kotlin/io/veloo/app/kotlin/feature/fibra/NokiaModemClient.kt` +
  `NokiaModemParser.kt` + `ExecutorFibra.kt` (padrão de referência pra Fase 5)
- `android/core/network/.../gateway/DeviceDriverCatalog.kt` (catálogo de fingerprint, ponto de
  extensão pra driver novo na Fase 5)

## Verificação end-to-end

Além dos testes unitários e de caracterização por fase, validar manualmente (device real, não
emulador — scan de LAN e Wi-Fi real exige rede física):
1. Numa rede mesh real (ex.: TP-Link Deco, Google Nest Wifi, ou qualquer sistema com 2+ nós):
   comparar o resultado estruturado mostrado (via `evidencias`/`confianca`) em Home, Sinal →
   Redes e Dispositivos antes e depois de cada fase — devem convergir a partir da Fase 2B,
   mostrando "sistema mesh provável" sem afirmar qual nó é central.
2. Numa rede com roteador único simples: confirmar que nenhum `SISTEMA_MESH_PROVAVEL` falso é
   gerado (a Fase 2A não deve produzir falso positivo em rede não-mesh).
3. Numa rede com múltiplos APs cabeados (não mesh): confirmar que a Fase 2A não confunde isso
   com mesh Wi-Fi.
4. Na tela Dispositivos, após a Fase 4: confirmar que a correlação fraca (só OUI) nunca
   reclassifica um dispositivo comum como gateway/mesh.
5. Com ONT Nokia configurado: confirmar que `tipoConexao` chega normalizado
   (ETHERNET/WIFI/DESCONHECIDO), nunca como string crua do firmware, em qualquer consumidor fora
   do driver.
