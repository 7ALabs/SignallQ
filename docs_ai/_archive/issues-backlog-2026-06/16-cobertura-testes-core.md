## Contexto
69 arquivos em `src/test` + `androidTest` para um projeto com 15+ módulos é baixo. Os módulos `core*` praticamente não têm testes, justamente onde regressões doem mais (migrations de Room, parsing de rede, contratos de DataStore).

## Evidência
- `coreDatabase/src/test/` — pouca cobertura
- `coreNetwork/src/test/` — pouca cobertura
- `coreDatastore/src/test/` — pouca cobertura

## Critério de aceite
- [ ] Cobertura em `coreDatabase`, `coreNetwork`, `coreDatastore` ≥ 60% (JaCoCo)
- [ ] Testes de migration Room para cada upgrade de schema histórico (usar `MigrationTestHelper`)
- [ ] Testes de contrato (request/response) para cada endpoint em `coreNetwork`, usando MockWebServer
- [ ] Testes de DataStore: leitura, escrita, default, corrupção
- [ ] JaCoCo configurado com task agregada `./gradlew jacocoTestReport`
- [ ] CI publica relatório de cobertura como artifact

## Como verificar
```powershell
.\gradlew.bat :coreDatabase:test :coreNetwork:test :coreDatastore:test jacocoTestReport
```

## Notas para o agente
- Skills: `signallq-arch`
- Não inflar com testes triviais de getters; foco em comportamento crítico (migrations, parsing, error paths)
- Dependências: facilita debug futuro de #3, #6, #7
