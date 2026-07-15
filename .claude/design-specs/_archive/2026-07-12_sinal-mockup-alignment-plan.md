> **ARQUIVADO em 2026-07-12** — todas as 5 tasks confirmadas implementadas em `SinalScreen.kt`
> (filtro "Todos", "Mostrar Mais", `CanalCongestionadoBanner`, `ChannelItem` redesenhado, label
> "USO POR CANAL", `SpectrumChart` com `redesRaw`/`seuSSID` gaussianas). Mantido como histórico
> de decisão.

# Plano Técnico — Alinhamento SinalScreen ao Mockup (Abas Wi-Fi e Canal)

**Arquivo principal:** `app/src/main/kotlin/io/linka/app/kotlin/ui/screen/SinalScreen.kt` (3248 linhas)
**Escopo:** 5 discrepâncias visuais, todas na camada UI. Nenhuma mudança de domínio/negócio.
**Lia:** obrigatória nas tasks 2, 3, 4 e 5 (todas envolvem estado visual ou microcopy).

---

## Descobertas do mapeamento real

### Fatos confirmados no código

| # | Observação | Linha(s) |
|---|---|---|
| 1 | Filtro "Todos" **já existe** na aba Wi-Fi (`RedesTab`, linha 738/745) | 738, 745 |
| 2 | Filtro da aba Canal (`CanalTab`) tem só `["2.4GHz", "5GHz", "6GHz"]` — sem "Todos" | 1702 |
| 3 | Card "Canal atual" (a ser substituído/condicionalizado) está nas linhas 1826-1884 | 1826–1884 |
| 4 | `SpectrumChart` recebe apenas `espectro: SnapshotEspectroCanal` — sem redes individuais | 2118 |
| 5 | `DadoCanal` **não tem** `redesNoCanal: List<RedeWifiVizinha>` — gaussianas precisam de dado extra | DadoCanal.kt |
| 6 | `redesBanda` (lista de `RedeVizinha` filtrada por banda) já existe no scope de `CanalTab` | 1715 |
| 7 | `ChannelItem` atual: coluna esquerda (nome + badges de contagem), coluna direita (dBm + status) | 2422–2518 |
| 8 | Header da lista de canais usa "CANAL" / "REDES / SINAL" — mockup pede "USO POR CANAL" | 1963–1975 |
| 9 | Seção "OUTRAS REDES" na aba Wi-Fi não tem limite de exibição nem botão "Mostrar Mais" | 888–913 |
| 10 | `RedeWifiVizinha` tem: canal, rssiDbm, frequenciaMhz, ssid, bssid | DiagnosticInput.kt:86 |

### Risco crítico: gráfico de gaussianas

O `SpectrumChart` atual usa `DadoCanal.maxRssiDbm` (o pico do canal, não das redes individuais).
O gráfico de gaussianas precisa de **uma curva por rede**, centrada no canal dela, com altura proporcional ao RSSI.
Isso exige passar `redesBanda: List<RedeVizinha>` (já disponível em `CanalTab`) para `SpectrumChart` como novo parâmetro.
Nenhuma mudança de domínio é necessária — os dados já existem.

---

## Tasks de implementação

### Task 1 — Aba Wi-Fi: "Mostrar Mais" nas outras redes
**Domínio:** UI pura | **Arquivo:** `SinalScreen.kt` | **Lia:** sim

**Objetivo técnico:**
Limitar a exibição inicial de `otherClassificadas` a N itens (sugestão: 5) e adicionar link/botão "Mostrar Mais" no rodapé da lista.

**Localização exata:** linhas 888–913 em `RedesTab`.

**Implementação:**
```kotlin
// Estado local no scope de RedesTab
var mostrarTodasRedes by remember { mutableStateOf(false) }

// Derivar lista exibida
val redesExibidas = if (mostrarTodasRedes) otherClassificadas else otherClassificadas.take(5)
```

