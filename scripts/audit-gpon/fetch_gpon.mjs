// =============================================================================
// fetch_gpon.mjs — Auditoria GPON do app LINKA contra o modem real
// -----------------------------------------------------------------------------
// Faz login no painel web do Nokia G-1425G-B e extrai os dados GPON, WAN,
// PPP e device info usando o mapeamento documentado em
// `source/devices/compativeis/Nokia G-1425-B/integration-manual-v2.md`.
//
// Saída: JSON único em stdout, com seções para cada endpoint. Use para
// comparar campo a campo com o que o app exibe.
//
// IMPORTANTE — credenciais:
// - Pegue de variáveis de ambiente. NÃO coloque em arquivo. NÃO faça commit.
//   Exemplo (PowerShell):
//       $env:GPON_HOST="192.168.1.254"
//       $env:GPON_USER="userAdmin"
//       $env:GPON_PASS="<sua-senha>"
//       node fetch_gpon.mjs
//
// Pré-requisitos:
//   - Node 18+ (testado em v24)
//   - Playwright 1.40+ com Chromium instalado (já presente nesta máquina)
//   - Estar conectado ao Wi-Fi do modem (192.168.1.x)
//
// Flags opcionais:
//   --headed              Mostra o browser (debug visual)
//   --keep-open           Não fecha o browser ao final (debug)
//   --pretty              Saída JSON identada
//   --out=path/file.json  Grava o JSON em arquivo (não loga em stdout)
// =============================================================================

import { chromium } from 'playwright';
import { writeFileSync } from 'node:fs';

const HOST = process.env.GPON_HOST || '192.168.1.254';
const USER = process.env.GPON_USER;
const PASS = process.env.GPON_PASS;

// Logs de progresso vão para stderr — não poluem o JSON final em stdout.
function log(msg) {
  process.stderr.write(`[${new Date().toISOString()}] ${msg}\n`);
}

if (!USER || !PASS) {
  console.error('ERRO: defina GPON_USER e GPON_PASS no ambiente antes de rodar.');
  console.error('Exemplo (PowerShell):');
  console.error('  $env:GPON_USER="userAdmin"; $env:GPON_PASS="<senha>"');
  process.exit(2);
}

const args = process.argv.slice(2);
const argsSet = new Set(args);
const headed = argsSet.has('--headed');
const keepOpen = argsSet.has('--keep-open');
const pretty = argsSet.has('--pretty');
const outArg = args.find(a => a.startsWith('--out='));
const outFile = outArg ? outArg.slice('--out='.length) : null;

const baseUrl = `http://${HOST}`;
log(`Alvo: ${baseUrl} (user: ${USER})`);

async function login(page) {
  log('Abrindo página de login…');
  await page.goto(baseUrl + '/', { waitUntil: 'networkidle' });
  log('Página carregou. Preenchendo credenciais…');
  // O painel só autentica via JS (RSA + AES). Submetemos via formulário e
  // deixamos o JS embutido fazer a criptografia.
  await page.fill('#username', USER);
  await page.fill('#password', PASS);
  log('Submetendo login…');
  // O botão dispara o JS de submit via onclick — em alguns firmwares é
  // necessário apertar enter dentro do campo ou clicar diretamente.
  await Promise.all([
    page.waitForURL(/overview\.cgi/, { timeout: 15000 }).catch(() => null),
    page.click('#loginBT'),
  ]);
  // Se o redirect não veio, a sessão ainda pode ter sido criada — verifique
  // pelos cookies sid/lsid. Caso contrário, dispara erro.
  const cookies = await page.context().cookies(baseUrl);
  const hasSid = cookies.some(c => c.name === 'sid' && c.value);
  if (!hasSid) {
    log('Cookies recebidos: ' + cookies.map(c => c.name).join(', '));
    throw new Error('Login falhou: cookie sid não foi criado.');
  }
  log(`Login OK. Cookies de sessão: ${cookies.map(c => c.name).join(', ')}`);
}

