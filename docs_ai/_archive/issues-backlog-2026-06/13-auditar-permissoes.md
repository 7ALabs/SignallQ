## Contexto
12 permissões declaradas no `AndroidManifest.xml`. Algumas podem estar mortas após pivots de feature, e cada permissão extra reduz instalação (usuários reagem mal a "por que precisa de localização?"). Audit confirma uso real e remove o supérfluo.

## Evidência
- `app/src/main/AndroidManifest.xml:3-19` — INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, CHANGE_WIFI_STATE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, READ_PHONE_STATE, POST_NOTIFICATIONS, etc.

## Critério de aceite
- [ ] Tabela permissão → uso (linha:arquivo) → feature que depende dela, anexada ao PR
- [ ] Permissões sem uso real removidas do manifest
- [ ] Para cada permissão runtime mantida: validar fluxo de request via `corePermissions`, com rationale claro
- [ ] `POST_NOTIFICATIONS` solicitada apenas quando o usuário entra em fluxo que precisa
- [ ] Documentar em `docs_ai/technical/PERMISSIONS.md` (criar se não existir)

## Como verificar
```powershell
foreach ($perm in @("ACCESS_FINE_LOCATION","READ_PHONE_STATE","ACCESS_WIFI_STATE")) {
  Write-Host "=== $perm ==="
  Get-ChildItem -Recurse -Include *.kt | Select-String -Pattern $perm
}
```

## Notas para o agente
- Skills: `signallq-arch`, `signallq-docs`
- Dependências: independente
