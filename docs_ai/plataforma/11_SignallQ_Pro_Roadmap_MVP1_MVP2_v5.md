# SignallQ Pro — Roadmap MVP1 e MVP2

**Status:** ativo · **Versão:** 5.0 · **Data:** 17/07/2026 · **Substitui:** Roadmap MVP1/MVP2 v2 (conteúdo travado na v1, cabeçalho "Versão 1.0")

## Estado atual vs. Alvo

Todo o produto deste roadmap — o **SignallQ Pro** (`io.signallq.pro`, Canônico §7) — é **🎯 ALVO**: não existe ainda (Canônico §1). O roadmap descreve como sair do zero até um MVP1 operacional e um MVP2 escalável.

O **✅ ATUAL** que serve de fundação: o SignallQ consumidor (`gmmattey/linka-android`) com motores reaproveitáveis, o SignallQ Admin e os 5 Workers Cloudflare em produção. O monorepo-alvo `signallq-platform` e o modelo de dados D1 "Pro" são greenfield.

> **Bloqueio de monetização (Canônico §8.1):** o **preço do Pro** (mensal/anual) não está definido em nenhum documento. Os gates de monetização abaixo medem conversão e intenção de pagamento **sem um preço fixado** — enquanto o valor não for decidido, esses gates não podem fechar. É pendência de bloqueio, não detalhe.

Correções aplicadas nesta versão (vindas do Canônico): "NetHAL" → **SignallQ Nethal** (§2); "D1/R2 como storage" → **StorageProvider local-first**, com R2 apenas como add-on hospedado pago (§6); `applicationId` do Pro = `io.signallq.pro` (§7).

---

## 1. Princípio do roadmap

O MVP1 precisa permitir que um técnico realize e feche um atendimento real, mesmo com algumas etapas manuais. O MVP2 só começa quando o MVP1 estiver estável, utilizado por profissionais reais e demonstrar valor e intenção de pagamento. **Data não substitui evidência.**

> **Regra central.** MVP1 prova o fluxo e o valor. MVP2 reduz trabalho manual, aumenta retenção e escala. Nada entra no MVP2 apenas porque parece moderno.

---

## 2. Visão das fases

| Fase | Objetivo | Resultado |
|---|---|---|
| Fundação | Preparar monorepo, contratos, identidade e qualidade | Base segura para dois apps e serviços |
| MVP1 - Atendimento completo | Executar visita, laudo, cobrança e recibo | Produto utilizável e vendável |
| Validação | Testar com técnicos reais e medir comportamento | Decisão objetiva de avançar |
| MVP2 - Operação conectada | Sincronizar agenda, nuvem e assinatura madura | Produto recorrente e escalável |
| Pós-MVP2 | Expandir canais, hardware e automações | Ecossistema profissional |

---

## 3. Fase 0 — Fundação obrigatória

- Criar ou consolidar o monorepo `signallq-platform` com pipelines por impacto.
- Migrar motores compartilháveis do `linka-android` sem copiar dependências B2C para o Pro.
- Definir contratos de medição, diagnóstico, cliente, visita, laudo, pagamento e recibo.
- Criar Auth Worker, migrations D1 e sessão local segura.
- Configurar Firebase/Play Console separados para **`io.signallq.pro`**.
- Criar design system Pro e componentes básicos acessíveis.
- Definir analytics sem dados pessoais e política LGPD.
- Montar ambiente alpha e testes automáticos mínimos.

> **Saída da Fundação.** Build alpha instalável, login funcional, banco versionado, CI verde e arquitetura sem dependência do Pro para o Consumer.

**Storage (Canônico §6):** a fundação adota **local-first + Android SAF** abstraído por `StorageProvider` (`DeviceLocalProvider`, `AndroidSafProvider`). R2 hospedado (`SignallQHostedProvider`) é add-on pago e opcional — não é o storage padrão do MVP.

---

## 4. MVP1 — Atendimento profissional completo

