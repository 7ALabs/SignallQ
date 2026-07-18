# SignallQ — Arquitetura de Armazenamento

**Status:** ativo · **Versão:** 5.0 · **Data:** 17/07/2026 · **Substitui:** SignallQ Arquitetura de Storage v4

Local-first, nuvem do próprio técnico e hospedagem opcional para evidências, fotos, PDFs, logos e anexos da SignallQ Pro.

> **Fonte única de nomes e decisões:** `00_CANONICO_v5.md`. A decisão canônica de storage está em Canônico §6.

---

## Estado atual vs. Alvo

- 🎯 **ALVO** — Toda esta arquitetura serve à **SignallQ Pro** (`io.signallq.pro`), que ainda não existe. `StorageProvider`, providers e o metadado `storage_object` são proposta greenfield.
- ✅ **Decisão canônica firme** — Independente de estar implementado, o v5 reafirma: **local-first + Android SAF no MVP; `StorageProvider` abstrai tudo; R2 é apenas add-on hospedado pago, nunca o storage padrão** (Canônico §6). Qualquer documento anterior que diga "R2 com URLs assinadas" como storage principal (Arquitetura Android v3, Roadmap) está desatualizado e é corrigido aqui.

---

## 1. Objetivo

Evitar que a SignallQ Pro dependa obrigatoriamente de armazenamento pago mantido pela plataforma, permitindo operação local e associação da nuvem do próprio profissional. Referência para desenvolvimento mobile, backend, segurança, produto e planos comerciais.

Escopo: fotos, evidências, PDFs, logos e anexos; `StorageProvider` e sincronização; opções gratuitas, BYOC e hospedadas; segurança, conflito, backup e portabilidade.

---

## 2. Decisão de produto (canônica)

O MVP deve ser **local-first** e permitir que o técnico escolha uma pasta pelo **Storage Access Framework (SAF)** do Android. Essa pasta pode estar no armazenamento local ou em um provedor de nuvem apresentado pelo próprio sistema. Assim, o aplicativo não precisa inicialmente manter tokens específicos de Google Drive, OneDrive ou Dropbox.

**R2 é add-on hospedado pago**, nunca o storage padrão (Canônico §6). Conectores OAuth diretos além do Android SAF são **decisão pendente** (Canônico §8.4).

### Estratégia por modo

| Modo | Custo para a SignallQ | Experiência | Prioridade |
|---|---|---|---|
| Local no dispositivo | Praticamente zero | Funciona offline; risco se o usuário perder o aparelho. | MVP |
| Pasta escolhida via Android SAF | Praticamente zero | O técnico escolhe pasta local ou provedor exposto pelo sistema, como Drive/OneDrive. | MVP recomendado |
| Conector OAuth direto | Baixo a moderado | Melhor automação e status, mas exige integrações e manutenção por provedor. | MVP2 |
| R2 hospedado pela SignallQ | Variável | Experiência transparente e centralizada. | Add-on/plano futuro (pago) |
| S3 compatível do técnico | Baixo para a SignallQ | Usuário fornece endpoint/bucket próprios. | Futuro avançado |

---

## 3. Interface StorageProvider

```kotlin
interface StorageProvider {
  suspend fun put(request: PutObject): StoredObject
  suspend fun open(objectRef: ObjectRef): InputStream
  suspend fun delete(objectRef: ObjectRef)
  suspend fun exists(objectRef: ObjectRef): Boolean
  suspend fun list(prefix: String): List<ObjectRef>
  suspend fun healthCheck(): ProviderHealth
}
```

Toda escrita/leitura de arquivo passa por esta abstração; nenhuma tela ou domínio fala diretamente com um provider concreto.

---

## 4. Providers previstos

| Provider | Uso | Observação |
|---|---|---|
| `DeviceLocalProvider` | Cache e armazenamento local. | Sempre disponível; deve alertar sobre backup. |
| `AndroidSafProvider` | Pasta escolhida pelo usuário. | Primeira opção para nuvem própria sem integração dedicada. |
| `GoogleDriveProvider` | Integração OAuth direta. | Futuro, se houver ganho claro além do SAF. |
| `OneDriveProvider` | Integração OAuth direta. | Futuro. |
| `DropboxProvider` | Integração OAuth direta. | Futuro. |
| `S3CompatibleProvider` | R2, Backblaze, MinIO ou storage próprio. | Avançado; credenciais protegidas. |
| `SignallQHostedProvider` | Storage contratado e administrado pela plataforma (R2). | Opcional e precificado — add-on pago, nunca padrão. |

---

## 5. Metadados no D1 (`storage_object`)

Os binários **nunca** vão para o D1; só o metadado (Canônico §2, tabela `storage_object`).

| Campo | Descrição |
|---|---|
| `provider` | Tipo do provider. |
| `owner_id` | Profissional proprietário. |
| `object_key` | Referência lógica, não URL pública permanente. |
| `mime_type` | Tipo do arquivo. |
| `size_bytes` | Tamanho. |
| `checksum` | Integridade e deduplicação. |
| `encryption_state` | Estado de criptografia. |
| `sync_state` | `LOCAL_ONLY`, `PENDING`, `SYNCED`, `FAILED`, `CONFLICT`. |
| `created_at` / `deleted_at` | Ciclo de vida. |

---

## 6. Fluxo de evidência

```
Captura
  ↓
arquivo local temporário
  ↓
compressão e remoção de metadados desnecessários
  ↓
checksum
  ↓
StorageProvider selecionado
  ↓
storage_object no D1
  ↓
evidence vinculada à visita
  ↓
laudo usa referência controlada
```

---

## 7. Segurança

- Credenciais nunca ficam no D1 nem em logs.
- Tokens locais usam armazenamento seguro do sistema.
- URLs públicas permanentes devem ser evitadas; preferir acesso autenticado ou temporário.
- Fotos devem remover EXIF desnecessário, especialmente localização.
- Cada arquivo possui `checksum` e `owner_id`.
- Exclusão deve alcançar metadado e objeto, registrando falhas de limpeza.

---

## 8. Offline e conflitos

- Captura nunca depende de internet.
- Uploads entram em fila persistente com backoff.
- O mesmo checksum evita duplicação acidental.
- Conflitos não sobrescrevem silenciosamente; geram nova versão ou exigem decisão.
- O usuário vê estado de sincronização por visita e por arquivo.

---

## 9. Planos

| Plano | Armazenamento |
|---|---|
| Free | Local e pasta própria via SAF; limites funcionais do plano. |
| Pro | Local, SAF e conectores próprios quando disponíveis. |
| Pro Hosted / Business | Cota hospedada pela SignallQ (R2), cobrada conforme plano. |

Preço dos planos é **decisão pendente** (Canônico §8.1).

---

## 10. Portabilidade

- Exportar pacote com banco lógico, PDFs e manifesto de arquivos.
- Trocar provider sem quebrar referências de domínio.
- Job de migração copia, valida checksum e só depois altera o provider ativo.
- O técnico continua dono dos seus dados e pode desconectar a nuvem.

---

## Documentos relacionados

- `00_CANONICO_v5.md` — decisão de storage §6, glossário §2, pendências §8.
- `04_SignallQ_Modelo_Dados_D1_v5.md` — tabela `storage_object` e domínio de evidência.
- `05_SignallQ_Telemetria_Analytics_v5.md` — eventos `storage.*` / `upload.*` / `sync.conflict`.
- `07_SignallQ_Admin_Especificacao_v5.md` — visão de volume de arquivos por StorageProvider.
