package io.signallq.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

@Composable
fun SignallQListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = LocalLkTokens.current
    val interaction = if (onClick == null) Modifier else Modifier.clickable(enabled = enabled, onClick = onClick)
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .then(interaction)
                .defaultMinSize(minHeight = LkSpacing.compositionLarge),
        color = if (selected) c.surfaceContainerHigh else c.surface,
        contentColor = if (enabled) c.onSurface else c.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(LkSpacing.xl)) }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant) }
            }
            if (onClick != null) Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignallQTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = LocalLkTokens.current
    TopAppBar(
        title = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = c.surface),
    )
}

data class SignallQNavigationItem(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun SignallQNavigationBar(
    items: List<SignallQNavigationItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    NavigationBar(modifier = modifier, containerColor = c.surfaceContainerLow) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = c.primaryContainer),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignallQSheet(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalLkTokens.current
    ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier, containerColor = c.surfaceContainerLow) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = LkSpacing.xl).padding(bottom = LkSpacing.xxxl)) {
            Text(title, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.padding(top = LkSpacing.lg), content = content)
        }
    }
}

@Composable
fun SignallQDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { SignallQButton(confirmLabel, onConfirm, style = SignallQButtonStyle.Text) },
        dismissButton = dismissLabel?.let { { SignallQButton(it, onDismiss, style = SignallQButtonStyle.Text) } },
    )
}

@Composable
fun SignallQExpandableDetails(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .defaultMinSize(minHeight = LkSpacing.compositionLarge),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { expanded = !expanded }) {
                val icon = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore
                val description = if (expanded) "Recolher detalhes" else "Expandir detalhes"
                Icon(icon, contentDescription = description)
            }
        }
        if (expanded) {
            HorizontalDivider()
            Column(modifier = Modifier.padding(top = LkSpacing.md), content = content)
        }
    }
}