Após o `items(redesExibidas)`, adicionar:
```kotlin
if (otherClassificadas.size > 5 && !mostrarTodasRedes) {
    item {
        TextButton(
            onClick = { mostrarTodasRedes = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = LkSpacing.lg),
        ) {
            Text("Mostrar Mais (${otherClassificadas.size - 5})", color = LkColors.accent)
        }
    }
}
```

**Critério de aceite:**
- Quando há <= 5 outras redes: exibe todas, sem botão
- Quando há > 5: exibe 5, botão "Mostrar Mais (N)" aparece no rodapé
- Ao clicar: exibe todas, botão desaparece
- Pull-to-refresh reseta `mostrarTodasRedes = false`

**Testes:** nenhum teste unitário necessário (estado local puramente visual). Testar manualmente com > 5 redes vizinhas.

---

### Task 2 — Aba Canal: adicionar "Todos" ao filtro de banda
**Domínio:** UI pura | **Arquivo:** `SinalScreen.kt` | **Lia:** sim

**Objetivo técnico:**
Adicionar "Todos" como primeiro item de `bandasDisponiveis` em `CanalTab`, e ajustar a lógica de filtragem de `redesBanda` e `espectro` para lidar com "Todos".

**Localização exata:** linha 1702 (`CanalTab`) e linha 1715 (`redesBanda`).

**Implementação:**

1. Alterar lista de bandas:
```kotlin
val bandasDisponiveis = listOf("Todos", "2.4GHz", "5GHz", "6GHz")
```

2. Ajustar `selectedBanda` inicial — quando `connectedNetwork?.banda` é null, default para "Todos":
```kotlin
var selectedBanda by remember { mutableStateOf(connectedNetwork?.banda ?: "Todos") }
```

3. Ajustar `redesBanda`:
```kotlin
val redesBanda = remember(redes, selectedBanda) {
    if (selectedBanda == "Todos") redes else redes.filter { it.banda == selectedBanda }
}
```

4. Ajustar o chip de contagem no filtro — para "Todos", mostrar `redes.size`:
```kotlin
val n = if (banda == "Todos") redes.size else (bandaCounts[banda] ?: 0)
```

5. Ajustar o label da seção espectro:
```kotlin
SectionLabel(if (selectedBanda == "Todos") "ESPECTRO" else "ESPECTRO $selectedBanda")
```

**Atenção:** a chamada `computarEspectro(banda = selectedBanda)` na linha 1719 recebe a banda como parâmetro para calcular `canaisBase`. Quando `selectedBanda == "Todos"`, o engine usa `else -> redesValidas.mapNotNull { it.canal }.distinct().sorted()` — isso funciona, mas pode gerar muitos canais misturados de 2.4/5/6GHz no gráfico. A task deve documentar esse comportamento mas não bloqueia o merge.

**Critério de aceite:**
- Chip "Todos (N)" aparece como primeiro item do filtro horizontal
- Ao selecionar "Todos": exibe canais de todas as bandas
- Seleção inicial: banda da rede conectada, ou "Todos" se sem conexão
- Nenhuma quebra nos outros filtros de banda

**Testes:** nenhum teste unitário — estado local. Testar manualmente com redes 2.4/5GHz visíveis.

---

### Task 3 — Aba Canal: banner de congestionamento + remoção do card "Canal atual"
**Domínio:** UI | **Arquivo:** `SinalScreen.kt` | **Lia:** sim

**Objetivo técnico:**
Substituir o card "Canal atual" (linhas 1826-1884) por um banner warning de congestionamento que só aparece quando o canal atual está congestionado. O banner mostra: ícone Warning, título "Canal congestionado", subtítulo com contagem de redes vizinhas.

**Localização exata:** linhas 1826–1884 em `CanalTab` (LazyColumn).

**Lógica de visibilidade:**
```kotlin
val dadoCanalAtual = if (canalAtualInfo != null)
    espectro.dadosPorCanal.find { it.canal == canalAtualInfo }
else null

val mostrarBannerCongestionamento =
    dadoCanalAtual?.nivel == NivelCongestionamento.congestionado
```

