# Especificação Funcional por Tela

## Objetivo

Definir o comportamento funcional do SignallQ PWA com detalhes suficientes para orientar implementação, revisão e QA.

Este documento é o equivalente operacional da página Funcional no Notion.

## Premissa do produto

O SignallQ PWA deve ajudar o usuário comum a entender a qualidade da conexão de internet pelo navegador.

A promessa central não é “medir tudo”. A promessa é explicar, com honestidade, se a conexão está boa, lenta ou instável, e indicar o próximo passo.

## Regras funcionais obrigatórias

- Sem chat livre como interface principal.
- Diagnóstico curto, objetivo e acionável.
- Separar velocidade de estabilidade.
- Não inventar métrica que o navegador não mediu.
- Métrica ausente deve aparecer como “não medida” ou equivalente.
- Limitações da versão web devem ser comunicadas sem assustar o usuário.
- O app precisa funcionar sem login no MVP.
- Histórico inicial deve ser local.
- IA não pode bloquear o resultado do teste.
- Falha parcial de métrica não deve inutilizar todo o resultado.
- Paridade com Android deve seguir `pwa/docs/parity.md`.

## Escopo funcional por fase

### M0 — Fundação

M0 não entrega medição real. Entrega a base funcional navegável.

Inclui:

- App shell.
- Estrutura inicial de telas.
- Landing simples.
- Home simples.
- Estados visuais base.
- Manifest PWA básico.
- Design tokens mínimos.
- Nenhuma métrica falsa.

Fora do M0:

- SpeedTest real.
- Diagnóstico real.
- Histórico persistido.
- IA.
- Worker.
- D1.
- DNS benchmark real.
- Sinal Wi-Fi real.
- Dispositivos.
- Fibra/modem.

### M1 — Core funcional

Inclui:

- SpeedTest web com download e latência HTTP.
- Upload apenas se houver endpoint controlado.
- Jitter se houver amostras suficientes.
- Diagnóstico local básico.
- Resultado com recomendações.
- Histórico local com IndexedDB.
- Detalhe de teste salvo.

Não inclui:

- DNS benchmark real.
- RSSI Wi-Fi.
- Scan de rede.
- Sinal móvel real.
- Fibra/modem.
- Dispositivos conectados.

### M2 — UX e integração

Inclui:

- Refinamento visual mobile-first.
- Estados completos de erro, vazio, loading e sucesso.
- Install prompt quando viável.
- Diagnóstico IA via Worker, se contrato estiver pronto.
- Telemetria básica se não comprometer custo e privacidade.
- Sinal degradado, se fizer sentido: online/offline, tipo estimado de conexão e limitações.

### M3 — Beta e entrega

Inclui:

- QA cross-browser.
- Lighthouse.
- Ajustes finais.
- Documentação de uso.
- Deploy/preview validado.

## Fluxo principal do usuário

1. Usuário acessa o PWA.
2. Entende rapidamente a proposta.
3. Inicia teste de conexão.
4. App mede o que o navegador permite.
5. App classifica velocidade e estabilidade.
6. App apresenta diagnóstico simples.
7. Usuário recebe até 3 ações recomendadas.
8. Resultado pode ser salvo no histórico local.
9. Usuário pode consultar detalhes depois.

## Navegação mínima

A navegação inicial pode ser simples, sem React Router no M0.

Rotas ou estados previstos:

- Landing.
- Home.
- Teste.
- Resultado.
- Histórico.
- Detalhe do teste.
- Ajustes.
- Sobre/Privacidade.

Rotas futuras/degradadas:

- Sinal degradado.

Rotas fora do MVP por limitação web:

- Dispositivos.
- Fibra/modem.
- DNS benchmark real.

React Router só deve entrar quando houver necessidade real de URLs navegáveis ou compartilhamento.

## Tela: Landing Page

### Objetivo

Apresentar o SignallQ e converter o usuário para iniciar um teste.

### Perguntas que a tela responde

- O que é o SignallQ?
- Para que serve?
- O que ele consegue fazer na versão web?
- Qual é o próximo passo?

### Conteúdo mínimo

- Nome do produto: SignallQ.
- Frase principal: diagnóstico simples da sua internet.
- Explicação curta: mede a conexão pelo navegador e explica se está boa, lenta ou instável.
- CTA principal: “Iniciar teste”.
- Link secundário: “Como funciona” ou “Privacidade”.

### Ações

