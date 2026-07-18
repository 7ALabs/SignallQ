# SignallQ Pro — Design System

**Status:** ativo · **Versão:** 5.0 · **Data:** 17/07/2026 · **Substitui:** Design System v3

> **Atualização 2026-07-18 — virada de identidade.** O projeto Claude Design `77a19317` (fonte da verdade visual do Pro) passou de **teal-dominante para azul-dominante**: Primary marca `#0B6CFF`, Secondary ciano `#006B76`, Tertiary roxo `#6558E8`, escala de sinal de 6 níveis, dois temas oficiais (claro/escuro) e tokens de gráficos. O corpo abaixo já reflete essa paleta (seções 2, 3, 3.1 e 8 reescritas nesta data). O projeto online **evolui** — antes de qualquer entrega visual, reler `foundations/tokens.html`, `status-and-charts.html` e `dark-mode.html` para confirmar que este doc segue atual.

## Estado atual vs. Alvo

Este design system descreve o **SignallQ Pro** (`io.signallq.pro`), que é **🎯 ALVO** — ainda não existe (Canônico §1). Serve de fonte para protótipo e implementação do produto desejado.

O **✅ ATUAL** é o SignallQ consumidor, cujos tokens de marca (primary `#5B21D6`, secondary `#2851B8`) estão no código (`SignallQTheme.kt`, skill `SignallQ-design`). Consumer e Pro **compartilham fundações, tipografia e componentes de medição**, mas a matiz de marca diverge de propósito: o Pro tem identidade **azul** própria (`#0B6CFF` — atualizada de teal para azul em 2026-07-18; ver nota acima e Canônico §5.2). Os tokens `#6C2BFF` e a paleta teal-dominante anterior (`#006B73` como primary) estão **mortos**.

---

## Identidade

### 1. Fundamentos da marca

O SignallQ Pro transforma medições técnicas em um serviço profissional vendável. A experiência deve transmitir competência, clareza e confiança sem cair na estética de ferramenta corporativa velha e carregada.

**Relação com o SignallQ gratuito.** O SignallQ gratuito explica a conexão para o consumidor. O SignallQ Pro registra, compara e comprova o serviço realizado pelo profissional.

**Diretriz central.** O Pro não é uma nova identidade desconectada. É a extensão profissional da marca SignallQ, com maior densidade de informação, rastreabilidade e foco em evidências.

- **Clareza antes de densidade** — mostrar primeiro o que exige decisão; métricas avançadas ficam por expansão ou detalhe.
- **Evidência antes de opinião** — toda conclusão importante aponta a medição, foto, observação ou comparação que a sustenta.
- **Fluxo antes de dashboard** — o produto é guiado por trabalho: cliente → visita → ambiente → medição → laudo.
- **Profissional sem burocracia** — reduzir registro manual, não criar formulário infinito para justificar cada clique.

**Posicionamento.** Diagnóstico profissional de Wi-Fi e conectividade, do levantamento ao laudo, em uma única visita.

### 2. Logo e arquitetura de marca

O símbolo e o lettering SignallQ permanecem inalterados. "PRO" funciona como qualificador de produto e nunca deve competir com a marca principal.

**Regras de aplicação**

- Usar o selo PRO em caixa alta, com cantos arredondados, na cor de marca do Pro (azul `#0B6CFF`), com o roxo de apoio `#6558E8` reservado a acento pontual — nunca como cor dominante do selo.
- Manter área de proteção mínima equivalente à altura da letra "Q" ao redor do conjunto.
- Em ícones pequenos, não usar a palavra PRO; diferenciar por fundo escuro e detalhe de canto.
- Não redesenhar o símbolo, não adicionar ferramentas, antenas, maletas ou escudos ao ícone.
- Não usar "SignallQPRO" como uma palavra única.

---

## Tokens

### 3. Cores e semântica

