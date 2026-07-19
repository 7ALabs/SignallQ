// Vocabulário de tipo de conexão compartilhado entre o hook de estado de rede
// (hooks/useEstadoRede.ts) e o histórico local (lib/historyStore.ts) — vive em
// lib/ (não em hooks/) para não inverter a direção de dependência: lib/ não
// deve importar de hooks/.
export type TipoRede = 'wifi' | 'celular' | 'ethernet' | 'nenhuma' | 'desconhecida'

export function labelConexao(tipo: TipoRede | null | undefined): string {
  switch (tipo) {
    case 'wifi':
      return 'Wi-Fi'
    case 'celular':
      return 'Rede móvel'
    case 'ethernet':
      return 'Ethernet'
    default:
      return 'Conexão desconhecida'
  }
}

// Ícone Material Symbol por tipo — só distingue Wi-Fi do resto (protótipo
// "SignallQ WebApp.dc.html" do Luiz especifica só esses dois ícones para o
// card de histórico).
export function iconeConexao(tipo: TipoRede | null | undefined): string {
  return tipo === 'wifi' ? 'wifi' : 'speed'
}