**Novo composable:** `CanalCongestionadoBanner(dadoCanal: DadoCanal)`

```kotlin
@Composable
private fun CanalCongestionadoBanner(dadoCanal: DadoCanal) {
    val c = LocalLkTokens.current
    Row(
        modifier = Modifier
            .padding(horizontal = LkSpacing.lg)
            .fillMaxWidth()
            .clip(RoundedCornerShape(LkRadius.card))
            .border(1.dp, LkColors.warning.copy(alpha = 0.3f), RoundedCornerShape(LkRadius.card))
            .background(LkColors.warning.copy(alpha = 0.08f))
            .padding(LkSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            tint = LkColors.warning,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Canal congestionado",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = LkColors.warning,
            )
            Text(
                "${dadoCanal.countTerceiros} redes vizinhas dividem o canal ${dadoCanal.canal}.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
    }
}
```

**No LazyColumn:**
- Remover o bloco `if (canalAtualInfo != null) { item { Row(...) { ... "Canal atual" ... } } }` (linhas 1826-1884)
- Substituir por:
```kotlin
if (mostrarBannerCongestionamento && dadoCanalAtual != null) {
    item {
        CanalCongestionadoBanner(dadoCanal = dadoCanalAtual)
        Spacer(Modifier.height(LkSpacing.md))
    }
}
```

**Critério de aceite:**
- Canal livre ou moderado: banner não aparece, sem card nenhum no lugar
- Canal congestionado: banner aparece com cor warning, ícone Warning, texto correto
- Contagem de redes exibe `countTerceiros` (redes de terceiros, não total)
- Card "Canal ideal" (linhas 1917-1943, verde) e `CanalRecomendadoCard` continuam intactos

**Testes:** snapshot visual. Testar com mock de `DadoCanal` congestionado/livre.

---

### Task 4 — Aba Canal: redesenho do ChannelItem (barra horizontal de uso)
**Domínio:** UI | **Arquivo:** `SinalScreen.kt` | **Lia:** sim (redesenho completo de componente)

**Objetivo técnico:**
Substituir o layout atual do `ChannelItem` (linhas 2422-2518) pelo novo design do mockup:
- Linha: "Canal X" à esquerda | badge inline "SEU CANAL" ou "RECOMENDADO" | barra horizontal de uso | badge status à direita

**Novo layout:**

```
[ Canal 6 ] [ SEU CANAL ]  [████████░░] [ Congestionado ]
[ Canal 1 ]               [██░░░░░░░░] [ Livre ]
[ Canal 11] [ RECOMENDADO ] [███░░░░░░░] [ Moderado ]
```

**Assinatura do composable** — sem mudança de assinatura pública:
```kotlin
@Composable
private fun ChannelItem(
    dado: DadoCanal,
    isConnected: Boolean,
    onClick: () -> Unit,
)
```

**Implementação interna:**

```kotlin
@Composable
private fun ChannelItem(dado: DadoCanal, isConnected: Boolean, onClick: () -> Unit) {
    val c = LocalLkTokens.current
    val corStatus = congestionColor(dado.nivel)
    val labelStatus = when (dado.nivel) {
        NivelCongestionamento.livre -> "Livre"
        NivelCongestionamento.moderado -> "Moderado"
        NivelCongestionamento.congestionado -> "Congestionado"
    }
    // Fração de uso: baseada em count total (0 = vazio, >= 8 = 100%)
    val fracaoUso = (dado.count / 8f).coerceIn(0f, 1f)

    Row(
        Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = LkSpacing.lg, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        // "Canal X"
        Text(
            "Canal ${dado.canal}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isConnected) FontWeight.W700 else FontWeight.W500,
            color = if (isConnected) LkColors.accent else c.textPrimary,
            modifier = Modifier.widthIn(min = 60.dp),
        )

        // Badge "SEU CANAL" ou "RECOMENDADO"
        if (isConnected) {
            InlineBadge("SEU CANAL", LkColors.accent)
        } else if (dado.ehCanalRecomendado) {
            InlineBadge("RECOMENDADO", LkColors.accent)
        }

        // Barra horizontal de uso — peso para ocupar espaço restante
        LinearProgressBar(
            fraction = fracaoUso,
            color = corStatus,
            modifier = Modifier.weight(1f),
        )

        // Badge status à direita
        Text(
            labelStatus,
            style = MaterialTheme.typography.labelSmall,
            color = corStatus,
            fontWeight = FontWeight.W600,
        )
    }
}

@Composable
private fun InlineBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.W700, color = color)
    }
}

@Composable
private fun LinearProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val c = LocalLkTokens.current
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(c.bgSecondary),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
    }
}
```