Direção cromática do Pro (fonte: projeto Claude Design `77a19317`, `foundations/tokens.html`/`status-and-charts.html`, snapshot 2026-07-18): **azul `#0B6CFF` como cor de marca**, ciano técnico `#006B76` e roxo `#6558E8` como cores de apoio, sobre Material 3 com **dois temas oficiais** (claro e escuro, mesma estrutura e componentes — troca-se token, nunca layout). O azul domina CTAs e ações primárias; ciano e roxo são apoio (chips, gráficos, destaques secundários) e nunca competem com o azul. Cor nunca deve ser o único meio de comunicar estado — sempre ícone + rótulo + valor.

**Color roles — tema claro**

| Token | Valor | Uso principal |
|---|---|---|
| Primary — marca | `#0B6CFF` | CTAs, ações primárias, identidade Pro |
| Primary Container | `#D8E7FF` | Seleção, destaque suave |
| Secondary — ciano técnico | `#006B76` | Apoio: chips, destaques secundários |
| Secondary Container | `#A9EDF3` | Superfícies informativas |
| Tertiary — roxo de apoio | `#6558E8` | Apoio: gráficos e realces pontuais |
| Tertiary Container | `#E5DEFF` | — |
| Success / Good | `#1AA25A` | Medição aprovada / melhora |
| Warning / Attention | `#E9AD27` | Risco ou recomendação |
| Error / Critical | `#D9363E` | Falha ou bloqueio |
| Error Container | `#FFDAD6` | — |
| Background | `#F7F9FC` | Fundo de tela |
| Surface | `#FFFFFF` | Cards e planos de conteúdo |
| Surface Container High | `#E7ECF3` | Superfícies elevadas |
| Outline | `#C4CBD5` | Contornos |
| Divider | `#E3E7EC` | Divisores (`--sqp-color-divider`) |
| Inverse Surface | `#252B33` | Superfícies invertidas |

**Escala de sinal — 6 níveis** (`--sqp-status-*`, usada em medições de qualidade/cobertura):

| Nível | Claro | Escuro |
|---|---|---|
| Excelente | `#16A85A` | `#32D978` |
| Bom | `#1AA25A` | `#32D978` |
| Atenção | `#E9AD27` | `#F5C451` |
| Fraco | `#ED7D2D` | `#FF964F` |
| Crítico | `#D9363E` | `#FF5F66` |
| Informação (neutro) | `#0B6CFF` | `#3D96FF` |

**Identidade de gráficos** (`--sqp-chart-*`): séries de download, upload, latência, jitter e perda, mais `grid` (grade discreta) e `reference` (linha tracejada). Linhas limpas, preenchimento suave; fundo branco no tema claro, surface no escuro, sem glow.

**Tema escuro.** Estrutura, componentes e hierarquia são **idênticos** entre os dois temas — nenhum componente é exclusivo de um tema, tudo consome tokens semânticos via `data-theme`. Exceção: o **laudo técnico é sempre claro**, independentemente do tema do app.

**Contraste.** Textos corridos devem manter contraste mínimo de 4,5:1 em ambos os temas. Estados positivos, de atenção e críticos sempre incluem ícone e rótulo textual, nunca só cor.

**Tokens mortos — nunca usar:** `#6C2BFF` (primary antigo do consumer) e a paleta teal-dominante anterior do próprio Pro (`#006B73` como primary — hoje rebaixado a Secondary `#006B76`; o antigo "elo violeta `#5B21D6`" foi substituído pelo roxo próprio do Pro `#6558E8`).

### 3.1 Relação com a paleta do SignallQ consumer

O SignallQ consumidor (✅ ATUAL, do código) usa **primary `#5B21D6`** (violeta) e **secondary `#2851B8`** (azul fixo, não derivado do primary), em Material 3 estrito, Google Sans Flex e grid 8dp. O SignallQ Pro compartilha essas **fundações, tipografia e componentes de medição**, mas tem **marca própria**: azul `#0B6CFF` como identidade, com ciano `#006B76` e roxo `#6558E8` de apoio. Os dois produtos não compartilham matiz — é intencional, para separar visualmente o produto profissional do consumidor (Canônico §5.1/§5.2).

Diferente da versão anterior deste doc (v3/primeira v5), o Pro **não** usa mais o violeta do consumer como elo de marca — a identidade azul é autônoma. O token `#6C2BFF` está morto em toda a plataforma e não pode ser citado como vivo em nenhum documento.

