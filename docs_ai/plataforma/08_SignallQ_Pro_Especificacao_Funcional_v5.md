# SignallQ Pro — Especificação Funcional

**Status:** ativo · **Versão:** 5.0 · **Data:** 17/07/2026 · **Substitui:** Especificação Funcional Completa v4

## Estado atual vs. Alvo

Todo o **SignallQ Pro** descrito aqui é **🎯 ALVO** — o app não existe ainda (Canônico §1). É um aplicativo Android novo, `io.signallq.pro`, com Firebase e Play separados. Esta especificação define o produto desejado, não o implementado.

O que já é **✅ ATUAL** e serve de base: o **SignallQ** consumidor (`io.signallq.app`, repo `gmmattey/linka-android`) com seus motores de medição/diagnóstico reaproveitáveis, o **SignallQ Admin** (React/Vite, dentro do monorepo) que operará também o Pro, e os **Workers Cloudflare** já em produção. Os identificadores técnicos legados (`linkaKotlin.db`, `linkaPreferencias`, canais `linka_*`, repo `gmmattey/linka-android`) nunca são renomeados.

Os nomes de entidade, eventos de telemetria (`dot.case`) e tokens desta especificação seguem `00_CANONICO_v5.md`; em qualquer divergência, o Canônico prevalece.

---

## Sumário

1. Visão do produto
2. Perfis e proposta de valor
3. Escopo e limites
4. Jornadas ponta a ponta
5. Modelo funcional e entidades
6. Módulos e funcionalidades
7. Telas e navegação
8. Regras de negócio
9. Planos e monetização
10. Estados, exceções e offline
11. Privacidade e segurança funcional
12. Métricas e eventos
13. MVP1, MVP2 e critérios de saída
14. Critérios de aceite
15. Portal público SignallQ
16. Posicionamento do SignallQ Nethal
17. Portfólio público
18. Consolidação funcional
- Anexo A — Glossário

**Promessa central:** do primeiro contato ao recibo — organize o cliente, execute a visita, registre evidências, demonstre a melhoria, entregue um laudo e receba pelo serviço.

---

## 1. Visão do produto

O SignallQ Pro é um aplicativo Android separado do SignallQ gratuito, voltado a técnicos de informática, instaladores de redes, consultores e pequenos provedores. Seu papel é transformar medições de conectividade em um atendimento profissional rastreável, comprovável e vendável.

### 1.1 Objetivos do produto

- Reduzir improviso e retrabalho durante visitas técnicas.
- Padronizar diagnósticos de Wi-Fi e conectividade sem exigir equipamento enterprise.
- Produzir evidências compreensíveis para o cliente.
- Aumentar a percepção de profissionalismo e a taxa de cobrança.
- Criar histórico por cliente, local, visita e ambiente.

### 1.2 Princípios

| Princípio | Aplicação |
|---|---|
| Campo primeiro | Ações principais acessíveis com uma mão, leitura rápida e funcionamento offline. |
| Evidência antes de opinião | Toda recomendação relevante deve apontar para uma medição, foto, observação ou comparação. |
| Linguagem em camadas | Resumo simples para o cliente; detalhe técnico disponível para o profissional. |
| Imutabilidade documental | Laudos e recibos emitidos não são silenciosamente alterados. |
| Sem falsa confirmação | Pix é confirmado manualmente no MVP; o app não afirma validação bancária. |
| Produto separado | SignallQ Pro tem app, identidade, plano e ciclo de release próprios. |

---

## 2. Perfis e proposta de valor

| Perfil | Necessidade principal | Valor entregue |
|---|---|---|
| Técnico autônomo | Organizar e comprovar o serviço | Fluxo completo, laudo, Pix e recibo. |
| Instalador de redes | Medir ambientes e demonstrar cobertura | Medições por cômodo e antes/depois. |
| Consultor de Wi-Fi | Padronizar diagnóstico e recomendação | Evidências, histórico e relatório personalizável. |
| Pequeno provedor | Apoiar visita de campo sem sistema complexo | Agenda, cliente, visita e exportação. |
| Supervisor futuro | Acompanhar qualidade e produtividade | Painel, templates e governança no MVP2+. |

