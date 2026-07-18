# SignallQ — Telemetria e Analytics

**Status:** ativo · **Versão:** 5.0 · **Data:** 17/07/2026 · **Substitui:** SignallQ Telemetria e Analytics v4

Contrato unificado de eventos, métricas, privacidade e operação para SignallQ, SignallQ Pro, Portal, SignallQ Admin e SignallQ Nethal.

> **Fonte única de nomes e decisões:** `00_CANONICO_v5.md`. O catálogo de eventos canônico está em Canônico §3.

---

## Estado atual vs. Alvo

- **Telemetria ATUAL (Firebase)** — ✅ existe hoje. O app consumidor dispara via **Firebase Analytics + Crashlytics** os eventos reais `feature_used`, `screen_view`, `app_session_start`, `feature_crash`, `battery_snapshot`. São `snake_case` porque seguem a convenção do Firebase. **Não há** Realtime DB e **não há** pipeline Cloudflare Queue→D1 (Canônico §3.3).
- **Pipeline ALVO (Cloudflare)** — 🎯 **proposta, não implementada**. O fluxo Telemetry Worker → Queue → D1/Analytics Engine, o envelope padrão e o catálogo `dot.case` da plataforma completa são desejo de arquitetura, com janela de convivência entre os dois esquemas durante a migração.

Convenção de nome canônica: **`dot.case`** para eventos, `snake_case` para propriedades (Canônico §3).

---

## 1. Objetivo e escopo

Definir uma telemetria única e consistente para toda a plataforma. Referência para desenvolvimento, dados, produto, suporte, segurança e governança.

Escopo: envelope de eventos e catálogo; pipeline de ingestão, processamento e consulta; consentimento, minimização, retenção e qualidade; métricas operacionais, de produto, de receita e de laboratório.

---

## 2. Princípios

- Coletar apenas o necessário para uma finalidade declarada.
- Separar telemetria de produto, observabilidade técnica e auditoria.
- Nunca enviar senha, conteúdo integral de foto, token ou credencial.
- Usar identificadores pseudônimos e rotacionáveis quando possível.
- Eventos devem ser versionados, idempotentes e validados no Worker.

---

## 3. Telemetria ATUAL (consumer, Firebase)

**Estado: ✅ ATUAL.** Único esquema efetivamente coletado hoje.

| Evento (Firebase, `snake_case`) | Finalidade |
|---|---|
| `feature_used` | Uso de funcionalidade no app consumidor. |
| `screen_view` | Navegação entre telas. |
| `app_session_start` | Início de sessão do app. |
| `feature_crash` | Falha capturada (via Crashlytics). |
| `battery_snapshot` | Amostra de estado de bateria. |

Transporte: **Firebase Analytics** (eventos) + **Crashlytics** (falhas). Sem Realtime DB. A migração para o catálogo-alvo `dot.case` sobre pipeline Cloudflare é trabalho de plataforma (🎯 ALVO), com convivência dos dois esquemas.

---

## 4. Envelope padrão (ALVO)

**Estado: 🎯 ALVO.** Formato do evento no pipeline Cloudflare proposto:

```json
{
  "event_id": "uuid",
  "event_name": "visit.completed",
  "schema_version": 1,
  "occurred_at": "ISO-8601",
  "received_at": "servidor",
  "source": "android_pro",
  "app_version": "1.0.0",
  "environment": "production",
  "anonymous_id": "rotacionável",
  "user_id": "opcional/pseudônimo",
  "session_id": "uuid",
  "properties": {},
  "context": {
    "os": "Android",
    "network_type": "wifi",
    "locale": "pt-BR"
  }
}
```

---

## 5. Fontes (IDs canônicos)

IDs de `source` (Canônico §3.1):

