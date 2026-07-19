package io.signallq.pro.feature.auth

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.signallq.app.core.permissions.EstadoPermissao
import io.signallq.app.core.permissions.GerenciadorPermissoesRedeAndroid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val PREFS_PERMISSOES_NOME = "signallq_pro_permissoes"

data class PermissaoItemUiState(
    val manifestPermission: String,
    val titulo: String,
    val subtitulo: String,
    val concedida: Boolean,
)

data class PermissoesUiState(
    val itens: List<PermissaoItemUiState> = emptyList(),
    val todasConcedidas: Boolean = false,
) {
    val algumaBloqueadaPermanentemente: List<PermissaoItemUiState>
        get() = itens.filter { !it.concedida }
}

/**
 * Permissões reais do sistema exigidas pelo Pro em campo: localização fina (scan Wi-Fi),
 * Wi-Fi próximo (Android 13+, mesmo contrato do [GerenciadorPermissoesRedeAndroid] do
 * consumidor) e câmera (evidências fotográficas, tela 2.12 -- exclusiva do Pro).
 */
@HiltViewModel
class PermissoesViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PermissoesUiState())
        val uiState: StateFlow<PermissoesUiState> = _uiState

        // #1179 -- shouldShowRequestPermissionRationale() retorna false tanto para "nunca
        // pedida" quanto para "negada permanentemente", entao precisa de estado proprio pra
        // distinguir os dois casos (mesmo problema e solucao ja aplicados no app consumidor,
        // ver chaveTelefoniaPermissaoJaSolicitada em PreferenciasAppRepository). SharedPreferences
        // simples aqui porque o Pro ainda nao tem modulo :pro:core:datastore.
        private val preferenciasPermissoes =
            context.getSharedPreferences(PREFS_PERMISSOES_NOME, Context.MODE_PRIVATE)

        init {
            atualizarEstados()
        }

        fun permissaoJaSolicitada(manifestPermission: String): Boolean =
            preferenciasPermissoes.getBoolean(chaveJaSolicitada(manifestPermission), false)

        fun marcarPermissaoSolicitada(manifestPermission: String) {
            preferenciasPermissoes.edit { putBoolean(chaveJaSolicitada(manifestPermission), true) }
        }

        private fun chaveJaSolicitada(manifestPermission: String) = "ja_solicitada_$manifestPermission"

        fun atualizarEstados() {
            val redeSnapshot = GerenciadorPermissoesRedeAndroid(context).avaliar()
            val cameraConcedida = possuiPermissao(Manifest.permission.CAMERA)

            val itens =
                buildList {
                    add(
                        PermissaoItemUiState(
                            manifestPermission = Manifest.permission.ACCESS_FINE_LOCATION,
                            titulo = "Localização",
                            subtitulo = "Necessária para escanear redes Wi-Fi próximas",
                            concedida = redeSnapshot.localizacaoFina == EstadoPermissao.concedida,
                        ),
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(
                            PermissaoItemUiState(
                                manifestPermission = Manifest.permission.NEARBY_WIFI_DEVICES,
                                titulo = "Dispositivos Wi-Fi próximos",
                                subtitulo = "Necessária para identificar equipamentos de rede",
                                concedida = redeSnapshot.nearbyWifi == EstadoPermissao.concedida,
                            ),
                        )
                    }
                    add(
                        PermissaoItemUiState(
                            manifestPermission = Manifest.permission.CAMERA,
                            titulo = "Câmera",
                            subtitulo = "Necessária para registrar evidências fotográficas",
                            concedida = cameraConcedida,
                        ),
                    )
                }
            _uiState.update {
                it.copy(itens = itens, todasConcedidas = itens.all { item -> item.concedida })
            }
        }

        private fun possuiPermissao(permissao: String): Boolean =
            ContextCompat.checkSelfPermission(context, permissao) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
