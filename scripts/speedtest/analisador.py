"""analisador.py

Pos-processamento dos resultados fisicos coletados pelo `calibradorCloudflareIsolado.py`.

Responsabilidades:
1. Reagregar samples brutos das combinacoes fisicas usando parametros de
   pos-processamento (metodoCalculo, descarteWarmupPercentual,
   slowStartGuardSegundos).
2. Calcular diff% versus a referencia Cloudflare oficial.
3. Exportar 3 CSVs prontos para Excel pt-BR:
   - medicoesConsolidadas_*.csv
   - ranking_*.csv
   - refCloudflareOficial_*.csv
4. Gerar relatorio narrativo em Markdown.
"""
from __future__ import annotations

import csv
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Optional, Sequence

from variantesProprias import (
    ParamsMedicao,
    ResultadoFisico,
    Sample,
    reagregar,
)

CSV_SEP_BR = ";"
CSV_DECIMAL_BR = ","
CSV_ENCODING = "utf-8-sig"  # BOM => Excel pt-BR abre direto


@dataclass
class GradePosProcessamento:
    metodos: Sequence[str] = ("media", "p90", "p95")
    warmupsPercentuais: Sequence[float] = (0.0,)
    slowStartGuardsSegundos: Sequence[float] = (0.0,)


@dataclass
class Referencia:
    downloadMbps: float
    uploadMbps: float
    latenciaMs: Optional[float]
    jitterMs: Optional[float]
    p90DownloadMbps: Optional[float]
    p90UploadMbps: Optional[float]
    iniciadoEm: str
    finalizadoEm: str
    versaoSdk: Optional[str]
    ipPublicoMaquina: Optional[str]
    edgeIata: Optional[str]
    cruJson: dict


@dataclass
class RegistroMedicao:
    comboId: str
    comboFisicoId: str
    rodada: int
    payloadDownloadMB: int
    payloadUploadMB: int
    streamsParalelos: int
    modoJanela: str
    duracaoSegundos: int
    cvAlvoPercentual: float
    metodoCalculo: str
    descarteWarmupPercentual: float
    slowStartGuardSegundos: float
    downloadMbps: Optional[float]
    uploadMbps: Optional[float]
    referenciaDownloadMbps: float
    referenciaUploadMbps: float
    diffDownloadPct: Optional[float]
    diffUploadPct: Optional[float]
    diffMaxPct: Optional[float]
    dentroFaixa5a10: bool
    duracaoMedicaoSegundos: float
    amostrasColetadasDownload: int
    amostrasColetadasUpload: int
    motivoParadaDownload: str
    motivoParadaUpload: str
    ipServidorCloudflare: Optional[str]
    cfRay: Optional[str]
    timestampInicio: str
    timestampFim: str
    statusExecucao: str


# --------------------------------------------------------------------------- #
# Geracao de registros                                                        #
# --------------------------------------------------------------------------- #


def gerarRegistros(
    resultadosFisicos: List[ResultadoFisico],
    referencia: Referencia,
    grade: GradePosProcessamento,
) -> List[RegistroMedicao]:
    """Para cada resultado fisico, gera linhas reagregadas para todas as
    combinacoes da grade de pos-processamento."""
    registros: List[RegistroMedicao] = []
    for fisico in resultadosFisicos:
        for metodo in grade.metodos:
            for warmup in grade.warmupsPercentuais:
                for slowStart in grade.slowStartGuardsSegundos:
                    registros.append(_construirRegistro(
                        fisico=fisico,
                        referencia=referencia,
                        metodo=metodo,
                        warmup=warmup,
                        slowStart=slowStart,
                    ))
    return registros


