# SignallQ Admin

Painel administrativo web do SignallQ para acompanhar uso do app, diagnósticos, IA/custos, erros, versões, integrações e métricas de produto.

Este projeto é um frontend React + TypeScript + Vite. Ele roda com mocks por padrão, mas está estruturado para conexão futura com a SignallQ Admin API / Cloudflare Worker.

## Premissas

- O frontend não chama Firebase Admin, Google Play Console, App Store Connect, Gemini, OpenAI ou Workers AI diretamente.
- Integrações sensíveis devem passar pela futura Admin API.
- Dados mockados ficam centralizados em `src/mocks` e `src/services`.
- Contratos futuros ficam em `docs/openapi`.
- A App Store está preparada para uso futuro, mas desativada no momento.
- Anúncios e monetização estão planejados/desativados por padrão.

## Rodando localmente

```bash
npm install
npm run dev
```

## Validação

```bash
npm run lint
npm run build
```

## Variáveis de ambiente

Copie `.env.example` para `.env.local` quando quiser configurar ambiente local.

```txt
VITE_ADMIN_API_BASE_URL=
VITE_ENABLE_MOCKS=true
VITE_APP_ENV=production
```

Com `VITE_ENABLE_MOCKS=true`, os services retornam mocks realistas. Para API real, configure `VITE_ADMIN_API_BASE_URL` e desative mocks.

## Estrutura principal

```txt
src/
  components/       Componentes reutilizáveis
  config/           Navegação e constantes
  features/         Páginas/domínios do painel
  integrations/     Adapters mockados para Firebase, Google Play e App Store futura
  mocks/            Dados mockados centralizados
  services/         Camada de acesso a dados
  types/            Tipos TypeScript compartilhados

docs/
  openapi/          Swagger/OpenAPI da futura Admin API
  privacy/          Políticas de dados e privacidade
```

## Próximos passos recomendados

1. Adicionar code splitting por rota para reduzir o bundle inicial.
2. Implementar as páginas de governança: Experimentos, Privacidade, Qualidade dos Dados, Worker, Abuso, Premium e Exportações.
3. Conectar os services à Admin API quando o Worker estiver pronto.
4. Adicionar autenticação admin.
5. Adicionar testes unitários para services e componentes críticos.
