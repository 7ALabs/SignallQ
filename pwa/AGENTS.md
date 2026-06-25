# AGENTS.md — SignallQ PWA

## Escopo deste arquivo

Estas instruções se aplicam somente ao diretório `pwa/` e seus filhos.

O SignallQ PWA vive no mesmo repositório do Android, mas deve ser tratado como área de trabalho separada.

- Repositório GitHub: `gmmattey/linka-android`
- Caminho GitHub: `pwa/`
- Caminho local esperado: `C:\Projetos\SignallQ\pwa`
- Executor principal: Codex local
- Responsável técnico: Renan

## Squad Farol no Codex

Custom agents do Codex ficam em:

- `pwa/.codex/agents/renan.toml`
- `pwa/.codex/agents/eitam.toml`
- `pwa/.codex/agents/henrique.toml`

Skills repo-scoped ficam em:

- `pwa/.agents/skills/regras-pwa/SKILL.md`
- `pwa/.agents/skills/padroes-react/SKILL.md`
- `pwa/.agents/skills/signallq-design/SKILL.md`
- `pwa/.agents/skills/checar-release/SKILL.md`
- `pwa/.agents/skills/paridade-plataformas/SKILL.md`

Use subagents apenas sob demanda. Subagents ajudam em tarefas paralelizáveis, mas consomem mais tokens. Com orçamento apertado, o padrão é execução por agente principal e acionamento explícito dos apoios.

## Regra principal

Trabalhe somente dentro de `pwa/`, salvo autorização explícita do Luiz/Renan.

Não altere código Android, Gradle, Kotlin, manifests Android, recursos Android, CI/CD global ou arquivos compartilhados do repositório sem autorização explícita.

Se uma tarefa exigir mudança fora de `pwa/`, pare e peça validação antes de continuar.

## Contexto do produto

O SignallQ PWA é a versão web instalável do SignallQ.

Objetivo do produto:

- medir a qualidade da conexão no navegador;
- explicar se a internet está boa, ruim ou instável;
- separar velocidade de estabilidade;
- gerar diagnóstico curto, simples e acionável;
- respeitar limitações reais do navegador;
- manter custo baixo como regra padrão.

O PWA não deve tentar copiar capacidades nativas do Android quando o browser não permite.

## Stack alvo

- React
- TypeScript
- Vite
- CSS simples/modular ou abordagem definida no projeto
- PWA Manifest
- Service Worker quando fizer sentido
- IndexedDB para histórico local quando necessário
- Cloudflare Pages para deploy
- Cloudflare Workers para integrações quando necessário

## Limitações de navegador

Nunca invente métricas.

O PWA pode medir ou inferir apenas o que o navegador permitir.

Permitido/viável:

- download via HTTP;
- upload via HTTP quando houver endpoint adequado;
- latência aproximada via `fetch`/timing;
- jitter aproximado, se houver amostras suficientes;
- histórico local com IndexedDB/localStorage;
- estado online/offline;
- tipo de conexão via Network Information API quando disponível;
- informações básicas de navegador/dispositivo sem invasão.

Não prometer no PWA:

- scan de Wi-Fi;
- RSSI, RSRP, RSRQ, SINR;
- MAC address;
- ARP scan;
- dispositivos conectados na rede;
- Cell tower ID;
- ICMP ping real;
- DNS custom nativo por request;
- logs de sistema;
- foreground service persistente.

Quando algo não puder ser medido, a UI e o diagnóstico devem dizer que não foi possível medir, sem preencher valor falso.

## Regras de diagnóstico

- Diagnóstico deve ser curto, objetivo e acionável.
- Não usar chat livre como interface principal.
- Separar velocidade de estabilidade.
- Explicar limitação técnica em linguagem simples.
- Não gerar tese longa para usuário comum.
- Não prometer solução que o app não consegue validar.
- Não enviar dados sensíveis sem necessidade.

## Regras de UI/UX

- Mobile-first.
- Material Design 3 como direção visual.
- Usar identidade do SignallQ.
- Resultado principal precisa ser entendido rapidamente.
- Evitar jargão técnico sem explicação.
- Estados obrigatórios quando aplicável: carregando, erro, vazio e sucesso.
- Cards úteis, sem excesso visual.
- CTA principal sempre claro.

## Regras de código

- Preferir componentes pequenos e coesos.
- Usar TypeScript com tipos explícitos onde fizer sentido.
- Evitar `any` desnecessário.
- Separar lógica reutilizável em hooks ou módulos.
- Evitar lógica pesada diretamente no JSX.
- Não adicionar dependência de produção sem justificar.
- Não fazer refatoração cosmética fora do escopo da tarefa.
- Não misturar feature com reorganização estrutural grande.
- Manter mudanças pequenas e revisáveis.

## Fluxo Git obrigatório

Não trabalhar direto na `main`.

Branches do Codex para PWA devem começar com `codex` e seguir, quando possível:

`codex/pwa/<sig-id>-<descricao-curta>`

Se a ferramenta bloquear `/` no nome da branch, usar:

`codex-pwa-<sig-id>-<descricao-curta>`

Exemplos:

- `codex/pwa/sig-39-setup-vite`
- `codex/pwa/sig-43-speedtest-web`
- `codex-pwa-sig-39-setup-vite`

PRs abertos pelo fluxo Codex também devem começar com `Codex PWA —`.

Cada PR deve ser pequeno e focado.

Não misturar alterações PWA e Android no mesmo PR.

## Antes de começar uma tarefa local

Verifique o estado do repositório:

```bash
git status
```

Atualize a base quando apropriado:

```bash
git checkout main
git pull origin main
```

Crie uma branch específica para a tarefa:

```bash
git checkout -b codex/pwa/<sig-id>-<descricao-curta>
```

Se a branch com `/` não for aceita pela ferramenta usada, use a variação com hífen.

## Antes de alterar muitos arquivos

Se a tarefa envolver mais de 5 arquivos, arquitetura nova, Worker, persistência local, contrato de diagnóstico ou fluxo principal de UI, primeiro apresente um plano curto.

O plano deve listar:

- objetivo;
- arquivos prováveis;
- passos;
- riscos;
- validação.

Depois implemente por etapas.

## Validação obrigatória

Ao final de cada tarefa, rode o que existir no projeto:

```bash
npm run lint
npm run typecheck
npm run build
npm test
```

Se algum comando não existir ainda, informe claramente como pendência. Não finja validação.

## Resumo final obrigatório

Ao finalizar uma tarefa, informe:

- arquivos alterados;
- comandos executados;
- resultado dos comandos;
- pendências;
- riscos;
- se tocou ou não em arquivos fora de `pwa/`.

## Quando parar e pedir orientação

Pare antes de continuar se:

- precisar alterar Android;
- precisar alterar CI/CD global;
- precisar mudar contrato compartilhado com Android;
- precisar adicionar dependência pesada;
- o requisito estiver ambíguo;
- uma limitação de navegador impedir a feature como descrita;
- houver conflito Git fora de `pwa/`.

## Regra final

Codex local é executor.

Renan é responsável técnico.

Eitam prepara tarefas.

Henrique valida UX/QA.

Luiz decide prioridade e escopo.

O PWA deve evoluir rápido, mas sem quebrar Android, sem inventar métrica e sem transformar o repositório compartilhado em bagunça.