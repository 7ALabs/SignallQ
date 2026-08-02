# Verificação De-Para — issue #1502

Insumo de desenvolvimento/auditoria da entrega da issue #1502 ("Resultado do
diagnóstico é técnico demais"). **Não é embarcado no APK** e não é dependência
de runtime do app — só é usado por desenvolvedores/CI para conferir aderência
entre o De-Para editorial aprovado por Luiz e o estado atual do código.

- `de_para.csv`: extrato somente-leitura da aba **De-Para** da planilha
  `levantamento_textos_diagnostico_signallq_de_para_CORRIGIDO.xlsx` fornecida
  por Luiz (arquivo original não modificado; este CSV é gerado a partir dele
  para permitir verificação automatizada sem depender de `openpyxl`/Excel no
  CI). Se a planilha for revisada no futuro, regenere este CSV a partir da
  aba De-Para — não edite o CSV manualmente linha a linha para "corrigir"
  divergências.
- `verify_de_para.py`: compara cada linha aplicável do CSV contra o arquivo-fonte
  indicado na coluna "Arquivo-fonte", classifica (aplicado / mantido / dinâmico
  / verificado manualmente / divergência) e retorna código de saída != 0 se
  restar alguma divergência não documentada.

## Uso

```bash
python3 scripts/de-para-1502/verify_de_para.py
```

Opções: `--repo-root <path>` (default: diretório atual), `--csv <path>`
(default: `de_para.csv` ao lado do script), `--json <path>` (grava o relatório
completo em JSON).
