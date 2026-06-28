package io.signallq.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens

private enum class DispositivoGuia { Android, Roteador }

@Composable
fun WifiChannelGuide() {
    val c = LocalLkTokens.current
    var selecionado by remember { mutableStateOf(DispositivoGuia.Roteador) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Text(
            "Como mudar o canal da sua rede?",
            fontWeight = FontWeight.W600,
            fontSize = 14.sp,
            color = c.textPrimary,
        )

        // Seletor Android / Roteador
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.bgSecondary)
                    .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DispositivoGuia.entries.forEach { opcao ->
                val ativo = selecionado == opcao
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(LkRadius.card - 2.dp))
                            .background(if (ativo) c.bgCard else Color.Transparent)
                            .then(
                                if (ativo) {
                                    Modifier.border(1.dp, c.border, RoundedCornerShape(LkRadius.card - 2.dp))
                                } else {
                                    Modifier
                                },
                            ).semantics { role = Role.Tab }
                            .clickable { selecionado = opcao }
                            .padding(horizontal = LkSpacing.md, vertical = LkSpacing.sm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (opcao == DispositivoGuia.Android) Icons.Outlined.Smartphone else Icons.Outlined.Router,
                        contentDescription = null,
                        tint = if (ativo) LkColors.accent else c.textTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(LkSpacing.xs))
                    Text(
                        text = if (opcao == DispositivoGuia.Android) "Android" else "Roteador",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (ativo) FontWeight.W600 else FontWeight.W400,
                        color = if (ativo) c.textPrimary else c.textTertiary,
                    )
                }
            }
        }

        HorizontalDivider(color = c.border)

        when (selecionado) {
            DispositivoGuia.Android -> ConteudoAndroid(c)
            DispositivoGuia.Roteador -> ConteudoRoteador(c)
        }
    }
}

@Composable
private fun ConteudoAndroid(c: LkTokens) {
    Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
        GuideSection(
            number = 1,
            title = "Acesse Configurações > Wi-Fi",
            description = "Abra o app Configurações do seu Android e toque em \"Wi-Fi\" ou \"Rede e Internet\".",
            details = "",
            c = c,
        )

        GuideSection(
            number = 2,
            title = "Toque em sua rede",
            description = "Pressione o nome da rede conectada para ver os detalhes.",
            details = "",
            c = c,
        )

        GuideSection(
            number = 3,
            title = "Veja as informações avançadas",
            description = "Alguns modelos exibem o canal atual, frequência e velocidade de link.",
            details = "",
            c = c,
        )

        Spacer(Modifier.height(LkSpacing.sm))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(LkColors.warning.copy(alpha = 0.08f))
                    .border(1.dp, LkColors.warning.copy(alpha = 0.30f), RoundedCornerShape(LkRadius.card))
                    .padding(LkSpacing.md),
        ) {
            Text(
                "Observacao: o Android nao permite forcar o canal diretamente. " +
                    "Para mudar o canal da sua rede, use o painel do roteador.",
                fontSize = 12.sp,
                color = c.textSecondary,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun ConteudoRoteador(c: LkTokens) {
    Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
        GuideSection(
            number = 1,
            title = "Acesse o painel do roteador",
            description = "Abra o navegador e digite o endereco:",
            details = "* http://192.168.0.1\n* http://192.168.1.1\n* ou consulte o adesivo do roteador",
            c = c,
        )

        GuideSection(
            number = 2,
            title = "Va em Wireless / Wi-Fi",
            description = "Procure por:",
            details = "* Wireless Settings\n* Wi-Fi Configuration\n* 2.4GHz ou 5GHz Settings",
            c = c,
        )

        GuideSection(
            number = 3,
            title = "Selecione \"Canal\"",
            description = "Localize o campo de selecao de canal (Channel).",
            details = "* 2.4GHz: prefira canais 1, 6 ou 11\n* 5GHz: prefira canais 36, 100, 149 ou 165",
            c = c,
        )

        GuideSection(
            number = 4,
            title = "Escolha o canal recomendado ou \"Auto\"",
            description = "Se nao souber qual escolher, use o canal sugerido acima ou deixe em Auto.",
            details = "",
            c = c,
        )

        GuideSection(
            number = 5,
            title = "Salve e reinicie o roteador",
            description = "Clique em Salvar (Save / Apply).",
            details = "O roteador pode reiniciar por 5-10 segundos.\nSeu mesh ou extensor sincronizara automaticamente.",
            c = c,
        )

        Spacer(Modifier.height(LkSpacing.sm))
        Text(
            "Dica: execute um novo scan de canais apos a mudanca para confirmar a melhoria.",
            fontSize = 12.sp,
            color = c.textSecondary,
            fontWeight = FontWeight.W500,
        )
    }
}

@Composable
internal fun GuideSection(
    number: Int,
    title: String,
    description: String,
    details: String,
    c: LkTokens,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LkColors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.W600,
                fontSize = 13.sp,
                color = c.textPrimary,
            )
            if (description.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    fontSize = 12.sp,
                    color = c.textSecondary,
                )
            }
            if (details.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    details,
                    fontSize = 12.sp,
                    color = c.textTertiary,
                    lineHeight = 17.sp,
                )
            }
        }
    }
}
