---
title: "SignallQ Pro — congelado (on hold)"
description: "Specs do Pro seladas em 2026-08-06; documentação não é mantida enquanto o produto está parado"
type: "índice"
status: "congelado"
owner: "Luiz (decisão de retomada) · Camilo (estado técnico)"
last_updated: "2026-08-06"
---

# SignallQ Pro — congelado

> ⛔ **Documentação congelada. Não manter, não atualizar, não usar como verdade sobre o consumer.**
> Reflete o estado em **2026-08-06** e não acompanha mudanças de código a partir daí.

## Decisão

**SignallQ Pro está on hold por tempo indeterminado**, decidido por Luiz em 2026-08-06.

**Condição de retomada:** maturação do SignallQ consumer em produção. O consumer está hoje em
trilha **alpha** de teste — enquanto não estabilizar em produção, o Pro não recebe investimento de
desenvolvimento nem de documentação.

## Estado no congelamento

O código do Pro **permanece no repositório** (`android/pro/`) e continua compilando. Não foi
removido nem extraído — a extração para repositório próprio é decisão separada, a reavaliar quando
o Pro sair do hold.

| Item | Estado em 2026-08-06 |
|---|---|
| Módulos Gradle | 9 — `:pro:app`, `:pro:core:designsystem`, `:pro:core:database`, `:pro:feature:{auth,cliente,visita,ambiente,medicao-diagnostico,laudo}` |
| Versão | `proVersionName 0.3.0` / `proVersionCode 8` (contador próprio, `libs.versions.toml:13-14`) |
| applicationId | `io.signallq.pro` — sem Firebase e sem `signingConfig` (issue #1158 pendente) |
| Código | ~5.700 linhas Kotlin; APK debug compila |
| Persistência | Room v3, 11 tabelas, migrations com teste (`Migration1Para2Test`) |
| Design system | 19 componentes; paleta azul implementada e conferida (7/7 tokens) |
| Lacunas conhecidas | Grupo 5 (Ferramentas) e Grupo 3 (Entrega/Financeiro, bloqueado por #1160) sem destino de navegação; `feature/cliente` só tem cadastro, sem listagem |

## Documentos selados

| Doc | Conteúdo |
|---|---|
| `08_..._Especificacao_Funcional_v5.md` | Especificação funcional |
| `09_..._Jornada_e_Fluxo_de_Telas_v5.md` | Jornada e fluxo de telas |
| `10_..._Design_System_v5.md` | Design system (paleta azul — implementada no código) |
| `11_..._Roadmap_MVP1_MVP2_v5.md` | Roadmap MVP1/MVP2 |
| `12_..._Auditoria_Cobertura_Repositorios_2026-07-18.md` | Auditoria de cobertura vs. pesquisa de mercado |
| `13_..._Arquitetura_e_Reaproveitamento_v1.md` | Arquitetura e reaproveitamento com o consumer |

## Divergências conhecidas nestes documentos

Apuradas na validação contra código de 2026-08-06. Ficam registradas aqui em vez de corrigidas,
porque corrigir documentação de produto parado é trabalho sem retorno. **Resolver na retomada:**

1. **Glossário em inglês vs. código em português.** As specs fixam `customer`, `service_location`,
   `environment`, `measurement_session`, `measurement_point`. O código implementou `cliente`,
   `local`, `ambiente`, `medicao_pro`, `ponto_walktest_pro` — seguindo a regra de idioma de
   `.claude/rules/higiene-e-padronizacao-repositorio.md §2`, que manda PT-BR para conceitos de
   domínio. **O código está certo; o glossário nunca foi adotado.** Na retomada, oficializar o
   PT-BR e revogar o glossário em inglês, ou decidir o contrário explicitamente e assumir a
   renomeação de 11 tabelas.
2. **Tabelas previstas que não existem:** `account`, `identity_provider`, `session`, `pix_charge`,
   `pix_profile`, `storage_object`. Os análogos reais são `profissional`, `visita` e `evidencia`.
3. **Storage.** As specs preveem Android SAF com `StorageProvider` abstraindo providers. O código
   grava em `context.filesDir` + `FileProvider`. SAF, `StorageProvider` e R2 não têm uma linha
   escrita.
4. **Tokens de cor no módulo errado.** A paleta vive em `:pro:app/ui/theme/SignallQProColor.kt`, não
   em `:pro:core:designsystem`, que duplica alguns valores em `ProStatusColor.kt`/`ProSurfaceColor.kt`.

## Ao retomar

1. Revalidar todos os seis documentos contra o código antes de usar qualquer afirmação deles.
2. Resolver a divergência 1 (idioma) primeiro — ela decide o modelo de dados.
3. Decidir se o Pro sai para repositório próprio.
4. Trocar o `status` deste README de `congelado` para `ativo` ou remover a pasta.
