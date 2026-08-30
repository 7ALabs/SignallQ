package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens

/** Limite de caracteres do relato livre — ver kdoc de `DiagnosticoGuiadoEstado.relatoLivre`. */
const val LIMITE_RELATO_LIVRE_DIAGNOSTICO = 200

/** Tag de teste do campo de texto — usado por `DiagnosticoGuiadoScreenTest` para digitar sem
 *  depender do placeholder (some assim que há texto). */
const val TAG_RELATO_LIVRE_DIAGNOSTICO = "relato_livre_diagnostico"

/**
 * Tela do objetivo [io.signallq.app.core.diagnostico.ObjetivoDiagnostico.OUTRO_PROBLEMA] —
 * substitui a pergunta fechada por um campo de texto livre, único objetivo do roteiro que faz
 * isso (ver kdoc do enum). O texto é só contexto extra para a IA explicar o resultado — nunca é
 * lido pelo `DiagnosticoGuiadoEngine` para decidir status ou causa, e por isso continuar sem
 * escrever nada é uma opção válida, não um estado de erro.
 */
@Composable
internal fun DiagnosticoGuiadoRelatoLivreSection(
    modifier: Modifier = Modifier,
    texto: String,
    onTextoAlterado: (String) -> Unit,
    onContinuar: () -> Unit,
    /** Pular sem salvar o texto digitado — distinto de continuar com o campo vazio só pra
     *  deixar a intenção explícita na telemetria/testes. */
    onPular: () -> Unit,
    c: LkTokens,
) {
    Column(modifier.fillMaxSize().background(c.bgPrimary).padding(LkSpacing.xl)) {
        Text(
            "Descreva o que está acontecendo",
            style = MaterialTheme.typography.titleLarge,
            color = c.textPrimary,
        )
        Spacer(Modifier.height(LkSpacing.sm))
        Text(
            "Esse texto ajuda a IA a explicar melhor o resultado. É opcional — você pode continuar sem escrever nada.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(LkSpacing.lg))
        OutlinedTextField(
            value = texto,
            onValueChange = { novo ->
                onTextoAlterado(novo.take(LIMITE_RELATO_LIVRE_DIAGNOSTICO))
            },
            modifier = Modifier.fillMaxWidth().testTag(TAG_RELATO_LIVRE_DIAGNOSTICO),
            placeholder = { Text("Ex.: a internet cai só à noite, depois de uma hora usando...") },
            colors = TextFieldDefaults.colors(),
        )
        Spacer(Modifier.height(LkSpacing.xs))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "${texto.length}/$LIMITE_RELATO_LIVRE_DIAGNOSTICO",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
            )
        }
        Spacer(Modifier.height(LkSpacing.lg))
        Button(
            onClick = onContinuar,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
        ) {
            Text("Continuar")
        }
        Spacer(Modifier.height(LkSpacing.md))
        Text(
            "Pular esta pergunta",
            style = MaterialTheme.typography.titleSmall,
            color = c.textSecondary,
            modifier = Modifier.fillMaxWidth().clickable { onPular() }.padding(vertical = LkSpacing.sm),
        )
    }
}
