package io.signallq.app.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.component.ClienteConectadoUi
import io.signallq.app.ui.component.EquipamentoItemTecnico
import io.signallq.app.ui.component.EquipamentoSecaoTecnica
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.corSemantica

/**
 * Módulos técnicos (passos 9, 10, 11 e 12 da narrativa, bug #6 spec Lia) —
 * cada card representa uma seção de dado cru do equipamento (Fibra óptica,
 * Internet/WAN, LAN, Wi-Fi, Dispositivos conectados). Extraído de
 * `EquipamentoInternetScreen.kt` (dívida crítica, ver
 * `.claude/rules/higiene-e-padronizacao-repositorio.md` seção 4.6).
 *
 * ## Wi-Fi — full-width (revisão Lia 2026-07-18)
 * A primeira versão desta tela particionava Wi-Fi em 2 colunas por banda
 * (2,4GHz | 5/6GHz). Reavaliado: com só 1 item de dado por banda (canal +
 * configuração — o contrato
 * [io.signallq.app.core.network.contracts.localdevice.WifiRadioSnapshot] não
 * modela sinal/clientes por rádio), os dois cards lado a lado ficavam ~90%
 * vazios e recriavam a poluição visual que o redesign eliminou. Wi-Fi passou
 * a ser um card full-width único, com as duas bandas como linhas dentro do
 * mesmo card (2,4GHz antes de 5/6GHz — ordem já preservada pelo mapper, 1
 * item por rádio), no mesmo formato dos demais itens técnicos.
 *
 * ## Disclosure progressiva em todo módulo com >2 itens (revisão Lia 2026-07-24)
 * O redesign de 2026-07-18 já tinha resolvido a densidade do card "Fibra"
 * escondendo por padrão RX/TX/temperatura/modo/serial atrás de "Ver detalhes
 * técnicos" — mas a regra só valia para o título "Fibra óptica". Os cards
 * "Internet (WAN)" e "Rede local (LAN)" tinham a mesma quantidade de campos
 * crus (IP, gateway, DNS primário/secundário, interface / máscara, faixa
 * DHCP) e continuavam abrindo tudo de cara, reintroduzindo a mesma parede de
 * jargão que o redesign original eliminou no card de Fibra. A regra de
 * colapso agora é genérica — qualquer seção com mais de 2 itens nasce
 * colapsada, mostrando só os 2 primeiros (já os mais relevantes pra um
 * usuário não técnico, por ordem do mapper) — não mais amarrada ao título.
 *
 * ## Zona "Detalhes técnicos" — hairline em vez de fill (revisão Lia 2026-07-25)
 * Wireframe aprovado pelo Luiz (direção de reorganização da hierarquia visual
 * da tela, artifact `547faba7-3981-42d0-a82f-58e0eba64da8`): o card deixa de
 * usar o mesmo fill pesado (`surfaceContainer`) da zona de resumo e passa a
 * usar só borda hairline (`outlineVariant`) com fundo transparente — sinaliza
 * "isto é referência de consulta", não destaque. Linhas de dado ganham
 * divisor sutil entre si (`surfaceContainerHigh`, mesmo tom do token real por
 * trás do `#2B2930` do wireframe) e algarismos tabulares. Quando colapsado, um
 * rodapé "+ N campos ocultos" deixa explícito que há mais dado atrás do
 * toggle — o wireframe usava um hex ilustrativo (`#37333F`/`#7A7488`) sem
 * token 1:1 no app; mapeados para `outlineVariant`/`textTertiary`, os tokens
 * reais mais próximos, em vez de introduzir hex novo (regra do design system:
 * nunca hardcode fora de token).
 */
