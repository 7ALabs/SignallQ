# Auditoria de Issues — GitHub `gmmattey/linka-android`

- **Status:** proposta para aprovação humana; nenhuma alteração remota executada.
- **Data da auditoria:** 20/07/2026.
- **Fonte de verdade consultada:** GitHub Issues, comentários, labels, milestones e Projects ativos; governança do repositório em `.claude/CLAUDE.md`.
- **Escopo:** organização do backlog no GitHub. Não altera produto, código, datas ou estados nesta etapa.

## 1. Resumo executivo

Foram analisadas **65 issues abertas**, **521 fechadas** (das quais **28** com encerramento `not planned`) e **834 comentários**. O backlog atual mistura três gerações: migração do Linear, execução recente de correções do Consumer e o início do SignallQ Pro. Há valor técnico real nas issues abertas, mas a estrutura atual não permite leitura executiva: tipos, produtos, prioridade e status estão simultaneamente em labels, milestones e Projects; e vários épicos foram registrados como `Task`.

A proposta consolida a visão executiva em **7 Épicos de roadmap**. Na classificação de todas as 65 abertas há **9 issues propostas como Épico, 10 como Feature, 20 como História de Usuário, 9 como Bug e 17 como Tarefa**; os dois épicos excedentes são camadas de roadmap Pro que serão consolidadas visualmente, não novos cartões executivos. Bugs/Tarefas só recebem pai quando existir dependência ou impacto funcional real. O Project recomendado é o atual **LINKA Android — Roadmap (#8)**, remodelado para manter somente Status, Produto, Fase, Prioridade, Data de início, Data-alvo e Responsável. A visão executiva mostra exclusivamente os Épicos.

Decisão humana necessária antes da migração: a lista de produtos solicitada não inclui **SignallQ Site**, embora as issues #1147–#1155 sejam desse produto. Não é correto classificá-las como SignallQ Consumer. Recomenda-se acrescentar `SignallQ Site` ao campo Produto ou manter essas issues fora deste Project até haver Project próprio.

## 2. Diagnóstico atual

| Tema | Evidência | Diagnóstico |
|---|---|---|
| Tipos | `type:*`, `bug`, `enhancement`, `Feature`, `Estruturante`, `Release` coexistem | Tipo deve sair das labels e virar Issue Type nativo.
| Produtos | 475 `product:signallq`, 198 Admin, 130 Pro; há issues multi-produto e Site marcado como Consumer | Produto deve ser campo único; capacidades reutilizáveis usam Plataforma Compartilhada.
| Status | `status:*` e colunas do Project coexistem; Project #8 ainda usa Triagem/Em review/Docs & Higiene | Status deve ser campo do Project com os seis valores solicitados.
| Prioridade | `priority:p0`..`p3` conflita com Critical/High/Medium/Low | Converter por equivalência, preservando justificativas no corpo.
| Hierarquia | #642, #644 e #651 são épicos rotulados `Task`; #1119–#1124 são roadmaps soltos | Usar sub-issues nativas, sem inventar paternidade.
| Legado | 68 `legacy:veloo-linka`, 208 `migrated-from-linear`, referências a Linear e antigos agentes | Conservar como histórico quando fechado; atualizar ou arquivar somente após revisão item a item.
| Milestones | M3/M5 têm datas superadas e #1222 registra meta atual de 21/08/2026 | Não usar milestone como roadmap principal; ajustar somente após aprovação.

## 3. Estrutura-alvo

```text
Épico
└── Feature
    └── História de Usuário
        └── Bug ou Tarefa
```

Regras aplicadas: uma relação só é criada quando há escopo ou dependência explícita; Bugs transversais podem ser filhos da Feature ou do Épico; issues fechadas permanecem como histórico e não serão reabertas só para “completar” a árvore.

## 4. Épicos propostos para a visão executiva

| Épico | Objetivo | Produto | Fase | Responsável sugerido | Início/alvo com evidência | Features filhas | Progresso | Riscos | Issues |
|---|---|---|---|---|---|---|---|---|---|
| Confiabilidade e lançamento do SignallQ | Liberar RC e lançamento sem mascarar lacunas de QA | SignallQ | Lançamento | Claudete / Rhodolfo | RC 07/08; lançamento 21/08 (comentário #1222) | QA de release; rollout | 45% | testes em device real, Play | #651, #614–#620, #1222, #642, #644 |
| Inteligência de equipamento local | Exibir dados locais confiáveis de ONT/roteador por capacidade | SignallQ | Fase 2 | Camilo | sem data | Inventário local; suporte TP-Link | 55% | hardware, firmware, NetHAL externo | #547, #537, #865, #1213, #1216 |
| Motor canônico de diagnóstico | Uma sessão, contexto e classificação compartilhados | Plataforma Compartilhada | Estruturante | Camilo | sem data | Medição e contexto; flags remotas | 15% | migração multi-módulo, compatibilidade Pro | #1228, #1223, #1225, #1227, #1229, #1234 |
| SignallQ Pro — evolução do produto | Evoluir MVP0 validado para produto profissional vendável | SignallQ PRO | Fase 1 | Claudete / Camilo | MVP0 já iniciado; sem alvo aprovado para MVP1 | MVP0; MVP1; qualidade Pro | 40% | escopo além do MVP0 exige decisão do Luiz | #1119–#1123, #1158, #1160, #1163, #1168, #1183 |
| Plataforma de dados e Console | Tornar dados do Admin rastreáveis e operacionais | SignallQ Admin | Estruturante | Camilo | sem data | Métricas Play; custo IA; Console DS | 35% | permissões Google/GCS, dependência Android | #777, #787, #885, #942, #1060, #1061, #1137, #1171 |
| Experiência e consistência do Consumer | Eliminar dívida visual/funcional sem reintroduzir fluxos descontinuados | SignallQ | Fase 2 | Lia / Camilo | sem data | DS Consumer; jornada pós-speedtest; ferramentas | 50% | #1196 é grande demais; regressão visual | #550, #907, #975, #1015, #1023, #1058, #1117, #1169, #1172, #1182, #1196, #1200, #1201, #1206–#1212, #1217, #1224, #1226 |
| Presença digital do SignallQ | Site institucional separado do app Consumer | **Decisão pendente: SignallQ Site** | Fase 1 | Lia / Camilo | sem data | Site e PWA | 70% | produto ausente do campo solicitado | #1147–#1155, #1184 |

## 5. Features e Histórias de Usuário propostas

| Feature | Épico | Histórias de Usuário / itens de execução |
|---|---|---|
| QA de release | Confiabilidade e lançamento | #614 offline; #615 IA; #616 permissões; #618 speedtest; #620 upgrade; #1222 gate de lançamento |
| Rollout e hypercare | Confiabilidade e lançamento | #642 rollout; #644 hypercare |
| Inventário local de equipamento | Inteligência de equipamento local | #547 guarda-chuva; #537 regressão; #865 ligar leitura real; #1213 Nokia |
| Suporte a roteadores adicionais | Inteligência de equipamento local | #1216 TP-Link C6, somente após `nethal#125` |
| Sessão de medição e contexto | Motor canônico de diagnóstico | #1223 Home; #1225 pós-speedtest fechado; #1234 contexto Pro; #1228 iniciativa pai |
| Controles remotos | Motor canônico de diagnóstico | #1229 feature flags; #1224 bug de Remote Config |
| MVP0 profissional | SignallQ Pro | #1119; #1158; #1160; #1163; #1168; #1183 |
| MVP1 e operação profissional | SignallQ Pro | #1120, #1121, #1122, #1123; não detalhar sem aprovação de escopo |
| Dados operacionais do Console | Plataforma de dados e Console | #777; #787 dependente de #919; #885; #1060; #1061 |
| Qualidade de plataforma | Plataforma de dados e Console | #942 CI; #1137/#1171 design system do Console |
| Jornada de diagnóstico Consumer | Experiência e consistência | #550, #907, #952, #975, #1058, #1117, #1217 |
| Qualidade técnica e design Consumer | Experiência e consistência | #1015, #1023, #1169, #1172, #1182, #1196 |
| Site institucional | Presença digital | #1147 como Feature; #1148–#1155 como histórias/tarefas |

## 6. Tabela completa das issues abertas

Tabela operacional completa, com classificação proposta, está em [`PLANO-MIGRACAO.csv`](./PLANO-MIGRACAO.csv). Foram lidas título, corpo, labels, milestone, Project e comentários disponíveis de todas as 65 abertas. Destaques: #614–#620 têm evidência explícita de QA parcial/bloqueada; #547, #550, #787, #1213, #1216, #1217, #1222, #1223–#1229 tiveram comentários de contexto material; #1234 foi aberta no dia da auditoria.

## 7. Relação com issues fechadas

As 521 fechadas demonstram trabalho já entregue e não devem ser reabertas para reconstrução estética da árvore. Relações úteis:

- #1218 foi fechada como duplicata de #1221; manter #1221 como histórico da correção de speedtest.
- #1157, #1161, #1164, #1166 e #1176 compõem o histórico executado do MVP0 Pro; #1119 permanece como épico de validação.
- #953–#971 registram a primeira tentativa do motor/diretório remoto; #952/#1060/#1061 são a evolução planejada e não devem ser tratadas como trabalho já entregue.
- #976–#983 são histórico de consolidação de topologia; #975/#1058 são continuidade técnica, não duplicatas automáticas.
- #1206–#1212 e #1219–#1225 registram correções recentes; não marcar como concluídas pela existência de PR, pois as abertas ainda descrevem escopo remanescente.
- #307–#310 foram encerradas como `not planned`; a consolidação de tokens do Console deve ser avaliada em #1137/#1171, sem reabrir os itens antigos.

## 8. Duplicidades e sobreposições

| Itens | Conclusão | Ação proposta |
|---|---|---|
| #1218 / #1221 | Duplicidade já reconhecida pelo GitHub | manter fechamento de #1218; relacionar histórico a #1228 |
| #410 / #550 | #410 está concluída; #550 preserva escopo futuro mais detalhado | manter #550, atualizar referência à funcionalidade já entregue |
| #547 / #865 / #1213 / #1216 | não são duplicatas: épico, ligação de runtime, bug Nokia e expansão TP-Link | criar hierarquia explícita |
| #952 / #1060 | mesma linha de produto em fases distintas | #952 Feature, #1060 história/tarefa Fase 0 |
| #951 / #1061 | mesma linha de produto em fases distintas | #951 Feature, #1061 história/tarefa Fase 1 |
| #1169 / #1196 | ambas tratam DS Consumer, mas #1196 é auditoria concreta | #1169 Feature; #1196 dividir em tarefas filhas |
| #1137 / #1171 | ambas tratam Console DS, com escopos complementares | consolidar sob uma única Feature, sem fechar automaticamente |

## 9. Itens legados

| Classe | Itens | Classificação |
|---|---|---|
| Linka/Veloo fechado | grande parte das 68 `legacy:veloo-linka` | histórico, exceto quando o corpo descreve comportamento atual ainda aberto |
| Linear migrado | 208 `migrated-from-linear` | parcialmente válido: reclassificar apenas os abertos, manter os fechados como histórico |
| Operação Linear/Slack/Notion | #696–#732, fechadas/canceladas | obsoleto após migração de 09/07; não reabrir |
| Antigos agentes Felipe/Gema | #644, #651 e documentação relacionada | parcialmente válido: atualizar responsável sugerido para Claudete/Rhodolfo |
| NetHAL | #885 fechada, #1216 aberta | transversal: usar Plataforma Compartilhada somente quando infra for realmente comum; dependência no repo NetHAL continua externa |

## 10. Candidatas a fechamento

Nenhuma issue aberta deve ser fechada sem nova revisão do corpo e evidência atual. Candidatas condicionais, não automáticas:

- #550, se a funcionalidade concluída em #410 cobrir integralmente o escopo futuro remanescente — a auditoria indica que não há prova suficiente ainda.
- #1015, apenas se a migração avaliada em #1008 tiver incluído subset real da fonte — falta evidência de execução.
- #1007, se a documentação já estiver corrigida integralmente — requer diff documental.
- #787, somente após verificar estado real de #919 e dados em produção; o comentário ainda a marca bloqueada.

## 11. Issues que precisam ser divididas

| Issue | Motivo | Divisão mínima proposta |
|---|---|---|
| #1196 | 12 lotes / ~30 call sites; não é unidade verificável | 12 tarefas por lote/tela, sob Feature DS Consumer |
| #1213 | driver, sessão, saúde GPON, captura parcial e reboot | validação de host; sessão; parsing/capabilities; UX de estados; reboot seguro |
| #1217 | identidade, deduplicação, limites e confiança | identidade; resultado parcial tipado; dedup/mDNS; limites de varredura; UI de confiança |
| #1223 | dados, classificação, estados e Home | vínculo de execução; classificação canônica; estados/refresh; regressão Home |
| #1226 | contatos, links, identificação e estados | modelo de contato; ações externas; estado de identificação; testes |
| #1227 | conexão, privacidade, permissões, persistência e suporte | perfil por rede; consentimento; permissões; dados/limpeza; suporte |
| #1228 | iniciativa multi-produto | transformar em Épico e criar Features de contrato/sessão, classificação, diagnóstico/recomendação e migração |

## 12. Labels

**Remover das issues após migração:** `product:*`, `type:*`, `priority:*`, `status:*`, `agent:*`, `migrated-from-linear`, `legacy:veloo-linka`, `Feature`, `Estruturante`, `Release`, `Planejamento`, `Analítico`, `GoLive`, `android`, `backend`, `frontend`, `database`, `qa` e variantes de processo. Elas devem virar campos, comentário histórico ou descrição.

**Consolidar para labels simples de domínio (máximo duas por issue):** Wi-Fi, Velocidade, Diagnóstico, Dispositivos, Histórico, Design, Play Store, Arquitetura, Documentação, Segurança, Privacidade, Fibra, Operadoras, Permissões, Dados, Console, Site, Qualidade e Legado. `area:diagnostic` e `area:diagnostico` viram **Diagnóstico**; `area:security` e `area:seguranca` viram **Segurança**; `area:ui`, `area:ux`, `area:design-system` viram **Design** quando não houver domínio mais específico.

## 13. Milestones

- **Manter como histórico fechado:** #3–#7.
- **Manter, mas sem usar para hierarquia:** M0 (#8), M1 (#9), M2 (#10), M4 (#12), M5 (#13), H1 (#14).
- **Ajustar após aprovação:** #10 ainda diz “provavelmente cobertas”, contradito pelos comentários de #614–#620; #13 ainda traz 07/08, enquanto #1222 registra 21/08 como meta pública; H1 deve derivar da data de go-live efetiva, não do snapshot 07/08.
- **Remover somente quando não houver referência:** #5 já está fechado e vazio; não é necessário apagá-lo nesta migração.

## 14. Plano de migração em blocos

1. **Backup lógico e validação:** exportar issues, comentários, labels, milestones e items do Project; conferir #1222 e a decisão sobre SignallQ Site.
2. **Configurar o Project #8:** criar campos e opções solicitados, sem mover cards ainda; preparar visão Roadmap filtrada por Tipo=Épico.
3. **Criar tipos e árvore piloto:** usar 3 épicos (Lançamento, Motor Canônico, Pro) como piloto; criar/reclassificar apenas pais existentes e sub-issues naturais.
4. **Migrar campos:** preencher Produto, Fase, Prioridade, Status, Responsável e datas somente com evidência; não inventar datas.
5. **Simplificar labels:** adicionar labels de domínio antes de remover as antigas; limitar a duas por item e preservar rastreabilidade no comentário de migração.
6. **Normalizar roadmap e legado:** ajustar milestones obsoletos e comentários históricos; não fechar candidatas sem aprovação explícita.
7. **Auditoria pós-migração:** comparar totais, relações, cards sem campo e itens fora do Project; registrar reversão por lote.

## 15. Comandos `gh` e GraphQL necessários

```powershell
# leitura/auditoria
gh issue list --repo gmmattey/linka-android --state all --limit 2000 --json number,title,body,labels,milestone,assignees,closedAt,createdAt,updatedAt,state,stateReason,projectItems,url
gh api --paginate 'repos/gmmattey/linka-android/issues/comments?per_page=100'
gh label list --repo gmmattey/linka-android --limit 1000 --json name,description,color
gh api 'repos/gmmattey/linka-android/milestones?state=all&per_page=100'

# Project v2: schema e itens (usar somente após aprovação para mutações)
gh api graphql -f query='query { user(login:"gmmattey") { projectsV2(first:20) { nodes { id number title fields(first:100) { nodes { __typename ... on ProjectV2Field { id name dataType } ... on ProjectV2SingleSelectField { id name dataType options { id name } } } } } } } }'
gh project item-list 8 --owner gmmattey --limit 1000 --format json
```

Mutações futuras devem ser executadas em lotes reversíveis e registradas em comentário de migração; exemplos incluem `gh project field-create`, `gh project item-edit` e mutations GraphQL `addSubIssue`/`removeSubIssue`, depois de confirmar os IDs reais.

## 16. Riscos e decisões humanas pendentes

1. Aprovar ou não o Produto adicional **SignallQ Site** no Project; sem isso, #1147–#1155 não podem ser corretamente classificados.
2. Confirmar se #1222 substitui todas as datas de M5/H1 e autorizar atualização do marco.
3. Definir se o Project #8 substitui o Project #9 de bugs ou se #9 será mantido como visão filtrada; evitar cards duplicados sem regra.
4. Aprovar o uso de Issue Types nativos e sub-issues para a hierarquia; eles reduzem a dependência de labels.
5. Decidir se itens da Plataforma Compartilhada serão mantidos neste repositório enquanto o monorepo-alvo ainda é proposta.
6. Validar as quatro candidatas a fechamento com evidência atual antes de qualquer ação irreversível.
