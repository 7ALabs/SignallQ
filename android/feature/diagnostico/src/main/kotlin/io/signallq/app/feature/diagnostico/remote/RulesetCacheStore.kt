package io.signallq.app.feature.diagnostico.remote

import timber.log.Timber
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import org.json.JSONObject

/**
 * Ultimo ruleset remoto valido persistido -- nivel `CACHED_LOCAL` do fallback de 3
 * niveis descrito na secao "Fallback local" de #952 (issue #1450).
 *
 * [content] e o JSON bruto (serializado) da ultima resposta valida de
 * `POST /api/diagnostic/evaluate` -- mesmo shape que [RemoteDiagnosticReportMapper]
 * ja sabe mapear, entao o cache reusa o mapper existente, sem contrato paralelo.
 * [rulesetVersion] fica `null` quando o payload nao trouxe o campo (nunca inventado --
 * ver `.claude/rules/higiene-e-padronizacao-repositorio.md`).
 */
data class CachedRuleset(
    val content: String,
    val rulesetVersion: Int?,
    val hash: String,
    val syncedAtMs: Long,
)

/**
 * Persistencia do ultimo ruleset remoto valido (nivel `CACHED_LOCAL`). Contrato
 * deliberadamente pequeno: [load] nunca lanca (corrupcao vira `null`, nunca crash),
 * [save] so substitui o pacote atual depois de validar o conteudo novo por completo
 * (download incompleto/invalido nunca sobrescreve um cache valido existente).
 */
interface RulesetCacheStore {
    fun load(): CachedRuleset?
    fun save(content: String, rulesetVersion: Int?, syncedAtMs: Long): Boolean
}

/** Usado quando nao ha [RulesetCacheStore] real configurado (ex.: construcao de teste
 *  sem Context/Hilt). Degrada para o fallback de 2 niveis anterior -- REMOTE ->
 *  BUNDLED_LOCAL -- nunca quebra por falta de cache. */
object NoOpRulesetCacheStore : RulesetCacheStore {
    override fun load(): CachedRuleset? = null
    override fun save(content: String, rulesetVersion: Int?, syncedAtMs: Long): Boolean = false
}

/**
 * Implementacao em disco (arquivo privado do app, `filesDir` -- nunca `cacheDir`, que o
 * SO pode limpar sob pressao de storage e este dado e usado como fallback de
 * seguranca, nao cache descartavel).
 *
 * Escrita atomica: serializa para um arquivo `.tmp`, valida o conteudo relendo-o do
 * disco, so entao promove o atual para `.previous` (rollback local -- secao "Fallback
 * local" de #952) e move o `.tmp` por cima do arquivo atual via
 * `Files.move(..., REPLACE_EXISTING)`. Nada e sobrescrito antes da validacao completa.
 *
 * Leitura resiliente: se o arquivo atual estiver corrompido/ilegivel, tenta o
 * `.previous` antes de desistir (uso real do rollback, nao so um backup morto).
 */
class FileRulesetCacheStore(private val cacheDir: File) : RulesetCacheStore {

    private val current: File get() = File(cacheDir, CURRENT_FILE_NAME)
    private val previous: File get() = File(cacheDir, PREVIOUS_FILE_NAME)
    private val tmp: File get() = File(cacheDir, TMP_FILE_NAME)

    override fun load(): CachedRuleset? =
        readValid(current) ?: readValid(previous)?.also {
            Timber.w("FileRulesetCacheStore: cache atual invalido, usando .previous (rollback local)")
        }

    override fun save(content: String, rulesetVersion: Int?, syncedAtMs: Long): Boolean {
        return try {
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                Timber.w("FileRulesetCacheStore: nao foi possivel criar diretorio de cache")
                return false
            }

            val hash = sha256(content)
            val wrapper = JSONObject()
                .put("content", content)
                .put("rulesetVersion", rulesetVersion ?: JSONObject.NULL)
                .put("hash", hash)
                .put("syncedAtMs", syncedAtMs)
                .toString()

            tmp.writeText(wrapper, StandardCharsets.UTF_8)

            // Releh e revalida o que foi escrito antes de promover -- nunca confia
            // que a escrita em disco terminou intacta so porque nao lancou excecao.
            val writtenBack = readValid(tmp)
            if (writtenBack == null || writtenBack.hash != hash) {
                Timber.w("FileRulesetCacheStore: escrita em .tmp falhou na revalidacao, cache atual preservado")
                tmp.delete()
                return false
            }

            if (current.exists()) {
                Files.move(current.toPath(), previous.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            Files.move(tmp.toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (t: Throwable) {
            Timber.w(t, "FileRulesetCacheStore: falha ao persistir ruleset remoto, cache atual preservado")
            tmp.delete()
            false
        }
    }

    private fun readValid(file: File): CachedRuleset? {
        if (!file.exists()) return null
        return try {
            val wrapper = JSONObject(file.readText(StandardCharsets.UTF_8))
            val content = wrapper.getString("content")
            val hash = wrapper.getString("hash")
            if (sha256(content) != hash) {
                Timber.w("FileRulesetCacheStore: hash divergente em ${file.name}, cache corrompido")
                return null
            }
            CachedRuleset(
                content = content,
                rulesetVersion = if (wrapper.isNull("rulesetVersion")) null else wrapper.getInt("rulesetVersion"),
                hash = hash,
                syncedAtMs = wrapper.getLong("syncedAtMs"),
            )
        } catch (t: Throwable) {
            Timber.w(t, "FileRulesetCacheStore: falha ao ler/parsear ${file.name}, tratando como corrompido")
            null
        }
    }

    private fun sha256(content: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val CURRENT_FILE_NAME = "diagnostic_ruleset_cache.json"
        const val PREVIOUS_FILE_NAME = "diagnostic_ruleset_cache.previous.json"
        const val TMP_FILE_NAME = "diagnostic_ruleset_cache.tmp.json"
    }
}
