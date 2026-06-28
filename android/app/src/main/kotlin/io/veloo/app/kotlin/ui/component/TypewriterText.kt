package io.signallq.app.ui.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    charDelayMs: Long = 12L,
    style: TextStyle = LocalTextStyle.current,
    onComplete: () -> Unit = {},
) {
    // rememberSaveable(text) persists across back-navigation so a completed animation
    // is never replayed; resets automatically when `text` changes (new AI message).
    var hasCompleted by rememberSaveable(text) { mutableStateOf(false) }
    var visibleText by remember { mutableStateOf(if (hasCompleted) text else "") }

    LaunchedEffect(text) {
        if (hasCompleted) {
            visibleText = text
            return@LaunchedEffect
        }
        visibleText = ""
        text.forEachIndexed { index, _ ->
            delay(charDelayMs)
            visibleText = text.substring(0, index + 1)
        }
        hasCompleted = true
        onComplete()
    }

    Text(text = visibleText, style = style, modifier = modifier)
}
