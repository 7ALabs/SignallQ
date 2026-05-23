"""calibradorCloudflareIsolado.py

Orquestrador principal do calibrador isolado de Speedtest. Vive FORA do app
Flutter (`source/app/lib/`) e nao toca em nada do projeto LINKA.

Fluxo:
  Fase A - Coleta a referencia oficial via coletorReferenciaOficial.py (replica
           o algoritmo do SDK Cloudflare em Python puro: tiers crescentes + P90).
  Fase B - Varre a matriz fisica de combinacoes (payload x streams).
  Fase C - Reagrega a matriz fisica com grade de pos-processamento
           (metodoCalculo x warmup x slowStartGuard) sem chamar a rede.
  Fase D - Gera CSVs e relatorio.

Uso:
  python calibradorCloudflareIsolado.py [--smoke] [--debug] [--csv-internacional]
                                        [--rodadas 2] [--saida resultados]
                                        [--auto-confirmar]
"""
from __future__ import annotations

import argparse
import asyncio
import json
import os
import platform
import socket
import sys
import time
from dataclasses import asdict
from pathlib import Path
from typing import List, Optional

from rich.console import Console
from rich.panel import Panel
from rich.progress import (
    BarColumn,
    Progress,
    SpinnerColumn,
    TextColumn,
    TimeElapsedColumn,
)
from rich.table import Table

from analisador import (
    GradePosProcessamento,
    Referencia,
    escreverCsvConsolidado,
    escreverCsvRanking,
    escreverCsvReferencia,
    gerarRegistros,
    gerarRelatorioMd,
)
from coletorReferenciaOficial import ResultadoReferencia, coletarReferencia
from variantesProprias import (
    ParamsMedicao,
    ResultadoFisico,
    Sample,
    gerarComboId,
    medirComParametros,
)

CONSOLE = Console()
PASTA_RAIZ = Path(__file__).resolve().parent

# Matriz da Fase B (combinacoes fisicas que exigem chamada de rede)
MATRIZ_FISICA = [
    {"payloadDownloadMB": 50, "payloadUploadMB": 25, "streamsParalelos": 1},
    {"payloadDownloadMB": 50, "payloadUploadMB": 25, "streamsParalelos": 4},
    {"payloadDownloadMB": 50, "payloadUploadMB": 25, "streamsParalelos": 8},
    {"payloadDownloadMB": 100, "payloadUploadMB": 25, "streamsParalelos": 1},
    {"payloadDownloadMB": 100, "payloadUploadMB": 25, "streamsParalelos": 4},
    {"payloadDownloadMB": 100, "payloadUploadMB": 25, "streamsParalelos": 8},
    {"payloadDownloadMB": 200, "payloadUploadMB": 25, "streamsParalelos": 1},
    {"payloadDownloadMB": 200, "payloadUploadMB": 25, "streamsParalelos": 4},
    {"payloadDownloadMB": 200, "payloadUploadMB": 25, "streamsParalelos": 8},
]

# Smoke usa payload <=5MB para nao acionar rate-limit Cloudflare (>=10MB e bloqueado)
MATRIZ_FISICA_SMOKE = [
    {"payloadDownloadMB": 5, "payloadUploadMB": 5, "streamsParalelos": 1},
]
PAYLOAD_MAX_MB_SMOKE = 5  # cap para Fase A no modo smoke

# Grade da Fase C (pos-processamento - nao gera trafego)
GRADE_POS = GradePosProcessamento(
    metodos=("media", "p90", "p95"),
    warmupsPercentuais=(0.0, 15.0, 30.0),
    slowStartGuardsSegundos=(0.0, 2.0, 4.0),
)

GRADE_POS_SMOKE = GradePosProcessamento(
    metodos=("media", "p90"),
    warmupsPercentuais=(0.0,),
    slowStartGuardsSegundos=(0.0,),
)

PAUSA_ANTI_BLOQUEIO_S = 60        # pausa entre combos fisicos
BACKOFF_PROGRESSIVO_S = (120, 600, 3600)  # backoff em 429: 2min → 10min → 1h
PAUSA_APOS_FASE_A_S = 600         # cooldown apos referencia (full run, 10 min)
PAUSA_APOS_FASE_A_SMOKE_S = 60   # cooldown reduzido para --smoke