async function fetchWanIpv4(page) {
  await page.goto(baseUrl + '/show_wan_status.cgi?ipv4', { waitUntil: 'networkidle' });
  return page.evaluate(() => {
    // wan_conns é um array; achamos a conexão Connected ativa.
    const conns = (typeof window.wan_conns !== 'undefined' ? window.wan_conns : []) || [];
    if (conns.length === 0) return null;
    const all = conns.map(c => {
      const obj = (c.ipConns && c.ipConns[0]) || (c.pppConns && c.pppConns[0]) || null;
      if (!obj) return null;
      return {
        name: obj.Name,
        connection_status: obj.ConnectionStatus,
        connection_type: obj.ConnectionType,
        external_ip: obj.ExternalIPAddress,
        gateway: obj.RemoteIPAddress || obj.DefaultGateway,
        subnet_mask: obj.SubnetMask,
        dns_servers_raw: obj.DNSServers,
        dns_primary: obj.DNSServers ? obj.DNSServers.split(',')[0] : null,
        dns_secondary: obj.DNSServers ? obj.DNSServers.split(',')[1] || null : null,
        pppoe_concentrator: obj.PPPoEACName,
        vlan_id: c.xponLinkCfg && c.xponLinkCfg.VLANIDMark,
        interface: obj.X_ASB_COM_IfName,
        nat_enabled: obj.NATEnabled,
        uptime_seconds: obj.Uptime,
        bytes_sent: obj.EthernetBytesSent,
        bytes_received: obj.EthernetBytesReceived,
        packets_sent: obj.EthernetPacketsSent,
        packets_received: obj.EthernetPacketsReceived,
      };
    }).filter(Boolean);
    return {
      count: all.length,
      active: all.find(c => c.connection_status === 'Connected') || null,
      all,
    };
  });
}

async function fetchDeviceInfo(page) {
  await page.goto(baseUrl + '/device_status.cgi', { waitUntil: 'networkidle' });
  return page.evaluate(() => {
    const di = window.dev_info || {};
    const mi = window.mem_info || {};
    const ti = window.cpu_temperatureinfo || {};
    return {
      model: di.ModelName || null,
      manufacturer: di.Manufacturer || null,
      serial: di.SerialNumber || null,
      firmware: di.SoftwareVersion || null,
      hardware: di.HardwareVersion || null,
      uptime_seconds: di.UpTime != null ? Number(di.UpTime) : null,
      ram_total_kb: mi.Total != null ? Number(mi.Total) : null,
      ram_free_kb: mi.Free != null ? Number(mi.Free) : null,
      cpu_temp_c: ti.CPUTemp != null ? String(ti.CPUTemp) : null,
      chipset: di.X_ASB_COM_Chipset || null,
      oui: di.ManufacturerOUI || null,
    };
  });
}

