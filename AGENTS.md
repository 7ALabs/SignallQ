# SignallQ

## Identidade e estado atual

- **Organização:** `buildea-labs`
- **Finalidade:** aplicativo Android de diagnóstico de conectividade, com backend e Workers de suporte.
- **Classificação:** produto.
- **Estado atual:** monorepo ativo com Android Kotlin/Compose em `android/`, Workers Cloudflare em `integrations/cloudflare/`, scripts e documentação viva em `docs_ai/`.

## Escopo e exclusões

- **Pertence ao repositório:** aplicativo Android SignallQ, módulos Gradle, Workers Cloudflare, contratos e documentação técnica relacionados.
- **Não pertence:** aplicação Buildea Admin (repo `buildea-admin`); site e PWA (repo `signallq-web`); produto Linka (repo `linka` a ser criado — exclusivo ecossistema Apple, ver [ADR-016](docs_ai/decisions/ADR-016-portfolio-buildea.md)); projetos pessoais.
- **Plataformas do SignallQ:** exclusivamente **Android** (este repo) e **Web** (`signallq.com`, repo `signallq-web`). iOS/macOS/desktop/wearable/embedded ficam permanentemente fora. Ver [ADR-016](docs_ai/decisions/ADR-016-portfolio-buildea.md).
- **Descontinuados permanentemente:** SignallQ Pro, SignallQ ISP, Nethal, quaisquer derivados. Módulos `:pro:*` do Gradle e docs `docs_ai/pro-onhold/` serão removidos nas Fases 4a-b do épico [#1623](https://github.com/buildea-labs/signallq/issues/1623). Ver [ADR-016](docs_ai/decisions/ADR-016-portfolio-buildea.md).
- **Modelo comercial:** freemium com propaganda. Núcleo (diagnóstico, IA, monitoramento) gratuito e sustentado por ads. Recursos pagos futuros possíveis sem quebrar a promessa de gratuidade do núcleo.

## Arquitetura comprovada

- **Android:** Kotlin, Jetpack Compose, Material 3, MVVM, StateFlow, Hilt, Room, DataStore e WorkManager.
- **Módulos principais:** `:app`; módulos `:core*`, `:feature*`, `:pro:*` e `:core:featureflags` declarados em `android/settings.gradle.kts`.
- **Workers:** `ai-diagnosis-worker`, `game-latency-probe-worker`, `signallq-admin-worker`, `signallq-diagnostic-worker` e `signallq-privacy-worker`.
- **Integrações:** Firebase Analytics e Crashlytics; a IA de diagnóstico usa `ai-diagnosis-worker`. Contratos e disponibilidade de integrações devem ser confirmados nos arquivos e ambientes aplicáveis.
- **Identificadores técnicos:** preservar `io.signallq.app`; versões e SDKs são definidos em `android/gradle/libs.versions.toml`.

## Comandos essenciais comprovados

- **Instalação:** a validar conforme o ambiente Android.
- **Testes:** `./android/gradlew test`.
- **Lint:** `./android/gradlew ktlintCheck detekt`.
- **Build de debug:** `./android/gradlew assembleDebug`.
- **Validações específicas:** os workflows em `.github/workflows/android-ci.yml` executam testes, Ktlint, Detekt e build debug para alterações Android. Não executar publicação, release ou deploy sem a autorização aplicável.

## Restrições

- **Segurança e privacidade:** não versionar ou expor segredos, credenciais, arquivos de keystore ou dados pessoais; credenciais de integrações permanecem fora do cliente quando exigido.
- **Custos:** novos provedores, uso de IA, Firebase, Cloudflare ou outros custos recorrentes exigem aprovação do Luiz.
- **Compatibilidade:** consultar `android/gradle/libs.versions.toml` e a configuração Gradle antes de alterar versões, SDKs ou identificadores.
- **Publicação:** builds, releases Android, deploys de Workers, produção e mudanças irreversíveis exigem aprovação explícita do Luiz.

## Agentes aplicáveis

- **Líder funcional:** Claudete.
- **Responsável técnico:** Camilo.
- **Design:** Juliana.
- **Growth:** Marcos.
- **Operações e dados:** Gustavo.
- **Revisão independente:** Caio.
- **Fonte organizacional:** os únicos agentes corporativos aplicáveis são os definidos em `../ai-governance/agents/`.
- **Personas legadas:** arquivadas em `docs/archive/ai-governance/legacy-agents/`; não participam da descoberta ou do roteamento ativo.

## Skills locais e espelhos

- `.claude/skills/` é a fonte canônica das skills específicas deste repositório.
- `.agents/skills/` e `.github/skills/` são espelhos gerados e não devem ser editados diretamente.
- `scripts/sync-skills-mirrors.sh` é o processo de sincronização; não o execute sem demanda explícita.

## Critérios locais de conclusão

- O escopo autorizado está atendido, os comandos e validações aplicáveis foram executados com evidência, documentação afetada está atualizada e Caio revisou quando houver código, segurança, produção ou risco relevante.

## Regras operacionais obrigatórias

Valem para qualquer ferramenta e qualquer sessão neste repositório, não apenas para as que carregam
`.claude/` automaticamente. Leia antes de alterar código ou documentação; o diretório é caminho de
armazenamento, não condição de vigência.

- [Higiene e padronização do repositório](.claude/rules/higiene-e-padronizacao-repositorio.md) — idioma, nomes, limites de tamanho, correção oportunista, remoção segura, validação obrigatória e formato da entrega.
- [Documentação viva](.claude/rules/politica-documentacao-viva.md) — metadados, índices, sincronia com o código e o que o `docs-ci` reprova.

As regras comuns a todos os repositórios estão em `../ai-governance/policies/` e têm precedência
sobre instrução local em segurança, autorização e governança.

## Fontes complementares

- `docs_ai/README.md`
- `android/settings.gradle.kts`
- `android/gradle/libs.versions.toml`
- `docs_ai/CONTRATOS/openapi/`
- `.claude/skills/SignallQ-design/`
- `scripts/sync-skills-mirrors.sh`
- `../ai-governance/policies/agent-operating-contract.md`
- `../ai-governance/policies/demand-routing.md`
