# Arquitetura da SignallQ Platform

**Status:** ativo · **Versão:** 5.0 · **Data:** 17/07/2026 · **Substitui:** `v4__SignallQ_Platform_Arquitetura_v4.txt` (fundido com o detalhe Android de `v3__SignallQ_Platform_Arquitetura_Android_v3.txt`)

Visão consolidada dos produtos, aplicações, serviços e limites de responsabilidade da SignallQ Platform. Esta versão v5 reincorpora o detalhe de arquitetura Android — camadas, módulos, fluxo de medição e serviços de plataforma (Auth, Entitlements/Billing, Agenda, Pix/recibos) — que o v4 havia deixado raso, e aplica os nomes e decisões do dicionário canônico.

> **Fonte de nomes e decisões transversais:** `00_CANONICO_v5.md`. Em qualquer divergência, o canônico prevalece.

## Estado atual vs. Alvo

Todo componente abaixo é rotulado ATUAL (existe e está validado no código do repo `gmmattey/linka-android` em 17/07/2026) ou ALVO (proposta desta arquitetura, ainda não construída).

| Componente | Estado | Fato |
|---|---|---|
| SignallQ (android-consumer) | ATUAL | `gmmattey/linka-android`. versionName 0.26.0, versionCode 62. 16 módulos Gradle (app + 6 core + 9 features). Package `io.signallq.app`. |
| SignallQ Admin (admin-web) | ATUAL | Diretório `SignallQ Admin/` no monorepo. React 19 + Vite 6 + TS 5.8 + Tailwind 4. Pacote `signallq-admin` v0.1.0. |
| Workers Cloudflare | ATUAL (5 workers) | `ai-diagnosis-worker`, `signallq-admin-worker`, `signallq-diagnostic-worker`, `signallq-privacy-worker`, `game-latency-probe-worker`. |
| Telemetria | ATUAL: Firebase | Firebase Analytics + Crashlytics. Sem Realtime DB, sem pipeline Cloudflare Queue→D1. |
| SignallQ Pro (android-pro) | ALVO | Não existe. App novo, `io.signallq.pro`, Firebase/Play separados. |
| Portal (portal-web) | ALVO | Não existe. Funde `linka-webapp` + `linka-speedtest`. |
| Monorepo `signallq-platform` | ALVO | Não existe. Hoje o código vive em `linka-android` mais repos separados. |
| Pipeline de telemetria Cloudflare | ALVO | Telemetry Worker → Queue → D1/Analytics Engine é proposta. |
| SignallQ Nethal | ALVO | Hoje repo separado `gmmattey/nethal`; ainda sem driver de produção estável. |

**Identificadores técnicos a nunca renomear** (parecem marca, são técnicos): `io.signallq.app`, repo `gmmattey/linka-android`, DB `linkaKotlin.db`, DataStore `linkaPreferencias`, canais `linka_*`. O nome-alvo `signallq-platform` é do monorepo futuro, não substitui esses identificadores.

---

## 1. Objetivo

Definir a arquitetura de referência para a evolução conjunta de SignallQ, SignallQ Pro, Portal, SignallQ Admin e SignallQ Nethal dentro do monorepo-alvo `signallq-platform`. O documento parte do código que já funciona (medição, diagnóstico, painel Admin) e descreve como separar composição de produto, domínio e infraestrutura sem jogar a base fora.

## 2. Público e uso

Destinado a desenvolvimento, arquitetura, produto, segurança, testes e agentes de IA que atuem no repositório. Não é decisão de negócio: preços, domínio do portal e provedor de identidade permanecem pendências (seção 10).

## 3. Escopo

- Aplicações Android (consumer e Pro), web pública (Portal) e painel administrativo (Admin).
- Cloudflare Workers, D1, filas, telemetria e armazenamento abstraído por StorageProvider.
- Módulos compartilhados Android e estratégia de migração dos repositórios existentes.
- Limites entre produto público, operação interna e ferramenta técnica (Nethal).

