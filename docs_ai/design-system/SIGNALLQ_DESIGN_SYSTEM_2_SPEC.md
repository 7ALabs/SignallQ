---
title: "SignallQ Design System 2.0 — Direção canônica"
description: "Especificação de identidade, experiência, conteúdo e fundamentos visuais compartilhados entre Android e Web/PWA."
type: "funcional"
status: "draft"
owner: "Claudete"
last_updated: "2026-08-15"
version: "2.0.0-draft.6"
---

# SignallQ Design System 2.0

## 0. Brief e recorte da primeira versão

Esta especificação parte do brief confirmado em 2026-08-15:

- **cobertura:** UI de produto;
- **ponto de partida:** evolução da marca SignallQ existente;
- **profundidade inicial:** foundations e componentes centrais.

Esta primeira versão não inclui templates completos de tela, biblioteca de gráficos, componentes
de campanhas, documentação de implementação Web, redesign integral das jornadas atuais nem
migração do código Android. Esses itens dependem da validação dos fundamentos e dos componentes
abaixo.

## 1. Status e relação com o sistema atual

Este documento formaliza a direção aprovada para a próxima evolução do SignallQ em Android e
Web/PWA. Enquanto a migração não for concluída:

- [`../DESIGN_SYSTEM.md`](../DESIGN_SYSTEM.md) descreve o design system **implementado atualmente
  no Android**;
- este documento define o **alvo canônico** para produto, design, conteúdo e futuras mudanças;
- divergências entre os dois devem ser tratadas como trabalho de migração, não corrigidas de forma
  oportunista em telas sem escopo;
- nenhuma tela deve alegar capacidade ainda não implementada.

O sistema 2.0 compartilha fundamentos de marca, experiência e linguagem, mas possui implementações
próprias para Android e Web. Não se pretende compartilhar código de interface entre plataformas.

## 2. Norte da experiência

O SignallQ deve transmitir três atributos:

1. **Leveza:** telas arejadas, baixa densidade aparente e ausência de decoração competitiva.
2. **Confiança:** conclusões honestas, hierarquia previsível, privacidade explícita e sem falsas
   certezas.
3. **Profissionalismo:** acabamento consistente, componentes nativos, linguagem precisa e
   comportamento estável.

O Android deve parecer um aplicativo nativo criado para o ecossistema Google: Material Design 3,
Jetpack Compose, padrões familiares do Android, Material Symbols e movimento funcional.

O produto não deve parecer:

- dashboard técnico;
- coleção de ferramentas desconectadas;
- chatbot ou demonstração de IA;
- interface preenchida por cards, pills e cores de marca;
- aplicativo que exige exploração aleatória para revelar seu valor.

## 3. Princípios canônicos

### 3.1 A conclusão vem antes da métrica

A experiência padrão apresenta, nesta ordem:

1. o que foi observado;
2. o que isso significa para a pessoa;
3. a causa provável e o nível de confiança;
4. a ação recomendada;
5. os detalhes técnicos, sob demanda.

Métricas cruas permanecem acessíveis, mas não devem exigir interpretação para que o usuário entenda
o resultado.

### 3.2 Uma pergunta principal por tela

Cada tela deve ter um objetivo dominante e uma ação principal inequívoca. Conteúdo secundário não
compete visualmente com a próxima etapa da jornada.

### 3.3 Espaço comunica hierarquia

Espaçamento deve separar etapas, assuntos e níveis de importância. Evitar preencher áreas vazias
com cards ou texto apenas para aumentar densidade visual.

### 3.4 O produto conduz; o usuário não caça ferramentas

As capacidades técnicas devem ser organizadas ao redor de sintomas e objetivos. O produto escolhe,
combina ou recomenda ferramentas como Ping, DNS, Sinal, Dispositivos e Speed Test conforme o
contexto.

O hub de ferramentas pode permanecer como acesso avançado, mas não deve ser a única forma de
descobrir o que o SignallQ faz.

### 3.5 Complexidade progressiva

A camada principal é simples e visual. Explicações, métricas, metodologia e controles avançados
aparecem progressivamente quando forem úteis ou solicitados.

### 3.6 Honestidade diagnóstica

Separar visualmente e na linguagem:

- medição confirmada;
- indício;
- causa provável;
- resultado inconclusivo;
- ação para confirmar.