def _construirRegistro(
    fisico: ResultadoFisico,
    referencia: Referencia,
    metodo: str,
    warmup: float,
    slowStart: float,
) -> RegistroMedicao:
    p = fisico.params
    dl = reagregar(
        fisico.download.samples,
        metodoCalculo=metodo,
        descarteWarmupPercentual=warmup,
        slowStartGuardSegundos=slowStart,
    )
    ul = reagregar(
        fisico.upload.samples,
        metodoCalculo=metodo,
        descarteWarmupPercentual=warmup,
        slowStartGuardSegundos=slowStart,
    )
    diffDl = _diffPct(dl, referencia.downloadMbps)
    diffUl = _diffPct(ul, referencia.uploadMbps)
    diffMax = _maxAbs(diffDl, diffUl)
    dentro = diffMax is not None and 5.0 <= diffMax <= 10.0

    statusExec = "ok"
    if fisico.download.erro or fisico.upload.erro:
        statusExec = "erro"
    elif fisico.download.statusHttp and fisico.download.statusHttp >= 400:
        statusExec = f"http{fisico.download.statusHttp}"

    sufixo = f"_{metodo}_W{int(warmup)}_SS{int(slowStart)}"
    comboLogico = f"{fisico.comboId}{sufixo}"

    duracaoTotal = fisico.download.duracaoSegundos + fisico.upload.duracaoSegundos
    return RegistroMedicao(
        comboId=comboLogico,
        comboFisicoId=fisico.comboId,
        rodada=fisico.rodada,
        payloadDownloadMB=p.payloadDownloadMB,
        payloadUploadMB=p.payloadUploadMB,
        streamsParalelos=p.streamsParalelos,
        modoJanela=p.modoJanela,
        duracaoSegundos=p.duracaoSegundos,
        cvAlvoPercentual=p.cvAlvoPercentual,
        metodoCalculo=metodo,
        descarteWarmupPercentual=warmup,
        slowStartGuardSegundos=slowStart,
        downloadMbps=_arredondar(dl),
        uploadMbps=_arredondar(ul),
        referenciaDownloadMbps=referencia.downloadMbps,
        referenciaUploadMbps=referencia.uploadMbps,
        diffDownloadPct=_arredondar(diffDl),
        diffUploadPct=_arredondar(diffUl),
        diffMaxPct=_arredondar(diffMax),
        dentroFaixa5a10=dentro,
        duracaoMedicaoSegundos=round(duracaoTotal, 2),
        amostrasColetadasDownload=len(fisico.download.samples),
        amostrasColetadasUpload=len(fisico.upload.samples),
        motivoParadaDownload=fisico.download.motivoParada,
        motivoParadaUpload=fisico.upload.motivoParada,
        ipServidorCloudflare=fisico.download.ipServidor or fisico.upload.ipServidor,
        cfRay=fisico.download.cfRay or fisico.upload.cfRay,
        timestampInicio=fisico.iniciadoEm,
        timestampFim=fisico.finalizadoEm,
        statusExecucao=statusExec,
    )


def _diffPct(valor: Optional[float], ref: float) -> Optional[float]:
    if valor is None or ref <= 0:
        return None
    return (valor - ref) / ref * 100.0


def _maxAbs(a: Optional[float], b: Optional[float]) -> Optional[float]:
    if a is None and b is None:
        return None
    return max(abs(a or 0.0), abs(b or 0.0))


def _arredondar(v: Optional[float], casas: int = 3) -> Optional[float]:
    return round(v, casas) if v is not None else None


# --------------------------------------------------------------------------- #
# Exportacao CSV                                                              #
# --------------------------------------------------------------------------- #


COLUNAS_CONSOLIDADO = [
    "comboId",
    "comboFisicoId",
    "rodada",
    "payloadDownloadMB",
    "payloadUploadMB",
    "streamsParalelos",
    "modoJanela",
    "duracaoSegundos",
    "cvAlvoPercentual",
    "metodoCalculo",
    "descarteWarmupPercentual",
    "slowStartGuardSegundos",
    "downloadMbps",
    "uploadMbps",
    "referenciaDownloadMbps",
    "referenciaUploadMbps",
    "diffDownloadPct",
    "diffUploadPct",
    "diffMaxPct",
    "dentroFaixa5a10",
    "duracaoMedicaoSegundos",
    "amostrasColetadasDownload",
    "amostrasColetadasUpload",
    "motivoParadaDownload",
    "motivoParadaUpload",
    "ipServidorCloudflare",
    "cfRay",
    "timestampInicio",
    "timestampFim",
    "statusExecucao",
]