---

## 3. Escopo e limites

### 3.1 Dentro do escopo

- Autenticação Google e e-mail/senha.
- Perfil profissional, identidade, Pix e assinatura.
- Clientes, locais, contatos, solicitações, agendamentos e visitas.
- Ambientes, medições, fotos, observações e evidências.
- Comparação antes/depois, recomendações e laudo PDF.
- Cobrança Pix estática, pagamentos parciais e recibo digital.
- Histórico e busca por cliente.
- Funcionamento offline com sincronização posterior.
- Operação e acompanhamento da plataforma pelo SignallQ Admin já existente, migrado do `linka-android`.

### 3.2 Fora do escopo inicial

- Emissão de nota fiscal ou substituição de NFS-e.
- Confirmação bancária automática de Pix.
- CRM completo ou central oficial de WhatsApp.
- Calendário visual próprio completo.
- Acesso remoto a roteadores fora da rede local.
- Ações destrutivas em equipamentos sem driver validado.
- Gestão de equipes, comissionamento e estoque no MVP1.

---

## 4. Jornadas ponta a ponta

### 4.1 Ativação

1. Abrir o app e autenticar com Google ou e-mail.
2. Aceitar termos e configurar perfil profissional.
3. Escolher Free ou iniciar assinatura Pro mensal/anual.
4. Configurar logo, dados comerciais e chave Pix.
5. Criar primeiro cliente ou atendimento.

### 4.2 Atendimento completo

1. Registrar solicitação recebida por WhatsApp, ligação ou indicação.
2. Criar ou localizar Cliente e Local.
3. Definir data e horário; adicionar ao calendário externo.
4. Realizar check-in e confirmar rede principal.
5. Cadastrar Ambientes e executar medições por cômodo.
6. Adicionar fotos, observações e evidências.
7. Executar intervenção e repetir medições.
8. Comparar antes/depois e revisar recomendações.
9. Gerar e compartilhar laudo.
10. Gerar cobrança Pix; confirmar recebimento manualmente.
11. Emitir e compartilhar recibo digital.
12. Encerrar visita e manter histórico.

### 4.3 Recuperação e continuidade

- Rascunho automático durante toda a visita.
- Visita pode ser retomada após encerramento do app.
- Falha de rede não bloqueia medição local, fotos e observações.
- Itens pendentes de sincronização ficam visíveis e são reenviados.
- Laudo só é emitido quando dados obrigatórios estão válidos.

---

## 5. Modelo funcional e entidades

Nomes de entidade seguem o glossário canônico (Canônico §2). Tabelas D1 entre parênteses.

| Entidade (classe) | Tabela D1 | Descrição | Relacionamentos |
|---|---|---|---|
| `Account` | `account` | Conta de acesso e identidades vinculadas. | `ProfessionalProfile`, `Subscription`, `Session`, `IdentityProvider` |
| `ProfessionalProfile` | `professional_profile` | Dados comerciais, logo, assinatura e Pix. | `Account`, `ReportTemplate`, `PixProfile` |
| `Customer` | `customer` | Pessoa ou empresa atendida (Cliente). | `Contact`, `Location`, `Visit` |
| `Location` | `service_location` | Endereço ou local técnico do cliente (Local). | `Customer`, `Environment`, `Visit` |
| `Appointment` | `appointment` | Reserva de atendimento. | `Customer`, `Location`, `externalEventId` |
| `Visit` | `visit` | Unidade principal de execução do serviço. | `Appointment`, `MeasurementSession`, `Evidence`, `Report`, `Payment` |
| `Environment` | `environment` | Cômodo ou zona do imóvel (Ambiente). | `Location`, `MeasurementSession`, `Evidence` |
| `MeasurementSession` | `measurement_session` | Agrupador de leituras de um ambiente em uma fase (Medição). | `Visit`, `Environment`, fase BEFORE/AFTER, `MeasurementPoint` |
| `MeasurementPoint` | `measurement_point` | Amostra individual dentro da medição. | `MeasurementSession` |
| `Evidence` | `evidence` | Foto, nota, anexo ou marcador. | `Visit`, `Environment` |
| `Report` | `report` | Laudo emitido e versionado. | `Visit`, `ReportSnapshot` |
| `Payment` | `payment` | Recebimento total ou parcial. | `Visit`, `PixCharge`, `Receipt` |
| `PixCharge` | `pix_charge` | Cobrança Pix estática gerada. | `Payment`, `PixProfile` |
| `Receipt` | `receipt` | Comprovante imutável de pagamento. | `Payment`, `Customer`, `Visit` |
| `Subscription` | `subscription` | Plano e direito de acesso. | `Account`, `Entitlement` |

