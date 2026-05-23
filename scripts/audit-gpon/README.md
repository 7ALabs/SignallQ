# scripts/audit-gpon — Auditoria GPON do app LINKA

Script de validação dinâmica usado pela auditoria das telas GPON
(`docs/diagnostics/gpon-screens-audit-2026-04-25.md`).

Faz login no painel web do **Nokia / Alcatel-Lucent G-1425G-B** via Playwright
e extrai os dados GPON, WAN, PPP e device info usando o mapeamento da
v2.0.0 do manual de integração
(`source/devices/compativeis/Nokia G-1425-B/integration-manual-v2.md`).

## Pré-requisitos

- Node 18+ (validado em v24)
- Playwright 1.40+ com Chromium instalado (já presente nesta máquina)
- Estar conectado ao Wi-Fi do modem alvo (`192.168.1.x`)

## Como rodar

> **Credenciais:** o script lê de variáveis de ambiente. Nunca passe a senha
> em arquivo, em log persistido, em commit.

PowerShell:

```powershell
$env:GPON_HOST="192.168.1.254"
$env:GPON_USER="userAdmin"
$env:GPON_PASS="<sua-senha>"
node fetch_gpon.mjs --pretty
```

Bash:

```bash
GPON_HOST=192.168.1.254 GPON_USER=userAdmin GPON_PASS='<sua-senha>' \
  node fetch_gpon.mjs --pretty
```

## Flags

| Flag | Efeito |
|---|---|
| `--pretty` | JSON identado |
| `--headed` | Mostra o Chromium na tela (debug visual) |
| `--keep-open` | Não fecha o browser ao final (debug) |

## Saída

JSON único em stdout com seções:

- `wan_ipv4` — `wan_conns[]` parsed (DNS, IP, gateway, tráfego, uptime, VLAN)
- `device_info` — `dev_info`, `mem_info`, `cpu_temperatureinfo`
- `gpon` — `gpon_status` em forma RAW + 3 conversões (raw, ÷1000, conversão SI)
- `ppp` — payload nativo JSON de `/index.cgi?getppp`

A seção `gpon` traz **três variantes** das métricas ópticas para você
comparar com o que o app exibe e descobrir empiricamente qual conversão o
firmware está usando — útil porque o manual v2 lista µK/µV/µA mas o parser
do app divide tudo por 1000.

## Comparação com o app

1. Rode este script com o celular conectado ao Wi-Fi do modem.
2. Em paralelo, rode `flutter run` e abra a tela de Conexão / Fibra.
3. Compare campo a campo. Documente discrepâncias no relatório de auditoria.

## Cenários a forçar

Para validar regras de diagnóstico:

| Regra | Como reproduzir |
|---|---|
| FIBRA-02 (LOS) | Desconecte o cabo de fibra do modem por ~5s |
| FIBRA-08 (PPP off) | No painel web, desconecte o PPP manualmente |
| FIBRA-12 (uptime baixo) | Reinicie o modem e teste logo após o boot |
| Sem GPON | Conecte o celular a outro Wi-Fi (não o do Nokia) |

## Segurança

- **Não loga credenciais.** Apenas as lê do ambiente.
- **Não persiste cookies/sessão em disco.** Usa contexto efêmero.
- **Não comita nada.** Este diretório está fora do escopo de build do app.
