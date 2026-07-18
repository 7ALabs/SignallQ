# SignallQ Admin — Especificação

**Status:** ativo · **Versão:** 5.0 · **Data:** 17/07/2026 · **Substitui:** SignallQ Admin Especificação Completa v4

Especificação do painel administrativo do ecossistema SignallQ: o Admin **real de hoje** e as capacidades-alvo propostas para operar os dois aplicativos e o portal.

> **Fonte única de nomes e decisões:** `00_CANONICO_v5.md`. O inventário real (stack, workers, D1) está em Canônico §1.

---

## Estado atual vs. Alvo

- ✅ **ATUAL** — O SignallQ Admin existe hoje: React 19.0.1 + Vite 6 + TS 5.8 + Tailwind 4 + Lucide + Motion + Recharts + Vitest, no diretório `SignallQ Admin/` do monorepo, backend `signallq-admin-worker`, sobre 13 tabelas D1 reais e 5 workers Cloudflare reais.
- 🎯 **ALVO** — As capacidades ampliadas (visões separadas/consolidadas dos dois apps, RBAC completo, auditoria imutável, gestão de assinaturas Pro, configuração remota) são proposta. Cada seção abaixo marca o que ainda não existe.

---

## 1. Admin ATUAL (o que já existe)

**Estado: ✅ ATUAL** (Canônico §1).

### 1.1 Stack

| Camada | Tecnologia |
|---|---|
| Frontend | React **19.0.1** + Vite **6** + TypeScript **5.8** + Tailwind **4** |
| UI/Interação | Lucide (ícones), Motion (animação), Recharts (gráficos) |
| Testes | Vitest |
| Pacote | `signallq-admin` v0.1.0 |
| Backend | Worker Cloudflare `signallq-admin-worker` |
| Localização | Diretório `SignallQ Admin/` dentro do monorepo `gmmattey/linka-android` |

### 1.2 D1 real do Admin (13 tabelas)

`diagnostic_sessions`, `ai_usage`, `admin_settings`, `system_errors`, `admin_users`, `admin_sessions`, `auth_rate_limit`, `alerts`, `feature_flags`, `feature_flag_audit`, `analytics_events`, `play_console_tracks`, `system_health_snapshots`. Detalhe em `04_SignallQ_Modelo_Dados_D1_v5.md` §1.

### 1.3 Workers Cloudflare reais (5)

| Worker | Papel |
|---|---|
| `signallq-admin-worker` | Backend do painel administrativo. |
| `ai-diagnosis-worker` | Diagnóstico por IA (Gemini primário, Qwen3/CF fallback). |
| `signallq-diagnostic-worker` | Serviço de diagnóstico de conectividade. |
| `signallq-privacy-worker` | Privacidade/LGPD (exclusão, exportação). |
| `game-latency-probe-worker` | Sonda de latência para testes direcionados por jogo. |

A doc v4 citava apenas 2 workers; o v5 fixa os 5 reais (Canônico §1). No monorepo-alvo, o mapeamento é `signallq-admin-worker`→`admin`; `ai-diagnosis-worker` + `signallq-diagnostic-worker`→`diagnosis` (consolidar); `signallq-privacy-worker`→`privacy`; `game-latency-probe-worker`→`game-latency` (Canônico §4).

---

## 2. Capacidades-ALVO

**Estado: 🎯 ALVO.** O que segue é a especificação proposta do Admin para operar SignallQ, SignallQ Pro e Portal com RBAC, auditoria e gestão de assinaturas. Uso restrito a administradores, suporte, produto, engenharia, finanças e observabilidade, conforme RBAC.

Escopo-alvo: dashboard executivo e operacional; usuários, profissionais, clientes técnicos e assinaturas; telemetria, speedtests, diagnósticos, laudos e integrações; configuração remota, conteúdo, auditoria e suporte.

### 2.1 Perfis e permissões (🎯 ALVO)

| Perfil | Acesso principal | Restrições |
|---|---|---|
| Super Admin | Todos os módulos e configuração de segurança. | Ações críticas exigem reautenticação. |
| Produto | Métricas, funis, conteúdo e feature flags. | Sem acesso a credenciais ou exportações sensíveis. |
| Suporte | Busca de usuários, sessões, erros e solicitações. | Dados minimizados; sem alterar faturamento. |
| Financeiro | Assinaturas, receitas, reembolsos e conciliação. | Sem acesso a conteúdo técnico detalhado. |
| Engenharia | Saúde, versões, eventos, Workers, drivers e falhas. | Sem dados pessoais desnecessários. |
| Auditor | Leitura de trilhas e relatórios. | Somente leitura. |

Hoje o controle de acesso é sustentado por `admin_users` / `admin_sessions` / `auth_rate_limit` (ATUAL); o RBAC de seis perfis acima é alvo.

### 2.2 Navegação principal (🎯 ALVO)

Visão geral · SignallQ · SignallQ Pro · Portal e Speedtest · Usuários e profissionais · Assinaturas e pagamentos · Diagnósticos e medições · Laudos e evidências · Equipamentos e SignallQ Nethal · Conteúdo e configurações · Saúde da plataforma · Auditoria.

### 2.3 Dashboard consolidado (🎯 ALVO)

