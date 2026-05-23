"""variantesProprias.py

Implementacao parametrizada de medicao Speedtest contra os endpoints publicos
do Cloudflare (`speed.cloudflare.com/__down` e `__up`). Coleta samples brutos
(taxa instantanea por intervalo de 250ms) por stream e devolve de forma que o
analisador possa REAGREGAR sem novas chamadas de rede.

Reagregacao (metodoCalculo, descarteWarmupPercentual, slowStartGuardSegundos)
acontece em pos-processamento sobre os samples brutos.
"""
from __future__ import annotations

import asyncio
import json
import socket
import statistics
import time
import uuid
from dataclasses import asdict, dataclass, field
from typing import Awaitable, Callable, Iterable, List, Optional

import httpx

CLOUDFLARE_DOWN = "https://speed.cloudflare.com/__down"
CLOUDFLARE_UP = "https://speed.cloudflare.com/__up"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)
HEADERS_BASE = {
    "User-Agent": USER_AGENT,
    "Accept-Language": "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7",
    "Accept": "*/*",
    "Accept-Encoding": "identity",  # evita gzip que distorceria a medicao
}

INTERVALO_AMOSTRA_SEGUNDOS = 0.05  # 50ms — necessario para GbE+ onde 50MB completa em <500ms
DURACAO_MAXIMA_SEGUNDOS = 30.0
JANELA_ESTABILIZACAO_AMOSTRAS = 4
LimitsRedeRegistrar = Callable[[dict], None]


# --------------------------------------------------------------------------- #
# Tipos                                                                       #
# --------------------------------------------------------------------------- #


@dataclass
class ParamsMedicao:
    payloadDownloadMB: int
    payloadUploadMB: int
    streamsParalelos: int
    modoJanela: str  # "tempoFixo" | "estabilizacaoCV"
    duracaoSegundos: int = 15
    cvAlvoPercentual: float = 10.0
    timeoutSegundos: float = 60.0

    def comoDicionario(self) -> dict:
        return asdict(self)


@dataclass
class Sample:
    streamId: int
    tsRelativoSegundos: float
    bytesAcumulados: int
    taxaIntervaloMbps: float


@dataclass
class ResultadoDirecao:
    direcao: str  # "download" | "upload"
    samples: List[Sample]
    duracaoSegundos: float
    motivoParada: str  # "cvAtingido" | "tempoMaximo" | "completo" | "erro"
    bytesTotal: int
    cfRay: Optional[str]
    ipServidor: Optional[str]
    ttfbMs: Optional[float]
    statusHttp: Optional[int]
    erro: Optional[str] = None


@dataclass
class ResultadoFisico:
    comboId: str
    rodada: int
    params: ParamsMedicao
    download: ResultadoDirecao
    upload: ResultadoDirecao
    iniciadoEm: str
    finalizadoEm: str

    def comoDicionario(self) -> dict:
        return {
            "comboId": self.comboId,
            "rodada": self.rodada,
            "params": self.params.comoDicionario(),
            "download": _dirAsDict(self.download),
            "upload": _dirAsDict(self.upload),
            "iniciadoEm": self.iniciadoEm,
            "finalizadoEm": self.finalizadoEm,
        }


def _dirAsDict(r: ResultadoDirecao) -> dict:
    return {
        "direcao": r.direcao,
        "duracaoSegundos": r.duracaoSegundos,
        "motivoParada": r.motivoParada,
        "bytesTotal": r.bytesTotal,
        "cfRay": r.cfRay,
        "ipServidor": r.ipServidor,
        "ttfbMs": r.ttfbMs,
        "statusHttp": r.statusHttp,
        "erro": r.erro,
        "samples": [asdict(s) for s in r.samples],
    }


# --------------------------------------------------------------------------- #
# Reagregacao (pos-processamento, sem rede)                                   #
# --------------------------------------------------------------------------- #


def reagregar(
    samples: List[Sample],
    metodoCalculo: str = "media",
    descarteWarmupPercentual: float = 0.0,
    slowStartGuardSegundos: float = 0.0,
) -> Optional[float]:
    """Reduz lista de samples a um unico valor Mbps usando o metodo escolhido.

    `metodoCalculo`: "media" | "mediana" | "p75" | "p90" | "p95"
    `descarteWarmupPercentual`: 0..100
    `slowStartGuardSegundos`: pula amostras com tsRelativoSegundos < N
    """
    if not samples:
        return None

    serie = _seriarPorIntervalo(samples)
    if not serie:
        return None

    serie = [(t, mbps) for (t, mbps) in serie if t >= slowStartGuardSegundos]
    if not serie:
        return None

    descartar = int(len(serie) * (descarteWarmupPercentual / 100.0))
    serie = serie[descartar:]
    if not serie:
        return None

    valores = [mbps for (_, mbps) in serie]
    return _aplicarMetodo(valores, metodoCalculo)


