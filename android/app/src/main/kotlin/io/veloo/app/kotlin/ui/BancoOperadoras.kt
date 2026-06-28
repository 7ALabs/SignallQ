package io.signallq.app.ui

data class ContatoOperadora(
    val id: String,
    val nome: String,
    val grupo: String,
    val detectarPor: List<String>,
    val sac: String,
    val whatsapp: String?,
    val logoUrl: String? = null,
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
                logoUrl = "https://www.google.com/s2/favicons?domain=vivo.com.br&sz=128",
            ),
            ContatoOperadora(
                "claro_net",
                "Claro",
                "Claro",
                listOf("claro", "net virtua", "embratel", "net serviços", "net servicos"),
                sac = "10621",
                whatsapp = "11999910621",
                logoUrl = "https://www.google.com/s2/favicons?domain=claro.com.br&sz=128",
            ),
            ContatoOperadora(
                "tim_live",
                "TIM",
                "TIM",
                listOf("tim live", "tim"),
                sac = "10341",
                whatsapp = "41414141414",
                logoUrl = "https://www.google.com/s2/favicons?domain=tim.com.br&sz=128",
            ),
            ContatoOperadora(
                "oi_fibra",
                "Oi",
                "Oi",
                listOf("oi fibra", "oi", "telemar"),
                sac = "10331",
                whatsapp = null,
                logoUrl = "https://www.google.com/s2/favicons?domain=oi.com.br&sz=128",
            ),
            ContatoOperadora(
                "nio",
                "Nio Fibra",
                "Nio",
                listOf("nio", "nio internet", "nio fibra"),
                sac = "08000011000",
                whatsapp = "2136051000",
                logoUrl = "https://www.google.com/s2/favicons?domain=niointernet.com.br&sz=128",
            ),
            ContatoOperadora(
                "algar",
                "Algar Telecom",
                "Algar",
                listOf("algar"),
                sac = "08009421212",
                whatsapp = "34998840123",
                logoUrl = "https://www.google.com/s2/favicons?domain=algartelecom.com.br&sz=128",
            ),
            ContatoOperadora(
                "unifique",
                "Unifique",
                "Unifique",
                listOf("unifique"),
                sac = "10580",
                whatsapp = "4733800800",
                logoUrl = "https://www.google.com/s2/favicons?domain=unifique.com.br&sz=128",
            ),
            ContatoOperadora(
                "brisanet",
                "Brisanet",
                "Brisanet",
                listOf("brisanet"),
                sac = "10517",
                whatsapp = "84981118525",
                logoUrl = "https://www.google.com/s2/favicons?domain=brisanet.com.br&sz=128",
            ),
            ContatoOperadora(
                "desktop",
                "Desktop",
                "Desktop",
                listOf("desktop", "nextel"),
                sac = "10344",
                whatsapp = "1935143100",
                logoUrl = "https://www.google.com/s2/favicons?domain=desktop.com.br&sz=128",
            ),
            ContatoOperadora(
                "ligga",
                "Ligga Telecom",
                "Ligga",
                listOf("ligga", "copel telecom"),
                sac = "08004141810",
                whatsapp = null,
                logoUrl = "https://www.google.com/s2/favicons?domain=liggavc.com.br&sz=128",
            ),
            ContatoOperadora(
                "vero",
                "Vero Internet",
                "Vero",
                listOf("vero", "vero internet"),
                sac = "10385",
                whatsapp = null,
                logoUrl = "https://www.google.com/s2/favicons?domain=verointernet.com.br&sz=128",
            ),
            ContatoOperadora(
                "giga_mais",
                "Giga+ Fibra",
                "Giga+ / Sumicity",
                listOf("giga+", "giga mais", "giga fibra", "sumicity"),
                sac = "10353",
                whatsapp = "22920410350",
                logoUrl = "https://www.google.com/s2/favicons?domain=sumicity.com.br&sz=128",
            ),
        )

    fun resolver(ispNome: String?): ContatoOperadora? {
        if (ispNome.isNullOrBlank()) return null
        val normalizado = ispNome.lowercase().trim()
        return lista.firstOrNull { op -> op.detectarPor.any { termo -> normalizado.contains(termo) } }
    }
}
