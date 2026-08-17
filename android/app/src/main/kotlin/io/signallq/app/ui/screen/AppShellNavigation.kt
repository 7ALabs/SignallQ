package io.signallq.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Permite validar a Jornada 2.0 sem remover a navegação legada. */
enum class AppShellMode {
    Legacy,
    Guided2,
}

internal val LocalAppShellMode = staticCompositionLocalOf { AppShellMode.Legacy }

internal fun AppShellMode.usaInicio2(): Boolean = this == AppShellMode.Guided2

internal enum class AppShellOverlay {
    Laudo,
    Ping,
    Privacidade,
    Novidades,
    ResultadoVelocidade,
    Fibra,
    Dispositivos,
    EquipamentoInternet,
    Ferramentas,
    Dns,
    Perfil,
    Ajustes,
    SinalCanais,
    SinalWifi,
    Termos,
    DiagnosticoGuiado,
    DetalhesTecnicos,
    ModoGamer,
    Assist,
}

internal enum class AppShellRoot(
    val legacyIndex: Int,
) {
    Home(0),
    Speed(1),
    Wifi(2),
    History(3),
    Tools(4),
    ;

    companion object {
        fun fromLegacyIndex(index: Int): AppShellRoot = entries.firstOrNull { it.legacyIndex == index } ?: Tools
    }
}

internal fun AppShellMode.roots(): List<AppShellRoot> =
    when (this) {
        AppShellMode.Legacy -> AppShellRoot.entries
        AppShellMode.Guided2 ->
            listOf(
                AppShellRoot.Home,
                AppShellRoot.Speed,
                AppShellRoot.History,
                AppShellRoot.Tools,
            )
    }

internal fun AppShellRoot.screenName(): String =
    when (this) {
        AppShellRoot.Home -> "home"
        AppShellRoot.Speed -> "speedtest"
        AppShellRoot.Wifi -> "sinal_wifi"
        AppShellRoot.History -> "historico"
        AppShellRoot.Tools -> "ferramentas"
    }

internal fun shouldShowAppShellBottomBar(
    mode: AppShellMode,
    isAtRoot: Boolean,
    speedtestRunning: Boolean,
): Boolean = !speedtestRunning && (mode == AppShellMode.Legacy || isAtRoot)

@Composable
internal fun AppShellBackHandlers(
    navigator: AppShellNavigator,
    onOverlayRemoved: (AppShellOverlay) -> Unit = {},
) {
    BackHandler(enabled = navigator.isAtRoot && navigator.selectedRoot == AppShellRoot.History) {
        navigator.select(AppShellRoot.Home)
    }
    // Registrado por último para ter prioridade LIFO quando uma mudança de estado e a
    // recomposição dos handlers acontecem no mesmo frame.
    BackHandler(enabled = !navigator.isAtRoot) {
        // GH#1704 — o overlay do topo tem a primeira palavra: um fluxo interno de vários passos
        // (o diagnóstico guiado 2.0) precisa recuar UM passo antes de o overlay inteiro sair da
        // pilha. Só quando ele declara que não consumiu é que o navigator faz `pop`.
        //
        // Isto NÃO é um segundo motor de navegação: segue havendo um dispatcher, uma pilha de
        // overlays e um dono do back. O overlay não empilha nada aqui — só responde "consumi" ou
        // "não consumi". Ver `docs_ai/technical/appshell-root-content-registry.md`.
        if (navigator.consumirBackDoOverlayTopo()) return@BackHandler
        navigator.pop()?.let(onOverlayRemoved)
    }
}

/**
 * Registra um interceptador de back para [overlay] enquanto ele estiver no **topo** da pilha da
 * raiz atual — issue #1704 (2.0.09b), decisão de arquitetura de Caio.
 *
 * `onBack` devolve `true` quando consumiu o evento (recuou um passo interno) e `false` quando o
 * fluxo chegou ao início e o overlay inteiro deve sair. O registro é desfeito ao sair da
 * composição, então um overlay fechado nunca segura o back de quem ficou embaixo.
 *
 * A condição é "estar no topo", não "estar na pilha": com Perfil aberto por cima do diagnóstico
 * guiado, o back tem que fechar o Perfil, não recuar um passo do fluxo que está escondido atrás.
 */
@Composable
internal fun RegistrarBackDoOverlay(
    navigator: AppShellNavigator,
    overlay: AppShellOverlay,
    onBack: () -> Boolean,
) {
    val estaNoTopo = navigator.overlayStack.lastOrNull() == overlay
    DisposableEffect(navigator, overlay, estaNoTopo, onBack) {
        if (estaNoTopo) navigator.registrarBackDoOverlay(overlay, onBack)
        onDispose { navigator.desregistrarBackDoOverlay(overlay) }
    }
}

