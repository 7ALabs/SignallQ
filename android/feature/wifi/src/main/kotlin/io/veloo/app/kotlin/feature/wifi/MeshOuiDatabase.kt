package io.signallq.app.feature.wifi

object MeshOuiDatabase {

    // Sistemas mesh conhecidos — nós secundários
    val MESH_NO_OUIS: Set<String> = setOf(
        // Google Nest WiFi / Google WiFi
        "F4F5D8", "1AC487", "48D705", "94EB2C", "A4977A",
        // TP-Link Deco
        "50C7BF", "B09575", "C006C3", "E8DE27", "1027F5", "A42BB0", "74DA88",
        // Eero (Amazon)
        "F4F5E8", "98F1F2", "8C8590", "7C571C",
        // Orbi (Netgear)
        "9C3DCF", "B0B98A", "28C68E", "A040A0",
        // Intelbras ACMesh
        "C46E1F", "6C5AB0",
        // Ubiquiti UniFi APs
        "788A20", "24A43C", "E063DA", "802AA8", "74ACB9", "F09FC2",
        // Cisco/Meraki
        "0CE76F", "8843E1", "34BD3B",
        // Aruba/HP
        "D8C7C8", "6C722D", "24DEC6",
    )

    // Gateways/roteadores ISP Brasil — provavelmente roteador principal
    val GATEWAY_ISP_OUIS: Set<String> = setOf(
        // Intelbras (muito comum em ISPs regionais BR)
        "C46E1F", "B4A9FC", "6C5AB0",
        // ZTE (Vivo, Claro, TIM fibra)
        "2C26C5", "00E0FC", "ACF7F3", "14ADED", "88B111",
        // Huawei (Vivo, Oi)
        "286ED4", "9C74B0", "D440F0", "4C8BEF", "A0808F",
        // Nokia/Alcatel-Lucent (Oi, TIM)
        "001BC5", "A4CAA0", "3C8CF8",
        // Sagemcom (Sky, operadoras regionais)
        "549F13", "D86CE9", "B4A512",
        // Technicolor/Thomson
        "10BF48", "30D3C7", "E49665",
        // Mitrastar (ISPs regionais)
        "000AE7", "B0487A",
        // D-Link
        "14D64D", "1C7EE5", "28107B", "84C9B2",
        // TP-Link roteadores (não Deco)
        "50FA84", "10BEF5", "A0F3C1", "04D9F5",
    )

    fun isMeshNo(oui: String): Boolean = oui.uppercase() in MESH_NO_OUIS
    fun isGatewayIsp(oui: String): Boolean = oui.uppercase() in GATEWAY_ISP_OUIS
}
