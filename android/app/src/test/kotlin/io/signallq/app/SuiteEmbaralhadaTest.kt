package io.signallq.app

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.model.RunnerBuilder
import java.io.File
import java.lang.reflect.Modifier
import java.util.Random

/**
 * Ferramenta de verificacao para GH#1684 (poluicao entre testes no :app -- um teste
 * vaza corrotina/estado para fora do proprio escopo e derruba outro teste, escolhido
 * ao acaso, com `UncaughtExceptionsBeforeTest`).
 *
 * O Gradle Test task deste modulo (JUnit4 puro, sem `useJUnitPlatform()`) NAO tem
 * flag nativa para embaralhar a ordem de execucao das classes de teste dentro da
 * mesma JVM -- a ordem e uma funcao deterministica da varredura do diretorio de
 * `.class` compilados (essencialmente alfabetica), o que explica por que "adicionar
 * testes muda quem roda do lado de quem" (issue original). Rodar `:app:testDebugUnitTest`
 * varias vezes sem mexer no conjunto de testes reproduz sempre a MESMA ordem -- nao
 * prova nada sobre poluicao dependente de ordem.
 *
 * Esta suite contorna a limitacao sem adicionar dependencia nova (so usa
 * `junit:junit`, ja presente): descobre em runtime todas as classes de teste do
 * modulo a partir do classpath do proprio processo (`suite.embaralhada.classesDirs`,
 * injetado pelo `app/build.gradle.kts`), embaralha com uma seed explicita (impressa
 * no log, para reproduzir uma falha) e roda todas dentro da MESMA JVM via o
 * `JUnitCore` interno do `Suite` -- unico jeito de expor poluicao de estado estatico
 * entre classes (ex.: `kotlinx.coroutines.test.internal.ExceptionCollector`, que e
 * global ao processo, nao por `TestScope`).
 *
 * Uso (o `-PsuiteEmbaralhada` e obrigatorio -- sem ele o `app/build.gradle.kts` exclui esta
 * classe do proprio run, mesmo com `--tests` explicito, pra ela nao dobrar a suite normal):
 * ```
 * ./gradlew :app:testDebugUnitTest --tests "io.signallq.app.SuiteEmbaralhadaTest" --rerun \
 *     -PsuiteEmbaralhada -Dsuite.embaralhada.seed=<long opcional -- omitir sorteia uma nova>
 * ```
 *
 * Nao roda no CI por padrao (excluida do run normal por `app/build.gradle.kts`, precisa do
 * `-PsuiteEmbaralhada` explicito) -- e uma ferramenta de verificacao sob demanda para
 * suspeita de poluicao entre testes, nao um gate adicional (rodar as ~580 classes
 * embaralhadas N vezes custa CI real).
 */
@RunWith(SuiteEmbaralhadaTest.EmbaralhadaRunner::class)
class SuiteEmbaralhadaTest {
    // JUnit4 exige ao menos um metodo de teste na classe anotada com @RunWith(Suite),
    // mesmo quando o runner real (EmbaralhadaRunner) ignora esse metodo e substitui a
    // lista de filhos pelas classes descobertas em runtime.
    @Test
    fun placeholderIgnoradoPeloRunnerReal() = Unit

    class EmbaralhadaRunner(
        @Suppress("UNUSED_PARAMETER") klass: Class<*>,
        builder: RunnerBuilder,
    ) : Suite(builder, descobrirClassesEmbaralhadas())

    companion object {
        private fun descobrirClassesEmbaralhadas(): Array<Class<*>> {
            val seed = System.getProperty("suite.embaralhada.seed")?.toLongOrNull() ?: System.nanoTime()
            val classes = descobrirClassesDeTeste().shuffled(Random(seed))
            println("SuiteEmbaralhadaTest: seed=$seed classes=${classes.size}")
            return classes.toTypedArray()
        }

        private fun descobrirClassesDeTeste(): List<Class<*>> {
            val loader = Thread.currentThread().contextClassLoader
            val raizes =
                System
                    .getProperty("suite.embaralhada.classesDirs")
                    ?.split(File.pathSeparatorChar)
                    ?.map { File(it) }
                    ?.filter { it.exists() }
                    .orEmpty()
            check(raizes.isNotEmpty()) {
                "suite.embaralhada.classesDirs nao configurado -- rode via " +
                    "':app:testDebugUnitTest --tests \"io.signallq.app.SuiteEmbaralhadaTest\"' " +
                    "(a system property e injetada pelo app/build.gradle.kts)."
            }
            val nomesQualificados =
                raizes
                    .flatMap { raiz ->
                        raiz
                            .walkTopDown()
                            .filter { it.isFile && it.extension == "class" && '$' !in it.name }
                            .map { arquivo ->
                                arquivo
                                    .relativeTo(raiz)
                                    .path
                                    .removeSuffix(".class")
                                    .replace(File.separatorChar, '.')
                            }
                    }.distinct()
            return nomesQualificados
                .mapNotNull { nome -> runCatching { Class.forName(nome, false, loader) }.getOrNull() }
                .filter { it != SuiteEmbaralhadaTest::class.java }
                .filter { classe -> !Modifier.isAbstract(classe.modifiers) && !Modifier.isInterface(classe.modifiers) }
                .filter { classe -> classe.methods.any { it.isAnnotationPresent(Test::class.java) } }
        }
    }
}
