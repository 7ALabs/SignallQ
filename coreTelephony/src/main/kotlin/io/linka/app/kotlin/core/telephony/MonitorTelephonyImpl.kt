package io.linka.app.kotlin.core.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

private const val TAG = "MonitorTelephony"

/**
 * Implementacao Android do MonitorTelephony.
 *
 * Estrategia:
 *  - API 31+ (S): TelephonyManager.registerTelephonyCallback com callbacks
 *    granulares (signalStrengths, cellInfo, serviceState). Recomendado pelo Google.
 *  - API <= 30: fallback para PhoneStateListener.LISTEN_SIGNAL_STRENGTHS |
 *    LISTEN_CELL_INFO | LISTEN_SERVICE_STATE.
 *
 * Tratamento gracioso:
 *  - Sem READ_PHONE_STATE -> emite null e loga warn uma vez.
 *  - Sem SIM ativa / emulador -> emite null silenciosamente.
 *  - SecurityException em qualquer chamada -> capturada, snapshot fica null.
 *
 * NAO chama getDeviceId, getImei, getSubscriberId. NAO solicita ACCESS_*_LOCATION.
 */
class MonitorTelephonyImpl(
    context: Context,
) : MonitorTelephony {

    private val applicationContext = context.applicationContext
    private val telephonyManager = applicationContext
        .getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "MonitorTelephony").apply { isDaemon = true }
    }

    private val mutableSnapshot = MutableStateFlow<MovelSnapshot?>(null)
    override val snapshotFlow: StateFlow<MovelSnapshot?> = mutableSnapshot.asStateFlow()

    @Volatile private var sinalAtual: SignalStrength? = null
    @Volatile private var serviceState: ServiceState? = null
    @Volatile private var iniciou = false

    // Callbacks API 31+
    private var callback31: Any? = null

    // Callback API 30-
    private var listenerLegado: PhoneStateListener? = null

    override fun iniciar() {
        if (iniciou) return
        if (telephonyManager == null) {
            Log.w(TAG, "TelephonyManager indisponivel — emulador ou device sem radio.")
            return
        }
        if (!possuiPermissaoReadPhoneState()) {
            Log.w(TAG, "READ_PHONE_STATE negada — snapshot ficara null. Solicite a permissao na UI.")
            return
        }
        iniciou = true
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                registrarCallback31(telephonyManager)
            } else {
                @Suppress("DEPRECATION")
                registrarPhoneStateListener(telephonyManager)
            }
            // Emite um snapshot inicial baseado no estado corrente.
            recomputar()
        } catch (se: SecurityException) {
            Log.w(TAG, "SecurityException ao registrar telephony callback: ${se.message}")
            iniciou = false
        } catch (t: Throwable) {
            Log.w(TAG, "Falha ao iniciar MonitorTelephony: ${t.message}")
            iniciou = false
        }
    }

    override fun encerrar() {
        if (!iniciou) return
        iniciou = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (callback31 as? TelephonyCallback)?.let {
                    telephonyManager?.unregisterTelephonyCallback(it)
                }
            } else {
                @Suppress("DEPRECATION")
                listenerLegado?.let {
                    telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
                }
            }
        } catch (_: Throwable) {
            // ignora — best-effort
        } finally {
            callback31 = null
            listenerLegado = null
            mutableSnapshot.value = null
        }
    }

    // -------------------------------------------------------------------------
    // Registro de callbacks
    // -------------------------------------------------------------------------

    @android.annotation.TargetApi(Build.VERSION_CODES.S)
    private fun registrarCallback31(tm: TelephonyManager) {
        val cb = object : TelephonyCallback(),
            TelephonyCallback.SignalStrengthsListener,
            TelephonyCallback.ServiceStateListener,
            TelephonyCallback.CellInfoListener {

            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                sinalAtual = signalStrength
                recomputar()
            }

            override fun onServiceStateChanged(state: ServiceState) {
                serviceState = state
                recomputar()
            }

            override fun onCellInfoChanged(cellInfo: MutableList<android.telephony.CellInfo>) {
                recomputar()
            }
        }
        tm.registerTelephonyCallback(executor, cb)
        callback31 = cb
    }

    @Suppress("DEPRECATION")
    private fun registrarPhoneStateListener(tm: TelephonyManager) {
        val listener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                sinalAtual = signalStrength
                recomputar()
            }

            override fun onServiceStateChanged(state: ServiceState) {
                serviceState = state
                recomputar()
            }

            override fun onCellInfoChanged(cellInfo: MutableList<android.telephony.CellInfo>?) {
                recomputar()
            }
        }
        tm.listen(
            listener,
            PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or
                PhoneStateListener.LISTEN_SERVICE_STATE or
                PhoneStateListener.LISTEN_CELL_INFO,
        )
        listenerLegado = listener
    }

    // -------------------------------------------------------------------------
    // Captura unificada do estado
    // -------------------------------------------------------------------------

    private fun recomputar() {
        val tm = telephonyManager ?: return
        try {
            val snap = capturarSnapshot(tm)
            mutableSnapshot.value = snap
        } catch (se: SecurityException) {
            // Permissao revogada em runtime — para de tentar.
            Log.w(TAG, "READ_PHONE_STATE revogada em runtime.")
            mutableSnapshot.value = null
        } catch (t: Throwable) {
            Log.w(TAG, "Falha ao capturar snapshot movel: ${t.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun capturarSnapshot(tm: TelephonyManager): MovelSnapshot? {
        // Se nao tem SIM ativa, simState != READY tipicamente.
        if (tm.simState != TelephonyManager.SIM_STATE_READY) return null

        val operadora = runCatching { tm.networkOperatorName?.takeIf { it.isNotBlank() } }.getOrNull()
        val (mcc, mnc) = parseMccMnc(tm.networkOperator)
        val roaming = runCatching { tm.isNetworkRoaming }.getOrNull()
        val tecnologia = derivarTecnologia(tm)

        // Tenta obter CellInfo (LTE ou NR).
        val cellInfos = runCatching { tm.allCellInfo }.getOrNull().orEmpty()
        val celulaServidora = cellInfos.firstOrNull { it.isRegistered }

        var rsrpDbm: Int? = null
        var rsrqDb: Int? = null
        var sinrDb: Int? = null
        var bandaMovel: String? = null
        var cellId: Long? = null
        var tac: Int? = null
        var earfcn: Int? = null

        when (celulaServidora) {
            is CellInfoLte -> {
                val s: CellSignalStrengthLte = celulaServidora.cellSignalStrength
                val id: CellIdentityLte = celulaServidora.cellIdentity
                rsrpDbm = s.rsrp.takeIf { it != Int.MAX_VALUE }
                rsrqDb = s.rsrq.takeIf { it != Int.MAX_VALUE }
                sinrDb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    s.rssnr.takeIf { it != Int.MAX_VALUE }
                } else null
                cellId = id.ci.takeIf { it != Int.MAX_VALUE }?.toLong()
                tac = id.tac.takeIf { it != Int.MAX_VALUE }
                earfcn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) id.earfcn else null
                bandaMovel = bandaLteFromEarfcn(earfcn)
            }
            is CellInfoNr -> {
                val s = celulaServidora.cellSignalStrength as? CellSignalStrengthNr
                val id = celulaServidora.cellIdentity as? CellIdentityNr
                rsrpDbm = s?.csiRsrp?.takeIf { it != Int.MAX_VALUE }
                    ?: s?.ssRsrp?.takeIf { it != Int.MAX_VALUE }
                rsrqDb = s?.csiRsrq?.takeIf { it != Int.MAX_VALUE }
                    ?: s?.ssRsrq?.takeIf { it != Int.MAX_VALUE }
                sinrDb = s?.csiSinr?.takeIf { it != Int.MAX_VALUE }
                    ?: s?.ssSinr?.takeIf { it != Int.MAX_VALUE }
                cellId = id?.nci?.takeIf { it != Long.MAX_VALUE }
                tac = id?.tac?.takeIf { it != Int.MAX_VALUE }
                earfcn = id?.nrarfcn?.takeIf { it != Int.MAX_VALUE }
                bandaMovel = bandaNrFromArfcn(earfcn) ?: id?.bands?.firstOrNull()?.let { "n$it" }
            }
            else -> { /* Outras tecnologias (CDMA/GSM) — sem RSRP. */ }
        }

        // derivarTecnologia usa serviceState.toString() para detectar 5G NSA, o que falha
        // em vários OEMs. Se allCellInfo já encontrou um CellInfoNr registrado, isso é
        // evidência direta de NR ativo — forçar "5G NSA" quando derivarTecnologia ficou
        // em "4G" ou null por não conseguir ler nrState.
        val tecnologiaFinal = when {
            celulaServidora is CellInfoNr && (tecnologia == "4G" || tecnologia == null) -> "5G NSA"
            else -> tecnologia
        }

        // Se nao conseguiu absolutamente nada significativo, devolve null
        // pra evitar payload com so operadora.
        val temAlgo = rsrpDbm != null || sinrDb != null || cellId != null ||
            mcc != null || tecnologiaFinal != null
        if (!temAlgo) return null

        return MovelSnapshot(
            operadora = operadora,
            tecnologia = tecnologiaFinal,
            rsrpDbm = rsrpDbm,
            rsrqDb = rsrqDb,
            sinrDb = sinrDb,
            ecnoDb = null,
            bandaMovel = bandaMovel,
            cellId = cellId,
            mcc = mcc,
            mnc = mnc,
            tac = tac,
            roaming = roaming,
            timestampMs = System.currentTimeMillis(),
        )
    }

    private fun derivarTecnologia(tm: TelephonyManager): String? {
        val nrAtivo = serviceState?.let { detectarNrAtivo(it) } ?: false

        val dataNetType = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                @Suppress("MissingPermission") tm.dataNetworkType
            } else {
                @Suppress("DEPRECATION") tm.networkType
            }
        }.getOrNull() ?: return null

        return when (dataNetType) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G SA"
            TelephonyManager.NETWORK_TYPE_LTE -> if (nrAtivo) "5G NSA" else "4G"
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G"
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
            else -> null
        }
    }

    /**
     * Detecta 5G NSA (LTE com canal NR secundário ativo).
     *
     * Abordagem 1 (API 30+): NetworkRegistrationInfo.getNrState() via reflexão.
     *   getNrState() é @SystemApi — não acessível diretamente, mas presente em runtime.
     *   Valor 3 = NR_STATE_CONNECTED (constante documentada na AOSP).
     * Abordagem 2: ServiceState.toString() — fallback cross-OEM para Android 9+.
     */
    private fun detectarNrAtivo(ss: ServiceState): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                @Suppress("NewApi")
                val regList = ss.networkRegistrationInfoList
                val nrConnected = regList.any { reg ->
                    runCatching {
                        reg.javaClass.getMethod("getNrState").invoke(reg) as? Int == 3
                    }.getOrDefault(false)
                }
                if (nrConnected) return true
            } catch (_: Throwable) {
                // Fallback abaixo
            }
        }
        return runCatching {
            val str = ss.toString()
            str.contains("nrState=CONNECTED", ignoreCase = true) ||
                str.contains("isNrAvailable=true", ignoreCase = true)
        }.getOrDefault(false)
    }

    private fun parseMccMnc(networkOperator: String?): Pair<String?, String?> {
        if (networkOperator.isNullOrBlank() || networkOperator.length < 5) return null to null
        val mcc = networkOperator.substring(0, 3)
        val mnc = networkOperator.substring(3)
        return mcc to mnc
    }

    /**
     * Aproximacao da banda LTE a partir do EARFCN (Downlink).
     * Faixas oficiais 3GPP TS 36.101. Cobertura focada nas bandas usadas no Brasil:
     * B1, B2, B3, B4, B5, B7, B8, B17, B28, B38, B40, B41, B66.
     */
    private fun bandaLteFromEarfcn(earfcn: Int?): String? {
        if (earfcn == null) return null
        return when (earfcn) {
            in 0..599 -> "B1 (2100 MHz)"
            in 600..1199 -> "B2 (1900 MHz)"
            in 1200..1949 -> "B3 (1800 MHz)"
            in 1950..2399 -> "B4 (1700/2100 MHz AWS)"
            in 2400..2649 -> "B5 (850 MHz)"
            in 2750..3449 -> "B7 (2600 MHz)"
            in 3450..3799 -> "B8 (900 MHz)"
            in 5730..5849 -> "B17 (700 MHz)"
            in 9210..9659 -> "B28 (700 MHz APT)"
            in 37750..38249 -> "B38 (TDD 2600)"
            in 38650..39649 -> "B40 (TDD 2300)"
            in 39650..41589 -> "B41 (TDD 2500)"
            in 66436..67335 -> "B66 (1700/2100 MHz Ext)"
            else -> "EARFCN $earfcn"
        }
    }

    /** Aproximacao NR a partir do NRARFCN. Cobertura mais comum no Brasil: n78 (3.5 GHz). */
    private fun bandaNrFromArfcn(nrarfcn: Int?): String? {
        if (nrarfcn == null) return null
        return when (nrarfcn) {
            in 620000..680000 -> "n78 (3.5 GHz)"
            in 422000..440000 -> "n1 (2100 MHz)"
            in 386000..398000 -> "n3 (1800 MHz)"
            in 173800..178800 -> "n28 (700 MHz APT)"
            in 693334..733333 -> "n77 (3.7 GHz)"
            else -> "NRARFCN $nrarfcn"
        }
    }

    private fun possuiPermissaoReadPhoneState(): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
