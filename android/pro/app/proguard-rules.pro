# Regras de ProGuard/R8 do SignallQ Pro (:pro:app).

# androidx.security:security-crypto (via :core:datastore/CredenciaisModemStore) traz
# com.google.crypto.tink, que referencia anotacoes com.google.errorprone.annotations so em
# nivel de compilacao (nao existem em runtime). Sem essa regra o R8 trata como erro fatal em
# vez de warning. Padrao conhecido do proprio Google Tink, nao e classe realmente ausente.
-dontwarn com.google.errorprone.annotations.**
