"""coletorReferenciaOficial.py

Replica o algoritmo do SDK oficial `@cloudflare/speedtest` diretamente em Python
usando httpx. O SDK Node falha em ambiente non-browser (sem
PerformanceResourceTiming.transferSize, sem RTCPeerConnection), entao
reimplementamos aqui usando a mesma metodologia documentada publicamente:

- Latencia: 20 amostras de TTFB em requests de 0 bytes → mediana
- Download: tiers crescentes; throughput por request = bytes*8/(duracao-TTFB); P90
- Upload: idem via POST
- Resultado igual ao site, dentro da variância de rede

Pode ser chamado como modulo (funcaoReferencia()) ou diretamente na CLI.
"""
from __future__ import annotations

import asyncio
import json
import socket
import time
from dataclasses import asdict, dataclass
from typing import List, Optional

import httpx

CLOUDFLARE_DOWN = "https://speed.cloudflare.com/__down"
CLOUDFLARE_UP = "https://speed.cloudflare.com/__up"
CLOUDFLARE_TRACE = "https://speed.cloudflare.com/cdn-cgi/trace"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
)
HEADERS = {
    "User-Agent": USER_AGENT,
    "Accept-Language": "pt-BR,pt;q=0.9,en-US;q=0.8",
    "Accept": "*/*",
    "Accept-Encoding": "identity",
}

# Tiers do SDK oficial (número de requests por tier × bytes)
TIERS_DOWNLOAD: List[dict] = [
    {"bytes": 100_000,   "count": 3},
    {"bytes": 1_000_000, "count": 3},
    {"bytes": 10_000_000,"count": 3},
    {"bytes": 25_000_000,"count": 1},
    {"bytes": 100_000_000,"count": 1},
]
TIERS_UPLOAD: List[dict] = [
    {"bytes": 100_000,   "count": 3},
    {"bytes": 1_000_000, "count": 3},
    {"bytes": 10_000_000,"count": 3},
    {"bytes": 25_000_000,"count": 1},
    {"bytes": 50_000_000,"count": 1},
]
NUM_AMOSTRAS_LATENCIA = 20
TIMEOUT_CONEXAO = 10.0
TIMEOUT_REQUISICAO = 120.0


@dataclass
class ResultadoReferencia:
    downloadMbps: float
    uploadMbps: float
    latenciaMs: float
    jitterMs: float
    p90DownloadMbps: float
    p90UploadMbps: float
    iniciadoEm: str
    finalizadoEm: str
    versaoSdk: str
    ipPublicoMaquina: Optional[str]
    edgeIata: Optional[str]
    edgeLoc: Optional[str]
    # amostras brutas para transparencia
    amostrasDownloadMbps: List[float]
    amostrasUploadMbps: List[float]
    amostrasLatenciaMs: List[float]

    def paraDicionario(self) -> dict:
        d = asdict(self)
        # alias para compatibilidade com analisador.py
        d["edgeCloudflare"] = {"iata": d.pop("edgeIata"), "loc": d.pop("edgeLoc")}
        return d


# --------------------------------------------------------------------------- #
# Funcao principal                                                            #
# --------------------------------------------------------------------------- #


async def coletarReferencia(payloadMaxMB: Optional[int] = None) -> ResultadoReferencia:
    """payloadMaxMB: se informado, limita os tiers a esse tamanho (util para smoke/dev)."""
    iniciadoEm = _agora()

    async with httpx.AsyncClient(
        http2=False,
        timeout=httpx.Timeout(TIMEOUT_REQUISICAO, connect=TIMEOUT_CONEXAO),
        headers=HEADERS,
        follow_redirects=False,
        verify=True,
    ) as client:
        contexto = await _obterContexto(client)
        amostrasLat = await _medirLatencia(client)
        amostrasDown = await _medirDownload(client, payloadMaxBytes=payloadMaxMB * 1024 * 1024 if payloadMaxMB else None)
        amostrasUp = await _medirUpload(client, payloadMaxBytes=payloadMaxMB * 1024 * 1024 if payloadMaxMB else None)

    latenciaMs = _mediana(amostrasLat) if amostrasLat else 0.0
    jitterMs = _jitter(amostrasLat) if amostrasLat else 0.0
    p90Down = _p90(amostrasDown) if amostrasDown else 0.0
    p90Up = _p90(amostrasUp) if amostrasUp else 0.0

    return ResultadoReferencia(
        downloadMbps=round(p90Down, 3),
        uploadMbps=round(p90Up, 3),
        latenciaMs=round(latenciaMs, 2),
        jitterMs=round(jitterMs, 2),
        p90DownloadMbps=round(p90Down, 3),
        p90UploadMbps=round(p90Up, 3),
        iniciadoEm=iniciadoEm,
        finalizadoEm=_agora(),
        versaoSdk="replica-python-v1",
        ipPublicoMaquina=contexto.get("ip"),
        edgeIata=contexto.get("colo"),
        edgeLoc=contexto.get("loc"),
        amostrasDownloadMbps=[round(v, 3) for v in amostrasDown],
        amostrasUploadMbps=[round(v, 3) for v in amostrasUp],
        amostrasLatenciaMs=[round(v, 2) for v in amostrasLat],
    )


# --------------------------------------------------------------------------- #
# Latencia                                                                    #
# --------------------------------------------------------------------------- #


