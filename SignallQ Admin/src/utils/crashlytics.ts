import { FirebaseCrashlyticsSummary } from "../integrations/firebase/firebase.types";

// Motivo humano pra qualquer `source` de Crashlytics que não seja "bigquery"
// (dado real). Compartilhado entre todos os cards/tabelas que consomem
// Crashlytics, pra nunca colapsar no_credentials/no_data_yet/error na mesma
// mensagem genérica "não configurado" quando na verdade está configurado, só
// sem volume ainda (#880, achado 3).
export function crashFreeReason(
  source: FirebaseCrashlyticsSummary["source"] | undefined
): string {
  switch (source) {
    case "no_credentials":
      return "Firebase não configurado no Admin Worker";
    case "no_data_yet":
      return "BigQuery export ainda sem volume de crash";
    case "error":
      return "Erro ao consultar o BigQuery — tente novamente";
    default:
      return "Crashlytics indisponível";
  }
}
