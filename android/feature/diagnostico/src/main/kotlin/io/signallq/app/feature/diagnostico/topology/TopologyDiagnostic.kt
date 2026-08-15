package io.signallq.app.feature.diagnostico.topology

import android.content.Context
import io.signallq.app.core.diagnostico.topology.correlation.NatClassifier
import io.signallq.app.core.diagnostico.topology.correlation.TopologyTracer
import io.signallq.app.core.diagnostico.topology.internet.GeoIpResolver
import io.signallq.app.core.diagnostico.topology.internet.PublicIpResolver
import io.signallq.app.feature.diagnostico.topology.lan.GatewayResolver
import io.signallq.app.feature.diagnostico.topology.lan.MeshDetector
import io.signallq.app.feature.diagnostico.topology.lan.OuiVendorLookup
import io.signallq.app.feature.diagnostico.topology.lan.UpnpIgdDiscovery
import io.signallq.app.feature.diagnostico.topology.lan.UpnpSoapClient
import io.signallq.app.core.diagnostico.topology.model.DeviceInfo
import io.signallq.app.core.diagnostico.topology.model.NetworkTopology
import io.signallq.app.core.network.contracts.wifi.RedeVizinha
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient

class TopologyDiagnostic(
    context: Context,
    httpClient: OkHttpClient,
    private val networks: List<RedeVizinha> = emptyList()
) {
    private val gatewayResolver = GatewayResolver(context)
    private val upnpDiscovery = UpnpIgdDiscovery(context, httpClient)
    private val soapClient = UpnpSoapClient()
    private val ouiLookup = OuiVendorLookup.fromAssets(context)
    private val meshDetector = MeshDetector(ouiLookup)
    private val publicIpResolver = PublicIpResolver()
    private val geoIpResolver = GeoIpResolver()

    suspend fun diagnose(): NetworkTopology = coroutineScope {
        // LAN e Internet em paralelo
        val gatewayIpDeferred = async { gatewayResolver.resolve() }
        val upnpDeferred = async { upnpDiscovery.discover() }
        val publicIpDeferred = async { publicIpResolver.resolve() }
        val geoDeferred = async { geoIpResolver.resolve() }
        val traceDeferred = async { TopologyTracer.trace() }

        val gatewayIp = gatewayIpDeferred.await()
        val upnpInfo = upnpDeferred.await()
        val publicIp = publicIpDeferred.await()
        val geoIp = geoDeferred.await()
        val traceHops = traceDeferred.await()

        // WAN IP via SOAP só se IGD respondeu com controlUrl
        val wanIp = upnpInfo?.controlUrl?.let { soapClient.getExternalIpAddress(it) }

        val vendor = gatewayIp?.let { ouiLookup.lookup(it) }
            ?: upnpInfo?.manufacturer?.let { ouiLookup.lookup(it) }

        val router = if (gatewayIp != null || upnpInfo != null) {
            DeviceInfo(
                ip = gatewayIp,
                mac = null,
                vendor = vendor,
                friendlyName = upnpInfo?.friendlyName,
                manufacturer = upnpInfo?.manufacturer,
                model = listOfNotNull(upnpInfo?.modelName, upnpInfo?.modelNumber)
                    .joinToString(" ").takeIf { it.isNotBlank() }
            )
        } else null

        val meshNodes = meshDetector.detectSatellites(networks, gatewayIp)

        NetworkTopology(
            gatewayIp = gatewayIp,
            wanIp = wanIp,
            publicIp = publicIp,
            router = router,
            meshNodes = meshNodes,
            nat = NatClassifier.classify(wanIp, publicIp),
            isp = geoIp?.isp,
            region = geoIp?.region,
            traceHops = traceHops
        )
    }
}