## 4. Portfólio

| Componente | Público | Papel | Canal | Estado |
|---|---|---|---|---|
| SignallQ | Consumidor final | Diagnóstico, testes, histórico e recomendações de conectividade. | Google Play | ATUAL |
| SignallQ Pro | Técnicos e prestadores | Clientes, locais, visitas, medições por ambiente, evidências, laudos e cobrança. | Google Play | ALVO |
| Portal | Público geral | Speedtest web, conteúdo, comparação, preços, downloads e páginas legais. | Web pública | ALVO |
| SignallQ Admin | Operação autorizada | Usuários, assinaturas, telemetria, diagnósticos, conteúdo e configuração. | Web restrita | ATUAL |
| SignallQ Nethal | Equipe técnica | Validação de equipamentos, drivers, fingerprints e capacidades. | Distribuição interna | ALVO |

## 5. Arquitetura lógica

```
Clientes
├── SignallQ (Android)            [ATUAL]
├── SignallQ Pro (Android)        [ALVO]
├── Portal (web público)          [ALVO]
├── SignallQ Admin (web restrita) [ATUAL]
└── SignallQ Nethal (interno)     [ALVO]
        │
        ▼
Cloudflare Edge (Workers)
├── API Gateway / Workers
├── Auth e Entitlements
├── Telemetry Ingestion           [ALVO: pipeline Queue→D1]
├── Serviços profissionais (Pro)
├── Serviços de Admin             [ATUAL: signallq-admin-worker]
├── Diagnóstico / IA / Config     [ATUAL: ai-diagnosis + signallq-diagnostic]
└── Serviços públicos (Portal)
        │
        ├── D1                     [ATUAL: 13 tabelas do Admin]
        ├── Queues                 [ALVO]
        ├── Analytics Engine       [ALVO]
        └── StorageProvider
              ├── Local / Android SAF   [padrão MVP]
              ├── Nuvem do técnico
              └── R2 hospedado (add-on pago)
```

Regra de storage (canônico §6): o padrão do MVP é **local-first + Android SAF**; `StorageProvider` abstrai tudo. R2 é apenas add-on hospedado pago, nunca o storage padrão.

## 6. Estrutura do monorepo (árvore canônica)

Esta é a árvore canônica única (`00_CANONICO_v5.md` §4). Substitui as três árvores divergentes das versões anteriores. Nomes de módulo são **hierárquicos com `/`** (`mobile/core/network`), nunca concatenados (`core-network`).

```
signallq-platform/
├── apps/
│   ├── android-consumer/     # SignallQ  (ATUAL: gmmattey/linka-android)
│   ├── android-pro/          # SignallQ Pro  (ALVO)
│   ├── portal-web/           # Portal público  (ALVO: funde linka-webapp + linka-speedtest)
│   ├── admin-web/            # SignallQ Admin  (ATUAL: "SignallQ Admin/")
│   └── signallq-nethal/      # Lab interno  (ALVO: hoje repo gmmattey/nethal)
├── mobile/
│   ├── core/{designsystem, network, database, preferences, permissions, files, sync, analytics, telephony}
│   └── features/{auth, customers, appointments, visits, environments, measurements, evidence,
│                 reports, payments, history, settings, speedtest, wifi, devices, dns, fibra, diagnostico}
├── domain/
│   └── {customer, appointment, visit, diagnostics, reporting, billing, entitlement}
├── network-hardware/
│   └── {core, engine, security, catalog, drivers}
├── backend/
│   ├── workers/{auth, sync, billing, report, admin, telemetry, diagnosis, privacy, game-latency}
│   ├── d1/{migrations, seeds}
│   ├── queues/
│   └── contracts/
├── web/
│   └── packages/{ui, diagnostics, speedtest, reporting}
├── packages/
│   └── {telemetry-schema, api-contracts, shared-types}
├── docs/
│   └── {product, technical, architecture, modules, api, qa, decisions, archive}
└── tooling/
    └── {gradle, scripts, github-actions}
```

