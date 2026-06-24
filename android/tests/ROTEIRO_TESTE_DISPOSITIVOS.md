# Roteiro de teste em device — Scan de Dispositivos

**Versão:** 0.18.0 · **Objetivo:** validar em hardware real o que só é confirmável fora do unit test — descoberta de hosts, qualidade dos nomes (mDNS/SSDP) e fabricante.

> Por que manual: ping nativo, mDNS (jmDNS) e SSDP dependem da rede/OEM. O CI cobre só lógica pura (parser XML, prioridade de nome, OUI, subnet). O resto exige Wi-Fi real com dispositivos.

## Pré-condições
- Device físico (não emulador — emulador não enxerga a LAN real).
- Conectado a um **Wi-Fi** com vários dispositivos ligados: idealmente 1 Chromecast/Google TV, 1 Apple (iPhone/Apple TV), 1 impressora, 1 TV UPnP/smart, 1 PC, o roteador.
- Permissões de localização/dispositivos próximos concedidas.
- Ter à mão a "verdade": liste no painel do roteador os dispositivos conectados (IP/nome) para comparar.

## Passos
1. Abrir o app → aba **Sinal** (Wi-Fi) → rolar até o fim → tocar **"Ver dispositivos na rede"**.
2. Aguardar o scan concluir (~5–10s). Observar a lista.
3. Voltar à **Início** e conferir o chip **"N dispositivos na rede"** no card de Wi-Fi (deve refletir a contagem do scan; sem scan prévio na sessão, não aparece número).
4. Tocar no chip da Home → deve abrir a mesma tela de dispositivos.
5. Repetir o scan 2–3x para checar estabilidade da contagem.
6. Trocar para **rede móvel** (desligar Wi-Fi) → o botão em Sinal e o chip na Home **não** devem aparecer/abrir scan.

## O que verificar (critérios)
| Item | Esperado | OK? |
|---|---|---|
| Cobertura | nº de hosts detectados ≈ nº real no painel do roteador (ping nativo) | |
| Nome — Chromecast/Google | nome amigável (ex.: "Sala", não "android-xxxx") via mDNS TXT `fn`/`md` | |
| Nome — Apple/AirPlay | nome do dispositivo via mDNS | |
| Nome — TV/UPnP | `friendlyName` real via SSDP (XML) — não "Linux UPnP/1.0" | |
| Nome — impressora | nome/modelo via mDNS `_ipp`/`_printer` | |
| Fabricante | aparece quando há UPnP `manufacturer` ou TXT (mesmo sem MAC) | |
| MAC | pode faltar na maioria (limite Android 10+/`/proc/net/arp`) — **não é falha** | |
| Tempo | scan conclui em ~5–10s (fase mDNS ~4,5s) — não dezenas de segundos | |
| Sem Wi-Fi | botão/chip ausentes; nenhum scan disparado | |
| Estabilidade | contagem consistente entre execuções | |

## Como capturar evidência
- Print de cada tela (lista de dispositivos + card Home).
- Se algo vier errado, capturar logcat filtrando a tag do scanner:
  `adb logcat | grep -i -E "scanner|dispositiv|mdns|ssdp|jmdns"`
- Anotar device/OEM/ROM (ex.: Samsung OneUI 6, Xiaomi MIUI) — multicast/SSDP variam por OEM.

## Limitações conhecidas (esperadas, não são bug)
- MAC/fabricante via OUI raramente disponível em Android 10+ (sandbox bloqueia ARP). Fabricante deve vir de UPnP/mDNS quando existir.
- ROMs agressivas (MIUI/EMUI) podem bloquear multicast → mDNS pode vir vazio; o scan não trava (cai para ping/SSDP).
- `MulticastLock` pode ser liberado em background por alguns OEMs → rodar com o app em primeiro plano.