---

## Fundação visual

### 4. Tipografia, grid e espaçamento

**Tipografia.** Usar a família tipográfica já adotada no SignallQ (Google Sans Flex, fallback Roboto). Na implementação Android, preferir a fonte do sistema ou a fonte oficial empacotada no projeto, sem misturar famílias por tela.

| Estilo | Tamanho | Peso | Aplicação |
|---|---|---|---|
| Display | 32 sp | Semibold | Conclusões e estados de sucesso |
| Título | 24 sp | Semibold | Nome da tela |
| Seção | 18 sp | Semibold | Blocos e grupos |
| Corpo | 16 sp | Regular | Conteúdo principal |
| Apoio | 14 sp | Regular | Metadados e ajuda |
| Label | 12 sp | Medium | Chips e indicadores |

**Grid e espaçamento**

- Base de espaçamento: 4 dp.
- Margem horizontal mobile: 16 dp; tablet: 24 dp.
- Distância padrão entre blocos: 24 dp.
- Cards: raio de 16 dp; controles: raio de 12 dp; chips: raio total.
- Alvos de toque mínimos: 48 × 48 dp.

---

## Produto

### 5. Arquitetura de informação

A navegação precisa refletir o trabalho real do profissional. O objeto principal não é "o teste"; é a **visita técnica**, que reúne contexto, ambientes, medições, evidências e resultado.

Cadeia conceitual: **01 Clientes** (quem recebe o serviço) → **02 Locais** (onde ocorre) → **03 Visitas** (quando e por quê) → **04 Ambientes** (onde foi medido) → **05 Laudo** (o que foi comprovado).

**Navegação principal**

| Destino | Conteúdo |
|---|---|
| Início | Visitas em andamento, ações rápidas, pendências e histórico recente. |
| Clientes | Cadastro, locais, contatos e histórico consolidado. |
| Nova visita | Fluxo guiado para levantamento, intervenção ou validação. |
| Relatórios | Laudos gerados, rascunhos e compartilhamentos. |
| Perfil profissional | Marca, assinatura, dados comerciais, plano e preferências. |

---

## Jornada

### 6. Fluxo principal da visita

Sequência guiada: **1 Preparar** (cliente, local e objetivo) → **2 Mapear** (ambientes e rede atual) → **3 Medir** (sinal, velocidade e estabilidade) → **4 Intervir** (mudanças e recomendações) → **5 Validar** (antes × depois) → **6 Entregar** (laudo e aceite).

**Tipos de visita**

- Diagnóstico inicial — identificar causa de lentidão, cobertura ruim, interferência ou instabilidade.
- Instalação / otimização — posicionar roteador ou mesh, ajustar canais, bandas, SSIDs e configurações.
- Validação pós-serviço — comprovar resultado após correções ou instalação.
- Vistoria técnica — registrar estado da rede e emitir parecer sem realizar intervenção.

**Status da visita**

- Rascunho — ainda sem execução.
- Em andamento — possui coleta ativa ou dados não finalizados.
- Aguardando validação — serviço executado, falta medição final ou aceite.
- Concluída — laudo final gerado.
- Cancelada — preserva motivo e histórico, mas não gera laudo final.

> **Regra.** Uma visita em andamento deve permanecer recuperável. Fechar o app ou perder conexão não pode apagar medições, fotos ou observações.

---

## Escopo

### 7. Inventário de telas do MVP

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

**Telas de referência no projeto online** (`foundations/screen-*.html`, claro vs. escuro lado a lado): Home, Walk Test, Medição por ambiente, Teste de velocidade, Atendimento, Histórico, Configurações. Laudo técnico é exemplificado sempre no tema claro.

---

## UI kit

### 8. Componentes essenciais

**Botões**

- Primário: uma ação dominante por tela.
- Secundário: ações alternativas sem competir com a principal.
- Texto: navegação leve, editar, ver detalhes.
- Destrutivo: exige rótulo explícito; nunca usar apenas ícone de lixeira.
- Ação persistente no rodapé apenas quando necessária para avançar no fluxo.

