import { FirebaseCrashlyticsSummary } from "../integrations/firebase/firebase.types";

// Motivo honesto por source quando o Crashlytics não tem dado real
// (source !== "bigquery"). Compartilhado entre OverviewMetricGrid e
// ErrorMetricGrid para não duplicar o mesmo texto em dois lugares.
export function crashFreeReason(source: FirebaseCrashlyticsSummary["source"] | undefined): string {
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
