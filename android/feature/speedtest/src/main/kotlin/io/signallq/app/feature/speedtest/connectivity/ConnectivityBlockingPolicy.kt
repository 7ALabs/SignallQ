package io.signallq.app.feature.speedtest.connectivity

import io.signallq.app.core.network.contracts.connectivity.ConnectivityDiagnosis
import io.signallq.app.core.network.contracts.connectivity.ConnectivityStatus
import io.signallq.app.core.network.contracts.topologia.NivelConfianca

/**
 * Decide se um [ConnectivityDiagnosis] é evidência suficientemente forte de ausência de
 * internet para interromper um Speedtest (GH#1512). Extraída de `MainViewModel`/
 * `SpeedtestViewModel` (que duplicavam esta lógica de forma idêntica -- achado de revisão
 * R-6) para ficar testável sem depender de infraestrutura de teste de ViewModel/Hilt.
 *
 * Regra (3ª revisão adversarial, GH#1512): decidir só por `status` cria uma assimetria
 * indefensável -- `DNS_FAILURE` pode ter confiança MEDIA com ZERO sondagem feita (nenhum
 * servidor DNS configurado), enquanto `GATEWAY_UNREACHABLE` pode ter confiança ALTA
 * (sondagem TCP ativa falhou) mas nunca teria bloqueado nada antes desta correção --
 * silenciando o app numa fatia real dos casos "Wi-Fi sem internet" (o motor curto-circuita
 * DNS/rota externa após falha do gateway, então esse é o ÚNICO sinal disponível ali). Por
 * isso: exige confiança ALTA para os estados de evidência mais fraca, e para
 * `GATEWAY_UNREACHABLE` exige também que o próprio Android corrobore (`!androidValidated`)
 * -- dois sinais independentes concordando, não um proxy TCP isolado que um roteador
 * legitimamente saudável pode falhar (ex.: sem admin em 53/80/443).
 *
 * Correção (4ª revisão, achado de revisão): a condição original exigia também
 * `androidInternetCapability == true`, o que excluía o caso real de
 * `getNetworkCapabilities()` devolver `null` (rede desapareceu no meio da checagem --
 * [io.signallq.app.core.network.connectivity.ConnectivityDiagnosisRunner] zera os dois
 * flags nesse caso). Isso não é "Android discordando" -- é "nenhum sinal do Android", e
 * ficava de fora da corroboração por engano, criando exatamente o mesmo buraco de
 * silêncio que esta política existe para fechar. `!androidValidated` sozinho já basta:
 * significa que o Android não confirmou internet funcional nesta rede, com ou sem a
 * capability declarada.
 */
fun ConnectivityDiagnosis.indicaAusenciaDeInternetParaBloquearSpeedtest(): Boolean =
    when (status) {
        ConnectivityStatus.WIFI_WITHOUT_INTERNET,
        ConnectivityStatus.EXTERNAL_ROUTE_FAILURE,
        ConnectivityStatus.CAPTIVE_PORTAL,
        ConnectivityStatus.NO_LOCAL_ADDRESS,
        -> true

        ConnectivityStatus.DNS_FAILURE,
        -> confidence == NivelConfianca.ALTA

        ConnectivityStatus.GATEWAY_UNREACHABLE,
        -> confidence == NivelConfianca.ALTA && !androidValidated

        ConnectivityStatus.INTERNET_AVAILABLE,
        ConnectivityStatus.PARTIAL_CONNECTIVITY,
        ConnectivityStatus.INCONCLUSIVE,
        ConnectivityStatus.WIFI_DISCONNECTED,
        -> false
    }
