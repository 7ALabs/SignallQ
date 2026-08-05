## Contexto
`android:usesCleartextTraffic="true"` está habilitado globalmente no `AndroidManifest.xml`, permitindo HTTP em qualquer destino, inclusive em build de release. Isso quebra boas práticas OWASP MASVS e expõe o app a MITM. O app provavelmente só precisa de cleartext para gateways locais (192.168.x.x) durante diagnóstico Wi-Fi/Fibra.

## Evidência
- `app/src/main/AndroidManifest.xml:28` — atributo global `usesCleartextTraffic="true"`
- Features que provavelmente exigem cleartext: `featureFibra`, `featureWifi`, `featureDiagnostico`

## Critério de aceite
- [ ] Criar `app/src/main/res/xml/network_security_config.xml` permitindo cleartext APENAS em `domain-config` de IPs locais (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) e `localhost`
- [ ] Remover `usesCleartextTraffic="true"` do manifest e referenciar `android:networkSecurityConfig="@xml/network_security_config"`
- [ ] Build de release passa sem cleartext global
- [ ] Acesso HTTPS continua funcionando; HTTP para gateways locais continua funcionando; HTTP para domínio público falha
- [ ] Documentar a política em `docs_ai/technical/`

## Como verificar
```powershell
.\gradlew.bat assembleRelease
# Smoke test manual: rodar speedtest, diagnóstico Wi-Fi, varredura DNS
```
Inspecionar APK: `apkanalyzer manifest print app/build/outputs/apk/release/app-release.apk | Select-String cleartext`

## Notas para o agente
- Skills: `signallq-arch`, `signallq-docs`
- Arquivos a tocar: `AndroidManifest.xml`, novo `res/xml/network_security_config.xml`
- Dependências: nenhuma; pode rodar em paralelo com #1