def _seriarPorIntervalo(samples: List[Sample]) -> List[tuple]:
    """Agrega samples (que podem vir de varios streams) em uma serie temporal
    unica de Mbps somando-se os streams a cada intervalo."""
    if not samples:
        return []
    porIntervalo: dict[int, float] = {}
    for s in samples:
        bucket = int(s.tsRelativoSegundos / INTERVALO_AMOSTRA_SEGUNDOS)
        porIntervalo[bucket] = porIntervalo.get(bucket, 0.0) + s.taxaIntervaloMbps
    serie = sorted(
        [(b * INTERVALO_AMOSTRA_SEGUNDOS, mbps) for b, mbps in porIntervalo.items()]
    )
    return serie


def _aplicarMetodo(valores: List[float], metodo: str) -> float:
    if metodo == "media":
        return float(statistics.mean(valores))
    if metodo == "mediana":
        return float(statistics.median(valores))
    if metodo in ("p75", "p90", "p95"):
        pct = {"p75": 75, "p90": 90, "p95": 95}[metodo]
        return float(_percentil(valores, pct))
    raise ValueError(f"metodoCalculo desconhecido: {metodo}")


def _percentil(valores: List[float], pct: int) -> float:
    if not valores:
        return 0.0
    s = sorted(valores)
    k = (len(s) - 1) * (pct / 100.0)
    f = int(k)
    c = min(f + 1, len(s) - 1)
    if f == c:
        return s[f]
    return s[f] + (s[c] - s[f]) * (k - f)


def coeficienteVariacao(valores: Iterable[float]) -> Optional[float]:
    valores = list(valores)
    if len(valores) < 2:
        return None
    media = statistics.mean(valores)
    if media <= 0:
        return None
    return statistics.pstdev(valores) / media * 100.0


# --------------------------------------------------------------------------- #
# Medicao fisica                                                              #
# --------------------------------------------------------------------------- #


async def medirComParametros(
    params: ParamsMedicao,
    comboId: str,
    rodada: int,
    registrarEvento: Optional[LimitsRedeRegistrar] = None,
) -> ResultadoFisico:
    """Executa uma medicao DL+UL fisica com os parametros recebidos."""
    iniciadoEm = _agora()
    log = registrarEvento or (lambda _: None)

    log(
        {
            "tipo": "medicaoStart",
            "comboId": comboId,
            "rodada": rodada,
            "params": params.comoDicionario(),
            "ts": _agora(),
        }
    )

    download = await _medirDirecao(
        direcao="download",
        params=params,
        comboId=comboId,
        rodada=rodada,
        registrar=log,
    )

    # Pequeno respiro entre DL e UL para nao misturar congestion control.
    await asyncio.sleep(2.0)

    upload = await _medirDirecao(
        direcao="upload",
        params=params,
        comboId=comboId,
        rodada=rodada,
        registrar=log,
    )

    finalizadoEm = _agora()
    log(
        {
            "tipo": "medicaoEnd",
            "comboId": comboId,
            "rodada": rodada,
            "downloadMbps": reagregar(download.samples, metodoCalculo="media"),
            "uploadMbps": reagregar(upload.samples, metodoCalculo="media"),
            "ts": finalizadoEm,
        }
    )

    return ResultadoFisico(
        comboId=comboId,
        rodada=rodada,
        params=params,
        download=download,
        upload=upload,
        iniciadoEm=iniciadoEm,
        finalizadoEm=finalizadoEm,
    )


