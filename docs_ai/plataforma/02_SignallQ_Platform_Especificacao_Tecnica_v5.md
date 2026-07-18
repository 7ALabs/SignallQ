# SignallQ Platform — Especificação Técnica Completa

**Status:** ativo · **Versão:** 5.0 · **Data:** 17/07/2026 · **Substitui:** `v4__SignallQ_Platform_Especificacao_Tecnica_Completa_v4.txt`

Arquitetura-alvo, fusão de repositórios e plano de migração da SignallQ Platform. Documento oficial de produto e engenharia. Esta v5 aplica os nomes canônicos (tabelas D1, `StorageProvider`, os 5 workers reais) e marca item a item o que é atual e o que é alvo.

> **Fonte de nomes e decisões transversais:** `00_CANONICO_v5.md`. Em qualquer divergência, o canônico prevalece.

## Estado atual vs. Alvo

| Componente | Estado | Fato |
|---|---|---|
| SignallQ (android-consumer) | ATUAL | `gmmattey/linka-android`. versionName 0.26.0, versionCode 62. 16 módulos Gradle. Package `io.signallq.app`. |
| SignallQ Admin (admin-web) | ATUAL | `SignallQ Admin/` no monorepo. React 19.0.1 + Vite 6 + TS 5.8 + Tailwind 4 + Lucide + Motion + Recharts + Vitest. Pacote `signallq-admin` v0.1.0. |
| Workers Cloudflare | ATUAL (5 workers) | `ai-diagnosis-worker`, `signallq-admin-worker`, `signallq-diagnostic-worker`, `signallq-privacy-worker`, `game-latency-probe-worker`. |
| D1 do Admin | ATUAL (13 tabelas) | `diagnostic_sessions`, `ai_usage`, `admin_settings`, `system_errors`, `admin_users`, `admin_sessions`, `auth_rate_limit`, `alerts`, `feature_flags`, `feature_flag_audit`, `analytics_events`, `play_console_tracks`, `system_health_snapshots`. |
| Telemetria | ATUAL: Firebase | Firebase Analytics + Crashlytics. Sem Realtime DB, sem pipeline Cloudflare Queue→D1. |
| Modelo de dados D1 "Pro" | ALVO (greenfield) | Nenhuma das tabelas Pro existe hoje. 100% novo sobre o D1 do Admin. |
| SignallQ Pro / Portal / Nethal / Monorepo | ALVO | Não existem. |

**Identificadores técnicos a nunca renomear:** `io.signallq.app`, repo `gmmattey/linka-android`, DB `linkaKotlin.db`, DataStore `linkaPreferencias`, canais `linka_*`.

---

## Sumário

1. Decisão arquitetural
2. Leitura dos repositórios e produtos atuais
3. Estratégia de consolidação
4. Arquitetura lógica
5. Estrutura do monorepo
6. Arquitetura Android
7. Backend Cloudflare
8. Dados e sincronização
9. Integrações
10. Segurança
11. Observabilidade
12. Testes e qualidade
13. CI/CD e releases
14. Plano de migração
15. Critérios de desativação
16. Riscos e decisões
17. ADRs iniciais
18. Portal Web
19. Papel e desativação do SignallQ Nethal

## 1. Decisão arquitetural

Criar o monorepo `signallq-platform` (ALVO) como única fonte ativa. Migrar código útil preservando histórico, estabilizar dois apps Android independentes — SignallQ e SignallQ Pro — compartilhando bibliotecas, e arquivar os repositórios antigos após gates objetivos. SignallQ Nethal vira camada de capacidade de hardware, não terceiro produto comercial; permanece apenas como app interno de validação.

### 1.1 Restrições fundamentais

- Não usar product flavors para fingir separação de produtos.
- Não copiar código sem rastreabilidade de origem.
- Não migrar documentação antiga como verdade automática.
- Não liberar driver experimental no app de produção.
- Não manter dois motores de speedtest ou diagnóstico sem proprietário claro.
- Não arquivar repositório antes de paridade funcional, build e rollback.