Notas de reconciliação (Canônico §2): `client`/`client.created` **não** se usa — a entidade é `Customer`. A tabela do local é `service_location` (não `locations`). O cômodo é `Environment`/`environment` (não `room`). A conta é `Account`/`account` (não `User`/`users`). A medição da v4 (`Measurement`) foi desmembrada em `MeasurementSession` (agrupador) + `MeasurementPoint` (amostra).

---

## 6. Módulos e funcionalidades

### 6.1 Conta e autenticação

- Login Google via Credential Manager.
- Cadastro local com verificação de e-mail.
- Recuperação de senha.
- Vinculação de Google e senha à mesma conta (Identidade / `IdentityProvider`).
- Sessões por dispositivo e opção sair de todos (`Session`).
- Exclusão de conta e exportação de dados.

### 6.2 Painel inicial

- Resumo do dia.
- Próximos atendimentos.
- Pendências de sincronização, laudo ou pagamento.
- Ações rápidas: atendimento, cliente, medição e cobrança.
- Indicadores básicos de uso do plano.

### 6.3 Clientes e locais

- Pessoa física ou jurídica.
- Múltiplos contatos e locais.
- Busca por nome, telefone e endereço.
- Histórico consolidado.
- Observações permanentes separadas das observações da visita.

### 6.4 Agenda e solicitações

- Cadastro manual com origem WhatsApp, telefone, indicação ou web.
- Status: solicitado, pendente, confirmado, em deslocamento, em andamento, concluído, cancelado e ausência.
- Inserção no calendário por intent no MVP1.
- Integração Google Calendar bidirecional no MVP2.
- Mensagem pronta para confirmação e reagendamento.

### 6.5 Visita técnica

- Check-in manual.
- Checklist inicial.
- Rede e equipamento principal.
- Ambientes previstos e visitados.
- Cronologia de ações.
- Pausa, retomada e cancelamento com motivo.

### 6.6 Medições

- Sinal Wi-Fi, rede/banda, velocidade, resposta, oscilação e estabilidade conforme capacidade do aparelho.
- Fase antes/depois (`MeasurementSession` com fase BEFORE/AFTER).
- Múltiplas amostras por ambiente (`MeasurementPoint`).
- Marcação de amostra representativa.
- Avisos sobre limitações e contexto.

### 6.7 Evidências

- Fotos pela câmera ou galeria.
- Notas e marcadores.
- Vínculo com visita e ambiente.
- Compressão com preservação de legibilidade.
- Consentimento e exclusão antes da emissão.

### 6.8 Diagnóstico e recomendações

- Resumo técnico determinístico.
- Explicação em linguagem simples.
- Recomendação ligada à evidência.
- IA como enriquecimento, nunca única fonte da conclusão.
- Fallback local sem internet.

### 6.9 Antes e depois

- Comparação por ambiente e consolidada.
- Indicadores de melhoria, estabilidade e cobertura.
- Declaração cautelosa quando não houver evidência suficiente.
- Seleção das comparações que entram no laudo.

### 6.10 Laudo

- Prévia, geração PDF e compartilhamento.
- Logo e dados do profissional no Pro.
- Resumo, metodologia, ambientes, resultados, evidências e recomendações.
- Número, hash, data de emissão e snapshot imutável (`ReportSnapshot`).
- Cancelamento e nova versão sem sobrescrever o original.

### 6.11 Pagamentos e recibos