O SignallQ nunca transforma correlação em certeza nem culpa provedor, roteador ou usuário sem
evidência suficiente.

## 4. Voz e conteúdo

### 4.1 Personalidade

A voz é simples, direta, calma, humana e factual. Ela não imita conversa espontânea, não dramatiza
e não revela desnecessariamente a tecnologia usada para produzir uma explicação.

O produto pode usar IA internamente, mas a interface deve parecer um produto consistente, não texto
gerado por um assistente.

### 4.2 Regras

- Português do Brasil, com “você”.
- Títulos em sentence case.
- Frases curtas; preferir uma ideia por frase.
- Começar pela conclusão útil.
- Usar palavras cotidianas antes do termo técnico.
- Evitar introduções, saudações, elogios e preenchimento conversacional.
- Não usar emoji.
- Não usar “eu analisei”, “a IA identificou” ou construções semelhantes.
- Não usar jargão sem tradução contextual.
- Não prometer resolver o que o produto apenas pode investigar ou orientar.

### 4.3 Tradução técnica

| Termo | Apresentação padrão |
|---|---|
| Ping | Tempo de resposta |
| Jitter | Variação do tempo de resposta |
| Packet loss | Falhas ou dados perdidos na conexão, conforme o método realmente medido |
| Bufferbloat | Atraso quando a rede fica ocupada |
| RSSI | Força do sinal Wi-Fi |
| RSRP/RSRQ/SINR | Qualidade do sinal móvel; siglas nos detalhes |

### 4.4 Estrutura de resultado

Exemplo preferido:

> **O Wi-Fi está fraco neste cômodo**  
> A distância do roteador está afetando sua conexão.  
> **Próximo passo:** aproxime-se do roteador e teste novamente.

Detalhes técnicos podem mostrar RSSI, banda, canal e evidências após a conclusão.

## 5. Fundação visual compartilhada

### 5.1 Cor

O roxo permanece como cor principal da marca, com uso restrito a:

- CTA principal;
- foco e seleção importante;
- progresso atual;
- navegação ativa quando necessário;
- pequenos momentos de identidade.

Não usar roxo por padrão em cards, ícones neutros, grandes áreas de fundo ou elementos sem função
de marca/ação.

O azul do sistema atual não integra a paleta ativa dos componentes centrais 2.0. Ele permanece no
logo oficial e como token técnico de compatibilidade durante a migração, mas não deve aparecer em
novo botão, seleção, link, badge informativo ou estado vazio. A UI usa violeta tonal para seleção e
ação secundária relacionada à marca, e neutros para informação sem julgamento.

Estados semânticos permanecem independentes da marca:

- verde: resultado positivo confirmado;
- âmbar: atenção ou resultado parcial;
- vermelho: falha ou condição crítica;
- neutro: informação sem julgamento de qualidade.

### 5.2 Superfícies

- Tema claro: superfícies brancas e cinzas neutros, com contraste discreto.
- Tema escuro: fundo-base **preto absoluto `#000000`**.
- Cards realmente necessários usam um par neutro dedicado: cinza quase branco `#F7F7F8` no tema
  claro e cinza quase preto `#161616` no escuro. Conteúdo interno elevado pode usar `#EEEEF0` e
  `#222222`, respectivamente.
- No tema escuro, agrupamentos, sheets e elementos elevados usam cinzas muito escuros para manter
  hierarquia; “preto absoluto” não significa usar a mesma cor em todas as camadas.
- A profundidade nasce primeiro da diferença tonal entre fundo, superfície e conteúdo elevado.
- Sombras são discretas, secundárias à hierarquia tonal e reservadas a elementos realmente
  elevados ou prioritários.
- Não usar contorno perimetral como tratamento padrão de cards ou seções. Bordas permanecem para
  controles, divisores e situações em que comuniquem um limite funcional.
- Não usar gradientes decorativos, glassmorphism ou texturas.

Os valores completos das superfícies escuras serão definidos e validados por contraste durante a
fase de tokens; somente o fundo-base `#000000` está aprovado neste documento.

### 5.3 Tipografia

- Família principal: Google Sans Flex no Android; fallback compatível na Web.
- Hierarquia curta e inequívoca.
- Títulos compactos; texto de apoio breve.
- Métricas grandes apenas quando forem o conteúdo principal da etapa.
- Unidades e detalhes têm peso visual secundário.
- Evitar overlines em excesso; usar somente quando organizarem grupos extensos.

