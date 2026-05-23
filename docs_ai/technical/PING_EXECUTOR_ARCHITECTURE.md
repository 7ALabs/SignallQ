# PingExecutor — Arquitetura & Implementação

**Módulo:** `:featureSpeedtest`  
**Linguagem:** Kotlin  
**Framework:** OkHttp3, Coroutines  
**Versão:** 0.8.5+  

---

## 1. Componentes

### `PingExecutor` (class)

Executor de pings ICMP sobre HTTP/2, com coleta de 20 amostras + análise estatística.

```kotlin
class PingExecutor {
    suspend fun executar(
        count: Int = 20,
        onProgresso: (Int) -> Unit = {},
    ): PingResultado
}
```

**Responsabilidades:**
- Disparar N requisições GET para `https://speed.cloudflare.com/__down?bytes=0` (sem payload)
- Medir RTT (round-trip time) de cada requisição
- Filtrar outliers (outliers ≤ 3× mediana)
- Calcular latência (mediana), jitter (std dev), perda (%)
- Reportar progresso real-time via callback

**Timeout & Configuração:**

```kotlin
val pingClient: OkHttpClient = OkHttpClient.Builder()
    .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))  // H2 preferido
    .connectTimeout(4, TimeUnit.SECONDS)
    .readTimeout(4, TimeUnit.SECONDS)
    .callTimeout(4, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; SM-A256E) AppleWebKit/537.36")
                .header("Cache-Control", "no-store")
                .build(),
        )
    }
    .build()
```

**Timeout total:** 4s/amostra × 20 = ~80s máximo (com rede lenta).

### `PingResultado` (data class)

```kotlin
data class PingResultado(
    val latenciaMs: Double,      // mediana das amostras válidas
    val jitterMs: Double,        // std dev das amostras válidas
    val perdaPercentual: Double, // (timeouts / amostras_excluindo_1a) * 100
    val amostras: Int,           // total disparado (always 20)
)
```

---

## 2. Fluxo de Execução

```
executar(count=20)
  │
  ├─ 1ª amostra (aquecimento)
  │  └─ descartada (sempre)
  │
  ├─ 2ª-20ª amostra (19 total)
  │  ├─ medirPing() → RTT ou null (timeout)
  │  ├─ reportar progresso (i/count)
  │  └─ acumular em lista
  │
  ├─ Calcular mediana de válidas
  │
  ├─ Filtrar outliers
  │  └─ remover amostras > mediana×3
  │
  ├─ Usar amostras filtradas (ou todas se nenhuma foi filtrada)
  │
  ├─ Calcular:
  │  ├─ latenciaMs = mediana(usadas)
  │  ├─ jitterMs = stdDev(usadas)
  │  └─ perdaPercentual = (timeouts / 19) × 100
  │
  └─ return PingResultado(...)
```

### Cálculos Estatísticos

**Latência:**
```
mediana(amostras_filtradas)  → ms
```

**Jitter:**
```
stdDev = sqrt(Σ(x - média)² / n)  → ms
```

**Perda:**
```
(timeouts / amostras_excluindo_1a) × 100  → %
```

---

## 3. Tratamento de Erros

| Erro | Tratamento |
|------|-----------|
| Timeout (4s) | `medirPing()` retorna `null` → contabiliza como perda |
| DNS inválido | OkHttp falha, exception propagada → `PingScreenState.Erro` |
| Sem internet | Primeiro timeout dispara exception → modal mostra "Sem conexão" |

**UI correspondente:**

```kotlin
sealed interface PingScreenState {
    data object Idle : PingScreenState
    data class Executando(val progresso: Int) : PingScreenState  // 0-100
    data class Resultado(val resultado: PingResultado) : PingScreenState
    data class Erro(val mensagem: String) : PingScreenState
}
```

---

## 4. Performance & Otimizações

| Aspecto | Implementação |
|--------|--------------|
| **Paralelização** | Sequencial (20 pings em série) — evita spike de carga |
| **Caching** | `Cache-Control: no-store` header — força fresh |
| **Protocol** | HTTP/2 preferido (menor latência setup) |
| **Primeiro ping** | Descartado (aquecimento, time de DNS + TLS) |
| **Filtro outliers** | 3× mediana — remove picos de latência (ex: GC) |

**Tempo esperado:**
- Latência baixa (20ms): ~20s total
- Latência alta (150ms): ~30s total
- Com timeouts: até 80s

---

## 5. Integração com ViewModel

### `PingScreenViewModel`

```kotlin
class PingScreenViewModel {
    private val mutableStateFlow = MutableStateFlow<PingScreenState>(PingScreenState.Idle)
    val stateFlow: StateFlow<PingScreenState> = mutableStateFlow.asStateFlow()

    suspend fun executarPing() {
        try {
            mutableStateFlow.value = PingScreenState.Executando(0)
            val executor = PingExecutor()
            val resultado = executor.executar(count = 20) { progresso ->
                mutableStateFlow.value = PingScreenState.Executando(progresso)
            }
            mutableStateFlow.value = PingScreenState.Resultado(resultado)
        } catch (e: Exception) {
            mutableStateFlow.value = PingScreenState.Erro(e.message ?: "Erro ao executar ping")
        }
    }

    fun resetar() {
        mutableStateFlow.value = PingScreenState.Idle
    }
}
```

---

## 6. Testes Unitários

### PingExecutor

```kotlin
@Test
fun testExecutarComAmostraValida() = runTest {
    val executor = PingExecutor()
    val resultado = executor.executar(count = 5)
    
    assert(resultado.latenciaMs > 0)
    assert(resultado.jitterMs >= 0)
    assert(resultado.perdaPercentual in 0.0..100.0)
    assert(resultado.amostras == 5)
}

@Test
fun testProgressoCallback() = runTest {
    val executor = PingExecutor()
    val progressos = mutableListOf<Int>()
    
    executor.executar(count = 5) { p ->
        progressos.add(p)
    }
    
    assert(progressos.last() == 5)  // 0-indexed → última = count
}
```

---

## 7. Endpoints Testados

| Provedor | URL | Status |
|----------|-----|--------|
| Cloudflare Speed | `https://speed.cloudflare.com/__down?bytes=0` | ✓ Testado |

**Por quê Cloudflare?**
- Sem autenticação
- Geográfico distribuído globalmente
- Payload zero (`bytes=0`) = latência pura
- Cache-friendly headers aceitos

---

## 8. Conhecidos Limitações

| Limitação | Motivo | Mitigação |
|-----------|--------|-----------|
| Não é ICMP "real" | Android não permite ICMP raw | HTTP proxy é padrão em tools |
| Timeouts em rede ruim > 80s | 4s × 20 amostras | User pode interromper (BackHandler) |
| Primeira amostra sempre descartada | Aquecimento (DNS/TLS) | Implementação padrão |
| Sem DNS público hardcoded | Seguir resolução local | Respeita Private DNS do device |

---

## 9. Changelog da Feature

**v0.8.5:**
- [NOVO] PingExecutor (20 amostras, OkHttp2)
- [NOVO] PingResultado (data class)
- [NOVO] PingScreen + PingScreenViewModel
- [NOVO] Integração em Central de Testes (grid)