| Épico | Entrega MVP1 |
|---|---|
| Conta e perfil | Login Google e local, verificação, sessão, perfil profissional, logo e contato |
| Planos | Free ativo; tela de planos mensal/anual; compra Pro via Play Billing pode entrar em rollout controlado |
| Clientes e locais | Cadastro, busca, edição, histórico básico e múltiplos locais |
| Atendimentos | Criar solicitação/visita manual, data, horário, status e adicionar ao calendário por intent |
| Ambientes | Cadastrar cômodos e organizar medições por ambiente |
| Medições | Speedtest, Wi-Fi, DNS e demais motores estáveis reaproveitados |
| Evidências | Fotos, observações e anexos locais |
| Diagnóstico | Resumo técnico, recomendações e comparação antes/depois |
| Laudo | PDF padrão, compartilhamento e identidade profissional básica |
| Pix | Perfil Pix, QR Code com/sem valor, copia e cola e compartilhamento |
| Pagamento | Registro manual integral/parcial e saldo |
| Recibo | PDF numerado, imutável, cancelável e vinculado à visita |
| Offline | Fluxo da visita e geração local; sincronização posterior quando disponível |
| Qualidade | Crashlytics, analytics, testes e canal de feedback |

---

## 5. O que fica fora do MVP1

- Sincronização bidirecional com Google Agenda.
- Leitura automática de conversas do WhatsApp.
- Página pública de agendamento.
- Confirmação bancária do Pix.
- Integração com NFS-e.
- Portal web completo para o técnico.
- Equipe multiusuário e permissões avançadas.
- Drivers SignallQ Nethal experimentais com ações destrutivas.
- Automação financeira e conciliação.

---

## 6. Gates para sair do MVP1 e iniciar o MVP2

| Dimensão | Gate mínimo |
|---|---|
| Qualidade | Crash-free sessions ≥ 99,5%; sem bug bloqueador aberto; ANR dentro da meta da Play |
| Fluxo | Pelo menos 90% dos testes acompanhados conseguem concluir cliente → visita → laudo sem suporte |
| Uso real | No mínimo 15 técnicos ativos e 75 atendimentos completos registrados |
| Retenção | Pelo menos 30% dos técnicos ativos retornam e concluem novo atendimento em até 30 dias |
| Valor | Pelo menos 10 técnicos geram laudo e 5 utilizam Pix/recibo em atendimento real |
| Monetização | Sinal claro de intenção de pagamento: 5 assinantes ou 20% dos elegíveis iniciando compra/teste — **depende de preço do Pro definido (bloqueio, Canônico §8.1)** |
| Performance | Geração de laudo e recibo confiável em aparelhos alvo; sincronização sem perda de dados |
| Suporte | Principais dúvidas documentadas; nenhuma falha recorrente sem plano de correção |
| Arquitetura | Migrations testadas, contratos versionados e módulos compartilhados sem quebra do Consumer |

> **Decisão Go/No-Go.** Se qualidade ou conclusão do fluxo falhar, não iniciar features do MVP2. Corrigir o MVP1. Se uso for baixo, validar aquisição e proposta de valor antes de automatizar agenda ou criar portal. O gate de monetização não pode ser dado como cumprido enquanto o preço do Pro não for definido.

---

## 7. MVP2 — Operação conectada e recorrente

| Épico | Entrega MVP2 |
|---|---|
| Google Agenda | Conectar conta, escolher calendário, criar/atualizar/cancelar eventos e detectar conflitos |
| Solicitação via WhatsApp | Link público compartilhável para cliente solicitar atendimento |
| Sincronização | Clientes, visitas, evidências, laudos, recibos e configurações sincronizados via StorageProvider (D1 para dados estruturados; R2 hospedado só se contratado o add-on pago) |
| Assinatura madura | Mensal/anual, entitlements, grace period, restauração de compra e gestão de limites |
| Histórico avançado | Comparações por cliente/local, filtros e indicadores de evolução |
| Laudo Pro | Modelos, identidade completa, assinatura/aceite e verificação pública opcional |
| Financeiro básico | Pendências, pagamentos parciais, recibos e exportação; sem conciliação bancária |
| SignallQ Nethal aprovado | Inventário e leituras de equipamentos com drivers validados e ações seguras |
| Admin | Suporte, feature flags, métricas de funil e gestão de assinaturas/entitlements |
| Backup e recuperação | Restauração de dados e troca de aparelho com segurança |

---

