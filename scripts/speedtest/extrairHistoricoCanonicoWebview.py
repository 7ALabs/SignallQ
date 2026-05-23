"""extrairHistoricoCanonicoWebview.py

Extrai o historico de speedtest salvo no localStorage do app canonico
(Capacitor WebView / LevelDB) e converte para JSON no formato aceito pelo
comparadorParidadeFlutterKotlin.py.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any, Dict, List, Optional, Set


CHAVE_HISTORICO = b"linka.speedtest.history.v1"
MODOS_VALIDOS = {"fast", "complete"}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Extrai historico canonico do localStorage (LevelDB WebView).",
    )
    parser.add_argument(
        "--leveldb-dir",
        type=Path,
        required=True,
        help="Diretorio com arquivos do leveldb exportados do app canonico.",
    )
    parser.add_argument(
        "--saida-dir",
        type=Path,
        required=True,
        help="Diretorio de saida dos JSONs convertidos.",
    )
    parser.add_argument(
        "--prefixo",
        default="canonico",
        help="Prefixo dos arquivos JSON gerados.",
    )
    return parser.parse_args()


def encontrar_bloco_json_array(blob: bytes, idx_inicio: int) -> Optional[str]:
    idx_abre = blob.find(b"[", idx_inicio)
    if idx_abre < 0:
        return None

    profundidade = 0
    em_string = False
    escape = False
    idx_fecha = -1
    for i in range(idx_abre, len(blob)):
        c = blob[i]
        if em_string:
            if escape:
                escape = False
                continue
            if c == 0x5C:  # \
                escape = True
                continue
            if c == 0x22:  # "
                em_string = False
            continue

        if c == 0x22:  # "
            em_string = True
            continue
        if c == 0x5B:  # [
            profundidade += 1
            continue
        if c == 0x5D:  # ]
            profundidade -= 1
            if profundidade == 0:
                idx_fecha = i
                break

    if idx_fecha < 0:
        return None

    trecho = blob[idx_abre : idx_fecha + 1]
    try:
        return trecho.decode("utf-8")
    except UnicodeDecodeError:
        return trecho.decode("utf-8", errors="ignore")


def normalizar_modo(valor: Any) -> Optional[str]:
    if valor is None:
        return None
    s = str(valor).strip().lower()
    if s in MODOS_VALIDOS:
        return s
    if s in {"rapido", "rápido", "quick"}:
        return "fast"
    if s in {"completo", "normal", "advanced"}:
        return "complete"
    return None


def sanitizar_json_texto(json_txt: str) -> str:
    # Alguns blocos do LevelDB podem conter caracteres de controle espurios
    # embutidos no payload textual; remove para permitir parse resiliente.
    return "".join(ch for ch in json_txt if ord(ch) >= 0x20 or ch in ("\n", "\r", "\t"))


def converter_registro(registro: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    modo = normalizar_modo(registro.get("testMode"))
    if modo is None:
        return None

    timestamp = registro.get("timestamp")
    try:
        timestamp_int = int(timestamp)
    except (TypeError, ValueError):
        return None
    if timestamp_int <= 0:
        return None

    dl = registro.get("dl")
    ul = registro.get("ul")
    lat = registro.get("latency")
    jit = registro.get("jitter")
    perda = registro.get("packetLoss")

    try:
        dl_f = float(dl) if dl is not None else None
    except (TypeError, ValueError):
        dl_f = None
    try:
        ul_f = float(ul) if ul is not None else None
    except (TypeError, ValueError):
        ul_f = None
    try:
        lat_f = float(lat) if lat is not None else None
    except (TypeError, ValueError):
        lat_f = None
    try:
        jit_f = float(jit) if jit is not None else None
    except (TypeError, ValueError):
        jit_f = None
    try:
        perda_f = float(perda) if perda is not None else None
    except (TypeError, ValueError):
        perda_f = None

    return {
        "modoTeste": modo,
        "timestampFim": timestamp_int,
        "download": {"throughputMbps": dl_f},
        "upload": {"throughputMbps": ul_f},
        "latencia": {"medianaMs": lat_f, "jitterMs": jit_f, "perda": perda_f},
        "specVersionUsada": registro.get("ruleSetVersion") or "historyV1",
        "contaminado": None,
        "origemCanonica": "historyLocalStorage",
    }


def extrair_historicos(leveldb_dir: Path) -> List[Dict[str, Any]]:
    resultados: List[Dict[str, Any]] = []
    ids_vistos: Set[str] = set()

    arquivos = sorted([p for p in leveldb_dir.glob("*") if p.is_file()])
    for arquivo in arquivos:
        blob = arquivo.read_bytes()
        busca_inicio = 0
        while True:
            idx = blob.find(CHAVE_HISTORICO, busca_inicio)
            if idx < 0:
                break
            busca_inicio = idx + len(CHAVE_HISTORICO)

            json_txt = encontrar_bloco_json_array(blob, idx)
            if not json_txt:
                continue
            try:
                arr = json.loads(json_txt)
            except json.JSONDecodeError:
                try:
                    arr = json.loads(sanitizar_json_texto(json_txt))
                except json.JSONDecodeError:
                    continue
            if not isinstance(arr, list):
                continue

            for item in arr:
                if not isinstance(item, dict):
                    continue
                registro_id = str(item.get("id") or "")
                if registro_id and registro_id in ids_vistos:
                    continue
                convertido = converter_registro(item)
                if convertido is None:
                    continue
                resultados.append(convertido)
                if registro_id:
                    ids_vistos.add(registro_id)

    resultados.sort(key=lambda r: int(r.get("timestampFim", 0)), reverse=True)
    return resultados


def salvar_jsons(
    registros: List[Dict[str, Any]],
    saida_dir: Path,
    prefixo: str,
) -> None:
    saida_dir.mkdir(parents=True, exist_ok=True)
    for idx, reg in enumerate(registros, start=1):
        ts = int(reg.get("timestampFim", 0))
        nome = f"{prefixo}_{idx:03d}_{ts}.json"
        (saida_dir / nome).write_text(
            json.dumps(reg, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )


def main() -> int:
    args = parse_args()
    if not args.leveldb_dir.exists():
        raise SystemExit(f"diretorio leveldb inexistente: {args.leveldb_dir}")

    registros = extrair_historicos(args.leveldb_dir)
    if not registros:
        raise SystemExit("nenhum registro canonico encontrado no leveldb informado")

    salvar_jsons(registros, args.saida_dir, args.prefixo)
    print(f"registrosCanonicos: {len(registros)}")
    print(f"saidaDir: {args.saida_dir}")
    print(f"maisRecenteEpochMs: {registros[0].get('timestampFim')}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
