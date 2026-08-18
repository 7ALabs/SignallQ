---
title: "Política de Privacidade — SignallQ"
description: "Quais dados o app coleta, como são usados, com quem são compartilhados e quais são os direitos do usuário. Documento público, espelhado pelo signallq-privacy-worker."
type: "referência"
status: "ativo"
owner: "Luiz Giammattey"
last_updated: "2026-08-17"
version: "2.0.0"
---

# Política de Privacidade — SignallQ

**Última atualização:** 17 de agosto de 2026
**Vigência:** a partir de 17 de agosto de 2026

> Alteração desta revisão: a versão anterior afirmava que o aplicativo não exibia anúncios. O SignallQ exibe anúncios do Google AdMob, que são **personalizados** pelo Google, e as seções 1, 2, 3, 4, 6 e 7 foram atualizadas para descrever isso com precisão — incluindo o identificador de publicidade e como controlá-lo.

O SignallQ é um aplicativo de diagnóstico de conexão à internet para Android. Esta política descreve quais dados são coletados, como são usados, com quem são compartilhados e quais são os seus direitos como usuário.

---

## 1. Dados coletados e finalidade

O SignallQ coleta dados técnicos de conectividade para fins de diagnóstico. Não coletamos seu nome, e-mail ou endereço. Os anúncios exibidos no aplicativo, porém, usam o **identificador de publicidade do Android**, descrito abaixo e na seção 3.

### Dados coletados

- **Métricas de rede:** velocidade de download e upload, latência, jitter, perda de pacotes e bufferbloat.
- **Informações de Wi-Fi:** SSID, intensidade de sinal (RSSI), frequência de banda e canal.
- **Informações de rede móvel:** tecnologia (4G/5G), intensidade de sinal (RSRP/RSRQ/SINR) e operadora.
- **Dispositivos na rede local:** identificados via UPnP (somente nome e endereço MAC, nunca conteúdo de tráfego).
- **Histórico de medições:** armazenado localmente no dispositivo do usuário.
- **Credenciais do modem:** armazenadas localmente com criptografia, usadas para acesso ao painel do modem quando configurado pelo usuário.
- **Identificador de publicidade (Advertising ID):** identificador do aparelho, redefinível por você nas configurações do Android, usado pelo Google AdMob para escolher e medir os anúncios. Não é coletado nem armazenado pelo SignallQ — quem o usa é o SDK do Google, dentro do aplicativo.

### Dados NÃO coletados

O SignallQ **não** coleta: nome, e-mail, endereço, localização GPS, contatos, fotos, arquivos nem histórico de navegação.

O aplicativo também **não envia** ao AdMob nenhuma métrica de conectividade, nome de rede Wi-Fi (SSID), endereço MAC de dispositivo da sua rede nem o laudo de diagnóstico. Isso é diferente de dizer que o anúncio não é personalizado: o Google personaliza a partir do que ele já sabe do seu aparelho e da sua conta, não a partir do que o SignallQ mede.

---

## 2. Como os dados são usados

- Exibição de diagnóstico local no próprio dispositivo.
- Envio ao motor de inteligência artificial para geração de laudo técnico de conectividade.
- Monitoramento periódico em segundo plano para alertas de queda de qualidade.
- Exibição de anúncios para sustentar a gratuidade do aplicativo. O SignallQ informa ao AdMob apenas o assunto da tela em que o anúncio aparece (por exemplo, "resultado de teste de velocidade"); a personalização em si é feita pelo Google.

Os dados enviados ao servidor de IA são processados em tempo real e descartados imediatamente após a geração do laudo. Nenhum dado é armazenado de forma persistente no servidor.

---

## 3. Compartilhamento com terceiros

Os dados de diagnóstico (métricas de rede anonimizadas, sem identificação pessoal) são enviados a um servidor de processamento hospedado na Cloudflare para análise por inteligência artificial. O servidor é operado pelo próprio desenvolvedor do SignallQ.

Além disso, o app utiliza:

- **Firebase Analytics:** coleta de eventos anônimos de uso (telas visitadas, ações realizadas). Nenhum dado pessoal é vinculado a esses eventos.
- **Firebase Crashlytics:** coleta automática de relatórios de falha (crash reports) anônimos para melhoria da estabilidade do app.
- **Google AdMob:** o SignallQ exibe anúncios para sustentar a gratuidade do aplicativo. Os anúncios são **personalizados pelo Google**, que usa o identificador de publicidade do seu aparelho e os dados que ele já possui. O SignallQ acrescenta a isso um único sinal: o assunto da tela em que o anúncio aparece. **Nenhuma métrica de conectividade, nome de rede Wi-Fi (SSID), endereço MAC ou laudo de diagnóstico é enviado ao AdMob.** Consulte a [política de privacidade do Google](https://policies.google.com/privacy).

Nenhum dado é vendido ou alugado pelo SignallQ, e nós não construímos perfil de comportamento nem compartilhamos dados de diagnóstico com anunciantes. O Google, por sua vez, usa o identificador de publicidade para personalizar anúncios — é isso que a seção 4 explica como controlar.

---

## 4. Consentimento para anúncios

Antes de qualquer anúncio ser solicitado, o SignallQ usa a **User Messaging Platform (UMP)** do Google para verificar se o seu consentimento é necessário na sua região e, quando for, apresentar o formulário correspondente. Enquanto não houver resposta, ou se você recusar, o aplicativo **não solicita anúncios** — não se trata apenas de ocultar o anúncio da tela.

Fora dessas regiões — no Brasil, por exemplo — a legislação não exige o formulário, e os anúncios são solicitados sem ele.

Em qualquer região, você encontra **Privacidade → Preferências de anúncios** dentro do aplicativo. Onde a UMP tem formulário, o item o abre; onde não tem, ele leva às configurações de anúncios do Android, onde é possível **limitar a personalização** e **redefinir ou excluir o identificador de publicidade**. O SignallQ continua funcionando integralmente com a personalização desligada.

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
| **com.google.android.gms.permission.AD_ID** | Acesso ao identificador de publicidade, usado pelo Google AdMob para escolher e medir anúncios. Vem do SDK do Google, não é pedida em tela | Não identifica você pessoalmente e pode ser redefinida ou excluída por você nas configurações do Android |

---

## 7. Direitos do usuário (LGPD)

Em conformidade com a Lei Geral de Proteção de Dados (Lei 13.709/2018), você pode a qualquer momento:

- **Acessar** seus dados armazenados localmente diretamente no app (tela de Histórico).
- **Corrigir** dados que considere incorretos (configurações do modem).
- **Excluir** todo o histórico de medições pelo app (Ajustes > Limpar histórico) ou desinstalando o aplicativo.
- **Revogar** permissões do app nas configurações do sistema Android.
- **Solicitar informações** sobre o tratamento de dados pelo e-mail de contato abaixo.
- **Portar** seus dados: como os dados são armazenados exclusivamente no seu dispositivo, você tem controle total sobre eles.

Os dados de diagnóstico ficam no seu aparelho e não são mantidos em nossos servidores, então boa parte desses direitos é atendida pela própria forma como o app funciona.

O identificador de publicidade é a exceção, e vale dizer com clareza: ele é um identificador do seu aparelho, tratado pelo Google. Você o controla nas configurações do Android — pode limitar a personalização, redefini-lo ou excluí-lo — e o caminho está em **Privacidade → Preferências de anúncios** dentro do aplicativo.

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
