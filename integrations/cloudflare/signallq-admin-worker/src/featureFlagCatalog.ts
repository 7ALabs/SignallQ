// Catálogo canônico de Feature Flags do Consumer — issue #1478 (Épico #1347, Feature F2).
//
// DECISÃO DE ARQUITETURA (2026-07-26, Camilo): o Worker importa o JSON do catálogo Android
// DIRETO do repositório (`android/core/featureflags/.../consumer-catalog.json`), via import
// relativo cruzando de `integrations/cloudflare/signallq-admin-worker/` para `android/` — não
// existe uma segunda cópia do arquivo em lugar nenhum. O bundler do wrangler (esbuild) resolve
// imports relativos pelo filesystem, sem se importar com "workspace boundary" — funciona
// idêntico para JSON dentro ou fora da pasta do Worker.
//
// Por que essa opção e não as outras:
//   - Endpoint HTTP que serve o catálogo (`GET /admin/firebase/feature-flags/catalog`, abaixo)
//     PRECISA ler o catálogo de algum jeito — se o Worker buscasse o catálogo via rede
//     (ex.: `raw.githubusercontent.com`), toda validação de publish/sync passaria a depender de
//     uma chamada de rede externa a mais, com risco de rate-limit do GitHub, latência e uma
//     terceira fonte de falha em cada `publish`/`sync` — inaceitável para o gate de governança
//     do Épico #1347 ("o Admin não pode permitir criação de chaves arbitrárias; apenas chaves
//     presentes no catálogo").
//   - Copiar o arquivo manualmente (script de sync) tem o mesmo problema do
//     `SITE_INGEST_KEY`/dívida de tooling do repo: alguém esquece de rodar o script antes do
//     deploy e o Worker fica servindo catálogo desatualizado silenciosamente, sem nenhum sinal.
//   - Import direto do JSON faz o catálogo entrar no bundle a cada `npx wrangler deploy` — a
//     MESMA regra que já existe no projeto ("Worker Cloudflare: código editado ≠ deployado —
//     sempre `npx wrangler deploy` real antes de declarar validado", `.claude/CLAUDE.md`) já
//     cobre o caso "catálogo mudou, alguém precisa lembrar de re-deployar" — sem exigir uma
//     ferramenta nova, sem duplicar arquivo, sem chamada de rede extra em runtime.
//
// Trade-off aceito e documentado: se `consumer-catalog.json` mudar (ex.: Camilo adiciona uma
// flag nova em F4/#1480) e ninguém rodar `npx wrangler deploy` deste Worker depois, o backend
// admin continua servindo/validando contra o catálogo antigo até o próximo deploy — mesmo
// comportamento de qualquer mudança de código deste Worker, não uma categoria nova de risco.
//
// Import cross-diretório: `integrations/cloudflare/signallq-admin-worker/src/` -> 4 níveis
// acima (raiz do repo) -> `android/core/featureflags/src/main/resources/featureflags/`.
// Requer `resolveJsonModule: true` em tsconfig.json (ver commit desta mudança). O atributo
// `with { type: 'json' }` é exigido pelo ESM nativo do Node (usado por `node --test`) — o
// bundler do wrangler (esbuild) também entende essa sintaxe normalmente, sem mudança de
// comportamento no bundle publicado.
import catalogJson from '../../../../android/core/featureflags/src/main/resources/featureflags/consumer-catalog.json' with { type: 'json' }

export type FeatureFlagValueType = 'BOOLEAN' | 'STRING' | 'LONG' | 'DOUBLE'
export type FeatureFlagCriticality = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface FeatureFlagCatalogEntry {
  key: string
  module: string
  type: FeatureFlagValueType
  defaultValue: boolean | string | number
  criticality: FeatureFlagCriticality
  owner: string
  description: string
  disabledBehavior: string
  disabledMessage: string | null
  dependencies: string[]
  androidImplemented: boolean
  adminManaged: boolean
  analyticsEvent: string | null
}

export interface FeatureFlagCatalog {
  schemaVersion: string
  flags: FeatureFlagCatalogEntry[]
}

// Criticidades que exigem confirmação reforçada (motivo obrigatório) em publish/rollback —
// regra do Épico #1347: "kill switches críticos devem exigir confirmação explícita".
const REINFORCED_CONFIRMATION_CRITICALITIES: readonly FeatureFlagCriticality[] = ['HIGH', 'CRITICAL']

const CATALOG = catalogJson as unknown as FeatureFlagCatalog

export function loadFeatureFlagCatalog(): FeatureFlagCatalog {
  return CATALOG
}

export function findCatalogFlag(key: string): FeatureFlagCatalogEntry | undefined {
  return CATALOG.flags.find((flag) => flag.key === key)
}

// Só flags `adminManaged: true` podem ser criadas/publicadas pelo Admin — campo definido no
// próprio catálogo (schema do Épico #1347, "adminManaged: Se o Admin (F3) pode gerenciar").
export function adminManagedFlags(): FeatureFlagCatalogEntry[] {
  return CATALOG.flags.filter((flag) => flag.adminManaged)
}

export function requiresReinforcedConfirmation(criticality: FeatureFlagCriticality): boolean {
  return REINFORCED_CONFIRMATION_CRITICALITIES.includes(criticality)
}

// Mapeia o `type` do catálogo (schema Android) pro `valueType` da Firebase Remote Config REST
// API (`STRING`|`BOOLEAN`|`NUMBER`|`JSON`) — LONG/DOUBLE viram NUMBER (Remote Config não separa
// inteiro de ponto flutuante, é sempre texto por trás).
export function toRemoteConfigValueType(type: FeatureFlagValueType): 'STRING' | 'BOOLEAN' | 'NUMBER' {
  switch (type) {
    case 'BOOLEAN': return 'BOOLEAN'
    case 'LONG':
    case 'DOUBLE': return 'NUMBER'
    case 'STRING':
    default: return 'STRING'
  }
}

// A Firebase Remote Config REST API sempre representa `defaultValue.value` como string, mesmo
// para BOOLEAN/NUMBER (o SDK cliente faz o parse de volta pro tipo nativo).
export function stringifyDefaultValue(entry: FeatureFlagCatalogEntry): string {
  return String(entry.defaultValue)
}

export interface RemoteConfigParameterShape {
  defaultValue: { value: string }
  valueType: 'STRING' | 'BOOLEAN' | 'NUMBER'
  description: string
}

// Constrói o parâmetro no formato exigido pelo PUT .../remoteConfig ao CRIAR uma flag ausente —
// usa só o default embarcado no catálogo, nunca inventa valor.
export function catalogEntryToRemoteConfigParameter(entry: FeatureFlagCatalogEntry): RemoteConfigParameterShape {
  return {
    defaultValue: { value: stringifyDefaultValue(entry) },
    valueType: toRemoteConfigValueType(entry.type),
    description: entry.description,
  }
}
