package io.veloo.app.feature.diagnostico.topology.model

enum class NatStatus { DIRECT_PUBLIC, CGNAT, DOUBLE_NAT_OR_CGNAT, UNKNOWN }

data class DeviceInfo(
    val ip: String?,
    val mac: String?,
    val vendor: String?,
    val friendlyName: String?,
    val manufacturer: String?,
    val model: String?
)

data class NetworkTopology(
    val gatewayIp: String?,
    val wanIp: String?,        // do IGD GetExternalIPAddress
    val publicIp: String?,     // serviço externo
    val router: DeviceInfo?,
    val meshNodes: List<DeviceInfo>,
    val nat: NatStatus,
    val isp: String?,
    val region: String?,
    val traceHops: List<String>?
)
