# SignallQ — Modelo de Dados Cloudflare D1

**Status:** ativo · **Versão:** 5.0 · **Data:** 17/07/2026 · **Substitui:** SignallQ Modelo de Dados D1 v4

Especificação lógica de tabelas, índices, migrações, isolamento, auditoria e retenção do Cloudflare D1 da plataforma SignallQ.

> **Fonte única de nomes e decisões:** `00_CANONICO_v5.md`. Em qualquer divergência de nome de tabela, evento, worker ou rótulo, o canônico prevalece.

---

## Estado atual vs. Alvo

Este documento descreve **dois modelos distintos** que não devem ser confundidos:

- **D1 ATUAL do Admin** — ✅ existe hoje. São as **13 tabelas reais** já provisionadas no banco do `signallq-admin-worker` (Canônico §1). É o único D1 da plataforma que já roda em produção.
- **D1 ALVO do Pro** — 🎯 **greenfield, proposta**. É o modelo transacional da SignallQ Pro descrito da seção 3 em diante. **Nenhuma das tabelas do modelo Pro existe hoje**; todas seriam criadas de novo, sobre o mesmo D1 do Admin ou em banco dedicado, quando o app Pro (`io.signallq.pro`) sair do estado de alvo.

A separação é obrigatória porque o pacote v1→v4 tratava tudo como "baseline proposta", sem distinguir o que está implantado do que é desejo.

---

## 1. D1 ATUAL do Admin (13 tabelas reais)

São as tabelas efetivamente existentes no banco consumido pelo `signallq-admin-worker` em 17/07/2026 (Canônico §1). Não fazem parte do modelo transacional Pro; sustentam o painel administrativo atual (diagnósticos recebidos do app consumidor, uso de IA, autenticação do Admin, flags, saúde e telemetria agregada).

| Tabela | Domínio | Papel atual |
|---|---|---|
| `diagnostic_sessions` | Diagnóstico consumer | Sessões de diagnóstico recebidas do app SignallQ. |
| `ai_usage` | IA | Registro de chamadas ao Worker de IA (modelo, custo, latência, fallback). |
| `admin_settings` | Configuração | Configurações do painel administrativo. |
| `system_errors` | Observabilidade | Erros e falhas operacionais capturados pela plataforma. |
| `admin_users` | Identidade Admin | Contas administrativas do painel. |
| `admin_sessions` | Identidade Admin | Sessões autenticadas do painel (curtas, revogáveis). |
| `auth_rate_limit` | Segurança | Controle de taxa de tentativas de autenticação. |
| `alerts` | Observabilidade | Alertas operacionais e de negócio. |
| `feature_flags` | Configuração remota | Flags por aplicativo/versão/rollout. |
| `feature_flag_audit` | Auditoria | Trilha imutável de alterações de flags. |
| `analytics_events` | Telemetria agregada | Eventos de analytics recebidos/derivados. |
| `play_console_tracks` | Release | Estado das trilhas do Google Play (internal/alpha/beta/production). |
| `system_health_snapshots` | Observabilidade | Instantâneos de saúde da plataforma. |

**Regra:** ao evoluir o Admin, mudanças de schema seguem as migrations reais em `integrations/cloudflare/signallq-admin-worker/` (ver skill `cloudflare-d1-console`). Não presumir que qualquer tabela do modelo Pro abaixo já exista.

---

## 2. D1 ALVO do Pro — objetivo e público

**Estado: 🎯 ALVO (greenfield).** Nenhuma tabela desta seção em diante existe hoje.

Definir o modelo de dados transacional da SignallQ Pro no Cloudflare D1, separando domínios e evitando acoplamento direto das aplicações. Referência para backend, Android (Pro), web, administração, testes e migrações.

### Escopo

- Identidade, profissionais, clientes, locais, ambientes, visitas, medições, evidências e laudos.
- Assinaturas, pagamentos e o domínio **Financeiro com Pix** (corrige a omissão C5 do pacote v4).
- Telemetria agregada, configuração, conteúdo e auditoria.
- Metadados de storage, sem armazenar binários no D1.
- Índices, integridade, migrações, backup e retenção.

---

## 3. Convenções

- IDs no formato UUID/ULID textual, gerados por serviço confiável.
- Datas em UTC ISO-8601 ou epoch consistente por tabela.
- Nomes em **snake_case, tabela no singular** (padrão canônico único — Canônico §2).
- Soft delete apenas quando houver requisito de recuperação; caso contrário, exclusão controlada.
- Toda tabela mutável relevante possui `created_at`, `updated_at` e `version`.