async function fetchGpon(page) {
  await page.goto(baseUrl + '/wan_status.cgi?gpon', { waitUntil: 'networkidle' });
  // Captura também o texto renderizado da página — assim conseguimos ver
  // o que o painel web está exibindo de fato ao usuário, depois de o JS do
  // modem aplicar suas próprias conversões (algumas vezes diferentes do
  // raw e da conversão "documentada" no manual).
  const renderedText = await page.evaluate(() => {
    const main = document.querySelector('#main') || document.body;
    return main.innerText || '';
  });
  // HTML cru — usado para alimentar testes Dart do parser sem precisar do
  // modem ligado. Salvo apenas se GPON_HTML_DIR estiver definido.
  const htmlPath = process.env.GPON_HTML_DIR
    ? `${process.env.GPON_HTML_DIR}/gpon-page.html`
    : null;
  if (htmlPath) {
    const html = await page.content();
    writeFileSync(htmlPath, html);
    log(`HTML GPON: ${htmlPath}`);
  }
  const screenshotPath = process.env.GPON_SCREENSHOT_DIR
    ? `${process.env.GPON_SCREENSHOT_DIR}/gpon-page.png`
    : null;
  if (screenshotPath) {
    await page.screenshot({ path: screenshotPath, fullPage: true });
    log(`Screenshot GPON: ${screenshotPath}`);
  }
  const data = await page.evaluate(() => {
    const g = window.gpon_status || {};
    // Os valores raw vêm em micro-unidades. Aplicamos as conversões
    // documentadas no manual v2 §5.4 para entregar valores em SI.
    const rawTempUk = g.TransceiverTemperature; // µK
    const rawVoltUv = g.SupplyVoltage; // µV
    const rawCurrUa = g.BiasCurrent; // µA
    return {
      raw: g, // payload completo, para auditoria
      status: g.Status,
      connection_mode: g.ConnectionMode,
      // RXPower/TXPower são raw — manual v2 §14 sugere "/1000 se >60".
      // Entregamos os 3 valores: raw, /1000, /100, e o usuário decide o
      // que bate com o app.
      rx_power_raw: g.RXPower,
      tx_power_raw: g.TXPower,
      rx_power_div_1000: g.RXPower != null ? g.RXPower / 1000 : null,
      tx_power_div_1000: g.TXPower != null ? g.TXPower / 1000 : null,
      // Conversões "corretas" segundo §5.4:
      transceiver_temp_c: rawTempUk != null ? (rawTempUk / 1_000_000) - 273.15 : null,
      transceiver_temp_uk_div_1000_kelvin: rawTempUk != null ? rawTempUk / 1000 : null,
      supply_voltage_v: rawVoltUv != null ? rawVoltUv / 1_000_000 : null,
      supply_voltage_uv_div_1000_mv: rawVoltUv != null ? rawVoltUv / 1000 : null,
      bias_current_ma: rawCurrUa != null ? rawCurrUa / 1000 : null,
      // Detecta typo conhecido `SupplyVottage` deste firmware específico.
      supply_vottage_typo_raw: g.SupplyVottage ?? null,
      supply_vottage_div_10000_v: g.SupplyVottage != null ? g.SupplyVottage / 10000 : null,
    };
  });
  data.rendered_text = renderedText;
  return data;
}

async function fetchPpp(page) {
  // Endpoint nativo JSON — não precisa parse via window.*
  const resp = await page.request.get(baseUrl + '/index.cgi?getppp');
  const status = resp.status();
  if (status !== 200) {
    return { http_status: status, body: null };
  }
  let json = null;
  try { json = await resp.json(); } catch {}
  return { http_status: status, body: json };
}

async function main() {
  log(`Lançando Chromium (${headed ? 'headed' : 'headless'})…`);
  const browser = await chromium.launch({ headless: !headed });
  const ctx = await browser.newContext({ ignoreHTTPSErrors: true });
  const page = await ctx.newPage();

  const result = { host: HOST, generated_at: new Date().toISOString() };
  try {
    await login(page);
    result.login = 'ok';
    // Capturas em série para minimizar concorrência no thttpd do modem.
    log('Coletando /show_wan_status.cgi?ipv4 (wan_conns)…');
    result.wan_ipv4 = await fetchWanIpv4(page).catch(e => ({ error: String(e) }));
    log('Coletando /device_status.cgi (dev_info / mem_info / cpu_temperatureinfo)…');
    result.device_info = await fetchDeviceInfo(page).catch(e => ({ error: String(e) }));
    log('Coletando /wan_status.cgi?gpon (gpon_status)…');
    result.gpon = await fetchGpon(page).catch(e => ({ error: String(e) }));
    log('Coletando /index.cgi?getppp (JSON nativo)…');
    result.ppp = await fetchPpp(page).catch(e => ({ error: String(e) }));
    log('Coleta concluída.');
  } catch (e) {
    result.error = String(e);
    log(`ERRO: ${result.error}`);
  } finally {
    if (!keepOpen) {
      log('Fechando browser…');
      await browser.close();
    }
  }

  const json = pretty ? JSON.stringify(result, null, 2) : JSON.stringify(result);
  if (outFile) {
    writeFileSync(outFile, json);
    log(`JSON salvo em: ${outFile}`);
  } else {
    console.log(json);
  }
  process.exit(result.error ? 1 : 0);
}

main();
