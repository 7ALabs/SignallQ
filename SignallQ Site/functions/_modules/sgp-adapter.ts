import type { ChamadoCanônico } from '@/shared/chamado';
import type { ErpAdapter, ErpCredenciais, ResultadoChamado } from './erp-adapter';
import { abrirChamadoSgp } from './sgp';

// Adapter SGP/TSMX implementando ErpAdapter (issue #26). Não reimplementa a lógica de chamado —
// só adapta o contrato existente e testado de sgp.ts (abrirChamadoSgp, prova de escrita SIG-273)
// para a interface canônica. sgp.ts continua tendo as demais funções (lookup, títulos, religa,
// atualizar OS) fora do escopo de ErpAdapter — essas seguem sendo consumidas diretamente pelos
// endpoints que já as usavam, sem regressão de comportamento.
export const sgpAdapter: ErpAdapter = {
  tipo: 'sgp',

  async criarChamado(
    credenciais: ErpCredenciais,
    chamado: ChamadoCanônico,
    fetcher: typeof fetch = fetch,
  ): Promise<ResultadoChamado> {
    const protocolo = await abrirChamadoSgp(
      credenciais.baseUrl,
      credenciais.token,
      credenciais.app,
      chamado,
      fetcher,
    );
    return { protocolo };
  },
};
