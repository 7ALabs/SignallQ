# SignallQ Platform — Validação Cruzada e Changelog v4 → v5

- **Status:** ativo · **Versão:** 5.0 · **Data:** 17/07/2026
- **Escopo:** avaliação de cobertura do pacote v1→v4, validação cruzada entre documentos e contra o código real, e registro do que o v5 corrigiu.
- **Método:** leitura integral dos 27 documentos v1→v4 + validação das afirmações factuais contra o repositório `gmmattey/linka-android` (working tree em 17/07/2026).

> **Atualização 2026-07-18:** a decisão C9 abaixo (paleta Pro **teal `#006B73`**) foi **superada**. O projeto Claude Design `77a19317` (fonte da verdade visual do Pro) evoluiu para identidade **azul `#0B6CFF`** (Secondary ciano `#006B76`, Tertiary roxo `#6558E8`, escala de sinal de 6 níveis, 2 temas oficiais). A paleta vigente está em `00_CANONICO_v5.md §5.2`. As linhas de C9 e da tabela §7 permanecem como registro histórico do passe v4→v5.

---

## 1. Veredito de cobertura

O pacote v1→v4 entrega uma **boa visão de consolidação de plataforma**, mas **não cobre tudo** e **contradiz a si mesmo**. Três falhas estruturais:

1. **Documentos órfãos** — 4 famílias pararam antes do v4 e não cobrem as superfícies novas (Portal, Admin como produto).
2. **Contradições internas** — mesmo dentro do v4, eventos, dados e estrutura de monorepo divergem entre documentos irmãos.
3. **Sem separação atual vs. alvo** — tudo é "baseline proposta"; nada diz o que já existe. Validado: a maior parte é greenfield.

---

## 2. Documentos órfãos (gaps de cobertura)

| Família | Última versão real | Consequência |
|---|---|---|
| Roadmap MVP1/MVP2 | **v2 (conteúdo idêntico à v1, cabeçalho ainda diz "Versão 1.0")** | Nunca revisado. Ainda cita "NetHAL" e "D1/R2". |
| Design System (Pro) | v3 | Sem tokens/design para Portal e Admin. |
| Jornada e Fluxo de Telas (Pro) | v3 (idêntico à v2) | Sem jornada para Portal e Admin. |
| Governança GitHub/Monorepo | v3 (idêntico à v2) | Não governa artefatos novos (D1 migrations, RBAC Admin, telemetry-schema). |
| Arquitetura Android (detalhada) | v3 | O "v4" a substituiu por um doc de plataforma **mais raso** — perdeu camadas/módulos/ADRs. |

Famílias completas v1→v4: **Especificação Funcional** e **Especificação Técnica**. Nasceram só no v4 (sem histórico): **Admin, Storage, Modelo Dados D1, Platform Arquitetura, Telemetria**.

**Correção v5:** todos os documentos re-emitidos em v5. Os órfãos ganham conteúdo para as superfícies novas. A Arquitetura Android detalhada é reincorporada dentro de `01_..._Arquitetura_v5` (não se perde profundidade).

---

## 3. Contradições internas (com localização)

| # | Contradição | Onde | Decisão v5 |
|---|---|---|---|
| C1 | Eventos `snake_case` (Funcional) vs `dot.case` (Telemetria); eventos Pix/retenção só num lado | Funcional v4 §12 vs Telemetria v4 §7 | **`dot.case` canônico** (Canônico §3). Funcional realinhado; Pix/retenção entram no catálogo. |
| C2 | `customer` vs `client` para o mesmo conceito | Funcional/D1 vs Telemetria (`client.created`) | **`customer`** (Canônico §2). |
| C3 | `location/environments` vs `service_location/room` | Espec Técnica vs Modelo D1 vs Telemetria | **`service_location` + `environment`** (Canônico §2). |
| C4 | Tabelas D1 plural (Espec Técnica) vs singular (Modelo D1); nomes diferentes | Espec Técnica v1 §7 vs Modelo D1 v4 §5 | **Dicionário D1 singular canônico** (Modelo D1 v5). Espec Técnica passa a referenciar. |
| C5 | Domínio **Pix ausente** do modelo D1 canônico | Modelo D1 v4 §7 (Financeiro sem Pix) | **Adicionadas** `pix_charge`, `pix_profile`, `payment_allocation`. |
| C6 | Três árvores de monorepo incompatíveis | Arq Android v1 vs Espec Técnica v1 vs Platform v4 | **Uma árvore canônica** (Canônico §4). |
| C7 | `web-consumer` vs `portal-web` | Espec Técnica v1 vs v3+/Platform v4 | **`portal-web`** (portal público). |
| C8 | Storage R2 (v1–v3) vs StorageProvider local-first (v4) | Espec Técnica v1/Arq Android vs Storage v4 | **Local-first/SAF; R2 = add-on pago** (Canônico §6). Roadmap/Arq Android corrigidos. |
| C9 | Primary Pro violeta `#6C2BFF` (DS v2) vs teal `#006B73` (DS v3); resíduos violeta | DS v2 vs DS v3 vs Jornada/Funcional | **Teal `#006B73` primary; acento `#5B21D6`** (`#6C2BFF` morto). |
| C10 | 5 grafias de Nethal | Todas as famílias | **"SignallQ Nethal"; `apps/signallq-nethal`; source `signallq_nethal`**. |

