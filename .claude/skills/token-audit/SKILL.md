---
name: token-audit
description: Detecta desperdício de tokens no ecossistema Linka. Identifica prompts gigantes, leitura excessiva de arquivos, repetição de contexto e padrões que inflam custo sem valor. Use quando sessão parecer cara ou lenta.
---

Audite o desperdício de tokens na sessão ou contexto abaixo:

$ARGUMENTS

---

## O que detectar

### Desperdício de leitura

| Padrão | Impacto |
|---|---|
| Arquivo lido inteiro quando apenas função específica era necessária | Alto |
| Módulo aberto sem uso posterior no plano | Médio |
| Mesmo arquivo lido múltiplas vezes na sessão | Médio |
| `Glob **/*.kt` em módulo grande quando arquivo específico bastava | Alto |
| Read de CLAUDE.md a cada subtask em vez de uma vez por sessão | Baixo |

### Desperdício de geração

| Padrão | Impacto |
|---|---|
| Agente repete contexto completo que o anterior já entregou | Alto |
| Formato de 8 seções para task simples de 2 linhas | Médio |
| Justificativa de cada microdecisão quando "implementei X" bastava | Baixo |
| Raciocínio filosófico ou chain-of-thought completo visível | Alto |
| Explicar o que o código faz em vez de o que faz diferente | Baixo |

### Desperdício de planejamento

| Padrão | Impacto |
|---|---|
| Cláudio (opus) planeja task trivial que Camilo poderia resolver direto | Alto |
| map-impact acionado para bugfix de 5 linhas | Médio |
| design-review acionado para mudança de token de cor existente | Médio |
| Otávio consultado para task sem domínio crítico Android | Baixo |

---

## Regras de leitura incremental (referência)

- Use `Grep` por símbolo/classe específico antes de `Read` do arquivo
- Se Grep encontrar o arquivo, `Read` apenas as linhas relevantes (offset + limit)
- Não abra módulo inteiro para encontrar uma função
- Leia CLAUDE.md uma vez por sessão, não a cada subtask
- Prefira `Grep -A 10` ao redor do match a `Read` do arquivo inteiro

---

## Entregue

1. **Padrão de desperdício identificado** — onde e como está acontecendo
2. **Impacto estimado** — baixo / médio / alto por ocorrência
3. **Como corrigir** — mudança específica no prompt, agente ou skill
4. **Ganho esperado** — redução estimada de custo/contexto por sessão

[PRÓXIMO: nenhum agente específico — use os achados para ajustar comportamento na sessão]
