# SignallQ Pro — Jornada e Fluxo de Telas

**Status:** ativo · **Versão:** 5.0 · **Data:** 17/07/2026 · **Substitui:** Jornada e Fluxo de Telas v3

## Estado atual vs. Alvo

Toda a jornada e o catálogo de telas descritos aqui são **🎯 ALVO** — o **SignallQ Pro** (`io.signallq.pro`) ainda não existe (Canônico §1). O documento orienta protótipo, implementação e caderno de testes do produto desejado.

O **✅ ATUAL** é o SignallQ consumidor e seus tokens de marca (primary `#5B21D6`, secondary `#2851B8`, confirmados no código). As cores de status/chip desta jornada seguem os presets do design system do Pro (identidade **azul `#0B6CFF`**; usar `StatusChip`/`ATENDIMENTO_STATUS` do projeto `77a19317`) e a escala de sinal de 6 níveis — ver Canônico §5.2. Os tokens `#6C2BFF` e a paleta teal-dominante anterior estão **mortos** (atualização 2026-07-18). Entidades e eventos seguem o glossário canônico (`dot.case`).

---

## Visão geral

### 1. Objetivo deste documento

Define como o profissional percorre o SignallQ Pro desde o primeiro acesso até a entrega do laudo. Conecta objetivos do usuário, estados da visita, navegação, telas, decisões e exceções operacionais.

**Resultados esperados**

- Reduzir o tempo para iniciar uma visita e registrar o contexto correto.
- Evitar medições soltas, sem cliente, local, ambiente ou finalidade.
- Permitir continuidade mesmo com interrupções ou ausência de internet externa.
- Transformar dados técnicos em comparação compreensível e laudo vendável.
- Manter histórico reutilizável por cliente e local.

---

## Personas

### 2. Quem percorre a jornada

**Contexto de uso.** O profissional normalmente está em pé, circulando pelo imóvel, alternando entre o aplicativo, o roteador, cabos e outros equipamentos. A experiência precisa funcionar com uma mão, oferecer alvos de toque grandes e preservar o trabalho a cada etapa.

| Perfil | Necessidade dominante | Como o produto ajuda |
|---|---|---|
| Técnico autônomo | Executar rápido e provar o serviço | Roteiro guiado, evidências e laudo com marca própria |
| Instalador de redes | Padronizar visitas e comparar instalações | Checklist, medições por ambiente e antes × depois |
| Consultor de Wi-Fi | Analisar cenários complexos | Registro detalhado, recomendações e histórico |
| Pequeno provedor | Escalar qualidade da equipe | Processo consistente, rastreabilidade e documentos |

---

## Jornada

### 3. Jornada ponta a ponta

| Etapa | Objetivo do profissional | Saída obrigatória |
|---|---|---|
| Preparar | Identificar cliente, local, tipo e objetivo da visita | Visita criada com escopo mínimo |
| Diagnosticar | Medir o cenário atual de forma organizada | Baseline por ambiente |
| Intervir | Registrar o que foi alterado ou recomendado | Ações e evidências associadas |
| Comprovar | Repetir medições e demonstrar impacto | Comparação antes × depois |
| Entregar | Revisar, gerar e compartilhar o resultado | Laudo final e visita concluída |

### 4. Preparar a visita (Etapa 1)

A jornada pode começar pela tela Início, pelo cadastro de um Cliente ou por uma ação rápida "Nova visita". O aplicativo deve lembrar o último cliente e locais recentes, mas nunca criar uma visita sem associação explícita.

| Passo | Tela | Ação principal | Regra |
|---|---|---|---|
| 1 | Início | Tocar em Nova visita | Ação dominante e sempre visível |
| 2 | Selecionar cliente | Buscar ou cadastrar | Permitir cliente provisório com dados mínimos |
| 3 | Selecionar local | Escolher ou adicionar endereço | Um cliente pode ter vários locais |
| 4 | Definir visita | Selecionar tipo, objetivo e observações | Tipo determina checklist sugerido |
| 5 | Preparação | Confirmar rede, permissões e equipamentos | Bloquear apenas o que inviabiliza a coleta |

