> **ARQUIVADO em 2026-07-12** — todas as specs verificadas contra o código real
> (`HomeScreen.kt`, `SinalScreen.kt`, `SpeedTestScreen.kt`, `ResultadoVelocidadeScreen.kt`) e
> confirmadas implementadas (PR #206, mergeada 2026-05-28). Mantido como histórico de decisão.

# Design Specs — Mockup v2 UI (PR #206)

> Especificacao visual ancorада nos mockups JSX de referencia (`C:\Users\luizg\Downloads\SignallQ Disign_v2\screens\`).
> Plataforma: Android Kotlin (Compose + Material Design 3 + tokens `LocalLkTokens.current`).
> Mergeado em main via PR #206 em 2026-05-28.

---

## 1. HomeScreen

Arquivo: `app/src/main/kotlin/io/linka/app/kotlin/ui/screen/HomeScreen.kt`
Mockup: `home.jsx`

### 1.1 Ordem do LazyColumn

| Posicao | Composable |
|---------|------------|
| 1 | `NetworkPath` (card com 3 nos: dispositivo → roteador → provedor) |
| 2 | `MedicoesCard` (ultimas velocidades + mini-chart + botao "Medir velocidade") |
| 3 | `MiniCardsRow` (3 chips: Testar DNS / Ping / Diagnostico IA) |
| 4 | `SignalCard` (Wi-Fi ou movel — veja 1.2) |
| 5 | `SimChips` (so exibido em dual SIM — veja 1.3) |

### 1.2 SignalCard

Estrutura visual (linha unica, `Row`, `verticalAlignment = CenterVertically`, `gap = 12dp`):

```
[ CircleIcon 44dp ] [ Column: overline / ssid / metricas ]  [ Column: SignalBars / QualityWord ]
```

**CircleIcon:**
- Tamanho: 44dp diametro, `clip(CircleShape)`
- Fundo: `success` a 10% de opacidade quando Wi-Fi (`${LK.success}1A`); `accent` a 10% quando movel (`${LK.accent}1A`)
- Icone interno: 22dp, cor `success` (Wi-Fi) ou `accent` (movel)

**Coluna central:**

| Elemento | Spec |
|----------|------|
| Overline | `fontSize 11sp`, `fontWeight 600`, `color textTertiary`, `letterSpacing 0.3`. Formato: `"WI-FI · 5 GHZ"` ou `"REDE MOVEL · LTE"` (uppercase, ponto medio como separador) |
| SSID / Operadora | `fontSize 15sp`, `fontWeight 600`, `color textPrimary` |
| Metricas | `fontSize 11sp`, `color textSecondary`. Wi-Fi: `"RSSI −52 dBm · Canal 36 · 433 Mbps"`. Movel: `"RSRP −95 dBm · Claro · 5G"` |

**Coluna trailing (SignalBars + QualityWord):**
- `SignalBars`: 4 barras verticais, alturas `[6, 9, 12, 16]dp`, largura `3dp`, `borderRadius 1dp`. Barras preenchidas ate o valor de qualidade; barras vazias em `border`.
- `QualityWord`: `fontSize 10sp`, `fontWeight 600`, cor semantica:
  - "Forte" → `success`
  - "Regular" → `warning`
  - "Fraco" → `error`

**Ramificacao por tipo de conexao:**

> Wi-Fi: overline = `"WI-FI · {BANDA}"` (ex: `"WI-FI · 5 GHZ"`), icone Wi-Fi, metricas RSSI/canal/velocidade.
>
> Movel: overline = `"REDE MOVEL · {TECH}"` (ex: `"REDE MOVEL · LTE"`), icone de sinal celular, metricas RSRP/operadora/tecnologia. SignalBars usa RSRP mapeado para 1–4 barras.

### 1.3 SimChips (dual SIM)

Exibido abaixo do `SignalCard` apenas quando dois chips SIM estao ativos.

Layout: `Row`, `gap = 8dp`, cada chip ocupa `weight(1f)`.

Cada `SimChip`:
- Fundo: `bgCard`, borda `border` (chip inativo) ou `accent@25%` (chip ativo com dados)
- Conteudo em linha: icone SIM `14dp` + label operadora `12sp 600` + badge tecno `10sp` (ex: `"5G"`, `"4G"`)
- Indicador ativo: dot `8dp` em `success` ou badge `"EM USO"` `9sp 700` em `success@10%`
- Padding: `10dp vertical, 12dp horizontal`

---

## 2. SinalScreen

Arquivo: `app/src/main/kotlin/io/linka/app/kotlin/ui/screen/SinalScreen.kt`
Mockup: `sinal.jsx`

### 2.1 TabRow — sempre fixo com 3 abas

```
[ Wi-Fi ]  [ Canal ]  [ Movel ]
```

- Tabs presentes independente de conectividade (Wi-Fi ativo, movel ativo, ou ambos).
- Tab selecionada: `fontWeight 700`, `color accent`, `borderBottom 2dp solid accent`.
- Tab nao selecionada: `fontWeight 500`, `color textSecondary`, `borderBottom 2dp transparent`.
- Badge de erro (ponto vermelho `8dp`, `error`) exibido na aba Canal quando canal congestionado.

### 2.2 Aba Wi-Fi

**Estado normal (Wi-Fi conectado):** exibe `SinalRedes` — filtro de banda + card "SUA CONEXAO" + lista "OUTRAS REDES".

**Estado vazio — `WifiEmptyState` (sem Wi-Fi, so movel):**

```
[ CircleIcon 80dp: icone globo, cor accent, fundo accent@10% ]
[ Titulo: "Voce esta usando a internet do chip"  fontSize 17 fontWeight 600 ]
[ Descricao: "O Wi-Fi esta desligado ou desconectado..."  fontSize 13 textSecondary lineHeight 1.5 ]
[ Botao outline: "Abrir Wi-Fi nas configuracoes"  fontSize 12 fontWeight 600 ]
```

### 2.3 Card de rede conectada — cor de fundo

- **Antes (v1):** `successContainer` a 45% de opacidade.
- **Agora (v2):** `success` a 12% de opacidade — token: `${LK.success}1F`.
- Aplica ao container do grupo "SUA CONEXAO" e ao no conectado dentro da arvore de nos.

### 2.4 Aba Canal

Sem mudancas estruturais no mockup v2. Comportamento anterior mantido: grafico de espectro 2.4/5 GHz, lista de canais com nivel de congestionamento, guia de troca de canal (seletor Android/Roteador).

### 2.5 Aba Movel — `MovelTab`

Novo composable. Renderiza conforme estado:

| Estado | Conteudo |
|--------|----------|
| Chip unico ativo | `MobileChipPanel` — hero card com operadora + tecnologia, seguido de 3 `FriendlyCard` (Qualidade do sinal / Tipo de conexao / Experiencia esperada) + aviso de consumo de dados |
| Dual SIM | Secao "CHIPS ATIVOS" com dois `ChipFullCard` empilhados + aviso de consumo |
| Sem chip / sem permissao | `EmptyState` com icone amarelo e CTA "Permitir leitura do chip" |

**`FriendlyCard` (chip unico):**
- Container: `bgCard`, borda `border`, `borderRadius rCard`, `padding 12dp`
- Icone 36dp em circulo com fundo `color@10%`
- Titulo `12sp 600 textPrimary`, descricao `11sp textSecondary lineHeight 1.35`
- Badge trailing: `fontSize 11 fontWeight 700`, cor semantica, fundo `color@10%`, `borderRadius 999`

**`ChipFullCard` (dual SIM):**
- Chip ativo: fundo `accent@6%`, borda `accent@25%`
- Chip inativo: fundo `bgCard`, borda `border`
- Cabecalho: icone SIM + label "SIM 1" / "SIM 2" + badge "EM USO" (so no ativo)
- Corpo: operadora `17sp 700`, tecnologia `12sp textSecondary`
- Divisor, depois duas colunas: SINAL (qualidade humanizada) e QUALIDADE
- Rodape: descricao contextual `11sp textSecondary lineHeight 1.4`

---

## 3. SpeedTestScreen

Arquivo: `app/src/main/kotlin/io/linka/app/kotlin/ui/screen/SpeedTestScreen.kt`
Mockup: `speedtest.jsx`

### 3.1 TopBar

- Titulo: `"Velocidade"` (mantido)
- Subtitulo: condicional — exibe `"Plano contratado: {N} Mbps"` quando o valor esta disponivel; subtitulo omitido caso contrario. Nao exibir `"—"` ou placeholder vazio.

### 3.2 ModeSelector

```
[ Rapido ]  [ Completo ]  [ Triplo ]
```

- Container externo: `background bgSecondary`, `borderRadius 999`, `padding 2dp`
- Opcao ativa: `background bgPrimary`, `borderRadius 999`, `boxShadow elevation 1`, `fontWeight 600`, `color textPrimary`
- Opcao inativa: fundo transparente, `fontWeight 600`, `color textSecondary`
- Sem uso de `accent` como cor de selecao — paleta neutra intencional.

### 3.3 LastResultCard

Header: label `"ULTIMO RESULTADO"` (`fontSize 12 textTertiary fontWeight 600 letterSpacing 0.4`) + timestamp relativo (`fontSize 11 textTertiary`) alinhados nas extremidades da linha.

Corpo: `Row` com 3 colunas de metrica (`weight(1f)` cada):

| Coluna | Label | Cor |
|--------|-------|-----|
| 1 | Download | `success` |
| 2 | Upload | `accent` |
| 3 | Latencia | `success` |

Cada `Metric`:
```
label    fontSize 10 textTertiary  marginBottom 2dp
value    fontSize 20 fontWeight 700 [color]
unit     fontSize 10 textSecondary  alinhado baseline com value
```

---

## 4. ResultadoVelocidadeScreen

Arquivo: `app/src/main/kotlin/io/linka/app/kotlin/ui/screen/ResultadoVelocidadeScreen.kt`
Mockup: `resultado.jsx`

### 4.1 Grade circle — removido

O circulo com letra de nota (A/B/C/D) foi eliminado completamente. Nao ha substituto grafico — o topo da tela passa direto para o titulo de avaliacao.

### 4.2 Novo topo da tela

```
[ TopBar: back arrow / "Resultado do teste" / share icon ]

padding top 16dp
  Titulo: "Conexao excelente"        fontSize 20 fontWeight 600 color textPrimary  textAlign center
  Subtitulo: "Sua internet esta..."  fontSize 13 color textSecondary lineHeight 1.4  textAlign center  marginTop 6dp
  Badge de conexao: chip outline     background bgSecondary border border  fontSize 10  borderRadius 999
    [ icone wifi 12dp | "Via Wi-Fi" ]
```

O titulo e subtitulo sao derivados da nota calculada internamente — o composable recebe a string final, nao a letra.

### 4.3 Grade de metricas (sem alteracao estrutural)

Mantida a grade 2 colunas com `BigMetric` (Download, Upload, Latencia, Oscilacao, Perda, Bufferbloat). Apenas o topo mudou — o restante da tela permanece igual ao v1.

---

## Tokens de cor usados neste documento

| Token | Uso |
|-------|-----|
| `LK.success` | Wi-Fi conectado, sinal forte, metricas OK |
| `LK.accent` | Movel conectado, destaque de UI, botoes primarios |
| `LK.warning` | Sinal regular, aba canal congestionada, chip inativo fraco |
| `LK.error` | Sinal fraco, canal congestionado |
| `LK.bgPrimary` | Fundo de tela |
| `LK.bgSecondary` | Fundo de secoes internas, chips |
| `LK.bgCard` | Fundo de cards |
| `LK.border` | Bordas de cards e divisores |
| `LK.textPrimary` | Texto principal |
| `LK.textSecondary` | Texto de suporte |
| `LK.textTertiary` | Labels, overlines, timestamps |

Opacidades usadas como sufixo hex: `1A` = 10%, `1F` = 12%, `26` = 15%, `33` = 20%, `40` = 25%.
