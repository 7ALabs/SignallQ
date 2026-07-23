# Regra permanente — Higiene e padronização do repositório

- **Status:** ativo
- **Última validação:** 2026-07-16
- **Fonte de verdade:** este arquivo (`.claude/rules/higiene-e-padronizacao-repositorio.md`) — não duplicar em `docs_ai/`, `AGENTS.md`, mirrors ou docs de módulo
- **Escopo:** repositório `7ALabs/SignallQ` (monorepo SignallQ) inteiro — Android, Admin, Cloudflare, docs
- **Responsável:** Claudete (dono do processo), aplicado por todo agente (Camilo, Lia, Rhodolfo) e por qualquer sessão humana no repo

Referenciada a partir de `.claude/CLAUDE.md` (seção "Higiene e padronização do repositório") e dos
perfis de Claudete, Camilo, Lia e Rhodolfo. Não copiar o conteúdo completo em nenhum outro lugar —
só linkar.

---

## 1. Princípio geral

Durante qualquer tarefa:

> Deixe a área tocada em estado igual ou melhor do que estava antes.

Isso não significa tentar arrumar o repositório inteiro. A melhoria deve ser:

- incremental;
- relacionada à área modificada;
- segura;
- comprovável;
- pequena o suficiente para não desviar da entrega principal.

Não transformar uma correção simples em uma reforma arquitetural. Não ignorar problemas evidentes
encontrados na área tocada.

---

## 2. Idioma padrão

O idioma padrão do projeto é português do Brasil. Use PT-BR em: respostas ao usuário, planos,
relatórios, documentação, issues, descrições de PR, mensagens de erro exibidas ao usuário,
comentários que expliquem regras de negócio, critérios de aceite, nomes de branches e commits
quando isso não contrariar a convenção técnica existente. Não usar inglês desnecessariamente em
documentação ou comunicação.

### Identificadores de código

Use português para conceitos de produto e domínio: diagnóstico, rede, dispositivo, velocidade,
histórico, ajustes, sinal, fibra, topologia, recomendação, monitoramento, operadora, provedor.

Mantenha em inglês apenas termos técnicos consolidados no ecossistema Android/Kotlin ou já adotados
como sufixo estrutural: `Screen`, `ViewModel`, `UiState`, `Repository`, `UseCase`, `Worker`, `Dao`,
`Entity`, `Mapper`, `Parser`, `Driver`, `Client`, `Provider`, `Factory`, `Coordinator`, `Module`,
`Test`.

Exemplos aceitáveis: `DiagnosticoScreen.kt`, `DiagnosticoViewModel.kt`, `DiagnosticoUiState.kt`,
`RepositorioDiagnosticoLocal.kt` ou `DiagnosticoRepository.kt` (conforme o padrão dominante da
área), `ClassificadorTopologiaRede.kt`, `MedirLatenciaGatewayUseCase.kt`,
`EquipamentoInternetMapper.kt`.

Não introduzir combinações arbitrárias como `NetworkDiagnosticoManager`, `WifiAnaliseHelper` ou
`DeviceRedeUtils`.

Ao tocar em código antigo que mistura idiomas, padronize somente quando a renomeação for local,
segura e completamente validável. Renomeações espalhadas por vários módulos são tarefa específica.

Não renomear identificadores técnicos preservados pelo projeto: `io.signallq.app`,
`7ALabs/SignallQ`, `linkaKotlin.db`, `linkaPreferencias`, canais `linka_*`, workers cujos
nomes técnicos já estejam publicados (ver `.claude/CLAUDE.md`, seção "Identidade").

---

## 3. Precedência de fontes técnicas (código vs. documentação)

Distinta da tabela "Fontes da Verdade" do `.claude/CLAUDE.md` (que roteia execução/backlog entre
GitHub, Notion, Linear etc.) — esta ordem resolve divergência entre **código e documentação**
quando os dois descrevem o mesmo fato técnico:

1. código executado e testes;
2. `android/settings.gradle.kts`;
3. `android/gradle/libs.versions.toml`;
4. arquivos `build.gradle.kts`;
5. contratos e schemas realmente consumidos;
6. ADRs vigentes;
7. documentação ativa;
8. documentação histórica.

