# Cobertura do protótipo SignallQ Android 2.0

Inventário baseado no app Android real e na jornada guiada 2.0. O protótipo contém 42 destinos navegáveis.

## Navegação principal proposta

- Início — veredito atual, trilha da conexão e entrada do diagnóstico guiado.
- Velocidade — medição manual como fonte de evidência.
- Histórico — diagnósticos, medições e comparações anteriores.
- Ferramentas — acesso direto ao hub completo, sem ficar escondido em “Mais”.
- Perfil — acesso pela barra superior a Ajustes, Privacidade, Novidades, Ajuda, Termos e Sobre.

Todas as 39 telas de uso diário são alcançáveis a partir da Início. Boas-vindas, Permissões e LGPD
são contextuais ao primeiro acesso e, por isso, não aparecem na navegação cotidiana.

## Jornada principal

- Início
- Seleção de sintoma
- SignallQ Assist — pergunta breve para ajustar a análise ao caso informado
- Análise em andamento
- Resultado do diagnóstico
- Orientação
- Nova verificação
- Comparação antes e depois
- Evidência insuficiente
- Erro recuperável

## Medição e diagnóstico técnico

- Velocidade
- Medição em andamento
- Resultado da medição
- Detalhes técnicos
- Laudo compartilhável

## Sinal e infraestrutura

- Redes Wi-Fi
- Canais Wi-Fi
- Sinal móvel
- Sinal Wi-Fi em tempo real
- Dispositivos conectados
- Equipamento de internet
- Detalhe de rede
- Detalhe de dispositivo
- Acesso ao equipamento

## Ferramentas

- Hub de ferramentas
- Ping
- DNS
- Monitoramento
- Modo gamer
- Contato da operadora

## Histórico, ajustes e informações

- Histórico
- Mais
- Ajustes
- Privacidade
- Termos de uso
- Novidades
- Dados locais
- Ajuda e suporte
- Sobre o SignallQ

## Primeiro acesso

- Boas-vindas
- Permissões opcionais
- Consentimento LGPD

## Estados que devem ser revisados na próxima rodada visual

Cada recurso crítico ainda deve receber suas variações de carregamento, vazio, permissão negada,
offline, rede móvel, falha parcial e recurso temporariamente indisponível. Essas variações não devem
virar destinos permanentes na navegação: serão controladas dentro das próprias telas.

## Regra da trilha de conexão para mesh

- Exibir o nó “mesh” na trilha principal somente quando o motor retornar `NO_MESH` com confiança alta.
- `SISTEMA_MESH_PROVAVEL`, confiança média/baixa ou sinais conflitantes não autorizam uma afirmação
  visual; nesses casos, a possibilidade aparece apenas nos detalhes técnicos.
- SSID igual, isoladamente, nunca é evidência suficiente.
- O nó é opcional: quando não há evidência confiável, a trilha liga equipamento diretamente ao Wi-Fi.