### 6.1 Os 5 workers reais e o mapa para `backend/workers/`

Hoje existem **5 workers Cloudflare** (ATUAL) — a documentação v4 só citava 2. O mapeamento para a árvore-alvo:

| Worker atual | Estado | Destino em `backend/workers/` |
|---|---|---|
| `signallq-admin-worker` | ATUAL | `admin` |
| `ai-diagnosis-worker` | ATUAL | `diagnosis` (consolidar com o abaixo) |
| `signallq-diagnostic-worker` | ATUAL | `diagnosis` (consolidar) |
| `signallq-privacy-worker` | ATUAL | `privacy` |
| `game-latency-probe-worker` | ATUAL | `game-latency` |
| `auth`, `sync`, `billing`, `report`, `telemetry` | ALVO | novos, ainda não existem |

## 7. Regras arquiteturais

1. Aplicações não acessam tabelas D1 diretamente; toda escrita passa por Workers e contratos versionados (`packages/api-contracts`, ALVO).
2. Eventos de telemetria usam um envelope único em `dot.case`, independente da aplicação de origem (catálogo canônico §3).
3. SignallQ Admin nunca compartilha sessão ou privilégios com usuários dos aplicativos.
4. SignallQ Nethal não é produto comercial e não define contratos incompatíveis com `network-hardware`.
5. Arquivos e evidências são abstraídos por `StorageProvider`; o domínio não depende de R2.
6. Funcionalidades offline usam fila local idempotente e reconciliação posterior.
7. Toda operação destrutiva ou sensível produz evento de auditoria.
8. `android-consumer` e `android-pro` nunca dependem um do outro; `domain` não depende de Android/Firebase/Cloudflare/UI; features nunca dependem de outra feature; drivers não importam telas/ViewModels; design system sem regra de negócio.

## 8. Arquitetura Android (detalhe reincorporado do v3)

O caminho é evoluir `linka-android` para `android-consumer` dentro do monorepo, preservando os motores de Wi-Fi, speedtest, DNS, dispositivos, fibra, diagnóstico e geração de PDF, mas separando composição de produto, domínio e infraestrutura. O SignallQ Pro nasce como app profissional distinto — não flavor.

### 8.1 Camadas internas

| Camada | Responsabilidade | Não deve fazer |
|---|---|---|
| UI | Renderizar estado, emitir eventos e navegar | Acessar Room, Retrofit, APIs Wi-Fi do Android ou decidir regra técnica |
| Application | Executar casos de uso e coordenar uma ação do usuário | Conhecer detalhes de Compose ou implementação de API |
| Domain | Entidades, contratos, políticas, score e validações | Depender de `Context`, Firebase ou banco |
| Data | Repositories, mapeamento, cache, APIs e drivers | Conter regra de apresentação |
| Platform | Wrappers Android e SDKs externos | Vazar tipos do SDK para o domínio |

Dependências apontam para dentro: UI depende de casos de uso; casos de uso dependem de contratos; infraestrutura implementa os contratos. Regras de classificação, score, diagnóstico e relatório devem ser Kotlin puro (domínio sem Android quando possível).

### 8.2 Fronteiras dos dois produtos

| Capacidade | Consumer | Pro | Compartilhamento |
|---|---|---|---|
| Speedtest / DNS / Wi-Fi / móvel | Sim | Sim | Motor e modelos compartilhados |
| Diagnóstico local e IA | Linguagem simplificada | Linguagem técnica e evidenciável | Contrato e motor compartilhados; prompt/apresentação separados |
| Histórico | Por usuário/dispositivo | Por cliente, local e visita | Infra de persistência compartilhável; domínio separado |
| Anúncios | Possível | Não | Exclusivo do `android-consumer` |
| Clientes, locais e visitas | Não | Sim | Exclusivo do Pro |
| Fotos e evidências | Limitado | Sim | Exclusivo do Pro |
| Antes e depois | Não | Sim | Exclusivo do Pro |
| Laudo | Compartilhamento simples | PDF profissional com marca e assinatura | Engine de PDF compartilhada; templates separados |
| SignallQ Nethal / equipamentos | Consulta orientada ao consumidor | Ferramenta de trabalho e intervenção | Drivers e capacidades compartilhados |

