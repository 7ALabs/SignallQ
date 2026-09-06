---
name: inventario
description: Lista implementações existentes antes de criar módulo, serviço, componente, utilitário ou Worker novo no SignallQ.
argument-hint: "[--modulos|--componentes|--utilitarios|--workers|--tudo] [--grep <termo>]"
allowed-tools: Bash(grep *), Bash(find *), Bash(cat *), Bash(ls *), Bash(awk *)
---

# Inventário

Use quando uma tarefa propõe símbolo reutilizável ou responsabilidade nova. O objetivo é impedir duplicação silenciosa.

Consulte o **estado real do código**, não uma lista manual possivelmente desatualizada:

- módulos: `android/settings.gradle.kts`;
- componentes: árvore Android de `ui/component`/equivalentes;
- serviços, repositories, use cases, managers, providers, mappers e parsers: busca em `android/`;
- utilitários/extensões: módulos `core*` e compartilhados;
- Workers: `integrations/cloudflare/` e seus `wrangler.toml`.

## Procedimento

1. liste candidatos por nome e responsabilidade;
2. procure sinônimos e implementações próximas, não só match exato;
3. informe caminho e propósito observado;
4. se algo cobre boa parte do caso, recomende reuso/extensão;
5. se criação ainda for necessária, registre por que os candidatos não servem.

Se a criação introduzir novo módulo, serviço estrutural, Worker/API ou mudança de fronteira entre módulos, avalie o gate do Camillo antes de escrever código.

## Saída

```text
INVENTÁRIO — <termo/escopo>
Existentes:
- caminho — símbolo — responsabilidade
Candidatos a reuso:
- ...
Lacuna real: ...
Gate Camillo: SIM/NÃO — motivo
Próximo: reusar | estender | verificar-modulo | arquitetar | criar
```

Qualquer agente técnico pode usar esta skill. Ela não pertence a uma persona e não define modelo de IA.
