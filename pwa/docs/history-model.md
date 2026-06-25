# Modelo de Histórico Local

## Decisão inicial

O MVP deve usar histórico local no navegador.

Banco recomendado:

- IndexedDB.

D1 fica fora do MVP até haver necessidade real de compartilhamento por link, sincronização, painel admin ou telemetria agregada.

## Princípios

- Histórico deve funcionar sem login.
- Histórico deve ficar local no dispositivo.
- Usuário deve poder limpar dados.
- Não armazenar dado sensível sem necessidade.
- Não depender de rede para ver resultados anteriores.

## Entidade principal

```ts
type HistoryEntry = {
  id: string;
  createdAt: string;
  speedTest: SpeedTestResult;
  diagnosis: DiagnosisResult;
  appVersion?: string;
};
```

## Índices desejados

- `id`;
- `createdAt`;
- `quality`;
- `speedStatus`;
- `stabilityStatus`.

## Operações mínimas

```ts
type HistoryRepository = {
  save(entry: HistoryEntry): Promise<void>;
  list(): Promise<HistoryEntry[]>;
  getById(id: string): Promise<HistoryEntry | null>;
  remove(id: string): Promise<void>;
  clear(): Promise<void>;
};
```

## Retenção

MVP pode manter todos os registros localmente, mas deve ter opção de limpar histórico.

Depois, se necessário:

- manter últimos 100 testes;
- remover os mais antigos automaticamente.

## Estados de tela

Histórico deve contemplar:

- vazio: nenhum teste salvo;
- carregando: lendo IndexedDB;
- erro: falha ao ler histórico;
- lista: testes salvos;
- detalhe: um teste específico.

## Privacidade

Texto mínimo esperado:

“Seu histórico fica salvo neste navegador. Se você limpar os dados do navegador ou usar outro dispositivo, o histórico pode não aparecer.”

## Exportação/importação

Fora do M1, mas preparada conceitualmente.

Formato futuro recomendado:

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-06-25T00:00:00.000Z",
  "entries": []
}
```

## Critérios de aceite M1

- Salvar resultado após teste.
- Listar resultados salvos.
- Abrir detalhe de resultado.
- Apagar item individual.
- Limpar histórico inteiro.
- Funcionar sem login.
- Não quebrar se IndexedDB falhar; exibir erro claro.

## Fora do escopo inicial

- Sincronização entre dispositivos.
- Login.
- Backup automático.
- D1.
- Compartilhamento público.
- Criptografia avançada local.

## Risco

IndexedDB pode falhar em modo privado ou ambientes restritos.

O app deve tratar falha e não travar o diagnóstico.