**Componentes de conteúdo**

| Componente | Descrição |
|---|---|
| Card de visita | Cliente, local, objetivo, status, horário e ação de continuar. |
| Card de ambiente | Nome, tipo, progresso, resumo das métricas e estado. |
| Medidor de qualidade | Valor, faixa, interpretação e contexto da medição. |
| Comparador antes/depois | Dois valores, variação absoluta, percentual e conclusão. |
| Chip de evidência | Foto, observação, equipamento ou configuração vinculada. |
| Banner de sincronização | Estado offline, itens pendentes e última sincronização. |
| Bloco de recomendação | Problema, impacto, ação sugerida e prioridade. |
| Assinatura/aceite | Nome, data, confirmação e observação opcional. |

> **Proibido.** Card inteiro clicável com vários botões escondidos, ícones sem rótulo para ações críticas e três CTAs primários brigando na mesma tela.

**Chips de status do atendimento** — usar sempre os presets do componente `StatusChip` (`ATENDIMENTO_STATUS`, bundle `window.SignallQPRODesignSystem_77a193` no projeto `77a19317`), nunca hex avulso:

| Estado | Uso visual | Ação |
|---|---|---|
| Solicitado | Chip neutro (Outline) | Confirmar ou propor horário |
| Confirmado | Chip de marca (Primary, azul) | Abrir visita / adicionar ao calendário |
| A caminho | Chip de apoio (Secondary ou Tertiary, discreto) | Iniciar deslocamento |
| Em atendimento | Chip de atenção (Warning) | Continuar visita |
| Concluído | Chip de sucesso (Success) | Gerar laudo e cobrar |
| Cancelado / não compareceu | Chip crítico (Error) | Registrar motivo |

---

## Dados

### 9. Apresentação das medições

O Pro pode mostrar mais informação que o app gratuito, mas isso não significa despejar telemetria crua. Cada métrica precisa responder: qual o valor, o que significa e o que fazer.

| Métrica | Unidade | Exibição mínima |
|---|---|---|
| Sinal Wi-Fi | dBm | Valor atual + faixa + banda + SSID/BSSID |
| Velocidade | Mbps | Download, upload e referência do plano |
| Latência | ms | Média, variação e destino do teste |
| Jitter | ms | Estabilidade para voz, vídeo e jogos |
| Perda | % | Impacto e duração da amostra |
| Canal | número / MHz | Ocupação, largura e interferência |
| Dispositivo | modelo / IP | Origem do dado e nível de confiança |

> **Importante.** Os limites devem considerar contexto, tipo de uso e banda. Um único corte universal produz diagnóstico burro.

**Estados semânticos**

- Bom — atende ao objetivo declarado da visita.
- Atenção — funciona, mas apresenta risco ou margem pequena.
- Crítico — compromete o uso ou viola o critério definido.
- Não avaliado — não houve dados suficientes; não fingir certeza.

---

## Confiabilidade

### 10. Evidências e rastreabilidade

Toda medição deve registrar contexto suficiente para ser auditável e comparável. A interface precisa fazer isso automaticamente sempre que possível.

| Elemento | Conteúdo |
|---|---|
| Contexto automático | Data, hora, local, ambiente, rede, banda, dispositivo e versão do app. |
| Foto | Imagem original, miniatura, legenda e vínculo com ambiente ou recomendação. |
| Observação | Texto curto, ditado opcional e categoria. |
| Alteração técnica | O que mudou, valor anterior, valor novo e responsável. |
| Confiança | Origem do dado e limitações da medição. |
| Sincronização | Identificador local, estado e registro do envio. |

**Modo offline**

- Salvar localmente imediatamente após cada coleta.
- Exibir claramente o que ainda não foi sincronizado.
- Não bloquear conclusão da visita por falta temporária de internet.
- Gerar PDF local quando tecnicamente possível; sincronizar depois.

---

## Entrega

### 11. Laudo profissional

O laudo é o produto final do trabalho. Ele deve ser compreensível para o cliente e tecnicamente defensável para o profissional.

**Estrutura mínima**