def escreverCsvConsolidado(
    registros: Iterable[RegistroMedicao],
    caminho: Path,
    formatoBR: bool = True,
) -> None:
    sep = CSV_SEP_BR if formatoBR else ","
    enc = CSV_ENCODING if formatoBR else "utf-8"
    with caminho.open("w", encoding=enc, newline="") as f:
        writer = csv.writer(f, delimiter=sep, quoting=csv.QUOTE_MINIMAL)
        writer.writerow(COLUNAS_CONSOLIDADO)
        for r in registros:
            writer.writerow([_formatar(getattr(r, c), formatoBR) for c in COLUNAS_CONSOLIDADO])


COLUNAS_RANKING = [
    "comboId",
    "comboFisicoId",
    "payloadDownloadMB",
    "payloadUploadMB",
    "streamsParalelos",
    "modoJanela",
    "metodoCalculo",
    "descarteWarmupPercentual",
    "slowStartGuardSegundos",
    "downloadMbpsMedia",
    "uploadMbpsMedia",
    "downloadDesvioPadrao",
    "uploadDesvioPadrao",
    "referenciaDownloadMbps",
    "referenciaUploadMbps",
    "diffDownloadPctMedio",
    "diffUploadPctMedio",
    "diffMaxPctMedio",
    "numRodadas",
    "dentroFaixa5a10",
]


def escreverCsvRanking(
    registros: Iterable[RegistroMedicao],
    caminho: Path,
    formatoBR: bool = True,
) -> List[dict]:
    """Agrega por comboId (logico, ja considerando metodo/warmup/slowStart),
    media das rodadas, ordena por |diffMaxPct|."""
    porCombo: dict[str, List[RegistroMedicao]] = {}
    for r in registros:
        porCombo.setdefault(r.comboId, []).append(r)

    agregados: List[dict] = []
    for comboId, lista in porCombo.items():
        amostraDl = [r.downloadMbps for r in lista if r.downloadMbps is not None]
        amostraUl = [r.uploadMbps for r in lista if r.uploadMbps is not None]
        amostraDiffDl = [r.diffDownloadPct for r in lista if r.diffDownloadPct is not None]
        amostraDiffUl = [r.diffUploadPct for r in lista if r.diffUploadPct is not None]
        amostraDiffMax = [r.diffMaxPct for r in lista if r.diffMaxPct is not None]
        primeiro = lista[0]
        agregados.append({
            "comboId": comboId,
            "comboFisicoId": primeiro.comboFisicoId,
            "payloadDownloadMB": primeiro.payloadDownloadMB,
            "payloadUploadMB": primeiro.payloadUploadMB,
            "streamsParalelos": primeiro.streamsParalelos,
            "modoJanela": primeiro.modoJanela,
            "metodoCalculo": primeiro.metodoCalculo,
            "descarteWarmupPercentual": primeiro.descarteWarmupPercentual,
            "slowStartGuardSegundos": primeiro.slowStartGuardSegundos,
            "downloadMbpsMedia": _arredondar(_media(amostraDl)),
            "uploadMbpsMedia": _arredondar(_media(amostraUl)),
            "downloadDesvioPadrao": _arredondar(_dp(amostraDl)),
            "uploadDesvioPadrao": _arredondar(_dp(amostraUl)),
            "referenciaDownloadMbps": primeiro.referenciaDownloadMbps,
            "referenciaUploadMbps": primeiro.referenciaUploadMbps,
            "diffDownloadPctMedio": _arredondar(_media(amostraDiffDl)),
            "diffUploadPctMedio": _arredondar(_media(amostraDiffUl)),
            "diffMaxPctMedio": _arredondar(_media(amostraDiffMax)),
            "numRodadas": len(lista),
            "dentroFaixa5a10": (
                _media(amostraDiffMax) is not None
                and 5.0 <= _media(amostraDiffMax) <= 10.0
            ),
        })

    agregados.sort(key=lambda d: (
        9999.0 if d["diffMaxPctMedio"] is None else abs(d["diffMaxPctMedio"])
    ))

    sep = CSV_SEP_BR if formatoBR else ","
    enc = CSV_ENCODING if formatoBR else "utf-8"
    with caminho.open("w", encoding=enc, newline="") as f:
        writer = csv.writer(f, delimiter=sep, quoting=csv.QUOTE_MINIMAL)
        writer.writerow(COLUNAS_RANKING)
        for d in agregados:
            writer.writerow([_formatar(d[c], formatoBR) for c in COLUNAS_RANKING])

    return agregados


