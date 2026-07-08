package io.signallq.app.ui

data class ContatoOperadora(
    val id: String,
    val nome: String,
    val grupo: String,
    val detectarPor: List<String>,
    val sac: String,
    val whatsapp: String?,
    val site: String,
)

object BancoOperadoras {
    val lista =
        listOf(
            ContatoOperadora(
                "vivo_fibra",
                "Vivo",
                "Vivo / Telefônica",
                listOf("vivo", "telefônica", "telefonica", "gvt"),
                sac = "10315",
                whatsapp = "11999151515",
                site = "https://www.vivo.com.br",
            ),
            ContatoOperadora(
                "claro_net",
                "Claro",
                "Claro",
                listOf("claro", "net virtua", "embratel", "net serviços", "net servicos"),
                sac = "10621",
                whatsapp = "11999910621",
                site = "https://www.claro.com.br",
            ),
            ContatoOperadora(
                "tim_live",
                "TIM",
                "TIM",
                listOf("tim live", "tim"),
                sac = "10341",
                whatsapp = "4141414141",
                site = "https://www.tim.com.br",
            ),
            ContatoOperadora(
                "oi_fibra",
                "Oi",
                "Oi",
                listOf("oi fibra", "oi", "telemar"),
                sac = "10331",
                whatsapp = null,
                site = "https://www.oi.com.br",
            ),
            ContatoOperadora(
                "nio",
                "Nio Fibra",
                "Nio",
                listOf("nio", "nio internet", "nio fibra"),
                sac = "08000011000",
                whatsapp = "2136051000",
                site = "https://www.niointernet.com.br",
            ),
            ContatoOperadora(
                "algar",
                "Algar Telecom",
                "Algar",
                listOf("algar"),
                sac = "08009421212",
                whatsapp = "34998840123",
                site = "https://www.algartelecom.com.br",
            ),
            ContatoOperadora(
                "unifique",
                "Unifique",
                "Unifique",
                listOf("unifique"),
                sac = "10580",
                whatsapp = "4733800800",
                site = "https://www.unifique.com.br",
            ),
            ContatoOperadora(
                "brisanet",
                "Brisanet",
                "Brisanet",
                listOf("brisanet"),
                sac = "10517",
                whatsapp = "84981118525",
                site = "https://www.brisanet.com.br",
            ),
            ContatoOperadora(
                "desktop",
                "Desktop",
                "Desktop",
                listOf("desktop", "nextel"),
                sac = "10344",
                whatsapp = "1935143100",
                site = "https://www.desktop.com.br",
            ),
            ContatoOperadora(
                "ligga",
                "Ligga Telecom",
                "Ligga",
                listOf("ligga", "copel telecom"),
                sac = "08004141810",
                whatsapp = null,
                site = "https://www.liggavc.com.br",
            ),
            ContatoOperadora(
                "vero",
                "Vero Internet",
                "Vero",
                listOf("vero", "vero internet"),
                sac = "10385",
                whatsapp = null,
                site = "https://www.verointernet.com.br",
            ),
            ContatoOperadora(
                "giga_mais",
                "Giga+ Fibra",
                "Giga+ / Sumicity",
                listOf("giga+", "giga mais", "giga fibra", "sumicity"),
                sac = "10353",
                whatsapp = "22920410350",
                site = "https://www.sumicity.com.br",
            ),
        )

    fun resolver(ispNome: String?): ContatoOperadora? {
        if (ispNome.isNullOrBlank()) return null
        val normalizado = ispNome.lowercase().trim()
        return lista.firstOrNull { op ->
            op.detectarPor.any { termo ->
                // Match por palavra inteira (\b) — evita que termos curtos como "oi" sejam
                // capturados como substring de outro nome (ex.: "oi" dentro de "nio"/"condomínio").
                // Ver GH#411: operadora "Oi" era exibida como "Nio" por causa de contains() puro.
                Regex("\\b${Regex.escape(termo)}\\b").containsMatchIn(normalizado)
            }
        }
    }

    /**
     * Prefixos das 4 operadoras que atuam tanto em fibra quanto em móvel — reaproveita a
     * mesma identidade visual ([id]) cadastrada em [lista]/[OperadoraLogoCatalog], já que a
     * marca é a mesma independente da linha de produto.
     */
    private val prefixosMovel: Map<String, List<String>> =
        mapOf(
            "vivo_fibra" to listOf("vivo"),
            "claro_net" to listOf("claro"),
            "tim_live" to listOf("tim"),
            "oi_fibra" to listOf("oi"),
        )

    /**
     * Resolve o nome de operadora móvel (`TelephonyManager`/lookup de IP) para a mesma
     * [ContatoOperadora] da linha fixa. Não reaproveita [resolver]: nomes de operadora móvel
     * chegam concatenados sem separador (ex.: "TIMBRASIL"), então o `\b` de [resolver] nunca
     * bate — aqui o match é por prefixo simples.
     */
    fun resolverMovel(nomeOperadora: String?): ContatoOperadora? {
        if (nomeOperadora.isNullOrBlank()) return null
        val normalizado = nomeOperadora.lowercase().trim()
        return lista.firstOrNull { op ->
            prefixosMovel[op.id]?.any { normalizado.startsWith(it) } == true
        }
    }
}
