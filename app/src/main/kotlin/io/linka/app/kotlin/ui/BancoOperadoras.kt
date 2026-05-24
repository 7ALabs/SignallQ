package io.linka.app.kotlin.ui

data class ContatoOperadora(
    val id: String,
    val nome: String,
    val grupo: String,
    val detectarPor: List<String>,
    val sac: String,
    val whatsapp: String?,
)

object BancoOperadoras {
    val lista =
        listOf(
            ContatoOperadora(
                "vivo_fibra",
                "Vivo Fibra",
                "Vivo / Telefônica",
                listOf("vivo", "telefônica", "telefonica", "gvt"),
                sac = "1058",
                whatsapp = "11940209030",
            ),
            ContatoOperadora(
                "claro_net",
                "Claro / NET",
                "Claro",
                listOf("claro", "net virtua", "embratel"),
                sac = "1052",
                whatsapp = "21971500120",
            ),
            ContatoOperadora(
                "tim_live",
                "TIM Live",
                "TIM",
                listOf("tim live", "tim"),
                sac = "1056",
                whatsapp = "41999310056",
            ),
            ContatoOperadora(
                "oi_fibra",
                "Oi Fibra / NIO",
                "Oi",
                listOf("oi fibra", "oi", "nio", "telemar"),
                sac = "1031",
                whatsapp = "21971500031",
            ),
            ContatoOperadora(
                "sky",
                "Sky",
                "Sky",
                listOf("sky", "directv"),
                sac = "1062",
                whatsapp = null,
            ),
            ContatoOperadora(
                "algar",
                "Algar Telecom",
                "Algar",
                listOf("algar"),
                sac = "08007224242",
                whatsapp = null,
            ),
            ContatoOperadora(
                "brisanet",
                "Brisanet",
                "Brisanet",
                listOf("brisanet"),
                sac = "08005911777",
                whatsapp = "8540203455",
            ),
            ContatoOperadora(
                "ligga",
                "Ligga Telecom",
                "Ligga",
                listOf("ligga", "copel telecom"),
                sac = "08008887777",
                whatsapp = null,
            ),
            ContatoOperadora(
                "sumicity",
                "Sumicity",
                "Sumicity",
                listOf("sumicity"),
                sac = "08009401000",
                whatsapp = null,
            ),
            ContatoOperadora(
                "desktop",
                "Desktop / Nextel",
                "Desktop",
                listOf("desktop", "nextel"),
                sac = "08007269697",
                whatsapp = null,
            ),
            ContatoOperadora(
                "unifique",
                "Unifique",
                "Unifique",
                listOf("unifique"),
                sac = "08006002200",
                whatsapp = null,
            ),
            ContatoOperadora(
                "mob",
                "Mob Telecom",
                "Mob",
                listOf("mob telecom", "mob"),
                sac = "08006461010",
                whatsapp = null,
            ),
            ContatoOperadora(
                "giga_mais",
                "Giga+ Fibra",
                "Giga+",
                listOf("giga+", "giga mais", "giga fibra"),
                sac = "08008889494",
                whatsapp = null,
            ),
            ContatoOperadora(
                "eletronet",
                "Eletronet",
                "Eletronet",
                listOf("eletronet"),
                sac = "08000202012",
                whatsapp = null,
            ),
            ContatoOperadora(
                "vogel",
                "Vogel Telecom",
                "Vogel",
                listOf("vogel"),
                sac = "08009405000",
                whatsapp = null,
            ),
            ContatoOperadora(
                "tely",
                "Tely",
                "Tely",
                listOf("tely"),
                sac = "08009501200",
                whatsapp = null,
            ),
        )

    fun resolver(ispNome: String?): ContatoOperadora? {
        if (ispNome.isNullOrBlank()) return null
        val normalizado = ispNome.lowercase().trim()
        return lista.firstOrNull { op -> op.detectarPor.any { termo -> normalizado.contains(termo) } }
    }
}