- Iniciar teste.
- Abrir privacidade/sobre.

### Estados

- carregada;
- erro crítico do app, se ocorrer.

### Critérios de aceite

- CTA principal visível sem esforço no mobile.
- Nenhuma promessa de medir sinal Wi-Fi real.
- Texto em PT-BR simples.

## Tela: Home / Dashboard

### Objetivo

Servir como ponto de entrada recorrente para novo teste e histórico.

### Dados exibidos

- CTA para novo teste.
- Último resultado, se existir.
- Estado resumido da última conexão.
- Atalho para histórico.
- Opcional futuro: status online/offline.

### Ações

- Novo teste.
- Abrir histórico.
- Abrir detalhe do último teste.

### Estados

#### Sem histórico

Mostrar CTA forte para primeiro teste.

#### Com histórico

Mostrar último resultado de forma resumida.

#### Erro ao carregar histórico

Informar erro e permitir iniciar novo teste mesmo assim.

### Critérios de aceite

- Usuário consegue iniciar um teste em um toque.
- Histórico quebrado não bloqueia novo teste.
- Último resultado não deve parecer medição em tempo real.

## Tela: SpeedTest

### Objetivo

Executar medição web de conexão com feedback visual claro.

### Etapas previstas

1. Preparando teste.
2. Medindo latência HTTP.
3. Medindo download.
4. Medindo upload, se endpoint existir.
5. Calculando estabilidade.
6. Gerando diagnóstico local.

### Dados exibidos durante o teste

- Etapa atual.
- Progresso textual ou visual.
- Métrica parcial quando disponível.
- Aviso curto se alguma métrica não puder ser medida.

### Ações

- Iniciar.
- Cancelar, se a implementação permitir.
- Tentar novamente após erro.

### Estados

#### Pronto

Usuário ainda não iniciou teste.

#### Medindo

Teste em andamento.

#### Erro parcial

Uma métrica falhou, mas o restante pode continuar.

#### Erro total

Teste não conseguiu obter métricas mínimas.

#### Concluído

Resultado disponível.

### Critérios de aceite

- Download não pode ser valor fake.
- Latência deve ser descrita como medição HTTP, não ping ICMP real.
- Upload só aparece como medido se houver endpoint.
- Erro parcial não deve apagar métricas válidas.
- Usuário deve entender que o teste está em andamento.

## Tela: Resultado do Diagnóstico

### Objetivo

Explicar o estado da conexão e orientar o usuário.

### Hierarquia da tela

1. Status geral.
2. Resumo em linguagem simples.
3. Velocidade.
4. Estabilidade.
5. Ações recomendadas.
6. Métricas técnicas.
7. Limitações do teste.

### Dados exibidos

- Status geral: bom, atenção, ruim ou desconhecido.
- Download.
- Upload, se medido.
- Latência HTTP.
- Jitter, se medido.
- Estabilidade.
- Resumo do diagnóstico.
- Até 3 recomendações.
- Limitações relevantes.

### Ações

- Refazer teste.
- Salvar no histórico.
- Abrir detalhes técnicos.
- Voltar para Home.

### Estados

#### Diagnóstico local

Usado quando IA não existe ou não foi chamada.

#### Diagnóstico IA

Usado quando Worker responder dentro do contrato.

#### IA indisponível

Usar fallback local.

#### Métricas parciais

Exibir resultado com confiança menor.

### Critérios de aceite

- Resultado deve ser entendido em menos de 10 segundos.
- Não mostrar mais de 3 recomendações principais.
- Separar velocidade e estabilidade visualmente.
- Limitações não devem ficar escondidas.
- Se IA falhar, o usuário ainda recebe diagnóstico local.

## Tela: Histórico

### Objetivo

Permitir consultar testes anteriores salvos localmente.

### Dados exibidos por item

- Data e hora.
- Status geral.
- Download.
- Latência.
- Estabilidade resumida.

### Ações

- Abrir detalhe.
- Apagar item.
- Limpar histórico.
- Fazer novo teste.

### Estados

#### Vazio

Mensagem simples e CTA para iniciar teste.

#### Carregando

Indicar leitura local.

#### Lista

Mostrar testes ordenados do mais recente para o mais antigo.

#### Erro

Informar falha de leitura local e permitir novo teste.

### Critérios de aceite

- Funciona sem login.
- Apagar item individual funciona.
- Limpar histórico inteiro exige confirmação.
- Modo privado ou falha de IndexedDB não trava o app.
- Não apresentar histórico local como sincronizado.

