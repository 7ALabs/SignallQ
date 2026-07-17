> **Arquivado em 2026-07-16.** Consolidacao de documentacao docs_ai/ (branch docs/consolidacao-documentacao-docs-ai). Registro de migracao ja concluida (2026-06-21). Preservado apenas como referencia historica.

---

# Cache de IA com Expiração (TTL 5 min)

**Data:** 2026-06-21 | **Versão:** v0.16.0+ | **Status:** Mergeado para main

---

## Problema

O `AiDiagnosisRepository` armazenava respostas de IA em cache indefinidamente sem expiração. Quando a contexto do diagnóstico era idêntico, o cache devolvia sempre a mesma resposta, mesmo que o usuário esperasse uma análise atualizada após minutos passarem.

**Impacto:** Métricas recém-coletadas (download, latência, etc.) eram ignoradas se um diagnóstico anterior com contexto similar tinha sido executado — usuário via diagnóstico antigo sem saber.

---

## Solução

Implementado **TTL (Time To Live) de 5 minutos** no cache do `AiDiagnosisRepository`:

- **`ConcurrentHashMap<String, Pair<AiDiagnosisResult, Long>>`** — armazena `(resultado, timestamp de inserção em ms)`.
- **Lookup com validação de expiração** — quando uma entrada é recuperada do cache, verifica se `elapsed = clock() - timestamp > CACHE_TTL_MS (300_000ms)`. Se expirado, remove e executa requisição nova.
- **Clock injetável** — `clock: () -> Long = System::currentTimeMillis` permite testes determinísticos sem `Thread.sleep()`.
- **Acesso `internal` ao cache** — `cache` e `cacheKey()` marcados `internal` para permitir injeção e validação em testes unitários.

---

## Arquivos Modificados

### 1. `featureDiagnostico/src/main/kotlin/io/veloo/app/kotlin/feature/diagnostico/ai/AiDiagnosisRepository.kt`

**Mudanças principais:**

```kotlin
// Linha 60: Constante TTL adicionada
companion object {
    private const val TAG = "AiDiagnosisRepository"
    private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutos
}

// Linha 56: Clock injetável
internal val clock: () -> Long = System::currentTimeMillis

// Linha 74: Cache agora armazena Pair(resultado, timestamp)
internal val cache = ConcurrentHashMap<String, Pair<AiDiagnosisResult, Long>>()

// Linhas 116–123: Validação de expiração no explainDiagnosis()
cache[key]?.let { (result, timestamp) ->
    if (clock() - timestamp > CACHE_TTL_MS) {
        cache.remove(key)
    } else {
        return AiDiagnosisState.success(result.copy(source = "cache"))
    }
}

// Linha 162: Inserção com timestamp ao cachear resultado
cache[key] = Pair(normalized, clock())
```

---

### 2. `featureDiagnostico/src/test/kotlin/io/veloo/app/kotlin/feature/diagnostico/ai/AiDiagnosisRepositoryTest.kt`

**Mudanças principais:**

- **Testes de TTL** — verificam expiração e remoção de entradas antigas.
- **Testes com clock mock** — injetam clock customizado para simular passagem de tempo sem esperar 5 min.
- **Cobertura de cenários:**
  - Cache hit dentro do TTL → devolve cached, não chama API.
  - Cache miss (expirado) → remove entrada, chama API normalmente.
  - Clock injetável com valores diferentes → valida lógica de comparação.

---

## Como Validar que Funciona

### 1. **Cache Hit (5 min)**

```kotlin
// Testar localmente
val repo = AiDiagnosisRepository(
    baseUrl = "http://worker...",
    isAuthorized = { true },
    clock = { /* mock clock */ }
)

// Primeira chamada: executa API
val result1 = repo.explainDiagnosis(context, fallback = { ... })

// Segunda chamada (< 5 min depois): cache hit
val result2 = repo.explainDiagnosis(context, fallback = { ... })
// result2.source == "cache" ✓

// Terceira chamada (> 5 min depois): cache miss
val result3 = repo.explainDiagnosis(context, fallback = { ... })
// result3.source == "cloudflare_ai" ✓ (nova requisição)
```

### 2. **Verificar Logs**

Em device, abra Logcat e filtre por `AiDiagnosisRepository`:

```
D/AiDiagnosisRepository: IA respondeu com sucesso: status=regular modelo=Gemma 4 26B
D/AiDiagnosisRepository: <segundos depois> IA respondeu com sucesso...
```

Se aparecer dois logs similares com curto intervalo de tempo, o cache estava expirado e a API foi chamada novamente ✓

### 3. **Teste Unitário**

```bash
./gradlew :featureDiagnostico:test \
  -Dtest.single=AiDiagnosisRepositoryTest
```

Confirma que todos os cenários de TTL passam (cache valid, expired, etc.).

### 4. **QA Manual**

1. Abra Diagnóstico na tela inicial.
2. Aguarde resultado da IA (ex: "status=regular").
3. Volte para Home, depois retorne ao Diagnóstico **dentro de 1–2 minutos**.
4. Se a tela exibir o mesmo resultado **instantaneamente** (sem loading) → cache hit ✓
5. Aguarde **5 minutos+**, retorne.
6. Tela mostra loading novamente → cache expirado, nova requisição ✓

---

## Implicações Técnicas

- **Compatibilidade:** Nenhuma mudança em interface pública. Cache é interno.
- **Thread-safety:** `ConcurrentHashMap` garante operações atômicas.
- **Memory:** Máximo N entradas (uma por contexto único). Expiração automática ao acessar.
- **Tests:** Clock injetável garante testes rápidos e determinísticos.

---

## Referência

- **Commit 1:** Estrutura base + TTL implementation.
- **Commit 2:** Testes cobertura completa.
- **Feature branch:** `feat/cache-ia-com-expiracao-5min`