- Perfil Pix estático (`PixProfile`).
- QR Code com ou sem valor e copia e cola (`PixCharge`).
- Pagamento total ou parcial.
- Confirmação manual claramente identificada.
- Recibo numerado, imutável, compartilhável e verificável.
- Não se apresenta como documento fiscal.

### 6.12 Histórico

- Linha do tempo por cliente e local.
- Filtros por período, status e tipo.
- Reabertura somente como nova visita ou complemento controlado.
- Exportação e exclusão conforme plano e política.

### 6.13 SignallQ Admin

O SignallQ Admin é a aplicação interna de operação e acompanhamento do ecossistema. Ele já existe dentro do repositório `linka-android` (✅ ATUAL) e será preservado e evoluído no monorepo, passando a operar também o Pro.

O painel deve permitir, conforme autorização: acompanhar usuários e assinaturas; versões e adoção; eventos e funis; feedbacks; falhas e diagnósticos; consumo e custo de IA; telemetria sanitizada; regras e catálogos remotos; indicadores de suporte e operação.

O painel não faz parte da experiência do técnico, não é disponibilizado na Play Store e não utiliza a mesma autorização das contas profissionais. Acesso administrativo deve ser restrito, auditável e revogável.

---

## 7. Telas e navegação

Navegação raiz recomendada: **Início, Atendimentos, Clientes e Ajustes.** Fluxos de execução usam navegação empilhada com top bar e ação principal persistente, sem menu hambúrguer.

| Grupo | Telas principais |
|---|---|
| Entrada | Splash, login, criar conta, verificar e-mail, recuperar senha, termos. |
| Ativação | Escolha do plano, perfil profissional, identidade visual, Pix, permissões. |
| Núcleo | Painel, agenda, clientes, atendimento, ambientes, medição, evidências. |
| Entrega | Antes/depois, diagnóstico, prévia do laudo, compartilhar. |
| Financeiro | Cobrança Pix, confirmar pagamento, recibo, histórico financeiro. |
| Conta | Plano, assinatura, sincronização, privacidade, ajuda e sobre. |

---

## 8. Regras de negócio

| ID | Regra |
|---|---|
| RN-001 | Uma visita pertence a exatamente um cliente e um local. |
| RN-002 | Medição precisa registrar instante, ambiente, fase e contexto mínimo. |
| RN-003 | Antes/depois só compara métricas compatíveis e coletadas na mesma visita ou em visitas explicitamente selecionadas. |
| RN-004 | Laudo emitido é snapshot imutável; correção gera nova versão. |
| RN-005 | Recibo emitido não é editado; correção exige cancelamento e substituição. |
| RN-006 | Pagamento Pix no MVP é confirmado pelo técnico, não pelo banco. |
| RN-007 | A soma de pagamentos não pode ultrapassar o valor da visita sem confirmação explícita de crédito excedente. |
| RN-008 | Recibo só pode ser emitido para valor efetivamente marcado como recebido. |
| RN-009 | Usuário Free mantém acesso de leitura aos documentos já emitidos após atingir limite. |
| RN-010 | Expiração Pro não apaga dados; bloqueia apenas capacidades premium e novas operações limitadas. |
| RN-011 | Ações em roteador dependem de capability declarada e driver aprovado. |
| RN-012 | Ausência de conectividade não pode causar perda silenciosa da visita. |
| RN-013 | Dados sensíveis não entram em telemetria bruta. |
| RN-014 | O app não afirma descumprimento da operadora com base em uma única medição. |

---

## 9. Planos e monetização

O produto terá assinatura mensal e anual via Google Play Billing. O backend mantém entitlements com status ACTIVE, GRACE_PERIOD, PAUSED ou EXPIRED; não usa apenas um booleano `isPro`.

> **Pendência de bloqueio (Canônico §8.1):** o **preço do Pro** (mensal/anual) não está definido em nenhum documento. Enquanto não houver valor, o gate de monetização do roadmap não fecha. Não inventar valor aqui.

