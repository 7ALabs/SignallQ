package io.signallq.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Resolve a [Activity] hospedeira a partir de um [Context] de Compose — issue #1703.
 *
 * `LocalContext.current` **não** é garantidamente uma `Activity`: o Compose entrega o contexto da
 * composição, que em vários caminhos é um [ContextWrapper] (tema aplicado via `ContextThemeWrapper`,
 * `ComposeView` dentro de outro host, previews). Fazer `LocalContext.current as Activity` funciona
 * em teste e estoura em produção quando qualquer camada embrulha o contexto — por isso o
 * desembrulho iterativo.
 *
 * Devolve `null` em vez de lançar. Quem chama precisa tratar a ausência: num `@Preview` ou num
 * teste de composição sem Activity não há hospedeira, e isso não pode derrubar a tela — no caso da
 * #1703, significa apenas não oferecer a entrada de opções de privacidade da UMP, que exige
 * `Activity` para abrir o formulário do Google.
 *
 * Único consumidor hoje: `AppShellPrivacidadeOverlay`. Se aparecer um segundo, esta função já é o
 * lugar — não duplicar o laço no call site.
 */
tailrec fun Context.encontrarActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.encontrarActivity()
        else -> null
    }
