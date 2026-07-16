---
name: reconhecimento-equipamento-rede
description: Metodologia tática para mapear schema/capacidade de interfaces web administrativas de ONTs, roteadores e equipamentos de rede local (incluindo esquemas de autenticação proprietários por firmware), gerando documento de field-map read-only. Consultar antes de qualquer scan de reconhecimento em equipamento novo (ONT, roteador, AP, mesh).
---

## Quando usar

Antes de iniciar reconhecimento técnico (não implementação) da interface web de
um equipamento de rede local — ONT, roteador, AP, mesh — para mapear campos
disponíveis, comparar com o que o app já usa, e alimentar backlog de features
de diagnóstico. Casos reais que geraram esta skill:
`docs_ai/technical/NOKIA_GPON_FIELD_MAP.md` (ONT Nokia G-1425G-B, RSA+AES/sjcl)
e `docs_ai/technical/TPLINK_ARCHER_ROUTER_FIELD_MAP.md` (roteador TP-Link
Archer, RSA duplo+AES/jsbn, família `stok-luci`).

Para discovery em massa, scoring de confiança e priorização de família de
protocolo/driver, ver `/protocolos-locais` (projeto NetHAL) — essa skill cobre
a big picture; esta aqui cobre a execução tática de UM equipamento específico,
já com credencial em mãos, até virar documento.

## Princípios inegociáveis

- **Read-only.** Nunca alterar configuração, nunca clicar em ação destrutiva
  (reboot, factory reset, firmware upgrade) mesmo que o endpoint pareça
  inofensivo — só `GET`/leitura e o `POST` mínimo necessário para autenticar.
- **Sem bypass, exploit ou brute-force.** Só autenticar com credencial já
  fornecida explicitamente pelo dono do equipamento. Uma tentativa de login por
  sessão — se falhar, parar e reportar, nunca tentar variações de senha.
- **Sem senha/segredo persistido.** Credencial só existe em variável de
  ambiente do processo (`ROUTER_PASS`/`ONT_PASS` etc.) durante a sessão —
  nunca em arquivo, nunca em log, nunca em código versionado.
- **Scratch fora do repositório git, sempre.** Todo script de apoio (Node/
  Python) e todo HTML/JSON baixado do equipamento vai em diretório temporário
  do SO (`C:\Windows\Temp\<nome>_scan`), nunca dentro do repo — nem
  temporariamente. Apagar o diretório inteiro ao final da sessão.
- **Documento final nunca contém segredo real.** Senha (admin, Wi-Fi, PPPoE,
  RADIUS), serial/chave de registro do equipamento, hostname ou nome de rede
  pessoal — sempre `[REDACTED]` ou valor fictício. Dado técnico não sensível
  (potência óptica, canal Wi-Fi, contadores de erro, versão de firmware,
  estrutura de menu) pode e deve usar valor real capturado.
- **Se travar em MFA, certificado client-side ou app proprietário (não-web)**:
  parar, descrever exatamente o que foi encontrado, e devolver a decisão —
  nunca tentar contornar.

## Processo — passo a passo

### 1. Reconhecimento passivo inicial

```
curl -s -o /dev/null -w "HTTP_CODE:%{http_code}\n" http://<ip>/
curl -s -o /dev/null -w "HTTPS_CODE:%{http_code}\n" -k https://<ip>/
```

Buscar: título da página, redirect (`meta refresh` ou `Location`), headers
(`Server`, `X-Frame-Options`, `Set-Cookie`), e qualquer coisa que já identifique
fabricante/modelo sem autenticar.

### 2. Achar o esquema de autenticação real — nunca assumir, sempre ler o código do próprio equipamento

Toda interface web administrativa de ONT/roteador serve o próprio JS que faz o
login. **Baixe e leia esse JS antes de escrever qualquer script de auth.**
Não reimplementar de memória um esquema "genérico" — cada fabricante (e às
vezes cada família de firmware do mesmo fabricante) tem detalhes que quebram
tentativas ingênuas (padding, ordem de bytes, chave dinâmica vs. estática,
chunking).

```
curl -s http://<ip>/ -o root.html
# ler root.html, achar <script src="...">, baixar cada um
curl -s http://<ip>/<path-do-script-de-login>.js -o login.js
```

