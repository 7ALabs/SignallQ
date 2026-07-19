package io.signallq.pro.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Material3 ColorScheme nao modela "sucesso"/"atencao" (so primary/secondary/tertiary/error) --
// mas a semantica de status verde/ambar/vermelho e nao-negociavel do design system (ver
// .claude/CLAUDE.md, secao Design System). Valores 1:1 com ProSuccess/ProSuccessDark/ProWarning
// (io.signallq.pro.ui.theme.SignallQProColor, :pro:app) -- duplicados aqui porque um componente
// de :core:designsystem nao pode depender de :app (regra de dependencia, ver
// .claude/rules/higiene-e-padronizacao-repositorio.md, secao 5).
private val ProStatusSucessoClaro = Color(0xFF1AA25A)
private val ProStatusSucessoEscuro = Color(0xFF32D978)
private val ProStatusAtencaoClaro = Color(0xFFE9AD27)

@Composable
fun corStatusSucesso(): Color = if (isSystemInDarkTheme()) ProStatusSucessoEscuro else ProStatusSucessoClaro

@Composable
fun corStatusAtencao(): Color = ProStatusAtencaoClaro
