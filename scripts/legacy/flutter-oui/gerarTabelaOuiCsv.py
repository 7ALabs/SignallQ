#!/usr/bin/env python3
"""Gera assets/oui/oui.csv a partir do registro IEEE OUI.

Baixa https://standards-oui.ieee.org/oui/oui.csv (lista oficial e pública,
atualizada diariamente), normaliza para o formato consumido por
OuiLookup.preload() — uma linha por prefixo `<oui6hex>,<vendor>` em hex
minusculo sem separadores — e grava em
`source/app/assets/oui/oui.csv`.

Uso:
    python tools/generate_oui_csv.py            # baixa e regrava o asset
    python tools/generate_oui_csv.py --offline  # só normaliza um CSV ja em cache

O Flutter consome o arquivo via rootBundle no boot do app
(main.dart -> OuiLookup.preload()). Reexecutar este script + `flutter build`
e o app passa a reconhecer milhares de fabricantes adicionais sem nenhuma
chamada de rede em runtime.
"""

from __future__ import annotations

import argparse
import csv
import io
import sys
from pathlib import Path
from urllib.request import Request, urlopen

REPO_ROOT = Path(__file__).resolve().parent.parent
ASSET_PATH = REPO_ROOT / "source" / "app" / "assets" / "oui" / "oui.csv"
CACHE_PATH = REPO_ROOT / "tools" / ".cache_ieee_oui.csv"
SOURCE_URL = "https://standards-oui.ieee.org/oui/oui.csv"


def fetch_remote() -> str:
    print(f"[oui] baixando {SOURCE_URL} ...", file=sys.stderr)
    req = Request(SOURCE_URL, headers={"User-Agent": "linka-build/1.0"})
    with urlopen(req, timeout=60) as resp:
        data = resp.read().decode("utf-8", errors="replace")
    CACHE_PATH.parent.mkdir(parents=True, exist_ok=True)
    CACHE_PATH.write_text(data, encoding="utf-8")
    return data


def load_source(offline: bool) -> str:
    if offline:
        if not CACHE_PATH.exists():
            print(
                f"[oui] modo --offline mas nao ha cache em {CACHE_PATH}",
                file=sys.stderr,
            )
            sys.exit(1)
        return CACHE_PATH.read_text(encoding="utf-8")
    return fetch_remote()


def normalize(raw: str) -> dict[str, str]:
    table: dict[str, str] = {}
    reader = csv.DictReader(io.StringIO(raw))
    for row in reader:
        # Colunas oficiais: Registry,Assignment,Organization Name,...
        assignment = (row.get("Assignment") or "").strip()
        org = (row.get("Organization Name") or "").strip()
        if not assignment or not org:
            continue
        prefix = "".join(ch for ch in assignment.lower() if ch in "0123456789abcdef")
        if len(prefix) != 6:
            continue
        # Sanitiza vendor: retira virgulas e quebras de linha; corta em 64 chars.
        vendor = " ".join(org.replace(",", " ").split())[:64]
        table[prefix] = vendor
    return table


def write_asset(table: dict[str, str]) -> None:
    ASSET_PATH.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        "# OUI extended table - gerado por tools/generate_oui_csv.py",
        "# Formato: <oui6hex>,<vendor>",
        f"# Total: {len(table)} prefixos",
    ]
    for prefix in sorted(table):
        lines.append(f"{prefix},{table[prefix]}")
    ASSET_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"[oui] gravado {ASSET_PATH} ({len(table)} prefixos)")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--offline",
        action="store_true",
        help="usa cache local (.cache_ieee_oui.csv) em vez de baixar.",
    )
    args = parser.parse_args()

    raw = load_source(offline=args.offline)
    table = normalize(raw)
    if not table:
        print("[oui] nenhuma entrada valida no CSV de origem.", file=sys.stderr)
        sys.exit(1)
    write_asset(table)


if __name__ == "__main__":
    main()
