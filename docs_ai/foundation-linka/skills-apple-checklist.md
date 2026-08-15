---
title: "Checklist de skills Apple — Linka"
description: "Lista das skills que o repo Linka precisará criar, e das que podem ser reaproveitadas como processo cross-produto"
type: "referência"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-15"
---

# Checklist de skills Apple — Linka

**Isto é uma lista, não uma implementação.** Nenhuma skill deste checklist é criada nesta PR.
Propósito: quando o repo `linka` nascer, o squad sabe de cara o que falta em `.claude/skills/` sem
precisar redescobrir olhando as skills do SignallQ uma por uma.

Cada skill **precisa ser criada no repo Linka do zero**, adaptada à stack e à plataforma Apple.
Nenhuma skill Android/Kotlin/Compose migra ou é copiada automaticamente — o conteúdo técnico é
outro (SwiftUI não é Compose, HIG não é Material 3, StoreKit não é Play Billing).

## Skills novas, específicas Apple (a criar do zero)

| Skill | Propósito | Análoga a (SignallQ) |
|---|---|---|
| `/regras-apple` | API levels iOS/iPadOS/macOS suportados, deprecations da Apple, restrições e guidelines de App Store review (2.x, 3.x, 4.x), permissões Info.plist e justificativa exigida. | `regras-android` |
| `/padroes-swiftui` | Estrutura de View, `@State`/`@Binding`/`@StateObject`/`@ObservedObject`, padrão de navegação (NavigationStack ou equivalente), anti-padrões SwiftUI (view gigante, side-effect em body, etc.). | `padroes-compose` |
| `/storekit-check` | Configuração de produtos StoreKit, IAP, assinatura, sandbox testing, validação de receipt, tratamento de renovação/cancelamento/reembolso. **Não tem análogo no SignallQ** — é 100% novo porque o SignallQ é grátis com ads, não tem StoreKit. |
| `/motor-diagnostico-apple` | Engine de diagnóstico de rede nativo Apple. Compartilha *domínio* com o SignallQ (thresholds, vocabulário, ANATEL — ver `regras-diagnostico-rede` do repo `signallq` como referência de partida), mas a implementação é Swift, não Kotlin. Se o backend Cloudflare for compartilhado entre os dois produtos (decisão ainda em aberto — ver `AGENTS.md.template`), esta skill deve citar isso explicitamente. | `regras-diagnostico-rede` + `motor-diagnostico` |
| `/design-check-apple` | Validação de tela/View contra Human Interface Guidelines — o equivalente Apple do gate MD3 estrito do SignallQ. Cobre tipografia (SF Pro/Dynamic Type), espaçamento, cor, componentes nativos vs. customizados, dark mode. | `design-check` |

## Skills de processo, prováveis candidatas a compartilhar

Estas são skills de **processo cross-produto** no SignallQ — não têm conteúdo específico de
stack/plataforma, então é provável que o squad Linka queira algo equivalente. Mas "provável" não é
"automático": cada uma precisa ser recriada (ou adaptada) no repo `linka`, mesmo que o conteúdo
final seja quase idêntico.

| Skill (SignallQ) | Por que é candidata a cross-produto |
|---|---|
| `estimativa-impacto` | Framework de tamanho/risco/milestone não depende de Kotlin nem Swift. |
| `checar-release` | Estrutura de checklist pré-release é genérica; os itens dentro (build, assinatura, changelog) mudam para Xcode/TestFlight/App Store. |
| `handoff` | Formato de handoff entre agentes é definido pelo contrato operacional Buildea (`ai-governance/policies/agent-operating-contract.md`), não pela stack. |
| `check-done` | Os 9 critérios de conclusão do contrato operacional são cross-produto; os comandos técnicos de validação dentro de cada critério mudam. |
| `inventario` | Princípio anti-duplicação (ver o que já existe antes de criar) é universal; a implementação lê `settings.gradle.kts` no SignallQ — o Linka precisa de uma versão que leia a estrutura de packages Swift (SPM `Package.swift` ou estrutura Xcode). |
| `verificar-modulo` | Mesmo raciocínio de `inventario` — princípio cross-produto, implementação específica de stack. |

**Se essas skills viverem em `ai-governance/policies/` como processo compartilhado**, em vez de
serem recriadas em cada repo, isso é decisão de governança organizacional (fora do escopo desta
foundation) — hoje elas vivem como skill local do SignallQ em `.claude/skills/`, então o caminho
mais direto é recriar localmente em `linka` seguindo o mesmo padrão, não assumir compartilhamento
automático entre repos.

## O que não migra e não ganha análogo

- `cloudflare-d1-console` — específico do schema D1 do Console SignallQ; só relevante para o Linka
  se ele decidir compartilhar backend Cloudflare com o SignallQ (em aberto).
- `protocolo-ci-android`, `protocolo-ktlint` — específicos de dependabot/Kotlin/Ktlint; o
  equivalente Apple (dependência via SPM, SwiftLint) só nasce se o squad Linka achar que precisa de
  skill dedicada — pode não valer a pena vs. só documentar em `AGENTS.md`.
- `SignallQ-design`, `signallq-code-design`, `signallq-arch`, `signallq-docs` — guardiões
  específicos do design system e arquitetura do SignallQ; o Linka precisa dos próprios guardiões
  (design system Apple/HIG, arquitetura Swift) se o squad decidir que o volume de trabalho justifica
  automatizar isso como skill, e não antes.

## Ao instanciar

1. Confirmar com Luiz e com o squad Linka recém-formado se a lista acima ainda faz sentido — este
   checklist foi escrito sem o repo existir, é hipótese razoável, não verdade testada.
2. Criar as skills específicas Apple (seção 1) conforme a necessidade real aparecer — não é
   obrigatório criar as 5 no dia 1; a boa prática do SignallQ (skill nasce quando resolve um problema
   recorrente, não especulativamente) vale aqui também.
3. Recriar as skills de processo (seção 2) sob demanda, adaptando exemplos e comandos para a stack
   Apple.
4. Manter este checklist atualizado ou removê-lo do `linka` assim que deixar de ser útil — ele é
   material de bootstrap, não documentação viva permanente.
