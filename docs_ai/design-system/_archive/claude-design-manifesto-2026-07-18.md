# Snapshot — manifesto do Claude Design `e77ea465` antes da separação DS/Protótipos (2026-07-18)

Registro do estado do projeto **antes** da Fase 1 da [[DECISAO_SEPARACAO_DS_PROTOTIPOS_2026-07-18]]
(remoção de `components/screens/**` + `components/sheets/**` da biblioteca do DS). Fonte: `_ds_sync.json`
do projeto (`styleSha f623a482…`, `bundleSha12 97db2a405349`), 50 componentes sincronizados.

As telas em si **não se perdem** — sobrevivem como composições nos fluxos `tobe/*` e `templates/*` do
mesmo projeto. Os `components/screens|sheets` a remover são artefatos derivados do gêmeo digital
(descontinuado 2026-07-14).

## Reutilizáveis — PERMANECEM no DS (14)

- **Primitivos (4):** Avatar, Badge, Icon, SignalBars
- **Layout (8):** BottomNav, Card, Overline, PhoneFrame, ScreenScroll, SheetFrame, StatusBar, TopBar
- **Animações (2):** Thinking, TypeOut

> `SheetFrame` é reutilizável porém remote-only (sem `src` local) — permanece, mas fica na paleta velha
> até re-adicionar o `src` (follow-up).

## Telas — REMOVIDAS da biblioteca do DS (13) — preservadas em `tobe/`

AjustesScreen, DispositivosScreen, DnsScreen, FibraModemScreen, HistoricoScreen, HomeScreen, LaudoScreen,
NovidadesScreen, OnboardingScreen, PrivacidadeScreen, SignallQScreen, SinalScreen, SpeedFlow

## Sheets — REMOVIDAS da biblioteca do DS (24) — preservadas em `tobe/sheets/`

CellularInfoSheet, ChannelDetailSheet, DadosLocaisSheet, DeviceDetailSheet, DeviceInfoSheet,
DiagnosticoAppSheet, DiagnosticoDetalhadoSheet, DiagnosticoSheet, ExportHistoricoBottomSheet,
GatewayConnectionSheet, GatewayCredentialsGuideSheet, GatewayInfoSheet, HistoricoDetailSheet,
InternetInfoSheet, MedicaoTipoSheet, MeshApSheet, MinhaConexaoSheet, NetworkDetailSheet,
OperadoraBottomSheet, PerfilEditSheet, PermissaoLocalizacaoContextoSheet, PermissaoTelefoniaContextoSheet,
PingScreenSheet, SimpleInfoSheet

Total removido: **37** (13 telas + 24 sheets). Total DS final: **14 reutilizáveis**.