## 2. Leitura dos repositórios e produtos atuais

Cada leitura é marcada ATUAL (fato verificado no código) ou ALVO (destino da migração).

### 2.1 `gmmattey/linka-android` — ATUAL

Base mais madura, tronco técnico inicial. README declara Kotlin, Jetpack Compose, Material 3, MVVM/StateFlow, Hilt, Room, DataStore, WorkManager, Firebase e Cloudflare Worker. O Gradle registra **16 módulos**: `app`, seis módulos core e nove features. versionName 0.26.0, versionCode 62.

### 2.2 SignallQ Admin (dentro de `linka-android`) — ATUAL

Já existe, no diretório `SignallQ Admin/`. Não é projeto futuro nem deve ser refeito do zero. Aplicação web **React 19 + Vite + TypeScript**, com Tailwind, Lucide, Motion, Recharts e testes Vitest. O worker `integrations/cloudflare/signallq-admin-worker` é o backend do painel, e o D1 do Admin tem **13 tabelas reais** (ver seção 7.2).

Decisão: classificar como **ADAPT + MOVE**. Migra para `apps/admin-web` (ALVO) preservando histórico e funcionalidades validadas; o `signallq-admin-worker` vai para `backend/workers/admin`. Antes de qualquer redesenho, roda matriz de paridade para login, métricas, usuários, versões, eventos, feedbacks, custos de IA, diagnósticos e telemetria já implementados. O Admin continua interno e separado dos apps Android; não vai à Play Store e não compartilha credenciais administrativas com contas de técnicos.

### 2.3 `gmmattey/nethal` — ATUAL (repo separado) → ALVO (`apps/signallq-nethal`)

Já adota a direção correta: arquitetura por Vendor → Platform → Protocol → Authentication Strategy → Driver Family → Profile → Capability. Possui módulos de discovery, fingerprint, capability, auth, consent, telemetry, scheduling, history, ferramentas e famílias de drivers. O próprio status informa que **ainda não há driver de produção estável** (por isso o componente é ALVO).

### 2.4 `gmmattey/linka-webapp` — ATUAL (repo separado) → ALVO (`apps/portal-web`)

PWA React/TypeScript com Vite, Vitest, Recharts, jsPDF, html2canvas, PWA e Cloudflare Pages. Vira parte do Portal, preservando a experiência web e a infraestrutura de build, adotando pacotes compartilhados para regras, modelos e geração documental quando fizer sentido.

### 2.5 `gmmattey/linka-speedtest` — ATUAL (repo separado) → funde no Portal

Versão isolada da feature de velocidade: motor de medição, classificador, histórico e exportação PDF. Forte sobreposição com `linka-webapp`. Tratado como **fonte de código a comparar**, não como produto a manter.

### 2.6 Repositórios excluídos da fusão

Orbit, Esquilo Invest, Vera Insights, Ei Raiz e demais projetos não pertencem ao domínio SignallQ e ficam fora do monorepo.

## 3. Estratégia de consolidação

### 3.1 Método de migração

1. Criar `signallq-platform` com proteção de main e CI mínimo.
2. Importar cada repositório em branch temporária com `git filter-repo` ou `git subtree` para preservar commits e autoria.
3. Mover arquivos para o diretório-alvo no mesmo commit de importação.
4. Criar relatório automático de origem por módulo.
5. Executar build e testes antes de qualquer refatoração.
6. Migrar SignallQ Admin e `signallq-admin-worker` como uma unidade, validando build, autenticação, rotas, dashboards e dados antes de alterar contratos.
7. Refatorar em PRs pequenos por fronteira de domínio.
8. Declarar paridade e congelar escrita no repositório antigo.
9. Arquivar repositório somente após dois ciclos de release estáveis.

### 3.2 Classificação de cada ativo