@Composable
internal fun ModuloTecnicoCard(
    secao: EquipamentoSecaoTecnica,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    val tituloExibido = if (secao.titulo == "Fibra óptica") "Fibra" else secao.titulo
    val toggleLabel = "Ver detalhes técnicos"
    var expandido by remember(secao.titulo) { mutableStateOf(secao.itens.size <= 2) }

    LkSurfaceCardHairline(modifier = modifier, c = c) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(secao.icone, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(LkSpacing.xs))
            Text(
                text = tituloExibido,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (secao.itens.size > 2) {
                Text(
                    text = if (expandido) "Ocultar" else toggleLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = c.primary,
                    modifier = Modifier.clickable { expandido = !expandido },
                )
            }
        }
        Spacer(Modifier.height(LkSpacing.sm))
        val itensVisiveis = if (expandido || secao.itens.size <= 2) secao.itens else secao.itens.take(2)
        secao.overline?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
            Spacer(Modifier.height(4.dp))
        }
        itensVisiveis.forEachIndexed { index, item ->
            DataRowCard(
                item = secao.normalizarItem(item),
                c = c,
                estiloTecnico = true,
                mostrarDivisorSuperior = index > 0,
            )
        }
        if (!expandido && secao.itens.size > itensVisiveis.size) {
            Text(
                text = "+ ${secao.itens.size - itensVisiveis.size} campos ocultos",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
                color = c.textTertiary,
                textAlign = TextAlign.End,
            )
        }
        if (secao.clientes.isNotEmpty()) {
            secao.clientes.forEach { cliente ->
                Spacer(Modifier.height(8.dp))
                ClienteResumoRow(cliente = cliente, c = c)
            }
        }
        secao.trailing?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
    }
}

/** Container hairline (borda `outlineVariant`, fundo transparente) usado só pelos cards da
 *  zona "Detalhes técnicos" — distinto do `LkSurfaceCard` (fill `surfaceContainer`) da zona de
 *  resumo, ver KDoc de [ModuloTecnicoCard]. */
@Composable
private fun LkSurfaceCardHairline(
    modifier: Modifier = Modifier,
    c: LkTokens,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.card))
                .border(1.dp, c.outlineVariant, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.base),
        content = content,
    )
}

internal fun EquipamentoSecaoTecnica.normalizarItem(item: EquipamentoItemTecnico): EquipamentoItemTecnico =
    if (titulo == "Fibra" && item.label == "Conexão da fibra") {
        item.copy(label = "Conexão PON")
    } else {
        item
    }

/** [estiloTecnico] só é usado pela zona "Detalhes técnicos" (ver [ModuloTecnicoCard]) —
 *  aplica algarismos tabulares ao valor (linha de dado técnico do wireframe aprovado) e,
 *  quando [mostrarDivisorSuperior] também é verdadeiro, desenha um hairline
 *  `surfaceContainerHigh` acima da linha (toda linha exceto a primeira da seção). Ambos falsos
 *  por padrão preservam o visual original nos demais consumidores (ex.: `DeviceInfoSectionCard`,
 *  fora do escopo desta mudança). */
@Composable
internal fun DataRowCard(
    item: EquipamentoItemTecnico,
    c: LkTokens,
    estiloTecnico: Boolean = false,
    mostrarDivisorSuperior: Boolean = false,
) {
    if (mostrarDivisorSuperior) {
        HorizontalDivider(color = c.surfaceContainerHigh, thickness = 1.dp)
    }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (estiloTecnico) Modifier.padding(vertical = LkSpacing.sm) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = item.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.valor,
            modifier = Modifier.weight(1f),
            style =
                MaterialTheme.typography.labelLarge.let {
                    if (estiloTecnico) it.copy(fontFeatureSettings = "tnum") else it
                },
            color = item.statusValor?.let { it.corSemantica(c) } ?: c.textPrimary,
            textAlign = TextAlign.End,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ClienteResumoRow(
    cliente: ClienteConectadoUi,
    c: LkTokens,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(cliente.tipoIcone, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(cliente.titulo, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary, modifier = Modifier.weight(1f))
                cliente.tipoLabel?.let {
                    Spacer(Modifier.width(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                }
            }
            cliente.ip?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textTertiary) }
        }
    }
}

@Composable
internal fun DevicesSummaryCard(
    summary: DevicesSummaryUi,
    c: LkTokens,
) {
    LkSurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Devices, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(LkSpacing.xs))
            Text("Dispositivos conectados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
        }
        Spacer(Modifier.height(LkSpacing.sm))
        Text("${summary.total} dispositivos", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
        Spacer(Modifier.height(2.dp))
        Text("${summary.wifi} pelo Wi-Fi · ${summary.cabo} por cabo", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        if (summary.flags.isNotEmpty()) {
            Spacer(Modifier.height(LkSpacing.sm))
            summary.flags.forEachIndexed { index, flag ->
                Text(flag, style = MaterialTheme.typography.labelMedium, color = c.warning)
                if (index < summary.flags.lastIndex) Spacer(Modifier.height(4.dp))
            }
        }
    }
}