**Dados mínimos:** nome do Cliente, identificação do Local, tipo da visita e objetivo. Telefone, e-mail, documento e endereço completo podem ser concluídos depois.

Evento canônico ao concluir esta etapa: `customer.created` (quando cliente novo), `appointment.created`, `visit.started`.

### 5. Diagnosticar o cenário atual (Etapa 2)

A medição inicial cria o baseline. Antes de iniciar, o profissional define ou confirma os ambientes do local. O aplicativo mostra progresso e deixa claro o que já foi coletado, o que está incompleto e o que foi ignorado conscientemente.

**Medições mínimas por ambiente**

- Força e qualidade do sinal Wi-Fi.
- Banda, SSID, frequência e identificação do ponto de acesso quando disponível.
- Velocidade, latência e estabilidade conforme o tipo de visita.
- Foto ou observação quando houver condição física relevante.
- Classificação simples: adequado, atenção ou crítico.

Cada leitura registra um `MeasurementPoint` dentro do `MeasurementSession` do ambiente; evento `environment.measured`.

### 6. Registrar a intervenção (Etapa 3)

O SignallQ Pro não deve presumir que toda visita envolve alteração. O profissional pode executar uma ação, apenas recomendar ou documentar que nenhuma mudança foi autorizada.

| Tipo de registro | Exemplos | Comportamento |
|---|---|---|
| Executado | Reposicionamento, troca de canal, ajuste de banda, instalação de mesh | Exigir descrição e permitir foto |
| Recomendado | Troca de equipamento, passagem de cabo, ponto adicional | Registrar prioridade, justificativa e custo opcional |
| Não autorizado | Cliente recusou mudança ou acesso | Registrar motivo sem bloquear conclusão |
| Sem intervenção | Visita apenas diagnóstica | Seguir diretamente para análise e laudo |

**Vínculo com evidências.** Cada ação deve poder ser associada a um ambiente e a uma ou mais evidências. Fotos, notas e resultados não ficam em uma galeria solta; ficam ligados ao que comprovam.

### 7. Comprovar o resultado (Etapa 4)

Quando houve intervenção, o aplicativo sugere repetir apenas as medições relevantes. O profissional pode repetir todo o roteiro ou selecionar ambientes afetados. Evento `comparison.viewed`.

| Situação | Decisão do app | Decisão do profissional |
|---|---|---|
| Houve melhoria clara | Destacar ganho e impacto prático | Confirmar conclusão ou repetir |
| Resultado equivalente | Mostrar variação dentro da tolerância | Aceitar, investigar ou registrar limitação |
| Resultado pior | Alertar sem esconder o dado | Reverter, ajustar ou justificar |
| Sem medição final | Marcar comparação incompleta | Concluir como diagnóstico, não como melhoria comprovada |

**Sem maquiagem de resultado.** O laudo deve exibir resultados desfavoráveis. Esconder medição ruim destrói a credibilidade profissional do produto.

### 8. Revisar e entregar o laudo (Etapa 5)

Antes de gerar o documento, uma tela de revisão resume pendências e permite corrigir dados sem obrigar o profissional a navegar novamente por toda a jornada.

- Identificação do profissional, cliente e local.
- Objetivo, escopo e data da visita.
- Resumo executivo em linguagem simples.
- Resultados por ambiente e evidências selecionadas.
- Comparação antes × depois quando disponível.
- Ações executadas, recomendações e limitações.
- Assinatura ou aceite opcional e canais de compartilhamento.

Eventos `report.generated` e `report.shared`.

**Estados finais da visita**

| Estado | Quando usar | Efeito |
|---|---|---|
| Rascunho | Há campos ou medições pendentes | Pode ser retomado e não gera laudo final |
| Aguardando validação | Execução concluída, falta revisão ou aceite | Mantém alerta na Início |
| Concluída | Laudo revisado e gerado | Bloqueia alterações silenciosas; revisão cria nova versão |
| Cancelada | Visita não ocorreu ou foi interrompida | Preserva motivo e auditoria |

---

## Navegação