| Grupo | Indicadores |
|---|---|
| Aquisição | Instalações, sessões, visitantes do portal, origem, conversão para Google Play. |
| Uso | DAU, WAU, MAU, retenção, testes, diagnósticos e visitas concluídas. |
| Pro | Profissionais ativos, clientes cadastrados, laudos, conversão Free > Pro. |
| Receita | Assinaturas ativas, MRR, cancelamentos, grace period, receita publicitária e Pix conciliado. |
| Qualidade | Crashes, ANR, falhas de Worker, latência, sincronizações e erros de storage. |
| IA | Chamadas, custo estimado, modelo, fallback, latência e qualidade percebida. |

Métricas de IA e diagnóstico já têm lastro real hoje via `ai_usage` e `diagnostic_sessions` (ATUAL); os demais grupos dependem do pipeline de telemetria alvo.

### 2.4 SignallQ (consumer)

Parte ATUAL (dados já recebidos): acompanhar diagnósticos e uso de IA a partir de `diagnostic_sessions` / `ai_usage`; gerenciar `feature_flags` do consumidor com trilha em `feature_flag_audit`; acompanhar trilhas de release em `play_console_tracks`.

Parte 🎯 ALVO: separar métricas por versão, fabricante, Android, ISP e tipo de rede; DNS, Wi-Fi, dispositivos e histórico consolidados; consultar falhas sem expor conteúdo pessoal além do necessário; mensagens e versões mínimas remotas.

### 2.5 SignallQ Pro (🎯 ALVO)

- Acompanhar profissionais, clientes, locais, ambientes e medições.
- Visualizar volume de fotos, evidências, PDFs e sincronizações por `StorageProvider`.
- Gerenciar planos, entitlements e limites de uso.
- Acompanhar funil: cadastro > primeira visita > primeiro laudo > assinatura.
- Suportar solicitação de exclusão, exportação e correção de dados (apoiado pelo `signallq-privacy-worker`, ATUAL).

### 2.6 Portal e anúncios (🎯 ALVO)

- Testes iniciados e concluídos, abandono, Core Web Vitals e conversão para os aplicativos.
- Receita e impressões publicitárias por posição, sem armazenar identificadores além do consentido.
- Conteúdos, FAQs, banners e links de download.
- Status das páginas legais e versão publicada.

### 2.7 Equipamentos e SignallQ Nethal (🎯 ALVO)

| Visão | Conteúdo |
|---|---|
| Catálogo | Marca, modelo, firmware, evidência e nível de compatibilidade. |
| Drivers | Versão, capability, taxa de sucesso, erros e data da última validação. |
| Fingerprint | Sinais usados, confiança e falsos positivos conhecidos. |
| Testes de laboratório | Execuções, dispositivo físico, massa, evidências e resultado. |
| Rollout | Experimental, beta, estável, desativado. |

### 2.8 Configuração remota (🎯 ALVO)

- Feature flags por aplicativo, versão, país, plano e percentual de rollout.
- Versão mínima e bloqueio somente quando indispensável.
- Modelos de IA, limites, timeout e estratégia de fallback.
- Textos remotos e conteúdos editoriais versionados.
- Kill switches para drivers, endpoints e integrações.

Base ATUAL: `feature_flags` + `feature_flag_audit` + `admin_settings` já sustentam flags e configuração; a granularidade por país/plano/percentual é alvo.

### 2.9 Auditoria e segurança (🎯 ALVO)

- Login administrativo separado com MFA recomendado.
- RBAC aplicado no backend, não apenas na interface.
- Registro imutável de leitura sensível, alteração, exportação e exclusão.
- Mascaramento de e-mail, IP e identificadores em telas gerais.
- Sessões curtas, revogáveis e vinculadas ao dispositivo quando possível.

---

## 3. Contratos de API (rotas de exemplo, 🎯 ALVO)

| Grupo | Exemplos |
|---|---|
| Métricas | `GET /admin/metrics/overview`, `GET /admin/apps/{app}/funnels` |
| Usuários | `GET /admin/users`, `GET /admin/users/{id}`, `POST /admin/users/{id}/actions` |
| Assinaturas | `GET /admin/subscriptions`, `POST /admin/subscriptions/{id}/reconcile` |
| Config | `GET/PUT /admin/config/{scope}`, `POST /admin/flags/{id}/rollout` |
| Auditoria | `GET /admin/audit?actor=&action=&date=` |

Contratos OpenAPI completos ainda não existem (Canônico §8.7) — o v5 define a estrutura, não os contratos fechados.

---

## 4. Critérios de aceitação

- Todo indicador identifica fonte, período, fuso e regra de cálculo.
- Filtros não alteram silenciosamente o universo da métrica.
- Nenhuma ação administrativa sensível existe sem autorização backend e auditoria.
- Os dois aplicativos aparecem separadamente e também em visão consolidada.
- Painel existente é migrado com matriz de paridade antes de mudanças visuais extensas.

---

## Documentos relacionados

- `00_CANONICO_v5.md` — inventário real §1 (stack, D1, workers), glossário §2, monorepo §4, pendências §8.
- `04_SignallQ_Modelo_Dados_D1_v5.md` — 13 tabelas reais do Admin e modelo Pro alvo.
- `05_SignallQ_Telemetria_Analytics_v5.md` — eventos `admin.*` e dashboards.
- `06_SignallQ_Arquitetura_Storage_v5.md` — volume por `StorageProvider`.