# --------------------------------------------------------------------------- #
# Estado/log forense                                                          #
# --------------------------------------------------------------------------- #


class LogForense:
    """Gravador JSONL append-only. Resistente a crash."""

    def __init__(self, caminho: Path, debug: bool = False):
        self.caminho = caminho
        self.debug = debug
        self._handle = caminho.open("a", encoding="utf-8")

    def registrar(self, evento: dict) -> None:
        if not self.debug and evento.get("tipo") in {"sample", "conexao"}:
            return
        if "ts" not in evento:
            evento["ts"] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
        self._handle.write(json.dumps(evento, ensure_ascii=False) + "\n")
        self._handle.flush()

    def fechar(self) -> None:
        self._handle.close()


# --------------------------------------------------------------------------- #
# Fase A - Referencia Cloudflare oficial                                      #
# --------------------------------------------------------------------------- #


def _referenciaRecente(saida: Path, maxIdadeSeg: int = 3600) -> Optional[dict]:
    """Retorna o JSON da referencia mais recente se tiver menos de maxIdadeSeg."""
    candidatos = sorted(saida.glob("refCloudflare_*.json"), reverse=True)
    agora = time.time()
    for arq in candidatos:
        if (agora - arq.stat().st_mtime) < maxIdadeSeg:
            try:
                return json.loads(arq.read_text(encoding="utf-8"))
            except Exception:
                continue
    return None


