#!/usr/bin/env python3
"""Monta o payload de embed do Discord para o status da squad (em andamento / fila / pontos).

Uso: python3 _discord_squad_status_payload.py <arquivo_json>

Schema esperado no arquivo JSON:
{
  "em_andamento": [{"issue": "898", "titulo": "IA texto tecnico", "agente": "Camilo", "pontos": 5}],
  "fila": [{"issue": "862", "titulo": "Badge de rede", "pontos": 1}],
  "sessao_pts_usados": 13,
  "sessao_pts_teto": 20
}
Imprime o JSON do payload completo do webhook (com "username" e "embeds") na stdout.

Sem emoji (regra de marca SignallQ), labels em UPPERCASE. O resumo numerico fica num bloco
monoespacado curto (largura fixa pequena, sem risco de quebra de linha feia no cliente Discord);
as tasks em si vao em campos normais do embed, que quebram linha sozinhas sem ficar torto.
"""
import json
import sys
from datetime import datetime

BARRA_LEN = 12
FIELD_LIMIT = 1000  # Discord permite 1024 por field.value — margem de seguranca


def truncate(text, n):
    text = text or ""
    return text if len(text) <= n else text[: n - 1] + "…"


def lista_tasks(items, com_agente):
    if not items:
        return "_nenhuma_" if com_agente else "_vazia_"
    linhas = []
    for i in items:
        issue = f"`#{i.get('issue', '?')}`"
        pontos = f"{i.get('pontos', '?')}pt"
        titulo = truncate(i.get("titulo", ""), 60)
        if com_agente:
            agente = i.get("agente", "?")
            linhas.append(f"{issue} · {agente} · {pontos} — {titulo}".rstrip(" —"))
        else:
            linhas.append(f"{issue} · {pontos} — {titulo}".rstrip(" —"))
    texto = "\n".join(linhas)
    if len(texto) > FIELD_LIMIT:
        texto = texto[:FIELD_LIMIT] + f"\n… +{len(linhas)} itens (cortado)"
    return texto


def barra_progresso(pts_usados, pts_teto):
    ratio = (pts_usados / pts_teto) if pts_teto else 0
    preenchido = min(BARRA_LEN, round(BARRA_LEN * ratio))
    return "█" * preenchido + "░" * (BARRA_LEN - preenchido)


def main():
    with open(sys.argv[1], "r", encoding="utf-8") as f:
        data = json.load(f)

    em_andamento = data.get("em_andamento", [])
    fila = data.get("fila", [])
    pts_usados = data.get("sessao_pts_usados", 0)
    pts_teto = data.get("sessao_pts_teto", 20)

    pts_andamento = sum(item.get("pontos", 0) for item in em_andamento)
    pts_fila = sum(item.get("pontos", 0) for item in fila)

    ratio = (pts_usados / pts_teto) if pts_teto else 0
    if ratio >= 1:
        color = 15158332  # vermelho — estourou o teto
    elif ratio >= 0.7:
        color = 15844367  # amarelo — perto do teto
    else:
        color = 3447003  # azul — normal

    agora = datetime.now().strftime("%H:%M")

    resumo = (
        "```\n"
        f"EM ANDAMENTO  {len(em_andamento):>2} · {pts_andamento:>2}pt\n"
        f"NA FILA       {len(fila):>2} · {pts_fila:>2}pt\n"
        f"PONTOS  [{barra_progresso(pts_usados, pts_teto)}]  {pts_usados}/{pts_teto}\n"
        "```"
    )

    payload = {
        "username": "Juninho · SignallQ",
        "embeds": [
            {
                "title": f"STATUS DA SQUAD — {agora}",
                "description": resumo,
                "color": color,
                "fields": [
                    {
                        "name": "EM ANDAMENTO",
                        "value": lista_tasks(em_andamento, com_agente=True),
                        "inline": False,
                    },
                    {
                        "name": "NA FILA",
                        "value": lista_tasks(fila, com_agente=False),
                        "inline": False,
                    },
                ],
                "footer": {"text": f"SignallQ squad · atualizado às {agora}"},
            }
        ],
    }

    print(json.dumps(payload))


if __name__ == "__main__":
    main()
