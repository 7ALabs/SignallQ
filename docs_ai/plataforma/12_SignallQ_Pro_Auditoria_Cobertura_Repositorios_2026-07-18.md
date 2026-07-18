# SignallQ Pro — Auditoria de Cobertura de Engenharia (repositórios reais)

**Status:** ativo · **Data:** 18/07/2026 · **Tipo:** auditoria pontual (snapshot), não faz parte do pacote reconciliado v5 (docs 00–11)

## Origem e propósito

O Luiz fez uma pesquisa de mercado (via ChatGPT) sobre o SignallQ Pro, que identificou 16 gaps funcionais organizados em P0 (bloqueadores antes do MVP), P1 (logo após validar o fluxo) e P2 (não deve atrasar o MVP). Este documento cruza esses 16 gaps contra o **código real** de todos os repositórios do GitHub do Luiz — não contra documentação — para responder: o que já existe e pode ser reaproveitado, o que existe parcialmente e precisa evoluir, e o que precisa ser construído do zero.

**Método:** listagem completa dos repositórios via `gh repo list` (13 repositórios reais: 10 em `gmmattey`, 3 em `7AgentsStudio` — não assumido de memória). 5 pertencem ao domínio de diagnóstico de rede/Wi-Fi; os outros 8 são de outros domínios (finanças pessoais, orquestrador de IA, páginas institucionais) e foram descartados após confirmação. Cada um dos 5 repositórios relevantes foi auditado por um agente independente, lendo código real (`Read`/`Grep`, e `gh api`/clone raso para o repositório remoto não clonado localmente), citando arquivo e linha para cada veredito.