Cuidado com paths relativos: se a página de login está em `/webpages/login.html`
e referencia `src="js/libs/x.js"`, o path real é `/webpages/js/libs/x.js`, não
`/js/libs/x.js` — teste os dois se o primeiro der 404.

Padrões já catalogados em campo (adicionar novos aqui conforme forem
encontrados):

| Fabricante/família | Esquema | Sinal de detecção |
|---|---|---|
| Nokia/ALU GPON (`G-14xxG-*`) | RSA1024 (chave pública estática por device) cifra AES-128-CBC key+IV; POST `encrypted=1&ct=&ck=`; sucesso = HTTP **299** + header `X-SID` | `js_glb/jsencrypt.min.js`, `js_glb/sjcl.js`, `js_glb/crypto_page.js`; `var pubkey = '-----BEGIN PUBLIC KEY-----...'` no HTML |
| TP-Link/Mercusys (`stok-luci`, Archer/A6/C6) | RSA1024 cifra senha + RSA512 assina (`sign`, chunks de 53 bytes) + AES-128-CBC com key/IV = strings de 16 dígitos decimais ASCII; `POST /cgi-bin/luci/;stok=/login?form=login`; resposta também vem envelopada em AES | `js/libs/tpEncrypt.js` + `js/libs/encrypt.js` (biblioteca jsbn/RSA de Tom Wu embutida inline); endpoints `?form=keys`/`?form=auth`/`?form=login` |

Para outros fabricantes (FRITZ!Box TR-064, OpenWrt LuCI/ubus, MikroTik,
Huawei HiLink, ZTE, Xiaomi MiWiFi, D-Link HNAP), consultar primeiro a tabela de
heurísticas em `/protocolos-locais` (NetHAL) antes de investigar do zero — é
bem provável que o protocolo já esteja catalogado lá.

### 3. Reproduzir a criptografia em Node.js nativo — nunca reimplementar BigInteger na mão

Node `crypto` já resolve RSA (PKCS1 v1.5, OAEP) e AES (CBC/GCM) nativamente.
Não portar bibliotecas JS de bignum (jsbn, sjcl) linha a linha.

- **RSA a partir de `(n_hex, e_hex)` crus** (formato comum: o equipamento
  devolve módulo e expoente em hex, não PEM): montar uma JWK e usar
  `crypto.createPublicKey`:

  ```js
  const jwk = { kty: 'RSA', n: Buffer.from(nHex,'hex').toString('base64url'),
                e: Buffer.from(eHex,'hex').toString('base64url') };
  const pubKey = crypto.createPublicKey({ key: jwk, format: 'jwk' });
  const ct = crypto.publicEncrypt(
    { key: pubKey, padding: crypto.constants.RSA_PKCS1_PADDING },
    Buffer.from(plaintext, 'utf8'));
  ```

  Node sempre retorna o ciphertext do tamanho exato do módulo — ao converter
  para hex já vem com o zero-padding correto à esquerda, sem esforço manual.

- **AES-CBC com key/IV explícitos** (não derivados de senha):
  `crypto.createCipheriv('aes-128-cbc', keyBuf, ivBuf)` com padding PKCS7
  automático do Node. Atenção a como o firmware gera a chave: pode ser bytes
  aleatórios binários OU (caso TP-Link) uma **string de dígitos decimais
  ASCII usada diretamente como bytes** — ler o JS do device para saber qual.

- **Chunking de assinatura RSA** (visto no TP-Link): se o plaintext a assinar
  é maior que `tamanho_modulo_bytes - 11` (overhead do PKCS1v1.5), o firmware
  fatia em blocos fixos (ex. 53 bytes para RSA512) e cifra cada bloco
  separadamente, concatenando o hex resultante sem separador.

- **Testar os passos read-only do handshake primeiro** (ex. `form=keys`,
  `form=auth` no TP-Link) — são seguros de chamar quantas vezes for preciso
  porque não enviam credencial nem podem falhar por senha errada. Só depois de
  validar que o parsing desses dois passos está correto, montar e disparar o
  POST de login (esse sim, uma tentativa só).

- **A resposta também pode vir envelopada na mesma cifra** (visto no TP-Link:
  `{"data":"<base64 AES>"}` mesmo em caso de sucesso/erro de login) — sempre
  tentar decriptar antes de concluir que a resposta é "só" um JSON de erro.

### 4. Mapear a árvore de telas/menu

