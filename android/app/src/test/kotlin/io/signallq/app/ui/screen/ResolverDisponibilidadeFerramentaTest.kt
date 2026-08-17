package io.signallq.app.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de caracterização de [resolverDisponibilidadeFerramenta] — issue #1698, épico #1647.
 *
 * A regra vivia como lambda inline dentro de `AppShell.kt` (fatia 2.0.05), onde só podia ser
 * exercitada compondo o shell inteiro. Extraída para função pura, cada ramo vira teste barato e
 * determinístico — este arquivo é a razão prática de a extração valer a pena, não só a contagem
 * de linhas.
 *
 * O que está travado aqui é a **ordem de precedência**: flag remota desligada vence falta de
 * conexão, que vence falta de permissão. Trocar essa ordem muda a mensagem que o usuário lê sem
 * quebrar compilação — exatamente o tipo de regressão silenciosa que a issue pede para prevenir.
 */
class ResolverDisponibilidadeFerramentaTest {
    private fun flags(
        wifi: Boolean = true,
        devices: Boolean = true,
        dns: Boolean = true,
        fibra: Boolean = true,
        diagnostico: Boolean = true,
        settings: Boolean = true,
    ) = AppShellFeatureFlagsState(
        wifiEnabled = wifi,
        devicesEnabled = devices,
        dnsEnabled = dns,
        fibraEnabled = fibra,
        diagnosticoEnabled = diagnostico,
        settingsEnabled = settings,
    )

    private fun resolver(
        tipo: TipoFerramenta,
        featureFlags: AppShellFeatureFlagsState = flags(),
        conectado: Boolean = true,
        temPermissaoLocalizacao: Boolean = true,
    ) = resolverDisponibilidadeFerramenta(tipo, featureFlags, conectado, temPermissaoLocalizacao)

    @Test
    fun `tudo ligado conectado e com permissao deixa as nove ferramentas disponiveis`() {
        TipoFerramenta.entries.forEach { tipo ->
            assertEquals(
                "esperado Disponivel para $tipo",
                FerramentaDisponibilidade.Disponivel,
                resolver(tipo),
            )
        }
    }

    @Test
    fun `cada ferramenta le a flag do seu proprio modulo`() {
        // Trava o mapeamento tipo -> flag: trocar wifiEnabled por devicesEnabled em qualquer
        // linha do `when` passaria despercebido sem esta asserção por ferramenta.
        val esperado =
            mapOf(
                TipoFerramenta.SINAL_CANAIS_MOVEL to flags(wifi = false),
                TipoFerramenta.SINAL_WIFI to flags(wifi = false),
                TipoFerramenta.DISPOSITIVOS to flags(devices = false),
                TipoFerramenta.EQUIPAMENTO_INTERNET to flags(fibra = false),
                TipoFerramenta.DNS to flags(dns = false),
                TipoFerramenta.LAUDO to flags(diagnostico = false),
                TipoFerramenta.MONITORAMENTO to flags(settings = false),
            )
        esperado.forEach { (tipo, featureFlags) ->
            assertTrue(
                "$tipo deveria ficar IndisponivelRemotamente com a flag do seu modulo desligada",
                resolver(tipo, featureFlags = featureFlags) is FerramentaDisponibilidade.IndisponivelRemotamente,
            )
        }
    }

    @Test
    fun `ping e modo gamer nao sao gateados por flag remota`() {
        // Único par sem módulo `:feature:*` correspondente no catálogo — o `when` devolve `true`
        // fixo para eles. Desligar tudo não pode torná-los indisponíveis por flag.
        val todasDesligadas =
            flags(
                wifi = false,
                devices = false,
                dns = false,
                fibra = false,
                diagnostico = false,
                settings = false,
            )
        assertEquals(
            FerramentaDisponibilidade.Disponivel,
            resolver(TipoFerramenta.PING, featureFlags = todasDesligadas),
        )
        assertEquals(
            FerramentaDisponibilidade.Disponivel,
            resolver(TipoFerramenta.MODO_JOGOS, featureFlags = todasDesligadas),
        )
    }

