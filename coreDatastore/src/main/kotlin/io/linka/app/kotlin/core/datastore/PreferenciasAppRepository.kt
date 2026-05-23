package io.linka.app.kotlin.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferenciasAppRepository(
    private val context: Context,
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "linkaPreferencias")

    private val chaveMonitoramentoAtivo = booleanPreferencesKey("monitoramentoAtivo")
    private val chaveModemHost = stringPreferencesKey("modemHost")
    private val chaveModemUsername = stringPreferencesKey("modemUsername")
    private val chaveModemPassword = stringPreferencesKey("modemPassword")
    private val chaveModemPermanecerConectado = booleanPreferencesKey("modemPermanecerConectado")
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

    val monitoramentoAtivoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveMonitoramentoAtivo] ?: false }

    val modemHostFlow: Flow<String?> =
        context.dataStore.data.map { it[chaveModemHost] }

    val modemUsernameFlow: Flow<String> =
        context.dataStore.data.map { it[chaveModemUsername] ?: "userAdmin" }

    val modemPasswordFlow: Flow<String> =
        context.dataStore.data.map { it[chaveModemPassword] ?: "" }

    val modemPermanecerConectadoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveModemPermanecerConectado] ?: false }

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

    // Flows de histerese
    val alertaLatenciaAtivoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveAlertaLatenciaAtivo] ?: false }

    val alertaDnsAtivoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveAlertaDnsAtivo] ?: false }

    val alertaRssiAtivoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveAlertaRssiAtivo] ?: false }

    val alertaSemInternetAtivoFlow: Flow<Boolean> =
        context.dataStore.data.map { it[chaveAlertaSemInternetAtivo] ?: false }

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
        context.dataStore.edit { it[chaveMonitoramentoAtivo] = ativo }
    }

    suspend fun definirModemHost(host: String?) {
        context.dataStore.edit { prefs ->
            if (host != null) prefs[chaveModemHost] = host
            else prefs.remove(chaveModemHost)
        }
    }

    suspend fun definirModemUsername(username: String) {
        context.dataStore.edit { it[chaveModemUsername] = username }
    }

    suspend fun definirModemPassword(password: String) {
        context.dataStore.edit { it[chaveModemPassword] = password }
    }

    suspend fun definirModemPermanecerConectado(permanecer: Boolean) {
        context.dataStore.edit { it[chaveModemPermanecerConectado] = permanecer }
    }

    suspend fun definirTemaSelecionado(tema: String) {
        context.dataStore.edit { it[chaveTemaSelecionado] = tema }
    }

    suspend fun definirAnaliseAvancada(ativa: Boolean) {
        context.dataStore.edit { it[chaveAnaliseAvancada] = ativa }
    }

    suspend fun definirNomeUsuario(nome: String) {
        context.dataStore.edit { it[chaveNomeUsuario] = nome }
    }

    suspend fun definirFotoUriUsuario(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri != null) prefs[chaveFotoUriUsuario] = uri
            else prefs.remove(chaveFotoUriUsuario)
        }
    }

    suspend fun definirOperadora(operadora: String) {
        context.dataStore.edit { it[chaveOperadora] = operadora }
    }

    suspend fun definirPlanoInternet(plano: String) {
        context.dataStore.edit { it[chavePlanoInternet] = plano }
    }

    suspend fun definirRegiao(regiao: String) {
        context.dataStore.edit { it[chaveRegiao] = regiao }
    }

    suspend fun definirLimiteAlertaMbps(limite: Int) {
        context.dataStore.edit { it[chaveLimiteAlertaMbps] = limite }
    }

    suspend fun definirUltimaVerificacaoMonitoramento(timestamp: Long) {
        context.dataStore.edit { it[chaveUltimaVerificacaoMonitoramento] = timestamp }
    }

    suspend fun definirIspConfirmado(confirmado: Boolean) {
        context.dataStore.edit { it[chaveIspConfirmado] = confirmado }
    }

    suspend fun definirOperadoraMovel(operadora: String) {
        context.dataStore.edit { it[chaveOperadoraMovel] = operadora }
    }

    suspend fun definirEstadoUf(uf: String) {
        context.dataStore.edit { it[chaveEstadoUf] = uf }
    }

    suspend fun definirCidadeNome(cidade: String) {
        context.dataStore.edit { it[chaveCidadeNome] = cidade }
    }

    suspend fun definirUltimaVersaoVista(versao: String) {
        context.dataStore.edit { it[chaveUltimaVersaoVista] = versao }
    }

    suspend fun definirOnboardingConcluido(concluido: Boolean) {
        context.dataStore.edit { it[ONBOARDING_CONCLUIDO] = concluido }
    }

    // Setters de controles granulares
    suspend fun definirNotificacaoLatenciaAtiva(ativa: Boolean) {
        context.dataStore.edit { it[chaveNotificacaoLatenciaAtiva] = ativa }
    }

    suspend fun definirNotificacaoDnsAtiva(ativa: Boolean) {
        context.dataStore.edit { it[chaveNotificacaoDnsAtiva] = ativa }
    }

    suspend fun definirNotificacaoRssiAtiva(ativa: Boolean) {
        context.dataStore.edit { it[chaveNotificacaoRssiAtiva] = ativa }
    }

    suspend fun definirNotificacaoSemInternetAtiva(ativa: Boolean) {
        context.dataStore.edit { it[chaveNotificacaoSemInternetAtiva] = ativa }
    }

    // Setters de histerese
    suspend fun setAlertaLatenciaAtivo(ativo: Boolean) {
        context.dataStore.edit { it[chaveAlertaLatenciaAtivo] = ativo }
    }

    suspend fun setAlertaDnsAtivo(ativo: Boolean) {
        context.dataStore.edit { it[chaveAlertaDnsAtivo] = ativo }
    }

    suspend fun setAlertaRssiAtivo(ativo: Boolean) {
        context.dataStore.edit { it[chaveAlertaRssiAtivo] = ativo }
    }

    suspend fun setAlertaSemInternetAtivo(ativo: Boolean) {
        context.dataStore.edit { it[chaveAlertaSemInternetAtivo] = ativo }
    }

    suspend fun limparTodasPreferencias() {
        context.dataStore.edit { it.clear() }
    }
}