| Classe | Critério | Ação |
|---|---|---|
| REUSE | Funciona, tem testes e encaixa na arquitetura-alvo | Migrar com poucas alterações |
| ADAPT | Funciona, mas acopla UI, pacote ou produto antigo | Migrar e adaptar após paridade |
| MERGE | Duas implementações úteis do mesmo domínio | Escolher contrato único e portar melhores trechos/testes |
| REWRITE | Valor comprovado, estrutura insegura ou inviável | Reescrever apoiado em testes de caracterização |
| RETIRE | Duplicado, obsoleto ou sem valor demonstrável | Não migrar; registrar justificativa |
| QUARANTINE | Driver ou integração sem validação física | Migrar isolado, desabilitado por feature flag |

**Regra de ouro:** primeiro migrar sem melhorar; depois consolidar. Misturar migração com grande refatoração torna regressões impossíveis de rastrear.

Destino por área do `linka-android` (ATUAL → ALVO):

| Área atual | Destino | Tratamento |
|---|---|---|
| `app` | `apps/android-consumer` | Migrar shell e navegação; retirar dependências que devem ser compartilhadas |
| `core/network` | `mobile/core/network` | Reaproveitar; separar contratos Android de lógica pura |
| `core/database` | `mobile/core/database` | Reusar padrões Room; bancos/schemas por produto |
| `core/datastore` | `mobile/core/preferences` | Reusar wrappers e chaves com namespaces novos |
| `core/permissions` | `mobile/core/permissions` | Reusar e adequar às jornadas Pro |
| `core/telephony` | `mobile/core/telephony` | Consumer; compartilhar só contratos úteis |
| `core/recommendation` | `domain/diagnostics` | Consolidar regras, remover motores paralelos |
| `feature/speedtest` | `mobile/features/speedtest` | Feature compartilhável com shells próprios |
| `feature/wifi,devices,dns,fibra` | `mobile/features/*` | Reusar conforme capabilities e permissões |
| `feature/diagnostico,history,settings` | `mobile/features/*` | Separar domínio compartilhado de UI específica |
| SignallQ Admin | `apps/admin-web` | Migrar como app web do monorepo |
| Workers Cloudflare | `backend/workers/*` | Migrar e padronizar contratos, ambientes e deploy |

## 4. Arquitetura lógica

Clean Architecture pragmática e modularização por capacidade. Apps dependem de features; features dependem de `domain` e `core`; hardware e integrações são acessados por contratos. Nenhuma feature depende diretamente de outra feature.

| Camada | Responsabilidade |
|---|---|
| Apps | Shell, branding, navegação, DI e configuração de produto |
| Presentation | Compose UI, ViewModels, estados e eventos |
| Domain | Entidades, casos de uso, políticas e interfaces |
| Data | Room, D1 API, DataStore, arquivos e sincronização |
| Platform | Android APIs, permissões, calendário, billing e compartilhamento |
| Network Hardware | Discovery, fingerprint, drivers, capabilities e safety guard |
| Backend | Auth, sync, billing validation, reports, telemetry e admin |

## 5. Estrutura do monorepo

Árvore canônica única (`00_CANONICO_v5.md` §4). Nomes hierárquicos com `/`, nunca concatenados.

```
signallq-platform/
├── apps/{android-consumer, android-pro, portal-web, admin-web, signallq-nethal}
├── mobile/
│   ├── core/{designsystem, network, database, preferences, permissions, files, sync, analytics, telephony}
│   └── features/{auth, customers, appointments, visits, environments, measurements, evidence,
│                 reports, payments, history, settings, speedtest, wifi, devices, dns, fibra, diagnostico}
├── domain/{customer, appointment, visit, diagnostics, reporting, billing, entitlement}
├── network-hardware/{core, engine, security, catalog, drivers}
├── backend/
│   ├── workers/{auth, sync, billing, report, admin, telemetry, diagnosis, privacy, game-latency}
│   ├── d1/{migrations, seeds}
│   ├── queues/
│   └── contracts/
├── web/packages/{ui, diagnostics, speedtest, reporting}
├── packages/{telemetry-schema, api-contracts, shared-types}
├── docs/{product, technical, architecture, modules, api, qa, decisions, archive}
└── tooling/{gradle, scripts, github-actions}
```

