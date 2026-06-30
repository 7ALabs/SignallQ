# DNS DoH Investigation

Data da consolidacao: 2026-06-30

## Decisao operacional

SIG-44 continua como investigacao opcional e nao bloqueante.

O PWA nao deve prometer DNS real do sistema nem benchmark real de resolvedor no browser.

## Conclusao

Classificacao: `degradado`, apenas como checagem indireta se um dia entrar.

O que seria possivel:

- chamar um endpoint DoH por `fetch`
- medir latencia HTTP desse endpoint
- comparar respostas entre provedores sob CORS permitido

O que nao seria possivel:

- descobrir o DNS real configurado no dispositivo
- medir o resolvedor do sistema operacional do usuario
- equivaler isso ao DNS do Android nativo
- tratar esse numero como benchmark DNS real

## Impacto no produto

- nao bloqueia SpeedTest
- nao bloqueia diagnostico
- nao bloqueia historico
- nao bloqueia QA final
- nao bloqueia deploy

## Recomendacao

Manter DNS fora do MVP e fora do fluxo principal de QA/release.

Se o time quiser retomar o assunto depois, a feature deve nascer como experimento explicitamente rotulado como checagem HTTP/DoH indireta, nunca como DNS real.

## Texto simples para usuario, se um dia aparecer

`A web nao consegue medir o DNS real do seu aparelho. Esta checagem, se exibida, representa apenas a resposta HTTP de um servico DoH compativel.`