COLUNAS_REFERENCIA = [
    "iniciadoEm",
    "finalizadoEm",
    "downloadMbps",
    "uploadMbps",
    "latenciaMs",
    "jitterMs",
    "p90DownloadMbps",
    "p90UploadMbps",
    "versaoSdk",
    "ipPublicoMaquina",
    "edgeIata",
]


def escreverCsvReferencia(
    referencia: Referencia,
    caminho: Path,
    formatoBR: bool = True,
) -> None:
    sep = CSV_SEP_BR if formatoBR else ","
    enc = CSV_ENCODING if formatoBR else "utf-8"
    with caminho.open("w", encoding=enc, newline="") as f:
        writer = csv.writer(f, delimiter=sep, quoting=csv.QUOTE_MINIMAL)
        writer.writerow(COLUNAS_REFERENCIA)
        writer.writerow([
            _formatar(getattr(referencia, c, None), formatoBR)
            for c in COLUNAS_REFERENCIA
        ])


def _formatar(v, formatoBR: bool):
    if v is None:
        return ""
    if isinstance(v, bool):
        return "sim" if v and formatoBR else ("Sim" if v else "Nao") if formatoBR else v
    if isinstance(v, float):
        if formatoBR:
            return f"{v:.3f}".replace(".", CSV_DECIMAL_BR)
        return f"{v:.6f}"
    return v


def _media(valores: Sequence[float]) -> Optional[float]:
    if not valores:
        return None
    return sum(valores) / len(valores)


def _dp(valores: Sequence[float]) -> Optional[float]:
    if len(valores) < 2:
        return None
    m = _media(valores)
    if m is None:
        return None
    return (sum((v - m) ** 2 for v in valores) / len(valores)) ** 0.5


# --------------------------------------------------------------------------- #
# Relatorio Markdown                                                          #
# --------------------------------------------------------------------------- #