- Capa com marca SignallQ Pro e identidade do profissional.
- Cliente, endereço/local e dados da visita.
- Objetivo e escopo do serviço.
- Resumo executivo em linguagem simples.
- Ambientes avaliados e resultados principais.
- Problemas encontrados e evidências.
- Ações realizadas.
- Comparação antes/depois.
- Recomendações pendentes e prioridades.
- Limitações da avaliação.
- Assinatura ou aceite do cliente.

**Personalização.** Permitir logo, nome comercial, documento profissional ou empresarial, telefone, e-mail e assinatura. A personalização não pode apagar a indicação "Gerado com SignallQ Pro".

> **Regra editorial.** O resumo executivo fala com o cliente. O anexo técnico preserva as métricas completas. Misturar os dois deixa o laudo incompreensível para um e raso para o outro.

---

## Conteúdo

### 12. Linguagem e microcopy

A interface deve ser direta, profissional e explicativa. Evitar termos técnicos quando não agregam decisão; quando forem necessários, explicar no próprio contexto. Copy em PT-BR com "você", sentence case em títulos, sem emoji.

**Tom**

- Não culpar o usuário.
- Não prometer causa exata quando os dados só indicam hipótese.
- Distinguir claramente fato medido, inferência e recomendação.
- Usar verbos de ação em botões: Medir, Salvar ambiente, Gerar laudo.

| Preferir | Evitar |
|---|---|
| "O sinal neste cômodo está fraco e pode causar travamentos em vídeo." | "RSSI abaixo do threshold operacional detectado." |
| "Medição salva neste aparelho. Será sincronizada quando houver internet." | "Falha 503 no endpoint de persistência." |
| "Refaça o teste próximo ao ponto onde o cliente usa a rede." | "Amostra inválida. Tente novamente." |

---

## Padrões

### 13. Acessibilidade e qualidade

- Compatibilidade com leitor de tela em todos os controles e gráficos.
- Ordem de foco coerente com a leitura visual.
- Escala de fonte sem corte ou sobreposição até 200%.
- Não depender apenas de cor para estados e comparação.
- Textos alternativos para fotos relevantes no laudo.
- Feedback háptico opcional em conclusão de medição, nunca como único retorno.
- Respeitar barras e áreas seguras do Android; conteúdo não pode ficar sob navegação nativa.
- Modo escuro com contraste validado, não mera inversão de cores.

**Checklist de aceite visual**

- Tela implementada comparada com a especificação em dispositivo-alvo.
- Estados vazio, carregando, erro, offline e conteúdo longo testados.
- Botões e campos funcionais, não apenas decorativos.
- Navegação de retorno e persistência da visita validadas.
- Capturas anexadas à evidência de teste.

---

## Manutenção

### 14. Governança e handoff

O design system deve ser uma fonte viva e versionada junto da plataforma. Componentes duplicados e decisões escondidas em telas isoladas viram dívida rapidamente.

**Fonte da verdade**

- Tokens visuais compartilhados entre SignallQ e SignallQ Pro (fundações comuns; matiz de marca divergente).
- Componentes comuns em módulo reutilizável; variações Pro configuráveis por propriedades.
- Especificações funcionais vinculadas às telas e componentes.
- Mudanças relevantes registradas em changelog curto.
- Componentes depreciados marcados antes da remoção.

**Critério para criar componente novo**

- O padrão aparece ou aparecerá em pelo menos três contextos.
- A variação não pode ser resolvida com propriedades de um componente existente.
- A função e os estados estão definidos, não apenas a aparência.
- Há responsabilidade clara de manutenção e teste.

**Decisão arquitetural.** SignallQ e SignallQ Pro devem compartilhar fundações, tokens e componentes de medição. Fluxos, entidades e navegação podem divergir. Copiar tudo e manter duas versões seria pedir para a inconsistência começar.

---

## Evolução do produto

### 15. Conta, acesso e primeiro uso

O SignallQ Pro exige identidade persistente para sincronizar clientes, visitas, assinatura, recibos e configurações profissionais. O acesso deve ser simples, mas não pode depender exclusivamente de uma conta externa.