async def _medirLatencia(client: httpx.AsyncClient) -> List[float]:
    """20 TTFBs de requests de 0 bytes = latencia sem transferencia."""
    amostras: List[float] = []
    for _ in range(NUM_AMOSTRAS_LATENCIA):
        try:
            t0 = time.monotonic()
            r = await client.get(f"{CLOUDFLARE_DOWN}?bytes=0", timeout=5.0)
            ttfb = (time.monotonic() - t0) * 1000.0
            await r.aread()
            amostras.append(ttfb)
        except httpx.HTTPError:
            pass
        await asyncio.sleep(0.05)
    return amostras


# --------------------------------------------------------------------------- #
# Download                                                                    #
# --------------------------------------------------------------------------- #


async def _medirDownload(client: httpx.AsyncClient, payloadMaxBytes: Optional[int] = None) -> List[float]:
    amostras: List[float] = []
    tierAnteriorBytes = 0
    for tier in TIERS_DOWNLOAD:
        tamanho = tier["bytes"]
        if payloadMaxBytes and tamanho > payloadMaxBytes:
            tamanho = payloadMaxBytes
        # Pausa maior ao mudar de tier (reduz densidade de requisicoes grandes)
        if tamanho > tierAnteriorBytes and tierAnteriorBytes > 0:
            await asyncio.sleep(3.0)
        tierAnteriorBytes = tamanho
        for _ in range(tier["count"]):
            mbps = await _umaRequisicaoDownload(client, tamanho)
            if mbps is not None:
                amostras.append(mbps)
            await asyncio.sleep(0.5)
    return amostras


async def _umaRequisicaoDownload(client: httpx.AsyncClient, bytesPayload: int) -> Optional[float]:
    url = f"{CLOUDFLARE_DOWN}?bytes={bytesPayload}"
    try:
        t0 = time.monotonic()
        async with client.stream("GET", url) as resp:
            ttfb = time.monotonic() - t0
            bytesRecebidos = 0
            async for chunk in resp.aiter_bytes(65536):
                bytesRecebidos += len(chunk)
        duracao = time.monotonic() - t0
        # Desconta TTFB: mede apenas a fase de transferencia de dados
        tempTransferencia = max(duracao - ttfb, 0.001)
        return (bytesRecebidos * 8) / tempTransferencia / 1e6
    except httpx.HTTPError:
        return None


# --------------------------------------------------------------------------- #
# Upload                                                                      #
# --------------------------------------------------------------------------- #


async def _medirUpload(client: httpx.AsyncClient, payloadMaxBytes: Optional[int] = None) -> List[float]:
    amostras: List[float] = []
    tierAnteriorBytes = 0
    for tier in TIERS_UPLOAD:
        tamanho = tier["bytes"]
        if payloadMaxBytes and tamanho > payloadMaxBytes:
            tamanho = payloadMaxBytes
        if tamanho > tierAnteriorBytes and tierAnteriorBytes > 0:
            await asyncio.sleep(3.0)
        tierAnteriorBytes = tamanho
        for _ in range(tier["count"]):
            mbps = await _umaRequisicaoUpload(client, tamanho)
            if mbps is not None:
                amostras.append(mbps)
            await asyncio.sleep(0.5)
    return amostras


async def _umaRequisicaoUpload(client: httpx.AsyncClient, bytesPayload: int) -> Optional[float]:
    payload = b"\0" * bytesPayload
    try:
        t0 = time.monotonic()
        r = await client.post(
            CLOUDFLARE_UP,
            content=payload,
            headers={
                **HEADERS,
                "Content-Type": "application/octet-stream",
                "Content-Length": str(bytesPayload),
            },
        )
        duracao = time.monotonic() - t0
        await r.aread()
        return (bytesPayload * 8) / max(duracao, 0.001) / 1e6
    except httpx.HTTPError:
        return None


# --------------------------------------------------------------------------- #
# Contexto Cloudflare                                                         #
# --------------------------------------------------------------------------- #


async def _obterContexto(client: httpx.AsyncClient) -> dict:
    try:
        resp = await client.get(CLOUDFLARE_TRACE, timeout=5.0)
        return dict(
            l.split("=", 1)
            for l in resp.text.splitlines()
            if "=" in l
        )
    except httpx.HTTPError:
        return {}


# --------------------------------------------------------------------------- #
# Estatistica                                                                 #
# --------------------------------------------------------------------------- #


def _mediana(valores: List[float]) -> float:
    if not valores:
        return 0.0
    s = sorted(valores)
    n = len(s)
    if n % 2 == 0:
        return (s[n // 2 - 1] + s[n // 2]) / 2.0
    return s[n // 2]


def _jitter(valores: List[float]) -> float:
    if len(valores) < 2:
        return 0.0
    diffs = [abs(valores[i + 1] - valores[i]) for i in range(len(valores) - 1)]
    return sum(diffs) / len(diffs)


def _p90(valores: List[float]) -> float:
    if not valores:
        return 0.0
    s = sorted(valores)
    k = (len(s) - 1) * 0.9
    f = int(k)
    c = min(f + 1, len(s) - 1)
    if f == c:
        return s[f]
    return s[f] + (s[c] - s[f]) * (k - f)


def _agora() -> str:
    return time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())


# --------------------------------------------------------------------------- #
# CLI                                                                         #
# --------------------------------------------------------------------------- #


if __name__ == "__main__":
    resultado = asyncio.run(coletarReferencia())
    import sys
    sys.stdout.write(json.dumps(resultado.paraDicionario()) + "\n")
