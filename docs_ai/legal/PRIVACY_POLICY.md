# Política de Privacidade — SignallQ

**Última atualização:** 17 de agosto de 2026
**Vigência:** a partir de 17 de agosto de 2026

> Alteração desta revisão: a versão anterior afirmava que o aplicativo não exibia anúncios. O SignallQ passou a exibir anúncios contextuais, e as seções 2, 3 e 4 foram atualizadas para descrever isso com precisão.

O SignallQ é um aplicativo de diagnóstico de conexão à internet para Android. Esta política descreve quais dados são coletados, como são usados, com quem são compartilhados e quais são os seus direitos como usuário.

---

## 1. Dados coletados e finalidade

O SignallQ coleta exclusivamente dados técnicos de conectividade para fins de diagnóstico. Nenhum dado de identificação pessoal é coletado.

### Dados coletados

- **Métricas de rede:** velocidade de download e upload, latência, jitter, perda de pacotes e bufferbloat.
- **Informações de Wi-Fi:** SSID, intensidade de sinal (RSSI), frequência de banda e canal.
- **Informações de rede móvel:** tecnologia (4G/5G), intensidade de sinal (RSRP/RSRQ/SINR) e operadora.
- **Dispositivos na rede local:** identificados via UPnP (somente nome e endereço MAC, nunca conteúdo de tráfego).
- **Histórico de medições:** armazenado localmente no dispositivo do usuário.
- **Credenciais do modem:** armazenadas localmente com criptografia, usadas para acesso ao painel do modem quando configurado pelo usuário.

### Dados NÃO coletados

O SignallQ **não** coleta: nome, e-mail, endereço, localização GPS, contatos, fotos, arquivos, histórico de navegação nem qualquer dado de identificação pessoal.

---

## 2. Como os dados são usados

- Exibição de diagnóstico local no próprio dispositivo.
- Envio ao motor de inteligência artificial para geração de laudo técnico de conectividade.
- Monitoramento periódico em segundo plano para alertas de queda de qualidade.
- Exibição de anúncios contextuais para sustentar a gratuidade do aplicativo, sem uso de dados pessoais ou de diagnóstico na seleção do anúncio.

Os dados enviados ao servidor de IA são processados em tempo real e descartados imediatamente após a geração do laudo. Nenhum dado é armazenado de forma persistente no servidor.

---

## 3. Compartilhamento com terceiros

Os dados de diagnóstico (métricas de rede anonimizadas, sem identificação pessoal) são enviados a um servidor de processamento hospedado na Cloudflare para análise por inteligência artificial. O servidor é operado pelo próprio desenvolvedor do SignallQ.

Além disso, o app utiliza:

- **Firebase Analytics:** coleta de eventos anônimos de uso (telas visitadas, ações realizadas). Nenhum dado pessoal é vinculado a esses eventos.
- **Firebase Crashlytics:** coleta automática de relatórios de falha (crash reports) anônimos para melhoria da estabilidade do app.
- **Google AdMob:** o SignallQ exibe anúncios para sustentar a gratuidade do aplicativo. Os anúncios são **contextuais e não personalizados** — a escolha do anúncio considera apenas o assunto da tela em que ele aparece (por exemplo, "resultado de teste de velocidade"), nunca o seu histórico de navegação, a sua identidade ou os dados da sua rede. Nenhuma métrica de conectividade, nome de rede Wi-Fi (SSID), endereço IP ou endereço MAC é enviado ao AdMob. Consulte a [política de privacidade do Google](https://policies.google.com/privacy).

Nenhum dado é vendido ou alugado. O SignallQ **não** utiliza rastreamento entre aplicativos, não constrói perfil de comportamento e não compartilha dados de diagnóstico com anunciantes.

---

## 4. Consentimento para anúncios

Antes de qualquer anúncio ser solicitado, o SignallQ usa a **User Messaging Platform (UMP)** do Google para verificar se o seu consentimento é necessário na sua região e, quando for, apresentar o formulário correspondente. Enquanto não houver resposta, ou se você recusar, o aplicativo **não solicita anúncios** — não se trata apenas de ocultar o anúncio da tela.

Onde o consentimento é exigido, você pode revisar sua escolha a qualquer momento em **Privacidade → Preferências de anúncios**, dentro do aplicativo.

---

## 5. Armazenamento e segurança

- **Dados locais:** o histórico de medições é armazenado no dispositivo do usuário em banco de dados local. Credenciais do modem são armazenadas com criptografia. Todos os dados locais podem ser apagados pelo usuário a qualquer momento via configurações do app ou pela desinstalação.
- **Dados enviados ao servidor:** processados em tempo real e descartados. Não há armazenamento persistente no servidor.
- **Infraestrutura:** o servidor de processamento de IA opera na infraestrutura da Cloudflare, sujeita à [política de privacidade da Cloudflare](https://www.cloudflare.com/privacypolicy/).
- **Firebase:** os dados de analytics e crash são processados pelo Google Firebase conforme a [política de privacidade do Google](https://policies.google.com/privacy).

---

## 6. Permissões solicitadas

| Permissão | Finalidade | O que NÃO faz |
|---|---|---|
| **ACCESS_FINE_LOCATION** | Necessária pelo sistema Android para leitura do SSID e canal Wi-Fi | Não rastreia localização GPS |
| **READ_PHONE_STATE** | Leitura de métricas de sinal celular (RSRP/RSRQ/SINR) em redes 4G/5G | Não acessa chamadas, SMS ou contatos |
| **FOREGROUND_SERVICE** | Manter o monitoramento ativo em segundo plano com notificação visível | — |
| **ACCESS_NETWORK_STATE / ACCESS_WIFI_STATE / CHANGE_NETWORK_STATE** | Leitura de estado da conexão e configuração de rede | — |

---

## 7. Direitos do usuário (LGPD)

Em conformidade com a Lei Geral de Proteção de Dados (Lei 13.709/2018), você pode a qualquer momento:

- **Acessar** seus dados armazenados localmente diretamente no app (tela de Histórico).
- **Corrigir** dados que considere incorretos (configurações do modem).
- **Excluir** todo o histórico de medições pelo app (Ajustes > Limpar histórico) ou desinstalando o aplicativo.
- **Revogar** permissões do app nas configurações do sistema Android.
- **Solicitar informações** sobre o tratamento de dados pelo e-mail de contato abaixo.
- **Portar** seus dados: como os dados são armazenados exclusivamente no seu dispositivo, você tem controle total sobre eles.

Como o SignallQ não coleta dados pessoais identificáveis e não mantém dados persistentes em servidores, a maior parte dos direitos previstos na LGPD já são atendidos pela natureza do funcionamento do app.

---

## 8. Menores de idade

O SignallQ não é direcionado a menores de 13 anos e não coleta conscientemente dados de crianças.

---

## 9. Contato

Dúvidas, solicitações ou outros assuntos relacionados à privacidade:

**E-mail:** giammattey.luiz@gmail.com
**Desenvolvedor:** Luiz Giammattey — 7Agents

---

## 10. Alterações nesta política

Esta política pode ser atualizada periodicamente. A data de última atualização está indicada no topo do documento. O uso continuado do app após uma alteração implica aceitação da nova versão.
