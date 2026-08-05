## Contexto
~10 instâncias de `contentDescription = null` em componentes Compose, sem auditoria de quais são decorativos vs funcionais. Sem essa distinção, TalkBack ou anuncia ruído ou perde elementos importantes. Não há evidência de testes de acessibilidade.

## Evidência
- Varredura: `Select-String -Path **/*.kt -Pattern 'contentDescription\s*=\s*null'`
- Telas a auditar primeiro: Home, Speedtest, Diagnóstico, Sinal

## Critério de aceite
- [ ] Cada `contentDescription = null` justificado por comentário curto OU substituído por string descritiva
- [ ] Todos os ícones funcionais (botões, ações em cards) com `contentDescription` vindo de `strings.xml`
- [ ] Targets touch ≥ 48dp validados
- [ ] Contraste mínimo WCAG AA validado para texto secundário sobre `linkaBlack`
- [ ] Rodada de Accessibility Scanner nas 5 rotas principais — relatório anexado ao PR
- [ ] Pelo menos 1 fluxo completo navegável via TalkBack (Home → Speedtest → resultado)

## Como verificar
```powershell
.\gradlew.bat lint
# instalar Accessibility Scanner no device e rodar nas rotas principais
```

## Notas para o agente
- Skills: `signallq-design`, `signallq-arch`
- Dependências: depende de #10 (strings em xml)
