package io.linka.app.kotlin.monitoramento

import android.os.Build
import java.util.Locale

internal object OemKillInfo {
    val fabricanteRiscoAlto: Boolean
        get() {
            val f = Build.MANUFACTURER.lowercase(Locale.ROOT)
            return f.contains("samsung") ||
                f.contains("xiaomi") ||
                f.contains("oppo") ||
                f.contains("vivo") ||
                f.contains("huawei") ||
                f.contains("honor")
        }

    val nomeFabricante: String?
        get() =
            if (fabricanteRiscoAlto) {
                Build.MANUFACTURER.replaceFirstChar { it.uppercaseChar() }
            } else {
                null
            }
}
