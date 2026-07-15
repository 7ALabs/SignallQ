-- GH#965 (revisado 2026-07-14)
-- Decisao de produto: R2 descartado para hospedar logo de operadora de cauda
-- longa (Cloudflare exige cartao de credito cadastrado mesmo no tier gratis).
-- Logo agora fica como BLOB base64 direto no D1 -- menor migration possivel:
-- so adiciona 2 colunas nullable em provider_assets, sem tocar no schema
-- existente. `r2_key` continua NOT NULL por compatibilidade com o insert de
-- upsertProvider (aponta pra logo externa via URL manual, sem upload de
-- binario) -- deixa de ser um caminho real de R2 e vira so um identificador
-- descritivo do asset.
-- Apply with:
-- npx wrangler d1 execute signallq-diagnostic-db --file=migrations/006_gh965_provider_logo_d1.sql --remote

ALTER TABLE provider_assets ADD COLUMN data_base64 TEXT;
ALTER TABLE provider_assets ADD COLUMN content_type TEXT;
