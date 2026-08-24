#!/usr/bin/env python3
"""Gera criativos editoriais da ficha Play Store a partir de capturas reais do app."""

from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont


RAIZ = Path(__file__).resolve().parents[1]
ENTRADA = RAIZ / "android/assets/store/screenshots/playstore-2026-08-24-intercaladas"
SAIDA = RAIZ / "android/assets/store/screenshots/playstore-2026-08-24-molduradas"

FONTES = RAIZ / "android/app/src/main/res/font"
REGULAR = str(FONTES / "google_sans_flex_regular.ttf")
MEDIUM = str(FONTES / "google_sans_flex_medium.ttf")
SEMIBOLD = str(FONTES / "google_sans_flex_semibold.ttf")

CRIATIVOS = [
    ("01-inicio-escuro.png", "01-inicio-escuro.png", "Entenda sua conexão de verdade"),
    ("04-velocidade-claro.png", "02-velocidade-claro.png", "Meça o que realmente importa"),
    ("05-historico-escuro.png", "03-historico-escuro.png", "Acompanhe a evolução da sua internet"),
    ("08-ferramentas-claro.png", "04-ferramentas-claro.png", "Descubra o próximo passo"),
]


def fonte(caminho: str, tamanho: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(caminho, tamanho)


def linhas_para_largura(draw: ImageDraw.ImageDraw, texto: str, font: ImageFont.FreeTypeFont, limite: int) -> list[str]:
    palavras = texto.split()
    linhas: list[str] = []
    atual = ""
    for palavra in palavras:
        candidato = f"{atual} {palavra}".strip()
        if draw.textbbox((0, 0), candidato, font=font)[2] <= limite or not atual:
            atual = candidato
        else:
            linhas.append(atual)
            atual = palavra
    if atual:
        linhas.append(atual)
    return linhas


def criar(nome: str, saida: str, titulo: str) -> None:
    origem = Image.open(ENTRADA / nome).convert("RGB")
    # Remove apenas as barras de status e navegação do sistema; a UI do app permanece integral.
    app = origem.crop((0, 82, origem.width, origem.height - 82))

    escuro = "escuro" in nome
    fundo = (29, 18, 48) if escuro else (246, 242, 255)
    tinta = (255, 255, 255) if escuro else (31, 22, 48)
    violeta = (208, 188, 255) if escuro else (91, 33, 214)

    tela = Image.new("RGB", (1080, 1920), fundo)
    draw = ImageDraw.Draw(tela)
    titulo_font = fonte(SEMIBOLD, 64)
    marca_font = fonte(MEDIUM, 28)
    linhas = linhas_para_largura(draw, titulo, titulo_font, 860)
    y = 96
    draw.text((1080 // 2, y), "SignallQ", font=marca_font, fill=violeta, anchor="ma")
    y += 82
    for linha in linhas:
        draw.text((1080 // 2, y), linha, font=titulo_font, fill=tinta, anchor="ma")
        y += 78

    largura = 760
    altura = round(app.height * largura / app.width)
    app = app.resize((largura, altura), Image.Resampling.LANCZOS)
    x = (1080 - largura) // 2
    y_app = max(350, 1920 - altura - 70)
    raio = 42

    sombra = Image.new("RGBA", tela.size, (0, 0, 0, 0))
    sombra_draw = ImageDraw.Draw(sombra)
    sombra_draw.rounded_rectangle((x - 12, y_app - 12, x + largura + 12, y_app + altura + 12), radius=raio + 12, fill=(0, 0, 0, 110))
    sombra = sombra.filter(ImageFilter.GaussianBlur(18))
    tela = Image.alpha_composite(tela.convert("RGBA"), sombra)

    moldura = Image.new("RGBA", tela.size, (0, 0, 0, 0))
    moldura_draw = ImageDraw.Draw(moldura)
    moldura_draw.rounded_rectangle((x - 10, y_app - 10, x + largura + 10, y_app + altura + 10), radius=raio + 10, fill=(12, 9, 20, 255))
    moldura_draw.rounded_rectangle((x - 3, y_app - 3, x + largura + 3, y_app + altura + 3), radius=raio + 3, outline=(*violeta, 210), width=3)
    tela = Image.alpha_composite(tela, moldura)

    mascara = Image.new("L", (largura, altura), 0)
    ImageDraw.Draw(mascara).rounded_rectangle((0, 0, largura, altura), radius=raio, fill=255)
    tela.paste(app, (x, y_app), mascara)
    tela.convert("RGB").save(SAIDA / saida, format="PNG", optimize=True)


def main() -> None:
    SAIDA.mkdir(parents=True, exist_ok=True)
    for nome, saida, titulo in CRIATIVOS:
        criar(nome, saida, titulo)


if __name__ == "__main__":
    main()
