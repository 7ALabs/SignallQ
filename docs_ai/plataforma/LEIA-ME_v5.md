# SignallQ Platform — Documentação v5

Versão consolidada e reconciliada do pacote v1→v4. O v5 resolve as contradições internas do pacote anterior, traz os documentos órfãos ao mesmo patamar, e — pela primeira vez — separa **o que já existe (ATUAL)** do **que é proposta (ALVO)**, com base em validação contra o código real do repositório `gmmattey/linka-android`.

- **Versão:** 5.0 · **Data:** 17/07/2026
- **Formato:** Markdown (fonte da verdade, versionável).

## Como ler

Comece por dois documentos:

1. **`00_CANONICO_v5.md`** — o dicionário único. Nomes de tabela, catálogo de eventos, árvore do monorepo, paleta, mapa atual-vs-alvo e decisões pendentes. **Em qualquer divergência, este prevalece.**
2. **`00_CHANGELOG_e_Validacao_Cruzada_v5.md`** — a avaliação de cobertura, as contradições encontradas (C1–C10), a validação contra o código e o que mudou de v4→v5.

## Índice

| # | Documento | Substitui | Superfície |
|---|---|---|---|
| — | `00_CANONICO_v5.md` | (novo) | Transversal |
| — | `00_CHANGELOG_e_Validacao_Cruzada_v5.md` | (novo) | Transversal |
| 01 | `01_SignallQ_Platform_Arquitetura_v5.md` | Platform Arquitetura v4 + Arquitetura Android v3 | Plataforma |
| 02 | `02_SignallQ_Platform_Especificacao_Tecnica_v5.md` | Especificação Técnica v4 | Plataforma |
| 03 | `03_SignallQ_Governanca_GitHub_e_Monorepo_v5.md` | Governança v3 | Plataforma |
| 04 | `04_SignallQ_Modelo_Dados_D1_v5.md` | Modelo de Dados D1 v4 | Backend/Dados |
| 05 | `05_SignallQ_Telemetria_Analytics_v5.md` | Telemetria e Analytics v4 | Backend/Dados |
| 06 | `06_SignallQ_Arquitetura_Storage_v5.md` | Arquitetura de Storage v4 | Backend/Dados |
| 07 | `07_SignallQ_Admin_Especificacao_v5.md` | Admin Especificação v4 | Admin |
| 08 | `08_SignallQ_Pro_Especificacao_Funcional_v5.md` | Especificação Funcional v4 | SignallQ Pro |
| 09 | `09_SignallQ_Pro_Jornada_e_Fluxo_de_Telas_v5.md` | Jornada e Fluxo de Telas v3 | SignallQ Pro |
| 10 | `10_SignallQ_Pro_Design_System_v5.md` | Design System v3 | SignallQ Pro |
| 11 | `11_SignallQ_Pro_Roadmap_MVP1_MVP2_v5.md` | Roadmap MVP1/MVP2 v2 | SignallQ Pro |

## O que mudou de v4 para v5 (resumo)

- **Contradições resolvidas** (detalhe no changelog, C1–C10): catálogo de eventos único em `dot.case`; dicionário de dados D1 único; tabelas Pix adicionadas ao modelo; uma única árvore de monorepo; StorageProvider local-first (R2 vira add-on pago); paleta Pro corrigida (`#6C2BFF` morto → acento `#5B21D6`); "SignallQ Nethal" em vez das 5 grafias antigas.
- **Órfãos atualizados:** Design System, Jornada, Governança (eram v3) e Roadmap (era v2) trazidos à v5. O detalhe de Arquitetura Android, que o v4 havia perdido, foi reincorporado ao doc 01.
- **Atual vs. Alvo:** cada documento marca o que existe hoje (SignallQ consumer 0.26.0/vc62, Admin React 19, 5 workers Cloudflare, 13 tabelas D1 do Admin, telemetria Firebase) e o que é proposta (SignallQ Pro, Portal, monorepo `signallq-platform`, modelo D1 Pro, pipeline de telemetria Cloudflare, Nethal internalizado).

## Auditorias pontuais (fora do pacote v5, mas informam corte de escopo)

| Documento | Data | Escopo |
|---|---|---|
| `12_SignallQ_Pro_Auditoria_Cobertura_Repositorios_2026-07-18.md` | 18/07/2026 | Cruza os 16 gaps de uma pesquisa de mercado do SignallQ Pro contra o código real dos 13 repositórios do GitHub — o que já existe, o que evolui, o que constrói do zero. |

## Pendências que o v5 expõe (não resolve — exigem decisão)

Preço do Pro; domínio `signallq.app`; provedor de identidade; conectores de nuvem além do SAF; política de retenção; ADRs e contratos OpenAPI a materializar. Ver `00_CANONICO_v5.md §8`.

## Nota de proveniência

Os textos-fonte v1→v4 foram fornecidos em DOCX/PDF. O v5 é reautoria em Markdown a partir desse conteúdo, com validação contra o código do repositório em 17/07/2026. Recomenda-se mover este pacote para a fonte da verdade de documentação (repo em `docs/` ou Notion) conforme a convenção do time.