**Ajuste do header da lista (linhas 1963-1975):**
- Substituir "CANAL" / "REDES / SINAL" por um único label "USO POR CANAL" à esquerda.

```kotlin
item {
    Text(
        "USO POR CANAL",
        modifier = Modifier.padding(horizontal = LkSpacing.lg),
        style = MaterialTheme.typography.labelMedium,
        color = c.textTertiary,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.8.sp,
    )
    Spacer(Modifier.height(LkSpacing.sm))
}
```

**Critério de aceite:**
- Canal conectado: texto accent, badge "SEU CANAL" accent
- Canal recomendado (não conectado): badge "RECOMENDADO" accent
- Barra: verde se livre, amarelo se moderado, vermelho se congestionado
- Badge de status à direita com cor correspondente
- Header mostra "USO POR CANAL" centralizado/esquerda
- Click no item continua abrindo o BottomSheet

**Testes:** revisar com dados de mock cobrindo: canal livre sem badge / canal atual / canal recomendado / canal congestionado.

---

### Task 5 — Aba Canal: gráfico de espectro com curvas gaussianas
**Domínio:** UI (Canvas) | **Arquivo:** `SinalScreen.kt` | **Lia:** sim (novo visual central da aba)

**Objetivo técnico:**
Substituir `SpectrumChart` (barras verticais por canal, linhas 2118-2256) por `SpectrumChartGaussian` (curvas parabólicas/gaussianas por rede individual, com legenda de nomes de redes).

**Importante:** a assinatura pública de `SpectrumChart` muda — precisa receber também a lista de redes brutas.

#### 5.1 — Adicionar parâmetro `redesRaw` ao SpectrumChart

No call site (linha 1891):
```kotlin
// Antes:
SpectrumChart(espectro = espectro)

// Depois:
SpectrumChart(espectro = espectro, redesRaw = redesBanda)
```

`redesBanda` é `List<RedeVizinha>` já computada na linha 1715 de `CanalTab`. `RedeVizinha` tem: ssid, canal, rssiDbm, banda, bssid.

#### 5.2 — Nova assinatura de SpectrumChart

```kotlin
@Composable
private fun SpectrumChart(
    espectro: SnapshotEspectroCanal,
    redesRaw: List<RedeVizinha> = emptyList(),
)
```

#### 5.3 — Lógica do gráfico gaussiano

**Conceito:**
- Cada rede vizinha é desenhada como uma curva gaussiana (parábola) centrada no seu canal
- Largura da curva: proporcional ao channel width (20MHz = 2 canais de raio, 40MHz = 4, 80MHz = 8)
  - Como `RedeVizinha` não tem `channelWidth`, usar largura fixa = 2 canais de raio para 2.4GHz e 4 canais de raio para 5/6GHz
  - Refinamento futuro: derivar de `frequenciaMhz` se disponível
- Altura da curva: proporcional ao RSSI (ex.: rssi=-30 → 100%, rssi=-90 → 0%)
- Cor: por rede, usando paleta fixa (accent para rede própria, sequência de cores para as demais)

**Algoritmo para desenhar uma gaussiana no Canvas:**