def faseA_referenciaOficial(saida: Path, pularFaseA: bool = False, payloadMaxMB: Optional[int] = None) -> Referencia:
    CONSOLE.print(Panel.fit("[bold cyan]Fase A[/] — Cloudflare oficial (referencia)"))

    dados: Optional[dict] = None

    if pularFaseA:
        dados = _referenciaRecente(saida)
        if dados:
            CONSOLE.print(
                "[yellow]--pular-fase-a: reutilizando referencia salva "
                f"(menos de 1h) — {dados.get('iniciadoEm', '?')}[/]"
            )
        else:
            CONSOLE.print(
                "[red]--pular-fase-a solicitado mas nenhuma referencia recente encontrada "
                "em resultados/. Sera necessario executar Fase A.[/]"
            )
            pularFaseA = False

    if not pularFaseA:
        with CONSOLE.status(
            "[cyan]Executando medicao de referencia Cloudflare "
            "(tiers crescentes + P90 — pode levar ate 90s)...[/]"
        ):
            try:
                res: ResultadoReferencia = asyncio.run(coletarReferencia(payloadMaxMB=payloadMaxMB))
            except Exception as e:
                CONSOLE.print(f"[red]Erro na coleta de referencia:[/] {e}")
                sys.exit(2)

        dados = res.paraDicionario()
        arquivo = saida / f"refCloudflare_{_carimboTempo()}.json"
        arquivo.write_text(json.dumps(dados, indent=2, ensure_ascii=False), encoding="utf-8")
        CONSOLE.print(f"[dim]JSON cru salvo em:[/] {arquivo.relative_to(PASTA_RAIZ)}")

    edge = dados.get("edgeCloudflare") or {}
    referencia = Referencia(
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

    tabela = Table(title="Referencia Cloudflare (replica algoritmo oficial)", show_header=True)
    tabela.add_column("Metrica", style="bold")
    tabela.add_column("Valor", justify="right")
    tabela.add_row("Download P90 (Mbps)", f"[bold green]{referencia.downloadMbps}[/]")
    tabela.add_row("Upload P90 (Mbps)", f"[bold green]{referencia.uploadMbps}[/]")
    tabela.add_row("Latencia mediana (ms)", f"{referencia.latenciaMs}")
    tabela.add_row("Jitter (ms)", f"{referencia.jitterMs}")
    tabela.add_row("Edge (IATA)", f"{referencia.edgeIata}")
    tabela.add_row("IP publico", f"{referencia.ipPublicoMaquina}")
    CONSOLE.print(tabela)

    return referencia


# --------------------------------------------------------------------------- #
# Fase B - Varredura fisica                                                   #
# --------------------------------------------------------------------------- #


async def faseB_varreduraFisica(
    rodadas: int,
    log: LogForense,
    saida: Path,
    smoke: bool,
) -> List[ResultadoFisico]:
    CONSOLE.print(
        Panel.fit("[bold cyan]Fase B[/] — Varredura fisica (chamadas de rede)")
    )

    matriz = MATRIZ_FISICA_SMOKE if smoke else MATRIZ_FISICA
    rodadas = 1 if smoke else rodadas

    if not smoke:
        falhouSmoke = await _smokeAntiBloqueio(log)
        if falhouSmoke:
            CONSOLE.print(
                "[red]Smoke test inicial falhou (provavel rate-limit/bloqueio).[/]"
            )
            CONSOLE.print(
                "Aguarde ~30 minutos e tente novamente. Encerrando esta execucao."
            )
            sys.exit(3)

    resultados: List[ResultadoFisico] = []
    falhasConsecutivas = 0

    total = len(matriz) * rodadas
    with Progress(
        SpinnerColumn(),
        TextColumn("[progress.description]{task.description}"),
        BarColumn(),
        TextColumn("{task.completed}/{task.total}"),
        TimeElapsedColumn(),
        console=CONSOLE,
    ) as progresso:
        tarefa = progresso.add_task("Medindo combinacoes fisicas", total=total)

        for combo in matriz:
            for rodada in range(1, rodadas + 1):
                params = ParamsMedicao(
                    payloadDownloadMB=combo["payloadDownloadMB"],
                    payloadUploadMB=combo["payloadUploadMB"],
                    streamsParalelos=combo["streamsParalelos"],
                    modoJanela="estabilizacaoCV",
                    cvAlvoPercentual=10.0,
                )
                comboId = gerarComboId(params)
                progresso.update(
                    tarefa,
                    description=f"[cyan]{comboId}[/] (rodada {rodada}/{rodadas})",
                )

                resultado = await medirComParametros(
                    params=params,
                    comboId=comboId,
                    rodada=rodada,
                    registrarEvento=log.registrar,
                )
                resultados.append(resultado)
                _salvarSamples(resultado, saida)

                # Detecta falha em qualquer das direcoes
                houveFalha = (
                    resultado.download.erro is not None
                    or resultado.upload.erro is not None
                    or (
                        resultado.download.statusHttp
                        and resultado.download.statusHttp in (403, 429)
                    )
                    or (
                        resultado.upload.statusHttp
                        and resultado.upload.statusHttp in (403, 429)
                    )
                )
                if houveFalha:
                    CONSOLE.print(
                        f"[dim]debug falha: dl.erro={resultado.download.erro!r} "
                        f"ul.erro={resultado.upload.erro!r} "
                        f"dl.status={resultado.download.statusHttp} "
                        f"ul.status={resultado.upload.statusHttp}[/]"
                    )
                    falhasConsecutivas += 1
                    if falhasConsecutivas >= 3:
                        CONSOLE.print(
                            "[red]3 falhas consecutivas. Abortando para nao agravar bloqueio.[/]"
                        )
                        log.registrar({"tipo": "abortado", "motivo": "3falhasSeguidas"})
                        return resultados
                    espera = BACKOFF_PROGRESSIVO_S[
                        min(falhasConsecutivas - 1, len(BACKOFF_PROGRESSIVO_S) - 1)
                    ]
                    CONSOLE.print(
                        f"[yellow]Falha detectada — backoff de {espera}s antes de continuar[/]"
                    )
                    log.registrar({
                        "tipo": "backoff",
                        "duracaoSegundos": espera,
                        "tentativaNumero": falhasConsecutivas,
                    })
                    await asyncio.sleep(espera)
                else:
                    falhasConsecutivas = 0
                    await asyncio.sleep(PAUSA_ANTI_BLOQUEIO_S)

                progresso.advance(tarefa)

    return resultados


async def _smokeAntiBloqueio(log: LogForense) -> bool:
    CONSOLE.print("[dim]Smoke test inicial (10 MB) para verificar rate-limit...[/]")
    params = ParamsMedicao(
        payloadDownloadMB=10,
        payloadUploadMB=5,
        streamsParalelos=1,
        modoJanela="tempoFixo",
        duracaoSegundos=8,
    )
    res = await medirComParametros(
        params=params,
        comboId="SMOKE",
        rodada=0,
        registrarEvento=log.registrar,
    )
    falhou = (
        res.download.erro is not None
        or (res.download.statusHttp and res.download.statusHttp in (403, 429))
    )
    if not falhou:
        CONSOLE.print("[green]Smoke OK — IP nao parece bloqueado.[/]")
    return falhou


def _salvarSamples(resultado: ResultadoFisico, pastaSaida: Path) -> None:
    pasta = pastaSaida / "samples"
    pasta.mkdir(parents=True, exist_ok=True)
    base = f"{resultado.comboId}_r{resultado.rodada}"

    def gravar(direcao: str, samples: List[Sample]):
        arq = pasta / f"{base}_{direcao}.csv"
        with arq.open("w", encoding="utf-8", newline="") as f:
            f.write("streamId,tsRelativoSegundos,bytesAcumulados,taxaIntervaloMbps\n")
            for s in samples:
                f.write(
                    f"{s.streamId},{s.tsRelativoSegundos:.3f},"
                    f"{s.bytesAcumulados},{s.taxaIntervaloMbps:.4f}\n"
                )

    gravar("download", resultado.download.samples)
    gravar("upload", resultado.upload.samples)


# --------------------------------------------------------------------------- #
# Fase C/D - Pos-processamento e relatorio                                    #
# --------------------------------------------------------------------------- #


def faseC_D_posProcessamentoERelatorio(
    resultados: List[ResultadoFisico],
    referencia: Referencia,
    grade: GradePosProcessamento,
    saida: Path,
    formatoBR: bool,
    log: LogForense,
) -> None:
    CONSOLE.print(
        Panel.fit("[bold cyan]Fase C+D[/] — Reagregacao + CSVs + relatorio")
    )

    if not resultados:
        CONSOLE.print("[red]Nenhum resultado fisico para processar.[/]")
        return

    registros = gerarRegistros(resultados, referencia, grade)
    timestamp = _carimboTempo()
    nomes = {
        "consolidado": f"medicoesConsolidadas_{timestamp}.csv",
        "ranking": f"ranking_{timestamp}.csv",
        "referencia": f"refCloudflareOficial_{timestamp}.csv",
        "log": _nomeLog(saida),
    }

    arquivos = {chave: saida / nome for chave, nome in nomes.items()}

    escreverCsvConsolidado(registros, arquivos["consolidado"], formatoBR=formatoBR)
    rankingAgregado = escreverCsvRanking(
        registros, arquivos["ranking"], formatoBR=formatoBR
    )
    escreverCsvReferencia(referencia, arquivos["referencia"], formatoBR=formatoBR)

    relatorioMd = saida / f"relatorio_{timestamp}.md"
    gerarRelatorioMd(
        registros=registros,
        rankingAgregado=rankingAgregado,
        referencia=referencia,
        caminhoMd=relatorioMd,
        nomesCsv=nomes,
    )

    log.registrar({
        "tipo": "execEnd",
        "totalRegistros": len(registros),
        "combinacoesLogicas": len(rankingAgregado),
        "arquivos": nomes,
    })

    CONSOLE.print(f"[green]Relatorio:[/] {relatorioMd.relative_to(PASTA_RAIZ)}")
    CONSOLE.print(f"[green]Ranking CSV:[/] {arquivos['ranking'].relative_to(PASTA_RAIZ)}")
    CONSOLE.print(
        f"[green]Tabela completa CSV:[/] {arquivos['consolidado'].relative_to(PASTA_RAIZ)}"
    )

    dentroFaixa = [d for d in rankingAgregado if d["dentroFaixa5a10"]]
    if dentroFaixa:
        CONSOLE.print(
            f"\n[bold green]{len(dentroFaixa)} combinacao(oes) caiu na faixa 5%-10%.[/]"
        )
        topo = dentroFaixa[0]
        CONSOLE.print(
            f"Melhor candidata: [cyan]{topo['comboId']}[/] "
            f"(diff max medio = {topo['diffMaxPctMedio']}%)"
        )
    else:
        CONSOLE.print(
            "\n[yellow]Nenhuma combinacao na faixa 5%-10%. Veja relatorio para top mais proximas.[/]"
        )


# --------------------------------------------------------------------------- #
# Setup/contexto                                                              #
# --------------------------------------------------------------------------- #


def _carimboTempo() -> str:
    return time.strftime("%Y-%m-%dT%H%M", time.gmtime())


def _nomeLog(saida: Path) -> str:
    return f"log_{_carimboTempo()}.jsonl"


def _gravarContexto(saida: Path) -> Path:
    info = {
        "ts": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "sistemaOperacional": platform.system() + " " + platform.release(),
        "versaoPython": platform.python_version(),
        "hostname": socket.gethostname(),
    }
    arq = saida / f"contexto_{_carimboTempo()}.json"
    arq.write_text(json.dumps(info, indent=2, ensure_ascii=False), encoding="utf-8")
    return arq


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.strip().splitlines()[0])
    parser.add_argument("--smoke", action="store_true", help="Roda 1 combinacao apenas para validar pipeline")
    parser.add_argument("--debug", action="store_true", help="Liga eventos volumosos no log JSONL")
    parser.add_argument(
        "--csv-internacional",
        action="store_true",
        help="CSV com vircula como separador e ponto decimal (pandas)",
    )
    parser.add_argument("--rodadas", type=int, default=2, help="Rodadas por combinacao fisica (default 2)")
    parser.add_argument("--saida", type=str, default="resultados", help="Pasta de saida (relativa a script)")
    parser.add_argument(
        "--auto-confirmar",
        action="store_true",
        help="Pula a confirmacao manual entre Fase A e Fase B",
    )
    parser.add_argument(
        "--pular-fase-a",
        action="store_true",
        help="Reutiliza a referencia Cloudflare mais recente (< 1h) — evita refazer downloads desnecessarios",
    )
    args = parser.parse_args()

    saida = PASTA_RAIZ / args.saida
    saida.mkdir(parents=True, exist_ok=True)

    timestamp = _carimboTempo()
    caminhoLog = saida / f"log_{timestamp}.jsonl"
    log = LogForense(caminhoLog, debug=args.debug)
    log.registrar({
        "tipo": "execStart",
        "versaoPython": platform.python_version(),
        "sistemaOperacional": platform.system() + " " + platform.release(),
        "argv": sys.argv,
    })

    arqContexto = _gravarContexto(saida)
    CONSOLE.print(f"[dim]Contexto salvo em:[/] {arqContexto.relative_to(PASTA_RAIZ)}")

    try:
        payloadMaxMB = PAYLOAD_MAX_MB_SMOKE if args.smoke else None
        referencia = faseA_referenciaOficial(saida, pularFaseA=args.pular_fase_a, payloadMaxMB=payloadMaxMB)

        if not args.auto_confirmar:
            CONSOLE.print(
                "\n[bold]Confirme se deseja prosseguir para a Fase B (varredura).[/] "
                "Pressione [green]Enter[/] para continuar ou Ctrl+C para abortar."
            )
            try:
                input()
            except KeyboardInterrupt:
                CONSOLE.print("\n[yellow]Abortado pelo usuario.[/]")
                log.registrar({"tipo": "abortado", "motivo": "ctrlCnoFaseA"})
                return 1

        if not args.pular_fase_a:
            pausa_a_b = PAUSA_APOS_FASE_A_SMOKE_S if args.smoke else PAUSA_APOS_FASE_A_S
            CONSOLE.print(
                f"[dim]Aguardando {pausa_a_b}s de cooldown pos-Fase-A "
                f"para limpar rate-limit Cloudflare...[/]"
            )
            for restante in range(pausa_a_b, 0, -10):
                CONSOLE.print(f"[dim]  {restante}s restantes...[/]")
                time.sleep(min(10, restante))

        resultados = asyncio.run(
            faseB_varreduraFisica(
                rodadas=args.rodadas,
                log=log,
                saida=saida,
                smoke=args.smoke,
            )
        )

        gradeUsada = GRADE_POS_SMOKE if args.smoke else GRADE_POS
        faseC_D_posProcessamentoERelatorio(
            resultados=resultados,
            referencia=referencia,
            grade=gradeUsada,
            saida=saida,
            formatoBR=not args.csv_internacional,
            log=log,
        )
        return 0
    except KeyboardInterrupt:
        CONSOLE.print("\n[yellow]Interrompido pelo usuario.[/]")
        log.registrar({"tipo": "abortado", "motivo": "ctrlC"})
        return 1
    finally:
        log.fechar()


if __name__ == "__main__":
    sys.exit(main())