**Versão visual (artefato interativo, com evidência expansível por dimensão):** [status-cobertura-pro-v1](https://claude.ai/code/artifact/0dc72809-7de0-4050-b6af-105bf57475ed)

---

## Repositórios no domínio (5 de 13)

| Repositório | Papel | Estado | Achado principal |
|---|---|---|---|
| `gmmattey/linka-android` | SignallQ — app Android consumidor | Público, ativo, produção | Motor de causa raiz (`FindingEngine`) e de amostragem estatística mais maduros de todo o portfólio. Gap único: dado de rede vivo (BSSID/canal) não persiste junto da medição. |
| `gmmattey/nethal` | Laboratório de reconhecimento de equipamento de rede | Público, 6 dias de idade | Arquitetura (capability model, safety guard, consentimento por escopo) é referência sólida. Zero drivers em estágio estável; cobertura de só ~6 modelos de equipamento. |
| `gmmattey/linka-webapp` | PWA web legado do consumidor | Arquivado localmente, ainda público no GitHub | Engine de laudo PDF com paginação real (`anatelReport.ts`) e classificador puro reaproveitáveis. UI e histórico acoplados ao consumidor, sem noção de cliente/visita. |
| `gmmattey/linka-speedtest` | PWA standalone de velocidade | Público, ativo, Cloudflare Pages | Motor de medição web portável (TS puro) e wrapper Capacitor com plugins Android nativos já existentes. Dois classificadores concorrentes sem fonte única (dívida de higiene). |
| `7AgentsStudio/signallq-isp` | Técnico Virtual — diagnóstico white-label para ISPs | Privado, produção | Classificador rede-vs-local com confiança calculada, e padrão de payload canônico (`ChamadoCanônico`) — referência direta para "conclusão padronizada". |

**Fora do domínio (8, checados e descartados):** `ei-raiz`, `vera-insights`, `Quebra_Nozes`, `Esquilo-Invest-2.0`, `esquilo-invest` (finanças pessoais) · `orbit-project` (orquestrador de agentes IA) · `7AgentsStudio.github.io`, `.github` (páginas institucionais).

---

## P0 — bloqueadores antes do MVP

| # | Dimensão | Veredito | Resumo |
|---|---|---|---|
| P0.1 | Protocolo de medição confiável | **Evoluir** | SignallQ Android já cobre múltiplas amostras + mediana ("Modo Triplo": 3 rodadas completas + mediana entre elas), rejeição de amostra inválida, banda/canal/largura. Falta: persistir BSSID/canal junto da medição (é capturado no scan mas nunca anexado ao resultado), timestamp/staleness do scan, detecção de troca de AP (roaming) durante a coleta, comparação estruturada sessão-a-sessão amarrada a local/ambiente. É um fix de schema (`ResultadoSpeedtest`/`MedicaoEntity`), não engine nova. |
| P0.2 | Diagnóstico de causa raiz rastreável | **Coberto** | A `FindingEngine` do SignallQ Android já implementa a cadeia completa sintoma → causa provável → confiança (0–1) → evidência → ação, com hipóteses descartadas registradas (nunca somem) e proveniência de dado (`medida`/`estimada`/`indisponível`) em cada métrica. É o motor mais maduro do portfólio inteiro. Reforço em `signallq-isp` (classificador rede-vs-local com confiança calculada). Cuidado: existe um segundo motor homônimo em `coreRecommendation` que é de monetização, não de causa raiz — não confundir. Falta: adaptar domínio de causas para visita técnica Wi-Fi doméstica e adicionar loop de "resultado após intervenção". |
| P0.3 | Modo de visita rápida | **Do zero** | Não encontrado em nenhum repositório — é fluxo de produto (tela + navegação), sem motor por trás. |
| P0.4 | Checklists por tipo de serviço | **Do zero** | `signallq-isp` tem checklist fixo de nível N1 para consumidor final, não "tipo de visita" (instalação/manutenção/mudança). A máquina de estados de fluxo do signallq-isp é boa referência de implementação (estrutura, não conteúdo). |
| P0.5 | Aceite do cliente | **Do zero (no código); já desenhado no protótipo** | Não encontrado em nenhum repositório de código — nem assinatura, nem checkbox de ciência de limitações, nem registro de recusa. **Nota:** o protótipo Claude Design do Pro (`SignallQ Pro - Protótipos.dc.html`) já tem a tela "3.4 Aceite do laudo (cliente)" desenhada — o design está à frente do código aqui, o que é esperado e saudável. |
| P0.6 | Conclusão padronizada | **Evoluir** | Padrão arquitetural maduro em dois repos: SignallQ Android usa enums fechados com evidência anexada (`DiagnosticStatus`, `VereditoUso`); `signallq-isp` tem `ChamadoCanônico` — payload padronizado que empacota diagnóstico + métricas + confiança antes de abrir chamado, com idempotência robusta. Falta apenas definir o enum específico de fechamento de visita (resolvido/parcial/dependência externa/recusado/inconclusivo) — a arquitetura já está validada em produção duas vezes. |

## P1 — depois de validar o fluxo

| # | Dimensão | Veredito | Resumo |
|---|---|---|---|
| P1.1 | Inventário de equipamentos / topologia | **Evoluir (manual primeiro)** | Nethal tem capability model sólido como referência, mas fingerprint com confiança teto de 0,55 e zero drivers estáveis (cobertura real de ~6 modelos). SignallQ Android tem inventário rico de fibra (`SnapshotFibra`) mas só 1 fabricante com parser de produção. Recomendação (igual à da pesquisa): inventário manual simples no MVP1, revisitar Nethal quando houver driver em estágio beta. |
| P1.2–P1.3 | Orçamento/materiais, garantia/retorno | **Do zero** | Não encontrado. Feature de produto pura, baixa complexidade dado o modelo de visita/cliente já definido. |
| P1.4 | Templates de laudo personalizáveis | **Evoluir** | `linka-webapp` tem `anatelReport.ts` — laudo formal completo com paginação multi-página real. Template fixo hoje (foco consumidor), mas a mecânica difícil (HTML→canvas→PDF paginado) já está resolvida. |
| P1.5–P1.7 | Duplicar visita · exportação CSV/JSON · backup e restauração | **Do zero** | Não encontrado, mas schemas de dado já mapeados (D1 canônico, `MedicaoEntity`) tornam exportação trivial. Backup depende da decisão `StorageProvider` já registrada na doc v5 (local-first + SAF) — arquitetura definida, implementação não começou. |
| P1.8–P1.9 | Link público verificável · cobertura por cômodo | **Do zero** | Nenhum precedente. Achado notável: `linka-webapp` tem Recharts instalado no `package.json` mas **zero importações reais** no código — nunca foi usado. Não existe nenhuma visualização de cobertura/qualidade por ambiente em nenhum lugar do portfólio. |

## P2 — não deve atrasar o MVP

| Item | Veredito |
|---|---|
| Heatmap com planta | Do zero (confirmado ausente) |
| Portal web (fundir linka-webapp + linka-speedtest) | Evoluir — já é decisão registrada na doc v5 |
| Acesso remoto a roteadores | Bloqueado por desenho (Nethal recusa qualquer alvo fora da LAN via `PrivateIpRanges.isPrivate()`) — decisão de segurança correta, não é gap |
| Integração completa do Nethal | Evoluir, sem pressa (repo com 6 dias de idade) |
| Google Agenda bidirecional | **Não verificado** nesta rodada |
| Gestão de equipe / estoque / comissão | **Não verificado** nesta rodada |
| NFS-e / conciliação Pix | **Não verificado** nesta rodada |
| Monitoramento contínuo | **Não verificado** nesta rodada |

---

## Leitura executiva

A pesquisa está certa sobre o que falta, mas subestimou o que já existe. O motor de causa raiz (`FindingEngine`) do próprio SignallQ é mais maduro do que qualquer coisa que os 5 repositórios auditados sugeririam construir do zero — é o ativo de engenharia mais valioso para o Pro, já testado e em produção. O padrão que se repete em quase todo P0 "Evoluir": a peça difícil (motor estatístico, engine de PDF, classificador, payload canônico) já está pronta em algum repositório — falta *fiação*, não *invenção*. Os únicos 3 itens genuinamente do zero em P0 (visita rápida, checklist por tipo, aceite do cliente) são fluxo de produto puro, sem motor técnico por trás.

**Próximos passos recomendados:**
1. Fix de schema no SignallQ Android (BSSID/canal/timestamp na medição) — destrava 3 sub-gaps do P0.1 de uma vez, PR pequeno no app existente.
2. Aplicar o corte de escopo que a pesquisa recomenda antes de abrir qualquer issue de UI — o MVP1 documentado hoje carrega ~30% a mais do que deveria.
3. Não integrar Nethal no MVP1 — inventário manual simples, revisitar quando houver driver em estágio beta.

## Documentos relacionados

- `00_CANONICO_v5.md` — dicionário canônico do ecossistema.
- `08_SignallQ_Pro_Especificacao_Funcional_v5.md`, `09_SignallQ_Pro_Jornada_e_Fluxo_de_Telas_v5.md`, `11_SignallQ_Pro_Roadmap_MVP1_MVP2_v5.md` — specs que este documento informa (corte de escopo do MVP1).
- Projeto Claude Design "SignallQ PRO app Android" (`69e53070-6aa8-485a-8d0a-5bfa36e1a08c`) — protótipo de telas do Pro, já com 33+ telas desenhadas cobrindo boa parte da jornada.