### 5.1 Regras de dependência

- `android-consumer` e `android-pro` nunca dependem um do outro.
- `domain` não depende de Android, Firebase, Cloudflare ou UI.
- Drivers não importam telas ou ViewModels.
- Design system não contém regra de negócio.
- Backend contracts são versionados e testados por consumidor.

### 5.2 SignallQ Admin na arquitetura-alvo

`apps/admin-web` será o painel operacional da plataforma. Consumirá APIs administrativas versionadas e nunca acessará D1 diretamente pelo navegador. Todas as leituras e mutações passam por `backend/workers/admin`, com RBAC, logs de auditoria e escopos mínimos.

Responsabilidades: usuários e assinaturas; versões e adoção; eventos e funis; feedbacks; falhas e diagnósticos; custo e consumo de IA; telemetria sanitizada; catálogos e regras remotas; suporte operacional. Funções ainda não existentes serão adicionadas incrementalmente, sem apagar o que já funciona. O Admin não é biblioteca compartilhada com os apps móveis; o reaproveitamento ocorre em contratos TypeScript, esquemas, cliente HTTP, tokens visuais e componentes web compatíveis. Código experimental fica em módulo explícito e desligado por padrão.

## 6. Arquitetura Android

| Tema | Decisão |
|---|---|
| UI | Jetpack Compose + Material 3 customizado com tokens de marca |
| Estado | Unidirectional data flow; StateFlow para estado persistente da tela |
| DI | Hilt na borda do app; construtores puros no domínio |
| Persistência | Room para dados estruturados, DataStore para preferências/sessões leves |
| Background | WorkManager para sync, upload, limpeza e retries |
| Navegação | Navigation Compose, rotas tipadas e deep links controlados |
| Arquivos | Storage privado; ContentProvider somente para compartilhamento |
| Módulos | Gradle convention plugins e catálogo de versões único |

Tokens de marca por produto (canônico §5): SignallQ (consumer) usa primary `#5B21D6` / secondary `#2851B8` (ATUAL, de `SignallQTheme.kt`). SignallQ Pro usa identidade **azul**, primary `#0B6CFF` (marca), com ciano `#006B76` e roxo `#6558E8` de apoio (atualizado 2026-07-18; fonte visual viva: projeto `77a19317`, ver Canônico §5.2). Os tokens `#6C2BFF` e a paleta teal-dominante anterior do Pro (`#006B73` como primary) estão mortos.

### 6.1 Bancos locais

Consumer e Pro não compartilham o mesmo arquivo Room. Bibliotecas podem compartilhar entidades de domínio, mas cada app mantém schema, migrations e retenção próprios. O Pro utiliza IDs UUID e outbox de sincronização.

## 7. Backend Cloudflare

### 7.1 Workers

Hoje existem **5 workers reais** (ATUAL). O mapa para `backend/workers/` (canônico §4):

| Worker atual | Destino | Estado |
|---|---|---|
| `signallq-admin-worker` | `admin` | ATUAL |
| `ai-diagnosis-worker` + `signallq-diagnostic-worker` | `diagnosis` (consolidar) | ATUAL |
| `signallq-privacy-worker` | `privacy` | ATUAL |
| `game-latency-probe-worker` | `game-latency` | ATUAL |

Workers-alvo adicionais (ALVO), com responsabilidade prevista:

| Worker | Responsabilidade |
|---|---|
| `auth` | Google token exchange, e-mail/senha, verificação, refresh e revogação |
| `sync` | Delta sync, conflitos, anexos e idempotência |
| `billing` | Validação Google Play, webhooks e entitlements |
| `report` | Verificação pública opcional, snapshots e geração server-side futura |
| `telemetry` | Ingestão sanitizada e allowlist de eventos |