### 5.4 Espaçamento

- Manter grid base de 8 dp/px compatível com Material 3.
- Manter os degraus existentes para componentes.
- Adotar espaçamentos de composição de **48** e **64** para separar blocos principais quando o
  viewport permitir.
- Respiro não deve ser substituído por cards vazios ou divisores repetitivos.

### 5.5 Forma

Formas seguem o componente, não uma estética universal arredondada. Cantos e pills não devem ser
usados para transformar todo conteúdo em objetos isolados.

### 5.6 Tokens-alvo da primeira versão

Os tokens abaixo são o contrato de design do 2.0. Eles preservam a identidade existente e reduzem
divergências entre artefatos. Até a migração, diferenças em relação a `SignallQTheme.kt` são alvo,
não estado implementado.

#### Cor

| Papel | Claro | Escuro | Uso principal |
|---|---:|---:|---|
| `primary` | `#5B21D6` | `#D0BCFF` | CTA, foco e seleção principal |
| `onPrimary` | `#FFFFFF` | `#38137E` | Conteúdo sobre `primary` |
| `primaryContainer` | `#EAE0FF` | `#4F2FA8` | Seleção e ênfase tonal |
| `onPrimaryContainer` | `#210A5C` | `#EADDFF` | Conteúdo sobre `primaryContainer` |
| `secondary` | `#2851B8` | `#AAC7FF` | Compatibilidade com o sistema atual; não usar em componente central novo |
| `surface` | `#FFFFFF` | `#000000` | Fundo-base da tela |
| `cardSurface` | `#F7F7F8` | `#161616` | Card necessário, sem contorno perimetral |
| `cardSurfaceElevated` | `#EEEEF0` | `#222222` | Região interna ou card elevado |
| `surfaceContainerLow` | `#F8F5FB` | `#121212` | Agrupamento discreto |
| `surfaceContainer` | `#F3EEFA` | `#1E1E1E` | Componentes e regiões elevadas |
| `surfaceContainerHigh` | `#ECE5F5` | `#2A2A2A` | Seleção, sheet e destaque elevado |
| `onSurface` | `#1C1B1F` | `#F5F2F7` | Texto e ícone principais |
| `onSurfaceVariant` | `#49454F` | `#CAC4D0` | Texto e ícone secundários |
| `outline` | `#79747E` | `#938F99` | Controles e bordas funcionais |
| `outlineVariant` | `#CAC4D0` | `#49454F` | Divisores e bordas discretas |
| `success` | `#146C2E` | `#83DA99` | Resultado positivo confirmado |
| `warning` | `#8A5000` | `#FFB870` | Atenção ou resultado parcial |
| `error` | `#BA1A1A` | `#FFB4AB` | Falha ou condição crítica |

As cores de conteúdo sobre containers semânticos continuam sendo pares dedicados
`onSuccessContainer`, `onWarningContainer` e `onErrorContainer`; não usar o tom de status puro
como texto sobre um container sem validar contraste.

#### Tipografia

Família única: Google Sans Flex, com Google Sans, Roboto e a fonte do sistema como fallbacks.

| Token | Tamanho/altura | Peso | Uso |
|---|---:|---:|---|
| `displaySmall` | 34/40 sp | 700 | Evidência principal excepcional |
| `headlineLarge` | 26/32 sp | 700 | Título de tela |
| `headlineSmall` | 22/28 sp | 600 | Conclusão ou seção principal |
| `titleLarge` | 20/26 sp | 600 | Título de componente destacado |
| `titleMedium` | 16/22 sp | 500 | Linha, item ou subseção |
| `titleSmall` | 14/20 sp | 500 | Rótulo de campo ou grupo |
| `bodyLarge` | 16/24 sp | 400 | Explicação principal |
| `bodyMedium` | 14/20 sp | 400 | Apoio e detalhes |
| `bodySmall` | 12/16 sp | 400 | Legenda curta |
| `labelLarge` | 14/20 sp | 500 | Botão |
| `labelMedium` | 12/16 sp | 500 | Chip e badge |
| `labelSmall` | 11/16 sp | 500 | Overline excepcional |

#### Espaçamento, forma e alvo de toque

