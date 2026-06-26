# Design System Operacional

## Princípio visual

O SignallQ PWA deve parecer um produto técnico confiável, limpo e simples de usar.

Não deve parecer painel velho de roteador.

Não deve parecer dashboard cheio de card inútil.

## Direção

- Material Design 3 como referência.
- Mobile-first.
- Interface clara e leve.
- Resultado principal entendido em menos de 10 segundos.
- Pouca ornamentação.
- Componentes simples.
- Copy curta em PT-BR.

## Tokens iniciais

### Cores

```css
:root {
  --sq-color-primary: #6c2bff;
  --sq-color-primary-contrast: #ffffff;
  --sq-color-background: #f7f7fb;
  --sq-color-surface: #ffffff;
  --sq-color-surface-muted: #f0f0f6;
  --sq-color-text-primary: #17151f;
  --sq-color-text-secondary: #5f5a6b;
  --sq-color-border: #dedbe8;
  --sq-color-success: #168a4a;
  --sq-color-warning: #b7791f;
  --sq-color-danger: #c2413b;
  --sq-color-info: #2563eb;
}
```

### Espaçamento

```css
:root {
  --sq-space-1: 4px;
  --sq-space-2: 8px;
  --sq-space-3: 12px;
  --sq-space-4: 16px;
  --sq-space-5: 24px;
  --sq-space-6: 32px;
}
```

### Radius

```css
:root {
  --sq-radius-sm: 8px;
  --sq-radius-md: 12px;
  --sq-radius-lg: 16px;
  --sq-radius-xl: 24px;
}
```

### Tipografia

```css
:root {
  --sq-font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  --sq-font-size-xs: 12px;
  --sq-font-size-sm: 14px;
  --sq-font-size-md: 16px;
  --sq-font-size-lg: 20px;
  --sq-font-size-xl: 28px;
}
```

## Componentes mínimos

### App Shell

Responsável por:

- largura máxima;
- background;
- área principal;
- navegação base quando existir.

### Botão primário

Uso:

- iniciar teste;
- refazer teste;
- ação principal da tela.

Regra:

- só uma ação primária forte por tela.

### Card de métrica

Deve conter:

- nome da métrica;
- valor;
- unidade;
- status visual;
- texto curto quando houver limitação.

### Badge de status

Estados:

- bom;
- atenção;
- ruim;
- desconhecido.

### Bloco de recomendação

Deve conter:

- título curto;
- explicação simples;
- prioridade;
- categoria.

## Estados obrigatórios

Toda feature visual relevante deve ter:

- carregando;
- erro;
- vazio;
- sucesso.

## Copy

Regras:

- usar “você”;
- frases curtas;
- explicar termos técnicos;
- sem emoji na interface;
- sem tom alarmista;
- sem promessa absoluta.

Bom:

“Sua internet está rápida, mas oscilou durante o teste.”

Ruim:

“Sua conexão apresenta degradação estatística severa com instabilidade na malha.”

## Regras anti-carnaval

Não criar card se ele não responder uma pergunta real do usuário.

Perguntas que a tela deve responder:

- minha internet está boa?
- está lenta ou instável?
- isso afeta o quê?
- o que faço agora?

## Critérios de aceite visual

- Funciona bem em 360px de largura.
- CTA principal aparece sem rolagem excessiva.
- Resultado geral é o item mais evidente.
- Métricas técnicas não competem com o diagnóstico.
- Ausência de métrica é comunicada claramente.
- A tela não depende de texto longo para ser entendida.
