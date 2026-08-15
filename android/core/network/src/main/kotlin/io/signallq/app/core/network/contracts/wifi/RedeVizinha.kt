package io.signallq.app.core.network.contracts.wifi

enum class SegurancaWifi { aberta, wep, wpa, wpa2, wpa3, desconhecida }

data class RedeVizinha(
    val ssid: String?,
    val bssid: String,
    val rssiDbm: Int,
    val frequenciaMhz: Int,
    val seguranca: SegurancaWifi,
    val larguraCanalMhz: Int?,
    val oui: String = "",
) {
    val banda: String
        get() = when {
            frequenciaMhz < 3000 -> "2.4GHz"
            frequenciaMhz < 6000 -> "5GHz"
            else -> "6GHz"
        }

    val canal: Int?
        get() = when {
            frequenciaMhz == 2484 -> 14
            frequenciaMhz in 2412..2483 -> (frequenciaMhz - 2412) / 5 + 1
            frequenciaMhz in 5160..5885 -> (frequenciaMhz - 5000) / 5
            frequenciaMhz >= 5925 -> (frequenciaMhz - 5955) / 5 + 1
            else -> null
        }
}
