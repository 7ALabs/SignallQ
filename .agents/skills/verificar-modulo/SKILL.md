---
name: verificar-modulo
description: Verifica se módulo, serviço, componente ou utilitário proposto já existe ou tem equivalente próximo antes da criação.
argument-hint: "<tipo> <nome>"
allowed-tools: Bash(grep *), Bash(find *), Bash(ls *), Bash(awk *)
---

# Verificar módulo/símbolo

Use depois de `inventario` quando ainda houver dúvida se um símbolo novo é necessário.

## Procedimento

1. faça match exato pelo nome;
2. procure variações e sinônimos;
3. compare responsabilidade, não apenas nome;
4. classifique:
   - **PASS** — nenhum equivalente razoável; criação pode ser considerada;
   - **WARN** — há candidato próximo; justificar extensão vs. criação;
   - **FAIL** — equivalente já existe; reutilize ou prove incompatibilidade.

Escopos típicos:

- módulo Gradle;
- Repository/UseCase/Service/Manager/Provider;
- Composable reutilizável;
- mapper/parser/helper/extension;
- Worker/serviço Cloudflare.

`PASS` não autoriza automaticamente arquitetura nova. Se a criação acionar gatilho do `AGENTS.md`, o Camillo precisa revisar o Architecture Plan antes da implementação.

## Saída

```text
VERIFICAR: PASS | WARN | FAIL
Proposto: <tipo/nome>
Matches exatos: ...
Equivalentes próximos: ...
Decisão recomendada: reusar | estender | criar | arquitetar antes
Justificativa: ...
```

A skill é mecânica/procedural e pode ser usada por qualquer agente técnico.
