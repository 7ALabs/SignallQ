// medicaoDireta.js — mede download igual ao App Flutter (streaming, 300ms ticks)
// HTTP/1.1 via Node.js https nativo — equivalente ao dart:io HttpClient.

import https from 'https';
import { performance } from 'perf_hooks';

const BASE = 'https://speed.cloudflare.com';
const DL_URL = `${BASE}/__down`;
const TRACE_URL = `${BASE}/cdn-cgi/trace`;
const TIMEOUT_MS = 40_000;

function cb() {
  return `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

async function getTrace() {
  try {
    const res = await fetch(TRACE_URL);
    const txt = await res.text();
    const obj = {};
    for (const line of txt.trim().split('\n')) {
      const [k, v] = line.split('=');
      if (k && v) obj[k] = v;
    }
    return obj;
  } catch { return {}; }
}

const UA = 'Mozilla/5.0 (compatible; linkaAPP-speedtest-reference)';

// Ping simples: GET __down?bytes=0
function ping() {
  return new Promise((resolve, reject) => {
    const t0 = performance.now();
    const req = https.get(`${DL_URL}?bytes=0&_cb=${cb()}`, {
      headers: { 'Cache-Control': 'no-cache, no-store', 'User-Agent': UA },
      timeout: 4000,
    }, (res) => {
      res.resume();
      res.on('end', () => resolve({ ms: +(performance.now() - t0).toFixed(1), httpVersion: res.httpVersion }));
      res.on('error', reject);
    });
    req.on('error', reject);
    req.on('timeout', () => { req.destroy(); reject(new Error('ping timeout')); });
  });
}

// Stream contínuo: conta bytes via evento 'data' enquanto durar
function abrirStream(bytes, aoReceberBytes, httpVersionCb, cancelado) {
  return new Promise((resolve) => {
    if (cancelado()) { resolve(); return; }
    const url = `${DL_URL}?bytes=${bytes}&_cb=${cb()}`;
    const req = https.get(url, {
      headers: { 'Cache-Control': 'no-cache, no-store', 'User-Agent': UA },
      timeout: TIMEOUT_MS,
    }, (res) => {
      if (httpVersionCb) { httpVersionCb(res.httpVersion); }
      res.on('data', (chunk) => {
        if (!cancelado()) aoReceberBytes(chunk.length);
      });
      res.on('end', resolve);
      res.on('error', () => resolve());
    });
    req.on('error', () => resolve());
    req.on('timeout', () => { req.destroy(); resolve(); });
  });
}

// Loop de stream: ao terminar um request, abre outro imediatamente (igual ao App)
async function loopStream(bytes, aoReceberBytes, cancelado, httpVersionCb) {
  let primeiro = true;
  while (!cancelado()) {
    await abrirStream(bytes, aoReceberBytes, primeiro ? httpVersionCb : null, cancelado);
    primeiro = false;
  }
}

async function medirLatencia(n = 8) {
  const amostras = [];
  for (let i = 0; i < n; i++) {
    try {
      const { ms } = await ping();
      amostras.push(ms);
    } catch { /* ignora */ }
  }
  if (amostras.length < 2) return { mediana: 0, media: 0 };
  amostras.shift(); // descarta 1º (warm-up)
  amostras.sort((a, b) => a - b);
  const mediana = amostras[Math.floor(amostras.length / 2)];
  const media = amostras.reduce((s, v) => s + v, 0) / amostras.length;
  return { mediana: +mediana.toFixed(1), media: +media.toFixed(1) };
}

// Download idêntico ao App: tick a cada 300ms, janela 35%/65%
async function medirDownload({ bytes = 25_000_000, duracaoMs = 7000, paralelo = 4, warmupMs = 1000 } = {}) {
  const amostras = [];
  const inicio = performance.now();
  let cancelado = false;
  let tickBytes = 0;
  let lastTickMs = 0;
  let httpVersionObtido = '?';

  const timerDuracao = setTimeout(() => { cancelado = true; }, duracaoMs);

  const timerAmostragem = setInterval(() => {
    const nowMs = performance.now() - inicio;
    const elapsedTickSec = (nowMs - lastTickMs) / 1000;
    lastTickMs = nowMs;
    if (elapsedTickSec > 0 && tickBytes > 0) {
      const instant = (tickBytes * 8) / elapsedTickSec / 1e6;
      amostras.push({ tMs: +nowMs.toFixed(0), mbps: +instant.toFixed(3) });
    }
    tickBytes = 0;
  }, 300);

  function aoReceberBytes(n) { tickBytes += n; }
  function httpVersionCb(v) { if (httpVersionObtido === '?') httpVersionObtido = v; }

  // Todos os streams fazem loop idêntico (sem distinção firstReq)
  const streams = [];
  for (let i = 0; i < paralelo; i++) {
    streams.push(loopStream(bytes, aoReceberBytes, () => cancelado, i === 0 ? httpVersionCb : null));
  }

  await Promise.all(streams);
  clearTimeout(timerDuracao);
  clearInterval(timerAmostragem);

  // Janela estável: pós-warmup, descarta primeiros 35%, MEAN dos 65%
  const valid = amostras.filter(s => s.tMs >= warmupMs && s.mbps > 0);
  const stableStart = Math.ceil(valid.length * 0.35);
  const stable = valid.slice(stableStart);
  const throughput = stable.length
    ? stable.reduce((s, v) => s + v.mbps, 0) / stable.length
    : 0;
  const peak = valid.length ? Math.max(...valid.map(s => s.mbps)) : 0;

  return {
    throughputMbps: +throughput.toFixed(2),
    peakMbps: +peak.toFixed(2),
    totalAmostras: amostras.length,
    amostrasValidas: valid.length,
    amostrasEstaveis: stable.length,
    httpVersion: httpVersionObtido,
    amostrasDetalhadas: amostras,
  };
}

async function main() {
  process.stderr.write('Obtendo contexto Cloudflare...\n');
  const trace = await getTrace();
  process.stderr.write(`Edge: ${trace.colo ?? '?'} | IP público: ${trace.ip ?? '?'} | HTTP (fetch): ${trace.http ?? '?'}\n`);

  process.stderr.write('\nMedindo latência (8 pings)...\n');
  const lat = await medirLatencia(8);
  process.stderr.write(`Latência: ${lat.mediana} ms (mediana)  ${lat.media} ms (média)\n`);

  const resultados = [];
  for (let r = 1; r <= 3; r++) {
    process.stderr.write(`\n=== RODADA ${r}/3 ===\n`);
    process.stderr.write('Download 25 MB × 4 streams × 7s...\n');
    const dl = await medirDownload({ bytes: 25_000_000, duracaoMs: 7000, paralelo: 4, warmupMs: 1000 });
    process.stderr.write(`  DL: ${dl.throughputMbps} Mbps  peak: ${dl.peakMbps}  HTTP/${dl.httpVersion}  amostras: ${dl.totalAmostras} válidas: ${dl.amostrasValidas} estáveis: ${dl.amostrasEstaveis}\n`);
    resultados.push(dl);
  }

  const throughputs = resultados.map(r => r.throughputMbps).filter(v => v > 0);
  const media = throughputs.length ? +(throughputs.reduce((a, b) => a + b, 0) / throughputs.length).toFixed(2) : 0;

  const saida = {
    plataforma: 'nodejs',
    edgeCloudflare: trace.colo ?? null,
    ipPublico: trace.ip ?? null,
    httpVersionConexao: resultados[0]?.httpVersion ?? '?',
    httpVersionFetch: trace.http ?? '?',
    latenciaMedianaMs: lat.mediana,
    latenciaMediaMs: lat.media,
    downloadMbps: {
      r1: resultados[0]?.throughputMbps ?? null,
      r2: resultados[1]?.throughputMbps ?? null,
      r3: resultados[2]?.throughputMbps ?? null,
      media,
    },
  };

  process.stdout.write(JSON.stringify(saida, null, 2) + '\n');
}

main().catch(e => { process.stderr.write(String(e) + '\n'); process.exit(1); });