| Capacidade | Free | Pro |
|---|---|---|
| Clientes ativos | Até 3 | Ilimitado ou limite operacional alto |
| Visitas novas/mês | Até 3 | Ilimitado conforme política comercial |
| Medições | Básicas | Completas e comparação |
| Laudo | Modelo padrão com marca SignallQ Pro | Personalizado com logo e dados |
| Recibo e Pix | Incluído | Incluído + histórico completo |
| Histórico | Limitado | Completo |
| Sincronização | Essencial | Completa e prioritária |
| Agenda | Adicionar ao calendário | Integração avançada no MVP2 |
| Suporte | Base de ajuda | Prioritário conforme plano |

---

## 10. Estados, exceções e offline

| Situação | Comportamento esperado |
|---|---|
| Sem internet no login | Permitir acesso somente se houver sessão válida em cache; explicar limitação. |
| Sem internet na visita | Permitir operação local e enfileirar sincronização. |
| Falha no upload de foto | Manter arquivo local, exibir pendência e tentar novamente. |
| Conflito de edição | Priorizar versão com revisão explícita; nunca descartar conteúdo automaticamente. |
| Assinatura não validada | Usar cache com prazo; depois aplicar Free sem apagar dados. |
| QR Pix inválido | Bloquear compartilhamento e indicar campo incorreto. |
| PDF falha | Manter visita concluída e permitir nova tentativa. |
| Driver incerto | Somente leitura segura ou ocultar ação; nunca tentar escrita por inferência fraca. |

---

## 11. Privacidade e segurança funcional

### 11.1 Regras funcionais do painel administrativo

Nenhum dado de rede bruto ou credencial de roteador deve ser exibido no Admin. Telemetria deve chegar sanitizada, com mascaramento ou hash conforme finalidade declarada.

Toda ação administrativa com impacto em usuário, assinatura, catálogo ou regra remota deve registrar autor, data, objeto afetado e resultado.

Perfis administrativos devem usar menor privilégio: leitura, suporte, operação, produto e administrador não são equivalentes.

### 11.2 Regras funcionais do app

- Consentimento específico para fotos, telemetria e diagnóstico de rede.
- Segredos e credenciais de roteador não entram em laudos, logs ou telemetria.
- IP público oculto por padrão em documentos compartilhados.
- Permissões Android solicitadas no contexto e com justificativa.
- Exclusão de conta com janela de recuperação e política de retenção.
- QR e hash de verificação não devem expor dados pessoais desnecessários.

---

## 12. Métricas e eventos

**Convenção canônica: `dot.case`** para nomes de evento; propriedades em `snake_case` (Canônico §3). A tabela abaixo já corrige o `snake_case` flat que a v4 usava.

| Grupo | Eventos canônicos (dot.case) |
|---|---|
| Ativação | `auth.started`, `auth.succeeded`, `profile.completed`, `pix.configured` |
| Uso | `customer.created`, `appointment.created`, `visit.started`, `environment.measured` |
| Entrega | `comparison.viewed`, `report.generated`, `report.shared` |
| Receita | `paywall.viewed`, `trial.started`, `subscription.activated`, `pix_charge.created`, `payment.confirmed` |
| Qualidade | `sync.failed`, `report.failed`, `measurement.failed`, `feature.crash` |
| Retenção | `visit.completed_7d`, `visit.completed_30d`, `customer.returned` |

### 12.1 Mapa de migração (v4 snake → canônico dot)

`signup_started`→`auth.started` · `signup_completed`→`auth.succeeded` · `profile_completed`→`profile.completed` · `pix_configured`→`pix.configured` · `customer_created`→`customer.created` · `appointment_created`→`appointment.created` · `visit_started`→`visit.started` · `environment_measured`→`environment.measured` · `comparison_viewed`→`comparison.viewed` · `report_generated`→`report.generated` · `report_shared`→`report.shared` · `paywall_viewed`→`paywall.viewed` · `trial_started`→`trial.started` · `subscription_activated`→`subscription.activated` · `pix_charge_created`→`pix_charge.created` · `payment_confirmed`→`payment.confirmed` · `sync_failed`→`sync.failed` · `report_failed`→`report.failed` · `measurement_failed`→`measurement.failed` · `feature_crash`→`feature.crash` · `visit_completed_7d`→`visit.completed_7d` · `visit_completed_30d`→`visit.completed_30d` · `customer_returned`→`customer.returned`.

