# SignallQ Admin — Arquitetura

## Visão geral

O SignallQ Admin é o console web separado do app Android. Ele serve para acompanhar uso, diagnósticos, métricas de rede, IA/custos, erros, versões, integrações e sinais de monetização futura.

O painel deve ficar dentro do mesmo repositório do SignallQ, preferencialmente em `/admin`, como projeto irmão do Android, Workers e contratos OpenAPI.

```txt
signallq/
  admin/
  workers/
  contracts/
  docs/
  app/ ou android/
```

## Fronteira correta

O frontend nunca deve acessar serviços sensíveis diretamente.

```txt
SignallQ Admin Web
    ↓
SignallQ Admin API / Cloudflare Worker
    ↓
Analytics DB / D1 / Analytics Engine
Firebase
Google Play Console
App Store Connect futuro
AI Providers
```

Credenciais, service accounts, tokens OAuth e chaves de IA pertencem ao backend/Worker, não ao React.

## Estado atual

- React + TypeScript + Vite.
- Tailwind CSS.
- Mocks centralizados em `src/mocks`.
- Services tipados em `src/services`.
- Adapters de integrações em `src/integrations`.
- OpenAPI inicial em `docs/openapi`.
- `npm run lint` passando.
- `npm run build` passando.

## API futura

O frontend foi preparado para variáveis:

```txt
VITE_ADMIN_API_BASE_URL
VITE_ENABLE_MOCKS
VITE_APP_ENV
```

Enquanto `VITE_ENABLE_MOCKS=true`, os services retornam dados simulados. Com a Admin API pronta, os services devem chamar `apiClient.request<T>()`.

## Fontes de dados previstas

- SignallQ Analytics: eventos do app e Worker.
- Firebase Analytics/Crashlytics: uso, sessões, crashes e versões.
- Google Play Console: instalações, rollout, versões e avaliações.
- App Store Connect: preparada para futuro iOS, desativada agora.
- Provedores de IA: uso, tokens, custos, fallback, latência e erros via Worker.

## Segurança e privacidade

Não coletar ou expor no painel:

- nome;
- e-mail;
- telefone;
- SSID completo;
- BSSID completo;
- IP público completo;
- localização exata;
- histórico individual identificável.

Usar dados agregados, `anonymousUserId`, `sessionId`, `stackHash`, cidade/estado quando necessário e categorias técnicas.

## Observação técnica

O build atual funciona, mas o bundle inicial ainda é grande. Antes de produção, aplicar code splitting por rota e revisar imports pesados de gráficos/ícones.