### 7.2 D1 e armazenamento

| Tecnologia | Uso |
|---|---|
| D1 | Contas, perfis, clientes, visitas, metadados, pagamentos, recibos, entitlements e sync cursors |
| StorageProvider | Fotos, logos, PDFs e anexos no provider configurado; D1 mantém somente metadados |
| KV | Configurações remotas pequenas, feature flags e cache |
| Queues | Processamento assíncrono de anexos, documentos e telemetria |
| Secrets | Chaves Google, billing, e-mail e provedores de IA |

**D1 ATUAL (Admin, 13 tabelas):** `diagnostic_sessions`, `ai_usage`, `admin_settings`, `system_errors`, `admin_users`, `admin_sessions`, `auth_rate_limit`, `alerts`, `feature_flags`, `feature_flag_audit`, `analytics_events`, `play_console_tracks`, `system_health_snapshots`.

**Modelo de dados D1 "Pro" — ALVO (greenfield).** Nenhuma tabela abaixo existe hoje; é 100% novo sobre o D1 do Admin. Grupos e tabelas (nomes conforme canônico §2 para os agregados de domínio):

| Grupo | Tabelas |
|---|---|
| Identidade | `account`, `identity_provider`, `session`, verificações e resets |
| Profissional | `professional_profiles`, `pix_profile`, `report_templates` |
| Operação | `customer`, `customer_contacts`, `service_location`, `appointments`, `visits`, `environment` |
| Diagnóstico | `measurement_session`, `measurement_point`, `evidence`, `recommendations`, `device_sessions` |
| Documentos | `reports`, `report_versions`, `receipts`, `receipt_replacements` |
| Financeiro | `payments`, `payment_allocations`, `pix_charge`, `subscriptions`, `entitlements` |
| Sincronização | `sync_devices`, `sync_cursors`, `change_log`, `idempotency_keys` |
| Governança | `audit_log`, `feature_flags`, `telemetry_events_sanitized` |

Isolamento por `professional_id`, migrações versionadas (expand-migrate-contract), índices e integridade, backup e restauração testados. Detalhamento de campos, índices e retenção fica em documento de modelo de dados próprio (referência do pacote v4: Modelo de Dados D1).

## 8. Dados e sincronização

### 8.1 Offline-first

1. Toda escrita nasce localmente em transação Room.
2. Registro recebe `clientId`, `updatedAt`, `revision` e `syncState`.
3. Outbox agenda operação idempotente.
4. Worker envia lote quando houver rede adequada.
5. Servidor aplica regra de versão e devolve cursor.
6. Cliente confirma, atualiza IDs remotos e remove outbox.

### 8.2 Conflitos

| Tipo | Estratégia |
|---|---|
| Campos simples | Last-write-wins apenas quando seguro e com audit trail |
| Notas e listas | Merge por item/UUID |
| Laudo/recibo emitido | Imutável; nunca merge |
| Pagamento | Operação idempotente; divergência exige revisão |
| Foto/anexo | Upload resumível; metadado separado do binário |
| Exclusão | Tombstone sincronizado com retenção |

## 9. Integrações

| Integração | MVP | Implementação |
|---|---|---|
| Google Sign-In | MVP1 | Credential Manager + validação de ID token no `auth` worker |
| Google Play Billing | MVP1 | Billing Library + backend verification + RTDN quando configurado |
| Calendário Android | MVP1 | Intent `ACTION_INSERT` sem permissão ampla |
| Google Calendar API | MVP2 | OAuth incremental, agenda dedicada e `externalEventId` |
| WhatsApp | MVP1 | Share/deep link e mensagens prontas; sem leitura de conversas |
| Pix | MVP1 | Payload EMV/BR Code estático gerado localmente e validado |
| PDF | MVP1 | Geração local determinística; opção server-side futura |
| SignallQ Nethal | Progressivo | Read-only e capability-gated; escrita só após homologação |

