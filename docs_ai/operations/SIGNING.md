# App Signing — Keystore e Credenciais

## Visão geral

O Linka Android usa assinatura de release para distribuir builds assinados em produção. Todas as credenciais (senhas, alias, keystore) ficam **fora do git** — nunca são comitadas nem expostas no repositório.

### Status de segurança

- `key.properties` está em `.gitignore` (linha 17)
- `*.jks` (keystore) está em `.gitignore` (linha 18)
- `segredos/` está em `.gitignore` (linha 38)
- Keystore nunca foi comitada ou exposta
- Histórico do repositório está limpo

## Estrutura local

O keystore fica organizado assim:

```
C:\Projetos\Linka Android\
├── segredos/
│   └── linka.jks              # ← Keystore local, NÃO vai ao git
├── key.properties             # ← Credenciais locais, NÃO vai ao git
└── key.properties.template    # ← Template sem credenciais, RASTREADO no git
```

## Setup local — Primeira vez

### 1. Copiar template

```powershell
cd "C:\Projetos\Linka Android"
Copy-Item key.properties.template key.properties
```

### 2. Preencher credenciais

Edite `key.properties` e preencha os 4 campos:

```properties
storePassword=SUA_SENHA_DO_KEYSTORE
keyPassword=SUA_SENHA_DA_CHAVE
keyAlias=linka
storeFile=segredos/linka.jks
```

**Nota:** `keyAlias` é sempre `linka` (fixo). Os dois campos de senha vêm de quem controla o keystore.

### 3. Colocar keystore

O arquivo `segredos/linka.jks` já deve estar disponível localmente (transferido de forma segura, não via git).

```
C:\Projetos\Linka Android\segredos\linka.jks
```

Se ainda não existe, veja seção "Gerar novo keystore" abaixo.

## Como funciona (build.gradle.kts)

O script de build (`app/build.gradle.kts`) carrega `key.properties` assim:

```kotlin
private val keyPropertiesFile = rootProject.file("key.properties")
private val keyProperties = Properties().apply {
    if (keyPropertiesFile.exists()) load(keyPropertiesFile.inputStream())
}
```

E usa as credenciais para configurar o signing do release:

```kotlin
signingConfigs {
    create("release") {
        if (keyPropertiesFile.exists()) {
            keyAlias = keyProperties["keyAlias"] as String
            keyPassword = keyProperties["keyPassword"] as String
            storeFile = keyProperties["storeFile"]?.let { rootProject.file(it as String) }
            storePassword = keyProperties["storePassword"] as String
        }
    }
}

buildTypes {
    release {
        if (keyPropertiesFile.exists()) {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

Se `key.properties` não existir, o build de release falha (como esperado).

## Build release assinado

Com `key.properties` e `segredos/linka.jks` presentes e preenchidos:

```powershell
.\scripts\build-apk-release.ps1
```

ou:

```powershell
.\gradlew.bat archiveReleaseApk
```

O APK assinado sai em:

```
builds\apk\release\<versionName>\linka-android-v<versionName>+<versionCode>-release-<timestamp>.apk
```

## CI/CD futuro — GitHub Secrets

Para automatizar release builds em CI (GitHub Actions), você precisará criar 4 GitHub Secrets no repositório:

| Secret                | Valor                                |
|-----------------------|--------------------------------------|
| `KEYSTORE_BASE64`     | Arquivo `linka.jks` em base64        |
| `KEY_ALIAS`           | `linka`                              |
| `KEY_PASSWORD`        | Senha da chave privada               |
| `STORE_PASSWORD`      | Senha do keystore                    |

O workflow CI vai:

1. Decodificar `KEYSTORE_BASE64` de volta para `linka.jks`
2. Criar `key.properties` com os secrets
3. Rodar `./gradlew.bat archiveReleaseApk`
4. Assinar e distribuir o APK

### Para gerar KEYSTORE_BASE64

```powershell
$bytes = [System.IO.File]::ReadAllBytes("C:\Projetos\Linka Android\segredos\linka.jks")
$base64 = [System.Convert]::ToBase64String($bytes)
Write-Output $base64 | Set-Clipboard
```

Cole o valor em GitHub Secrets → Repository secrets → `KEYSTORE_BASE64`.

## Gerar novo keystore (se necessário)

Se não tiver um keystore existente, crie um com:

```powershell
$keystorePath = "C:\Projetos\Linka Android\segredos\linka.jks"
$storePassword = "SENHA_FORTE_AQUI"
$keyPassword = "SENHA_DA_CHAVE_AQUI"

keytool -genkey `
    -alias linka `
    -keyalg RSA `
    -keysize 2048 `
    -keystore $keystorePath `
    -validity 10000 `
    -storepass $storePassword `
    -keypass $keyPassword `
    -dname "CN=Linka, O=Linka, C=BR"
```

Depois preencha `key.properties` com essas senhas e o alias `linka`.

## Segurança — Checklist

- [ ] `key.properties` **nunca** é commitado (verificar `.gitignore`)
- [ ] `*.jks` **nunca** é commitado (verificar `.gitignore`)
- [ ] `segredos/` **nunca** é commitado (verificar `.gitignore`)
- [ ] `key.properties.template` **é** rastreado (serve como referência)
- [ ] Senhas do keystore não são compartilhadas em plain text no git/email
- [ ] Credenciais CI (GitHub Secrets) são criadas apenas quando necessário automatizar
- [ ] Keystore local é protegido em sistema de arquivos (permissões)

## Validação

Depois de um build release bem-sucedido, valide a assinatura:

```powershell
$apk = "builds\apk\release\<versionName>\linka-android-v<versionName>+<versionCode>-release-<timestamp>.apk"
jarsigner -verify $apk
```

Saída esperada:

```
jar verified.
```

## Referências

- `app/build.gradle.kts` — configuração de signing
- `docs/GuiaReleaseBuild.md` — fluxo completo de release
- `.gitignore` — arquivos sempre ignorados