### 9. Fluxo macro de telas

A navegação principal organiza o histórico e a operação, enquanto a visita utiliza uma progressão guiada. O profissional pode sair e retomar sem perder o estado.

| Destino | Conteúdo | Ação de destaque |
|---|---|---|
| Início | Agenda, visitas em andamento, pendências e recentes | Nova visita |
| Clientes | Busca, clientes recentes e locais | Novo cliente |
| Histórico | Visitas, laudos e filtros | Abrir ou compartilhar |
| Perfil | Marca profissional, assinatura, preferências e suporte | Editar perfil profissional |

### 10. Catálogo de telas do MVP

| Módulo | Tela | Responsabilidade |
|---|---|---|
| Acesso | Boas-vindas | Apresentar valor e entrar/criar conta |
| Acesso | Cadastro profissional | Dados, logo e identificação |
| Início | Dashboard | Retomar visitas e iniciar nova |
| Clientes | Lista de clientes | Buscar, filtrar e cadastrar |
| Clientes | Detalhe do cliente | Ver locais, contatos e histórico |
| Locais | Detalhe do local | Ambientes, equipamentos e visitas |
| Visita | Nova visita | Tipo, objetivo e escopo |
| Visita | Preparação | Checklist, permissões e rede |
| Visita | Ambientes | Criar e ordenar roteiro |
| Medição | Coleta por ambiente | Executar testes e registrar evidências |
| Medição | Resultado do ambiente | Interpretar e recomendar |
| Intervenção | Ações realizadas | Registrar alteração ou recomendação |
| Comparação | Antes × depois | Comprovar impacto |
| Laudo | Revisão | Corrigir dados e pendências |
| Laudo | Pré-visualização | Visualizar documento final |
| Laudo | Compartilhamento | PDF, link ou envio |
| Histórico | Lista de visitas | Filtrar e reabrir registros |
| Perfil | Identidade profissional | Logo, assinatura e contatos |
| Configurações | Preferências | Unidades, privacidade e sincronização |

**IDs de tela (protótipo P01..P15)**

| ID | Tela | Objetivo |
|---|---|---|
| P01 | Onboarding profissional | Apresentação, cadastro e permissão de uso |
| P02 | Início | Agenda operacional e atalhos |
| P03 | Clientes | Busca, filtros e cadastro |
| P04 | Detalhe do cliente | Locais, contatos e histórico |
| P05 | Detalhe do local | Rede, equipamentos e visitas |
| P06 | Criar visita | Objetivo, escopo e dados iniciais |
| P07 | Ambientes | Lista e progresso por cômodo |
| P08 | Medição do ambiente | Sinal, velocidade, estabilidade e observações |
| P09 | Evidências | Fotos, legendas e anexos |
| P10 | Comparação | Antes × depois por ambiente e indicador |
| P11 | Resumo técnico | Achados, causas e recomendações |
| P12 | Editor de laudo | Identidade, seções e prévia |
| P13 | Laudo concluído | PDF, compartilhamento e aceite |
| P14 | Histórico | Visitas e relatórios anteriores |
| P15 | Perfil profissional | Logo, assinatura, contato e plano |

### 11. Regras de navegação por tela

**Início**

- Mostrar primeiro visitas em andamento e pendências, não métricas de vaidade.
- Manter "Nova visita" como ação principal.
- Exibir atalhos para clientes recentes e último laudo.

**Clientes e locais**

- Busca tolerante a nome, telefone, endereço e apelido do local.
- Cadastro rápido sem exigir todos os campos.
- Excluir somente sem histórico; caso contrário, arquivar.

**Visita em andamento**

- Topbar com nome do cliente/local e estado de salvamento.
- Indicador de progresso por etapa, não uma barra genérica sem significado.
- Ação "Salvar e sair" sempre disponível.
- Voltar nunca descarta dados automaticamente.

**Medição**

- Uma ação técnica dominante por vez.
- Explicar previamente o que será medido e quanto exige do ambiente.
- Resultados essenciais primeiro; detalhes técnicos sob expansão.
- Permitir repetir sem duplicar evidência por engano.