## 10. Segurança

- Senha local com algoritmo resistente e parâmetros revisáveis; nunca texto puro.
- Access token curto e refresh token rotacionado por dispositivo.
- `StorageProvider` com objetos privados ou acesso controlado.
- TLS obrigatório; certificate pinning apenas com processo seguro de rotação.
- Credenciais de roteador permanecem em memória/armazenamento protegido e não são exportadas.
- Safety Guard obrigatório antes de qualquer comando de escrita em equipamento.
- PII separada de telemetria; eventos com allowlist e reason codes fechados.
- Audit log para emissão, cancelamento, pagamento e alteração de plano.
- LGPD: base legal, retenção, exclusão, exportação e minimização.

## 11. Observabilidade

| Camada | Instrumentação |
|---|---|
| Android | Crashlytics, analytics tipado, métricas de sync e logs locais redigidos |
| Workers | Structured logs, `requestId`, status, duração e rate limit |
| D1/StorageProvider | Métricas de erro, latência, uso e falha de migração/upload |
| Produto | Funil signup→primeira visita→laudo→pagamento→retorno |
| SignallQ Nethal | Sessão e capability result sanitizados; sem payload bruto |

Nota atual-vs-alvo: hoje a telemetria é Firebase Analytics + Crashlytics (ATUAL). O pipeline unificado Telemetry Worker → Queue → D1/Analytics Engine com envelope `dot.case` é ALVO, com janela de convivência dos dois esquemas.

## 12. Testes e qualidade

| Nível | Obrigatório |
|---|---|
| Unitário | Casos de uso, classificadores, Pix, numeração e regras de plano |
| Caracterização | Implementações antigas antes da fusão, especialmente speedtest e diagnóstico |
| Contrato | Android/web contra Workers e schemas JSON |
| Integração | Room migrations, D1 migrations, StorageProvider upload e Billing sandbox |
| UI | Compose tests por fluxo e screenshots nos devices-alvo |
| E2E | Conta→visita offline→sync→laudo→Pix→recibo |
| Hardware | Matriz por modelo/firmware; driver sem evidência permanece experimental |
| Segurança | SAST, secret scan, dependency scan, auth abuse e privacy checks |

## 13. CI/CD e releases

CI por path/impacto. Cada app mantém `applicationId`, assinatura, listing, versionCode, release notes e rollout separados. Releases nascem de tags independentes.

| Path | Pipeline |
|---|---|
| `apps/android-*` ou `mobile/**` | Lint, detekt, unit, Compose tests, assemble e distribuição beta |
| `network-hardware/**` | Unit, fixtures, parser fuzz/snapshots e build SignallQ Nethal |
| `backend/**` | Typecheck, tests, D1 migration dry-run e deploy por ambiente |
| `apps/portal-web` ou `web/**` | Lint, test, build, Playwright e Pages preview |
| `docs/**` | Links, diagramas e validação de estrutura |

Tags de release canônicas (`00_CANONICO_v5.md` §7): `consumer/android/vX.Y.Z`, `pro/android/vX.Y.Z`, `portal-web/vX.Y.Z`, `admin-web/vX.Y.Z`, `signallq-nethal/vX.Y.Z`, `worker-<nome>/vX.Y.Z`.

## 14. Plano de migração

### 14.1 Ordem recomendada de fusão

`linka-android` primeiro (base mais madura); `nethal` em seguida (preservando o laboratório e isolando drivers); `linka-webapp` como base web do Portal; `linka-speedtest` por último, apenas para diff e portabilidade de trechos superiores.

### 14.2 Fases 0–9