Depois de autenticado, achar a fonte da árvore de navegação em vez de adivinhar
URLs:
- Puxar o "index"/dashboard pós-login e procurar por `menu.cgi`, `menu.json`,
  `data/menu.*.json`, ou um objeto JS tipo `var allNodes = {...}` /
  `var advMenuList = {...}` com `name`/`url`/`children` — isso dá a lista
  completa e oficial de telas, muito mais confiável que tentar adivinhar paths.
- Em SPAs com iframe (`<iframe name="mainFrame">`), cada item do menu carrega
  um sub-HTML/endpoint — baixar todos em lote:
  ```bash
  for t in "${targets[@]}"; do
    curl -s -b "$COOKIE" "http://<ip>/$t" -o "pages/$(echo "$t" | tr '?=&' '_').html"
  done
  ```
- Muitas dessas sub-páginas HTML/JS **são servidas sem autenticação** (só os
  dados dinâmicos exigem sessão) — vale baixá-las primeiro para extrair os
  endpoints reais de leitura (`grep` por `url:`, `$.su.url(`, `form=` etc.)
  antes de gastar chamadas autenticadas tentando adivinhar.

### 5. Extrair campos por tela e classificar

Para cada tela: nome exato do menu, endpoint, e para cada campo — nome, tipo,
unidade, e se já é usado hoje no código do produto (`grep` em `android/` por
nomes de campo prováveis, ex. `RXPower`, `psk_key`, `ConnectionType`) ou é
**dado novo**. Isso é o que torna o documento útil para priorização de feature,
não só uma cópia do payload.

### 6. Sanitizar e documentar

Estrutura do documento final (seguir os dois exemplos já existentes):

1. Fingerprint do equipamento (vendor, modelo, firmware, chipset) — dado
   técnico, não sensível, pode usar valor real.
2. Nota de segurança — campos sensíveis identificados durante o scan (nome do
   campo + tela + formato, nunca o valor real) e qualquer achado de segurança
   relevante (credencial padrão ainda ativa, debug endpoint hardcoded, senha
   em texto plano onde não devia).
3. Esquema de autenticação (referência técnica reutilizável — como o passo 2/3
   acima, mas específico do device).
4. Uma seção por tela/menu: campos, tipo, unidade, "usado hoje?" (comparado ao
   código real do produto).
5. Seção final "Oportunidades": campos novos priorizados por valor de
   diagnóstico, e lista do que foi varrido mas é baixo valor (não perder tempo
   documentando profundamente firewall/parental control/QoS se o produto é de
   diagnóstico de conectividade, não de segurança de rede).

**Antes de commitar, sempre rodar grep de verificação** contra o arquivo final
buscando: senha usada na sessão, qualquer PSK/chave capturada, seriais reais,
hostnames/SSIDs pessoais, tokens de sessão (`stok`/`sid`/`sysauth` capturados).
Só commitar se todas as buscas voltarem vazias. Prestar atenção especial a
frases tipo "valor real omitido" onde o valor real foi colado do lado por
engano — já aconteceu.

### 7. Limpeza

Apagar o diretório de scratch inteiro (`rm -rf /c/Windows/Temp/<nome>_scan`)
antes de considerar a tarefa concluída. Fazer logout da sessão no equipamento
se houver endpoint óbvio de logout.

## Onde salvar

- Documento final: `docs_ai/technical/<FABRICANTE>_<TIPO>_FIELD_MAP.md` no
  repo do produto que vai consumir o dado (hoje, SignallQ).
- Se o achado for sobre um protocolo/família nova de driver ainda não
  catalogada, também vale contribuir para
  `C:\Projetos\SevenAgents\Nethal\docs\drivers\live-evidence\` (fora do escopo
  de edição do Camilo — reportar para o squad do NetHAL decidir).

## Limites

- Esta skill cobre reconhecimento até virar documento — não implementa parser,
  não cria model Kotlin, não altera código de produto. Implementação é uma
  tarefa separada, planejada depois que o documento existir.
- Ação de escrita em qualquer equipamento real (mudar config, reboot, restore)
  está fora do escopo desta skill — nunca é piloto automático.
- Se o equipamento exigir MFA, certificado client TLS, ou só tiver app
  proprietário (sem superfície web), parar e reportar — não é escopo desta
  skill tentar contornar.
