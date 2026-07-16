package io.signallq.app.ui

data class ContatoOperadora(
    val id: String,
    val nome: String,
    val grupo: String,
    val detectarPor: List<String>,
    val sac: String?,
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
                "nio",
                "Nio Fibra",
                "Nio",
                // "oi"/"oi fibra"/"telemar" cobrem dado legado de roteador/API que ainda reporta o
                // nome antigo -- a Oi foi rebrandeada para Nio, nao existe mais como marca separada.
                listOf("nio", "nio internet", "nio fibra", "oi fibra", "oi", "telemar"),
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
            ContatoOperadora(
                "wlinks",
                "WLINKS",
                "Wlinks Internet",
                listOf("wlinks", "wlinks internet"),
                sac = "08000420227",
                whatsapp = "08000420227",
                site = "https://www.wlinks.com.br",
            ),
            ContatoOperadora(
                "amigo",
                "Amigo Internet",
                "Brasil TecPar",
                listOf("amigo", "amigo internet", "brasil tecpar"),
                sac = "08006454200",
                whatsapp = "8006454200",
                site = "https://www.sejaamigo.com.br",
            ),
            ContatoOperadora(
                "viamar",
                "Viamar Telecom",
                "Viamar Internet Banda Larga / NWA Telecomunicações",
                listOf("viamar", "viamar telecom", "nwa telecomunicacoes"),
                sac = "2226305230",
                whatsapp = "22988328168",
                site = "https://www.viamartelecom.com.br",
            ),
            ContatoOperadora(
                "west_telecom",
                "West Telecom",
                "West Comércio e Serviços de Telecomunicações",
                listOf("west telecom", "westtelecom"),
                sac = "2124066200",
                whatsapp = "21985219472",
                site = "https://westtelecom.net",
            ),
            ContatoOperadora(
                "west_fibra",
                "West Fibra",
                "West Fibra",
                // Empresa distinta da "West Telecom" acima (CNPJ diferente), confirmado no site.
                listOf("west fibra", "westfibra"),
                sac = "2138339001",
                whatsapp = null,
                site = "https://westfibra.com.br",
            ),
            ContatoOperadora(
                "fhp_fibra",
                "FHP Fibra",
                "FHP Telecomunicações",
                listOf("fhp fibra", "fhp telecomunicacoes", "fhp"),
                sac = "2135128383",
                whatsapp = "2135128383",
                site = "https://www.fhpfibra.com.br",
            ),
            ContatoOperadora(
                "topfibra",
                "Top Fibra",
                "Top Fibra Internet",
                listOf("top fibra", "topfibra", "top fibra internet"),
                sac = "2231990001",
                whatsapp = "2231990001",
                site = "https://topfibrarj.net.br",
            ),
            ContatoOperadora(
                "coopertec_speed",
                "Coopertec SPEED",
                "NC Brasil Telecom e Serviços",
                // ISP local que revende a rede da Turbi (por isso a logo é a da Turbi, não uma
                // marca própria) -- mantém "turbi" nas palavras-chave pois é o que pode aparecer
                // em lookup de rede/WHOIS, mesmo com o nome exibido sendo "Coopertec SPEED".
                listOf("coopertec speed", "coopertec", "nc brasil telecom", "turbi"),
                sac = null,
                whatsapp = "21971609082",
                site = "https://coopertec.atlaz.com.br",
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
            // "oi_fibra" removido (rebrand pra Nio, fibra-only) -- Nio nao tem produto movel
            // conhecido sob a mesma marca, entao nao redireciona "oi" pra "nio" aqui. Se a Oi
            // Movel (marca separada, grupo diferente) precisar de deteccao propria, cadastrar
            // como operadora nova, nao reaproveitar o id "nio".
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
