// coletorReferenciaOficial.js
//
// Wrapper Node.js do SDK oficial `@cloudflare/speedtest`. É o MESMO motor que
// roda em https://speed.cloudflare.com — chamamos exatamente como o site faz
// e devolvemos um JSON único em stdout para o orquestrador Python consumir.
//
// Saída em stdout (única linha JSON):
//   {
//     "downloadMbps": number,
//     "uploadMbps": number,
//     "latenciaMs": number,
//     "jitterMs": number,
//     "packetLoss": number | null,
//     "p90DownloadMbps": number | null,
//     "p90UploadMbps": number | null,
//     "iniciadoEm": iso8601,
//     "finalizadoEm": iso8601,
//     "versaoSdk": string,
//     "ipPublicoMaquina": string | null,
//     "edgeCloudflare": { "iata": string, "cidade": string, "ip": string } | null,
//     "amostrasBrutas": object   // resultado.results (raw)
//   }
//
// Códigos de saída: 0 sucesso · 1 falha (mensagem em stderr).

import SpeedTest from '@cloudflare/speedtest';
import { WebSocket } from 'ws';

// SDK foi escrito para browser. Em Node precisamos garantir os globais que ele
// espera (fetch é nativo no Node 18+; WebSocket vem do `ws`).
if (typeof globalThis.WebSocket === 'undefined') {
  globalThis.WebSocket = WebSocket;
}

function logErro(msg, erro) {
  const detalhe = erro?.stack || erro?.message || String(erro || '');
  process.stderr.write(`[coletorReferenciaOficial] ${msg}: ${detalhe}\n`);
}

async function obterContextoCloudflare() {
  // Endpoint público de trace — devolve IP do cliente, IATA do edge e colo.
  try {
    const resp = await fetch('https://speed.cloudflare.com/cdn-cgi/trace', {
      headers: {
        'User-Agent':
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
      },
    });
    if (!resp.ok) return null;
    const texto = await resp.text();
    const linhas = Object.fromEntries(
      texto
        .split('\n')
        .filter((l) => l.includes('='))
        .map((l) => l.split('=', 2))
    );
    return {
      ip: linhas.ip || null,
      iata: linhas.colo || null,
      uag: linhas.uag || null,
      loc: linhas.loc || null,
    };
  } catch (e) {
    logErro('falha em /cdn-cgi/trace', e);
    return null;
  }
}

function lerVersaoSdk() {
  try {
    // Lê a versão real instalada para registrar no JSON de saída.
    return import('@cloudflare/speedtest/package.json', { with: { type: 'json' } })
      .then((m) => m.default?.version || 'desconhecida')
      .catch(() => 'desconhecida');
  } catch {
    return Promise.resolve('desconhecida');
  }
}

async function rodar() {
  const iniciadoEm = new Date().toISOString();
  const contextoPromise = obterContextoCloudflare();
  const versaoPromise = lerVersaoSdk();

  // Configuração padrão do site (medições básicas: latência + DL + UL + bufferbloat opcional).
  // Não habilitamos packetLoss porque exige WebRTC, indisponível em Node.
  const engine = new SpeedTest({
    autoStart: false,
    measurements: [
      { type: 'latency', numPackets: 20 },
      { type: 'download', bytes: 1e5, count: 1, bypassMinDuration: true },
      { type: 'latency', numPackets: 20 },
      { type: 'download', bytes: 1e5, count: 9 },
      { type: 'download', bytes: 1e6, count: 8 },
      { type: 'upload', bytes: 1e5, count: 8 },
      { type: 'packetLoss', numPackets: 1e3, batchSize: 10, batchWaitTime: 10 },
      { type: 'upload', bytes: 1e6, count: 6 },
      { type: 'download', bytes: 1e7, count: 6 },
      { type: 'upload', bytes: 1e7, count: 4 },
      { type: 'download', bytes: 2.5e7, count: 4 },
      { type: 'upload', bytes: 2.5e7, count: 4 },
      { type: 'download', bytes: 1e8, count: 3 },
      { type: 'upload', bytes: 5e7, count: 3 },
      { type: 'download', bytes: 2.5e8, count: 2 },
    ],
  });

  const finalizado = new Promise((resolve, reject) => {
    engine.onFinish = (results) => resolve(results);
    engine.onError = (err) => reject(err);
  });

  engine.play();

  let resultado;
  try {
    resultado = await finalizado;
  } catch (e) {
    logErro('SDK reportou erro', e);
    process.exit(1);
  }

  const finalizadoEm = new Date().toISOString();

  // Extração — métricas principais expostas pelo SDK.
  const downloadBps = resultado.getDownloadBandwidth?.() ?? null;
  const uploadBps = resultado.getUploadBandwidth?.() ?? null;
  const latenciaMs = resultado.getUnloadedLatency?.() ?? null;
  const jitterMs = resultado.getUnloadedJitter?.() ?? null;
  const packetLoss = resultado.getPacketLoss?.() ?? null;

  // Cloudflare reporta P90 internamente — tentamos extrair se disponível, senão null.
  const p90DownloadBps = resultado.getDownloadBandwidthPoints?.()?.p90 ?? null;
  const p90UploadBps = resultado.getUploadBandwidthPoints?.()?.p90 ?? null;

  const contexto = await contextoPromise;
  const versaoSdk = await versaoPromise;

  const saida = {
    downloadMbps: downloadBps != null ? +(downloadBps / 1e6).toFixed(3) : null,
    uploadMbps: uploadBps != null ? +(uploadBps / 1e6).toFixed(3) : null,
    latenciaMs: latenciaMs != null ? +latenciaMs.toFixed(2) : null,
    jitterMs: jitterMs != null ? +jitterMs.toFixed(2) : null,
    packetLoss: packetLoss,
    p90DownloadMbps: p90DownloadBps != null ? +(p90DownloadBps / 1e6).toFixed(3) : null,
    p90UploadMbps: p90UploadBps != null ? +(p90UploadBps / 1e6).toFixed(3) : null,
    iniciadoEm,
    finalizadoEm,
    versaoSdk,
    ipPublicoMaquina: contexto?.ip ?? null,
    edgeCloudflare: contexto
      ? { iata: contexto.iata, ip: contexto.ip, loc: contexto.loc }
      : null,
    amostrasBrutas: resultado.results ?? null,
  };

  process.stdout.write(JSON.stringify(saida) + '\n');
  process.exit(0);
}

rodar().catch((e) => {
  logErro('falha não tratada', e);
  process.exit(1);
});