async def _medirDirecao(
    direcao: str,
    params: ParamsMedicao,
    comboId: str,
    rodada: int,
    registrar: LimitsRedeRegistrar,
) -> ResultadoDirecao:
    inicio = time.monotonic()
    samples: List[Sample] = []
    samples_lock = asyncio.Lock()
    parar = asyncio.Event()
    bytes_total = [0]
    metadata = {"cfRay": None, "ipServidor": None, "ttfbMs": None, "statusHttp": None}
    erro = None

    bytesPayload = (
        params.payloadDownloadMB if direcao == "download" else params.payloadUploadMB
    ) * 1024 * 1024

    async def supervisor():
        """Encerra a medicao por tempo OU por estabilizacao de CV."""
        ultimoBucket = -1
        while not parar.is_set():
            await asyncio.sleep(INTERVALO_AMOSTRA_SEGUNDOS)
            decorrido = time.monotonic() - inicio
            if decorrido >= DURACAO_MAXIMA_SEGUNDOS:
                metadata["motivoParada"] = "tempoMaximo"
                parar.set()
                return
            if (
                params.modoJanela == "tempoFixo"
                and decorrido >= params.duracaoSegundos
            ):
                metadata["motivoParada"] = "completo"
                parar.set()
                return
            if params.modoJanela == "estabilizacaoCV" and decorrido >= 5.0:
                async with samples_lock:
                    serie = _seriarPorIntervalo(samples)
                if len(serie) >= JANELA_ESTABILIZACAO_AMOSTRAS:
                    ultimas = [v for _, v in serie[-JANELA_ESTABILIZACAO_AMOSTRAS:]]
                    cv = coeficienteVariacao(ultimas)
                    if cv is not None and cv <= params.cvAlvoPercentual:
                        metadata["motivoParada"] = "cvAtingido"
                        parar.set()
                        return

    async def stream(streamId: int, client: httpx.AsyncClient):
        try:
            ultimaMarca = time.monotonic()
            if direcao == "download":
                # localBytes: contador POR-STREAM para calculo de taxa.
                # bytes_total[0] e global (somado por todos os streams) e usado
                # apenas pelo supervisor para detectar fim do payload.
                localBytes = 0
                ultimoLocal = 0
                url = f"{CLOUDFLARE_DOWN}?bytes={bytesPayload}"
                async with client.stream("GET", url, headers=HEADERS_BASE) as resp:
                    _capturarMeta(resp, metadata, time.monotonic() - inicio)
                    async for chunk in resp.aiter_bytes(chunk_size=64 * 1024):
                        if parar.is_set():
                            break
                        localBytes += len(chunk)
                        bytes_total[0] += len(chunk)
                        agora = time.monotonic()
                        intervalo = agora - ultimaMarca
                        if intervalo >= INTERVALO_AMOSTRA_SEGUNDOS:
                            delta = localBytes - ultimoLocal
                            taxaMbps = (delta * 8) / intervalo / 1e6
                            sample = Sample(
                                streamId=streamId,
                                tsRelativoSegundos=agora - inicio,
                                bytesAcumulados=localBytes,
                                taxaIntervaloMbps=taxaMbps,
                            )
                            async with samples_lock:
                                samples.append(sample)
                            ultimoLocal = localBytes
                            ultimaMarca = agora
                # Fallback: se o download completou rapido demais para capturar
                # amostras por intervalo (ex.: <50ms por conexao em GbE+),
                # registra a taxa media total do stream como unico sample.
                async with samples_lock:
                    minhasSamples = [s for s in samples if s.streamId == streamId]
                if not minhasSamples and localBytes > 0:
                    duracaoStream = time.monotonic() - inicio
                    taxaMbps = (localBytes * 8) / max(duracaoStream, 0.001) / 1e6
                    async with samples_lock:
                        samples.append(Sample(
                            streamId=streamId,
                            tsRelativoSegundos=duracaoStream,
                            bytesAcumulados=localBytes,
                            taxaIntervaloMbps=taxaMbps,
                        ))
            else:
                # Upload via POST com async generator.
                # Usamos dict mutavel (_st) para contornar escopo de closure:
                # assignments dentro de gerador() criariam variaveis locais
                # Python (sem nonlocal), corrompendo o rastreamento de bytes.
                _st = {"bytes": 0, "prev": 0, "marca": time.monotonic()}

                async def gerador():
                    enviado = 0
                    bloco = b"\0" * (64 * 1024)
                    while enviado < bytesPayload and not parar.is_set():
                        restante = bytesPayload - enviado
                        chunk = bloco if restante >= len(bloco) else bloco[:restante]
                        yield chunk
                        enviado += len(chunk)
                        _st["bytes"] += len(chunk)
                        bytes_total[0] += len(chunk)
                        agora = time.monotonic()
                        intervalo = agora - _st["marca"]
                        if intervalo >= INTERVALO_AMOSTRA_SEGUNDOS:
                            delta = _st["bytes"] - _st["prev"]
                            taxaMbps = (delta * 8) / intervalo / 1e6
                            sample = Sample(
                                streamId=streamId,
                                tsRelativoSegundos=agora - inicio,
                                bytesAcumulados=_st["bytes"],
                                taxaIntervaloMbps=taxaMbps,
                            )
                            async with samples_lock:
                                samples.append(sample)
                            _st["prev"] = _st["bytes"]
                            _st["marca"] = agora

                headers = {
                    **HEADERS_BASE,
                    "Content-Type": "application/octet-stream",
                    "Content-Length": str(bytesPayload),
                }
                resp = await client.post(
                    CLOUDFLARE_UP, content=gerador(), headers=headers
                )
                _capturarMeta(resp, metadata, time.monotonic() - inicio)
        except (httpx.HTTPError, asyncio.CancelledError) as e:
            registrar(
                {
                    "tipo": "erro",
                    "comboId": comboId,
                    "rodada": rodada,
                    "direcao": direcao,
                    "streamId": streamId,
                    "categoria": _classificarErro(e),
                    "mensagem": str(e),
                    "ts": _agora(),
                }
            )

    timeout = httpx.Timeout(params.timeoutSegundos, connect=10.0)
    limites = httpx.Limits(
        max_connections=params.streamsParalelos * 2,
        max_keepalive_connections=params.streamsParalelos,
    )
    try:
        async with httpx.AsyncClient(
            http2=False,  # http/1.1 e o que o site oficial usa para esses endpoints
            timeout=timeout,
            limits=limites,
            follow_redirects=False,
            verify=True,
        ) as client:
            tasks = [
                asyncio.create_task(stream(i, client))
                for i in range(params.streamsParalelos)
            ]
            tasks.append(asyncio.create_task(supervisor()))
            await asyncio.gather(*tasks, return_exceptions=True)
    except Exception as e:  # noqa: BLE001
        erro = f"{type(e).__name__}: {e}"

    duracao = time.monotonic() - inicio
    motivoParada = metadata.get("motivoParada", "completo")
    if erro:
        motivoParada = "erro"

    return ResultadoDirecao(
        direcao=direcao,
        samples=samples,
        duracaoSegundos=duracao,
        motivoParada=motivoParada,
        bytesTotal=bytes_total[0],
        cfRay=metadata["cfRay"],
        ipServidor=metadata["ipServidor"],
        ttfbMs=metadata["ttfbMs"],
        statusHttp=metadata["statusHttp"],
        erro=erro,
    )


