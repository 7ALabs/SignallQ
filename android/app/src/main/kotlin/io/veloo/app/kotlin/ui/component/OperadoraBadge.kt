package io.signallq.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import io.signallq.app.ui.ContatoOperadora
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.OperadoraLogoCatalog
import io.signallq.app.ui.ResolvedOperadoraIdentity

/**
 * Badge visual local e reutilizavel de uma operadora (SIG-292).
 *
 * Preferencia: logo oficial bundled ([OperadoraVisualIdentity.logoRes]) quando o catalogo
 * tiver um asset seguro para aquela operadora (ver `docs/brand-assets/operators-sources.md`).
 * Cai para o badge de cor+monograma quando nao houver logo oficial disponivel/baixavel com
 * seguranca — sem nenhuma chamada de rede em ambos os casos.
 *
 * Uso apenas identificativo — nao sugere parceria, patrocinio ou endosso das operadoras.
 */
@Composable
fun OperadoraBadge(
    operadora: ContatoOperadora,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val identidade = OperadoraLogoCatalog.identidadePara(operadora)
    val logoRes = identidade.logoRes

    if (logoRes != null) {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(logoRes),
                contentDescription = operadora.nome,
                modifier = Modifier.padding(size * 0.08f),
            )
        }
    } else {
        MonogramaBadge(
            monograma = identidade.monograma,
            corMarca = identidade.corMarca,
            modifier = modifier,
            size = size,
        )
    }
}

/**
 * Badge de operadora resolvida pela cadeia local -> remoto -> fallback (GH#965/#970,
 * [io.signallq.app.ui.OperadoraDirectoryResolver]). Igual ao overload acima para o
 * caso LOCAL (mesmo asset bundled); para REMOTE, carrega [ResolvedOperadoraIdentity.logoUrl]
 * via rede (Coil) — enquanto carrega ou se a imagem falhar, cai no mesmo badge de
 * cor+monograma, nunca em tela quebrada ou vazia.
 */
@Composable
fun OperadoraBadge(
    identidade: ResolvedOperadoraIdentity,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    when {
        identidade.logoRes != null -> {
            Box(
                modifier = modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(identidade.logoRes),
                    contentDescription = identidade.displayName,
                    modifier = Modifier.padding(size * 0.08f),
                )
            }
        }
        identidade.logoUrl != null -> {
            SubcomposeAsyncImage(
                model = identidade.logoUrl,
                contentDescription = identidade.displayName,
                modifier = modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop,
                loading = {
                    MonogramaBadge(
                        monograma = identidade.monograma,
                        corMarca = identidade.corMarca,
                        size = size,
                    )
                },
                error = {
                    MonogramaBadge(
                        monograma = identidade.monograma,
                        corMarca = identidade.corMarca,
                        size = size,
                    )
                },
            )
        }
        else -> {
            MonogramaBadge(
                monograma = identidade.monograma,
                corMarca = identidade.corMarca,
                modifier = modifier,
                size = size,
            )
        }
    }
}

@Composable
private fun MonogramaBadge(
    monograma: String,
    corMarca: Color?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .background(corMarca ?: LkColors.accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = monograma,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42).sp,
        )
    }
}
