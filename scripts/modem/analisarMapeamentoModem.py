#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Analisa o mapeamento de dados do modem Nokia G-1425-B
Compara o que o manual diz vs. o que realmente está nas páginas
"""

import json
import re
from pathlib import Path

OUTPUT_DIR = Path("modem_captures")

def analyze_wan_ipv4():
    """Analisa /show_wan_status.cgi?ipv4"""
    html = (OUTPUT_DIR / "wan_status_ipv4.html").read_text(encoding='utf-8')

    print("[WAN IPv4] /show_wan_status.cgi?ipv4")
    print("=" * 80)

    # Extrair dados DNS do JavaScript
    dns_pattern = r"DNSServers:'([^']*)',"
    dns_match = re.search(dns_pattern, html)
    if dns_match:
        dns_str = dns_match.group(1)
        print(f"\n[ACHADO] DNS vem do objeto JavaScript: {dns_str}")
        ips = dns_str.split(",")
        print(f"  DNS Primario: {ips[0] if ips else '?'}")
        print(f"  DNS Secundario: {ips[1] if len(ips) > 1 else '?'}")

    # Mostrar como aparece no DOM
    print(f"\n[DOM] Os DNS são renderizados em divs:")
    print(f"  <div id='DNSServers'>{{dns_primario}}</div>")
    print(f"  <div id='DNSServers2'>{{dns_secundario}}</div>")

    # Extrair outros dados do JS
    data_patterns = {
        "IP WAN": r"ExternalIPAddress:'([^']*)',",
        "Gateway": r"RemoteIPAddress:'([^']*)',",
        "Concentrador PPPoE": r"PPPoEACName:'([^']*)',",
        "Status de conexao": r"ConnectionStatus:'([^']*)',",
        "VLAN ID": r"VLANIDMark:\s*(\d+)",
        "Interface": r"X_ASB_COM_IfName:'([^']*)',",
        "MAC Address": r"MACAddress:'([^']*)',",
        "Bytes Enviados": r"EthernetBytesSent:(\d+),",
        "Bytes Recebidos": r"EthernetBytesReceived:(\d+),",
        "Packets Enviados": r"EthernetPacketsSent:(\d+),",
        "Packets Recebidos": r"EthernetPacketsReceived:(\d+),",
        "Uptime (s)": r"Uptime:(\d+),",
    }

    print(f"\n[FONTE] Todos os dados vem do objeto JavaScript 'wan_conns':")
    for label, pattern in data_patterns.items():
        match = re.search(pattern, html)
        if match:
            value = match.group(1)
            print(f"  {label}: {value}")

    print(f"\n[PROBLEMA IDENTIFICADO]")
    print(f"  O codigo do app procura por tabelas HTML com rótulos como:")
    print(f"    'primary dns', 'dns primary', 'dns server', etc.")
    print(f"  MAS esses rótulos NAO aparecem em tabelas nesta página!")
    print(f"  Os dados vem do objeto JavaScript 'wan_conns[].pppConns[].DNSServers'")
    print()

def analyze_device_status():
    """Analisa /device_status.cgi"""
    html = (OUTPUT_DIR / "device_status.html").read_text(encoding='utf-8')

    print("[DEVICE STATUS] /device_status.cgi")
    print("=" * 80)

    data_patterns = {
        "Modelo": r'"ModelName":"([^"]*)"',
        "Fabricante": r'"Manufacturer":"([^"]*)"',
        "Serial": r'"SerialNumber":"([^"]*)"',
        "Firmware": r'"SoftwareVersion":"([^"]*)"',
        "Hardware": r'"HardwareVersion":"([^"]*)"',
        "Uptime (s)": r'"UpTime":(\d+)',
        "RAM Total (KB)": r'"Total":(\d+)',
        "RAM Livre (KB)": r'"Free":(\d+)',
        "Temp CPU (C)": r'"CPUTemp":"([^"]*)"',
    }

    print(f"\n[FONTE] Todos os dados vem do objeto JavaScript 'dev_info':")
    for label, pattern in data_patterns.items():
        match = re.search(pattern, html)
        if match:
            value = match.group(1)
            print(f"  {label}: {value}")

    print(f"\n[PROBLEMA IDENTIFICADO]")
    print(f"  Os dados estao no objeto JavaScript, NAO em tabelas HTML!")
    print()

def analyze_gpon_status():
    """Analisa /wan_status.cgi?gpon"""
    html = (OUTPUT_DIR / "wan_status_gpon.html").read_text(encoding='utf-8')

    print("[GPON STATUS] /wan_status.cgi?gpon")
    print("=" * 80)

    # Procura por padrões JavaScript
    gpon_patterns = {
        "RX Power": r"RXPower['\"]?\s*:\s*([^,\n]*),",
        "TX Power": r"TXPower['\"]?\s*:\s*([^,\n]*),",
        "Status": r"Status['\"]?\s*:\s*['\"]?([^'\"}\n]*)['\"]?",
        "Temperatura": r"TransceiverTemperature['\"]?\s*:\s*([^,\n]*),",
        "Tensao": r"SupplyVol[^:]*['\"]?\s*:\s*([^,\n]*),",
        "Corrente Laser": r"BiasCurrent['\"]?\s*:\s*([^,\n]*),",
        "Modo Conexao": r"ConnectionMode['\"]?\s*:\s*['\"]?([^'\"}\n]*)['\"]?",
    }

    print(f"\n[DADOS ENCONTRADOS]:")
    for label, pattern in gpon_patterns.items():
        match = re.search(pattern, html)
        if match:
            value = match.group(1)
            print(f"  {label}: {value}")

    print(f"\n[NOTA] Verificar se os rótulos de display coincidem com os nomes dos campos")
    print()

def analyze_devices():
    """Analisa /lan_status.cgi?wlan"""
    html = (OUTPUT_DIR / "lan_status_wlan.html").read_text(encoding='utf-8')

    print("[DEVICES CONECTADOS] /lan_status.cgi?wlan")
    print("=" * 80)

    # Procura por tabelas de dispositivos
    table_pattern = r'<table[^>]*>(.*?)</table>'
    tables = re.findall(table_pattern, html, re.DOTALL)

    print(f"\n[ESTRUTURA] Encontradas {len(tables)} tabelas")

    # Procura por headers
    for i, table in enumerate(tables):
        th_pattern = r'<th[^>]*>([^<]*)</th>'
        headers = re.findall(th_pattern, table, re.IGNORECASE)
        if headers:
            print(f"\n  Tabela {i}: {len(headers)} colunas")
            for j, header in enumerate(headers):
                clean = header.strip().replace('\n', ' ')[:60]
                print(f"    Col {j}: {clean}")

    print(f"\n[NOTA] Verificar qual tabela contem dispositivos (Status, IP, MAC, Device Name)")
    print()

def main():
    print("\n[TOOL] Analise de Mapeamento - Nokia G-1425-B\n")

    analyze_wan_ipv4()
    analyze_device_status()
    analyze_gpon_status()
    analyze_devices()

    print("\n" + "=" * 80)
    print("[RESUMO] Discrepancias Encontradas:")
    print("=" * 80)
    print("""
1. DNS (/show_wan_status.cgi?ipv4)
   MANUAL DIZ: "campos com rótulos 'DNS Primario' e 'DNS Secundario' em tabelas"
   REALIDADE: Os dados vem do objeto JS 'wan_conns[].pppConns[].DNSServers'
   IMPACTO: Codigo procura tabelas que NAO existem nesta página

2. Device Info (/device_status.cgi)
   MANUAL DIZ: "campos em tabelas HTML"
   REALIDADE: Os dados vem do objeto JS 'dev_info'
   IMPACTO: Codigo procura tabelas que MAY estar vazias

3. GPON Status (/wan_status.cgi?gpon)
   VERIFICAR: Comparar rótulos de display vs. nomes de campos JavaScript

4. Endpoint optics_module (router_endpoints.dart)
   PROBLEMA: Apontando para /check.expire.cgi (incorreto)
   CORRETO: Deveria apontar para /wan_status.cgi?gpon

5. Arquivo DNS (/dns.cgi)
   PROBLEMA: Endpoint nao esta definido no codigo
   VERIFICAR: Se deveria estar mapeado
""")

if __name__ == "__main__":
    main()
