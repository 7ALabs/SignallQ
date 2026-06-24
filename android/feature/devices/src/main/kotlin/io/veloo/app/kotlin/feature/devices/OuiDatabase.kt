package io.veloo.app.feature.devices

// Base curada de prefixos OUI (primeiros 6 hex do MAC, sem dois pontos, uppercase).
// Fonte: IEEE OUI registry — ~130 prefixos de maior ocorrência em redes domésticas brasileiras.
internal object OuiDatabase {

    private val db: Map<String, String> = mapOf(
        // Apple
        "000393" to "Apple", "000A27" to "Apple", "000A95" to "Apple",
        "001124" to "Apple", "001451" to "Apple", "0016CB" to "Apple",
        "0017F2" to "Apple", "001EC2" to "Apple", "001F5B" to "Apple",
        "002312" to "Apple", "002500" to "Apple", "002608" to "Apple",
        "0026BB" to "Apple", "003065" to "Apple", "040CCE" to "Apple",
        "101AB0" to "Apple", "14499E" to "Apple", "1893E4" to "Apple",
        "20AB37" to "Apple", "28CFE9" to "Apple", "2CAB25" to "Apple",
        "3C0754" to "Apple", "40A6D9" to "Apple", "44D884" to "Apple",
        "4C8D79" to "Apple", "50EAD6" to "Apple", "5C95AE" to "Apple",
        "6CBE3B" to "Apple", "70DEE2" to "Apple", "748114" to "Apple",
        "7C6D62" to "Apple", "88655D" to "Apple", "8C2937" to "Apple",
        "90C1C6" to "Apple", "9803D8" to "Apple", "9C35EB" to "Apple",
        "A45E60" to "Apple", "A82066" to "Apple", "AC3C0B" to "Apple",
        "ACB589" to "Apple", "B418D1" to "Apple", "B8C75D" to "Apple",
        "BC926B" to "Apple", "C41326" to "Apple", "C82A14" to "Apple",
        "CC08E0" to "Apple", "D49A20" to "Apple", "D83062" to "Apple",
        "DC2B2A" to "Apple", "E0B9BA" to "Apple", "E45F01" to "Apple",
        "E885D7" to "Apple", "ECAD9F" to "Apple", "F0D1A9" to "Apple",
        "F40B93" to "Apple", "F4F5DB" to "Apple", "F81EDF" to "Apple",
        // Samsung
        "001247" to "Samsung", "001599" to "Samsung", "001632" to "Samsung",
        "001A8A" to "Samsung", "001E75" to "Samsung", "0021D1" to "Samsung",
        "00233A" to "Samsung", "002454" to "Samsung", "0026E2" to "Samsung",
        "00E064" to "Samsung", "1CAFF7" to "Samsung", "2843FE" to "Samsung",
        "34885D" to "Samsung", "38016B" to "Samsung", "3CBDD8" to "Samsung",
        "480FD3" to "Samsung", "4C3C16" to "Samsung", "508569" to "Samsung",
        "5454CE" to "Samsung", "5CA399" to "Samsung", "606BBD" to "Samsung",
        "6C2F2C" to "Samsung", "74458A" to "Samsung", "7825AD" to "Samsung",
        "7C1D9A" to "Samsung", "84388B" to "Samsung", "8C71F8" to "Samsung",
        "904CE5" to "Samsung", "9C0298" to "Samsung", "A01022" to "Samsung",
        "B47443" to "Samsung", "B8C68E" to "Samsung", "BC20A4" to "Samsung",
        "C81479" to "Samsung", "CCC9E3" to "Samsung", "D8C4E9" to "Samsung",
        "E4F8EF" to "Samsung", "F8043E" to "Samsung",
        // Xiaomi
        "0C1DAF" to "Xiaomi", "286ED4" to "Xiaomi", "34CE00" to "Xiaomi",
        "3480B3" to "Xiaomi", "50EC50" to "Xiaomi", "64B473" to "Xiaomi",
        "6C5AB5" to "Xiaomi", "7C1DD9" to "Xiaomi", "8C97EA" to "Xiaomi",
        "AC2339" to "Xiaomi", "C40BCB" to "Xiaomi", "D46107" to "Xiaomi",
        "F048EF" to "Xiaomi", "F4F5E8" to "Xiaomi",
        // Huawei
        "001E10" to "Huawei", "0025D3" to "Huawei", "002EC7" to "Huawei",
        "101B54" to "Huawei", "1C3350" to "Huawei", "28311E" to "Huawei",
        "2C9DFE" to "Huawei", "34DB7B" to "Huawei", "4062F9" to "Huawei",
        "4C1FCC" to "Huawei", "545B6A" to "Huawei", "609F9D" to "Huawei",
        "688ACA" to "Huawei", "70723C" to "Huawei", "7C60D7" to "Huawei",
        "88E3AB" to "Huawei", "8CAB8E" to "Huawei", "A0D5B1" to "Huawei",
        "B4430D" to "Huawei", "CC96A0" to "Huawei", "D4F9A1" to "Huawei",
        "E8CD2D" to "Huawei", "F46DEA" to "Huawei", "F8E811" to "Huawei",
        // Google / Nest
        "001A11" to "Google", "00F15B" to "Google", "083D88" to "Google",
        "1C9E46" to "Google", "20DF3B" to "Google", "3C5AB4" to "Google",
        "48D705" to "Google", "54600A" to "Google", "60A4D0" to "Google",
        "6C4008" to "Google", "7C2264" to "Google", "94EB2C" to "Google",
        "A47733" to "Google", "B4F1DA" to "Google", "D8D43C" to "Google",
        "F88FCA" to "Google",
        // Amazon / Alexa / Echo
        "0003E0" to "Amazon", "40B4CD" to "Amazon", "44650D" to "Amazon",
        "A002DC" to "Amazon", "AC63BE" to "Amazon", "B47C9C" to "Amazon",
        "CCF7AB" to "Amazon", "F0272D" to "Amazon", "FC65DE" to "Amazon",
        // TP-Link
        "003FDF" to "TP-Link", "14CC20" to "TP-Link", "1C61B4" to "TP-Link",
        "2C56DC" to "TP-Link", "4899DA" to "TP-Link", "50C7BF" to "TP-Link",
        "54AF97" to "TP-Link", "60E3AC" to "TP-Link", "6466B3" to "TP-Link",
        "700F6A" to "TP-Link", "74DA38" to "TP-Link", "94D9B3" to "TP-Link",
        "980D67" to "TP-Link", "9CB70D" to "TP-Link", "A0F3C1" to "TP-Link",
        "B0487A" to "TP-Link", "C46E1F" to "TP-Link", "F4F26D" to "TP-Link",
        // ASUS
        // Nota: 2C56DC foi removido daqui — pertence à TP-Link (IEEE OUI registry).
        "001A92" to "ASUS", "04924B" to "ASUS", "08606E" to "ASUS",
        "10BF48" to "ASUS", "1062E5" to "ASUS",
        "38D547" to "ASUS", "40167E" to "ASUS", "487727" to "ASUS",
        "50465D" to "ASUS", "548141" to "ASUS", "60A44C" to "ASUS",
        "6045CB" to "ASUS", "74D02B" to "ASUS", "90E6BA" to "ASUS",
        "9C5C8E" to "ASUS", "B06EBF" to "ASUS", "D850E6" to "ASUS",
        "E03F49" to "ASUS", "F04DA2" to "ASUS",
        // D-Link
        "000D88" to "D-Link", "001195" to "D-Link", "001346" to "D-Link",
        "00179A" to "D-Link", "001BED" to "D-Link", "001CF0" to "D-Link",
        "001E58" to "D-Link", "00215D" to "D-Link", "0022B0" to "D-Link",
        "002401" to "D-Link", "0026F2" to "D-Link", "1C7EE5" to "D-Link",
        "28107B" to "D-Link", "48EE0C" to "D-Link", "5CD998" to "D-Link",
        "9094E4" to "D-Link", "B8A386" to "D-Link", "C8BE19" to "D-Link",
        // Netgear
        "00146C" to "Netgear", "001E2A" to "Netgear", "001F33" to "Netgear",
        "002275" to "Netgear", "00224E" to "Netgear", "002722" to "Netgear",
        "0846D0" to "Netgear", "10BEDC" to "Netgear", "1E8F5B" to "Netgear",
        "20E52A" to "Netgear", "28C68E" to "Netgear", "44940C" to "Netgear",
        "4F47B7" to "Netgear", "6027AA" to "Netgear", "84948E" to "Netgear",
        "9C3DCF" to "Netgear", "A040A0" to "Netgear", "C03F0E" to "Netgear",
        // Cisco
        "000142" to "Cisco", "000299" to "Cisco", "00036B" to "Cisco",
        "000DED" to "Cisco", "000F90" to "Cisco", "001201" to "Cisco",
        "0013C3" to "Cisco", "0014A9" to "Cisco", "001A2F" to "Cisco",
        "001CB1" to "Cisco", "001E13" to "Cisco", "001F26" to "Cisco",
        "002035" to "Cisco", "002126" to "Cisco", "0022BE" to "Cisco",
        "002390" to "Cisco", "0024F7" to "Cisco", "0026CB" to "Cisco",
        "2C3F38" to "Cisco", "58AC78" to "Cisco", "70CA9B" to "Cisco",
        // Intel (chips Wi-Fi em notebooks)
        "001B21" to "Intel", "001F3B" to "Intel", "002168" to "Intel",
        "002311" to "Intel", "0024D7" to "Intel", "00269E" to "Intel",
        "080027" to "Intel", "1036BA" to "Intel", "18AB56" to "Intel",
        "2C6E85" to "Intel", "34029B" to "Intel", "4C7985" to "Intel",
        "5CF951" to "Intel", "60F677" to "Intel", "706655" to "Intel",
        "7085C2" to "Intel", "8086F2" to "Intel", "8C8D28" to "Intel",
        "94659C" to "Intel", "98F1EB" to "Intel", "A4C3F0" to "Intel",
        "B0C9AB" to "Intel", "B8770E" to "Intel", "D098F0" to "Intel",
        // Qualcomm Atheros
        // Nota: 002275 foi removido daqui — pertence à Netgear (IEEE OUI registry).
        "08865D" to "Qualcomm", "40BDE0" to "Qualcomm",
        "706F81" to "Qualcomm", "9CB6D0" to "Qualcomm", "E8DE27" to "Qualcomm",
        // Realtek
        "00E04C" to "Realtek", "1CC1DE" to "Realtek", "34298F" to "Realtek",
        "4CEEB9" to "Realtek", "60D819" to "Realtek", "EC086B" to "Realtek",
        // LG Electronics
        "001C62" to "LG", "002483" to "LG", "002EA9" to "LG",
        "0C2E28" to "LG", "3CA9F4" to "LG", "60D26A" to "LG",
        "6C4343" to "LG", "70B144" to "LG", "88C9D0" to "LG",
        "A00BBA" to "LG", "A8B8B4" to "LG", "C80E14" to "LG",
        // Sony
        "0013A9" to "Sony", "001D0D" to "Sony", "001FE2" to "Sony",
        "002618" to "Sony", "0050FB" to "Sony", "10A596" to "Sony",
        "1C98EC" to "Sony", "20070B" to "Sony", "2C6D8D" to "Sony",
        "38BEDA" to "Sony", "50819F" to "Sony", "60F187" to "Sony",
        "6C6567" to "Sony", "A04462" to "Sony", "AC9B0A" to "Sony",
        "B4523D" to "Sony", "DC85DE" to "Sony", "FC0FE6" to "Sony",
        // Microsoft
        "001DD8" to "Microsoft", "002248" to "Microsoft", "002550" to "Microsoft",
        "0050F2" to "Microsoft", "282898" to "Microsoft", "48509E" to "Microsoft",
        "60452C" to "Microsoft", "7C1E52" to "Microsoft", "985FD3" to "Microsoft",
        "A4C361" to "Microsoft",
        // Motorola
        "001753" to "Motorola", "00E0AA" to "Motorola", "1027F7" to "Motorola",
        "1CC1AC" to "Motorola", "2C4021" to "Motorola", "3434F9" to "Motorola",
        "480FCF" to "Motorola", "58AF7B" to "Motorola", "6C40B5" to "Motorola",
        "7CE9D3" to "Motorola", "BC2682" to "Motorola", "C065B4" to "Motorola",
        "C098E5" to "Motorola", "E8D0FC" to "Motorola",
        // Nokia (ONT/modem)
        "001BC5" to "Nokia", "00309C" to "Nokia", "006047" to "Nokia",
        "3C1450" to "Nokia", "484931" to "Nokia", "547B42" to "Nokia",
        "64A837" to "Nokia", "78B553" to "Nokia", "806C1B" to "Nokia",
        "9CF3CA" to "Nokia",
        // Arris (modems a cabo)
        "001799" to "Arris", "002071" to "Arris", "0026B8" to "Arris",
        "04186B" to "Arris", "34E2FD" to "Arris", "3C2867" to "Arris",
        "40ED98" to "Arris", "6CE9A5" to "Arris", "9C1168" to "Arris",
        "DC2050" to "Arris",
        // Intelbras (equipamentos de rede brasileiros)
        "00E09F" to "Intelbras", "006D70" to "Intelbras",
        "002B78" to "Intelbras",
        // Mikrotik
        "000C42" to "Mikrotik", "08550B" to "Mikrotik", "18FD74" to "Mikrotik",
        "2CC8D5" to "Mikrotik", "4C5E0C" to "Mikrotik", "6C3B6B" to "Mikrotik",
        "748DA8" to "Mikrotik", "B8699F" to "Mikrotik", "CC2DE0" to "Mikrotik",
        "D4CA6D" to "Mikrotik", "E48D8C" to "Mikrotik",
        // ZTE (ONT/roteadores usados por Claro, Vivo, OI no Brasil)
        "002A5A" to "ZTE", "28D855" to "ZTE", "3C9872" to "ZTE",
        "5C93B4" to "ZTE", "6427E7" to "ZTE", "784B87" to "ZTE",
        "80B686" to "ZTE", "9CD27B" to "ZTE", "B4440E" to "ZTE",
        "CC68B6" to "ZTE", "D05FB8" to "ZTE", "F443E1" to "ZTE",
        // Sagemcom (modems de ISPs: Oi, Vivo, GVT)
        "001C2E" to "Sagemcom", "006E37" to "Sagemcom", "38229D" to "Sagemcom",
        "4CAEA0" to "Sagemcom", "68A37D" to "Sagemcom", "9C976A" to "Sagemcom",
        // Ubiquiti (UniFi — muito popular em redes domésticas e empresariais no Brasil)
        "00156D" to "Ubiquiti", "0418D6" to "Ubiquiti", "18E829" to "Ubiquiti",
        "24A43C" to "Ubiquiti", "44D9E7" to "Ubiquiti", "546459" to "Ubiquiti",
        "68722D" to "Ubiquiti", "788A20" to "Ubiquiti", "80DCB8" to "Ubiquiti",
        "9C0560" to "Ubiquiti", "B4FBE4" to "Ubiquiti", "E063DA" to "Ubiquiti",
        "F09FC2" to "Ubiquiti", "FCECDA" to "Ubiquiti",
        // Tenda (roteadores populares no Brasil)
        "1C7AE7" to "Tenda", "88C3B3" to "Tenda", "C83A35" to "Tenda",
        "D46EB6" to "Tenda",
        // Dell (notebooks, workstations)
        "001143" to "Dell", "001560" to "Dell", "001E4F" to "Dell",
        "002564" to "Dell", "00265D" to "Dell", "18A99B" to "Dell",
        "5CCA1F" to "Dell", "84A9C4" to "Dell", "9440A5" to "Dell",
        "F81654" to "Dell",
        // HP (notebooks, impressoras)
        "001708" to "HP", "001F29" to "HP", "002255" to "HP",
        "3C4A92" to "HP", "7CC3A1" to "HP", "9CCFBF" to "HP",
        "D4C9EF" to "HP", "F4CE46" to "HP",
        // Lenovo (notebooks)
        "000D3A" to "Lenovo", "5CF9DD" to "Lenovo", "705A0F" to "Lenovo",
        "8CEA1B" to "Lenovo", "98FA9B" to "Lenovo", "B03AF2" to "Lenovo",
        // OnePlus
        "041E64" to "OnePlus", "94652D" to "OnePlus",
        // Oppo / Realme (populares no Brasil)
        "0026C7" to "Oppo", "28B2BD" to "Oppo", "3CE34F" to "Oppo",
        "5C2E59" to "Oppo", "70F11C" to "Oppo", "A4DDFB" to "Oppo",
        "20B5C2" to "Realme", "9C7AEF" to "Realme",
        // Nintendo (Switch)
        "002709" to "Nintendo", "00224C" to "Nintendo", "002331" to "Nintendo",
        "58BDA3" to "Nintendo", "7CBB8A" to "Nintendo", "98B6E9" to "Nintendo",
        // Roku (streaming)
        "AC3A7A" to "Roku", "B02344" to "Roku", "CC6E78" to "Roku",
        "D07671" to "Roku", "DC3A5E" to "Roku",
    )

    fun lookupFabricante(mac: String?): String? {
        val oui = normalizarOui(mac) ?: return null
        return db[oui]
    }

    private fun normalizarOui(mac: String?): String? {
        val limpo = mac
            ?.replace(":", "")
            ?.replace("-", "")
            ?.replace(".", "")
            ?.uppercase()
            ?: return null
        return if (limpo.length >= 6) limpo.take(6) else null
    }
}
