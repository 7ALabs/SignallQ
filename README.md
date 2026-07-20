# SignallQ

**Entenda sua conexão. Encontre o problema. Saiba o que fazer.**

O SignallQ é um ecossistema de produtos para diagnóstico de internet e redes. A proposta é transformar medições técnicas — que normalmente parecem um monte de números — em explicações claras, recomendações práticas e evidências que ajudam pessoas e profissionais a tomar decisões melhores.

O projeto nasceu no Brasil com foco em problemas reais de conectividade: Wi-Fi instável, velocidade abaixo do esperado, sinal fraco, latência, interferência, falhas na fibra, cobertura ruim e dificuldade para identificar a origem do problema.

## O ecossistema

### SignallQ

Produto voltado ao consumidor final.

Ajuda o usuário a avaliar a qualidade da conexão, entender o que está acontecendo e descobrir quais ações podem melhorar a experiência.

Entre as experiências previstas e em evolução estão:

- testes de velocidade, latência e estabilidade;
- análise de sinal Wi-Fi, rede móvel e conexão de fibra;
- diagnóstico em linguagem simples;
- recomendações práticas e contextualizadas;
- histórico de medições e comparação de resultados;
- ferramentas para investigar problemas de rede;
- geração e compartilhamento de resultados.

O objetivo não é apenas dizer que a internet está “boa” ou “ruim”, mas explicar **por que**, mostrar **onde pode estar o problema** e orientar **qual é o próximo passo**.

### SignallQ Pro

Produto separado, voltado a técnicos de informática, instaladores de redes, consultores, prestadores de serviço e pequenos provedores.

O SignallQ Pro transforma medições de conectividade em um fluxo profissional de atendimento, permitindo organizar clientes, locais, visitas, ambientes, evidências e resultados antes e depois de uma intervenção.

A visão do produto inclui:

- cadastro de clientes e locais atendidos;
- medições organizadas por visita e ambiente;
- fotos, observações e evidências técnicas;
- comparação antes e depois;
- histórico por cliente;
- relatórios profissionais personalizáveis;
- apoio à apresentação e venda do serviço realizado.

O Pro não é apenas uma versão com “mais botões”. Ele resolve outro problema: ajudar o profissional a transformar conhecimento técnico em um serviço organizado, demonstrável e valorizado pelo cliente.

### SignallQ Admin

Ambiente interno de operação e acompanhamento do ecossistema.

O Admin apoia a gestão de qualidade, versões, feedbacks, estabilidade e evolução dos produtos. Ele não é destinado ao consumidor final e não representa uma área pública de acesso.

A separação existe para manter responsabilidades claras:

| Produto | Público | Papel principal |
|---|---|---|
| **SignallQ** | Consumidores | Entender e melhorar a própria conexão |
| **SignallQ Pro** | Profissionais de redes e suporte | Executar atendimentos e gerar evidências e relatórios |
| **SignallQ Admin** | Operação interna | Acompanhar a saúde e a evolução dos produtos |

## O que torna o SignallQ diferente

Muitas ferramentas mostram métricas. O SignallQ quer conectar essas métricas ao problema real do usuário.

Isso significa combinar medições, contexto e orientação para responder perguntas como:

- O problema está na operadora, no Wi-Fi ou no dispositivo?
- A velocidade contratada está chegando de forma útil?
- A rede está estável ou apenas teve um pico de velocidade?
- O sinal está fraco por distância, interferência ou configuração?
- Trocar de canal, frequência, posição do roteador ou equipamento pode ajudar?
- A intervenção técnica realmente melhorou o ambiente?

## Princípios do projeto

- **Clareza antes de tecnicismo:** resultados devem ser compreensíveis sem exigir conhecimento de redes.
- **Ação antes de diagnóstico vazio:** sempre que possível, o usuário deve sair com um próximo passo.
- **Medição com contexto:** um número isolado raramente conta a história completa.
- **Privacidade e responsabilidade:** dados e acessos devem ser tratados com o mínimo necessário.
- **Separação de produtos:** consumidor, profissional e operação interna possuem necessidades diferentes.
- **Evolução baseada em evidências:** decisões de produto devem considerar testes, telemetria, feedback e uso real.

## Estado do projeto

O ecossistema está em desenvolvimento ativo. Funcionalidades, disponibilidade e escopo podem mudar conforme os produtos avançam em validação, testes e preparação para lançamento.

Este repositório concentra o desenvolvimento do aplicativo Android SignallQ e componentes relacionados. O SignallQ Pro é tratado como produto distinto, mesmo quando componentes reutilizáveis ou fundamentos técnicos possam ser compartilhados entre os produtos.

## Escopo público e segurança

Este README apresenta a visão pública do projeto. Por segurança e proteção do produto, ele não documenta credenciais, endpoints privados, regras proprietárias de diagnóstico, mecanismos internos de proteção, detalhes de implantação ou integrações restritas.

Relatos públicos também não devem incluir senhas, tokens, chaves, dados pessoais, endereços internos, arquivos de configuração privados ou informações de clientes.

## Colaboração e feedback

Sugestões, relatos de comportamento inesperado e propostas de melhoria podem ser registrados nas Issues do repositório, sem incluir informações sensíveis.

Ao relatar um problema, descreva o cenário, o comportamento esperado, o que aconteceu e, quando possível, o modelo do aparelho e a versão do Android.

---

**SignallQ** — conectividade explicada de um jeito que ajuda a agir.