**Laudo**

- Pendências agrupadas no topo.
- Itens opcionais claramente indicados.
- Pré-visualização fiel ao PDF.
- Versão do laudo e data de geração visíveis.

---

## Exceções

### 12. Fluxos alternativos e recuperação

| Condição | Resposta esperada |
|---|---|
| Sem internet externa | Continuar coleta local, salvar offline e sinalizar sincronização pendente. |
| Wi-Fi desconectado | Pausar teste dependente da rede e orientar reconexão sem apagar contexto. |
| Permissão negada | Explicar impacto e oferecer abrir configurações ou seguir com escopo reduzido. |
| App fechado durante a visita | Restaurar exatamente a etapa, ambiente e dados salvos. |
| Cliente/local duplicado | Sugerir associação ao cadastro existente antes de criar outro. |
| Medição inválida | Explicar motivo, não salvar como resultado válido e permitir repetir. |
| Foto não disponível | Permitir nota textual; evidência visual não pode ser obrigatória em todos os casos. |
| Laudo já concluído | Criar revisão versionada, preservando o documento anterior. |

---

## Estados

### 13. Estados transversais da interface

| Estado | Aplicação |
|---|---|
| Carregando | Skeleton curto; não usar spinner infinito sem contexto. |
| Vazio | Explicar por que está vazio e oferecer uma ação útil. |
| Erro recuperável | Manter dados preenchidos e oferecer tentar novamente. |
| Offline | Banner discreto e persistente; indicar o que continua funcionando. |
| Salvando | Confirmação não bloqueante e estado visível. |
| Sincronização pendente | Contador e opção de tentar novamente. |
| Concluído | Resumo, próxima ação e acesso ao histórico. |

---

## Critérios

### 14. Critérios de aceite da jornada

- Uma visita nova pode ser criada em poucos passos, sem cadastro burocrático.
- Nenhuma medição válida fica sem cliente, local, visita e ambiente.
- O profissional consegue pausar e retomar a visita sem perda de dados.
- O aplicativo diferencia claramente medição inicial e final.
- A comparação nunca oculta piora ou ausência de evidência.
- O laudo pode ser revisado antes da geração e preserva versões.
- Todos os fluxos críticos possuem estados vazio, erro e offline definidos.
- A navegação funciona com acessibilidade, alvos de toque mínimos e leitura por tecnologia assistiva.

---

## Resumo

### 15. Mapa de responsabilidades

Este fluxo deve orientar protótipos, implementação, testes funcionais e caderno de testes. Mudanças futuras precisam preservar a rastreabilidade entre visita, ambiente, medição, intervenção, comparação e laudo.

| Momento | Tela responsável | Dado produzido |
|---|---|---|
| Preparação | Nova visita / Preparação | Escopo, checklist e condições iniciais |
| Coleta | Ambientes / Medição | Resultados, fotos e observações |
| Decisão | Resultado / Intervenção | Diagnóstico, ação e recomendação |
| Comprovação | Antes × depois | Variação e impacto |
| Entrega | Revisão / Laudo | Documento final e aceite |
| Continuidade | Histórico | Registro reutilizável e rastreável |

> **Fora do MVP.** Agenda avançada, cobrança dinâmica dentro do app, equipes com múltiplos técnicos, CRM completo, assinatura jurídica avançada e portal do cliente entram apenas depois que a jornada básica provar valor.

---

## Jornada comercial

### 16. Da captação ao recibo

| Etapa | Resultado esperado |
|---|---|
| 1. Conta | Google ou e-mail e senha; verificação e sessão persistente. |
| 2. Plano | Entrada no Free; oferta mensal/anual do Pro contextualizada. |
| 3. Perfil | Identidade profissional, logo, contato e Pix. |
| 4. Solicitação | Atendimento recebido por WhatsApp ou cadastro manual. |
| 5. Agenda | Horário confirmado e evento adicionado ao calendário. |
| 6. Visita | Cliente, local, ambientes, medições e evidências. |
| 7. Resultado | Diagnóstico, intervenção e comparação antes/depois. |
| 8. Entrega | Laudo compartilhado com o cliente. |
| 9. Cobrança | QR Code Pix com valor e código copia e cola. |
| 10. Fechamento | Confirmação manual, recibo e histórico financeiro. |