- escala interna: `4, 8, 12, 16, 20, 24, 32, 40` dp;
- composição: `48` e `64` dp entre grandes blocos, quando o viewport permitir;
- margem horizontal móvel: `20` dp por padrão e `24` dp em larguras confortáveis;
- card: `16` dp; field: `12` dp; botão: `20` dp; dialog: `24` dp; sheet: `28` dp nos
  cantos superiores; chip e badge: pill;
- controles interativos: área tocável mínima de `48 × 48` dp, mesmo quando o elemento visual for
  menor;
- borda funcional: `1` dp; sombra apenas como reforço de elevação tonal.

#### Estado e movimento

- state layers: hover `8%`, focus `10%`, pressed `12%`, dragged `16%` sobre a cor de conteúdo
  apropriada;
- microinteração: `150–250 ms`; transição de tela ou container: `250–400 ms`;
- easing padrão: `cubic-bezier(.2, 0, 0, 1)`;
- movimento reduzido substitui deslocamento por troca imediata ou crossfade curto.

### 5.7 Expressão premium da marca

O acabamento premium do SignallQ não vem de brilho, 3D, glassmorphism ou decoração adicionada ao
logo. Ele nasce de cinco decisões:

1. **Escala:** símbolo e tipografia aparecem com presença, não como pequenos selos repetidos.
2. **Contraste:** preto absoluto, branco e violeta criam momentos inequívocos de marca.
3. **Profundidade:** sobreposição física, diferença tonal e sombra controlada indicam planos.
4. **Ritmo:** grandes áreas de respiro alternam com blocos compactos de mensagem.
5. **Precisão:** alinhamento, recorte e proporção permanecem rigorosos em qualquer aplicação.

O símbolo e os lockups oficiais em `brand/` não podem ser redesenhados, recoloridos nem receber
sombra, contorno ou efeitos. O azul presente no gradiente interno do símbolo permanece parte do
ativo oficial; isso não reintroduz azul como cor ativa da UI ou das superfícies de marca.

A expressão verbal da marca usa a ideia central **“Clareza para uma conexão invisível”** e mantém
o posicionamento `entender → diagnosticar → resolver → confirmar`. Em superfícies institucionais,
a marca pode usar tipografia em escala maior e composições mais assimétricas que a UI do produto,
desde que preserve leitura, acessibilidade e a voz calma, precisa e resolutiva.

## 6. Uso de componentes

### 6.1 Cards

Usar card somente quando o conteúdo:

- representa uma entidade ou resultado independente;
- é interativo;
- tem estado próprio;
- precisa ser separado do contexto;
- pode ser selecionado, movido, removido ou expandido como unidade.

Não usar card apenas para colocar fundo atrás de título e parágrafo. Preferir seção aberta, lista,
espaçamento ou divisor discreto.

Cards seguem Material 3: superfície neutra e profundidade tonal discreta. O padrão não tem linha
envolvendo todo o card; contorno só aparece quando comunica seleção, foco ou outro limite funcional.
Roxo não é fundo padrão de card.

### 6.2 Pills, chips e badges

Usar somente para:

- filtros curtos e mutuamente exclusivos;
- seletores segmentados;
- estados compactos indispensáveis;
- badges de contexto que não cabem naturalmente na hierarquia.

Não usar como contêiner padrão para títulos, métricas, navegação extensa ou decoração.

### 6.3 Botões e CTAs

- Uma ação principal por contexto.
- Filled roxo para a ação principal realmente prioritária.
- Ações secundárias usam tonal, outlined, text ou ícone conforme Material 3.
- Não colocar vários CTAs roxos competindo na mesma área.
- Rótulos descrevem a ação: “Analisar minha conexão”, “Testar novamente”, “Ver como melhorar”.

### 6.4 Ícones e ilustrações

- Material Symbols para ações, navegação e conceitos do sistema.
- Ilustrações próprias somente quando explicarem diagnóstico, topologia ou benefício do app.
- Ícone não precisa de círculo ou fundo colorido por padrão.
- Não usar emoji ou ícones decorativos sem função.

### 6.5 Biblioteca central da primeira versão

