"""comparadorParidadeFlutterKotlin.py

Compara resultados de speedtest entre Flutter e app Kotlin nativo.

Fontes suportadas:
- Flutter JSON estruturado (FormatoLogMedicao.md)
- Flutter SQLite legado (tabela medicao do drift)
- Kotlin SQLite Room (tabela medicao do app nativo)

Saida:
- CSV com pares Flutter x Kotlin
- Relatorio Markdown com resumo por modo e metrica
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import json
import sqlite3
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Optional


METRICAS = ("downloadMbps", "uploadMbps", "latenciaMs", "jitterMs", "perdaPercentual")
MODOS_VALIDOS = ("fast", "complete")


@dataclass
class RegistroCanonico:
    origem: str
    modo: str
    timestampEpochMs: int
    downloadMbps: Optional[float]
    uploadMbps: Optional[float]
    latenciaMs: Optional[float]
    jitterMs: Optional[float]
    perdaPercentual: Optional[float]
    contaminado: Optional[bool]
    specVersion: Optional[str]
    idOriginal: Optional[str]


@dataclass
class ParComparado:
    modo: str
    flutter: RegistroCanonico
    kotlin: RegistroCanonico
    deltaSegundos: float
    diffDownloadPct: Optional[float]
    diffUploadPct: Optional[float]
    diffLatenciaPct: Optional[float]
    diffJitterPct: Optional[float]
    diffPerdaPct: Optional[float]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Compara paridade speedtest Flutter x Kotlin.")
    parser.add_argument("--flutter-json-dir", type=Path, default=None)
    parser.add_argument("--flutter-sqlite", type=Path, default=None)
    parser.add_argument("--kotlin-sqlite", type=Path, default=None)
    parser.add_argument("--modo", choices=("all", "fast", "complete"), default="all")
    parser.add_argument("--janela-segundos", type=int, default=900)
    parser.add_argument("--saida-dir", type=Path, default=Path("scripts/speedtest/resultados"))
    parser.add_argument("--prefixo", default="paridade_flutter_kotlin")
    args = parser.parse_args()

    if not args.flutter_json_dir and not args.flutter_sqlite:
        parser.error("informe --flutter-json-dir ou --flutter-sqlite")
    if not args.kotlin_sqlite:
        parser.error("informe --kotlin-sqlite")
    return args


def main() -> int:
    args = parse_args()

    registros_flutter: List[RegistroCanonico] = []
    if args.flutter_json_dir:
        registros_flutter.extend(carregar_flutter_json_dir(args.flutter_json_dir))
    if args.flutter_sqlite:
        registros_flutter.extend(carregar_flutter_sqlite(args.flutter_sqlite))

    registros_kotlin = carregar_kotlin_sqlite(args.kotlin_sqlite)

    if args.modo != "all":
        registros_flutter = [r for r in registros_flutter if r.modo == args.modo]
        registros_kotlin = [r for r in registros_kotlin if r.modo == args.modo]

    registros_flutter = filtrar_registros_validos(registros_flutter)
    registros_kotlin = filtrar_registros_validos(registros_kotlin)

    if not registros_flutter:
        raise SystemExit("sem registros Flutter validos para comparar")
    if not registros_kotlin:
        raise SystemExit("sem registros Kotlin validos para comparar")

    pares = parear_registros(registros_flutter, registros_kotlin, args.janela_segundos)
    if not pares:
        raise SystemExit("nenhum par encontrado na janela informada")

    args.saida_dir.mkdir(parents=True, exist_ok=True)
    stamp = dt.datetime.now().strftime("%Y%m%d_%H%M%S")
    base_nome = f"{args.prefixo}_{stamp}"

    csv_path = args.saida_dir / f"{base_nome}.csv"
    md_path = args.saida_dir / f"{base_nome}.md"

    escrever_csv(pares, csv_path)
    escrever_markdown(
        pares=pares,
        saida=md_path,
        csv_nome=csv_path.name,
        origem_flutter=descrever_origens(registros_flutter),
        origem_kotlin=descrever_origens(registros_kotlin),
        janela_segundos=args.janela_segundos,
    )

    print(f"pares: {len(pares)}")
    print(f"csv: {csv_path}")
    print(f"md: {md_path}")
    return 0


def descrever_origens(registros: Iterable[RegistroCanonico]) -> str:
    origens = sorted({r.origem for r in registros})
    return ", ".join(origens)


def carregar_flutter_json_dir(pasta: Path) -> List[RegistroCanonico]:
    if not pasta.exists():
        return []
    saida: List[RegistroCanonico] = []
    for arquivo in sorted(pasta.glob("*.json")):
        try:
            dados = json.loads(arquivo.read_text(encoding="utf-8"))
            modo = normalizar_modo(dados.get("modoTeste"))
            if modo is None:
                continue
            ts = parse_timestamp_json(dados)
            registro = RegistroCanonico(
                origem=f"flutterJson:{arquivo.name}",
                modo=modo,
                timestampEpochMs=ts,
                downloadMbps=to_float(dados.get("download", {}).get("throughputMbps")),
                uploadMbps=to_float(dados.get("upload", {}).get("throughputMbps")),
                latenciaMs=to_float(dados.get("latencia", {}).get("medianaMs")),
                jitterMs=to_float(dados.get("latencia", {}).get("jitterMs")),
                perdaPercentual=to_float(dados.get("latencia", {}).get("perda")),
                contaminado=to_bool(dados.get("contaminado")),
                specVersion=to_str(dados.get("specVersionUsada") or dados.get("specVersion")),
                idOriginal=arquivo.stem,
            )
            saida.append(registro)
        except Exception:
            continue
    return saida


def parse_timestamp_json(dados: dict) -> int:
    candidatos = [
        dados.get("timestampFim"),
        dados.get("timestampInicio"),
        dados.get("timestamp"),
    ]
    for valor in candidatos:
        ts = parse_timestamp_any(valor)
        if ts is not None:
            return ts
    raise ValueError("timestampNaoEncontrado")


def carregar_flutter_sqlite(caminho: Path) -> List[RegistroCanonico]:
    if not caminho.exists():
        return []
    con = sqlite3.connect(str(caminho))
    try:
        colunas = listar_colunas(con, "medicao")
        if not colunas:
            return []
        if "download_mbps" not in colunas:
            return []

        query = """
            SELECT
                id,
                timestamp_utc,
                modo_teste,
                download_mbps,
                upload_mbps,
                latencia_sem_carga_ms,
                jitter_ms,
                perda_pacotes_pct,
                rule_set_version
            FROM medicao
            ORDER BY rowid DESC
        """
        registros: List[RegistroCanonico] = []
        for row in con.execute(query):
            modo = normalizar_modo(row[2])
            if modo is None:
                continue
            ts = parse_timestamp_any(row[1])
            if ts is None:
                continue
            registros.append(
                RegistroCanonico(
                    origem=f"flutterSqlite:{caminho.name}",
                    modo=modo,
                    timestampEpochMs=ts,
                    downloadMbps=to_float(row[3]),
                    uploadMbps=to_float(row[4]),
                    latenciaMs=to_float(row[5]),
                    jitterMs=to_float(row[6]),
                    perdaPercentual=to_float(row[7]),
                    contaminado=None,
                    specVersion=to_str(row[8]),
                    idOriginal=to_str(row[0]),
                )
            )
        return registros
    finally:
        con.close()


def carregar_kotlin_sqlite(caminho: Path) -> List[RegistroCanonico]:
    if not caminho.exists():
        return []
    con = sqlite3.connect(str(caminho))
    try:
        colunas = listar_colunas(con, "medicao")
        if not colunas:
            return []

        tem_room = {"timestampEpochMs", "downloadMbps", "uploadMbps"}.issubset(set(colunas))
        if not tem_room:
            return []

        query = """
            SELECT
                id,
                timestampEpochMs,
                speedtestMode,
                downloadMbps,
                uploadMbps,
                latencyMs,
                jitterMs,
                perdaPercentual,
                contaminado,
                specVersion
            FROM medicao
            ORDER BY timestampEpochMs DESC
        """
        registros: List[RegistroCanonico] = []
        for row in con.execute(query):
            modo = normalizar_modo(row[2])
            if modo is None:
                continue
            ts = parse_timestamp_any(row[1])
            if ts is None:
                continue
            registros.append(
                RegistroCanonico(
                    origem=f"kotlinSqlite:{caminho.name}",
                    modo=modo,
                    timestampEpochMs=ts,
                    downloadMbps=to_float(row[3]),
                    uploadMbps=to_float(row[4]),
                    latenciaMs=to_float(row[5]),
                    jitterMs=to_float(row[6]),
                    perdaPercentual=to_float(row[7]),
                    contaminado=to_bool(row[8]),
                    specVersion=to_str(row[9]),
                    idOriginal=to_str(row[0]),
                )
            )
        return registros
    finally:
        con.close()


def listar_colunas(con: sqlite3.Connection, tabela: str) -> List[str]:
    try:
        rows = con.execute(f"PRAGMA table_info({tabela})").fetchall()
        return [r[1] for r in rows]
    except sqlite3.DatabaseError:
        return []


def filtrar_registros_validos(registros: List[RegistroCanonico]) -> List[RegistroCanonico]:
    saida = []
    for r in registros:
        if r.modo not in MODOS_VALIDOS:
            continue
        if r.timestampEpochMs <= 0:
            continue
        if r.downloadMbps is None and r.uploadMbps is None:
            continue
        saida.append(r)
    return sorted(saida, key=lambda x: x.timestampEpochMs, reverse=True)


def parear_registros(
    flutter: List[RegistroCanonico],
    kotlin: List[RegistroCanonico],
    janela_segundos: int,
) -> List[ParComparado]:
    pares: List[ParComparado] = []
    for modo in MODOS_VALIDOS:
        flutter_modo = sorted((r for r in flutter if r.modo == modo), key=lambda x: x.timestampEpochMs, reverse=True)
        kotlin_modo = sorted((r for r in kotlin if r.modo == modo), key=lambda x: x.timestampEpochMs, reverse=True)
        usados = set()
        for rk in kotlin_modo:
            idx, delta = encontrar_mais_proximo(rk, flutter_modo, usados, janela_segundos)
            if idx is None:
                continue
            usados.add(idx)
            rf = flutter_modo[idx]
            pares.append(
                ParComparado(
                    modo=modo,
                    flutter=rf,
                    kotlin=rk,
                    deltaSegundos=delta,
                    diffDownloadPct=diff_pct(rk.downloadMbps, rf.downloadMbps),
                    diffUploadPct=diff_pct(rk.uploadMbps, rf.uploadMbps),
                    diffLatenciaPct=diff_pct(rk.latenciaMs, rf.latenciaMs),
                    diffJitterPct=diff_pct(rk.jitterMs, rf.jitterMs),
                    diffPerdaPct=diff_pct(rk.perdaPercentual, rf.perdaPercentual),
                )
            )
    return sorted(pares, key=lambda p: p.kotlin.timestampEpochMs)


def encontrar_mais_proximo(
    alvo: RegistroCanonico,
    candidatos: List[RegistroCanonico],
    usados: set,
    janela_segundos: int,
) -> tuple[Optional[int], float]:
    melhor_idx: Optional[int] = None
    melhor_delta = float("inf")
    for i, c in enumerate(candidatos):
        if i in usados:
            continue
        delta = abs(c.timestampEpochMs - alvo.timestampEpochMs) / 1000.0
        if delta <= janela_segundos and delta < melhor_delta:
            melhor_delta = delta
            melhor_idx = i
    if melhor_idx is None:
        return None, float("inf")
    return melhor_idx, melhor_delta


def escrever_csv(pares: List[ParComparado], caminho: Path) -> None:
    campos = [
        "modo",
        "deltaSegundos",
        "flutterTimestamp",
        "kotlinTimestamp",
        "flutterDownload",
        "kotlinDownload",
        "diffDownloadPct",
        "flutterUpload",
        "kotlinUpload",
        "diffUploadPct",
        "flutterLatencia",
        "kotlinLatencia",
        "diffLatenciaPct",
        "flutterJitter",
        "kotlinJitter",
        "diffJitterPct",
        "flutterPerda",
        "kotlinPerda",
        "diffPerdaPct",
        "flutterOrigem",
        "kotlinOrigem",
    ]
    with caminho.open("w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=campos, delimiter=";")
        w.writeheader()
        for p in pares:
            w.writerow(
                {
                    "modo": p.modo,
                    "deltaSegundos": fmt(p.deltaSegundos),
                    "flutterTimestamp": p.flutter.timestampEpochMs,
                    "kotlinTimestamp": p.kotlin.timestampEpochMs,
                    "flutterDownload": fmt(p.flutter.downloadMbps),
                    "kotlinDownload": fmt(p.kotlin.downloadMbps),
                    "diffDownloadPct": fmt(p.diffDownloadPct),
                    "flutterUpload": fmt(p.flutter.uploadMbps),
                    "kotlinUpload": fmt(p.kotlin.uploadMbps),
                    "diffUploadPct": fmt(p.diffUploadPct),
                    "flutterLatencia": fmt(p.flutter.latenciaMs),
                    "kotlinLatencia": fmt(p.kotlin.latenciaMs),
                    "diffLatenciaPct": fmt(p.diffLatenciaPct),
                    "flutterJitter": fmt(p.flutter.jitterMs),
                    "kotlinJitter": fmt(p.kotlin.jitterMs),
                    "diffJitterPct": fmt(p.diffJitterPct),
                    "flutterPerda": fmt(p.flutter.perdaPercentual),
                    "kotlinPerda": fmt(p.kotlin.perdaPercentual),
                    "diffPerdaPct": fmt(p.diffPerdaPct),
                    "flutterOrigem": p.flutter.origem,
                    "kotlinOrigem": p.kotlin.origem,
                }
            )


def escrever_markdown(
    pares: List[ParComparado],
    saida: Path,
    csv_nome: str,
    origem_flutter: str,
    origem_kotlin: str,
    janela_segundos: int,
) -> None:
    linhas: List[str] = []
    deltas = sorted([p.deltaSegundos for p in pares])
    mediana_delta = deltas[len(deltas) // 2] if deltas else None
    linhas.append("# Relatorio de paridade Flutter x Kotlin")
    linhas.append("")
    linhas.append(f"- Gerado em: `{dt.datetime.now().isoformat(timespec='seconds')}`")
    linhas.append(f"- Janela de pareamento: `{janela_segundos}s`")
    linhas.append(f"- Fonte Flutter: `{origem_flutter}`")
    linhas.append(f"- Fonte Kotlin: `{origem_kotlin}`")
    linhas.append(f"- Total de pares: **{len(pares)}**")
    if mediana_delta is not None:
        linhas.append(f"- Mediana do delta temporal: `{mediana_delta:.1f}s`")
    linhas.append("")
    if mediana_delta is not None and mediana_delta > 3600:
        linhas.append("> Aviso: pares com delta temporal alto; interpretar throughput com cautela (baseline possivelmente nao contemporaneo).")
        linhas.append("")

    for modo in MODOS_VALIDOS:
        bloco = [p for p in pares if p.modo == modo]
        if not bloco:
            continue
        linhas.append(f"## Modo {modo}")
        linhas.append("")
        linhas.append(f"- Pares: **{len(bloco)}**")
        for metrica, attr in (
            ("download", "diffDownloadPct"),
            ("upload", "diffUploadPct"),
            ("latencia", "diffLatenciaPct"),
            ("jitter", "diffJitterPct"),
            ("perda", "diffPerdaPct"),
        ):
            valores = [abs(getattr(p, attr)) for p in bloco if getattr(p, attr) is not None]
            media = media_lista(valores)
            pct10 = percentual_abaixo_limite(valores, 10.0)
            if media is None:
                linhas.append(f"- {metrica}: sem dados")
            else:
                linhas.append(
                    f"- {metrica}: diff abs medio `{media:.2f}%` | pares <=10% `{pct10:.1f}%`"
                )
        linhas.append("")

    linhas.append("## Arquivos")
    linhas.append("")
    linhas.append(f"- CSV detalhado: `{csv_nome}`")
    linhas.append("")
    linhas.append("## Regra de leitura")
    linhas.append("")
    linhas.append("- Paridade forte: maioria dos pares com diff abs <= 10% em download/upload.")
    linhas.append("- Se latencia/jitter divergem com download/upload proximos, investigar rota/edge (`cf-ray`) e protocolo HTTP.")

    saida.write_text("\n".join(linhas), encoding="utf-8")


def diff_pct(valor_kotlin: Optional[float], valor_flutter: Optional[float]) -> Optional[float]:
    if valor_kotlin is None or valor_flutter is None:
        return None
    if valor_flutter == 0:
        return None
    return ((valor_kotlin - valor_flutter) / valor_flutter) * 100.0


def media_lista(valores: List[float]) -> Optional[float]:
    if not valores:
        return None
    return sum(valores) / len(valores)


def percentual_abaixo_limite(valores: List[float], limite: float) -> float:
    if not valores:
        return 0.0
    ok = sum(1 for v in valores if v <= limite)
    return (ok / len(valores)) * 100.0


def normalizar_modo(valor: object) -> Optional[str]:
    if valor is None:
        return None
    s = str(valor).strip().lower()
    if s in MODOS_VALIDOS:
        return s
    if s in ("rapido", "rápido"):
        return "fast"
    if s in ("completo",):
        return "complete"
    return None


def parse_timestamp_any(valor: object) -> Optional[int]:
    if valor is None:
        return None
    if isinstance(valor, (int, float)):
        n = int(valor)
        if n <= 0:
            return None
        return n if n > 10_000_000_000 else n * 1000

    s = str(valor).strip()
    if not s:
        return None
    if s.isdigit():
        n = int(s)
        return n if n > 10_000_000_000 else n * 1000

    s = s.replace("Z", "+00:00")
    try:
        d = dt.datetime.fromisoformat(s)
    except ValueError:
        return None
    if d.tzinfo is None:
        d = d.replace(tzinfo=dt.timezone.utc)
    return int(d.timestamp() * 1000)


def to_float(valor: object) -> Optional[float]:
    if valor is None:
        return None
    try:
        return float(valor)
    except (ValueError, TypeError):
        return None


def to_bool(valor: object) -> Optional[bool]:
    if valor is None:
        return None
    if isinstance(valor, bool):
        return valor
    if isinstance(valor, (int, float)):
        return bool(int(valor))
    s = str(valor).strip().lower()
    if s in ("true", "1", "sim", "yes"):
        return True
    if s in ("false", "0", "nao", "não", "no"):
        return False
    return None


def to_str(valor: object) -> Optional[str]:
    if valor is None:
        return None
    s = str(valor).strip()
    return s if s else None


def fmt(valor: Optional[float]) -> str:
    if valor is None:
        return ""
    return f"{valor:.6f}".rstrip("0").rstrip(".")


if __name__ == "__main__":
    raise SystemExit(main())