- Usar Google Credential Manager no Android para login Google.
- Permitir vincular Google e senha local à mesma conta (Identidade / `IdentityProvider`).
- Nunca exibir diferença visual que faça a conta local parecer inferior ou insegura.

| Tela | Conteúdo principal | Ação primária |
|---|---|---|
| Boas-vindas | Benefício do produto, privacidade e opções de acesso | Continuar com Google / Entrar com e-mail |
| Criar conta | Nome, e-mail, senha e aceite dos termos | Criar conta |
| Verificar e-mail | Código ou link de confirmação | Confirmar e-mail |
| Perfil profissional | Nome público, documento opcional, contato, logo e cidade | Concluir perfil |
| Recuperar acesso | E-mail e redefinição segura de senha | Enviar instruções |

### 16. Planos, limites e assinatura

O produto possui dois níveis de acesso: Free e Pro. A interface deve explicar o valor do upgrade no contexto da tarefa, sem bloquear o fluxo com pop-ups agressivos.

- Exibir mensal e anual como duas opções do mesmo plano Pro.
- Mostrar preço anual equivalente por mês, desconto total e cobrança anual claramente (**valores pendentes — preço do Pro não definido, Canônico §8.1**).
- Usar estados ACTIVE, GRACE_PERIOD, PAUSED e EXPIRED; evitar um simples `isPro`.

| Capacidade | Free | Pro |
|---|---|---|
| Clientes ativos | Até 3 | Ilimitados |
| Visitas mensais | Até 3 | Ilimitadas ou política de uso justo |
| Laudo | Modelo padrão com marca SignallQ Pro | Completo e personalizado |
| Fotos e evidências | Limitadas | Completas |
| Histórico | Janela reduzida | Completo |
| Agenda integrada | Adicionar ao calendário | Sincronização e gestão avançada |
| Recibos e Pix | Incluídos | Incluídos com histórico financeiro completo |
| Nuvem e backup | Básico | Completo |

### 17. Agenda e origem via WhatsApp

O SignallQ Pro controla o ciclo do atendimento, mas não recria um calendário completo no MVP. A agenda externa organiza horários; o aplicativo transforma o compromisso em cliente, local, visita, laudo, pagamento e histórico.

- MVP1: criar evento por intent no calendário escolhido pelo técnico.
- MVP2: conectar Google Agenda e manter vínculo por `externalEventId`.
- Futuro: link público compartilhável no WhatsApp para solicitar atendimento.

### 18. Pagamentos, Pix e recibo digital

O Pix no MVP é estático e gerado localmente. Não existe confirmação bancária automática. O técnico declara manualmente o recebimento e o aplicativo emite o recibo com rastreabilidade.

- Recibos emitidos são imutáveis; correção ocorre por cancelamento e nova emissão.
- Permitir pagamento parcial e saldo pendente.
- Não enviar chave Pix, CPF/CNPJ ou dados financeiros para analytics ou logs.

| Tela | Elementos obrigatórios |
|---|---|
| Configurar Pix | Tipo de chave, chave, recebedor, cidade, descrição padrão e opção de ocultação |
| Cobrança | Valor, descrição, QR Code, código copia e cola, compartilhar e marcar como pago |
| Confirmar recebimento | Valor, método, data, integral/parcial e confirmação explícita |
| Recibo | Número, pagador, profissional, serviço, valor por extenso, método, visita, hash e status |

> **Regra de confiança.** O aplicativo deve escrever "pagamento informado pelo profissional" e nunca "pagamento confirmado pelo banco" sem integração financeira.

---

*SignallQ Pro — diagnóstico que vira serviço; medição que vira evidência.*

---

## Documentos relacionados

- `00_CANONICO_v5.md` — dicionário canônico de nomes, eventos, tokens e decisões (prevalece sobre este).
- `08_SignallQ_Pro_Especificacao_Funcional_v5.md` — visão, entidades, módulos e regras de negócio.
- `09_SignallQ_Pro_Jornada_e_Fluxo_de_Telas_v5.md` — jornada e catálogo de telas.
- `11_SignallQ_Pro_Roadmap_MVP1_MVP2_v5.md` — fases, gates e sequência de implementação.