**ID de fonte (`source`) do Pro:** `android_pro` (Canônico §3.1).

---

## 13. MVP1, MVP2 e critérios de saída

### 13.1 MVP1

- Conta, perfil e planos.
- Clientes, locais, atendimentos e calendário por intent.
- Visita, ambientes, medições, evidências e offline.
- Antes/depois, laudo PDF, Pix e recibo.
- Histórico, sincronização e telemetria mínima.

### 13.2 Gate para MVP2

| Dimensão | Premissa mínima |
|---|---|
| Estabilidade | Crash-free users ≥ 99,5% e nenhum defeito crítico aberto no fluxo visita→laudo→recibo. |
| Uso real | Pelo menos 30 técnicos ativados e 100 visitas concluídas com dados válidos. |
| Retenção | Sinal de recorrência: técnicos repetindo uso em semanas distintas. |
| Valor | Laudos compartilhados, pagamentos registrados e feedback positivo sobre profissionalização. |
| Monetização | Conversão ou intenção de pagamento suficiente para validar o plano Pro (**depende de preço definido — pendência de bloqueio**). |
| Operação | Suporte, privacidade, restore e sincronização testados. |

### 13.3 MVP2

- Google Calendar conectado e sincronizado.
- Página pública de solicitação/agendamento.
- Templates avançados de laudo.
- Integrações externas selecionadas.
- Drivers SignallQ Nethal aprovados e ações seguras.
- Painel de produtividade e recursos de equipe quando houver demanda comprovada.

---

## 14. Critérios de aceite

| Fluxo | Critério de aceite resumido |
|---|---|
| Conta | Usuário cria, valida, entra, recupera senha e vincula Google sem duplicar conta. |
| Visita offline | Consegue iniciar, medir, fotografar, concluir e sincronizar depois sem perda. |
| Medição | Cada resultado exibe contexto, limitações e origem do dado. |
| Laudo | PDF reproduz o snapshot da visita e abre corretamente em Android, web e mensageiros. |
| Pix | QR e copia e cola representam os dados configurados; confirmação permanece manual. |
| Recibo | Número único, valor correto, vínculo com pagamento e impossibilidade de edição silenciosa. |
| Plano | Limites Free são aplicados sem bloquear leitura ou apagar histórico. |
| Privacidade | Telemetria e documentos não vazam credenciais, SSID/MAC/IP bruto por padrão. |

---

## 15. Portal público SignallQ

O Portal SignallQ é a presença pública do ecossistema (🎯 ALVO — funde `linka-webapp` + `linka-speedtest`, Canônico §1). Combina ferramenta gratuita de teste de velocidade, apresentação dos produtos, aquisição, suporte, conteúdo educativo e cumprimento das obrigações jurídicas e de loja.

### 15.1 Jornada principal do visitante

1. Acessar a página inicial.
2. Iniciar o teste de velocidade sem cadastro.
3. Visualizar velocidade, resposta, estabilidade e diagnóstico resumido.
4. Receber orientação para melhorar a conexão.
5. Conhecer o SignallQ ou o SignallQ Pro conforme seu perfil.
6. Acessar a Google Play para instalar o aplicativo adequado.

### 15.2 Conteúdo comercial

- Página individual do SignallQ.
- Página individual do SignallQ Pro.
- Comparativo objetivo entre os dois aplicativos.
- Página de planos e valores do SignallQ Pro (**valor pendente**).
- Links oficiais de download.
- Perguntas frequentes e conteúdo educativo.

### 15.3 Conteúdo jurídico e suporte

- Política de privacidade do SignallQ.
- Política de privacidade do SignallQ Pro.
- Política aplicável ao portal e ao speedtest.
- Termos de uso.
- Canal de contato e suporte.
- Página pública de exclusão de conta e dados.

### 15.4 Monetização do portal

A monetização prioritária ocorre na experiência gratuita de speedtest. A publicidade deve ser complementar e nunca prejudicar a confiança, a leitura do resultado ou a operação dos controles.