| Fonte | Identificador | Exemplos |
|---|---|---|
| SignallQ (consumer) | `android_consumer` | `speedtest.completed`, `diagnosis.completed`, `recommendation.opened` |
| SignallQ Pro | `android_pro` | `customer.created`, `visit.started`, `report.generated`, `subscription.activated` |
| Portal | `portal_web` | `web_speedtest.completed`, `app_download.clicked`, `ad_slot.viewable` |
| Admin | `admin_web` | `admin.login`, `flag.updated`, `export.requested` |
| SignallQ Nethal | `signallq_nethal` | `device.detected`, `driver.executed`, `capability.failed` |
| Backend | `worker` | `request.failed`, `queue.retry`, `sync.conflict` |

---

## 6. Catálogo canônico de eventos (ALVO, `dot.case`)

Catálogo-alvo da plataforma completa (Canônico §3.1), inclui os eventos de **Pix** e **retenção** ausentes no v4:

| Domínio | Eventos canônicos |
|---|---|
| Identidade | `auth.started`, `auth.succeeded`, `auth.failed`, `account.deleted`, `account.identity_linked` |
| Ativação Pro | `profile.completed`, `pix.configured` |
| Speedtest | `speedtest.started`, `speedtest.phase_completed`, `speedtest.completed`, `speedtest.failed` |
| Diagnóstico | `diagnosis.requested`, `diagnosis.completed`, `recommendation.viewed`, `recommendation.opened`, `feedback.sent` |
| CRM/Visita (Pro) | `customer.created`, `location.created`, `appointment.created`, `visit.started`, `environment.measured`, `evidence.added`, `visit.completed` |
| Entrega (Pro) | `comparison.viewed`, `report.generated`, `report.shared` |
| Assinatura/Receita | `paywall.viewed`, `trial.started`, `purchase.started`, `purchase.completed`, `subscription.activated`, `entitlement.changed`, `pix_charge.created`, `payment.confirmed` |
| Storage | `storage.connected`, `upload.started`, `upload.completed`, `upload.failed`, `sync.conflict` |
| Retenção | `visit.completed_7d`, `visit.completed_30d`, `customer.returned` |
| Qualidade | `sync.failed`, `report.failed`, `measurement.failed`, `feature.crash` |
| Admin | `admin.login`, `admin.action`, `config.changed`, `flag.updated`, `sensitive_record.viewed`, `export.requested`, `export.generated` |
| Nethal | `device.detected`, `fingerprint.completed`, `driver.selected`, `driver.executed`, `command.executed`, `command.blocked`, `capability.failed` |
| Portal | `web_speedtest.completed`, `app_download.clicked`, `ad_slot.viewable` |
| Backend | `request.failed`, `queue.retry`, `sync.conflict` |

---

## 7. Mapa de migração snake→dot

Do estilo `snake_case` flat que o Funcional v4 usava para o canônico `dot.case` (Canônico §3.2):

| snake (v4) | dot (canônico) | | snake (v4) | dot (canônico) |
|---|---|---|---|---|
| `signup_started` | `auth.started` | | `paywall_viewed` | `paywall.viewed` |
| `signup_completed` | `auth.succeeded` | | `trial_started` | `trial.started` |
| `profile_completed` | `profile.completed` | | `subscription_activated` | `subscription.activated` |
| `pix_configured` | `pix.configured` | | `pix_charge_created` | `pix_charge.created` |
| `customer_created` | `customer.created` | | `payment_confirmed` | `payment.confirmed` |
| `appointment_created` | `appointment.created` | | `sync_failed` | `sync.failed` |
| `visit_started` | `visit.started` | | `report_failed` | `report.failed` |
| `environment_measured` | `environment.measured` | | `measurement_failed` | `measurement.failed` |
| `comparison_viewed` | `comparison.viewed` | | `feature_crash` | `feature.crash` |
| `report_generated` | `report.generated` | | `visit_completed_7d` | `visit.completed_7d` |
| `report_shared` | `report.shared` | | `visit_completed_30d` | `visit.completed_30d` |
| | | | `customer_returned` | `customer.returned` |

