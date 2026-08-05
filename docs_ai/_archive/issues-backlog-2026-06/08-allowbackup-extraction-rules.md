## Contexto
`android:allowBackup="true"` está habilitado sem `dataExtractionRules` ou `fullBackupContent`. Isso permite que Room (banco com histórico de medições) e DataStore (preferências, possivelmente IDs de dispositivo) sejam extraídos via `adb backup` ou backup automático Google. Mesmo sem credenciais, pode expor PII e histórico de uso.

## Evidência
- `app/src/main/AndroidManifest.xml:23` — `android:allowBackup="true"` sem regras
- Bases sensíveis: Room (`coreDatabase`) e DataStore (`coreDatastore`)

## Critério de aceite
- [ ] Criar `res/xml/data_extraction_rules.xml` (Android 12+) excluindo Room e DataStore de cloud + device transfer
- [ ] Criar `res/xml/full_backup_content.xml` (compatibilidade) com mesmas exclusões
- [ ] Referenciar ambos no manifest: `android:dataExtractionRules`, `android:fullBackupContent`
- [ ] Decidir e documentar: manter `allowBackup="true"` (com regras) OU passar para `false` se UX de restore não for prioridade
- [ ] Documentar decisão em ADR (`docs_ai/technical/adr/`)

## Como verificar
```powershell
.\gradlew.bat assembleDebug
adb backup -f test.ab io.veloo.app   # após restore, bases sensíveis não devem ter dados
```

## Notas para o agente
- Skills: `signallq-arch`, `signallq-docs`
- Dependências: nenhuma
