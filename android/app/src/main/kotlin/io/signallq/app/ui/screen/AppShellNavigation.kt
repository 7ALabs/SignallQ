package io.signallq.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
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
        navigator.pop()?.let(onOverlayRemoved)
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
