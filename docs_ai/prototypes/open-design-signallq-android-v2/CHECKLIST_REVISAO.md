---
title: "Checklist de revisão — protótipo SignallQ Android 2.0"
description: "Critérios para aceitar ou devolver a primeira rodada gerada no Open Design"
type: "funcional"
status: "draft"
owner: "Caio"
last_updated: "2026-08-15"
version: "draft"
---

# Checklist de revisão

Classifique cada item como **atende**, **ajustar** ou **não se aplica**. Uma rodada só pode ser
considerada pronta para decisão quando não houver item crítico em “ajustar”.

## Jornada

- [ ] A entrada principal começa pelo sintoma, não pela escolha de uma ferramenta.
- [ ] O fluxo segue contexto → análise → resultado → ação → confirmação.
- [ ] A pessoa entende o próximo passo sem explorar o app inteiro.
- [ ] Histórico e Mais não competem com a tarefa principal.
- [ ] O protótipo não inventa recursos fora do escopo.

## Resultado e orientação

- [ ] O veredito humano aparece antes das métricas.
- [ ] A causa é apresentada como fato, indício ou hipótese de forma honesta.
- [ ] Instabilidade sob carga é explicada sem depender de “bufferbloat”.
- [ ] As evidências são poucas e relevantes.
- [ ] Existe uma ação concreta e uma nova verificação.
- [ ] A comparação antes/depois comunica melhora ou ausência de melhora.

## Identidade

- [ ] A interface parece Android/Material 3, não um site responsivo dentro de um telefone.
- [ ] Há bastante respiro e baixa densidade.
- [ ] O violeta está restrito a elementos importantes.
- [ ] Cards e pills têm função clara.
- [ ] Seções comuns são abertas e cards não usam contorno perimetral decorativo.
- [ ] Cards necessários usam `#F7F7F8` no claro e `#161616` no escuro, com profundidade tonal.
- [ ] O azul não aparece na UI fora do gradiente original do símbolo oficial.
- [ ] Google Sans Flex foi carregada localmente nos pesos 400, 500, 600 e 700.
- [ ] Não há gradiente, neon, glassmorphism, glow decorativo ou estética de hacker.
- [ ] Os assets oficiais não foram alterados.

## Conteúdo

- [ ] O português é simples, direto e brasileiro.
- [ ] A voz não parece chatbot ou texto gerado por IA.
- [ ] Títulos e ações são curtos.
- [ ] Todo jargão ou valor técnico tem tradução próxima.
- [ ] Não há promessas absolutas sem evidência.

## Temas e movimento

- [ ] O tema claro mantém superfícies neutras.
- [ ] O fundo principal do tema escuro é `#000000`.
- [ ] O tema escuro não usa roxo como grande superfície.
- [ ] As transições comunicam continuidade e estado.
- [ ] Movimento reduzido é respeitado.

## Acessibilidade

- [ ] Texto e controles atendem contraste WCAG AA.
- [ ] Alvos interativos têm no mínimo 48×48px.
- [ ] Foco por teclado é visível e segue ordem lógica.
- [ ] A interface suporta fonte ampliada.
- [ ] Status não depende apenas de cor.
- [ ] Gráficos têm resumo textual.

## Integridade do protótipo

- [ ] Todos os caminhos principais são clicáveis.
- [ ] Voltar, cancelar e tentar novamente funcionam.
- [ ] Existem estados de análise, evidência insuficiente e erro recuperável.
- [ ] Não há links mortos, conteúdo de placeholder ou texto em outro idioma.
- [ ] A versão gerada está identificada e pode ser comparada com a próxima rodada.

## Critério de aceite

O protótipo pode seguir para decisão do Luiz quando a jornada completa funciona, o posicionamento
fica evidente sem explicação externa e todos os itens de resultado, identidade, conteúdo e
acessibilidade estão em **atende** ou têm exceção documentada.
