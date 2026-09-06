---
name: analytics-spec
description: Especifica telemetria antes da implementação, incluindo evento, propriedades, gatilho, anti-gatilho, privacidade e consumidor.
argument-hint: "<feature-ou-fluxo>"
allowed-tools: Bash(grep *), Read
---

# Analytics Spec

Use quando um comportamento novo ou alterado precisa ser medido. Produto decide **o que vale medir**; esta skill transforma a decisão em contrato de telemetria verificável.

## Antes de criar evento

Procure eventos existentes para evitar duplicação semântica:

```bash
grep -rhoE '"[a-z][a-z0-9_]+"' android/app/src/main/kotlin/*/analytics/*.kt 2>/dev/null | sort -u
```

Use a convenção real do código como fonte de verdade.

## Especificação

Para cada evento, registre:

- **nome**;
- **propriedades** com tipo e finalidade;
- **gatilho exato**;
- **anti-gatilho** — situações parecidas que não devem contar;
- **dado sensível proibido/evitado**;
- **consumidor** (Firebase, Crashlytics, Worker etc.);
- **retenção/política**, se diferente do comportamento atual;
- **teste/validação** esperada.

Nunca envie por conveniência IP, SSID, localização, identificador bruto ou outro dado pessoal sem necessidade de produto, base de privacidade e aprovação aplicável.

Mudança de política de dado sensível ou integração sistêmica aciona os gates de `AGENTS.md`; se a telemetria atravessar app↔Worker/API/contrato, Camillo deve revisar a arquitetura.

## Saída

```text
ANALYTICS-SPEC — <fluxo>
Evento: ...
Propriedades:
- nome: Tipo — finalidade
Dispara quando: ...
Não dispara quando: ...
Dados sensíveis: ...
Consumidor: ...
Privacidade/retenção: ...
Gate Camillo: SIM/NÃO — motivo
Validação: ...
Evento equivalente existente: SIM/NÃO — evidência
```

A skill não implementa `logEvent`, não analisa dados históricos e não aprova nova coleta sensível.
