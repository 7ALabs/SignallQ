package io.veloo.app.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.veloo.app.core.database.ApelidoDispositivoEntity
import io.veloo.app.core.database.SignallQDatabase
import io.veloo.app.core.datastore.PreferenciasAppRepository
import io.veloo.app.core.network.DispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel responsavel pelo scan de dispositivos na rede local e gestao de apelidos.
 *
 * Extraido do MainViewModel (1602L) — etapa A do refactor de ViewModels por feature.
 *
 * Responsabilidades:
 * - Scan de dispositivos (leve e profundo)
 * - Persistencia de apelidos via Room
 * - Deteccao de dispositivos novos (emite evento via [dispositivosNovos])
 *
 * Nota: a notificacao ao usuario e responsabilidade do chamador (MainActivity), que assina
 * [dispositivosNovos] e usa o contexto da Activity para exibir a notificacao do sistema.
 * Isso evita que featureDevices dependa do modulo :app (violaria a lei de dependencias).
 */
@HiltViewModel
class DevicesViewModel
    @Inject
    constructor(
        val scannerDispositivos: ScannerDispositivos,
        private val bancoDados: SignallQDatabase,
        private val preferenciasAppRepository: PreferenciasAppRepository,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        /**
         * Snapshot do scan de dispositivos — exposto para a UI observar.
         * Mapeado diretamente do ScannerDispositivos.
         */
        val snapshotDispositivos: StateFlow<SnapshotScanDispositivos> =
            scannerDispositivos.snapshotFlow
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    scannerDispositivos.snapshotFlow.value,
                )

        /**
         * Mapa de apelidos MAC -> nome customizado.
         * Filtra entidades sem apelido (registradas silenciosamente para supressao de notificacao).
         */
        val apelidos: StateFlow<Map<String, String>> =
            bancoDados
                .apelidoDispositivoDao()
                .observarTodos()
                .map { list ->
                    list.mapNotNull { e -> e.apelido?.let { ap -> e.mac to ap } }.toMap()
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

        /**
         * Emite o identificador (MAC ou IP) de cada dispositivo novo detectado.
         * O chamador (ex: MainActivity) assina este flow para exibir notificacoes do sistema.
         */
        private val _dispositivosNovos = MutableSharedFlow<String>(extraBufferCapacity = 10)
        val dispositivosNovos: SharedFlow<String> = _dispositivosNovos.asSharedFlow()

        /** Dispara scan de dispositivos completo (profundo = true). */
        fun refreshDispositivos() {
            viewModelScope.launch { scannerDispositivos.iniciarScan() }
        }

        /** Inicia scan leve — usado em iniciarRotinasNaoSpeedtest. */
        fun iniciarScanLeve() {
            viewModelScope.launch { scannerDispositivos.iniciarScan(profundo = false) }
        }

        /** Salva apelido de dispositivo por MAC no banco Room. */
        fun salvarApelido(
            mac: String,
            apelido: String,
        ) {
            viewModelScope.launch {
                bancoDados.apelidoDispositivoDao().salvar(
                    ApelidoDispositivoEntity(mac = mac, apelido = apelido),
                )
            }
        }

        /**
         * Verifica se ha dispositivos novos na rede.
         *
         * Executa scan leve e compara com identidades conhecidas (Room + DataStore).
         * Novos dispositivos sao emitidos via [dispositivosNovos] para o chamador notificar.
         *
         * Identidade estavel:
         * - Com MAC: "mac:<MAC lowercase>" (persistido no Room)
         * - Sem MAC: "ipnome:<IP>:<nome>" (persistido no DataStore)
         */
        fun verificarDispositivosNovos() {
            viewModelScope.launch(dispatchers.io) {
                try {
                    scannerDispositivos.iniciarScan(profundo = false)

                    val dispositivosAtuais = scannerDispositivos.snapshotFlow.value.dispositivos
                    val macsConhecidosRoom =
                        bancoDados.apelidoDispositivoDao().buscarTodos().map { it.mac }.toSet()
                    val identidadesConhecidas =
                        preferenciasAppRepository.buscarDispositivosConhecidos().toMutableSet()

                    val novasIdentidades = mutableSetOf<String>()

                    dispositivosAtuais.forEach { dispositivo ->
                        val identidade = DispositivosIdentidadeHelper.identidadeEstavelDispositivo(dispositivo)
                        val mac = dispositivo.mac
                        when {
                            mac != null -> {
                                val macNorm = mac.lowercase()
                                if (macNorm !in macsConhecidosRoom) {
                                    _dispositivosNovos.tryEmit(mac)
                                    bancoDados.apelidoDispositivoDao().inserirSilencioso(
                                        ApelidoDispositivoEntity(mac = macNorm, apelido = null),
                                    )
                                }
                            }
                            identidade != null && identidade !in identidadesConhecidas -> {
                                _dispositivosNovos.tryEmit(
                                    dispositivo.ip ?: dispositivo.nomeExibicao,
                                )
                                novasIdentidades.add(identidade)
                            }
                        }
                    }

                    if (novasIdentidades.isNotEmpty()) {
                        identidadesConhecidas.addAll(novasIdentidades)
                        preferenciasAppRepository.salvarDispositivosConhecidos(identidadesConhecidas)
                    }
                } catch (e: Exception) {
                    Timber.w("verificarDispositivosNovos falhou: ${e.message}")
                }
            }
        }

        /**
         * Delega para [DispositivosIdentidadeHelper.identidadeEstavelDispositivo].
         * Mantido como alias interno para retrocompatibilidade e testabilidade.
         */
        internal fun identidadeEstavelDispositivo(dispositivo: DispositivoRede): String? =
            DispositivosIdentidadeHelper.identidadeEstavelDispositivo(dispositivo)
    }
