# Auditoria de navegação — SignallQ Android 2.0

Data: 15 de agosto de 2026  
Escopo: protótipo navegável `index.html`, 42 telas, 116 ligações declaradas.

## Veredito pós-correção

**Aprovado como protótipo navegável para revisão visual e futura implementação.** As seis classes
de problemas P1 e as quatro P2 desta auditoria foram tratadas nesta rodada. As 39 telas do uso
diário agora são alcançáveis a partir da Início; Onboarding, Permissões e LGPD permanecem corretamente
contextuais ao primeiro acesso.

O índice lateral continua sendo apenas uma ferramenta de revisão, sem participar do grafo do app.
Antes de converter para Compose, ainda é necessária validação manual em dispositivo Android real,
com TalkBack, texto ampliado e botão Voltar do sistema.

## Saúde técnica

| Dimensão | Nota | Achado principal |
|---|---:|---|
| Acessibilidade | 4/4 | nomes acessíveis e alvos mínimos de 48 px verificados |
| Performance | 4/4 | HTML local leve, animações simples e redução de movimento |
| Responsividade | 3/4 | estrutura móvel sólida; validação em dispositivo real ainda pendente |
| Temas | 4/4 | tokens claro/escuro e contrastes principais consistentes |
| Antipadrões | 4/4 | composição aberta, poucos cards e sem efeitos decorativos excessivos |
| **Total** | **19/20** | **Excelente como protótipo; falta validação em aparelho real** |

## Evidência da correção

- 42 telas totais e 42 destinos declarados válidos;
- 39 telas diárias alcançáveis naturalmente a partir da Início;
- 3 telas contextuais de primeiro acesso fora da navegação diária;
- nenhum destino ausente e nenhum ID duplicado;
- tabs, pills, switches e botões com área mínima de 48 px;
- todos os switches com nome acessível;
- JavaScript validado sintaticamente;
- contrastes principais entre 8,73:1 e 17,13:1; CTA escuro em 11,37:1.

## Mapa principal recomendado

```text
Início
├── Diagnóstico guiado
│   └── Sintoma → contexto → análise → resultado → orientação → confirmação
├── Trilha da conexão
│   └── Internet → equipamento → mesh opcional → Wi-Fi → aparelho
└── Veredito atual

Velocidade
└── Preparação → medição → resultado → diagnóstico/detalhes

Histórico
└── Sessão → detalhes → comparação/compartilhamento

Ferramentas
├── Sinal
│   ├── Wi-Fi
│   ├── Canais
│   └── Móvel
├── Sinal Wi-Fi ao vivo
├── Dispositivos
├── Equipamento
├── Ping
├── DNS
├── Laudo
├── Monitoramento
└── Modo gamer

Perfil
├── Ajustes
├── Privacidade e dados locais
├── Novidades
├── Ajuda e suporte
├── Termos de uso
└── Sobre
```

## Achados P1 — resolvidos

### P1.1 — Recursos existentes sem caminho natural — resolvido

**Local:** `index.html`, hub de Ferramentas e tela de Ajustes.  
**Impacto:** Wi-Fi completo, Canais, Sinal móvel, Novidades, Ajuda, Sobre e Termos existem, mas
dependem do índice lateral ou de outra tela também inacessível. O usuário comum não os encontrará.

**Especificação:**

- incluir **Sinal** no hub de Ferramentas, abrindo a tela com as três abas Wi-Fi/Canal/Móvel;
- manter **Sinal Wi-Fi ao vivo** como ferramenta separada;
- completar Perfil/Ajustes com Novidades, Ajuda, Termos e Sobre;
- remover ou reaproveitar a tela órfã “Mais”.

### P1.2 — Ações que parecem funcionais, mas não respondem — resolvido

**Local:** Sintomas, modos de velocidade, redes Wi-Fi, jogos, Ping, DNS, Monitoramento, Privacidade,
acesso ao equipamento, operadora, dados locais e suporte.  
**Impacto:** quebra de confiança e impossibilidade de concluir tarefas mostradas como disponíveis.

**Exemplos verificados:** quatro sintomas sem destino; seletor Rápido/Completo/3 testes sem mudança;
Comparar DNS, Configurar alertas, Compartilhar PDF e jogos sem fluxo; contatos externos sem ação.

**Especificação:** todo botão deve navegar, alterar estado ou estar explicitamente desabilitado com
explicação. Não manter affordance de botão em elementos demonstrativos.

### P1.3 — Navegação inferior aparece em contextos onde não deveria — resolvido

**Local:** `index.html`, `.nav` é permanente.  
**Impacto:** onboarding, consentimento, medição em tela cheia e etapas profundas permitem abandonar
o fluxo sem confirmação e sem preservar contexto visível.

**Especificação:**

- esconder a navegação no onboarding, permissões, LGPD, medição em curso e fluxos profundos;
- manter botão Voltar e título do fluxo nesses destinos;
- mostrar a navegação apenas nos quatro destinos raiz e, se desejado, em páginas secundárias que
  preservem claramente a aba pai.