```kotlin
// Para cada rede com canal e rssi não-nulos:
// - centerX = posição do canal no eixo X
// - heightFraction = (rssi + 90).coerceIn(0, 70) / 70f
// - halfWidthInChannels = 2 (2.4GHz) ou 4 (5GHz/6GHz)
// - Gerar N pontos ao longo do eixo X e calcular Y gaussiano:
//   y(x) = heightFraction * exp(-0.5 * ((x - center) / sigma)^2)
//   onde sigma = halfWidthInChannels / 2.355 (conversão FWHM -> sigma)
// - Conectar pontos com drawPath (Path.moveTo + lineTo)
// - Preencher área abaixo com alpha 0.3
```

**Paleta de cores para as redes:**

```kotlin
private val SPECTRUM_COLORS = listOf(
    Color(0xFF4FC3F7), // azul claro
    Color(0xFFAED581), // verde claro
    Color(0xFFFFB74D), // laranja
    Color(0xFFBA68C8), // roxo
    Color(0xFFFF8A65), // salmon
    Color(0xFF4DB6AC), // teal
    Color(0xFFE57373), // vermelho suave
    Color(0xFFF06292), // pink
)
// Rede própria (seuSSID): LkColors.accent
```

**Legenda:**
- Abaixo do canvas, lista horizontal scrollável com chips: [cor] SSID
- Limitar a exibição: máximo 8 redes na legenda, ordenadas por RSSI desc
- SSIDs nulos: exibir como "Oculta"

**Eixo X:**
- Mostrar apenas os canais que existem na banda selecionada (mesmo vetor de `canaisBase` do engine)
- Espaçamento uniforme

**Eixo Y:**
- Gridlines em -30, -50, -70 dBm (manter como estava)

**Fallback:** se `redesRaw` estiver vazio, mostrar o gráfico de barras original (ou empty state "Sem redes visíveis").

#### 5.4 — Estrutura do composable

```kotlin
@Composable
private fun SpectrumChart(
    espectro: SnapshotEspectroCanal,
    redesRaw: List<RedeVizinha> = emptyList(),
) {
    val c = LocalLkTokens.current
    // ... cores e textMeasurer

    // Preparar lista de redes com canal não-nulo, ordenadas por rssi desc
    val redesParaDesenhar: List<RedeParaEspectro> = remember(redesRaw, espectro.banda) {
        redesRaw
            .filter { it.canal != null && it.rssiDbm != null }
            .sortedByDescending { it.rssiDbm }
            .take(20) // limite para performance
            .mapIndexed { idx, rede ->
                val isSua = ... // comparar com seuSSID (precisa chegar via parâmetro ou espectro)
                RedeParaEspectro(
                    ssid = rede.ssid ?: "Oculta",
                    canal = rede.canal!!,
                    rssiDbm = rede.rssiDbm!!,
                    cor = if (isSua) accentColor else SPECTRUM_COLORS[idx % SPECTRUM_COLORS.size],
                    isSua = isSua,
                )
            }
    }

    Column(...) {
        // Legenda horizontal scrollável
        LegendaRedes(redesParaDesenhar, c)

        Spacer(...)

        // Canvas gaussiano
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            // ... desenhar curvas
        }
    }
}

private data class RedeParaEspectro(
    val ssid: String,
    val canal: Int,
    val rssiDbm: Int,
    val cor: Color,
    val isSua: Boolean,
)
```

**Sobre `seuSSID`:** o composable `SpectrumChart` não tem esse dado hoje. Opções:
- Passar como parâmetro: `seuSSID: String? = null` (preferido — mais explícito)
- Derivar de `espectro` (não armazena)

**Decisão:** adicionar `seuSSID: String?` à assinatura de `SpectrumChart` e passar `connectedNetwork?.ssid` no call site.

#### 5.5 — Call site final

```kotlin
SpectrumChart(
    espectro = espectro,
    redesRaw = redesBanda,
    seuSSID = connectedNetwork?.ssid,
)
```

**Critério de aceite:**
- Cada rede visível tem uma curva gaussiana com cor única
- Curva da rede conectada usa `LkColors.accent`
- Altura proporcional ao RSSI (rede mais forte = curva mais alta)
- Legenda mostra: ponto colorido + SSID (ou "Oculta") para até 8 redes
- Gridlines Y em -30, -50, -70 dBm
- Canal atual marcado no eixo X com accent
- Se `redesRaw` vazio: fallback com empty state "Sem redes visíveis"

