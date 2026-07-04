package io.signallq.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.ContatoOperadora
import io.signallq.app.ui.OperadoraLogoCatalog

/**
 * Badge visual local e reutilizavel de uma operadora (SIG-292).
 *
 * Renderiza a identidade visual de [OperadoraLogoCatalog] (cor de marca + monograma),
 * sem nenhuma chamada de rede. Uso apenas identificativo — nao e o logo oficial da
 * operadora, nao sugere parceria, patrocinio ou endosso.
 */
@Composable
fun OperadoraBadge(
    operadora: ContatoOperadora,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val identidade = OperadoraLogoCatalog.identidadePara(operadora)
    Box(
        modifier =
            modifier
                .size(size)
                .background(identidade.corMarca, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = identidade.monograma,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42).sp,
        )
    }
}
