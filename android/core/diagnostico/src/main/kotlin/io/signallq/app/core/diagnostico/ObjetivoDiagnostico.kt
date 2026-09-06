package io.signallq.app.core.diagnostico

/**
 * Os 4 macro-objetivos do diagnóstico guiado (Sanitização - 2026-08).
 * A camada de perguntas de triagem foi removida para reduzir a carga cognitiva, 
 * unificando sintomas que disparam as mesmas métricas-chave no motor.
 */
enum class ObjetivoDiagnostico(
    val titulo: String,
    val subtitulo: String,
) {
    INSTABILIDADE_QUEDAS(
        titulo = "A internet cai ou fica instável",
        subtitulo = "A conexão para de funcionar ou o sinal fica sumindo.",
    ),
    LENTIDAO_GERAL(
        titulo = "A internet está lenta",
        subtitulo = "Tudo demora para carregar ou a velocidade está abaixo do plano.",
    ),
    PROBLEMAS_VIDEO_JOGOS(
        titulo = "Problemas com vídeo, áudio ou jogos",
        subtitulo = "Vídeos travam, chamadas congelam ou lag nas partidas.",
    ),
    OUTRO_PROBLEMA(
        titulo = "Outro problema",
        subtitulo = "Descreva com suas palavras o que está acontecendo.",
    ),
}