---

## 8. Pipeline (ALVO)

**Estado: 🎯 ALVO.** Não implementado hoje (Canônico §1).

```
SDK/Cliente
   ↓ lote + retry
Telemetry Worker
   ↓ validação / normalização / descarte PII
Cloudflare Queue
   ├── agregação operacional → Analytics Engine
   ├── fatos de produto → D1
   ├── falhas críticas → alertas
   └── auditoria → tabela append-only
SignallQ Admin
   ↓
dashboards, funis, investigação e exportação controlada
```

---

## 9. Tabelas de telemetria

| Tabela | Finalidade | Retenção sugerida |
|---|---|---|
| `telemetry_event_raw` | Janela curta para reprocessamento e investigação. | Curta; parametrizável. |
| `telemetry_event_fact` | Eventos normalizados necessários aos funis. | Média, conforme finalidade. |
| `telemetry_daily_aggregate` | Agregados por dia, app e dimensão permitida. | Longa. |
| `technical_error` | Erros e falhas operacionais. | Conforme severidade. |
| `audit_log` | Ações administrativas e sensíveis. | Maior e protegida. |

No D1 ATUAL do Admin, o análogo existente é `analytics_events` (Canônico §1). As tabelas acima são o alvo do pipeline Cloudflare.

---

## 10. Métricas e definições

| Métrica | Regra |
|---|---|
| Usuário ativo diário | Identidade distinta com evento qualificador no dia; excluir bots e testes. |
| Teste concluído | `speedtest.completed` válido e sem erro terminal. |
| Visita concluída | `visit.completed` com estado anterior válido. |
| Laudo gerado | `report.generated` com identificador único; reexportação não conta como novo laudo. |
| Conversão Pro | Usuário com entitlement ACTIVE após paywall ou trial. |
| Pagamento Pix confirmado | `payment.confirmed` conciliado com `pix_charge` de mesma referência. |
| Retenção 7d/30d | `visit.completed_7d` / `visit.completed_30d` sobre coorte de primeira visita. |
| Taxa de driver | Execuções bem-sucedidas / execuções elegíveis por versão do driver. |

---

## 11. Privacidade e consentimento

- Consentimento de analytics e publicidade deve ser granular quando aplicável.
- Observabilidade estritamente necessária deve ser distinguida de analytics opcional.
- IP bruto não deve virar dimensão permanente de produto.
- SSID, BSSID, nomes de clientes e observações não entram em telemetria geral.
- O usuário deve poder solicitar exclusão e exportação conforme os dados vinculados à conta.

---

## 12. Qualidade e governança

- Todo evento novo exige owner, finalidade, schema, exemplo, retenção e dashboard consumidor.
- Eventos inválidos são rejeitados com métrica de erro, sem serem persistidos como fatos válidos.
- Mudanças incompatíveis exigem nova `schema_version`.
- Ambientes dev e staging não contaminam produção.
- Testes automatizados validam nomes, propriedades obrigatórias e ausência de campos proibidos.

---

## 13. Observabilidade

- SLIs: disponibilidade, latência, taxa de erro, atraso de fila e perda de eventos.
- Alertas por impacto, evitando alertar por variação irrelevante.
- Correlação por `request_id` e `event_id`, nunca por dados pessoais em logs.
- Dashboards separados para consumidor, Pro, portal, admin e SignallQ Nethal.

---

## Documentos relacionados

- `00_CANONICO_v5.md` — catálogo de eventos §3.1, mapa de migração §3.2, telemetria atual §3.3.
- `04_SignallQ_Modelo_Dados_D1_v5.md` — tabelas de telemetria e `analytics_events` real.
- `06_SignallQ_Arquitetura_Storage_v5.md` — eventos de `storage.*`/`upload.*`.
- `07_SignallQ_Admin_Especificacao_v5.md` — consumo dos dashboards e funis.
