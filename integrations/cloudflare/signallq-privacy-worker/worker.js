const HTML = `<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Política de Privacidade — SignallQ</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 800px; margin: 0 auto; padding: 24px 16px; color: #1a1a1a; line-height: 1.6; }
    h1 { font-size: 1.75rem; margin-bottom: 0.25rem; }
    h2 { font-size: 1.15rem; margin-top: 2rem; }
    p, li { font-size: 1rem; }
    a { color: #6C2BFF; }
    .meta { color: #666; font-size: 0.9rem; margin-bottom: 2rem; }
    footer { margin-top: 3rem; padding-top: 1rem; border-top: 1px solid #eee; font-size: 0.85rem; color: #666; }
  </style>
</head>
<body>
  <h1>Política de Privacidade</h1>
  <p class="meta">SignallQ &mdash; Última atualização: junho de 2026</p>

  <p>O SignallQ é um aplicativo de diagnóstico de conexão à internet para Android. Esta política descreve quais dados são coletados, como são usados e com quem são compartilhados.</p>

  <h2>1. Dados coletados</h2>
  <p>O app coleta, exclusivamente para fins de diagnóstico de conectividade:</p>
  <ul>
    <li>Métricas de rede: velocidade de download e upload, latência, jitter, perda de pacotes e bufferbloat</li>
    <li>Informações de sinal Wi-Fi: SSID, intensidade de sinal (RSSI), frequência de banda e canal</li>
    <li>Informações de rede móvel: tecnologia (4G/5G), intensidade de sinal (RSRP/RSRQ/SINR) e operadora</li>
    <li>Dispositivos na rede local identificados via UPnP (somente nome e endereço MAC, nunca conteúdo)</li>
    <li>Histórico local de medições, armazenado apenas no dispositivo do usuário</li>
  </ul>
  <p>O app <strong>não</strong> coleta: nome, e-mail, localização GPS, contatos, fotos, arquivos nem qualquer dado de identificação pessoal.</p>

  <h2>2. Como os dados são usados</h2>
  <ul>
    <li>Exibição de diagnóstico local no próprio dispositivo</li>
    <li>Envio ao motor de inteligência artificial para geração de laudo (veja item 3)</li>
    <li>Monitoramento periódico em segundo plano para alertas de queda de qualidade</li>
  </ul>

  <h2>3. Compartilhamento com terceiros</h2>
  <p>Os dados de diagnóstico (métricas de rede anonimizadas, sem identificação pessoal) são enviados a um servidor de processamento hospedado na <strong>Cloudflare</strong> para análise por inteligência artificial. Nenhum dado é compartilhado com outros terceiros, vendido ou utilizado para fins publicitários.</p>
  <p>O servidor de IA é operado pelo próprio desenvolvedor do SignallQ na infraestrutura da Cloudflare, localizada nos EUA, sujeita à política de privacidade da Cloudflare disponível em <a href="https://www.cloudflare.com/privacypolicy/" target="_blank" rel="noopener">cloudflare.com/privacypolicy</a>.</p>

  <h2>4. Armazenamento e retenção</h2>
  <p>O histórico de medições é armazenado localmente no dispositivo do usuário e pode ser apagado a qualquer momento pelo próprio app ou pela desinstalação do aplicativo. Os dados enviados ao servidor de IA não são armazenados de forma persistente — são processados em tempo real e descartados.</p>

  <h2>5. Permissões solicitadas</h2>
  <ul>
    <li><strong>ACCESS_FINE_LOCATION</strong>: necessária pelo sistema Android para leitura do SSID e canal Wi-Fi. O app não rastreia localização GPS.</li>
    <li><strong>READ_PHONE_STATE</strong>: usada para leitura de métricas de sinal celular (RSRP/RSRQ/SINR) em redes 4G/5G.</li>
    <li><strong>FOREGROUND_SERVICE</strong>: usada para manter o monitoramento ativo em segundo plano com notificação visível.</li>
    <li><strong>ACCESS_NETWORK_STATE / ACCESS_WIFI_STATE / CHANGE_NETWORK_STATE</strong>: usadas para leitura de estado da conexão e configuração de rede.</li>
  </ul>

  <h2>6. Direitos do usuário</h2>
  <p>Você pode a qualquer momento:</p>
  <ul>
    <li>Apagar o histórico de medições dentro do próprio app (Ajustes)</li>
    <li>Revogar permissões do app nas configurações do Android</li>
    <li>Desinstalar o app, o que remove todos os dados armazenados localmente</li>
    <li>Solicitar informações adicionais pelo e-mail abaixo</li>
  </ul>

  <h2>7. Menores de idade</h2>
  <p>O SignallQ não é direcionado a menores de 13 anos e não coleta conscientemente dados de crianças.</p>

  <h2>8. Contato</h2>
  <p>Dúvidas, solicitações de exclusão de dados ou outros assuntos relacionados à privacidade:<br>
  <a href="mailto:giammattey.luiz@gmail.com">giammattey.luiz@gmail.com</a></p>

  <h2>9. Alterações nesta política</h2>
  <p>Esta política pode ser atualizada periodicamente. A data de última atualização está indicada no topo do documento. O uso continuado do app após uma alteração implica aceitação da nova versão.</p>

  <footer>SignallQ &mdash; Desenvolvido por Luiz Giammattey &mdash; <a href="mailto:giammattey.luiz@gmail.com">giammattey.luiz@gmail.com</a></footer>
</body>
</html>`;

export default {
  async fetch(req) {
    const url = new URL(req.url);
    if (url.pathname === '/health') {
      return new Response('ok', { status: 200 });
    }
    return new Response(HTML, {
      status: 200,
      headers: {
        'Content-Type': 'text/html; charset=utf-8',
        'Cache-Control': 'public, max-age=86400',
        'X-Content-Type-Options': 'nosniff',
      },
    });
  },
};
