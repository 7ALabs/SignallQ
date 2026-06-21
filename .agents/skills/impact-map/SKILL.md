---
description: Identifica quais módulos, arquivos e contratos são afetados por uma mudança proposta. Executada por Marcelo antes do planejamento de qualquer feature média ou grande.
---

## Quando usar
Antes de aprovar qualquer feature ou refactor que toca mais de 2 arquivos. Sempre antes da Claudete criar o task breakdown.

## Passos
1. **Receber descrição da mudança** proposta.
2. **Identificar o epicentro**: qual arquivo/módulo muda primeiro.
3. **Traçar dependências diretas**: quem usa o que vai mudar.
4. **Verificar dependências inversas**: quem depende do que vai mudar.
5. **Listar arquivos de teste** que precisariam ser atualizados.
6. **Verificar documentação** afetada.
7. **Classificar impacto**: Isolado / Médio / Amplo.

## Output esperado
```
Impact map — [mudança]:
Epicentro: [arquivo principal]
Afetados diretos: [lista]
Afetados indiretos: [lista]
Testes afetados: [lista]
Docs afetadas: [lista]
Impacto: Isolado | Médio | Amplo
Risco de regressão: Baixo | Médio | Alto
```

## Limites
- Impacto "Amplo" deve acionar alerta para Claudete antes de prosseguir.
- Não executar mudança durante impact map — apenas mapear.