| Fase | Entregas | Gate |
|---|---|---|
| 0 — Inventário | Mapa de módulos, licenças, segredos, builds e testes | Nenhum ativo crítico sem proprietário |
| 1 — Fundação | Monorepo, convention plugins, CI, docs e regras de dependência | Build vazio e pipelines verdes |
| 2 — Import Android | Histórico `linka-android`, consumer buildando sem mudança funcional | Paridade do APK e testes principais |
| 3 — Shared Core | Extrair design, network, permissions, storage, diagnostics e speedtest | Consumer continua estável |
| 4 — Pro Shell | `android-pro`, login, design system, clientes e visitas | Primeiro fluxo vertical funcional |
| 5 — SignallQ Nethal | Importar core/drivers/lab; eliminar ferramentas duplicadas | LAB builda e produção só usa approved |
| 6 — Web | Importar webapp; comparar e absorver speedtest único no Portal | PWA em paridade e speedtest repo congelado |
| 7 — Backend | Workers unificados, D1/StorageProvider, auth, sync e billing | Ambiente staging completo |
| 8 — Cutover | Releases beta, migração de dados/configurações e observação | Dois ciclos estáveis |
| 9 — Arquivamento | README de migração, releases finais e repos read-only | Rollback documentado e tags preservadas |

## 15. Critérios de desativação dos repositórios antigos

| Critério | Evidência |
|---|---|
| Código migrado | Mapa de origem e commit de importação |
| Build reproduzível | CI do monorepo produz artefatos equivalentes |
| Paridade | Checklist funcional e testes de regressão aprovados |
| Produção apontada | Deploys e pipelines usam apenas `signallq-platform` |
| Issues migradas | Backlog relevante movido ou referenciado |
| Documentação | README antigo aponta para novo local e informa data de congelamento |
| Rollback | Tags finais e procedimento de retorno testado |
| Estabilidade | Dois ciclos de release sem depender do repo antigo |

Admin: gate específico é paridade funcional do painel e do worker, produção apontada para o monorepo e ausência de dependência ativa do diretório `SignallQ Admin/` no repositório antigo. Depois dos gates: read-only, tópico `archived/migrated`, PRs pendentes fechados com destino explícito e arquivamento no GitHub. Repositórios não são excluídos — preservam auditoria e histórico.

## 16. Riscos e decisões

| Risco | Mitigação |
|---|---|
| Monorepo grande demais | CI por path, owners por domínio e módulos explícitos |
| Migração quebra app atual | Importação sem refatoração e testes de caracterização |
| Duplicidade speedtest/diagnóstico | Contrato único, benchmark e decisão documentada em ADR |
| SignallQ Nethal inseguro | Feature flags, capability gate, safety guard e homologação física |
| D1 usado como auth improvisado | Auth Worker dedicado, tokens rotacionados e auditoria |
| Offline gera conflito | Outbox, revisions, tombstones e regras por entidade |
| Starter/Free limita arquitetura | Entitlements centralizados e limites configuráveis |
| Documentação volta a divergir | Docs como código, owners e atualização obrigatória no PR |

Decisões de negócio ainda pendentes (canônico §8): preço do Pro, domínio do portal, provedor de identidade, conectores de nuvem, política de retenção.

## 17. ADRs iniciais

Os ADRs abaixo continuam citados como decisões obrigatórias, mas **os stubs ainda não existem** — o v5 os torna visíveis e prevê sua abertura em `docs/decisions/` (canônico §8, pendência 6). Enquanto os arquivos não forem criados, tratá-los como referência, não como documento consultável.

| ADR | Decisão |
|---|---|
| ADR-001 | Monorepo e produtos separados |
| ADR-002 | Estratégia de importação com preservação de histórico |
| ADR-003 | Motor único de diagnóstico e speedtest |
| ADR-004 | Offline-first e política de conflitos |
| ADR-005 | Autenticação própria sobre Cloudflare |
| ADR-006 | SignallQ Nethal capability-based e homologação de drivers |
| ADR-007 | Documentos imutáveis e numeração |
| ADR-008 | Billing e entitlements |