### P1.4 — Aceite do onboarding não bloqueia “Começar” — resolvido

**Local:** tela `onboarding`.  
**Impacto:** o protótipo permite continuar sem aceitar Termos/Privacidade, divergindo do contrato
funcional atual.

**Especificação:** botão desabilitado até o checkbox ser marcado, com estado visual e semântico
inequívoco.

### P1.5 — Alvos de toque e nomes acessíveis incompletos — resolvido

**Local:** `.tab` e `.pill` têm 40 px; `.switch` tem 32 px de altura; cinco switches não possuem
nome acessível.  
**Impacto:** maior chance de toque errado e experiência ruim com TalkBack/controle por voz.

**Especificação:** área interativa mínima de 48×48 px; `aria-label` contextual em cada switch;
manter o visual compacto dentro de um wrapper de toque de 48 px quando necessário.

### P1.6 — Recuperação de erro termina em CTA enganoso — resolvido

**Local:** handler de `retry-button`.  
**Impacto:** após três tentativas o rótulo muda para “Falar com o suporte”, mas o botão continua
executando a mesma rotina de reconexão.

**Especificação:** trocar também a ação para abrir Ajuda e suporte, preservando o código SQ-204.

## Achados P2 — resolvidos

### P2.1 — Aba pai não permanece identificada em telas profundas — resolvido

Telas abertas a partir de Velocidade e Ferramentas não declaram consistentemente `data-nav`.
Quando a barra permanece visível, nenhuma aba pode aparecer ativa. Associar cada destino à sua raiz
ou esconder a barra durante o fluxo.

### P2.2 — “Mais” ficou órfã após a promoção de Ferramentas — resolvido

A tela ainda duplica Ajustes e Ferramentas, mas deixou de ter entrada na navegação do app. Remover
o conceito ou transformá-lo explicitamente em Perfil; não manter dois centros de organização.

### P2.3 — Estados ativos não têm comportamento completo — resolvido

Abas e pills exibem estado selecionado, mas várias não atualizam `aria-pressed`, `aria-selected` ou
o conteúdo relacionado. Padronizar tabs com `role=tablist`, `role=tab` e seleção controlada.

### P2.4 — Ações destrutivas ainda não mostram confirmação — resolvido

“Apagar dados locais” e “Excluir dados locais” precisam abrir um diálogo com escopo, consequência e
cancelamento antes de qualquer implementação real.

## Fluxos críticos

| Fluxo | Entrada | Passos até a ação principal | Situação |
|---|---|---:|---|
| Diagnosticar | Início | 1 para iniciar; sequência guiada | claro, mas só um dos sintomas funciona |
| Medir velocidade | Barra inferior | 1 para abrir, 1 para iniciar | claro; modos ainda inertes |
| Abrir ferramentas | Barra inferior | 1 | claro |
| Analisar Wi-Fi completo | — | impossível pela UI natural | P1 |
| Consultar histórico | Barra inferior | 1 | claro |
| Abrir ajustes | Perfil na barra superior | 1 | claro |
| Consultar Termos após onboarding | — | impossível pela UI natural | P1 |
| Obter suporte | — | impossível pela UI natural | P1 |

## Back stack e foco

### Funcionando bem

- pilha interna evita loops ao usar o botão Voltar;
- Escape replica Voltar no protótipo;
- destino recém-aberto recebe foco programático;
- telas invisíveis usam `visibility: hidden`, evitando foco acidental;
- indicadores de foco são visíveis e há `prefers-reduced-motion`.

### A validar após correções

- troca entre abas raiz deve preservar ou reiniciar a pilha de cada aba de forma deliberada;
- sair de uma medição em andamento deve pedir confirmação;
- resultado → Voltar precisa retornar a Velocidade, não a um estágio temporário da execução;
- fechar sheet deve restaurar foco ao elemento que a abriu.

## Contraste e tema

Os pares principais verificados passam WCAG AA/AAA. O CTA escuro usa `#0D0D12` sobre `#D0BCFF`
com contraste de 11,37:1. O âmbar do veredito possui 6,51:1 no claro e 12,34:1 no escuro.

## Pontos positivos

- quatro destinos principais têm rótulo e ícone, dentro do limite recomendado de 3–5;
- Ferramentas agora é encontrável em um toque;
- Início oferece um CTA primário inequívoco;
- conclusão humana aparece antes das métricas;
- trilha da conexão oferece atalhos contextuais sem depender de cards;
- profundidade máxima dos recursos já conectados fica entre um e três toques;
- temas e foco têm fundamentos sólidos.

## Ordem recomendada de correção

1. `/impeccable shape` — fechar a arquitetura Perfil/Ferramentas e as regras de barra inferior.
2. `/impeccable harden` — ligar controles, estados, erros, confirmação e onboarding.
3. `/impeccable adapt` — ampliar alvos de toque e testar texto ampliado/viewport curto.
4. `/impeccable polish` — revisar consistência visual e navegação depois das correções.

Reexecutar a auditoria após as correções.