    @Test
    fun `sem conexao apenas as quatro ferramentas que dependem de rede ficam offline`() {
        val dependemDeRede =
            setOf(
                TipoFerramenta.EQUIPAMENTO_INTERNET,
                TipoFerramenta.PING,
                TipoFerramenta.DNS,
                TipoFerramenta.MODO_JOGOS,
            )
        TipoFerramenta.entries.forEach { tipo ->
            val resultado = resolver(tipo, conectado = false)
            if (tipo in dependemDeRede) {
                assertTrue("$tipo deveria ficar Offline", resultado is FerramentaDisponibilidade.Offline)
            } else {
                assertTrue(
                    "$tipo nao depende de conexao e nao deveria ficar Offline",
                    resultado !is FerramentaDisponibilidade.Offline,
                )
            }
        }
    }

    @Test
    fun `sem permissao de localizacao apenas as tres ferramentas de varredura pedem permissao`() {
        val precisamDeVarredura =
            setOf(
                TipoFerramenta.SINAL_CANAIS_MOVEL,
                TipoFerramenta.SINAL_WIFI,
                TipoFerramenta.DISPOSITIVOS,
            )
        TipoFerramenta.entries.forEach { tipo ->
            val resultado = resolver(tipo, temPermissaoLocalizacao = false)
            if (tipo in precisamDeVarredura) {
                assertTrue(
                    "$tipo deveria pedir permissao",
                    resultado is FerramentaDisponibilidade.PermissaoNecessaria,
                )
            } else {
                assertEquals("$tipo nao depende de localizacao", FerramentaDisponibilidade.Disponivel, resultado)
            }
        }
    }

    @Test
    fun `flag desligada vence falta de conexao e falta de permissao`() {
        // Precedência 1: mesmo offline e sem permissão, a flag remota é a mensagem exibida.
        val resultado =
            resolver(
                TipoFerramenta.DNS,
                featureFlags = flags(dns = false),
                conectado = false,
                temPermissaoLocalizacao = false,
            )
        assertTrue(resultado is FerramentaDisponibilidade.IndisponivelRemotamente)
    }

    @Test
    fun `falta de conexao vence falta de permissao`() {
        // Precedência 2. `DISPOSITIVOS` é o caso que distingue as duas regras: precisa de
        // permissão, mas NÃO está na lista das que dependem de conexão — então offline sem
        // permissão ele ainda pede permissão, não fica Offline.
        assertTrue(
            resolver(TipoFerramenta.DISPOSITIVOS, conectado = false, temPermissaoLocalizacao = false)
                is FerramentaDisponibilidade.PermissaoNecessaria,
        )
        // Já `SINAL_WIFI` também não depende de conexão — mesma conclusão.
        assertTrue(
            resolver(TipoFerramenta.SINAL_WIFI, conectado = false, temPermissaoLocalizacao = false)
                is FerramentaDisponibilidade.PermissaoNecessaria,
        )
    }

    @Test
    fun `textos de proximo passo nao regridem`() {
        // A copy é o que o usuário lê no card indisponível — trocá-la sem querer é regressão de
        // produto, não de arquitetura.
        assertEquals(
            "Tente novamente mais tarde.",
            (resolver(TipoFerramenta.DNS, featureFlags = flags(dns = false)) as FerramentaDisponibilidade.IndisponivelRemotamente)
                .proximoPasso,
        )
        assertEquals(
            "Reconecte-se e tente novamente.",
            (resolver(TipoFerramenta.PING, conectado = false) as FerramentaDisponibilidade.Offline).proximoPasso,
        )
        assertEquals(
            "Abra para permitir redes próximas.",
            (
                resolver(TipoFerramenta.SINAL_WIFI, temPermissaoLocalizacao = false)
                    as FerramentaDisponibilidade.PermissaoNecessaria
            ).proximoPasso,
        )
    }
}
