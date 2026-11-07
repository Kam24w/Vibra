"""Genera los recursos de marca de Vibra a partir de design/logo.png.

Uso (desde VibraAndroid/):  python tools/generate_brand_assets.py

- Recorta el símbolo (sin el texto "Vibra") por su bounding box.
- mipmap-*/ic_launcher_foreground.png y ic_launcher_monochrome.png: icono adaptativo
  (lienzo de 108 dp, símbolo dentro de la zona segura de 66 dp).
- mipmap-*/ic_launcher.webp y ic_launcher_round.webp: iconos legacy sobre crema.
- drawable-*/splash_icon.png: icono de splash (240 dp, símbolo de 150 dp dentro del círculo de 160 dp).
- drawable-nodpi/vibra_logo.png: símbolo para la UI.
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "design" / "logo.png"
RES = ROOT / "app" / "src" / "main" / "res"

CREAM = (242, 239, 225, 255)
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}


def symbol() -> Image.Image:
    im = Image.open(SRC).convert("RGBA")
    # El texto "Vibra" está debajo del símbolo, separado por una franja vacía.
    alpha = im.getchannel("A").point(lambda a: 255 if a > 40 else 0)
    rows = [any(alpha.getpixel((x, y)) for x in range(im.width)) for y in range(im.height)]
    top = rows.index(True)
    bottom = top
    while bottom + 1 < len(rows) and rows[bottom + 1]:
        bottom += 1
    sym = im.crop((0, top, im.width, bottom + 1))
    return sym.crop(sym.getchannel("A").point(lambda a: 255 if a > 40 else 0).getbbox())


def fit(sym: Image.Image, box_px: int) -> Image.Image:
    scale = box_px / max(sym.size)
    return sym.resize((max(1, round(sym.width * scale)), max(1, round(sym.height * scale))), Image.LANCZOS)


def centered(sym: Image.Image, canvas_px: int, content_px: int, bg=(0, 0, 0, 0)) -> Image.Image:
    canvas = Image.new("RGBA", (canvas_px, canvas_px), bg)
    s = fit(sym, content_px)
    canvas.alpha_composite(s, ((canvas_px - s.width) // 2, (canvas_px - s.height) // 2))
    return canvas


def monochrome(img: Image.Image) -> Image.Image:
    white = Image.new("RGBA", img.size, (255, 255, 255, 255))
    white.putalpha(img.getchannel("A"))
    return white


def main() -> None:
    sym = symbol()
    for name, d in DENSITIES.items():
        mip = RES / f"mipmap-{name}"
        mip.mkdir(parents=True, exist_ok=True)
        fg = centered(sym, round(108 * d), round(58 * d))
        fg.save(mip / "ic_launcher_foreground.png")
        monochrome(fg).save(mip / "ic_launcher_monochrome.png")

        legacy = round(48 * d)
        square = centered(sym, legacy, round(34 * d), CREAM)
        square.save(mip / "ic_launcher.webp", lossless=True)
        mask = Image.new("L", (legacy, legacy), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, legacy - 1, legacy - 1), fill=255)
        rnd = Image.new("RGBA", (legacy, legacy), (0, 0, 0, 0))
        rnd.paste(square, (0, 0), mask)
        rnd.save(mip / "ic_launcher_round.webp", lossless=True)

        draw = RES / f"drawable-{name}"
        draw.mkdir(parents=True, exist_ok=True)
        centered(sym, round(240 * d), round(150 * d)).save(draw / "splash_icon.png")

    nodpi = RES / "drawable-nodpi"
    nodpi.mkdir(parents=True, exist_ok=True)
    sym.save(nodpi / "vibra_logo.png")
    print(f"Símbolo {sym.size} -> recursos generados en {RES}")


if __name__ == "__main__":
    main()
