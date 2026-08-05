## Contexto
O arquivo `key.properties` está presente no working tree contendo `storePassword` e `keyPassword` em plaintext. Mesmo listado em `.gitignore:17`, qualquer commit histórico que o tenha incluído compromete o keystore de assinatura do app — qualquer pessoa com acesso ao histórico consegue assinar APKs como SIGNALLQ.

## Evidência
- `key.properties` (raiz) — credenciais em texto puro
- `.gitignore:17` — entrada existe, mas precisa validar histórico
- Política atual de release: `docs/GuiaReleaseBuild.md`

## Critério de aceite
- [ ] `git log --all --full-history -- key.properties` retorna vazio (ou foram purgados via `git filter-repo`)
- [ ] `key.properties` removido do disco em ambientes versionados; mantido só localmente
- [ ] Se houve qualquer exposição: keystore rotacionado e novo upload key registrado no Play Console
- [ ] `key.properties.template` documentado em `docs/GuiaReleaseBuild.md` como única referência versionada
- [ ] Documentado em `docs_ai/operations/` o fluxo: dev local usa arquivo, CI usa env vars/secrets

## Como verificar
```powershell
git log --all --full-history -- key.properties
git ls-files | Select-String key.properties
.\scripts\build-apk-release.ps1   # deve falhar com mensagem clara se key.properties ausente
```

## Notas para o agente
- Skills: `signallq-arch`, `signallq-docs`
- NÃO commitar nenhum arquivo com senha
- Se for necessário purgar histórico: avisar usuário antes (operação destrutiva no Git)
- Dependências: nenhuma
