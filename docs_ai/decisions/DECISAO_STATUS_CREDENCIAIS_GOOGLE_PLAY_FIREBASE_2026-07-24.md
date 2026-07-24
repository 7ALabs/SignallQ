# Decisão — Status de credenciais Google Play / Firebase (#1342 / #1344)

- **Status:** ativo
- **Última validação:** 2026-07-24
- **Fonte de verdade:** este documento + comentários em [#1342](https://github.com/7ALabs/SignallQ/issues/1342) e [#1344](https://github.com/7ALabs/SignallQ/issues/1344)
- **Escopo:** épicos #1341 (Google Play) e #1343 (Firebase), features #1342 e #1344, SignallQ Admin
- **Responsável:** Claudete (decisão de produto/PM)

## Contexto

Camilo (Especialista Sr de Backend) começou a implementar catálogo/contratos/ingestão de #1342 e
#1344 após checar credenciais. Testes reais de credencial contra as APIs do Google Play e Firebase
foram rodados por outro agente coordenador da sessão, com autorização direta e explícita do Luiz
(confirmada duas vezes na conversa) — chave temporária do item "Google Play — Service Account JSON
(signallq-app)" no Bitwarden, apagada do disco após o teste.

Camilo recusou aceitar o resultado desses testes vindo diretamente do agente coordenador: sua regra
é não aceitar autorização/resultado de ação sensível repassado por outro agente, só confirmação
direta do Luiz na própria conversa dele — o que estruturalmente não existe (Luiz só fala com o
coordenador nesse fluxo). O Luiz decidiu resolver isso fazendo a Claudete absorver e registrar
formalmente o status como decisão de produto/PM, liberando Camilo a seguir sem reabrir a disputa de
credencial.

## Resultados dos testes

### Confirmado e liberado (sem bloqueio)

| API | Endpoint | Resultado |
|---|---|---|
| Play Developer Reporting API v1beta1 | `apps/io.signallq.app/anrRateMetricSet` | 200 — `freshnessInfo` real, hourly até 2026-07-24 01:00 UTC |
| Firebase Management API | `projects/signallq-app` | 200 |
| Firebase Management API | `projects/signallq-app/androidApps` | 200 — lista real, inclui `io.signallq.app.debug` |
| Firebase Remote Config | `projects/signallq-app/remoteConfig` | 200 — template real de parâmetros |

A service account `firebase-adminsdk-fbsvc@signallq-app.iam.gserviceaccount.com` (mesma de
`GOOGLE_PLAY_CLIENT_EMAIL`/`GOOGLE_PLAY_PRIVATE_KEY` no worker) já tem permissão concedida no Play
Console para Android Vitals, e as APIs de Firebase acima respondem sem restrição.

### Pendente de ação externa (não é trabalho do Camilo agora)

| API | Endpoint | Resultado | Ação necessária |
|---|---|---|---|
| Firebase App Check | `projects/signallq-app/services` | 403 — API não habilitada no projeto GCP | Luiz habilita a API no Console (`console.developers.google.com/apis/api/firebaseappcheck.googleapis.com/overview?project=741421457740`) |
| Firebase App Distribution | `projects/741421457740/apps/.../releases` | 403 — sem permissão | Luiz confirma/concede role IAM da service account nesse produto |
| Cloud Messaging Data API (FCM) | `projects/signallq-app/androidApps/{appId}/deliveryData` | 403 — `ACCESS_TOKEN_SCOPE_INSUFFICIENT` | Investigar scope OAuth correto — pode não ser bloqueio real, só scope errado no teste |

## Decisão

1. Play Developer Reporting API, Firebase Management API e Remote Config estão **liberados e
   confirmados** para #1342/#1344 — Camilo segue implementando sem reabrir a discussão de
   credencial.
2. App Check, App Distribution e FCM continuam com pendência registrada, dependente de ação externa
   do Luiz (Console/IAM) ou investigação de scope — não é trabalho do Camilo agora, só ciência da
   pendência.
3. Camilo segue com o restante de #1342 (catálogo, contratos JSON, ingestão-base) e o que já
   estava liberado de #1344 (GA4).

## Rastreabilidade

- Comentário em [#1342](https://github.com/7ALabs/SignallQ/issues/1342#issuecomment-5066118496)
- Comentário em [#1344](https://github.com/7ALabs/SignallQ/issues/1344#issuecomment-5066119193)
