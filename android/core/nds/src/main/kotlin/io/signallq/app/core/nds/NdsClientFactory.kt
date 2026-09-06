package io.signallq.app.core.nds

/**
 * Factory de conveniencia para instanciar [NdsClient] com os valores reais de
 * `BuildConfig` (URL fixa do NDS + token lido de `local.properties`/variavel
 * de ambiente em build time). Consumidores futuros (NDS-02+) devem preferir
 * isto a ler `BuildConfig` diretamente em cada call site.
 */
object NdsClientFactory {
    fun create(): NdsClient =
        NdsClient(
            baseUrl = BuildConfig.NDS_BASE_URL,
            apiToken = BuildConfig.NDS_API_TOKEN,
        )
}
