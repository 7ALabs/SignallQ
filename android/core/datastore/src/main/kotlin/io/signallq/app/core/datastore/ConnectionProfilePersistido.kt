package io.signallq.app.core.datastore

/**
 * GH#1227 item 3/RF-A — representação de persistência de um perfil de conexão por rede
 * (provedor fixo + plano contratado + cidade/UF vinculados a um `networkId` estável, não mais
 * globais). Struct simples e sem dependência de `feature:settings` de propósito: `core:datastore`
 * não pode depender de módulos feature (lei de dependência do repo) — quem converte pro
 * tipo de domínio real (`io.signallq.app.feature.settings.ConnectionProfile`) é o consumidor
 * (`:app`/`feature:settings`, que já depende de `core:datastore`).
 */
data class ConnectionProfilePersistido(
    val networkId: String,
    val providerFixed: String?,
    val contractedDownloadMbps: Int?,
    val contractedUploadMbps: Int?,
    val city: String?,
    val state: String?,
    val userConfirmed: Boolean,
)

// Separadores de controle (US/RS ASCII, nunca digitados por usuário) — evita problema de
// escaping que vírgula/ponto-e-vírgula teriam em nome de cidade/provedor reais.
internal const val SEP_CAMPO_CONNECTION_PROFILE = ""
internal const val SEP_REGISTRO_CONNECTION_PROFILE = ""

/** Função pura, sem dependência de Context/DataStore — testável isoladamente. */
internal fun ConnectionProfilePersistido.serializar(): String =
    listOf(
        networkId,
        providerFixed.orEmpty(),
        contractedDownloadMbps?.toString().orEmpty(),
        contractedUploadMbps?.toString().orEmpty(),
        city.orEmpty(),
        state.orEmpty(),
        userConfirmed.toString(),
    ).joinToString(SEP_CAMPO_CONNECTION_PROFILE)

/** Inverso de [serializar]. Devolve `null` pra registro malformado (nº de campos errado) em
 *  vez de lançar exceção — um registro corrompido não pode derrubar a leitura dos demais. */
internal fun desserializarConnectionProfile(registro: String): ConnectionProfilePersistido? {
    val campos = registro.split(SEP_CAMPO_CONNECTION_PROFILE)
    if (campos.size != 7) return null
    return ConnectionProfilePersistido(
        networkId = campos[0],
        providerFixed = campos[1].ifBlank { null },
        contractedDownloadMbps = campos[2].toIntOrNull(),
        contractedUploadMbps = campos[3].toIntOrNull(),
        city = campos[4].ifBlank { null },
        state = campos[5].ifBlank { null },
        userConfirmed = campos[6].toBoolean(),
    )
}
