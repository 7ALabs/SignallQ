package io.signallq.app.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.component.LkSymbol

private data class AppShellNavItem(
    val root: AppShellRoot,
    val label: String,
    val symbol: String,
)

@Composable
internal fun AppShellBottomBar(
    c: LkTokens,
    selectedTab: Int,
    testeAtivo: Boolean = false,
    featureFlags: AppShellFeatureFlagsState = AppShellFeatureFlagsState(),
    onRootSelected: (AppShellRoot) -> Unit,
    onTabBloqueada: (moduleId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val items =
        listOf(
            AppShellNavItem(AppShellRoot.Home, "Início", "home"),
            AppShellNavItem(AppShellRoot.Speed, "Velocidade", "speed"),
            AppShellNavItem(AppShellRoot.History, "Histórico", "history"),
            AppShellNavItem(AppShellRoot.Tools, "Ferramentas", "build"),
        )

    Column(modifier = modifier) {
        HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)
        NavigationBar(containerColor = c.surfaceContainer, tonalElevation = 0.dp) {
            items.forEach { item ->
                AppNavItem(
                    c = c,
                    selectedTab = selectedTab,
                    item = item,
                    habilitada = featureFlags.tabHabilitada(item.root.index),
                    onRootSelected = onRootSelected,
                    onTabBloqueada = onTabBloqueada,
                    showBadge = item.root == AppShellRoot.Speed && testeAtivo,
                )
            }
        }
    }
}

@Composable
private fun RowScope.AppNavItem(
    c: LkTokens,
    selectedTab: Int,
    item: AppShellNavItem,
    habilitada: Boolean,
    onRootSelected: (AppShellRoot) -> Unit,
    onTabBloqueada: (moduleId: String) -> Unit,
    showBadge: Boolean,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val badgePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(700, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "badgeAlpha",
    )
    NavigationBarItem(
        selected = selectedTab == item.root.index,
        enabled = habilitada,
        onClick = {
            if (habilitada) {
                onRootSelected(item.root)
            } else {
                tabModuleId(item.root.index)?.let(onTabBloqueada)
            }
        },
        icon = {
            BadgedBox(badge = {
                if (showBadge) Badge(modifier = Modifier.graphicsLayer { alpha = badgePulseAlpha })
            }) {
                LkSymbol(name = item.symbol, filled = selectedTab == item.root.index)
            }
        },
        label = {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selectedTab == item.root.index) FontWeight.SemiBold else FontWeight.Medium,
            )
        },
        colors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = c.onPrimaryContainer,
                unselectedIconColor = c.onSurfaceVariant,
                selectedTextColor = c.onSurface,
                unselectedTextColor = c.onSurfaceVariant,
                indicatorColor = c.primaryContainer,
            ),
    )
}
