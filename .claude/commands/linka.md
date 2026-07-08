---
description: Kickoff de sessão LINKA — orienta Claude sobre o estado atual do projeto (versão, pendências, fase, skills disponíveis). Use no início de qualquer sessão nova.
allowed-tools: Read(*), Bash(*), PowerShell(*)
---

## Estado Atual do Projeto (lido dos arquivos agora)

**Versão Android (ativa):**
!`cat "C:/Projetos/SignallQ/android/gradle/libs.versions.toml" 2>/dev/null | grep -E "versionName|versionCode"`

**Último release (topo do changelog):**
!`cat "C:/Projetos/SignallQ/android/CHANGELOG.md" 2>/dev/null | head -25`

**Milestones do projeto:**
!`grep -A 6 "^## Milestones" "C:/Projetos/SignallQ/.claude/CLAUDE.md" 2>/dev/null`

---

## Sua Tarefa

Com base nas informações acima, apresente ao usuário um briefing de sessão conciso:

1. **Versão atual** — versionName + versionCode
2. **Último release** — data e o que foi entregue (do CHANGELOG)
3. **Milestone atual** — compare a data de hoje com a tabela de milestones e diga em qual estamos
4. **Pendências críticas** — consulte o Linear (projeto SignallQ, backlog/cycle atual) pelas issues de maior prioridade e o GitHub Issues do repo por bugs abertos não triados; liste as 3 mais críticas combinando as duas fontes

Em seguida, pergunte: **"Em que vamos trabalhar hoje?"**

---

## Skills e Comandos Disponíveis (para referência sua — não exibir ao usuário)

| Skill / Comando | Quando usar automaticamente |
|-------|-----------------------------|
| `/linka` | Início de sessão nova sem contexto anterior |
| `/linka-design create` | Criar nova tela ou componente Compose |
| `/linka-design review` | Editar arquivo em `ui/screen/` ou `ui/component/` |
| `/linka-design tokens` | Dúvida sobre cor, espaçamento ou tipografia |
| `/linka-arch create` | Criar módulo, ViewModel, DAO, serviço ou repositório |
| `/linka-arch review` | Revisão arquitetural de arquivo Kotlin |
| `/linka-arch map` | Dúvida sobre onde implementar algo |
| `/linka-docs impact` | Após qualquer mudança de código — sempre |
| `/linka-docs update` | Atualizar doc específico |
| `/linka-docs new` | Criar novo documento oficial |
| `/linka-docs check` | Auditar docs de uma feature |
| `/estimativa-impacto` | Avaliar tamanho/risco/milestone de uma issue antes do breakdown |
| `/checar-release` | Checklist pré-release (Android + Cloudflare Pages) |
| `/validar-release` | Checklist executável de release com validação de versionamento/build |
| `/gerar-docs` | Gerar ou atualizar documentação funcional/técnica/testes |
| `/auditar-ux` | Auditoria de design system e usabilidade |
| `/motor-diagnostico` | Trabalho no engine de diagnóstico, speedtest ou IA |