**Nomes canônicos de domínio (Canônico §2) — usar exatamente estes:**

| Conceito | Tabela D1 | Não usar |
|---|---|---|
| Cliente do técnico | `customer` | `client` |
| Local do cliente | `service_location` | `locations` (plural) |
| Cômodo/zona medida | `environment` | `room`, `environments` (plural) |
| Medição (agrupador) | `measurement_session` | `measurements` como única tabela |
| Amostra de medição | `measurement_point` | — |
| Cobrança Pix | `pix_charge` | — |
| Perfil Pix | `pix_profile` | — |
| Metadado de arquivo | `storage_object` | R2 como storage padrão |

---

## 4. Domínios e tabelas (modelo ALVO Pro)

| Domínio | Tabelas principais |
|---|---|
| Identidade | `account`, `identity_provider`, `session`, `device_registration`, `consent` |
| Profissional | `professional_profile`, `professional_branding`, `entitlement` |
| CRM Pro | `customer`, `customer_contact`, `service_location`, `environment` |
| Atendimento | `appointment`, `visit`, `visit_status_history`, `note` |
| Medição | `measurement_session`, `measurement_point`, `speedtest_result`, `wifi_observation` |
| Evidência | `evidence`, `storage_object`, `evidence_link` |
| Laudo | `report`, `report_version`, `report_item`, `report_delivery` |
| **Financeiro** | `plan`, `subscription`, `purchase`, `payment_record`, `receipt`, `receipt_replacement`, `entitlement`, **`pix_charge`**, **`pix_profile`**, **`payment_allocation`** |
| Plataforma | `app_release`, `feature_flag`, `remote_config`, `content_item` |
| Nethal | `device_catalog`, `driver`, `capability`, `compatibility_evidence`, `lab_execution` |
| Operação | `telemetry_event_fact`, `telemetry_daily_aggregate`, `technical_error`, `audit_log` |

### 4.1 Domínio Financeiro com Pix (corrige o buraco C5)

O pacote v4 omitia o meio de pagamento local (Pix) e o desdobramento de recibos/alocações. O v5 fixa o domínio Financeiro completo:

| Tabela | Papel |
|---|---|
| `plan` | Catálogo de planos (Free, Pro, Pro Hosted/Business). Preço **pendente** (Canônico §8.1). |
| `subscription` | Assinatura do profissional; `entitlement` derivado e reconciliável. |
| `purchase` | Compra pontual/one-off vinculada a plano ou item. |
| `payment_record` | Registro de pagamento confirmado, independente do meio. |
| `receipt` | Recibo emitido, imutável enquanto mantido. |
| `receipt_replacement` | Substituição/reemissão de recibo, preservando o original. |
| `entitlement` | Direito de uso ativo derivado de assinatura/compra. |
| `pix_profile` | Perfil Pix do profissional (chave, dados de recebimento). Sem segredo em texto puro. |
| `pix_charge` | Cobrança Pix (QR/copia-e-cola), status e vínculo ao pagamento. |
| `payment_allocation` | Alocação de um pagamento a uma ou mais cobranças/assinaturas. |

---

## 5. Relacionamentos principais

```
account 1──0..1 professional_profile
professional_profile 1──N customer
customer 1──N service_location
service_location 1──N environment
customer 1──N appointment
appointment 0..1──1 visit
visit 1──N measurement_session
measurement_session 1──N measurement_point
visit 1──N evidence
visit 1──N report
report 1──N report_version
evidence N──1 storage_object
professional_profile 1──N subscription
subscription 1──N pix_charge
pix_charge 1──N payment_allocation
payment_record 1──N payment_allocation
professional_profile 1──0..1 pix_profile
```

---

## 6. Tabelas críticas

| Tabela | Campos-chave | Regras |
|---|---|---|
| `account` | `id`, `email_hash`, `status`, `created_at` | E-mail normalizado fora de consultas gerais; status controlado. |
| `professional_profile` | `account_id`, `display_name`, `document`, `branding_id` | Dados profissionais separados da conta. |
| `customer` | `id`, `professional_id`, `name`, `status` | Isolamento obrigatório por `professional_id`. |
| `service_location` | `id`, `customer_id`, `label`, `address_hash` | Vinculado ao cliente; endereço não vira dimensão geral. |
| `environment` | `id`, `service_location_id`, `name`, `kind` | Cômodo/zona medida; substitui `room`. |
| `visit` | `id`, `appointment_id`, `status`, `started_at`, `completed_at` | Transições validadas no serviço. |
| `measurement_point` | `session_id`, `environment_id`, `metric_type`, `value`, `unit` | Unidade explícita e origem registrada. |
| `storage_object` | `provider`, `owner_id`, `object_key`, `checksum`, `size_bytes` | Sem binário e sem token no D1. |
| `report_version` | `report_id`, `version_no`, `content_hash`, `issued_at` | Versão emitida imutável. |
| `subscription` | `professional_id`, `provider`, `external_id`, `status` | Entitlement derivado e reconciliável. |
| `pix_charge` | `id`, `professional_id`, `subscription_id`, `amount`, `status`, `txid` | Status reconciliável; sem dado sensível em texto puro. |
| `payment_allocation` | `payment_record_id`, `pix_charge_id`, `amount` | Aloca pagamento a cobrança/assinatura. |
| `audit_log` | `actor_id`, `action`, `target_type`, `target_id`, `occurred_at` | Append-only. |

