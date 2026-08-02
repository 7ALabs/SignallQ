-- GH#1478 (Épico #1347, Feature F2): trilha de auditoria do backend admin de Feature Flags /
-- Firebase Remote Config. Distinta da tabela `feature_flags`/`feature_flag_audit` (migration
-- 005_sig13.sql) — aquela é um mecanismo local simples (SIG-13, blob de flags on/off gerido
-- direto no D1, sem Firebase). Esta tabela audita as operações reais contra o template do
-- Firebase Remote Config (GET/validate/publish/rollback), com ETag/versão de template — governa
-- as flags do catálogo canônico do Consumer (issue #1477, ainda em definição pelo Camilo).
--
-- Groundwork (#1478 parcial, 2026-07-26): tabela criada e já recebe registro nas tentativas de
-- validate/publish/rollback (mesmo enquanto os handlers ainda são skeleton bloqueado — status
-- 'blocked_pending_catalog' — até #1477 fechar o catálogo canônico que valida/publica de fato).
--
-- Aplicar via: npx wrangler d1 execute signallq-admin-db --file=migrations/021_gh1478_remote_config_audit.sql --remote

CREATE TABLE IF NOT EXISTS remote_config_audit_log (
  id                      TEXT    PRIMARY KEY, -- crypto.randomUUID()
  action                  TEXT    NOT NULL,    -- validate | publish | rollback
  status                  TEXT    NOT NULL,    -- ok | error | blocked_pending_catalog
  template_version_before TEXT,                -- versionNumber do template Remote Config antes (Firebase)
  template_version_after  TEXT,                -- versionNumber depois (NULL quando não publicou)
  etag_before             TEXT,                -- ETag lido antes da tentativa (proteção If-Match)
  etag_after              TEXT,                -- ETag retornado pelo Firebase após publish/rollback
  before_json             TEXT,                -- snapshot truncado dos parâmetros afetados antes
  after_json              TEXT,                -- snapshot truncado dos parâmetros afetados depois
  message                 TEXT    NOT NULL DEFAULT '',
  actor_user_id           TEXT    NOT NULL,    -- session.userId (admin_users)
  actor_role              TEXT    NOT NULL,    -- session.role
  created_at              INTEGER NOT NULL     -- Unix timestamp (segundos)
);

CREATE INDEX IF NOT EXISTS idx_rcal_action     ON remote_config_audit_log(action);
CREATE INDEX IF NOT EXISTS idx_rcal_created_at ON remote_config_audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_rcal_actor      ON remote_config_audit_log(actor_user_id);
