---
title: "Prompt inicial — protótipo SignallQ Android 2.0"
description: "Prompt pronto para gerar no Open Design o primeiro fluxo Android reposicionado"
type: "funcional"
status: "draft"
owner: "Claudete"
last_updated: "2026-08-15"
version: "draft"
---

# Prompt inicial

Copie a partir do bloco abaixo para o Open Design:

> Leia integralmente `PRODUCT.md`, `DESIGN.md`,
> `docs_ai/POSICIONAMENTO_PRODUTO.md`,
> `docs_ai/design-system/SIGNALLQ_DESIGN_SYSTEM_2_SPEC.md` e
> `docs_ai/functional/JORNADA_ANDROID_GUIADA_2_SPEC.md`.
>
> Crie um protótipo HTML navegável para o **SignallQ Android 2.0**, em português do Brasil. O
> SignallQ não é um speedtest: é um guia de diagnóstico que entende o sintoma, analisa a rede,
> explica o problema em linguagem comum, recomenda uma ação e confirma se houve melhora.
>
> Prototipe somente este cenário ponta a ponta: a pessoa relata **“Vídeos ou chamadas travam”**. O
> app pergunta apenas o contexto necessário, executa uma análise visualmente clara e encontra
> **instabilidade sob carga**. Explique que a velocidade pode estar boa enquanto a resposta da rede
> oscila. Recomende uma ação simples e permita testar novamente, exibindo comparação antes/depois.
>
> Inclua:
>
> - Home com resumo da conexão, CTA “Analisar minha rede”, sintomas comuns e último resultado;
> - lista de sintomas em linhas abertas;
> - uma pergunta curta de contexto;
> - análise em andamento com etapas compreensíveis;
> - resultado com veredito, causa provável, confiança verbal, evidências essenciais e CTA;
> - orientação passo a passo curta;
> - nova verificação e estado de melhora;
> - comparação antes/depois;
> - navegação inferior com Início, Histórico e Mais;
> - estados básicos de Histórico e Mais apenas para validar a arquitetura;
> - temas claro e escuro, com fundo escuro preto total.
>
> A aparência deve ser de um app nativo de um Google Pixel, usando Material 3, Google Sans Flex e
> Material Symbols Outlined. Trabalhe com bastante respiro, baixa densidade e movimento Android
> funcional. Use o violeta da marca apenas em CTA, foco, seleção e navegação ativa. Cards somente
> quando agruparem conteúdo ou ação independente; pills somente para seleção, filtro ou status
> curto. Prefira listas e seções abertas.
>
> Não use azul como cor ativa da interface. Ele existe apenas dentro do gradiente do símbolo
> oficial e como compatibilidade técnica do sistema antigo. A paleta ativa da UI é violeta,
> neutros e cores semânticas.
>
> Não envolva cards em linhas decorativas. Quando um card for realmente necessário, comunique
> profundidade por superfície tonal e sombra discreta: `#F7F7F8` no tema claro e `#161616` no
> escuro; regiões internas elevadas usam `#EEEEF0` e `#222222`. O fundo-base permanece `#FFFFFF`
> ou `#000000`. Seções comuns ficam abertas, separadas por espaço.
>
> Carregue localmente os arquivos Google Sans Flex do Android nos pesos 400, 500, 600 e 700. Não
> dependa da fonte instalada no sistema nem substitua visualmente por outra família.
>
> Use os assets oficiais em `brand/`. Não redesenhe o símbolo.
>
> Evite: velocímetro hero, mosaico de ferramentas, dashboard técnico, excesso de métricas, excesso
> de roxo, gradientes, glow, glassmorphism, cards aninhados, pills decorativas, estética hacker,
> emojis, chat, avatar de IA e frases como “nossa IA analisou”.
>
> O texto deve soar humano, curto e direto. Não afirme certeza quando houver hipótese. Todo dado
> técnico precisa de tradução próxima e todo problema precisa de um próximo passo.
>
> Entregue HTML, CSS e JavaScript funcionais, com navegação por clique e teclado, foco visível,
> contraste adequado, alvos de toque de no mínimo 48px, suporte a fonte ampliada e opção de reduzir
> movimento. Inclua estados de carregamento, evidência insuficiente e erro recuperável sem expandir
> o escopo.
