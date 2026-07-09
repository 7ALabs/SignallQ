package io.signallq.app.review

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wrapper fino sobre a In-App Review API do Google Play (SIG-173/#664).
 *
 * A decisao de QUANDO pedir avaliacao fica em [ReviewPromptPolicy] (pura, sem
 * dependencia de Android) — esta classe so sabe COMO disparar o fluxo nativo.
 *
 * Por design do Google, a API nunca informa se o usuario efetivamente avaliou nem
 * garante que o dialogo sera exibido (cota interna do Play). Qualquer falha e
 * silenciosa — nunca deve interromper ou travar o app.
 */
@Singleton
class InAppReviewManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        suspend fun solicitarFluxoAvaliacao(activity: Activity) {
            runCatching {
                val reviewManager = ReviewManagerFactory.create(context)
                val reviewInfo = requestReviewInfo(reviewManager)
                reviewManager.launchReviewFlow(activity, reviewInfo)
            }.onFailure { e -> Timber.w(e, "falha ao solicitar avaliacao in-app do Google Play") }
        }

        private suspend fun requestReviewInfo(reviewManager: ReviewManager): ReviewInfo =
            suspendCancellableCoroutine { continuation ->
                val request = reviewManager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(task.result)
                    } else {
                        continuation.resumeWithException(
                            task.exception ?: IllegalStateException("falha desconhecida ao solicitar ReviewInfo"),
                        )
                    }
                }
            }
    }