def _capturarMeta(resp: httpx.Response, dest: dict, decorridoSegundos: float) -> None:
    dest["statusHttp"] = resp.status_code
    dest["cfRay"] = resp.headers.get("cf-ray")
    if dest["ttfbMs"] is None:
        dest["ttfbMs"] = round(decorridoSegundos * 1000.0, 2)
    if dest["ipServidor"] is None:
        try:
            host = resp.request.url.host
            dest["ipServidor"] = socket.gethostbyname(host)
        except (socket.gaierror, OSError):
            dest["ipServidor"] = None


def _classificarErro(e: Exception) -> str:
    if isinstance(e, httpx.HTTPStatusError):
        s = e.response.status_code
        if s == 429:
            return "http429"
        if s == 403:
            return "http403"
        if s >= 500:
            return f"http{s}"
    if isinstance(e, httpx.TimeoutException):
        return "timeout"
    if isinstance(e, httpx.ConnectError):
        return "connectError"
    if isinstance(e, httpx.ReadError):
        return "readError"
    return "outro"


def _agora() -> str:
    return time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())


# --------------------------------------------------------------------------- #
# Util                                                                        #
# --------------------------------------------------------------------------- #


def gerarComboId(params: ParamsMedicao) -> str:
    """ID humano-legivel para uma combinacao fisica de parametros (sem incluir
    parametros de pos-processamento como metodoCalculo/warmup/slowStart)."""
    janela = (
        f"T{params.duracaoSegundos}"
        if params.modoJanela == "tempoFixo"
        else f"CV{int(params.cvAlvoPercentual)}"
    )
    return (
        f"DL{params.payloadDownloadMB}"
        f"_UL{params.payloadUploadMB}"
        f"_S{params.streamsParalelos}"
        f"_{janela}"
    )


__all__ = [
    "ParamsMedicao",
    "Sample",
    "ResultadoDirecao",
    "ResultadoFisico",
    "medirComParametros",
    "reagregar",
    "coeficienteVariacao",
    "gerarComboId",
]
