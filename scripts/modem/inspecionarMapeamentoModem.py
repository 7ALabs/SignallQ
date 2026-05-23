#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Inspetor de mapeamento do modem Nokia G-1425-B
Captura dados reais das páginas do modem para validar mapeamento
"""

import asyncio
import json
import re
import sys
import os
from pathlib import Path

# Fix encoding
if sys.platform == 'win32':
    os.environ['PYTHONIOENCODING'] = 'utf-8'
    sys.stdout.reconfigure(encoding='utf-8')

from playwright.async_api import async_playwright

ROUTER_URL = "http://192.168.1.254"
OUTPUT_DIR = Path("modem_captures")

class ModemInspector:
    def __init__(self, username: str, password: str):
        self.username = username
        self.password = password
        self.browser = None
        self.context = None
        self.page = None
        self.output_dir = OUTPUT_DIR
        self.output_dir.mkdir(exist_ok=True)

    async def __aenter__(self):
        p = await async_playwright().start()
        self.browser = await p.chromium.launch(headless=True, args=["--no-sandbox"])
        self.context = await self.browser.new_context()
        self.page = await self.context.new_page()
        return self

    async def __aexit__(self, *args):
        await self.context.close()
        await self.browser.close()

    async def login(self):
        """Autentica no modem"""
        print("[1/5] Conectando ao modem e autenticando...")
        await self.page.goto(f"{ROUTER_URL}/", wait_until="networkidle")
        await self.page.fill("#username", self.username)
        await self.page.fill("#password", self.password)
        await self.page.click("#loginBT")
        await self.page.wait_for_load_state("networkidle")
        await asyncio.sleep(1)
        print("[OK] Autenticado com sucesso")

    async def capture_page(self, endpoint: str, name: str) -> str:
        """Captura HTML de uma página"""
        print(f"  Capturando {name}...")
        await self.page.goto(f"{ROUTER_URL}{endpoint}", wait_until="networkidle")
        html = await self.page.content()

        output_file = self.output_dir / f"{name}.html"
        output_file.write_text(html, encoding='utf-8')

        return html

    async def extract_table_data(self, html: str) -> dict:
        """Extrai dados de tabelas do HTML"""
        result = {}

        # Encontrar todas as tabelas
        table_pattern = r'<table[^>]*>(.*?)</table>'
        tables = re.findall(table_pattern, html, re.DOTALL | re.IGNORECASE)

        for i, table_html in enumerate(tables):
            rows = re.findall(r'<tr[^>]*>(.*?)</tr>', table_html, re.DOTALL | re.IGNORECASE)
            table_data = []

            for row in rows:
                cells = re.findall(r'<t[dh][^>]*>([^<]*(?:<[^>]*>[^<]*)*)</t[dh]>', row, re.IGNORECASE)
                cleaned_cells = []
                for cell in cells:
                    # Remove tags HTML
                    clean = re.sub(r'<[^>]*>', '', cell).strip()
                    if clean:
                        cleaned_cells.append(clean)
                if cleaned_cells:
                    table_data.append(cleaned_cells)

            if table_data:
                result[f"table_{i}"] = table_data

        return result

    async def extract_form_fields(self, html: str) -> dict:
        """Extrai campos de formulário"""
        result = {}

        # Input fields
        input_pattern = r'<input[^>]*name=["\']?([^"\'>\s]+)["\']?[^>]*value=["\']?([^"\']*)["\']?[^>]*>'
        for match in re.finditer(input_pattern, html, re.IGNORECASE):
            result[match.group(1)] = match.group(2)

        # Selects
        select_pattern = r'<select[^>]*name=["\']?([^"\'>\s]+)["\']?[^>]*>.*?<option[^>]*selected[^>]*>([^<]*)'
        for match in re.finditer(select_pattern, html, re.DOTALL | re.IGNORECASE):
            result[match.group(1)] = match.group(2)

        # Divs com id (valores JavaScript)
        div_pattern = r'<div[^>]*id=["\']?([^"\'>\s]+)["\']?[^>]*>([^<]*)</div>'
        for match in re.finditer(div_pattern, html):
            if match.group(2).strip():
                result[f"div_{match.group(1)}"] = match.group(2).strip()

        return result

    async def inspect_all(self):
        """Inspeciona todas as páginas críticas"""
        try:
            await self.login()

            print("\n[2/5] Capturando páginas críticas...")

            pages = {
                "/device_status.cgi": "device_status",
                "/show_wan_status.cgi?ipv4": "wan_status_ipv4",
                "/show_wan_status.cgi?ipv6": "wan_status_ipv6",
                "/lan_status.cgi?wlan": "lan_status_wlan",
                "/lan_status.cgi?lan": "lan_status_lan",
                "/wan_status.cgi?gpon": "wan_status_gpon",
                "/lan_ipv4.cgi": "lan_ipv4",
                "/wan_config_glb.cgi": "wan_config_glb",
                "/wlan_config.cgi": "wlan_config_2.4g",
                "/wlan_config.cgi?v=11ac": "wlan_config_5g",
                "/dns.cgi": "dns_config",
                "/index.cgi?getppp": "ppp_status",
            }

            captured = {}
            for endpoint, name in pages.items():
                try:
                    html = await self.capture_page(endpoint, name)
                    captured[name] = {
                        "endpoint": endpoint,
                        "size": len(html),
                    }
                except Exception as e:
                    print(f"  [ERROR] Erro ao capturar {name}: {e}")
                    captured[name] = {
                        "endpoint": endpoint,
                        "error": str(e),
                    }

            print("\n[3/5] Analisando estrutura de dados...")

            analysis = {}
            for name, info in captured.items():
                if "error" not in info:
                    html_file = self.output_dir / f"{name}.html"
                    if html_file.exists():
                        html = html_file.read_text(encoding='utf-8')

                        # Detectar se é JSON
                        if html.strip().startswith('{'):
                            try:
                                data = json.loads(html)
                                analysis[name] = {
                                    "type": "JSON",
                                    "structure": str(list(data.keys()))[:200],
                                }
                            except:
                                pass
                        else:
                            # Analisar tabelas e campos
                            tables = await self.extract_table_data(html)
                            fields = await self.extract_form_fields(html)

                            analysis[name] = {
                                "type": "HTML",
                                "tables": len(tables),
                                "fields": list(fields.keys())[:20],
                                "table_rows": {k: len(v) for k, v in tables.items()},
                            }

            print(f"\n[4/5] Gerando relatório...")
            report = {
                "timestamp": __import__('datetime').datetime.now().isoformat(),
                "router": "Nokia G-1425-B",
                "captured_pages": captured,
                "analysis": analysis,
            }

            report_file = self.output_dir / "inspection_report.json"
            report_file.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding='utf-8')

            print(f"[OK] Relatório salvo em {report_file}")

            print("\n[5/5] Detalhes das páginas críticas:")
            print("\n" + "="*80)

            # Detalhar /show_wan_status.cgi?ipv4
            wan_file = self.output_dir / "wan_status_ipv4.html"
            if wan_file.exists():
                print("\n[DATA] /show_wan_status.cgi?ipv4 (WAN Status IPv4)")
                print("-" * 80)
                html = wan_file.read_text(encoding='utf-8')
                tables = await self.extract_table_data(html)
                for table_name, rows in tables.items():
                    print(f"\n  {table_name}:")
                    for row in rows[:10]:  # Primeiras 10 linhas
                        print(f"    {row}")

            # Detalhar /device_status.cgi
            device_file = self.output_dir / "device_status.html"
            if device_file.exists():
                print("\n\n[DEVICE] /device_status.cgi (Device Status)")
                print("-" * 80)
                html = device_file.read_text(encoding='utf-8')
                tables = await self.extract_table_data(html)
                for table_name, rows in tables.items():
                    print(f"\n  {table_name}:")
                    for row in rows[:15]:
                        print(f"    {row}")

            # Detalhar /wan_status.cgi?gpon
            gpon_file = self.output_dir / "wan_status_gpon.html"
            if gpon_file.exists():
                print("\n\n[GPON] /wan_status.cgi?gpon (GPON Status)")
                print("-" * 80)
                html = gpon_file.read_text(encoding='utf-8')
                tables = await self.extract_table_data(html)
                for table_name, rows in tables.items():
                    print(f"\n  {table_name}:")
                    for row in rows[:15]:
                        print(f"    {row}")

            # Detalhar /lan_status.cgi?wlan
            wlan_file = self.output_dir / "lan_status_wlan.html"
            if wlan_file.exists():
                print("\n\n[WLAN] /lan_status.cgi?wlan (WLAN + Devices)")
                print("-" * 80)
                html = wlan_file.read_text(encoding='utf-8')
                tables = await self.extract_table_data(html)
                for table_name, rows in tables.items():
                    print(f"\n  {table_name}:")
                    for row in rows[:20]:
                        print(f"    {row}")

            print("\n" + "="*80)
            print(f"\n[OK] Inspeção completa! Arquivos capturados em: {self.output_dir}/")

        except Exception as e:
            print(f"\n[ERROR] Erro na inspeção: {e}")
            raise

async def main():
    print("[TOOL] Inspetor de Mapeamento - Nokia G-1425-B\n")

    # Credenciais (fornecidas pelo usuário)
    username = "userAdmin"
    password = "k8@kTL:mh,Mc"

    async with ModemInspector(username, password) as inspector:
        await inspector.inspect_all()

if __name__ == "__main__":
    asyncio.run(main())
