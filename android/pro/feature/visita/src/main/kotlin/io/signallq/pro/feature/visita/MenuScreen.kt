package io.signallq.pro.feature.visita

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.signallq.pro.core.designsystem.Navbar
import io.signallq.pro.core.designsystem.NavbarItem

private const val ITEM_PAINEL = "painel"
private const val ITEM_CLIENTES = "clientes"
private const val ITEM_FERRAMENTAS = "ferramentas"
private const val ITEM_PERFIL = "perfil"

/** Tela 2.2 -- menu do Pro, usando [Navbar] do design system (sem navegacao custom). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onPainel: () -> Unit,
    onClientes: () -> Unit,
    onFerramentas: () -> Unit,
    onPerfil: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Menu") }) },
    ) { paddingValues ->
        Navbar(
            modifier = modifier.fillMaxSize().padding(paddingValues),
            itens =
                listOf(
                    NavbarItem(ITEM_PAINEL, "Painel", Icons.Outlined.Home),
                    NavbarItem(ITEM_CLIENTES, "Clientes", Icons.Outlined.Group),
                    NavbarItem(ITEM_FERRAMENTAS, "Ferramentas", Icons.Outlined.Build),
                    NavbarItem(ITEM_PERFIL, "Perfil", Icons.Outlined.Person),
                ),
            onItemClick = { item ->
                when (item.id) {
                    ITEM_PAINEL -> onPainel()
                    ITEM_CLIENTES -> onClientes()
                    ITEM_FERRAMENTAS -> onFerramentas()
                    ITEM_PERFIL -> onPerfil()
                }
            },
        )
    }
}