## Tela: Detalhe de Teste

### Objetivo

Mostrar o resultado completo de um teste salvo.

### Dados exibidos

- Data/hora.
- Status geral.
- Métricas completas.
- Diagnóstico.
- Recomendações.
- Limitações.
- Dados básicos do navegador, quando disponíveis.

### Ações

- Refazer teste.
- Apagar este resultado.
- Voltar ao histórico.

### Critérios de aceite

- Deve deixar claro que é um resultado salvo, não medição atual.
- Deve exibir limitações que afetaram o teste.
- Não deve mostrar campos técnicos vazios como se fossem medidos.

## Tela futura/degradada: Sinal

### Objetivo

Mostrar apenas informações de conectividade que o browser permite.

### Pode exibir

- Online/offline.
- Tipo estimado de conexão, quando Network Information API estiver disponível.
- Aviso de limitação web.

### Não pode exibir

- RSSI.
- Lista de redes Wi-Fi.
- Canal/frequência.
- Interferência.
- Sinal móvel RSRP/RSRQ/SINR.
- Dados de SIM/cell ID.

### Status

`degradado`.

## Fora do escopo permanente: Dispositivos

Scan de dispositivos por ARP, mDNS ou SSDP direto no browser é `n/a-browser`.

Não implementar em PWA público.

## Fora do escopo permanente: Fibra/modem

Diagnóstico direto de modem local/fibra é `n/a-browser` para PWA público.

Só seria possível com proxy, extensão ou ambiente controlado. Não entra no MVP.

## Fora do escopo inicial: DNS benchmark real

DNS benchmark real é `n/a-browser` sem proxy dedicado.

Pode haver checagem indireta ou Worker dedicado no futuro, mas não prometer benchmark DNS real no MVP.

## Tela: Ajustes mínimos

### Objetivo

Oferecer controles básicos realmente úteis.

### Conteúdo inicial

- Limpar histórico.
- Informações de privacidade.
- Versão do app.
- Link para Sobre.

### Fora do escopo

- Perfil complexo.
- Login.
- Preferências avançadas sem função real.
- Tema customizável no MVP.

### Critérios de aceite

- Limpar histórico exige confirmação.
- Não ter tela genérica cheia de opção inútil.

## Tela: Sobre / Privacidade

### Objetivo

Explicar o que o PWA mede, o que não mede e como trata dados.

### Conteúdo mínimo

- O PWA mede a conexão pelo navegador.
- Algumas métricas nativas não estão disponíveis na web.
- Histórico fica salvo neste navegador.
- Se IA for usada, o app pode enviar métricas estruturadas para análise.
- Nenhuma senha ou dado sensível deve ser solicitado no MVP.

### Texto base sugerido

“Este teste mede a experiência da conexão pelo navegador. Algumas medições avançadas, como sinal Wi-Fi real ou perda de pacote nativa, não estão disponíveis na versão web.”

## Estados globais

### Loading

Usar quando o app está aguardando operação real.

### Erro

Mensagem clara, sem stack trace para usuário.

### Vazio

Explicar o que falta e oferecer próxima ação.

### Sucesso

Confirmar ação realizada sem excesso visual.

## Eventos funcionais futuros

MVP não precisa de telemetria completa, mas os eventos abaixo podem ser planejados seguindo `docs_ai/technical/analytics-events.md`:

- `app_aberto` com `plataforma: "pwa"`.
- `speedtest_iniciado`.
- `speedtest_concluido`.
- `speedtest_erro`.
- `diag_iniciado`.
- `diag_concluido`.
- `diag_erro`.
- `ia_laudo_solicitado`.
- `ia_laudo_recebido`.
- `ia_laudo_erro`.
- `historico_tela_aberta`.

Nenhum evento deve enviar dado sensível.

## Critérios gerais de aceite

- Toda tela relevante tem estado vazio, erro, loading e sucesso.
- CTA principal é claro.
- Usuário entende o resultado em menos de 10 segundos.
- Métrica ausente aparece como não medida.
- Nenhuma tela depende de login no MVP.
- Nenhuma tela promete recurso nativo Android.
- Falha de IA não bloqueia resultado.
- Histórico local não é apresentado como sincronizado.
- Funcionalidade nativa impossível deve ser classificada conforme `parity.md`.
- Alterações futuras que mudem contrato devem atualizar este documento e a página Funcional no Notion.
