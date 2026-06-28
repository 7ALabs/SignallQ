package io.signallq.app.feature.dns

data class ConfiguracaoDnsSugerida(
    val nomeProvedor: String,
    val primario: String,
    val secundario: String,
    val hostDnsPrivado: String,
)

class OrientadorConfiguracaoDns {
    fun sugerir(
        melhorProvedor: String?,
        provedorAtivo: String?,
        diagnosticoCoerencia: DiagnosticoCoerenciaDns,
    ): ConfiguracaoDnsSugerida? {
        val melhorNormalizado = melhorProvedor?.trim()?.lowercase().orEmpty()
        if (melhorNormalizado.isBlank()) return null

        val sugestao = mapearProvedor(melhorNormalizado) ?: return null
        val ativoNormalizado = provedorAtivo?.trim()?.lowercase()
        val mesmaOpcao = !ativoNormalizado.isNullOrBlank() && ativoNormalizado == melhorNormalizado

        if (mesmaOpcao && diagnosticoCoerencia.nivelAlerta == NivelAlertaCoerenciaDns.none) {
            return null
        }
        return sugestao
    }

    private fun mapearProvedor(nome: String): ConfiguracaoDnsSugerida? {
        return when (nome) {
            "cloudflare" ->
                ConfiguracaoDnsSugerida(
                    nomeProvedor = "cloudflare",
                    primario = "1.1.1.1",
                    secundario = "1.0.0.1",
                    hostDnsPrivado = "one.one.one.one",
                )
            "google" ->
                ConfiguracaoDnsSugerida(
                    nomeProvedor = "google",
                    primario = "8.8.8.8",
                    secundario = "8.8.4.4",
                    hostDnsPrivado = "dns.google",
                )
            "quad9" ->
                ConfiguracaoDnsSugerida(
                    nomeProvedor = "quad9",
                    primario = "9.9.9.9",
                    secundario = "149.112.112.112",
                    hostDnsPrivado = "dns.quad9.net",
                )
            "opendns" ->
                ConfiguracaoDnsSugerida(
                    nomeProvedor = "opendns",
                    primario = "208.67.222.222",
                    secundario = "208.67.220.220",
                    hostDnsPrivado = "doh.opendns.com",
                )
            "adguard" ->
                ConfiguracaoDnsSugerida(
                    nomeProvedor = "adguard",
                    primario = "94.140.14.14",
                    secundario = "94.140.15.15",
                    hostDnsPrivado = "dns.adguard-dns.com",
                )
            else -> null
        }
    }
}
