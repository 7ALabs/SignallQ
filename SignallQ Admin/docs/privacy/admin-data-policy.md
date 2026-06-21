# Política de Segurança e Privacidade de Dados — SignallQ Admin

Para garantir a integridade dos dados e a privacidade dos usuários finais, todos os processos analíticos, diagnósticos e integrações do SignallQ seguem diretrizes de conformidade jurídica internacional (incluindo LGPD e GDPR).

## 1. Princípios de Coleta de Telemetria

Os dados operacionais de diagnóstico e conectividade vindos do app Android e processados por Cloudflare Workers não contêm dados pessoalmente identificáveis (PII).

### 1.1 Minimização e Ofuscação de Rede
*   **SSID (Identificador de Rede Sem Fio):** O SSID completo nunca é transmitido ou gravado de forma legível no banco de dados operacional. No tráfego interno, utiliza-se apenas o tipo genérico (`Wi-Fi (Rede Local)`) ou hashes criptográficos irreversíveis para identificação de hotspots corporativos.
*   **BSSID / MAC Address:** Não coletados pelo SDK do SignallQ.
*   **Endereço IP:** O IP público completo é retido apenas temporariamente na borda do Cloudflare Worker para balanceamento e geo-IP (resolução de Cidade e Estado), sendo descartado imediatamente após as agregação estatísticas. Nenhum IP completo é armazenado em logs ou exibido no console do SignallQ Admin.
*   **Localização Exata:** Informações de coordenadas GPS (`Latitude` e `Longitude`) de alta precisão não são enviadas. Toda a segmentação territorial é baseada em referências geográficas agregadas a nível de país, estado e cidade, recuperadas de forma segura na própria borda via requisições HTTP.

### 1.2 Tratamento de Logs e Stacktraces
*   **Hash de Stacktrace:** Exceções do SDK Android e códigos de erro são agregados via hashes de pile (`stackHash`). Isso previne que caminhos locais do dispositivo do usuário ou variáveis em memória poluam a base central de auditorias.
*   **Identificadores Anônimos (`anonymousUserId`):** Toda a jornada de uso é correlacionada utilizando um token de instalação único gerado pelo próprio sistema operacional (`UUID`). Esse token não pode ser ligado à identidade civil, e-mail, telefone ou contas Google sem aprovação prévia expressa do usuário.

---

## 2. Abstração de Credenciais e Isolamento de Front-end

Uma das premissas de arquitetura e conformidade premium do **SignallQ Admin** é o isolamento completo do navegador da Web:

```
Navegador Web (SignallQ Admin UI)
       ↓ (NÃO acessa chaves de terceiros diretamente!)
SignallQ Admin API / Cloudflare Worker (Processa nos bastidores)
       ↓
Firebase Suite / Google Play Developer APIs / Provedores IA (Chaves seguras)
```

1.  **Segredos Protegidos:** O front-end do painel administrativo não se autentica diretamente com o SDK do Firebase, chaves do Gemini, ou contas de serviço do Google Play Console.
2.  **API Gateways:** Todas as chamadas para APIs de parceiros e ferramentas são gerenciadas de forma segura no lado do servidor (Server-Side Backend) por meio de variables de ambiente protegidas em contêineres Cloud Run ou segredos do Cloudflare Workers encabeçados pela `Admin API`.
3.  **Cross-Origin Isolation:** Não expor credenciais em cookies expostos ou no código fonte compilado da UI (`import.meta.env`).

---

## 3. Classificação e Governança de Dados

A base de dados do SignallQ separa estritamente os dados agregados dos dados transacionais de debugging:

| Categoria | Descrição | Retenção recomendada | Destinatários |
| :--- | :--- | :--- | :--- |
| **Diagnósticos Agregados** | Scores, médias de latência e throughput por cidade/rede | 12 meses | Engenharia de Redes |
| **Exceções Técnicas** | Eventos de crash descartados do Crashlytics | 30 dias | Desenvolvedores Android |
| **Medições Operacionais** | Logs instantâneos de erros de rota HTTP e gateways | 14 dias | DevOps SRE |