### 8.3 Fluxo técnico de uma medição

1. **Captura** — adapter Android coleta RSSI, frequência, BSSID, IP, DNS, telefonia ou resultado do teste.
2. **Normalização** — `mobile/features/measurements` converte dados brutos em modelos estáveis, sem dependência do SDK.
3. **Classificação** — `domain/diagnostics` aplica faixas, confiança, contexto do ambiente e regras locais.
4. **Persistência** — o repositório salva a `MeasurementSession` ligada a `Visit` e `Environment` (nomes canônicos §2).
5. **Sincronização** — o outbox de `Sync` publica o evento no backend quando houver rede.
6. **Apresentação** — o Pro mostra detalhe técnico; o Consumer, explicação simples.
7. **Relatório** — `domain/reporting` monta blocos a partir de snapshots imutáveis da visita.

No Pro, todo agregado usa UUID gerado no dispositivo, o que permite criar cliente, visita e medição offline sem depender do backend para reservar ID. Conflito é resolvido por versão do registro e `updatedAt`; evidência já emitida em laudo nunca é sobrescrita silenciosamente.

### 8.4 Serviços de plataforma (reincorporados)

**Autenticação e identidade.** D1 é armazenamento, não provedor de autenticação. A autenticação é implementada por um **Auth Worker** (ALVO) com validação Google, credenciais locais e emissão/renovação de sessão.

| Componente | Responsabilidade |
|---|---|
| Credential Manager | Obter credencial Google no Android |
| Auth Worker | Validar Google, criar conta local, hash de senha, tokens, recuperação e revogação |
| Cloudflare D1 | Contas, identidades vinculadas, sessões, verificações e perfis |
| Android Keystore | Proteger tokens e segredos locais |
| API Gateway / Workers | Autorizar requests por escopo e conta |

Tabelas de identidade (nomes canônicos §2): `account`, `identity_provider`, `session`, mais verificações e perfis. Tokens de acesso curtos e refresh rotativos por dispositivo; vinculação segura de identidades para evitar contas duplicadas pelo mesmo e-mail.

**Entitlements e Google Play Billing.** O aplicativo consulta *entitlements*, não flags booleanas espalhadas. A Play Store vende; o backend valida e traduz a compra em direitos de acesso.

| Entidade | Campos principais |
|---|---|
| `subscription` | accountId, source, productId, basePlanId, status, validUntil |
| `entitlement` | accountId, capability, status, validUntil |
| `billing_event` | purchaseTokenHash, eventType, occurredAt, payloadVersion |
| `plan_limit` | plan, capability, numericLimit, resetPeriod |

**Agenda e integração externa.** `Appointment` pertence ao domínio Pro; o calendário externo é um adaptador. No MVP1, o app usa Calendar Intent (`ACTION_INSERT`, sem permissão ampla); no MVP2, um conector Google Calendar mantém `externalEventId` e sincronização.

**Pagamentos, Pix e recibos.** Chave Pix protegida localmente e criptografada em repouso no backend quando sincronizada. Payload EMV/BR Code e QR Code gerados localmente. Recibo imutável com status `ISSUED`, `CANCELLED` ou `REPLACED`. O MVP não armazena extrato bancário nem acessa conta financeira. Nomes canônicos: `PixCharge`/`pix_charge`, `PixProfile`/`pix_profile`.

### 8.5 Integração Nethal e equipamentos

A integração com roteadores, ONTs e mesh é um subsistema de adapters por capacidade em `network-hardware`, não uma coleção de `if`s por modelo espalhada nas telas.

