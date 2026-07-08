# Product

## Register

product

## Users

Usuário brasileiro não-técnico cuja internet "está dando problema" — quer saber o que está errado e o que fazer, não um relatório de rede. Contexto: em casa, ansioso/frustrado com Wi-Fi lento, streaming travando, jogo com lag, ou fibra caindo. Job to be done: diagnosticar rapidamente a causa (Wi-Fi, fibra/modem, operadora móvel, DNS) e receber um próximo passo acionável, sem precisar entender jargão de rede.

## Product Purpose

SignallQ é um app Android de diagnóstico de conectividade (Kotlin/Compose/Material 3) que analisa em tempo real velocidade, latência, sinal Wi-Fi/canais, DNS, modem de fibra (GPON) e sinal móvel (4G/5G), e usa IA (SignallQ, via Cloudflare Worker) para explicar em português claro por que a internet está lenta, instável ou fora do ar. Sucesso = usuário entende o problema e sabe o próximo passo em poucos toques, sem precisar ligar para a operadora ou entender termos técnicos sozinho.

## Brand Personality

Voz: calorosa, direta, tranquilizadora — nunca um engenheiro de redes dando aula. Fala **com** o usuário na 2ª pessoa ("você", "sua conexão"), nunca sobre ele. Três palavras: **acolhedor, claro, confiável**. Padrão "jargão, depois tradução": métrica crua sempre acompanhada de veredito humano (Excelente/Bom/Regular/Fraco/Forte). Sem emoji — significado vem de ícone Material + cor semântica.

## Anti-references

- Apps genéricos de speedtest (tipo Ookla/Speedtest.net): números soltos sem contexto humano, sem veredito, sem próximo passo.
- Dashboards técnicos/enterprise de rede (tipo Wireshark): jargão não traduzido, visual denso, feito para quem já entende de rede.

## Design Principles

1. **Jargão, depois tradução** — todo dado técnico (RSSI, jitter, dBm) vem acompanhado de um veredito em linguagem humana.
2. **Sempre um próximo passo** — diagnóstico nunca termina em "isso está ruim" sem indicar o que fazer.
3. **Uma cor de destaque, semântica clara** — violeta `#6C2BFF` para ação/marca; verde/âmbar/vermelho carregam significado de qualidade de conexão, nada mais.
4. **Nada decorativo compete com o dado** — sem imagens, sem gradiente decorativo (só avatar e header de IA), sem textura.
5. **App é o narrador, IA é a inteligência** — SignallQ (IA) tem superfície visual própria (sempre escura), separada do restante do app.

## Accessibility & Inclusion

Sem WCAG level formal declarado. Seguir boas práticas Material 3: contraste de texto, touch target mínimo 56dp, sem depender só de cor para status (ícone + cor + palavra sempre juntos).