---

## 4. Validação contra o código real (o descolamento da realidade)

Afirmações factuais do v4 checadas no repo:

| Afirmação v4 | Veredito | Correção v5 |
|---|---|---|
| 16 módulos (app+6core+9feat) | ✅ Confere | Mantido. |
| React 19 no Admin | ✅ Confere (19.0.1) | Mantido. |
| "apenas 2 workers" (ai-diagnosis, admin) | ❌ São **5** | Documentados os 5; mapeados na árvore. |
| Modelo D1 Pro (users, customers, visits…) | ❌ **0 existem** | Marcado 🎯 ALVO/greenfield; D1 real (13 tabelas) documentado como ATUAL no Admin v5. |
| Telemetria = Firebase + Crashlytics, sem Realtime DB | ✅ Confere | Firebase marcado ATUAL; pipeline Cloudflare marcado ALVO. |
| Pipeline Cloudflare Telemetry→Queue→D1/AE | ❌ Não implementado | Marcado 🎯 ALVO. |
| SignallQ Pro / Portal no repo | ❌ Não existem | Marcados 🎯 ALVO. |
| Nethal no repo | ✅ Não está (repo separado) | Marcado 🎯 ALVO (migração futura). |
| Stack Android | ✅ Confere | Mantido. |
| Tokens primary `#5B21D6` / secondary `#2851B8` | ✅ Confere | Fixados canônicos; `#6C2BFF` removido. |
| versionName/versionCode | ✅ Real **0.26.0 / 62** | Fixado (a própria CLAUDE.md do repo está defasada em 0.25.0/60 — ver §6). |

---

## 5. Lacunas nunca preenchidas (o v5 as torna explícitas)

- **Preço do Pro** — nenhum valor em lugar algum, apesar de rota `/precos`, planos mensal/anual e gate de monetização.
- **Domínio `signallq.app`** — "sujeito a aquisição".
- **Provedor de identidade / recuperação de conta** — decisão pendente.
- **ADR-001..008** — citados, inexistentes. v5 abre stubs em `docs/decisions/`.
- **Contratos OpenAPI** — referenciados (`packages/api-contracts`), inexistentes. v5 define estrutura.
- **Design/UX de Admin e Portal** — funcionalidade definida, sem design system nem jornada. v5 cobre no Design System e na Jornada.

Todas registradas em `00_CANONICO_v5.md §8` como decisões pendentes visíveis.

---

## 6. Achado colateral: a própria CLAUDE.md do repo está defasada

A validação encontrou que `SignallQ/.claude/CLAUDE.md` diz **0.25.0 / versionCode 60** e cita só o `ai-diagnosis-worker`. O real é **0.26.0 / vc62** e **5 workers**. Não faz parte do pacote de documentação, mas é dívida de documentação do repo — recomendo abrir issue `Task - Atualizar CLAUDE.md (versão 0.26.0/vc62 e 5 workers Cloudflare)`.

---

## 7. Mudanças por documento (v4/v3/v2 → v5)

| Documento v5 | Origem | Principais mudanças |
|---|---|---|
| `00_CANONICO_v5` | novo | Dicionário único, catálogo de eventos, árvore de monorepo, paleta, atual-vs-alvo. |
| `01_Platform_Arquitetura_v5` | Platform Arq v4 + Arq Android v3 | Reincorpora detalhe Android perdido; árvore canônica; 5 workers; atual-vs-alvo. |
| `02_Especificacao_Tecnica_v5` | Espec Técnica v4 | Nomes canônicos; StorageProvider; 5 workers; ADRs como stubs. |
| `03_Governanca_GitHub_Monorepo_v5` | Governança v3 | Nethal renomeado; árvore alinhada; governa D1/RBAC/telemetry-schema. |
| `04_Modelo_Dados_D1_v5` | Modelo D1 v4 | Dicionário singular canônico; **tabelas Pix adicionadas**; separa D1 Admin (atual) do D1 Pro (alvo). |
| `05_Telemetria_Analytics_v5` | Telemetria v4 | Catálogo `dot.case` canônico + Pix/retenção; Firebase atual vs Cloudflare alvo. |
| `06_Arquitetura_Storage_v5` | Storage v4 | R2 rebaixado a add-on; providers confirmados. |
| `07_Admin_Especificacao_v5` | Admin v4 | Documenta os 13 D1 reais + 5 workers como ATUAL; separa do alvo. |
| `08_Pro_Especificacao_Funcional_v5` | Funcional v4 | Eventos → `dot.case`; entidades → nomes canônicos. |
| `09_Pro_Jornada_Fluxo_Telas_v5` | Jornada v3 | Traz a v5; acento violeta = `#5B21D6`; cobre lacunas. |
| `10_Pro_Design_System_v5` | Design System v3 | Paleta corrigida (`#6C2BFF`→`#5B21D6` acento); relação com paleta consumer. |
| `11_Pro_Roadmap_MVP1_MVP2_v5` | Roadmap v2 | Rótulo corrigido; Nethal/StorageProvider; preço marcado como pendência de bloqueio. |
