#!/usr/bin/env python3
"""Gera assets oficiais do SignallQ a partir do símbolo canônico."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
BRAND = ROOT / "brand"
ANDROID_RES = ROOT / "android/app/src/main/res"
SOURCE = BRAND / "signallq-symbol-1024.png"
FONT_REGULAR = ROOT / "android/app/src/main/res/font/google_sans_flex_regular.ttf"
FONT_BOLD = ROOT / "android/app/src/main/res/font/google_sans_flex_bold.ttf"

INK = (13, 13, 26, 255)
WHITE = (255, 255, 255, 255)
VIOLET = (91, 33, 214, 255)
MUTED_DARK = (202, 196, 208, 255)

RESAMPLING = Image.Resampling.LANCZOS


def trimmed_symbol() -> Image.Image:
    source = Image.open(SOURCE).convert("RGBA")
    bbox = source.getchannel("A").getbbox()
    if bbox is None:
        raise RuntimeError("O símbolo oficial não possui pixels visíveis")
    return source.crop(bbox)


def fit_symbol(canvas_size: int, occupancy: float, monochrome: bool = False) -> Image.Image:
    symbol = trimmed_symbol()
    target = round(canvas_size * occupancy)
    scale = min(target / symbol.width, target / symbol.height)
    size = (round(symbol.width * scale), round(symbol.height * scale))
    symbol = symbol.resize(size, RESAMPLING)
    if monochrome:
        alpha = symbol.getchannel("A")
        symbol = Image.new("RGBA", size, WHITE)
        symbol.putalpha(alpha)
    layer = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    layer.alpha_composite(symbol, ((canvas_size - size[0]) // 2, (canvas_size - size[1]) // 2))
    return layer


def icon(size: int, background: tuple[int, int, int, int], occupancy: float) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), background)
    canvas.alpha_composite(fit_symbol(size, occupancy))
    return canvas


def round_icon(size: int, background: tuple[int, int, int, int], occupancy: float) -> Image.Image:
    square = icon(size, background, occupancy)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    square.putalpha(mask)
    return square


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True)


def generate_store_assets() -> None:
    light = icon(1024, WHITE, 0.76)
    dark = icon(1024, INK, 0.76)
    save_png(light, BRAND / "signallq-icon-1024-app-store.png")
    save_png(dark, BRAND / "signallq-icon-1024-app-store-dark.png")
    save_png(light.resize((512, 512), RESAMPLING), BRAND / "signallq-icon-512-play-store.png")
    save_png(dark.resize((512, 512), RESAMPLING), BRAND / "signallq-icon-512-play-store-dark.png")


def generate_web_assets() -> None:
    favicon_dir = BRAND / "favicon"
    favicon_dir.mkdir(parents=True, exist_ok=True)
    favicon_sizes = {}
    for size in (16, 32, 48):
        image = fit_symbol(size, 0.84)
        save_png(image, favicon_dir / f"favicon-{size}.png")
        favicon_sizes[size] = image
    favicon_sizes[48].save(
        favicon_dir / "favicon.ico",
        format="ICO",
        sizes=[(16, 16), (32, 32), (48, 48)],
    )
    for size in (192, 512):
        save_png(fit_symbol(size, 0.78), favicon_dir / f"icon-{size}.png")
        save_png(icon(size, INK, 0.62), favicon_dir / f"icon-{size}-maskable.png")
    save_png(icon(180, INK, 0.72), favicon_dir / "apple-touch-icon.png")


def generate_android_assets() -> None:
    densities = {
        "mdpi": (108, 48),
        "hdpi": (162, 72),
        "xhdpi": (216, 96),
        "xxhdpi": (324, 144),
        "xxxhdpi": (432, 192),
    }
    for density, (adaptive_size, legacy_size) in densities.items():
        target = ANDROID_RES / f"mipmap-{density}"
        save_png(Image.new("RGBA", (adaptive_size, adaptive_size), INK), target / "ic_launcher_background.png")
        save_png(fit_symbol(adaptive_size, 0.625), target / "ic_launcher_foreground.png")
        save_png(fit_symbol(adaptive_size, 0.625, monochrome=True), target / "ic_launcher_monochrome.png")
        save_png(icon(legacy_size, INK, 0.72), target / "ic_launcher.png")
        save_png(round_icon(legacy_size, INK, 0.72), target / "ic_launcher_round.png")


def generate_feature_graphic() -> None:
    canvas = Image.new("RGBA", (1024, 500), INK)
    symbol = fit_symbol(650, 0.88)
    symbol.putalpha(symbol.getchannel("A").point(lambda value: round(value * 0.46)))
    canvas.alpha_composite(symbol, (550, -74))

    lockup = Image.open(BRAND / "signallq-lockup-dark-bg.png").convert("RGBA")
    lockup.thumbnail((250, 80), RESAMPLING)
    canvas.alpha_composite(lockup, (72, 66))

    draw = ImageDraw.Draw(canvas)
    headline = ImageFont.truetype(FONT_BOLD, 52)
    body = ImageFont.truetype(FONT_REGULAR, 22)
    draw.text((72, 218), "Entenda. Resolva. Confirme.", fill=WHITE, font=headline)
    draw.text((72, 294), "Diagnóstico claro para uma conexão invisível.", fill=MUTED_DARK, font=body)
    draw.rounded_rectangle((72, 372, 230, 416), radius=22, fill=VIOLET)
    cta = ImageFont.truetype(FONT_BOLD, 16)
    draw.text((98, 384), "Analisar a rede", fill=WHITE, font=cta)
    save_png(canvas, BRAND / "signallq-feature-graphic-1024x500.png")


def main() -> None:
    generate_store_assets()
    generate_web_assets()
    generate_android_assets()
    generate_feature_graphic()
    print("Assets SignallQ gerados com sucesso.")


if __name__ == "__main__":
    main()
