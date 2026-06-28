package io.signallq.app.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

// ~80dp em pixels com densidade 3.75x (mdpi base). Ajustado empiricamente para fade suave.
private const val FADE_THRESHOLD_PX = 300

@Composable
fun LazyListState.rememberTopBarAlpha(): Float {
    val offset by remember {
        derivedStateOf {
            if (firstVisibleItemIndex == 0) firstVisibleItemScrollOffset else FADE_THRESHOLD_PX
        }
    }
    return (1f - offset.toFloat() / FADE_THRESHOLD_PX).coerceIn(0f, 1f)
}

@Composable
fun ScrollState.rememberTopBarAlpha(): Float {
    val offset by remember { derivedStateOf { value } }
    return (1f - offset.toFloat() / FADE_THRESHOLD_PX).coerceIn(0f, 1f)
}
