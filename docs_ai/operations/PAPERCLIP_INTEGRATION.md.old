# Integração Paperclip — Guia Operacional

**Data**: 2026-05-13  
**Status**: ✅ Configurado  
**Responsável**: Linka Tech Infrastructure

---

## 📋 Resumo

Paperclip é a plataforma de orquestração de agents para o projeto Linka Android Kotlin. Permite que agents IA (Claudete, Cláudio, Camilo, Caio, Gema) se coordenem automaticamente.

**Localização**: http://127.0.0.1:3100  
**Database**: PostgreSQL Embedded (porta 54329)  
**Storage**: Local Disk  

---

## 🚀 Quick Start

### 1. Validar Ambiente

```powershell
.\scripts\check-env.ps1
```

Deve retornar:
```
✓ Paperclip respondendo em http://127.0.0.1:3100
✓ .env.paperclip Encontrado
```

### 2. Verificar Status dos Agents

```powershell
.\scripts\agent-status.ps1
```

Output:
```
👥 AGENTS:

🟢 Claudete (CEO)              Status: idle
🟢 Cláudio (CTO)               Status: running
🟢 Sérgio Camilo (Engineer)    Status: running
🟢 Caio (Engineer)             Status: idle
🟢 Gema (Documentation)        Status: idle
```

### 3. Acordar um Agent

```powershell
.\scripts\agent-wake.ps1 -AgentName claudio
```

### 4. Fazer Handoff Entre Agents

```powershell
.\scripts\agent-delegate.ps1 `
    -FromAgent claudio `
    -ToAgent camilo `
    -TaskId "LINKA-123" `
    -Summary "Implementar autenticação com OAuth"
```

### 5. Acessar UI do Paperclip

```
http://127.0.0.1:3100
```

---

## 🔧 Configuração

### .env.paperclip

Localização: `.env.paperclip` (raiz do projeto)

**Variáveis Críticas:**

```env
PAPERCLIP_HOST=127.0.0.1
PAPERCLIP_PORT=3100
PAPERCLIP_BASE_URL=http://127.0.0.1:3100

# Agent IDs (obtidos de Paperclip UI)
PAPERCLIP_AGENT_CLAUDETE=590a8d14-69ee-483f-9...
PAPERCLIP_AGENT_CLAUDIO=7ecaa5dd-19f0-4e01-8...
PAPERCLIP_AGENT_CAMILO=8df992cf-e10e-46c1-b...
PAPERCLIP_AGENT_CAIO=973bcd26-43b7-493f-9...
PAPERCLIP_AGENT_GEMA=e0673a6c-fc9c-409f-b...

# Modelos
PAPERCLIP_MODEL_OPUS=claude-opus-4-7
PAPERCLIP_MODEL_SONNET=claude-sonnet-4-6
PAPERCLIP_MODEL_HAIKU=claude-haiku-4-5
```

**Obtendo IDs de Agents:**

1. Acesse http://127.0.0.1:3100
2. Navegue até "Agents"
3. Clique em cada agent para ver seu ID
4. Copie e atualize `.env.paperclip`

---

## 👥 Agents

### Claudete — CEO / Product Manager

**Responsabilidade**: Definição de features, priorização, decisões estratégicas  
**Modelo**: Claude Sonnet 4.6  
**Status**: Idle (acordado on-demand)

**Acordar:**
```powershell
.\scripts\agent-wake.ps1 -AgentName claudete
```

### Cláudio — CTO / Tech Lead

**Responsabilidade**: Arquitetura técnica, revisão de PRs, delegação  
**Modelo**: Claude Sonnet 4.6  
**Status**: Running (sempre acordado)

### Sérgio Camilo — Senior Engineer

**Responsabilidade**: Implementação de features complexas, refactors críticos  
**Modelo**: Claude Sonnet 4.6  
**Status**: Running

### Caio — Engineer (Codex/GPT)

**Responsabilidade**: Implementação com foco em código  
**Modelo**: GPT-5.4 (via codex_local adapter)  
**Status**: Idle

### Gema — Documentation & Release

**Responsabilidade**: Documentação, release notes, versionamento  
**Modelo**: Claude Sonnet 4.6  
**Status**: Idle  
**Nota**: Corrigido de `gemini_local` para `claude_local` (2026-05-13)

---

## 🔄 Fluxo de Delegação

### Padrão Básico

```
1. Claudete (definição-feature)
    ↓ delegação
2. Cláudio (quebra em microtasks)
    ↓ delegação
3. Camilo/Caio (implementação)
    ↓ delegação
4. Gema (documentação)
```

### Script de Delegação

```powershell
.\scripts\agent-delegate.ps1 `
    -FromAgent claudete `
    -ToAgent claudio `
    -TaskId "LINKA-001" `
    -Summary "Definir feature de autenticação com OAuth 2.0" `
    -Blocker $null
```

**Resultado esperado:**
```
✓ Delegação enviada com sucesso!
Agent acordado: claudio
Task: LINKA-001
```

### Monitorar Progresso

```powershell
.\scripts\agent-status.ps1 -Agent claudio
```

---

## 📊 Status & Monitoramento

### Verificar Saúde Geral

```powershell
.\scripts\agent-status.ps1
```

Interprete os status:
- 🟢 **running**: Agent ativo, processando ou pronto
- 🔵 **idle**: Agent dormindo, acordável
- 🔴 **error**: Agent com problema (verifique Paperclip UI)

### Verificar Agent Específico

```powershell
.\scripts\agent-status.ps1 -Agent gema
```

### Logs

**Localização dos logs:**
```
C:\Users\luizg\.paperclip\instances\default\logs\
```

**Ver logs do Paperclip:**
```powershell
Get-Content -Path "$env:USERPROFILE\.paperclip\instances\default\logs\*.log" -Tail 50 -Follow
```

---

## 🔌 API Reference

### Endpoints

```
GET  http://127.0.0.1:3100/api/health
     → Verifica saúde do servidor

