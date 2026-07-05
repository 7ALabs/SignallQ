# Guia Rápido — Agentes SignallQ

**Última atualização:** 2026-06-21
**Mantido por:** Taisa

Referência rápida de quem é quem, o que cada agente faz e quando acionar. Para o pipeline completo de entrega, ver `docs/PIPELINE_AUTONOMO.md`.

---

## Squad Ativo

| Agente | Modelo | Cargo | Quando acionar |
|---|---|---|---|
| Claudete | Sonnet | Diretora de Produto & Delivery | Intake de features, prioridade, task breakdown, WIP, decisão Done/Not Done |
| Cláudio | Sonnet | Líder Técnico | Planejamento técnico, breakdown de arquitetura, mapeamento de impacto |
| Camilo | Sonnet | Especialista Android | Implementação Android: Kotlin, Compose, ViewModel, diagnóstico nativo, integração IA |
| Lia | Sonnet/Haiku | Especialista de Produto & UX | UX/UI, Material Design 3, estados visuais, microcopy, acessibilidade |
| Gema | Haiku (Sonnet se pesado) | Analista de Qualidade & Release | Review de código, bugs, regressões, higiene de entrega, gate de Done |
| Marcelo | Haiku | Analista Júnior de Discovery | Busca em codebase, grep de símbolos, listagem de arquivos, tasks pequenas |
| Nina | Haiku | Documentação Operacional | Changelog, checklist, resumo de PR, tarefas leves de doc |
| Taisa | Sonnet | Documentação Estratégica | Documentação técnica e funcional, auditorias, releases, agents |
| Otávio | Sonnet | Especialista Device/OS Android | Validação consultiva: device real, OEM quirks, APIs de sistema |
| Bernardo | Sonnet | Especialista em Redes de Acesso | Validação consultiva: thresholds de sinal, GPON, diagnóstico de rede, ANATEL |

---

## Responsabilidades por Agente

### Claudete — Diretora de Produto & Delivery

- Recebe feature bruta e transforma em user story com critérios de aceite
- Quebra user stories em tasks pequenas, independentes e verificáveis
- Controla WIP: cada agente tem no máximo 1 atividade ativa
- Gerencia filas por agente em `.claude/tasks/queue/<agente>/`
- Decide Done / Not Done com base em critérios objetivos
- Absorveu parte do papel de Cláudio (planejamento de produto e priorização)

**Quando acionar:** qualquer nova feature ou melhoria que precisa ser refinada antes de ir para implementação.

---

### Cláudio — Líder Técnico

- Quebra tasks grandes em passos executáveis e ordenados
- Mapeia impacto nos módulos Android (`:feature*`, `:core*`)
- Identifica arquivos prováveis com caminhos reais (via Marcelo)
- Identifica riscos de regressão e define ordem de execução segura
- Sinaliza quando Lia ou Otávio/Bernardo devem ser acionados

**Quando acionar:** tasks com impacto em múltiplos módulos, risco de regressão ou dependência de APIs de sistema. Obrigatório antes de Camilo em tasks complexas.

**Regra:** delega buscas de código ao Marcelo antes de qualquer Read/Grep.

---

### Camilo — Especialista Android

- Implementa features Android: Kotlin, Compose, ViewModel, StateFlow
- Realiza refactors seguros e pontuais
- Corrige bugs Android com impacto > 5 arquivos
- Integra IA no app (Cloudflare Worker, streaming, thinking tokens)
- Otimiza engines de diagnóstico Android

**Skills disponíveis:** `/android-platform-rules` (substitui consulta ao Otávio em casos simples), `/compose-implementation`, `/android-permissions-check`

**Quando não acionar:** bugfix simples ≤5 arquivos → Marcelo implementa.

---

### Lia — Especialista de Produto & UX

- Valida e define estados visuais (loading, erro, vazio, thinking, sucesso)
- Melhora hierarquia visual, espaçamento e tipografia conforme MD3
- Define e valida microcopy — textos curtos, objetivos, PT-BR
- Verifica acessibilidade: contraste, tamanho de toque, semantics TalkBack
- Verifica consistência com o SignallQ Design System (`.claude/skills/linka-design/` — diretório histórico)

**Modo:** Haiku para revisão simples de copy/MD3. Sonnet para decisão de fluxo e experiência complexa.

**Obrigatória quando:** tela nova, estado visual novo, microcopy visível ao usuário, mudança de fluxo de navegação.

