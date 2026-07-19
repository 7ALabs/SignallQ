package io.signallq.pro.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.signallq.app.core.permissions.GerenciadorPermissoesRedeAndroid
import io.signallq.pro.core.database.profissional.ProfissionalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DestinoPosCarregamento { APRESENTACAO, PERMISSOES, PRONTO }

@HiltViewModel
class CarregamentoViewModel
    @Inject
    constructor(
        private val profissionalRepository: ProfissionalRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _destino = MutableStateFlow<DestinoPosCarregamento?>(null)
        val destino: StateFlow<DestinoPosCarregamento?> = _destino

        init {
            viewModelScope.launch {
                val perfil = profissionalRepository.buscarPerfil()
                _destino.value =
                    when {
                        perfil == null -> DestinoPosCarregamento.APRESENTACAO
                        !permissoesEssenciaisConcedidas() -> DestinoPosCarregamento.PERMISSOES
                        else -> DestinoPosCarregamento.PRONTO
                    }
            }
        }

        private fun permissoesEssenciaisConcedidas(): Boolean {
            val snapshot = GerenciadorPermissoesRedeAndroid(context).avaliar()
            return snapshot.estaAptoParaScanRede()
        }
    }