@Stable
internal class AppShellNavigator internal constructor(
    initialTab: Int,
    restoredStacks: Map<AppShellRoot, List<AppShellOverlay>> = emptyMap(),
) {
    var selectedTab by mutableIntStateOf(initialTab)

    private val stacks =
        AppShellRoot.entries.associateWith { root ->
            mutableStateListOf<AppShellOverlay>().apply {
                addAll(restoredStacks[root].orEmpty())
            }
        }

    val selectedRoot: AppShellRoot
        get() = AppShellRoot.fromLegacyIndex(selectedTab)

    val overlayStack: MutableList<AppShellOverlay>
        get() = stacks.getValue(selectedRoot)

    val isAtRoot: Boolean
        get() = overlayStack.isEmpty()

    fun select(root: AppShellRoot) {
        selectedTab = root.legacyIndex
    }

    fun open(overlay: AppShellOverlay) {
        if (overlay !in overlayStack) overlayStack.add(overlay)
    }

    fun pop(): AppShellOverlay? = overlayStack.removeLastOrNull()

    /**
     * Interceptadores de back por overlay (GH#1704). Não é uma segunda pilha de navegação: é um
     * mapa de "quem responde por si" consultado ANTES do [pop]. Deliberadamente **não** entra em
     * [saveState]/[restoreState] — lambda não é serializável, e cada overlay re-registra o seu ao
     * recompor. O que precisa sobreviver à recriação de processo é o estado do fluxo, que é
     * responsabilidade de quem o mantém, não deste mapa.
     */
    private val interceptadoresBack = mutableMapOf<AppShellOverlay, () -> Boolean>()

    internal fun registrarBackDoOverlay(
        overlay: AppShellOverlay,
        onBack: () -> Boolean,
    ) {
        interceptadoresBack[overlay] = onBack
    }

    internal fun desregistrarBackDoOverlay(overlay: AppShellOverlay) {
        interceptadoresBack.remove(overlay)
    }

    /**
     * Dá ao overlay do topo a chance de consumir o back. `false` quando não há topo, quando o topo
     * não registrou interceptador, ou quando ele declarou que não consumiu — nos três casos o
     * chamador segue para [pop], que é o comportamento que existia antes desta issue.
     *
     * Consulta só o **topo**: um interceptador de overlay soterrado não pode sequestrar o back de
     * quem está por cima.
     */
    internal fun consumirBackDoOverlayTopo(): Boolean {
        val topo = overlayStack.lastOrNull() ?: return false
        return interceptadoresBack[topo]?.invoke() ?: false
    }

    internal fun snapshotStacks(): Map<AppShellRoot, List<AppShellOverlay>> =
        stacks.mapValues { (_, stack) -> stack.toList() }

    companion object {
        internal fun saveState(navigator: AppShellNavigator): List<String> =
            buildList {
                add(navigator.selectedTab.toString())
                AppShellRoot.entries.forEach { root ->
                    add(navigator.snapshotStacks().getValue(root).joinToString(",") { it.name })
                }
            }

        internal fun restoreState(values: List<*>): AppShellNavigator {
            val selectedTab = values.firstOrNull()?.toString()?.toIntOrNull() ?: 0
            val stacks =
                AppShellRoot.entries
                    .mapIndexed { index, root ->
                        val overlays =
                            values
                                .getOrNull(index + 1)
                                ?.toString()
                                ?.split(',')
                                ?.filter(String::isNotBlank)
                                ?.mapNotNull { name -> AppShellOverlay.entries.firstOrNull { it.name == name } }
                                .orEmpty()
                        root to overlays
                    }.toMap()
            return AppShellNavigator(selectedTab, stacks)
        }

        val Saver: Saver<AppShellNavigator, Any> =
            Saver(
                save = { navigator -> saveState(navigator) },
                restore = { restored ->
                    val values = restored as? List<*> ?: return@Saver null
                    restoreState(values)
                },
            )
    }
}

@Composable
internal fun rememberAppShellNavigator(mode: AppShellMode): AppShellNavigator =
    rememberSaveable(mode, saver = AppShellNavigator.Saver) {
        AppShellNavigator(
            initialTab =
                when (mode) {
                    AppShellMode.Legacy -> AppShellRoot.Speed.legacyIndex
                    AppShellMode.Guided2 -> AppShellRoot.Home.legacyIndex
                },
        )
    }