**Testes:** testar com 0, 1, 3 e 10+ redes. Testar com SSIDs duplicados (redes mesh). Performance: não deve dropar frames em listas com 30+ redes.

---

## Ordem de execução segura

```
Task 1 → Task 2 → Task 3 → Task 4 → Task 5
```

- **Task 1** e **Task 2** são independentes entre si — podem ser feitas em paralelo se houver dois agentes, mas afetam seções distintas do mesmo arquivo. Sequencial é mais seguro.
- **Task 3** deve vir antes da **Task 4** — ambas tocam na LazyColumn da `CanalTab` e uma limpeza do card "Canal atual" antes de redesenhar `ChannelItem` evita conflitos de merge.
- **Task 5** é a mais complexa e pode ser feita por último sem bloquear as anteriores.

---

## Riscos e cuidados

| Risco | Probabilidade | Mitigação |
|---|---|---|
| `selectedBanda == "Todos"` em `computarEspectro` gera canais misturados 2.4+5+6GHz no gráfico | Média | Documentar como comportamento esperado; filtrar o gráfico por banda quando "Todos" selecionado (mostrar aviso) |
| Canvas de gaussianas com 30+ redes pode causar jank | Média | Limitar a 20 redes no cálculo; usar `remember` corretamente |
| `ChannelItem` redesenhado quebra click (BottomSheet) | Baixa | Manter `onClick` e `minimumInteractiveComponentSize` |
| `mostrarTodasRedes` não reseta no pull-to-refresh | Baixa | Linkar o reset ao `isRefreshing` via `LaunchedEffect(isRefreshing)` |
| SSID nulo (rede oculta) no gráfico gaussiano | Média | Exibir como "Oculta" — já mapeado |
| Rede com `canal == null` ou `rssiDbm == null` causa NPE no gráfico | Alta | `filter { it.canal != null && it.rssiDbm != null }` obrigatório antes de qualquer operação |
| `seuSSID` não chegando ao `SpectrumChart` → rede conectada sem destaque | Média | Adicionar `seuSSID: String?` como parâmetro explícito |

---

## Arquivos modificados

| Arquivo | Modificação |
|---|---|
| `app/src/main/kotlin/io/linka/app/kotlin/ui/screen/SinalScreen.kt` | Todas as 5 tasks — único arquivo afetado |

Nenhum arquivo de domínio, nenhum ViewModel, nenhuma data class precisa ser alterado.

---

## Gatilhos da Lia

Lia é obrigatória nas tasks 2, 3, 4 e 5:
- **Task 2:** novo chip "Todos" no filtro — microcopy e cor do estado ativo
- **Task 3:** banner warning — microcopy "Canal congestionado" e contagem de redes
- **Task 4:** redesenho completo do `ChannelItem` — layout, badges, barra de progresso
- **Task 5:** novo gráfico gaussiano — legenda, cores, eixos, estado vazio

Lia entra em dois momentos:
1. Antes da implementação: validar microcopy, cores e estados visuais mapeados
2. Após implementação: junto com Gema, confirmar alinhamento visual com mockup

---

## Resumo executivo para Camilo

Arquivo único: `SinalScreen.kt`. Nenhuma mudança de domínio.

**5 tasks em ordem:**
1. `RedesTab` — `mostrarTodasRedes` state + take(5) + TextButton "Mostrar Mais"
2. `CanalTab` — adicionar "Todos" em `bandasDisponiveis`, ajustar `redesBanda` e label do espectro
3. `CanalTab` — remover card "Canal atual" (1826-1884), adicionar `CanalCongestionadoBanner` condicional
4. `ChannelItem` — redesenho completo para barra horizontal + badges + header "USO POR CANAL"
5. `SpectrumChart` — novo parâmetros `redesRaw` e `seuSSID`, implementar curvas gaussianas no Canvas
