---
description: Claudete processa observações de skill acumuladas, decide o que atualizar, criar ou deprecar, e delega escrita para Nina e aprovação para Gema.
---

## Quando usar

Claudete executa após cada task concluída (acionada pela Nina via handoff com `--motivo skill-review`), **somente se houver arquivos de observação não processados** no diretório do mês atual.

Se não houver observações pendentes → encerrar imediatamente, sem trabalho adicional.

## Quem usa

Claudete — exclusivamente.

## Passos

### 1. Verificar observações pendentes

```bash
Get-ChildItem ".Codex/skill-observations/$(Get-Date -Format 'yyyy-MM')\" -Filter "obs-*.md" |
  Where-Object { (Get-Content $_.FullName | Select-String "## Processado em") -eq $null }
```

Se a lista estiver vazia → encerrar. Nada a processar.

### 2. Ler cada observação não processada

Para cada arquivo encontrado, ler o conteúdo completo e extrair:
- **Missing skills** com slug sugerido, priority e ocorrências anteriores
- **Incomplete skills** com gap concreto e sugestão
- **Removal candidates** com motivo

### 3. Agrupar por tipo de signal

Consolidar observações de múltiplos agentes sobre o mesmo padrão:
- Contar quantas observações independentes mencionam a mesma missing skill (mesmo slug)
- Contar quantas observações independentes mencionam a mesma removal candidate

### 4. Aplicar regras de decisão

| Signal | Critério | Ação |
|---|---|---|
| Missing skill | ≥2 observações independentes com mesmo padrão | Criar nova skill — delegar a Nina |
| Missing skill | 1 observação | Adicionar ao `backlog.md` |
| Incomplete skill | ≥1 observação com gap concreto | Atualizar skill — delegar a Nina |
| Removal candidate | ≥2 observações independentes | Soft-delete — delegar a Nina |
| Removal candidate | 1 observação | Ignorar |

**Proteção antes de qualquer deprecação:**
Verificar se a skill está listada em `## Skills recomendadas` de algum agent `.md`. Se sim, a referência deve ser removida do agent antes de Nina deprecar a skill — incluir isso na instrução para Nina.

### 5. Delegar para Nina

Para cada ação decidida, acionar Nina com instrução exata e completa:

**Para criar nova skill:**
> Nina, crie `.Codex/skills/[slug]/SKILL.md` com os seguintes conteúdos: [descrição do domínio, trigger, quem usa, passos essenciais, limites]. Base: observações [obs-file-1, obs-file-2].

**Para atualizar skill:**
> Nina, atualize `.Codex/skills/[skill-name]/SKILL.md`. Adicione [seção/subseção] com [conteúdo específico]. Base: observação [obs-file].

**Para soft-delete:**
> Nina, deprece `.Codex/skills/[skill-name]/SKILL.md`. Motivo: [motivo]. Evidências: [obs-file-1, obs-file-2]. Verificar referências em agents antes de renomear.

**Para backlog:**
Adicionar linha em `.Codex/skill-observations/backlog.md`:
| [data] | [slug sugerido] | [task-id] | [agente] | [padrão observado resumido] |

### 6. Marcar observações como processadas

Ao final de cada arquivo de observação lido, adicionar:

```markdown
## Processado em [DATA]
Decisão: [criar | atualizar | soft-delete | backlog | ignorar]
Ação: [descrição da ação ou "nenhuma"]
```

### 7. Aguardar aprovação da Gema

Após Nina completar as alterações, Claudete posta para Gema:
> Gema, revise as alterações de skill em `.Codex/skills/[nome]/SKILL.md` antes de confirmar. Alterações baseadas em observações de task #N.

## Output esperado

- Observações processadas e marcadas
- Skills criadas, atualizadas ou deprecadas (delegadas a Nina + aprovadas pela Gema)
- Backlog atualizado com observações únicas
- Nenhuma skill removida fisicamente (soft-delete apenas)

## Limites

- Claudete não escreve conteúdo de skill — delega sempre para Nina.
- Claudete não depreca skill sem ≥2 observações independentes.
- Hard-delete nunca é executado automaticamente — exige confirmação explícita do usuário.
- Skill com menos de 30 dias de existência não é candidata a remoção, independente do número de observações.
- Claudete não processa observações de tasks ainda não mergeadas.
