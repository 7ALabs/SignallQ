package io.signallq.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PreferenciasAppRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FeatureFlagStore {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "linkaPreferencias")

    private val credenciaisModem = CredenciaisModemStore(context)

    private val chaveMonitoramentoAtivo = booleanPreferencesKey("monitoramentoAtivo")
    private val chaveModemHost = stringPreferencesKey("modemHost")
    private val chaveModemUsername = stringPreferencesKey("modemUsername")
    private val chaveModemPassword = stringPreferencesKey("modemPassword")
    private val chaveModemPermanecerConectado = booleanPreferencesKey("modemPermanecerConectado")

    // Legado (GH#530) — chave plaintext onde o BSSID vinculado ficava antes do GH#527 mover
    // esse dado para o CredenciaisModemStore criptografado. Mantida so para migracao unica.
    private val chaveGatewaySessionBssidLegado = stringPreferencesKey("gatewaySessionBssid")
    private val chaveTemaSelecionado = stringPreferencesKey("temaSelecionado")
    private val chaveAnaliseAvancada = booleanPreferencesKey("analiseAvancada")
    private val chaveNomeUsuario = stringPreferencesKey("nomeUsuario")
    private val chaveFotoUriUsuario = stringPreferencesKey("fotoUriUsuario")
    private val chaveOperadora = stringPreferencesKey("operadora")
    private val chavePlanoInternet = stringPreferencesKey("planoInternet")
    private val chaveRegiao = stringPreferencesKey("regiao")
    private val chaveLimiteAlertaMbps = intPreferencesKey("limiteAlertaMbps")
    private val chaveUltimaVerificacaoMonitoramento = longPreferencesKey("ultimaVerificacaoMonitoramento")

    private val chaveIspConfirmado = booleanPreferencesKey("ispConfirmado")
    private val chaveOperadoraMovel = stringPreferencesKey("operadoraMovel")
    private val chaveEstadoUf = stringPreferencesKey("estadoUf")
    private val chaveCidadeNome = stringPreferencesKey("cidadeNome")
    private val chaveUltimaVersaoVista = stringPreferencesKey("ultimaVersaoVista")

    private val ONBOARDING_CONCLUIDO = booleanPreferencesKey("onboarding_concluido")
    private val chaveConsentimentoLgpd = booleanPreferencesKey("consentimento_lgpd")
    private val chaveAnatelBannerDismissed = booleanPreferencesKey("anatelBannerDismissed")

    // Velocidade contratada — MinhaConexaoScreen / Laudo (#85)
    private val chaveVelocidadeContratadaDownMbps = intPreferencesKey("velocidadeContratadaDownMbps")
    private val chaveVelocidadeContratadaUpMbps = intPreferencesKey("velocidadeContratadaUpMbps")

    // Speedtest em rede medida (móvel)
    private val chaveSpeedtestPermiteHeavyMovel = booleanPreferencesKey("speedtest_permite_heavy_movel")
    private val chaveSpeedtestMbConsumidosMes = longPreferencesKey("speedtest_mb_consumidos_mes")
    private val chaveSpeedtestMesReferencia = stringPreferencesKey("speedtest_mes_referencia")

    // Histerese de alertas — rastreiam se cada tipo de alerta está ativo
    private val chaveAlertaLatenciaAtivo = booleanPreferencesKey("alerta_latencia_ativo")
    private val chaveAlertaDnsAtivo = booleanPreferencesKey("alerta_dns_ativo")
    private val chaveAlertaRssiAtivo = booleanPreferencesKey("alerta_rssi_ativo")
    private val chaveAlertaSemInternetAtivo = booleanPreferencesKey("alerta_sem_internet_ativo")

    // Controles granulares do usuário — habilitam/desabilitam cada tipo de notificação
    private val chaveNotificacaoLatenciaAtiva = booleanPreferencesKey("notificacao_latencia_ativa")
    private val chaveNotificacaoDnsAtiva = booleanPreferencesKey("notificacao_dns_ativa")
    private val chaveNotificacaoRssiAtiva = booleanPreferencesKey("notificacao_rssi_ativa")
    private val chaveNotificacaoSemInternetAtiva = booleanPreferencesKey("notificacao_sem_internet_ativa")

    /**
     * Conjunto de identidades de dispositivos já vistos na rede, serializado como String CSV.
     * Usado para detectar dispositivos novos sem MAC (fallback ip+nome) sem poluir a tabela Room.
     * Formato de cada entrada: "mac:<MAC>" ou "ipnome:<IP>:<nomeNormalizado>".
     */
    private val chaveDispositivosConhecidos = stringPreferencesKey("dispositivos_conhecidos_set")

    // Feature flags remotas — JSON serializado: {"ai_diagnosis_enabled":true,...}
    private val chaveFeatureFlagsJson = stringPreferencesKey("feature_flags_json")

    // Sync retroativo para admin worker — checkpoint de progresso por tipo
    private val chaveAdminSyncMedicaoLastEpochMs = longPreferencesKey("admin_sync_medicao_last_epoch_ms")
    private val chaveAdminSyncChatLastEpochMs = longPreferencesKey("admin_sync_chat_last_epoch_ms")

    // Identificador anonimo permanente do dispositivo — UUID gerado na primeira execucao, sem PII
    private val chaveAnonDeviceId = stringPreferencesKey("anon_device_id")

    /**
     * Deve ser chamada uma vez após construção (idealmente no provide do Hilt).
     * Lê credenciais plaintext do DataStore e migra para o store criptografado.
     */
    suspend fun migrarCredenciaisSeNecessario() {
        withContext(ioDispatcher) {
            val prefs = context.dataStore.data.first()
            val userPlain = prefs[chaveModemUsername]
            val passPlain = prefs[chaveModemPassword]

            val migrou = credenciaisModem.migrarSeNecessario(userPlain, passPlain)
            if (migrou) {
                context.dataStore.edit {
                    it.remove(chaveModemUsername)
                    it.remove(chaveModemPassword)
                }
            }

            // GH#527 — migra o BSSID vinculado (GH#530) da chave plaintext legada para o
            // store criptografado, uma unica vez.
            val bssidLegado = prefs[chaveGatewaySessionBssidLegado]
            if (bssidLegado != null) {
                credenciaisModem.salvarBssidVinculado(bssidLegado)
                context.dataStore.edit { it.remove(chaveGatewaySessionBssidLegado) }
            }
        }
    }

    val monitoramentoAtivoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveMonitoramentoAtivo] ?: false }

    val modemHostFlow: Flow<String?> =
        context.dataStore.data.map { it[chaveModemHost] }

    val modemUsernameFlow: Flow<String> get() = credenciaisModem.usernameFlow

    val modemPasswordFlow: Flow<String> get() = credenciaisModem.passwordFlow

    val modemPermanecerConectadoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveModemPermanecerConectado] ?: false }

    // GH#527 — BSSID vinculado a credencial "manter conectado", agora persistido junto
    // dela no store criptografado (nunca em plaintext no DataStore comum).
    val gatewaySessionBssidFlow: Flow<String?> get() = credenciaisModem.bssidVinculadoFlow

    val temaSelecionadoFlow: Flow<String> =
        context.dataStore.data.map { it[chaveTemaSelecionado] ?: "sistema" }

    val analiseAvancadaFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveAnaliseAvancada] ?: false }

    val nomeUsuarioFlow: Flow<String> =
        context.dataStore.data.map { it[chaveNomeUsuario] ?: "" }

    val fotoUriUsuarioFlow: Flow<String?> =
        context.dataStore.data.map { it[chaveFotoUriUsuario] }

    val operadoraFlow: Flow<String> =
        context.dataStore.data.map { it[chaveOperadora] ?: "" }

    val planoInternetFlow: Flow<String> =
        context.dataStore.data.map { it[chavePlanoInternet] ?: "" }

    val regiaoFlow: Flow<String> =
        context.dataStore.data.map { it[chaveRegiao] ?: "" }

    val limiteAlertaMbpsFlow: Flow<Int> =
        context.dataStore.data.map { it[chaveLimiteAlertaMbps] ?: 0 }

    val ultimaVerificacaoMonitoramentoFlow: Flow<Long?> =
        context.dataStore.data.map { it[chaveUltimaVerificacaoMonitoramento] }

    val ispConfirmadoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveIspConfirmado] ?: false }

    val operadoraMovelFlow: Flow<String> =
        context.dataStore.data.map { it[chaveOperadoraMovel] ?: "" }

    val estadoUfFlow: Flow<String> =
        context.dataStore.data.map { it[chaveEstadoUf] ?: "" }

    val cidadeNomeFlow: Flow<String> =
        context.dataStore.data.map { it[chaveCidadeNome] ?: "" }

    val ultimaVersaoVistaFlow: Flow<String> =
        context.dataStore.data.map { it[chaveUltimaVersaoVista] ?: "" }

    val onboardingConcluidoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDING_CONCLUIDO] ?: false }

    val anatelBannerDismissedFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveAnatelBannerDismissed] ?: false }

    // null = nao respondido, true = aceito, false = recusado
    val consentimentoLgpdFlow: Flow<Boolean?> =
        context.dataStore.data.map { it[chaveConsentimentoLgpd] }

    val velocidadeContratadaDownMbpsFlow: Flow<Int> =
        context.dataStore.data.map { it[chaveVelocidadeContratadaDownMbps] ?: 0 }

    val velocidadeContratadaUpMbpsFlow: Flow<Int> =
        context.dataStore.data.map { it[chaveVelocidadeContratadaUpMbps] ?: 0 }


    // Speedtest em rede medida (móvel)
    val speedtestPermiteHeavyMovel: Flow<Boolean> =
        context.dataStore.data.map { it[chaveSpeedtestPermiteHeavyMovel] ?: false }

    val speedtestMbConsumidosMes: Flow<Long> =
        context.dataStore.data.map { it[chaveSpeedtestMbConsumidosMes] ?: 0L }

    val speedtestMesReferencia: Flow<String> =
        context.dataStore.data.map { it[chaveSpeedtestMesReferencia] ?: "" }

    // Flows de histerese
    val alertaLatenciaAtivoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveAlertaLatenciaAtivo] ?: false }

    val alertaDnsAtivoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveAlertaDnsAtivo] ?: false }

    val alertaRssiAtivoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveAlertaRssiAtivo] ?: false }

    val alertaSemInternetAtivoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveAlertaSemInternetAtivo] ?: false }

    /** Conjunto persistido de identidades de dispositivos já vistos (leitura pontual, não flow). */
    suspend fun buscarDispositivosConhecidos(): Set<String> =
        withContext(ioDispatcher) {
            val csv = context.dataStore.data.first()[chaveDispositivosConhecidos] ?: ""
            if (csv.isBlank()) emptySet() else csv.split(",").filter { it.isNotBlank() }.toSet()
        }

    /** Persiste o conjunto atualizado de identidades conhecidas. */
    suspend fun salvarDispositivosConhecidos(identidades: Set<String>) {
        withContext(ioDispatcher) {
            context.dataStore.edit { it[chaveDispositivosConhecidos] = identidades.joinToString(",") }
        }
    }

    // Flows de controles granulares do usuário (default: ativo)
    val notificacaoLatenciaAtivaFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveNotificacaoLatenciaAtiva] ?: true }

    val notificacaoDnsAtivaFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveNotificacaoDnsAtiva] ?: true }

    val notificacaoRssiAtivaFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveNotificacaoRssiAtiva] ?: true }

    val notificacaoSemInternetAtivaFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveNotificacaoSemInternetAtiva] ?: true }

    suspend fun definirMonitoramentoAtivo(ativo: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveMonitoramentoAtivo] = ativo } }
    }

    suspend fun definirModemHost(host: String?) {
        withContext(ioDispatcher) {
            context.dataStore.edit { prefs ->
                if (host != null) prefs[chaveModemHost] = host
                else prefs.remove(chaveModemHost)
            }
        }
    }

    suspend fun definirModemUsername(username: String) {
        withContext(ioDispatcher) { credenciaisModem.salvarUsername(username) }
    }

    suspend fun definirModemPassword(password: String) {
        withContext(ioDispatcher) { credenciaisModem.salvarPassword(password) }
    }

    suspend fun definirModemPermanecerConectado(permanecer: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveModemPermanecerConectado] = permanecer } }
    }

    /** BSSID em que a sessao "manter conectado" do gateway foi estabelecida. Null limpa. */
    suspend fun definirGatewaySessionBssid(bssid: String?) {
        withContext(ioDispatcher) { credenciaisModem.salvarBssidVinculado(bssid) }
    }

    suspend fun definirTemaSelecionado(tema: String) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveTemaSelecionado] = tema } }
    }

    suspend fun definirAnaliseAvancada(ativa: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveAnaliseAvancada] = ativa } }
    }

    suspend fun definirNomeUsuario(nome: String) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveNomeUsuario] = nome } }
    }

    suspend fun definirFotoUriUsuario(uri: String?) {
        withContext(ioDispatcher) {
            context.dataStore.edit { prefs ->
                if (uri != null) prefs[chaveFotoUriUsuario] = uri
                else prefs.remove(chaveFotoUriUsuario)
            }
        }
    }

    suspend fun definirOperadora(operadora: String) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveOperadora] = operadora } }
    }

    suspend fun definirPlanoInternet(plano: String) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chavePlanoInternet] = plano } }
    }

    suspend fun definirRegiao(regiao: String) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveRegiao] = regiao } }
    }

    suspend fun definirLimiteAlertaMbps(limite: Int) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveLimiteAlertaMbps] = limite } }
    }

    suspend fun definirUltimaVerificacaoMonitoramento(timestamp: Long) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveUltimaVerificacaoMonitoramento] = timestamp } }
    }

    suspend fun definirIspConfirmado(confirmado: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveIspConfirmado] = confirmado } }
    }

    suspend fun definirOperadoraMovel(operadora: String) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveOperadoraMovel] = operadora } }
    }

    suspend fun definirEstadoUf(uf: String) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveEstadoUf] = uf } }
    }

    suspend fun definirCidadeNome(cidade: String) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveCidadeNome] = cidade } }
    }

    suspend fun definirUltimaVersaoVista(versao: String) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveUltimaVersaoVista] = versao } }
    }

    suspend fun definirOnboardingConcluido(concluido: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[ONBOARDING_CONCLUIDO] = concluido } }
    }

    suspend fun definirAnatelBannerDismissed(dismissed: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveAnatelBannerDismissed] = dismissed } }
    }

    suspend fun definirConsentimentoLgpd(aceito: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveConsentimentoLgpd] = aceito } }
    }

    suspend fun buscarConsentimentoLgpd(): Boolean? =
        withContext(ioDispatcher) { context.dataStore.data.first()[chaveConsentimentoLgpd] }

    suspend fun definirVelocidadeContratadaDownMbps(mbps: Int) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveVelocidadeContratadaDownMbps] = mbps } }
    }

    suspend fun definirVelocidadeContratadaUpMbps(mbps: Int) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveVelocidadeContratadaUpMbps] = mbps } }
    }


    // Setters de speedtest em rede medida (móvel)
    suspend fun setSpeedtestPermiteHeavyMovel(value: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveSpeedtestPermiteHeavyMovel] = value } }
    }

    suspend fun setSpeedtestMbConsumidosMes(value: Long) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveSpeedtestMbConsumidosMes] = value } }
    }

    suspend fun setSpeedtestMesReferencia(value: String) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveSpeedtestMesReferencia] = value } }
    }

    // Setters de controles granulares
    suspend fun definirNotificacaoLatenciaAtiva(ativa: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveNotificacaoLatenciaAtiva] = ativa } }
    }

    suspend fun definirNotificacaoDnsAtiva(ativa: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveNotificacaoDnsAtiva] = ativa } }
    }

    suspend fun definirNotificacaoRssiAtiva(ativa: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveNotificacaoRssiAtiva] = ativa } }
    }

    suspend fun definirNotificacaoSemInternetAtiva(ativa: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveNotificacaoSemInternetAtiva] = ativa } }
    }

    // Setters de histerese
    suspend fun setAlertaLatenciaAtivo(ativo: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveAlertaLatenciaAtivo] = ativo } }
    }

    suspend fun setAlertaDnsAtivo(ativo: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveAlertaDnsAtivo] = ativo } }
    }

    suspend fun setAlertaRssiAtivo(ativo: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveAlertaRssiAtivo] = ativo } }
    }

    suspend fun setAlertaSemInternetAtivo(ativo: Boolean) {
        withContext(ioDispatcher) { context.dataStore.edit { it[chaveAlertaSemInternetAtivo] = ativo } }
    }

    // --- Sync retroativo admin worker ---

    /** Leitura pontual do checkpoint de sync de medicoes (epoch ms). */
    suspend fun buscarAdminSyncMedicaoLastEpochMs(): Long =
        withContext(ioDispatcher) {
            context.dataStore.data.first()[chaveAdminSyncMedicaoLastEpochMs] ?: 0L
        }

    suspend fun salvarAdminSyncMedicaoLastEpochMs(epochMs: Long) {
        withContext(ioDispatcher) {
            context.dataStore.edit { it[chaveAdminSyncMedicaoLastEpochMs] = epochMs }
        }
    }

    /** Leitura pontual do checkpoint de sync de sessoes de chat (epoch ms). */
    suspend fun buscarAdminSyncChatLastEpochMs(): Long =
        withContext(ioDispatcher) {
            context.dataStore.data.first()[chaveAdminSyncChatLastEpochMs] ?: 0L
        }

    suspend fun salvarAdminSyncChatLastEpochMs(epochMs: Long) {
        withContext(ioDispatcher) {
            context.dataStore.edit { it[chaveAdminSyncChatLastEpochMs] = epochMs }
        }
    }

    /**
     * Retorna o device_id anonimo persistente. Gera e salva um UUID na primeira chamada.
     * Nunca usa ANDROID_ID, IMEI, MAC ou qualquer PII.
     */
    suspend fun buscarOuGerarAnonDeviceId(): String =
        withContext(ioDispatcher) {
            val existente = context.dataStore.data.first()[chaveAnonDeviceId]
            if (!existente.isNullOrBlank()) return@withContext existente
            val novo = UUID.randomUUID().toString()
            context.dataStore.edit { it[chaveAnonDeviceId] = novo }
            novo
        }

    // --- Feature flags remotas ---

    override suspend fun salvarFeatureFlags(flags: Map<String, Boolean>) {
        withContext(ioDispatcher) {
            val json = buildString {
                append("{")
                flags.entries.joinToString(",") { (k, v) -> "\"$k\":$v" }.also { append(it) }
                append("}")
            }
            context.dataStore.edit { it[chaveFeatureFlagsJson] = json }
        }
    }

    override suspend fun buscarFeatureFlags(): Map<String, Boolean> =
        withContext(ioDispatcher) {
            val json = context.dataStore.data.first()[chaveFeatureFlagsJson] ?: return@withContext emptyMap()
            if (json.isBlank()) return@withContext emptyMap<String, Boolean>()
            try {
                val result = mutableMapOf<String, Boolean>()
                json.trim('{', '}').split(",").forEach { pair ->
                    val (k, v) = pair.split(":").map { it.trim().trim('"') }
                    result[k] = v.toBooleanStrict()
                }
                result
            } catch (_: Exception) {
                emptyMap()
            }
        }

    suspend fun limparTodasPreferencias() {
        withContext(ioDispatcher) {
            context.dataStore.edit { it.clear() }
            credenciaisModem.salvarUsername(CredenciaisModemStore.DEFAULT_USERNAME)
            credenciaisModem.salvarPassword(CredenciaisModemStore.DEFAULT_PASSWORD)
            credenciaisModem.salvarBssidVinculado(null)
        }
    }
}