Os contratos OpenAPI referenciados por `packages/api-contracts` também são pendência (canônico §8, item 7): o v5 define a estrutura, não os contratos completos.

## 18. Portal Web

O Portal (ALVO) passa a ser produto público oficial da plataforma, fundindo `linka-speedtest` + `linka-webapp`. Reúne velocímetro web, apresentação comercial dos aplicativos, comparativos, preços, downloads, conteúdo educativo e páginas jurídicas.

### 18.1 Objetivos

- Teste de velocidade gratuito no navegador, sem instalação.
- Cumprir requisitos públicos de privacidade, termos, suporte e exclusão de conta.
- Apresentar SignallQ e SignallQ Pro, públicos, diferenças e benefícios.
- Direcionar para as páginas oficiais na Google Play.
- Canal próprio de aquisição orgânica e monetização por anúncios.

### 18.2 Rotas e monetização

| Rota | Finalidade | Monetização |
|---|---|---|
| `/` | Home com velocímetro, diagnóstico resumido e apresentação do ecossistema | Anúncios após o resultado e entre blocos editoriais |
| `/speedtest` | Medição dedicada e histórico local | Anúncios fora da área de interação |
| `/signallq` | Apresentação do app gratuito | Sem anúncio obrigatório |
| `/pro` | Apresentação do SignallQ Pro | Sem anúncio obrigatório |
| `/comparar` | Comparativo SignallQ × Pro | Opcional |
| `/precos` | Planos, periodicidade e benefícios do Pro | Não |
| `/download` | Links oficiais para Google Play | Não |
| `/privacidade/*` | Políticas por produto e pelo site | Não |
| `/termos` | Termos de uso | Não |
| `/exclusao-de-conta` | Solicitação e instruções de exclusão de conta e dados | Não |
| `/ajuda` e `/contato` | Suporte, FAQ e contato | Não |

### 18.3 Arquitetura e publicidade

React + TypeScript + Vite no Cloudflare Pages, com integrações controladas a Workers para telemetria, diagnóstico e serviços públicos. O teste de velocidade roda no cliente sempre que tecnicamente possível. Domínio pretendido `signallq.app` (pendente de aquisição). Anúncios nunca sobre o velocímetro, botão de início/repetição ou cards clicáveis; a primeira inserção ocorre depois do resultado e do diagnóstico. O portal deve oferecer conteúdo original suficiente, não ser ferramenta cercada de anúncios. Consentimento, cookies e personalização respeitam a legislação e a plataforma de anúncios utilizada.

## 19. Papel e desativação do SignallQ Nethal

O SignallQ Nethal deixa de ser produto público permanente. Vira ferramenta interna e temporária para validação de drivers, descoberta, autenticação, capacidades e comandos em equipamentos reais. Sem posicionamento comercial próprio; sem necessidade de publicação na Google Play; distribuído por build interno, Firebase App Distribution ou APK de laboratório. O código reutilizável migra para `network-hardware`. O shell do laboratório só permanece enquanto acrescentar capacidade de teste não disponível nos aplicativos oficiais.

Critérios para arquivamento do shell: drivers estáveis incorporados aos módulos compartilhados; testes automatizados e fixtures cobrindo parsers e contratos; SignallQ ou SignallQ Pro capazes de executar os fluxos aprovados; ferramentas de diagnóstico interno substituindo as telas exclusivas do Lab; documentação e histórico preservados antes do arquivamento.

---

## Documentos relacionados

- `00_CANONICO_v5.md` — dicionário canônico de nomes e decisões (prevalece sobre este).
- `01_SignallQ_Platform_Arquitetura_v5.md` — visão consolidada, portfólio e serviços de plataforma.
- `03_SignallQ_Governanca_GitHub_e_Monorepo_v5.md` — proteção de main, PRs, CI por impacto, releases por produto e contrato de agentes.
