---
name: team-status
description: Coleta status de 1 linha de todos os teammates ativos ao mesmo tempo e exibe um painel geral. Use com /loop 10m /team-status para check-in automático a cada 10 minutos sem interromper o trabalho.
---

## O que fazer

Faça um check-in geral com todos os teammates ativos agora.

### Passo 1 — Identifique os teammates ativos
Liste mentalmente todos os teammates que você spawnou nesta sessão e que ainda não encerraram.

### Passo 2 — Peça status a todos em paralelo
Envie SendMessage para TODOS os teammates ao mesmo tempo (não um por vez).
Mensagem a enviar para cada um: `"Status rápido: 1 linha no teu estilo — o que você está fazendo agora? Depois continua o trabalho normalmente."`

### Passo 3 — Compile o painel
Aguarde as respostas e monte no seguinte formato:

```
╔══════════════════════════════════════════════════╗
║  STATUS GERAL — [HH:MM]                          ║
╠══════════════════════════════════════════════════╣
║ Camilo:  [resposta com personalidade dele]        ║
║ Renan:   [resposta com personalidade dele]        ║
║ Gema:    [resposta com personalidade dela]        ║
║ Marcelo: [resposta com personalidade dele]        ║
║ [outros teammates ativos]                        ║
╠══════════════════════════════════════════════════╣
║ Sem resposta: [lista quem não respondeu em 30s]  ║
╚══════════════════════════════════════════════════╝
```

### Regras

- Mande para TODOS ao mesmo tempo — nunca sequencial
- Preserve a personalidade de cada agente na resposta compilada
- Se um teammate não responder em ~30s, marque como "sem resposta — pode ter travado"
- Não interrompa o trabalho deles além do check-in de 1 linha
- Após exibir o painel, volte ao estado normal de monitoramento

### Como usar com loop automático

No início de uma sessão com Agent Team, rode:
```
/loop 10m /team-status
```

Isso dispara o check-in a cada 10 minutos automaticamente.
Para parar o loop, pressione Ctrl+C ou diga "pare o loop".
