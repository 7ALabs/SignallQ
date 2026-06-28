package io.signallq.app.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LocalLkTokens

@Composable
fun RotatingMessageText(
    message: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    textAlign: TextAlign = TextAlign.Center,
) {
    val c = LocalLkTokens.current
    AnimatedContent(
        targetState = message,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "rotating-msg",
        modifier = modifier,
    ) { msg ->
        Text(
            text = msg,
            fontSize = fontSize,
            color = c.textSecondary,
            textAlign = textAlign,
        )
    }
}