def gerarRelatorioMd(
    registros: List[RegistroMedicao],
    rankingAgregado: List[dict],
    referencia: Referencia,
    caminhoMd: Path,
    nomesCsv: dict,
) -> None:
    dentroFaixa = [d for d in rankingAgregado if d["dentroFaixa5a10"]]
    maisProximas = rankingAgregado[:5]
    topFaixa = dentroFaixa[:5]

    linhas = [
        "# Relatorio do calibrador Cloudflare Speedtest",
        "",
        f"- **Inicio:** {referencia.iniciadoEm}",
        f"- **Fim:** {referencia.finalizadoEm}",
        f"- **IP publico:** `{referencia.ipPublicoMaquina or 'desconhecido'}`",
        f"- **Edge Cloudflare (IATA):** `{referencia.edgeIata or 'desconhecido'}`",
        f"- **Versao SDK Cloudflare:** `{referencia.versaoSdk or 'desconhecida'}`",
        "",
        "## Referencia oficial",
        "",
        f"| Metrica | Valor |",
        f"|---|---|",
        f"| Download (Mbps) | {referencia.downloadMbps} |",
        f"| Upload (Mbps) | {referencia.uploadMbps} |",
        f"| Latencia (ms) | {referencia.latenciaMs} |",
        f"| Jitter (ms) | {referencia.jitterMs} |",
        f"| P90 Download (Mbps) | {referencia.p90DownloadMbps} |",
        f"| P90 Upload (Mbps) | {referencia.p90UploadMbps} |",
        "",
        "## Resumo numerico",
        "",
        f"- Total de registros (combinacoes x rodadas): **{len(registros)}**",
        f"- Combinacoes logicas distintas: **{len(rankingAgregado)}**",
        f"- Combinacoes dentro da faixa 5-10%: **{len(dentroFaixa)}**",
        "",
    ]

    if topFaixa:
        linhas.extend([
            "## Top 5 dentro da faixa-alvo (5%-10% de diferenca)",
            "",
            _tabelaRanking(topFaixa),
            "",
        ])
    else:
        linhas.extend([
            "## Faixa-alvo 5%-10%",
            "",
            "Nenhuma combinacao caiu dentro da faixa nesta execucao. Veja proximas mais proximas abaixo.",
            "",
        ])

    linhas.extend([
        "## Top 5 mais proximas da referencia (qualquer faixa)",
        "",
        _tabelaRanking(maisProximas),
        "",
        "## Arquivos gerados",
        "",
        f"- Tabela completa (Excel pt-BR): `{nomesCsv['consolidado']}`",
        f"- Ranking agregado (Excel pt-BR): `{nomesCsv['ranking']}`",
        f"- Referencia Cloudflare (Excel pt-BR): `{nomesCsv['referencia']}`",
        f"- Log forense JSONL: `{nomesCsv['log']}`",
        "",
        "## Como ler",
        "",
        "1. Abra `ranking_*.csv` no Excel: a primeira linha e a configuracao com menor diff%.",
        "2. Filtre `dentroFaixa5a10 = sim` para combinacoes na faixa-alvo.",
        "3. Use `medicoesConsolidadas_*.csv` para ver todas as rodadas individuais.",
        "4. Em caso de falha, abra `log_*.jsonl` e procure linhas com `tipo:erro`.",
    ])

    caminhoMd.write_text("\n".join(linhas), encoding="utf-8")


def _tabelaRanking(linhas: List[dict]) -> str:
    cabec = (
        "| comboId | DL Mbps | UL Mbps | diffDL% | diffUL% | diffMax% | rodadas |"
    )
    sep = "|---|---|---|---|---|---|---|"
    rows = [cabec, sep]
    for d in linhas:
        rows.append(
            f"| `{d['comboId']}` "
            f"| {d['downloadMbpsMedia']} "
            f"| {d['uploadMbpsMedia']} "
            f"| {d['diffDownloadPctMedio']} "
            f"| {d['diffUploadPctMedio']} "
            f"| {d['diffMaxPctMedio']} "
            f"| {d['numRodadas']} |"
        )
    return "\n".join(rows)


# --------------------------------------------------------------------------- #
# Carregamento de log JSONL (helper para reanalise offline)                   #
# --------------------------------------------------------------------------- #


def carregarReferenciaDoJson(caminho: Path) -> Referencia:
    dados = json.loads(caminho.read_text(encoding="utf-8"))
    edge = dados.get("edgeCloudflare") or {}
    return Referencia(
        downloadMbps=float(dados["downloadMbps"]),
        uploadMbps=float(dados["uploadMbps"]),
        latenciaMs=dados.get("latenciaMs"),
        jitterMs=dados.get("jitterMs"),
        p90DownloadMbps=dados.get("p90DownloadMbps"),
        p90UploadMbps=dados.get("p90UploadMbps"),
        iniciadoEm=dados.get("iniciadoEm", ""),
        finalizadoEm=dados.get("finalizadoEm", ""),
        versaoSdk=dados.get("versaoSdk"),
        ipPublicoMaquina=dados.get("ipPublicoMaquina"),
        edgeIata=edge.get("iata") if isinstance(edge, dict) else None,
        cruJson=dados,
    )


__all__ = [
    "GradePosProcessamento",
    "Referencia",
    "RegistroMedicao",
    "gerarRegistros",
    "escreverCsvConsolidado",
    "escreverCsvRanking",
    "escreverCsvReferencia",
    "gerarRelatorioMd",
    "carregarReferenciaDoJson",
]
