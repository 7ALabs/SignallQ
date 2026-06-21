---
description: Verifica a paridade entre uma feature Android (Kotlin/Compose) e sua implementação PWA (React/TypeScript). Identifica divergências reais vs. limitações técnicas legítimas do navegador.
---

## Quando usar
Ao implementar uma feature nas duas plataformas. Ao revisar se o PWA está prometendo algo que não consegue entregar.

## Passos
1. Chamar Marcelo para listar arquivos da feature em Android e PWA.
2. Comparar comportamento esperado em cada plataforma.
3. Para cada funcionalidade, classificar:
   - ✅ **Paridade total**: mesmo comportamento nas duas plataformas.
   - ⚠️ **Paridade parcial**: funciona, mas com limitação aceitável. Documentar.
   - ❌ **Impossível no browser**: API nativa sem equivalente web. Documentar com justificativa.
   - 🚫 **PWA prometendo o impossível**: remover ou substituir por alternativa honesta.

## Exemplos de limitações legítimas do browser
- Scan de redes Wi-Fi vizinhas: impossível no browser.
- Leitura de RSSI em tempo real: impossível no browser.
- Foreground service persistente: impossível sem PWA installada + Service Worker.
- Detalhes de cell tower: impossível.

## Output
Tabela por funcionalidade: Feature | Android | PWA | Classificação | Ação.

## Limites
- Não implementa alternativa — apenas classifica e recomenda.
- Alternativa de implementação → Renan.
