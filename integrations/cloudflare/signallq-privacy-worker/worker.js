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
    h3 { font-size: 1rem; margin-top: 1.25rem; color: #333; }
    p, li { font-size: 1rem; }
    a { color: #6C2BFF; }
    .meta { color: #666; font-size: 0.9rem; margin-bottom: 2rem; }
    table { width: 100%; border-collapse: collapse; margin: 1rem 0; font-size: 0.95rem; }
    th, td { text-align: left; padding: 8px 12px; border-bottom: 1px solid #eee; }
    th { font-weight: 600; color: #333; }
    footer { margin-top: 3rem; padding-top: 1rem; border-top: 1px solid #eee; font-size: 0.85rem; color: #666; }
  </style>
</head>
<body>
  <h1>Política de Privacidade</h1>
  <p class="meta">SignallQ &mdash; Última atualização: 28 de junho de 2026</p>

  <p>O SignallQ é um aplicativo de diagnóstico de conexão à internet para Android. Esta política descreve quais dados são coletados, como são usados, com quem são compartilhados e quais são os seus direitos como usuário.</p>

  <h2>1. Dados coletados e finalidade</h2>
  <p>O SignallQ coleta exclusivamente dados técnicos de conectividade para fins de diagnóstico. Nenhum dado de identificação pessoal é coletado.</p>

  <h3>Dados coletados</h3>
  <ul>
    <li><strong>Métricas de rede:</strong> velocidade de download e upload, latência, jitter, perda de pacotes e bufferbloat</li>
    <li><strong>Informações de Wi-Fi:</strong> SSID, intensidade de sinal (RSSI), frequência de banda e canal</li>
    <li><strong>Informações de rede móvel:</strong> tecnologia (4G/5G), intensidade de sinal (RSRP/RSRQ/SINR) e operadora</li>
    <li><strong>Dispositivos na rede local:</strong> identificados via UPnP (somente nome e endereço MAC, nunca conteúdo de tráfego)</li>
    <li><strong>Histórico de medições:</strong> armazenado localmente no dispositivo do usuário</li>
    <li><strong>Credenciais do modem:</strong> armazenadas localmente com criptografia, usadas para acesso ao painel do modem quando configurado pelo usuário</li>
  </ul>

  <h3>Dados NÃO coletados</h3>
  <p>O SignallQ <strong>não</strong> coleta: nome, e-mail, endereço, localização GPS, contatos, fotos, arquivos, histórico de navegação nem qualquer dado de identificação pessoal.</p>

  <h2>2. Como os dados são usados</h2>
  <ul>
    <li>Exibição de diagnóstico local no próprio dispositivo</li>
    <li>Envio ao motor de inteligência artificial para geração de laudo técnico de conectividade</li>
    <li>Monitoramento periódico em segundo plano para alertas de queda de qualidade</li>
  </ul>
  <p>Os dados enviados ao servidor de IA são processados em tempo real e descartados imediatamente após a geração do laudo. Nenhum dado é armazenado de forma persistente no servidor.</p>

  <h2>3. Compartilhamento com terceiros</h2>
  <p>Os dados de diagnóstico (métricas de rede anonimizadas, sem identificação pessoal) são enviados a um servidor de processamento hospedado na <strong>Cloudflare</strong> para análise por inteligência artificial. O servidor é operado pelo próprio desenvolvedor do SignallQ.</p>
  <p>Além disso, o app utiliza:</p>
  <ul>
    <li><strong>Firebase Analytics:</strong> coleta de eventos anônimos de uso (telas visitadas, ações realizadas). Nenhum dado pessoal é vinculado a esses eventos.</li>
    <li><strong>Firebase Crashlytics:</strong> coleta automática de relatórios de falha (crash reports) anônimos para melhoria da estabilidade do app.</li>
  </ul>
  <p>Nenhum dado é vendido, alugado ou compartilhado com terceiros para fins publicitários, de marketing ou qualquer outra finalidade comercial. O SignallQ não exibe anúncios e não utiliza rastreamento.</p>

  <h2>4. Armazenamento e segurança</h2>
  <ul>
    <li><strong>Dados locais:</strong> o histórico de medições é armazenado no dispositivo do usuário em banco de dados local. Credenciais do modem são armazenadas com criptografia. Todos os dados locais podem ser apagados pelo usuário a qualquer momento via configurações do app ou pela desinstalação.</li>
    <li><strong>Dados enviados ao servidor:</strong> processados em tempo real e descartados. Não há armazenamento persistente no servidor.</li>
    <li><strong>Infraestrutura:</strong> o servidor de processamento de IA opera na infraestrutura da Cloudflare, sujeita à <a href="https://www.cloudflare.com/privacypolicy/" target="_blank" rel="noopener">política de privacidade da Cloudflare</a>.</li>
    <li><strong>Firebase:</strong> os dados de analytics e crash são processados pelo Google Firebase conforme a <a href="https://policies.google.com/privacy" target="_blank" rel="noopener">política de privacidade do Google</a>.</li>
  </ul>

  <h2>5. Permissões solicitadas</h2>
  <table>
    <thead>
      <tr><th>Permissão</th><th>Finalidade</th><th>O que NÃO faz</th></tr>
    </thead>
    <tbody>
      <tr><td><strong>ACCESS_FINE_LOCATION</strong></td><td>Necessária pelo sistema Android para leitura do SSID e canal Wi-Fi</td><td>Não rastreia localização GPS</td></tr>
      <tr><td><strong>READ_PHONE_STATE</strong></td><td>Leitura de métricas de sinal celular (RSRP/RSRQ/SINR) em redes 4G/5G</td><td>Não acessa chamadas, SMS ou contatos</td></tr>
      <tr><td><strong>FOREGROUND_SERVICE</strong></td><td>Manter o monitoramento ativo em segundo plano com notificação visível</td><td>&mdash;</td></tr>
      <tr><td><strong>ACCESS_NETWORK_STATE / ACCESS_WIFI_STATE / CHANGE_NETWORK_STATE</strong></td><td>Leitura de estado da conexão e configuração de rede</td><td>&mdash;</td></tr>
    </tbody>
  </table>

  <h2>6. Direitos do usuário (LGPD)</h2>
  <p>Em conformidade com a Lei Geral de Proteção de Dados (Lei 13.709/2018), você pode a qualquer momento:</p>
  <ul>
    <li><strong>Acessar</strong> seus dados armazenados localmente diretamente no app (tela de Histórico)</li>
    <li><strong>Corrigir</strong> dados que considere incorretos (configurações do modem)</li>
    <li><strong>Excluir</strong> todo o histórico de medições pelo app (Ajustes &gt; Limpar histórico) ou desinstalando o aplicativo</li>
    <li><strong>Revogar</strong> permissões do app nas configurações do sistema Android</li>
    <li><strong>Solicitar informações</strong> sobre o tratamento de dados pelo e-mail de contato abaixo</li>
    <li><strong>Portar</strong> seus dados: como os dados são armazenados exclusivamente no seu dispositivo, você tem controle total sobre eles</li>
  </ul>
  <p>Como o SignallQ não coleta dados pessoais identificáveis e não mantém dados persistentes em servidores, a maior parte dos direitos previstos na LGPD já são atendidos pela natureza do funcionamento do app.</p>

  <h2>7. Menores de idade</h2>
  <p>O SignallQ não é direcionado a menores de 13 anos e não coleta conscientemente dados de crianças.</p>

  <h2>8. Contato</h2>
  <p>Dúvidas, solicitações ou outros assuntos relacionados à privacidade:<br>
  <strong>E-mail:</strong> <a href="mailto:giammattey.luiz@gmail.com">giammattey.luiz@gmail.com</a><br>
  <strong>Desenvolvedor:</strong> Luiz Giammattey &mdash; 7Agents</p>

  <h2>9. Alterações nesta política</h2>
  <p>Esta política pode ser atualizada periodicamente. A data de última atualização está indicada no topo do documento. O uso continuado do app após uma alteração implica aceitação da nova versão.</p>

  <footer>SignallQ &mdash; Desenvolvido por Luiz Giammattey &mdash; 7Agents &mdash; <a href="mailto:giammattey.luiz@gmail.com">giammattey.luiz@gmail.com</a></footer>
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
