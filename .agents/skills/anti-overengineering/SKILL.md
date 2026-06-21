---
name: anti-overengineering
description: Detecta abstrações desnecessárias, camadas extras, genericismo exagerado e complexidade sem ganho real no Linka. Use antes de aprovar refactors, novas arquiteturas ou adição de dependências.
---

Avalie a implementação ou proposta abaixo contra overengineering:

$ARGUMENTS

Use **Cláudio** para avaliar decisão arquitetural e **Gema** para revisão de código.

---

## Sinais vermelhos — questionar sempre

- Interface com uma única implementação presente e futura improvável
- Abstract class com um único filho concreto
- Repository que só repassa chamada sem lógica própria
- UseCase que não faz nada além de chamar `repository.method()`
- Factory para criar um único tipo de objeto
- Module de injeção com um único binding
- Enum/sealed class com um único valor real
- Genérico onde tipo concreto seria mais claro
- "Vamos abstrair para o futuro" sem caso de uso concreto hoje
- Wrapper de wrapper de wrapper sem transformação real
- Manager/Helper/Util com responsabilidade indefinida

## Sinais amarelos — avaliar contexto

- Nova camada adicionada sem especificar qual problema resolve
- Dependência nova que substitui código simples nativo
- Padrão de design aplicado onde if/else resolveria
- Módulo novo para funcionalidade que cabia no módulo existente
- Coroutine com dispatcher customizado sem motivo técnico

---

## Perguntas obrigatórias por abstração

Para cada abstração proposta ou encontrada, responda:

1. **Qual problema concreto ela resolve hoje** (não "no futuro")?
2. **Existe implementação mais simples** que resolve o mesmo problema?
3. **Quantas linhas de código adiciona vs. remove?**
4. **Adiciona teste ou dificulta teste?**
5. **Quem mais vai usar isso além do caller atual?**

Se a resposta à pergunta 5 for "ninguém", a abstração provavelmente não é necessária.

---

## Critério de aprovação

| Resultado | Critério |
|---|---|
| Manter | Abstração resolve problema real hoje, tem pelo menos 2 callers ou é contrato de módulo |
| Simplificar | Abstração útil mas pode ser reduzida (ex: interface → classe concreta direta) |
| Remover | Abstração sem caso de uso real, ou que duplica framework existente |

---

## Entregue

1. **Abstrações questionáveis encontradas** — localização e motivo
2. **Custo de complexidade** — linhas adicionadas, camadas criadas, acoplamento gerado
3. **Alternativa simples** — como seria sem a abstração
4. **Veredicto** — manter / simplificar / remover

[PRÓXIMO: Cláudio — revisar decisão arquitetural | Gema — revisar código]