---

## 7. Isolamento de dados

- Toda consulta Pro é escopada pelo `professional_id` derivado da sessão.
- O cliente nunca envia `professional_id` como única prova de autorização.
- Admin usa endpoints próprios e permissões explícitas.
- Dados de um profissional não podem ser agregados individualmente para outro.
- Exports são gerados por job controlado e auditado.

---

## 8. Índices recomendados

| Tabela | Índice |
|---|---|
| `customer` | `(professional_id, status, updated_at)` |
| `appointment` | `(professional_id, scheduled_start)`, `(status, scheduled_start)` |
| `visit` | `(professional_id, status, updated_at)` |
| `measurement_session` | `(visit_id, created_at)` |
| `evidence` | `(visit_id, created_at)`, `(storage_object_id)` |
| `report` | `(professional_id, customer_id, issued_at)` |
| `subscription` | `(professional_id, status)`, `UNIQUE(provider, external_id)` |
| `pix_charge` | `(professional_id, status, created_at)`, `UNIQUE(txid)` |
| `payment_allocation` | `(payment_record_id)`, `(pix_charge_id)` |
| `telemetry_event_fact` | `(source, event_name, occurred_at)` |
| `audit_log` | `(actor_id, occurred_at)`, `(target_type, target_id)` |

---

## 9. Migrações

- Cada mudança recebe arquivo numerado e imutável em `backend/d1/migrations`.
- Migrações são executadas primeiro em dev, depois staging e produção.
- Mudanças destrutivas usam estratégia expandir-migrar-contrair.
- Backfill é idempotente e observável.
- Rollback lógico deve ser previsto para releases críticas; nem toda alteração DDL será revertida automaticamente.

---

## 10. Backup e restauração

- Definir rotina de exportação e restauração testada, não apenas criada.
- Separar backup transacional de arquivos armazenados nos providers.
- Registrar RPO e RTO por ambiente.
- Executar teste periódico de restauração em ambiente isolado.
- Criptografar e controlar acesso aos exports.

---

## 11. Retenção e exclusão

| Classe | Tratamento |
|---|---|
| Conta ativa | Mantida conforme prestação do serviço e consentimentos. |
| Conta excluída | Anonimizar ou apagar dados vinculáveis conforme obrigação e política. |
| Laudo emitido | Retenção definida pelo usuário e obrigações aplicáveis; versão permanece íntegra enquanto mantida. |
| Registro financeiro (`payment_record`, `receipt`, `pix_charge`) | Retenção conforme obrigação fiscal/legal; imutável enquanto mantido. |
| Telemetria bruta | Janela curta. |
| Agregados | Podem ser mantidos sem possibilidade razoável de reidentificação. |
| Auditoria | Retenção maior e acesso restrito. |

Política de retenção por tipo de dado e plano é **decisão pendente** (Canônico §8.5).

---

## 12. Proibições

- Não guardar fotos, PDFs ou áudios como blobs no D1.
- Não guardar tokens OAuth, senhas, chaves Pix ou credenciais em texto puro.
- Não usar JSON genérico para substituir entidades centrais sem justificativa.
- Não executar migração manual diretamente em produção fora do pipeline.
- Não renomear identificadores técnicos preservados (`linkaKotlin.db`, `linkaPreferencias` etc. — Canônico §1).

---

## Documentos relacionados

- `00_CANONICO_v5.md` — dicionário canônico de nomes, §1 (D1 atual), §2 (glossário), §8 (pendências).
- `05_SignallQ_Telemetria_Analytics_v5.md` — tabelas de telemetria e catálogo de eventos.
- `06_SignallQ_Arquitetura_Storage_v5.md` — `storage_object` e fluxo de evidência.
- `07_SignallQ_Admin_Especificacao_v5.md` — consumo administrativo do D1.