| Componente | Variantes mínimas | Contrato principal |
|---|---|---|
| Botão | filled, tonal, outlined, text, icon | Uma ação principal por contexto; loading preserva largura; alvo 48 dp |
| Campo | texto, seleção, busca | Label persistente, ajuda e erro textual; não depender só de placeholder |
| Chip | filter, input, assist | Seleção ou ação compacta; nunca decoração |
| Badge de status | positivo, atenção, crítico, informativo, neutro | Palavra + ícone quando necessário; cor nunca sozinha |
| Linha de lista | simples, navegável, selecionável, com ação | Padrão para sintomas, ações, histórico e configurações |
| Card | informativo, interativo, selecionável, resultado | Só para unidade independente; sem cards aninhados |
| Banner | informação, offline, atenção, erro recuperável | Mensagem curta e ação opcional; não bloquear conteúdo sem necessidade |
| Top app bar | raiz, retorno, ação contextual | Título curto, navegação previsível e no máximo uma ação de destaque |
| Navegação principal | compacta, com labels | Destinos de primeiro nível; ferramentas avançadas não viram abas por padrão |
| Bottom sheet | padrão, seletora, etapa contextual | Tarefa curta e reversível; grabber e fechamento previsíveis |
| Dialog | confirmação, permissão contextual, erro crítico | Somente quando interromper for necessário; ação segura clara |
| Progresso | linear, circular, etapas | Explica o que está acontecendo; não simula precisão inexistente |
| Estado de tela | vazio, skeleton, offline, erro, permissão | Sempre explica situação e próximo passo possível |
| Bloco de resultado | positivo, atenção, problema, inconclusivo | Veredito → causa/confiança → evidência → ação → confirmação |
| Métrica com tradução | compacta, destaque | Valor + unidade + significado humano próximo |
| Detalhes expansíveis | fechado, aberto | Complexidade técnica sob demanda, com label descritivo |

Anúncios nativos, gráficos técnicos, topologia de rede, gauges e visualizações especializadas não
entram na biblioteca central desta etapa. Eles serão extensões de domínio após validação do núcleo.

### 6.6 Estados obrigatórios dos componentes

Todo componente interativo deve especificar, quando aplicável: default, pressed, focused,
selected, disabled, loading, erro e conteúdo longo. Hover é documentado para Web e ferramentas de
preview, mas não orienta o comportamento primário do Android.

Componentes com conteúdo assíncrono devem preservar a hierarquia durante o carregamento. Preferir
skeleton localizado a spinner isolado no centro da tela; usar indicador indeterminado somente
quando a estrutura do conteúdo ainda não puder ser antecipada.

## 7. Movimento

Movimento deve explicar continuidade, hierarquia ou mudança de estado.

### Android

- usar transições e componentes nativos do Material 3/Compose;
- shared axis para avanço e retorno entre etapas;
- container transform quando um elemento abre seu detalhe;
- fade through para troca de conteúdo no mesmo nível;
- sheets, dialogs, ripple e feedback háptico conforme a plataforma;
- durações preferenciais entre 200 e 400 ms;
- respeitar configurações de redução de movimento;
- evitar bounce, parallax, partículas e animações associadas a “mágica de IA”.

### Web

- preservar a mesma intenção, adaptada ao navegador;
- usar transições discretas, sem simular literalmente navegação Android;
- respeitar `prefers-reduced-motion`;
- movimento nunca deve atrasar leitura, interação ou indexação do conteúdo.

## 8. Android — papel e jornada

O Android é o produto principal de diagnóstico aprofundado. Deve se apresentar como um aplicativo
nativo pronto para analisar a rede em diversos níveis e orientar o usuário.

### 8.1 Entrada por necessidade

A experiência deve começar pelo estado atual da conexão ou pelo sintoma percebido, por exemplo:

- verificar minha conexão;
- internet lenta;
- quedas e travamentos;
- Wi-Fi não chega bem;
- problemas em jogos;
- investigar rede móvel.

O usuário não precisa saber previamente qual ferramenta mede cada problema.

### 8.2 Jornada principal

1. **Situação:** o usuário informa ou confirma o problema.
2. **Análise:** o produto executa ou solicita as medições necessárias.
3. **Conclusão:** apresenta resultado curto, visual e compreensível.
4. **Orientação:** oferece uma ação prioritária.
5. **Confirmação:** permite repetir e comparar.
6. **Detalhes:** expõe métricas e ferramentas avançadas sob demanda.

### 8.3 Densidade

- Pouco texto na camada principal.
- Uma visualização dominante por etapa.
- Ferramentas avançadas permanecem acessíveis, mas não definem a navegação principal.
- Estados vazio, carregando, parcial e inconclusivo sempre indicam a próxima ação possível.

