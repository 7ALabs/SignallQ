# Mapa de Coordenadas de UI — SignallQ Android v0.22.1

> Gerado em 2026-07-03 durante sessão de QA manual via ADB no Samsung A25 5G (SM-A256E, 1080×2340, densidade 420).
> Objetivo: eliminar erro de mira em testes automatizados futuros via `adb shell input tap x y`. Todas as coordenadas abaixo são em pixels físicos (resolução 1080×2340), origem no canto superior esquerdo.

## Como usar

Antes de assumir uma posição por "olho" na screenshot renderizada (que costuma vir redimensionada, ex. 923×2000), sempre:
1. Confirmar a resolução real do device (`adb shell wm size`).
2. Se possível, cropar a região suspeita da screenshot em resolução nativa (`PIL.Image.crop`) para achar o centro exato do elemento antes de tocar.
3. Preferir os valores desta tabela como ponto de partida — eles foram validados por log (`ViewPostIme pointer 0/1`) confirmando toque no elemento certo.

Essas coordenadas mudam se o layout for alterado (novo elemento, reordenação, mudança de copy) ou em telas com scroll (posição relativa ao scroll atual). Revalidar sempre que a UI mudar.

## Bottom Navigation (5 abas — fixa em todas as telas principais)

Y fixo: **2192**. X por aba (centros, tab width ≈ 216px em 1080 de largura):

| Aba | X |
|---|---|
| Início | 99 |
| Velocidade | 318 |
| Sinal | 539 |
| Histórico | **760** (não 650 — erro comum) |
| Ajustes | 972 |

## Home (Início)

| Elemento | X | Y | Nota |
|---|---|---|---|
| Card Wi-Fi/Rede (topo) | — | 184–230 | IP público aparece aqui |
| "Ver histórico" (link, card Medições) | 786 | 640 | **Dead link confirmado** (issue #377) |
| Gráfico Medições (área clicável) | 461 | 1050 | Sem ação (mesmo link morto) |
| "Ver detalhes —" | 780 | 1418 | **Dead link confirmado** (issue #377) |
| Botão "Medir velocidade" | 461 | 1097 | Funcional |
| Atalho DNS | 200 | 1780 | Abre sheet |
| Atalho Ping | 540 | 1780 | Abre sheet |
| Atalho Diagnóstico | 878 | 1780 | Abre Laudo |
| Card Wi-Fi conectado (Sinal resumido) | — | ~1440 | — |

Atenção: a posição vertical dos atalhos (DNS/Ping/Diagnóstico) muda dependendo do conteúdo acima (ex.: se há card de rede móvel extra). Sempre confirmar por crop antes de automatizar.

## Velocidade (Speedtest)

| Elemento | X | Y |
|---|---|---|
| Botão circular "Iniciar" | 461 | 924 |
| Seletor modo "Rápido" | 190–211 | 1528 |
| Seletor modo "Completo" | 461 | 1528 |
| Seletor modo "Triplo" | 743 | 1528 |
| Botão "Cancelar" (durante medição) | 461 | ~1900 |

### Tela de erro (offline / falha)

| Elemento | X | Y |
|---|---|---|
| Seta de voltar (topo) | 63 | 138 |
| Botão "Testar novamente" | 461 | 1182 |
| "Cancelar" (texto) | 490 | 1530 |

**Atenção:** nesta tela específica, "Cancelar" e a seta de voltar não têm ação (bug #374). O único jeito de sair é `KEYCODE_BACK` físico, que mata o app inteiro (não navega para tela anterior).

## Sinal

Sub-abas (Y fixo **478** — não 400, erro comum):

| Sub-aba | X |
|---|---|
| Wi-Fi | 115 |
| Canal | 345 |
| Móvel | 577 |
| Dispositivos | 944 |

## Histórico

| Elemento | X | Y |
|---|---|---|
| Ícone exportar/download (topo direito) | 858 | 240 | **Dead click confirmado** (issue #377) |
| Filtro "Todos" | 100 | 1112 |
| Filtro "Wi-Fi" | 235 | 1112 |
| Filtro "Rede móvel" | 394 | 1112 |
| Botão "Medir agora" | 461 | 1410 | **Dead click offline confirmado** (issue #377) |

Nota: a posição Y de "Medir agora" e dos filtros muda conforme o filtro de operadora aparece/desaparece (sub-filtro "Todas/TIMBRASIL" quando "Rede móvel" está ativo). Sempre cropar antes de confiar em Y fixo aqui.

## Ajustes

| Elemento | X | Y |
|---|---|---|
| Card "Minha conexão" | 461 | 810 |
| Card tema "Sistema" | 172 | 1160 |
| Card tema "Claro" | 461 | 1160 |
| Card tema "Escuro" | 890 | 1160 |
| "Notificações" | 461 | 1348 |
| (scroll) "Gerenciar dados locais" | 461 | ~610 pós-scroll |
| (scroll) "Fale conosco" | 461 | ~955 pós-scroll |
| (scroll) "Sobre o SignallQ" | 461 | ~1420 pós-scroll |
| (scroll, seção AVANÇADO) "Fibra óptica" | 461 | ~1550 pós-scroll | **Dead click confirmado com rede ativa** (issue #384) |

Nota: "Fibra óptica" só aparece após rolar Ajustes até o fim, seção "AVANÇADO" (abaixo de "Consumo em testes este mês"). Fácil de não notar em testes rápidos — não é um atalho da Home nem do Diagnóstico.

## Armadilhas identificadas nesta sessão

1. **Erro sistemático de Y em faixas de tabs/sub-abas**: subestimar Y em ~60-80px foi o erro mais comum (ex.: sub-abas de Sinal, filtros de Histórico, cards de tema em Ajustes). Sempre que uma linha de chips/tabs "não responde", suspeitar de Y errado antes de concluir dead click.
2. **Aba Histórico no bottom nav**: X=760, não 650 (não é distribuição perfeitamente igual visualmente, mas é 1080/5=216 por aba).
3. **Cold start não é neutro**: o app restaura a última tela visitada (bug #376), então um teste que espera "abrir na Home" após `force-stop` + relaunch pode começar em outra tela sem aviso. Sempre screenshot logo após relaunch antes de disparar taps.
4. **PID muda entre relaunches**: sempre recapturar `adb shell pidof io.signallq.app` antes de anexar logcat por `--pid`, não reaproveitar PID de captura anterior.

## Referências

- Checklist de execução: `docs_ai/QA_ACCEPTANCE_CHECKLIST_v0.22.1.md`
- Pendências priorizadas: `docs_ai/QA_PENDENCIAS_v0.22.1.txt`
- Bugs catalogados nesta sessão: issues GitHub #374–#383 em `gmmattey/linka-android`