| Contrato | Exemplos |
|---|---|
| DeviceDiscovery | Localizar gateway, fabricante, modelo e método provável de acesso |
| DeviceSession | Autenticar, manter sessão e expirar credenciais |
| ReadCapabilities | Ler WAN, Wi-Fi, canais, clientes, fibra e firmware |
| WriteCapabilities | Trocar senha, canal, banda ou reiniciar quando suportado |
| CapabilityMatrix | Declarar o que foi validado, inferido ou não suportado por modelo/firmware |
| EvidenceSnapshot | Registrar estado antes/depois para anexar ao laudo |

Regra de segurança: credenciais de equipamento ficam criptografadas no Android Keystore e não entram em logs, Analytics, Crashlytics ou PDF. Permanecer conectado é opt-in e limitado à sessão/local. Safety Guard obrigatório antes de qualquer comando de escrita.

## 9. Ambientes e deploy

| Ambiente | Finalidade | Dados |
|---|---|---|
| dev | Desenvolvimento local e branches | Sintéticos |
| staging | Integração, testes e homologação | Massa mascarada ou sintética |
| production | Operação pública | Dados reais com controles e retenção |

Cada produto mantém `applicationId`, assinatura, listing, versionCode, release notes e rollout separados. Releases nascem de tags independentes (identidade e tags canônicas em `00_CANONICO_v5.md` §7): `consumer/android/vX.Y.Z`, `pro/android/vX.Y.Z`, `portal-web/vX.Y.Z`, `admin-web/vX.Y.Z`, `signallq-nethal/vX.Y.Z`, `worker-<nome>/vX.Y.Z`. `versionName` fica em `0.x.y` enquanto em trilha de teste; `1.0.0` reservado ao primeiro publish em `production`.

## 10. Migração dos ativos atuais

| Origem | Destino | Decisão |
|---|---|---|
| `linka-android` | `apps/android-consumer` + módulos compartilhados | Preservar histórico, extrair módulos e manter paridade antes do refactor |
| `SignallQ Admin/` dentro de `linka-android` | `apps/admin-web` + `backend/workers/admin` | Mover e adaptar sem reescrita total |
| `linka-speedtest` | `apps/portal-web` | Fundir motor e experiência web superior |
| `linka-webapp` | `apps/portal-web` | Fundir conteúdo, PWA e componentes úteis |
| `nethal` | `apps/signallq-nethal` + `network-hardware` | Renomear, internalizar e extrair drivers/capabilities |

Estratégia de fusão detalhada, classificação por ativo e plano em fases estão em `02_SignallQ_Platform_Especificacao_Tecnica_v5.md`.

## 11. Decisões pendentes

Herdadas do v4, ainda abertas (o v5 as torna visíveis, não as inventa — `00_CANONICO_v5.md` §8):

1. **Preço do Pro** — mensal/anual sem valor definido em nenhum documento.
2. **Domínio do portal** — `signallq.app`, sujeito a aquisição.
3. **Provedor de identidade e estratégia de recuperação de conta.**
4. **Conectores de nuvem** além do Android SAF.
5. **Política de retenção por tipo de dado e plano.**
6. **ADR-001..008** citados mas inexistentes — o v5 abre os stubs em `docs/decisions/`.
7. **Contratos OpenAPI** referenciados (`packages/api-contracts`) mas inexistentes — v5 define a estrutura, não os contratos completos.

---

## Documentos relacionados

- `00_CANONICO_v5.md` — dicionário canônico de nomes e decisões (prevalece sobre este).
- `02_SignallQ_Platform_Especificacao_Tecnica_v5.md` — leitura dos repos, consolidação, backend, dados, plano de migração em fases e ADRs.
- `03_SignallQ_Governanca_GitHub_e_Monorepo_v5.md` — proteção de main, PRs, CI por impacto, releases por produto e contrato de agentes.