## 9. Web/PWA — conteúdo, aquisição e conversão

A Web segue a mesma identidade, mas seu objetivo principal é **atrair pessoas por problemas reais,
demonstrar o valor do SignallQ e converter para o aplicativo Android**.

O diagnóstico web é uma amostra útil e uma ferramenta de aquisição, não a promessa completa do
produto.

### 9.1 Funções da Web

- responder buscas sobre lentidão, quedas, Wi-Fi, jogos, DNS e estabilidade;
- oferecer conteúdo confiável, escaneável e indexável;
- disponibilizar ferramentas web que entreguem valor imediato;
- mostrar visualmente como o aplicativo aprofunda a análise;
- explicar limites do navegador sem diminuir o valor da experiência;
- direcionar para instalação, teste fechado ou página do aplicativo.

### 9.2 Estrutura de conteúdo

Páginas orientadas por busca devem conter:

1. resposta direta ao problema;
2. sintomas e causas possíveis;
3. teste ou ação que possa ser feita no navegador;
4. leitura do resultado em linguagem simples;
5. limite claro do navegador;
6. benefício específico do Android;
7. CTA contextual para o aplicativo.

O site não deve parecer apenas um banner de download. Ele entrega valor antes de pedir conversão.

### 9.3 Conversão contextual

Exemplo:

> Encontramos instabilidade, mas o navegador não consegue verificar a força do sinal dentro da sua
> casa. O aplicativo SignallQ analisa o Wi-Fi por cômodo.

O CTA deve corresponder ao benefício apresentado, não usar chamadas genéricas repetidas em todas
as seções.

### 9.4 SEO e apresentação

- HTML semântico e conteúdo acessível sem depender de animação ou interação.
- Títulos e descrições alinhados à intenção de busca.
- Conteúdo original, direto e tecnicamente responsável.
- Demonstrações reais do aplicativo, não interfaces fictícias.
- Evidências de confiança: metodologia, privacidade, limites e autoria.
- Boa performance, responsividade e estabilidade visual.

## 10. Acessibilidade

- Contraste mínimo WCAG AA para texto e controles.
- Alvos de toque adequados à plataforma.
- Ordem de leitura semântica e navegação por teclado na Web.
- Labels e estados compreensíveis por tecnologias assistivas.
- Cor nunca é a única forma de comunicar estado.
- Texto suporta aumento de fonte e reflow.
- Movimento respeita preferência de redução.
- Visualizações de rede oferecem alternativa textual curta.

## 11. Checklist para agentes

Antes de aprovar uma tela, fluxo ou componente, responder:

- A pessoa entende o objetivo da tela sem conhecimento técnico?
- Existe uma ação principal clara?
- A conclusão aparece antes dos detalhes?
- O produto orienta ou apenas oferece ferramentas?
- Todo card e pill tem justificativa funcional?
- O roxo está reservado ao que realmente importa?
- O texto poderia ser menor sem perder precisão?
- Há espaço suficiente entre assuntos diferentes?
- O estado escuro preserva preto-base e hierarquia?
- O movimento parece nativo e ajuda a entender a transição?
- A Web entrega valor de busca e conduz contextualmente ao Android?
- Limitações e nível de confiança estão explícitos?

## 12. Critérios de aceite da formalização

- [x] Fundamentos comuns definidos.
- [x] Papéis de Android e Web separados.
- [x] Voz e microcopy definidos.
- [x] Regras para cor, modo escuro, cards, pills, espaçamento e movimento definidas.
- [x] Jornada guiada do Android definida.
- [x] Papel de aquisição e SEO da Web definido.
- [x] Tokens visuais-alvo da primeira versão especificados; validação de implementação permanece na migração.
- [x] Biblioteca central e contratos de estado definidos; mapeamento do legado Android permanece na migração.
- [ ] Componentes Web mapeados para manter, adaptar ou remover.
- [ ] Jornada principal prototipada e aprovada.
- [ ] Migração implementada e verificada nas duas plataformas.

## 13. Fontes relacionadas

- [`../POSICIONAMENTO_PRODUTO.md`](../POSICIONAMENTO_PRODUTO.md)
- [`../DESIGN_SYSTEM.md`](../DESIGN_SYSTEM.md)
- [`../FUNCIONAL.md`](../FUNCIONAL.md)
- [`../HISTORIA.md`](../HISTORIA.md)