Nunca use como verdade atual um documento em `_archive`. Nunca atualize somente a data ou versão de
um documento para fazê-lo parecer atual. Antes de repetir versão, SDK, quantidade de módulos,
caminhos ou nomes de classes, confirme diretamente nas fontes acima.

---

## 4. Problemas estruturais já conhecidos

Dívidas conhecidas do repositório, validadas em 2026-07-15. Reconfirme se ainda existem antes de
agir — não presuma que seguem exatas conforme o tempo passa.

### 4.1 Caminho físico legado de packages (`io/veloo` vs `io.signallq.app`)

Validado: **460 arquivos `.kt`** ainda residem fisicamente em caminhos `io/veloo/app/kotlin/...`
(ex.: `android/app/src/main/kotlin/io/veloo/app/kotlin/`) apesar de declararem
`package io.signallq.app...` — divergência confirmada em **15 dos 16 módulos** (`:app`,
`core/database`, `core/datastore`, `core/network`, `core/permissions`, `core/telephony`, e todos os
9 módulos `feature/*`). Só `core/recommendation` já nasceu fisicamente em `io/signallq/` (módulo
criado depois do rebrand, issue #790).

O destino padronizado é `android/<modulo>/src/<sourceSet>/kotlin/io/signallq/app/...`.

Não mover apenas um ou dois arquivos oportunisticamente — isso criaria duas árvores físicas
concorrentes. A migração deve ser tarefa dedicada e atômica, cobrindo: `main`, `test`,
`androidTest`, imports, resources relacionados, schemas, regras de ProGuard, scripts, documentação,
referências de CI. Até essa migração acontecer, não criar novos arquivos dentro de novos
subdiretórios `io/veloo`.

### 4.2 `MainViewModel.kt`

Caminho real: `android/app/src/main/kotlin/io/veloo/app/kotlin/MainViewModel.kt` — **2191 linhas**
(acima do limiar de "dívida crítica" da seção 7). Concentra responsabilidades demais e não deve
continuar crescendo indiscriminadamente.

Ao tocar nele:
1. identifique qual responsabilidade está sendo modificada;
2. não adicione uma nova responsabilidade diretamente se ela puder viver em um componente dedicado;
3. prefira extrair orquestração, persistência, analytics, mapeamentos, diagnóstico, recomendações,
   ISP, DNS ou topologia para componentes próprios;
4. mantenha no `MainViewModel` apenas composição de estados e coordenação de alto nível;
5. crie testes de caracterização antes de extrações com risco de comportamento;
6. não crie outro ViewModel gigante apenas para reduzir linhas.

### 4.3 `AppShell.kt`

Caminho real: `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/AppShell.kt` — **1146
linhas** (acima do limiar de extração obrigatória da seção 7). Deve ser shell de composição e
navegação, não depósito de regras de negócio.

Ao tocar nele, prefira separar: estado de navegação, controle da pilha de overlays, adaptação de
estados das telas, wiring entre features, componentes da barra inferior, dialogs e sheets
independentes. Não mover lógica de uma tela gigante para outra função privada no mesmo arquivo e
chamar isso de modularização.

### 4.4 `AjustesScreen.kt`

Caminho real: `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/AjustesScreen.kt` — **771
linhas** (acima do limiar de extração obrigatória da seção 7, mas diminuiu de 809 em relação ao último audit). Ainda contém múltiplos fluxos e
componentes.

Ao tocar em uma seção de Ajustes: extraia sheets e fluxos independentes para arquivos próprios,
agrupe por responsabilidade do usuário, não crie arquivos genéricos como `AjustesUtils.kt`, mantenha
`AjustesScreen.kt` como composição das seções, preserve uma única fonte para cada configuração.

### 4.5 `HomeScreen.kt`

Caminho real: `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/HomeScreen.kt` — **3938
linhas** (acima do limiar de "dívida crítica" da seção 7). Concentra a tela Início e múltiplas sheets
(Meu dispositivo, Internet/Provedor, Rede móvel, Medir agora, mais SignalQualitySheet,
QualidadePlaceholderSheet, MedicaoTipoSheet).

Ao tocar nele:
1. identifique qual sheet ou seção está sendo modificada;
2. não adicione nova sheet diretamente — extraia para arquivo dedicado antes;
3. prefira separar: estado de cada sheet, orquestração de métrica de sinal, adaptadores de dados,
   componentes de visualização, wiring com features subjacentes;
4. mantenha em `HomeScreen.kt` apenas a composição da tela principal e delegação das sheets;
5. cada sheet independente deve ter seu próprio arquivo (ex.: `MeuDispositivoSheet.kt`,
   `InternetProveedorSheet.kt`, `MedicaoTipoSheet.kt`);
6. crie testes de caracterização antes de extrações com risco de comportamento visual ou estado.

### 4.6 `EquipamentoInternetScreen.kt`

Caminho real: `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/EquipamentoInternetScreen.kt` — **1549 linhas** (acima do limiar de "dívida crítica" da seção 7). Concentra a tela de equipamento de internet/fibra/roteador com múltiplos painéis por capacidade.

Ao tocar nele:
1. identifique qual painel ou seção de capacidade está sendo modificada;
2. não adicione novo painel diretamente — extraia para componente dedicado antes;
3. prefira separar: estado de cada painel, adaptadores de dados de equipamento, classificadores de capacidade, componentes de visualização, wiring com dados de rede;
4. mantenha em `EquipamentoInternetScreen.kt` apenas a composição da tela principal e delegação dos painéis;
5. cada painel independente deve ter seu próprio arquivo (ex.: `OntSheet.kt`, `RotadorSheet.kt`, `EquipamentoCapacidadePanel.kt`);
6. crie testes de caracterização antes de extrações com risco de comportamento de dados ou estado.

### 4.7 `DispositivosScreen.kt`

Caminho real: `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/DispositivosScreen.kt` — **1386 linhas** (acima do limiar de "dívida crítica" da seção 7). Concentra a lista de dispositivos conectados com sheets de detalhe.

Ao tocar nele:
1. identifique qual sheet ou fluxo de detalhe está sendo modificado;
2. não adicione nova sheet diretamente — extraia para arquivo dedicado antes;
3. prefira separar: estado da lista, estado de cada sheet, adaptadores de dados de dispositivo, componentes de visualização, wiring com dados de rede;
4. mantenha em `DispositivosScreen.kt` apenas a composição da lista principal e delegação das sheets;
5. cada sheet independente deve ter seu próprio arquivo (ex.: `DispositivoDetalheSheet.kt`, `DispositivoConfiguracaoSheet.kt`);
6. crie testes de caracterização antes de extrações com risco de comportamento de lista ou estado.

### 4.8 `JogosScreen.kt`

Caminho real: `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/JogosScreen.kt` — **1120 linhas** (acima do limiar de extração obrigatória da seção 7). Concentra o fluxo de teste direcionado por jogo com 5 etapas.

Ao tocar nele:
1. identifique qual etapa ou jogo está sendo modificado;
2. não adicione nova etapa diretamente — extraia para componente dedicado antes;
3. prefira separar: estado de cada etapa, orquestração de fluxo (navegação entre etapas), adaptadores de dados de jogo, componentes de visualização;
4. mantenha em `JogosScreen.kt` apenas a composição do fluxo principal e delegação das etapas;
5. cada etapa independente deve ter seu próprio arquivo (ex.: `JogoEtapa1Screen.kt`, `JogoEtapa2Screen.kt`) ou componente reutilizável;
6. crie testes de caracterização antes de extrações com risco de comportamento de fluxo ou estado.

### 4.8b `SinalScreen.kt`

**3503 linhas** (acima do limiar de "dívida crítica" da seção 7; era 3672 antes da primeira
extração incremental, PR #1237). Encontrado em 2026-07-20 durante a implementação das issues
#1207/#1209 (auditoria de Sinal Canal/Wi-Fi), não documentado antes. Concentra as três abas da
tela Sinal (Móvel, Canal, Wi-Fi), cada uma com Composables, sheets de detalhe e, até #1237,
também toda a lógica de classificação/topologia/banda misturada com composição visual.

A PR #1237 (#1207/#1209) deu o primeiro passo de separação: funções puras de classificação de
topologia, ícone, banda e segurança (sem nenhum `@Composable`) foram extraídas para
`SinalTopologiaHelpers.kt` (mesmo pacote, ~190 linhas). As três seções Composable (Móvel/Canal/
Wi-Fi) continuam no arquivo principal — extraí-las é o próximo passo, de risco maior por mexer
em estado e composição visual.

Ao tocar nele:
1. identifique qual aba (Móvel/Canal/Wi-Fi) ou sheet está sendo modificada;
2. motor/classificador real (regra de negócio de diagnóstico, ex. limiares RSRP/canal/topologia)
   pertence a `core/diagnostico` ou `core/network`, não à Screen; função pura só de apoio visual
   da própria tela (ícone, rótulo, cor, agrupamento) vai em `SinalTopologiaHelpers.kt` — não
   adicione nenhuma das duas direto no Composable;
3. prefira separar por aba quando extrair Composables: `SinalMovelSection.kt`,
   `SinalCanalSection.kt`, `SinalWifiSection.kt` são os nomes-alvo sugeridos;
4. mantenha em `SinalScreen.kt` apenas a composição do Scaffold, TabRow e delegação das abas;
5. crie testes de caracterização antes de extrações com risco de comportamento visual ou estado.

### 4.9 Identificação de topologia e dispositivos

Quando encontrar motores, heurísticas ou classificadores concorrentes:
1. liste todos os consumidores;
2. registre as entradas disponíveis para cada implementação;
3. compare os resultados para os mesmos cenários;
4. defina uma fonte de verdade;
5. preserve adaptadores somente quando houver diferenças legítimas de contrato;
6. não crie um novo classificador para contornar os existentes;
7. proteja a consolidação com testes de caracterização.

Features não podem depender diretamente de outras features. A composição entre domínios acontece em
`:app` ou por contratos normalizados em um módulo `core` adequado.

### 4.10 Documentação divergente

Valide antes de confiar: referências antigas a versões anteriores, quantidades antigas de módulos,
caminhos `io/veloo`, nomes antigos da marca, navegação anterior, agentes arquivados, issues antigas,
telas ou superfícies descontinuadas, módulos que já mudaram de localização. Ao modificar uma
funcionalidade, atualize somente a documentação diretamente relacionada — não revise todos os
documentos do projeto dentro de uma tarefa comum.

---

## 5. Convenção de módulos

Estrutura física atual (validada em `android/settings.gradle.kts`, 16 módulos):

```
android/
├── app/
├── core/
│   ├── network/          (:coreNetwork)
│   ├── database/         (:coreDatabase)
│   ├── datastore/        (:coreDatastore)
│   ├── permissions/      (:corePermissions)
│   ├── telephony/        (:coreTelephony)
│   └── recommendation/   (:coreRecommendation)
└── feature/
    ├── home/         (:featureHome)
    ├── speedtest/    (:featureSpeedtest)
    ├── wifi/         (:featureWifi)
    ├── devices/      (:featureDevices)
    ├── dns/          (:featureDns)
    ├── fibra/        (:featureFibra)
    ├── diagnostico/  (:featureDiagnostico)
    ├── history/      (:featureHistory)
    └── settings/     (:featureSettings)
```

Os aliases Gradle atuais (`:coreNetwork`, `:featureWifi` etc.) são legado compatível, enquanto as
pastas já usam estrutura hierárquica. O padrão desejado para uma **futura migração dedicada** é
renomear os aliases para `:core:network`, `:core:database`, `:core:datastore`, `:core:permissions`,
`:core:telephony`, `:core:recommendation`, `:feature:home`, `:feature:wifi`, `:feature:devices`,
`:feature:dns`, `:feature:speedtest`, `:feature:diagnostico`, `:feature:fibra`, `:feature:history`,
`:feature:settings`.

Não renomear aliases Gradle de forma oportunista — essa migração afeta dependências, CI, comandos,
documentação e possivelmente automações, e deve ser tarefa dedicada. Não criar novos módulos usando
o padrão antigo concatenado.

### Responsabilidade dos módulos

`:app` deve conter somente: inicialização, navegação, composição entre features, DI no nível da
aplicação, integrações que realmente dependam de múltiplas features, adaptação entre contratos.

`:feature:*` deve possuir: interface da feature, estado da feature, ViewModel ou state holder da
feature, casos de uso, regras específicas do domínio, componentes exclusivos daquela feature.

`:core:*` deve possuir: infraestrutura compartilhada, contratos normalizados, persistência, rede,
permissões, serviços reutilizáveis, regras que não pertencem a uma única feature.

Não criar `core-common`, `core-utils` ou outro módulo genérico usado como gaveta de bagunça.

---

## 6. Convenção de arquivos e símbolos Kotlin

### Arquivos

- Usar `PascalCase.kt`. O nome do arquivo deve corresponder ao principal símbolo público.
- Manter preferencialmente um tipo público principal por arquivo — tipos auxiliares pequenos e
  fortemente acoplados podem permanecer juntos.
- Não usar acentos, espaços, datas ou números arbitrários em nomes de arquivos.
- Não usar sufixos como `Novo`, `Antigo`, `Final`, `Final2`, `V2`, `Temp` ou `Backup`. Sufixos de
  versão só são permitidos quando representam contrato ou protocolo real, não tentativa informal de
  substituir código anterior.

### Classes e objetos proibidos como padrão genérico

Evitar nomes vagos: `Utils`, `Helper`, `Manager`, `Common`, `Misc`, `Base`, `Global`, `Data`,
`Service` (quando não for realmente um serviço), `Controller` (quando a responsabilidade não estiver
clara).

Substitua pelo comportamento real: `WifiUtils` → `CalculadoraCanalWifi`; `NetworkHelper` →
`MedidorLatenciaGateway`; `DeviceManager` → `ScannerDispositivosRede`; `DataMapper` →
`ResultadoSpeedtestMapper`.

### Funções

Use verbo que represente a ação. Evite nomes genéricos como `processar`, `executar`, `tratar` ou
`carregar` sem contexto. Função longa deve ser dividida por comportamento, não por blocos aleatórios
de linhas. Funções puras e regras determinísticas devem ser extraídas para permitir teste unitário.

### Enums

Novos enums devem usar constantes em `UPPER_SNAKE_CASE`. Antes de renomear enums existentes,
verifique: serialização, Room, DataStore, JSON, analytics, Worker Cloudflare, nomes persistidos,
compatibilidade de migrations.

---

## 7. Limites de tamanho como sinal de alerta

Os limites abaixo são alertas, não justificativa para criar abstrações inúteis.

- Arquivo acima de 400 linhas: revisar coesão.
- Arquivo acima de 800 linhas: considerar extração obrigatoriamente.
- Arquivo acima de 1.200 linhas: tratar como dívida crítica.
- Função acima de 60 linhas: revisar responsabilidades.
- Composable acima de 150 linhas: revisar componentes e estado.
- Classe com mais de 10 dependências no construtor: revisar orquestração e agregação.

Ao tocar em arquivo acima de 800 linhas: não aumentar seu tamanho sem justificativa; extrair ao
menos uma responsabilidade relacionada, quando isso for pequeno e seguro; caso a extração seja
arriscada ou ampla, registrar ou atualizar uma issue agrupada. Não dividir arquivos apenas para
satisfazer contagem de linhas. Cada extração deve possuir nome, responsabilidade e dependências
claras.

---

## 8. Regra para correção oportunista

Corrija na mesma tarefa quando o problema: estiver na área tocada; não alterar contrato público;
não exigir migration; não mudar comportamento não solicitado; não atingir vários módulos; puder ser
validado; não expandir significativamente a entrega.

Exemplos: nome local confuso, função longa diretamente relacionada, import sem uso, código morto
comprovado, duplicação pequena, comentário incorreto, documentação da área alterada, teste faltante
para a mudança, hardcode que já possui token ou constante oficial, arquivo temporário indevidamente
versionado.

Faça a melhoria na mesma branch e na mesma PR da tarefa principal. Quando a melhoria for relevante,
use commit separado — mas não abra uma PR para cada achado (ver também "Batching" em
`.claude/CLAUDE.md`, seção "Disciplina de Branches e PRs").

---

## 9. Quando abrir ou atualizar uma issue

Não executar silenciosamente quando o problema envolver: renomeação em massa, mudança de módulo,
mudança de package, alteração de contrato público, banco de dados ou migration, APIs, navegação
ampla, arquitetura, muitos consumidores, risco de regressão, arquivos históricos cuja remoção não
esteja comprovada, consolidação de motores concorrentes, mudança extensa de source set.

Antes de abrir uma issue, pesquise se já existe equivalente (`gh issue list --search`). Agrupe
problemas relacionados por domínio — não abrir uma issue para cada arquivo ruim.

A issue deve conter: contexto, evidências, arquivos e módulos afetados, comportamento atual, risco,
destino arquitetural, plano incremental, critérios de aceite, testes necessários, dependências,
estratégia de rollback quando aplicável. Seguir `issue-conventions` para nomenclatura e roteamento.

---

## 10. Documentação

### Estrutura real (validada em 2026-07-16, pós-consolidação)

```
docs_ai/
├── README.md              (índice curto)
├── FUNCIONAL.md            (o que o app faz)
├── TECNICO.md               (como o app é construído/integrado)
├── DESIGN_SYSTEM.md         (tokens/componentes Android)
├── ARQUITETURA/
│   ├── README.md            (visão de sistema, dependências entre módulos)
│   └── MODULOS/              (um doc por módulo Gradle real — 16 arquivos)
├── CONTRATOS/
│   ├── openapi/               (contrato OpenAPI 3.0 — 7 arquivos: 5 por Worker Cloudflare + 2
│   │                            transversais — analytics-events, integrations-api)
│   └── schemas/                (índice de schemas reais: Room, D1, analytics — referencia a origem)
├── RELEASES.md
├── ai/
├── decisions/
├── design-system/            (histórico — conteúdo vigente em DESIGN_SYSTEM.md)
├── functional/                (specs pontuais que não migraram para FUNCIONAL.md)
├── legal/
├── operations/
├── plataforma/                (visão-alvo do ecossistema, pacote v5 — ver `.claude/CLAUDE.md`,
│                                seção "Produtos e Superficies")
├── technical/                  (docs pontuais que não migraram para TECNICO.md/ARQUITETURA/)
├── testing/
└── _archive/
```

Nota: assets de marca (`signallq-*.png`) vivem em `brand/` na raiz do repo, não em `docs_ai/` — é a
fonte da verdade de logo/ícone/favicon, referenciada por build Android e Admin (ver `brand/README.md`).

A árvore `FUNCIONAL.md`/`TECNICO.md`/`ARQUITETURA/`/`CONTRATOS/`/`DESIGN_SYSTEM.md` é o alvo para
conteúdo funcional, técnico, arquitetural, de contrato e de design — não uma exigência de mover
tudo para dentro dela. `ai/`, `decisions/`, `functional/` (residual), `legal/`, `operations/`,
`plataforma/`, `technical/` (residual), `testing/` e `_archive/` continuam existindo para o que não
se encaixa nessa árvore (processo do squad, ADRs, visão-alvo consolidada, planos pontuais, mapas de
campo de equipamento, runbooks, termos legais). Ver `docs_ai/README.md` para o índice completo e a
justificativa de cada pasta residual.

`docs_ai/README.md` deve funcionar como índice, não como uma segunda documentação completa.

### Nomes

Para novos documentos, usar português, minúsculas e `kebab-case`, exceto nomes convencionais:
`README.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `LICENSE`, `ADR-0001-titulo-da-decisao.md`.

Documentos existentes só devem ser renomeados quando todos os links e consumidores forem
atualizados no mesmo trabalho.

### Metadados mínimos

Todo documento ativo relevante deve informar: status, última validação, fonte de verdade, escopo,
responsável ou domínio, documentos substituídos (quando houver).

### Templates de documento (decisão 2026-07-23)

O projeto [SignallQ Design System](https://claude.ai/design/p/2d25d7a1-31b2-4ac3-881f-72dbc8f35a29)
(`templates/`) define a estrutura de seção obrigatória para os três tipos de documento vivo — a
estrutura de metadados acima (status/última validação/fonte de verdade/escopo/responsável) cobre o
mesmo papel do cabeçalho do template (produto/autor/status/revisores/versão) e continua sendo usada
como está, em vez do formato visual do template.

- **Especificação Funcional** (`FUNCIONAL.md`, `functional/*`): Objetivo → Contexto e problema →
  Personas e casos de uso → Histórias de usuário → Fluxo principal → Requisitos funcionais (`RF-NN`)
  → Requisitos não funcionais → Critérios de aceite → Fora de escopo → Métricas de sucesso.
- **Especificação Técnica** (`TECNICO.md`, `technical/*`): Objetivo técnico → Visão geral da solução
  → Modelo de dados → APIs/Endpoints → Integrações e dependências → Segurança e privacidade →
  Performance e escalabilidade → Rollout e observabilidade → Riscos técnicos.
- **Arquitetura** (`ARQUITETURA/README.md`, `ARQUITETURA/MODULOS/*`): Visão geral → Diagrama de
  componentes → Componentes em detalhe → Fluxo de dados principal → Decisões arquiteturais (ADR) →
  Riscos e mitigação.

Documento novo desses três tipos nasce com essa estrutura de seções. Documento existente é
migrado quando tocado (não é obrigatório revisar tudo de uma vez — ver princípio geral, seção 1).

### ADRs

Cada ADR deve possuir número único. Existe histórico de numeração duplicada — antes de criar ADR:
1. liste os ADRs existentes;
2. identifique o maior número válido;
3. use o próximo número;
4. não reutilize números;
5. atualize o índice;
6. preserve links ou redirecionamentos ao renomear documento antigo.

### Arquivamento

Mover para `_archive` quando o documento: foi substituído; descreve produto descontinuado; registra
processo antigo; cita arquitetura que não existe mais; serve apenas como memória histórica.

Adicionar no início do documento arquivado: data de arquivamento, motivo, documento atual que o
substituiu. Não apagar histórico útil sem necessidade.

---

## 11. Limpeza do ambiente

Durante a tarefa, verificar apenas a área relacionada quanto a: arquivos temporários, logs, dumps,
APKs ou builds indevidamente versionados, caches, diretórios de ferramenta, secrets, credenciais,
arquivos duplicados, assets sem uso, scripts abandonados.

Antes de remover um arquivo: buscar referências; verificar build scripts; verificar CI; verificar
documentação; verificar uso por ferramentas e agentes; confirmar que não é mirror intencional.

Os mirrors `.agents/skills/` e `.github/skills/` (sincronização de skill para Codex e hooks do
GitHub) já têm regra própria documentada em `.claude/CLAUDE.md`, seção "Design System" → "Onde fica
cada 'design system'" — não duplicar aqui, só aplicar: fonte canônica é `.claude/skills/`, nunca
editar o mirror direto, resincronizar com `scripts/sync-skills-mirrors.sh` após editar a skill
original (`--check` valida sem escrever).

---

## 12. Validação obrigatória

Após mudanças Kotlin ou Gradle, executar ao menos as validações dos módulos afetados. Para mudança
estrutural relevante, executar (a partir de `android/`, usando `gradlew.bat` no Windows):

```
./gradlew ktlintCheck
./gradlew detekt
./gradlew test
./gradlew assembleDebug
```

Também executar `git diff --check` e `git status` antes de commitar.

Após renomear ou mover arquivos: buscar referências ao nome antigo; conferir imports; conferir
packages; conferir testes; conferir scripts; conferir documentação; conferir CI; verificar que não
existem duas implementações ativas por acidente.

A regra de nunca declarar merge, teste, build ou publicação como concluídos sem verificação real já
está registrada em `.claude/CLAUDE.md`, seção "Disciplina de Branches e PRs" → "Verificação real
antes de declarar" (`gh pr view --json state,merged`, `gh pr checks`, curl direto contra produção) —
esta seção só acrescenta os comandos técnicos específicos de Kotlin/Gradle acima.

---

## 13. Formato obrigatório da entrega

Ao concluir qualquer tarefa, apresentar:

**Entrega principal** — o que foi realizado para atender à solicitação original.

**Melhorias incrementais realizadas** — lista objetiva das melhorias de higiene realmente
executadas.

**Dívidas encontradas e não resolvidas** — problema; impacto; arquivos ou módulos; motivo de não
corrigir agora; issue criada ou atualizada, quando aplicável.

**Arquivos renomeados ou movidos** — formato `caminho antigo` → `caminho novo`.

**Validações executadas** — comandos e resultados reais.

**Pendências ou falhas** — não esconder validações que falharam.

---

## 14. Regra de decisão

Ao encontrar um problema, responda internamente:

1. Está diretamente relacionado à área tocada?
2. É pequeno ou médio?
3. O comportamento será preservado?
4. Não altera contratos, banco ou navegação ampla?
5. Pode ser validado objetivamente?
6. Não exige muitos arquivos ou módulos?
7. Não cria outra abstração genérica?
8. Não desvia da entrega principal?

Se todas as respostas forem "sim", corrija na mesma tarefa. Se alguma resposta for "não", registre
ou atualize uma issue e prossiga com a entrega original.
