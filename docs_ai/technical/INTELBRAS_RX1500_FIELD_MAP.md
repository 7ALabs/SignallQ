**Status:** ativo (documento de reconhecimento, não de produto — ver skill `/reconhecimento-equipamento-rede`)
**Última validação:** 2026-07-23 — confirmado que "Intelbras" no código só existe como fabricante no catálogo OUI (ver correção abaixo), sem parser de interface web
**Escopo:** levantamento estático do firmware Intelbras RX1500/RAX1500, insumo de reconhecimento
**Responsável:** Camilo (Backend Android)

# Mapeamento de campos — Firmware Intelbras RX1500 (análise estática)

> Levantamento de schema/capacidade a partir do **arquivo de firmware**
> `INTELBRAS_RX1500_2.2.24__20250828_release.aes` (~20 MB), fornecido pelo Luiz
> em `C:\Users\luizg\Documents\Firmware\`. **Diferente dos dois documentos
> irmãos** (`NOKIA_GPON_FIELD_MAP.md`, `TPLINK_ARCHER_ROUTER_FIELD_MAP.md`),
> este NÃO foi feito com o equipamento ao vivo na rede — é análise **estática/
> offline** de um único arquivo binário, sem nenhum acesso HTTP/rede a
> dispositivo real. Nenhum equipamento foi tocado nesta sessão.
>
> Levantamento feito em 2026-07-09 via parsing binário do arquivo (Python
> stdlib — `struct`, `zlib`, `hashlib`; sem binwalk, sem ferramenta de
> terceiros, sem download de nada). **Resultado parcial**: a estrutura
> geral do pacote foi mapeada com alta confiança, mas a extração completa do
> `rootfs` (onde vive a interface web administrativa) está **bloqueada** —
> ver seção "Bloqueio" abaixo. Isso não é uma falha de execução: é o limite
> real do que dá pra fazer com análise estática segura, sem a ferramenta
> proprietária da Intelbras.

## Fingerprint do equipamento

| Campo | Valor | Fonte |
|---|---|---|
| Nome do arquivo (fornecido) | `INTELBRAS_RX1500_...` | nome do arquivo |
| **Modelo real (auto-declarado pelo firmware)** | **`RAX1500`** | preâmbulo binário, offset `0x00` |
| Versão de hardware | `H1` | preâmbulo binário, offset `0x10` |
| Versão de firmware | `2.2.24` | preâmbulo binário, offset `0x14`, confere com o nome do arquivo |
| Timestamp de build | `Thu Aug 28 09:39:47 CST 2025` | conteúdo do componente `fwu_ver` dentro do pacote, confere com `20250828` no nome do arquivo |
| Kernel | **Linux 4.4.140** | header `uImage` (mkimage/U-Boot), campo `name` |
| Arquitetura | **MIPS** | header `uImage`, campo `arch` |
| Compressão do kernel | GZIP | header `uImage`, campo `comp` |
| Load address / entry point do kernel | `0x80010000` / `0x80679a90` | header `uImage` |
| SO/plataforma provável | Kernel proprietário derivado de SDK MIPS estilo Ralink/MediaTek (não confirmado o SoC exato) | ver seção "Pistas de SoC" abaixo |
| Sistema de arquivos raiz | Provavelmente SquashFS (não confirmado — ver Bloqueio) | inferência por convenção de mercado, não verificada nos bytes |

**Achado curioso #1 — nome do modelo não bate com o nome do arquivo.** O
firmware se autodeclara `RAX1500` no seu próprio cabeçalho binário, não
`RX1500` como no nome do arquivo. Pode ser apelido comercial vs. nome interno,
ou o arquivo pode ter sido renomeado manualmente em algum momento. Vale
confirmar com a etiqueta física do roteador antes de tratar isso como certeza
absoluta — documentando aqui o que o **firmware afirma sobre si mesmo**, que é
a fonte mais confiável disponível nesta análise.

---

## Estrutura do container — o que este arquivo realmente é

Ao contrário do que a extensão `.aes` sugere, **este não é um blob
uniformemente cifrado**. É um **arquivo TAR (formato USTAR) com um preâmbulo
proprietário de 48 bytes na frente**, mais um padrão sistemático de
"buracos" de integridade explicado abaixo. Nenhuma cifra AES real (chave, IV,
`openssl`, `EVP_*`) foi encontrada em lugar nenhum do conteúdo acessível desta
sessão.

### Preâmbulo (48 bytes, antes do TAR)

| Offset | Tamanho | Conteúdo | Valor real |
|---|---|---|---|
| `0x00` | 16 bytes | Modelo (ASCII, null-padded) | `RAX1500` |
| `0x10` | 4 bytes | Versão de hardware (ASCII) | `H1` |
| `0x14` | 10 bytes | Versão de firmware (ASCII) | `2.2.24` |
| `0x1E` | 1 byte | Flag/versão de formato do header | `1` (0x31) |
| `0x1F`–`0x2F` | 17 bytes | Reservado, zerado | — |

### TAR embutido (a partir do offset `0x30`)

O TAR é USTAR padrão (`ustar` magic no offset 257 de cada header, confirmado
byte a byte), mas o checksum de cada header **não bate** com o algoritmo
padrão de tar — o motivo é o padrão de "buracos" descrito abaixo, que
sobrescreve parte dos headers. Cinco entradas encontradas:

| Nome (campo `name` do header) | Tamanho | Modo | Papel |
|---|---|---|---|
| *(campo corrompido por buraco — ver abaixo)* | 6.865 bytes | `0755` (executável) | Script de atualização `fwu.sh` (identificado pelo conteúdo, não pelo nome) |
| `rootfs` | 16.293.888 bytes (~15,5 MB) | `0644` | Sistema de arquivos raiz — provável SquashFS, **não confirmado** |
| *(campo corrompido por buraco)* | 3.932.109 bytes (~3,75 MB) | `0644` | Kernel `uImage` (identificado pelo magic `0x27051956` no conteúdo) |
| `fwu_ver` | 46 bytes | `0644` | String de data/hora do build |
| `hw_ver` | 5 bytes | `0644` | Versão de hardware (conteúdo **irrecuperável** — ver abaixo) |
| `md5.txt` | 206 bytes | `0644` | Manifesto de checksums MD5 dos 5 componentes acima |

### O padrão de "buracos" (achado técnico central desta análise)

Analisando byte a byte, existe um padrão **perfeitamente regular e
determinístico**: contando blocos de 512 bytes a partir do início do TAR
embutido (offset `0x30`), **todo bloco de índice PAR tem seus primeiros 16
bytes substituídos por um valor de alta entropia** (provavelmente um
hash/assinatura de integridade, formato exato não identificado), enquanto
blocos de índice ÍMPAR ficam intactos. Isso foi validado cruzando múltiplos
pontos independentes do arquivo (headers de tar, o script de atualização, os
metadados de versão) — todos batem com a mesma regra de paridade, sem
exceção, em ~19.770 blocos afetados (~316 KB, ~1,6% do arquivo total).

Consequência prática por componente:

- **Script de atualização (`fwu.sh`)**: ~99% legível em texto puro. Apenas
  trechos isolados de 16 bytes (a cada 1024 bytes) foram perdidos — o
  suficiente para corromper o *nome* do arquivo no header do tar e alguns
  nomes de variável/comando no meio do script, mas o script inteiro continua
  compreensível.
- **Kernel (`uImage`)**: o header de 64 bytes do `uImage` caiu inteiro num
  bloco ímpar (íntegro) — por isso deu pra ler magic, versão, arquitetura,
  compressão e endereços com 100% de confiança. O corpo comprimido (GZIP)
  do kernel, porém, tem um "buraco" bem no início do stream comprimido
  (~byte 448) — como GZIP/DEFLATE é um stream contínuo, um único bloco
  corrompido invalida a descompressão de tudo que vem depois. Tentativas de
  remover/realinhar os buracos manualmente (via Python, sem ferramenta
  externa) não reconstituíram um stream válido — reportado como bloqueio,
  não forçado além disso.
- **`rootfs`**: exatamente os primeiros 16 bytes do arquivo — onde ficaria o
  magic number e os campos iniciais de um superbloco SquashFS — caem dentro
  de um bloco par (buraco). Por isso não foi possível confirmar o formato do
  filesystem nem montar/extrair nada dele. O resto do arquivo (~15,5 MB)
  tecnicamente ainda contém dados originais intercalados com buracos
  periódicos, mas SquashFS é internamente compactado em blocos — reconstruir
  isso corretamente exigiria escrever um parser de SquashFS tolerante a
  buracos, o que está fora do escopo de uma análise estática rápida.
- **`hw_ver`** (5 bytes): o arquivo inteiro é menor que o buraco de 16 bytes,
  ou seja, **o conteúdo real está 100% destruído/irrecuperável** nesta cópia
  do firmware. Não é possível dizer o que estava escrito ali.
- **`fwu_ver` e `md5.txt`**: os primeiros 16 bytes (dentro do buraco) se
  perdem, mas o resto do conteúdo é plaintext genuíno — confirmado porque o
  texto que sobra faz sentido perfeito (timestamp de build válido, manifesto
  de MD5 com nomes de arquivo reais). No caso do `md5.txt`, isso significa que
  o primeiro hash do manifesto (`rootfs`) aparece truncado pela metade
  (`134bcc3cb857d349` — só os últimos 16 caracteres hex de um MD5 de 32
  caracteres).

**Isso não é a Intelbras "cifrando com AES"** no sentido criptográfico
clássico — é algum tipo de marca de integridade/anti-repack aplicada em
posições fixas do container. A extensão `.aes` do arquivo pode ser resquício
de nomenclatura histórica/interna, ou se referir a uma etapa de proteção que
não está presente (ou não é reconhecível) nesta cópia específica do arquivo.

---

## Bloqueio — o que não foi possível concluir

Não foi possível extrair o conteúdo do `rootfs` (onde vive a interface web
administrativa, templates, endpoints, strings de configuração — todo o
objetivo original da tarefa). Dois caminhos tentados, ambos esgotados dentro
do escopo autorizado (sem baixar ferramenta de terceiros não verificada, sem
força bruta):

1. **Procurar chave/algoritmo de decriptação documentado no próprio
   firmware** — o único script presente (`fwu.sh`, majoritariamente legível)
   foi varrido por `aes`, `openssl`, `decrypt`, `cipher`, `crypt` — nenhuma
   ocorrência. O script só faz coisas convencionais: escolher partição MTD
   `ubi_k0`/`ubi_k1`/`ubi_r0`/`ubi_r1` (esquema **dual-bank A/B**), extrair
   `uImage`/`rootfs` do tar com `tar -x`, conferir hash com o `md5.txt`, e
   ler versão de hardware via um utilitário `flash get HW_HWVER` (típico de
   SDK Ralink/MediaTek de plataformas MIPS). Não há nenhuma chamada de
   decriptação — reforça a hipótese de que o "buraco" é um mecanismo de
   integridade, não uma cifra de conteúdo de fato.
2. **Reconstruir o stream a partir do padrão de buracos identificado** —
   tentativa de remover os 16 bytes de cada bloco par do stream GZIP do
   kernel e recolar o restante, para testar se os buracos eram *inserção*
   (recuperável) ou *sobrescrita* (destrutiva/irrecuperável). O resultado
   (`zlib` continua falhando com `invalid distance too far back`) indica
   sobrescrita — ou seja, mesmo sabendo exatamente onde e quanto foi
   corrompido, o conteúdo original desses 16 bytes **não está presente em
   nenhum outro lugar do arquivo** para ser recuperado.

**O que resolveria isso**, caso valha a pena continuar depois: a ferramenta
oficial de empacotamento/geração de firmware da Intelbras (que sabe recriar
esses "buracos" a partir do conteúdo original, logo teoricamente saberia
também como neutralizá-los), ou engenharia reversa dedicada de um parser de
SquashFS tolerante a lacunas de 16 bytes a cada 1024 — ambos fora do escopo
desta tarefa. Reportando como bloqueio, conforme instruído, em vez de insistir
com métodos obscuros.

---

## Pistas de SoC/plataforma (baixa confiança, mas vale registrar)

Sinais indiretos encontrados no `fwu.sh` (texto legível) sugerem uma origem
de SDK comum a roteadores MIPS de baixo custo, sem confirmação direta do
fabricante do chip:

- Utilitário de configuração via NVRAM chamado literalmente `flash`, com
  chaves tipo `flash get HW_HWVER` / `flash set` — assinatura característica
  de SDKs derivados de Ralink/MediaTek (RaSDK / MTK Wi-Fi SDK), usada em
  muitos roteadores MIPS de baixo custo no mercado.
- Partições nomeadas `ubi_k0`/`ubi_k1` (kernel) e `ubi_r0`/`ubi_r1`
  (rootfs) — confirma **flash NAND com UBI** (não NOR/raw flash) e esquema
  de **atualização dual-bank** (a imagem nova grava no banco inativo,
  parâmetro `$1` do script escolhe destino `0` ou `1` — clássico
  anti-brick de OTA).
- Referências a `framework.img` / `framework.sh` e ao termo interno
  "**YueMe framework**" — parece ser uma camada de atualização de um
  componente de aplicação separado do sistema base (não investigado a
  fundo, baixo valor pro diagnóstico de rede).

---

## Segurança — achados

Nenhuma senha, chave privada ou credencial de fábrica foi encontrada em
texto plano nesta sessão — mas isso é **porque a análise não chegou a
alcançar o `rootfs`** (onde esse tipo de coisa normalmente mora: interface
web, contas padrão, chaves de assinatura), não porque o firmware
necessariamente não tenha nada disso. Ou seja: **ausência de evidência aqui
não é evidência de ausência** — só não deu pra chegar lá.

Nenhum valor sensível real precisou ser redigido/mascarado neste documento,
porque nenhum foi de fato extraído.

---

## Oportunidades e comparação com o produto

Busca no código do SignallQ (`grep` em `android/`) mostra que "Intelbras"
hoje só existe como **fabricante no lookup de OUI** (mesmo padrão já visto no
documento do TP-Link):

- `android/core/network/src/main/kotlin/io/veloo/app/core/network/topologia/oui/OuiCatalog.kt`
  (**correção 2026-07-23**: os arquivos `MeshOuiDatabase.kt`/`OuiDatabase.kt` citados
  originalmente aqui foram unificados neste `OuiCatalog.kt` — ver
  `docs_ai/_archive/2026-07-23_PLANO_UNIFICACAO_TOPOLOGIA_WIFI_2026-07-15.md`, Fase 1)
- `android/feature/diagnostico/src/main/assets/oui.txt`

Ou seja: hoje o app só usa "Intelbras" pra identificar o *fabricante* de um
dispositivo pelo prefixo do MAC — não existe nenhum parser de interface web
Intelbras no produto. Isso é consistente com os dois documentos irmãos.

**Valor real desta análise, apesar do bloqueio:**
- Confirma que existe pelo menos um modelo Intelbras (`RAX1500`/RX1500) na
  base instalada que roda **kernel Linux MIPS com flash NAND/UBI e OTA
  dual-bank** — útil se algum dia o produto quiser detectar/anunciar
  compatibilidade por fabricante+família de firmware.
- Se o Luiz (ou outro usuário) tiver acesso físico/de rede a um roteador
  Intelbras RAX1500 ligado, a linha natural de continuação **não** é insistir
  na análise estática deste arquivo — é aplicar a metodologia da skill
  `/reconhecimento-equipamento-rede` **ao vivo** (interface web real do
  equipamento, com credencial fornecida), que teria acesso ao JS/HTML já
  servido sem precisar decifrar nada. Essa opção não foi executada aqui por
  estar fora do escopo autorizado desta tarefa (só o arquivo estático).

## Baixo valor / não aprofundado

- `framework.img`/`framework.sh` ("YueMe framework") — camada de app
  separada do sistema, sem sinal de ser relevante pra diagnóstico de rede.
- Qualquer coisa dentro do `rootfs` (interface web, endpoints, telas) —
  não é "baixo valor", é **bloqueado**, não descartado por escolha.
