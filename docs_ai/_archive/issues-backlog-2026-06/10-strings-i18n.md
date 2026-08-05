## Contexto
Strings de UI estão hardcoded em Composables, impedindo internacionalização e dificultando revisão de copy. Ex.: cards de sugestão do SignallQ têm labels fixos no Kotlin. O design system já é consistente; falta apenas externalizar texto.

## Evidência
- `app/src/main/kotlin/io/signallq/app/kotlin/ui/component/OrbitWelcomeState.kt:46-61` — "Internet lenta", "Streaming ruim", etc.
- Varredura: `Select-String -Path **/*.kt -Pattern 'Text\("[^"]+"\)|"[A-Z][a-zà-ú ]{3,}"'`

## Critério de aceite
- [ ] Todas as strings visíveis ao usuário movidas para `res/values/strings.xml` (português, default)
- [ ] Convenção de naming: `<screen>_<element>_<purpose>` (ex.: `home_card_internet_lenta_title`)
- [ ] Stub `res/values-en/strings.xml` criado (mesmo que vazio) para preparar i18n
- [ ] Plurals e formatted strings usando `quantityString` e `getString(R.string.x, arg)`
- [ ] Lint Android `HardcodedText` ativo como `error`
- [ ] Acessibilidade: `contentDescription` de ícones funcionais via strings.xml

## Como verificar
```powershell
.\gradlew.bat lint
# 0 warnings de HardcodedText
```
Mudar locale do device para EN no debug → telas com stub mostram chaves (esperado nesta fase).

## Notas para o agente
- Skills: `signallq-design`, `signallq-arch`
- Manter padrão LkTokens; não criar nova camada de tema
- Dependências: facilita #11 (acessibilidade)