GET  http://127.0.0.1:3100/api/agents
     → Lista todos os agents

GET  http://127.0.0.1:3100/api/agents/{agentId}
     → Detalhes de um agent

POST http://127.0.0.1:3100/api/agents/{agentId}/wake
     → Acorda um agent

POST http://127.0.0.1:3100/api/agents/{agentId}/delegate
     → Faz handoff de task
```

### Exemplos com cURL

```bash
# Status
curl http://127.0.0.1:3100/api/health

# Listar agents
curl http://127.0.0.1:3100/api/agents

# Acordar agent
curl -X POST http://127.0.0.1:3100/api/agents/{agentId}/wake \
  -H "Content-Type: application/json" \
  -d '{"timestamp":"2026-05-13T10:00:00Z"}'
```

---

## ⚠️ Troubleshooting

### Paperclip não responde

**Problema:**
```
✗ Paperclip não está respondendo em http://127.0.0.1:3100
```

**Solução:**

1. Verifique se Paperclip está rodando:
   ```powershell
   netstat -ano | findstr 3100
   ```

2. Se não está rodando, inicie com Paperclip CLI:
   ```powershell
   paperclip start
   # ou
   paperclip serve
   ```

3. Espere 10-15 segundos para que o servidor inicialize

4. Verifique a URL:
   ```
   http://127.0.0.1:3100
   ```

### Agent em erro

**Problema:**
```
🔴 Gema: Status error
```

**Solução:**

1. Acesse Paperclip UI: http://127.0.0.1:3100
2. Localize o agent em erro (ex: Gema)
3. Verifique a mensagem de erro
4. Ajuste configuração (ex: alterar adapter)
5. Teste com: `.\scripts\agent-status.ps1`

### .env.paperclip não encontrado

**Problema:**
```
⚠ .env.paperclip não encontrado
```

**Solução:**

1. Copie o modelo:
   ```powershell
   Copy-Item ".env.paperclip.example" ".env.paperclip"
   ```

2. Ou crie um novo:
   - Acesse `.env.paperclip` na raiz do projeto
   - Preenchase com valores do Paperclip UI

### Agent ID inválido

**Problema:**
```
✗ Agent não encontrado em .env.paperclip
```

**Solução:**

1. Obtenha IDs corretos de Paperclip UI
2. Atualize `.env.paperclip`
3. Re-execute o script

---

## 📚 Integração com Build Pipeline

### Check-env.ps1 validação

O script `check-env.ps1` agora valida:

```powershell
# Validação automática
.\scripts\check-env.ps1

# Output inclui:
# ✓ Paperclip respondendo
# ✓ .env.paperclip Encontrado
# ✓ Agents conectados: 5
```

### Build com Agents

Quando executar builds:

```powershell
# 1. Validar ambiente
.\scripts\check-env.ps1

# 2. Acordar agents necessários
.\scripts\agent-wake.ps1 -AgentName claudio

# 3. Executar build
.\scripts\build-apk-debug.ps1

# 4. Fazer code review automático (delegação para Cláudio)
.\scripts\agent-delegate.ps1 -FromAgent "build-system" -ToAgent claudio -TaskId "APK-BUILD" -Summary "Review do APK gerado"
```

---

## 🔐 Segurança

### Credenciais

- ✅ `.env.paperclip` não é versionado (.gitignore)
- ✅ API Keys armazenadas de forma encriptada no Paperclip
- ✅ Conexão local (127.0.0.1), sem exposição externa por padrão
- ✅ Masterkey criptografado em: `C:\Users\luizg\.paperclip\instances\default\secrets\master.key`

### Backups

Paperclip faz backup automático do database:

```
C:\Users\luizg\.paperclip\instances\default\data\backups\
  • A cada 60 minutos
  • Retenção de 30 dias
```

Para restaurar manualmente:
```powershell
# (Documentação Paperclip CLI)
paperclip restore --backup <timestamp>
```

---

## 📞 Suporte

### Verificar versão Paperclip

```powershell
paperclip --version
```

### Documentação oficial

```
https://github.com/paperclip-io/paperclip
```

### Logs para debug

```powershell
# Tail dos últimos logs
Get-Content -Path "$env:USERPROFILE\.paperclip\instances\default\logs\*" -Tail 100
```

---

## ✅ Checklist de Operação

```
Antes de usar Paperclip:
  [ ] Paperclip rodando (http://127.0.0.1:3100)
  [ ] .env.paperclip criado e preenchido
  [ ] Agent IDs válidos em .env.paperclip
  [ ] ./scripts/agent-status.ps1 retorna ✓

Durante operação:
  [ ] Monitorar status regularmente
  [ ] Logs em: C:\Users\luizg\.paperclip\instances\default\logs\
  [ ] Delegações documentadas
  [ ] Fallbacks automáticos funcionando

Manutenção:
  [ ] Backups rodando automaticamente
  [ ] Nenhum agent em estado "error" persistente
  [ ] Database performance OK
```

---

**Última Atualização**: 2026-05-13  
**Próxima Revisão Recomendada**: 2026-05-20  
**Mantido por**: Tech Infrastructure