## 8. Premissas de entrada de cada épico do MVP2

| Épico | Premissa |
|---|---|
| Google Agenda | Pelo menos 30% dos técnicos registram horário e pedem redução de retrabalho de agenda |
| Link público | Origem WhatsApp aparece em pelo menos 40% dos atendimentos pesquisados |
| Nuvem completa | Usuários demonstram uso recorrente ou troca entre dispositivos; modelo de segurança aprovado |
| Assinatura madura | Preço e limites Free validados; compra alpha sem falhas críticas |
| SignallQ Nethal | Driver possui testes, matriz de compatibilidade e validação em hardware real |
| Financeiro | Pix/recibo usados em volume suficiente e sem confusão com nota fiscal |
| Portal web | Demanda clara por uso em desktop; não apenas preferência estética |

---

## 9. Pós-MVP2 e hipóteses futuras

- Microsoft Outlook/Calendar e Cal.com.
- Portal web Pro completo.
- Times, técnicos subordinados e organizações.
- Cobrança Pix dinâmica e confirmação via provedor financeiro.
- Integração NFS-e e exportação contábil.
- Assinatura eletrônica e aceite do cliente.
- Automação de mensagens e lembretes.
- Catálogo ampliado de drivers SignallQ Nethal e ações remotas seguras.
- Dashboard de margem, produtividade e recorrência.

---

## 10. Sequência recomendada de implementação

| Step | Entrega |
|---|---|
| 1 | Monorepo, CI, módulos e contratos |
| 2 | Auth Worker + D1 + login Google/local |
| 3 | Perfil profissional e plano Free |
| 4 | Clientes, locais, atendimentos e ambientes |
| 5 | Medições e diagnóstico compartilhados |
| 6 | Evidências e comparação antes/depois |
| 7 | Laudo PDF e compartilhamento |
| 8 | Pix estático, pagamento e recibo |
| 9 | Offline, sync mínimo, segurança e observabilidade |
| 10 | Alpha com técnicos reais e correções |
| 11 | Play Billing mensal/anual e rollout Pro (**exige preço definido**) |
| 12 | Avaliação dos gates MVP1 → MVP2 |
| 13 | Google Agenda e solicitação pública |
| 14 | Sincronização completa, histórico e Admin |

---

## 11. Métricas principais

| Funil | Métrica |
|---|---|
| Aquisição | Instalações → contas criadas |
| Ativação | Conta → primeiro cliente → primeira visita |
| Valor | Visita → laudo compartilhado |
| Fechamento | Laudo → Pix exibido → pagamento marcado → recibo |
| Retenção | Novo atendimento em 7/30 dias |
| Monetização | Elegível → paywall → compra → renovação |
| Qualidade | Crash-free, ANR, erros de sync e falhas de PDF |

Eventos canônicos correspondentes (Canônico §3.1, `dot.case`, `source = android_pro`): `auth.succeeded`, `customer.created`, `visit.started`, `report.shared`, `pix_charge.created`, `payment.confirmed`, `paywall.viewed`, `subscription.activated`, `visit.completed_7d`, `visit.completed_30d`, `sync.failed`, `report.failed`, `feature.crash`.

---

## 12. Definition of Ready para MVP2

- Todos os gates obrigatórios estão medidos, não estimados.
- O backlog do MVP2 está priorizado por evidência de usuário.
- Contratos e migrations do MVP1 estão estabilizados.
- Não há débito crítico de segurança ou perda de dados.
- **O preço e os limites do Free foram testados com usuários reais** (bloqueado enquanto o preço do Pro não for definido, Canônico §8.1).
- Cada épico possui owner, critérios de aceite, telemetria e plano de rollback.

---

## Documentos relacionados

- `00_CANONICO_v5.md` — dicionário canônico de nomes, eventos, tokens e decisões pendentes (prevalece sobre este).
- `08_SignallQ_Pro_Especificacao_Funcional_v5.md` — visão, entidades, módulos e regras de negócio.
- `09_SignallQ_Pro_Jornada_e_Fluxo_de_Telas_v5.md` — jornada e catálogo de telas.
- `10_SignallQ_Pro_Design_System_v5.md` — tokens, componentes e apresentação de medições.
