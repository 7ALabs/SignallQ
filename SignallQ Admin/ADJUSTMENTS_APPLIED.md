# Ajustes aplicados nesta versão

- Renomeado `package.json` de `react-example` para `signallq-admin`.
- Removida dependência `@google/genai` do frontend.
- `apiClient.ts` agora usa `VITE_ADMIN_API_BASE_URL`, `VITE_APP_ENV` e `VITE_ENABLE_MOCKS`.
- Removidas URLs hardcoded de produção/staging no `apiClient.ts`.
- Adicionado `src/vite-env.d.ts` para tipagem de `import.meta.env`.
- Removidas referências indevidas a Postgres como decisão arquitetural assumida.
- Removidas referências obrigatórias a Slack; substituídas por webhooks operacionais genéricos.
- Corrigidos textos estranhos como “Banco Telegráfico” e “Demosntrativos”.
- Atualizados README e README_ADMIN_ARCHITECTURE com documentação real do projeto.
- `npm run lint` validado com sucesso.
- `npm run build` validado com sucesso.

Observação: o bundle JS ainda passa de 500 KB. Próximo ajuste recomendado: code splitting por rota.
