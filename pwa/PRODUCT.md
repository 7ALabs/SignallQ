# Product

## Register

product

## Users

Usuário comum de internet residencial/móvel, sem vocabulário técnico de redes, tentando entender por que a conexão parece ruim. Contexto de uso: geralmente sob frustração (Wi-Fi lento, chamada caindo, jogo travando), no navegador, muitas vezes no celular. Não tem paciência para telas de diagnóstico técnico cheias de números sem explicação.

## Product Purpose

SignallQ PWA é a versão web instalável do SignallQ: mede a qualidade da conexão pelo navegador e explica, com honestidade, se está boa, lenta ou instável — sem fingir medir o que o navegador não permite. Separa velocidade de estabilidade e indica o próximo passo. Sucesso é o usuário sair da tela sabendo exatamente o que está errado (ou que está tudo bem) e o que fazer a seguir, sem ansiedade extra.

## Brand Personality

Honesto, direto, calmo. Fala a verdade mesmo quando a notícia é ruim, mas sem alarmismo — problema de conexão já é estressante o suficiente. Tom PT-BR com "você", sentence case em títulos, sem emoji. Métrica crua nunca aparece sozinha: sempre vem com veredito humano (Excelente/Bom/Regular/Fraco/Forte).

## Anti-references

- SaaS genérico: hero-metric com gradiente, cards de feature idênticos, glassmorphism decorativo, eyebrows numerados em toda seção.
- Google Fiber Speed Test: serve só como inspiração de clareza (tela limpa, número grande, uma ação primária) — não copiar identidade visual, cores, layout ou marca.
- Qualquer coisa que pareça estar inventando ou arredondando métrica que o navegador não conseguiu medir de verdade.

## Design Principles

- Honestidade antes de completude: métrica não medida aparece como "não medida", nunca como número inventado.
- Veredito acompanha número: toda métrica técnica vem com uma leitura humana do que aquilo significa.
- Calma mesmo em cenário ruim: comunicar problema sem tom de alarme ou culpa do usuário.
- Ação sempre visível: toda tela de resultado aponta um próximo passo, não só um diagnóstico.
- Paridade de linguagem, não de estrutura: PWA segue os tokens e a linguagem visual do Android (`docs/parity.md`), mas usa padrões nativos da web (top nav, não bottom nav) em vez de copiar a estrutura do app nativo.

## Accessibility & Inclusion

WCAG AA como padrão: contraste mínimo AA em texto e estados, `prefers-reduced-motion` respeitado em toda animação. Sem requisito adicional conhecido além do já registrado em `CLAUDE.md` e `docs/design-system.md`.