---

### Gema — Analista de Qualidade & Release

- Review de implementações do Camilo
- Detecta bugs, regressões e riscos técnicos
- Verifica se testes foram feitos e passam
- Gate de Done: entrega só fecha com aprovação da Gema
- Higiene de entrega: versionamento (`libs.versions.toml`), CHANGELOG, task file fechado
- Valida tokens de design system na implementação

**Modelo:** Haiku por padrão. Sonnet apenas para análise de arquitetura ou review técnico pesado.

---

### Marcelo — Analista Júnior de Discovery

- Busca símbolos, classes, funções por nome ou padrão
- Lista arquivos de um módulo ou diretório
- Lê trechos de código para triagem antes de agentes Sonnet
- Verifica existência de componentes, testes e documentação
- Implementa tasks pequenas (<5 arquivos, sem mudança de contrato)

**Regra:** acionar ANTES de qualquer Read/Grep/Glob por agentes Sonnet (Cláudio, Camilo, Taisa, Claudete). Retorna dados brutos — o agente que acionou consolida e decide.

---

### Nina — Documentação Operacional

- Gera e atualiza CHANGELOG
- Escreve resumo de PR e notas de release
- Cria e revisa checklists
- Registra mudanças de arquitetura em docs
- Scout de documentação para Taisa (lista arquivos `.md` antes de auditorias)

**Quando não acionar:** documentação estratégica, técnica ou funcional → Taisa.

---

### Taisa — Documentação Estratégica

- Gera e atualiza `ANDROID_TECNICO.md`, `ANDROID_FUNCIONAL.md`, `RELEASES.md`
- Audita documentação existente antes de criar nova
- Atualiza arquivos de agentes em `.claude/agents/`
- Produz documentação adequada ao público (humano ou IA específica)
- Delega buscas de código ao Marcelo e buscas de docs à Nina

---

### Otávio — Especialista Device/OS Android (consultivo)

- Valida comportamento em devices reais antes de implementação crítica
- Identifica OEM quirks (Samsung One UI, MIUI, Moto)
- Confirma API level mínimo/máximo para APIs críticas
- Valida restrições de background execution (Doze, App Standby)
- Revisa comportamento de permissões runtime por OEM

**Quando acionar:** Obrigatório em tasks de permissões críticas, DNS, Wi-Fi, VPN, background service, ConnectivityManager. Em tasks simples, Camilo usa `/android-platform-rules` diretamente.

**Não implementa código.**

---

### Bernardo — Especialista em Redes de Acesso (consultivo)

- Valida thresholds de sinal Wi-Fi (RSSI/dBm, SNR, canal)
- Confirma parâmetros de fibra óptica GPON (potência óptica, atenuação)
- Valida thresholds de qualidade celular: RSRP, RSRQ, SINR para 4G/5G
- Orienta sobre CGNAT, duplo-NAT, IPv6, topologia de rede doméstica
- Valida conformidade ANATEL (Resolução 614/2013, Ato 7869/2022)

**Quando acionar:** antes de implementar ou alterar thresholds de diagnóstico de rede, engines de sinal, detecção de topologia. Label `needs:bernardo` na issue indica que validação é obrigatória.

**Não implementa código.**

---

## Labels do GitHub

| Prefixo | Exemplos | Uso |
|---|---|---|
| `area:` | `area:android`, `area:arquitetura`, `area:ux`, `area:qualidade` | Tipo de trabalho |
| `agent:` | `agent:camilo`, `agent:lia`, `agent:gema` | Agente responsável |
| `needs:` | `needs:bernardo` | Validação especializada necessária |
| Padrão GitHub | `bug`, `enhancement`, `documentation` | Classificação geral |

> **Atenção:** labels `type:*` e `status:*` não existem neste repo. Usar `area:*` para classificar tipo de trabalho.

---

## Fluxo de Pipeline (resumo)

```
/task [descrição]
    ↓
Claudete: refina, cria issue, task breakdown
    ↓
Cláudio: branch + plano técnico (tasks complexas)
    ↓
Camilo: implementa (Marcelo suporta busca)
    ↓       ↑ (loop se Gema reprovar)
Gema: review + gate de Done
    ↓
Done: PR mergeado, issue fechada, changelog atualizado
```

Para o protocolo completo com comandos de handoff, ver `docs/PIPELINE_AUTONOMO.md`.
