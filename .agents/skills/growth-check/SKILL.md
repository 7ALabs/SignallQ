---
name: growth-check
description: Checklist de Play Store, ASO, copy externa e superfície pública para mudanças do SignallQ.
argument-hint: "[contexto]"
allowed-tools: Read, Bash(grep *)
---

# Growth Check

Use quando uma entrega afeta Play Store, screenshots, descrição pública, landing, campanha, notificação promocional ou outra superfície vista antes de usar o app.

A decisão de posicionamento pertence a produto; esta skill evita esquecer efeitos colaterais de publicação.

## Checklist

Para cada item, responda `OK`, `N/A` com motivo ou `PENDENTE`:

1. Título/nome exibido continua correto?
2. Screenshots da loja ainda representam a UI atual?
3. Descrição pública promete apenas capacidades existentes?
4. Termos/keywords relevantes precisam ser atualizados?
5. Ícone/wordmark usam a fonte canônica de marca?
6. Nota de versão fala do efeito percebido pelo usuário, não de refactor interno?
7. Política de privacidade precisa mudar por nova coleta, permissão, retenção ou finalidade?
8. Monetização/ads continuam descritos corretamente?
9. Há mudança que exige decisão do Luiz por marca, custo ou modelo comercial?

Quando a mudança introduzir telemetria, use `analytics-spec`. Quando fizer parte de release, complemente com `checar-release`.

## Saída

```text
GROWTH-CHECK: OK | PENDENTE
Store: ...
Screenshots: ...
Copy: ...
Marca: ...
Privacidade: ...
Monetização: ...
Pendências:
- ...
```

A skill não escreve campanha, não publica loja e não decide estratégia de marca sozinha.
