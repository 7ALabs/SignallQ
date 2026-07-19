package io.signallq.pro.feature.auth

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.signallq.app.core.permissions.EstadoPermissao
import io.signallq.app.core.permissions.GerenciadorPermissoesRedeAndroid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

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
 * Permissoes reais do sistema exigidas pelo Pro em campo: localizacao fina (scan Wi-Fi),
 * Wi-Fi proximo (Android 13+, mesmo contrato do [GerenciadorPermissoesRedeAndroid] do
 * consumidor) e camera (evidencias fotograficas, tela 2.12 -- exclusiva do Pro).
 */
@HiltViewModel
class PermissoesViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PermissoesUiState())
        val uiState: StateFlow<PermissoesUiState> = _uiState

        init {
            atualizarEstados()
        }

        fun atualizarEstados() {
            val redeSnapshot = GerenciadorPermissoesRedeAndroid(context).avaliar()
            val cameraConcedida = possuiPermissao(Manifest.permission.CAMERA)

            val itens =
                buildList {
                    add(
                        PermissaoItemUiState(
                            manifestPermission = Manifest.permission.ACCESS_FINE_LOCATION,
                            titulo = "Localizacao",
                            subtitulo = "Necessaria para escanear redes Wi-Fi proximas",
                            concedida = redeSnapshot.localizacaoFina == EstadoPermissao.concedida,
                        ),
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(
                            PermissaoItemUiState(
                                manifestPermission = Manifest.permission.NEARBY_WIFI_DEVICES,
                                titulo = "Dispositivos Wi-Fi proximos",
                                subtitulo = "Necessaria para identificar equipamentos de rede",
                                concedida = redeSnapshot.nearbyWifi == EstadoPermissao.concedida,
                            ),
                        )
                    }
                    add(
                        PermissaoItemUiState(
                            manifestPermission = Manifest.permission.CAMERA,
                            titulo = "Camera",
                            subtitulo = "Necessaria para registrar evidencias fotograficas",
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