- Bloco publicitário após resultado e diagnóstico.
- Bloco adicional entre conteúdos educativos.
- Nenhum anúncio colado a botões ou controles.
- Páginas jurídicas sem publicidade.
- Páginas comerciais do Pro preferencialmente sem distrações publicitárias.

Eventos do portal (Canônico §3.1): `web_speedtest.completed`, `app_download.clicked`, `ad_slot.viewable`; `source = portal_web`.

---

## 16. Posicionamento do SignallQ Nethal

O **SignallQ Nethal** (🎯 ALVO; hoje repo separado `gmmattey/nethal`) não é um produto comercial voltado ao público. É uma ferramenta interna de laboratório, usada enquanto a plataforma valida compatibilidade e segurança de integração com equipamentos de rede.

- Usuários: equipe interna e testadores autorizados.
- Distribuição: privada.
- Sem marca ou campanha comercial própria.
- Sem dependência obrigatória para o usuário do SignallQ ou do SignallQ Pro.
- Pode ser encerrado após a incorporação dos recursos estáveis aos produtos oficiais.

> Grafia canônica: **SignallQ Nethal** (Canônico §2). Não usar `NetHAL`, `NetHAL Lab`, `nethal-lab`.

---

## 17. Portfólio público

| Produto | Público | Papel | Disponibilidade |
|---|---|---|---|
| SignallQ | Consumidor final | Diagnóstico e melhoria da própria conexão. | Google Play. |
| SignallQ Pro | Técnicos e prestadores | Transformar medições em serviço profissional. | Google Play. |
| Portal SignallQ | Público geral | Speedtest, conteúdo, comparação, preços, downloads e políticas. | Web pública. |
| SignallQ Admin | Operação interna | Gestão, observabilidade, suporte e administração. | Acesso restrito. |
| SignallQ Nethal | Equipe técnica | Validação temporária de hardware e drivers. | Distribuição interna. |

---

## 18. Consolidação funcional

### 18.1 Armazenamento do profissional

O profissional pode operar localmente e escolher uma pasta própria, inclusive disponibilizada por seu provedor de nuvem no Android (Android SAF). O app informa o estado de sincronização; a troca de provider não perde vínculos; o storage hospedado pela SignallQ é opcional.

> Canônico §6: o storage padrão é **local-first + Android SAF**, abstraído por `StorageProvider`. R2 é apenas **add-on hospedado pago** (`SignallQHostedProvider`), nunca o storage padrão.

### 18.2 Operação pelo SignallQ Admin

A equipe autorizada acompanha o SignallQ e o SignallQ Pro no mesmo painel, mantendo separação de dados e métricas: usuários e profissionais, assinaturas, visitas e laudos em métricas agregadas, saúde/versões/falhas e solicitações de privacidade.

### 18.3 SignallQ Nethal

O antigo NetHAL Lab passa a se chamar **SignallQ Nethal** e permanece ferramenta técnica interna: sem oferta comercial, drivers reutilizados pelos produtos oficiais, distribuição privada e arquivamento quando não agregar testes exclusivos.

---

## Anexo A — Glossário

| Termo | Definição |
|---|---|
| Atendimento | Solicitação ou compromisso ainda não iniciado. |
| Visita | Execução concreta do serviço. |
| Ambiente | Cômodo ou zona avaliada. |
| Evidência | Registro que sustenta uma conclusão. |
| Laudo | Documento técnico da visita. |
| Recibo | Comprovante não fiscal de valor recebido. |
| Capability | Ação ou leitura que um equipamento suporta com segurança. |
| Entitlement | Direito de acesso derivado do plano/assinatura. |

---

## Documentos relacionados

- `00_CANONICO_v5.md` — dicionário canônico de nomes, eventos, tokens e decisões (prevalece sobre este).
- `09_SignallQ_Pro_Jornada_e_Fluxo_de_Telas_v5.md` — jornada e catálogo de telas.
- `10_SignallQ_Pro_Design_System_v5.md` — sistema visual e componentes.
- `11_SignallQ_Pro_Roadmap_MVP1_MVP2_v5.md` — fases, gates e sequência de implementação.