### 17. Fluxo de autenticação e assinatura

Primeiro acesso → escolher Google ou e-mail → validar identidade → criar perfil → entrar no Free → conhecer limites → assinar Pro mensal ou anual quando houver valor percebido.

- Sem conexão, permitir acesso apenas se houver sessão local válida dentro da janela offline.
- Quando a assinatura expirar, preservar os dados e limitar novas operações; não apagar histórico.
- Upgrade deve retornar o usuário à tarefa que motivou a assinatura.

Eventos: `auth.started`, `auth.succeeded`, `profile.completed`, `paywall.viewed`, `trial.started`, `subscription.activated`.

### 18. Fluxo de agenda

| Origem | Fluxo |
|---|---|
| Cadastro manual | Novo atendimento → cliente/local → data e horário → salvar → adicionar ao calendário |
| WhatsApp no MVP | Conversa externa → técnico abre SignallQ Pro → cria solicitação com dados mínimos → confirma horário |
| Link público futuro | Cliente envia solicitação → técnico aceita ou propõe novo horário → visita é criada |
| Google Agenda no MVP2 | Evento e visita permanecem vinculados; reagendamento e cancelamento são sincronizados |

**Estados do atendimento e cor do chip** (usar os presets `StatusChip`/`ATENDIMENTO_STATUS` do projeto `77a19317`; Canônico §5.2):

| Estado | Uso visual | Ação |
|---|---|---|
| Solicitado | Chip neutro | Confirmar ou propor horário |
| Confirmado | Chip azul-petróleo (Primary) | Abrir visita / adicionar ao calendário |
| A caminho | Chip de deslocamento (preset `ATENDIMENTO_STATUS`, tom de apoio) | Iniciar deslocamento |
| Em atendimento | Chip laranja (Attention) | Continuar visita |
| Concluído | Chip verde (Positive) | Gerar laudo e cobrar |
| Cancelado / não compareceu | Chip vermelho (Critical) | Registrar motivo |

### 19. Fluxo de cobrança e recibo

Finalizar visita → informar valor → gerar Pix com ou sem valor → cliente paga → técnico verifica no banco → marcar como pago → confirmar declaração → emitir recibo → compartilhar laudo e recibo.

- Se parcial, emitir recibo do valor recebido e manter saldo pendente.
- Se não pago, manter cobrança pendente sem bloquear a conclusão técnica da visita.
- Se recibo estiver errado, cancelar com motivo e gerar substituto; nunca editar silenciosamente.

Eventos: `pix_charge.created`, `payment.confirmed`.

### 20. Novas telas e rotas

| Rota | Tela |
|---|---|
| `auth/welcome` | Boas-vindas e métodos de acesso |
| `auth/email` | Login/criação de conta local |
| `auth/verify` | Verificação de e-mail |
| `subscription/paywall` | Planos mensal e anual |
| `profile/professional` | Perfil profissional |
| `profile/pix` | Configuração Pix |
| `appointments/list` | Atendimentos |
| `appointments/edit` | Novo/editar atendimento |
| `payments/charge` | Cobrança Pix |
| `payments/confirm` | Confirmar recebimento |
| `receipts/detail` | Recibo digital |

**Princípio central.** O app não deve parecer uma coleção de testes. Cada tela precisa ajudar o profissional a preparar, executar, comprovar ou entregar um serviço.

---

## Documentos relacionados

- `00_CANONICO_v5.md` — dicionário canônico de nomes, eventos, tokens e decisões (prevalece sobre este).
- `08_SignallQ_Pro_Especificacao_Funcional_v5.md` — visão, entidades, módulos e regras de negócio.
- `10_SignallQ_Pro_Design_System_v5.md` — tokens, componentes e apresentação de medições.
- `11_SignallQ_Pro_Roadmap_MVP1_MVP2_v5.md` — fases, gates e sequência de implementação.
