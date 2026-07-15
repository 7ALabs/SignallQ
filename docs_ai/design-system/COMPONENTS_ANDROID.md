# Componentes Android — SignallQ

**Escopo:** Android v0.23.0+  
**Localização principal:** `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/component/`  
**Última atualização:** 2026-07-15

---

## Fonte de verdade para componentes-base

Os componentes-base compartilhados do fluxo principal devem convergir para:

- `LkSurfaceCard`
- `LkSectionOverline`
- `LkSheetSectionTitle`
- `LkSheetInfoRow`
- `LkSheetDivider`
- `LkSheetFrame`

Arquivo atual:

- [BaseComponents.kt](C:/Projetos/SignallQ/android/app/src/main/kotlin/io/veloo/app/kotlin/ui/component/BaseComponents.kt)

---

## Diretriz de arquitetura visual

Toda tela nova ou refinada deve:

1. usar tokens de `SignallQTheme.kt`
2. preferir componentes compartilhados
3. evitar recriar manualmente card, row, badge, divider e frame de sheet

Se um padrão visual se repetir em duas ou mais telas, ele deve migrar para a biblioteca compartilhada.

---

## Situação atual

- A biblioteca compartilhada já existe, mas ainda cobre só parte dos padrões.
- Muitas telas ainda reimplementam localmente:
  - cards de seção
  - rows com ícone + título + subtítulo + ação
  - headers de sheet
  - badges e disclosures
  - métricas em grade

---

## Legado

Ainda existem componentes históricos/auxiliares no repositório que não devem orientar o fluxo principal atual. Isso inclui componentes ligados à antiga superfície dedicada de IA e outras estruturas legadas. Eles podem continuar no código por compatibilidade, histórico ou uso interno residual, mas não devem servir como referência para novas telas do fluxo principal.

---

## Meta da fase de saneamento

Ao final da padronização, o fluxo principal do app deve depender majoritariamente de uma biblioteca compartilhada pequena, previsível e reutilizável, em vez de estilização artesanal por tela.
