#!/usr/bin/env python3
"""Generate the Serial Monitor app icon in all needed formats.

Draws at high resolution with Pillow and downsamples for anti-aliasing.
Outputs: PNG masters, a multi-size .ico (Windows), a Linux .png, a Swing
resource PNG, and a macOS .iconset folder (converted to .icns by iconutil
in the accompanying shell step)."""

import math
import os
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

S = 4096                      # supersampled drawing canvas
TEAL_TOP = (0, 194, 201)
TEAL_BOT = (0, 118, 123)
SCREEN = (7, 33, 35)
GRID = (255, 255, 255, 26)
WAVE = (95, 243, 208)         # mint
WAVE_GLOW = (95, 243, 208, 70)
NODE = (255, 255, 255)


def vgradient(size, top, bottom):
    col = Image.new("RGB", (1, size))
    for y in range(size):
        t = y / (size - 1)
        col.putpixel((0, y), tuple(int(top[i] * (1 - t) + bottom[i] * t) for i in range(3)))
    return col.resize((size, size))


def thick_polyline(draw, pts, width, fill):
    r = width / 2
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        draw.line([x0, y0, x1, y1], fill=fill, width=int(width))
    for (x, y) in pts:                    # round the joins/caps
        draw.ellipse([x - r, y - r, x + r, y + r], fill=fill)


def draw_icon():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))

    # Rounded teal tile with a small transparent margin.
    margin = int(S * 0.055)
    radius = int(S * 0.225)
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [margin, margin, S - margin, S - margin], radius=radius, fill=255)
    tile = vgradient(S, TEAL_TOP, TEAL_BOT).convert("RGBA")
    img.paste(tile, (0, 0), mask)

    # Inset "oscilloscope screen".
    sm = int(S * 0.155)
    srad = int(S * 0.13)
    screen_box = [sm, sm, S - sm, S - sm]
    ImageDraw.Draw(img).rounded_rectangle(screen_box, radius=srad, fill=SCREEN)

    # Subtle grid, clipped to the screen.
    overlay = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    x0, y0, x1, y1 = screen_box
    for i in range(1, 4):
        gx = x0 + (x1 - x0) * i / 4
        gy = y0 + (y1 - y0) * i / 4
        od.line([gx, y0, gx, y1], fill=GRID, width=int(S * 0.004))
        od.line([x0, gy, x1, gy], fill=GRID, width=int(S * 0.004))
    smask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(smask).rounded_rectangle(screen_box, radius=srad, fill=255)
    img.paste(overlay, (0, 0), Image.composite(smask, Image.new("L", (S, S), 0),
                                               overlay.split()[3]))

    # Waveform (line chart) across the screen.
    ys = [0.52, 0.66, 0.34, 0.6, 0.22, 0.7, 0.46]
    pad = int((x1 - x0) * 0.11)
    xs = [x0 + pad + (x1 - x0 - 2 * pad) * i / (len(ys) - 1) for i in range(len(ys))]
    pts = [(xs[i], y0 + (y1 - y0) * ys[i]) for i in range(len(ys))]

    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    thick_polyline(ImageDraw.Draw(glow), pts, S * 0.075, WAVE_GLOW)
    img.alpha_composite(glow)
    thick_polyline(ImageDraw.Draw(img), pts, S * 0.032, WAVE)

    r = int(S * 0.026)
    d = ImageDraw.Draw(img)
    for (x, y) in pts:
        d.ellipse([x - r, y - r, x + r, y + r], fill=NODE)
    return img


def main():
    master = draw_icon().resize((1024, 1024), Image.LANCZOS)
    master.save(os.path.join(HERE, "appicon-1024.png"))

    # Windows .ico (multi-size).
    master.save(os.path.join(HERE, "SerialMonitor.ico"),
                sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64),
                       (128, 128), (256, 256)])

    # Linux launcher png.
    master.resize((512, 512), Image.LANCZOS).save(os.path.join(HERE, "SerialMonitor.png"))

    # Swing window icon (bundled on the classpath).
    master.resize((256, 256), Image.LANCZOS).save(
        os.path.join(ROOT, "src", "main", "resources", "appicon.png"))

    # macOS .iconset (iconutil turns this into .icns).
    iconset = os.path.join(HERE, "SerialMonitor.iconset")
    os.makedirs(iconset, exist_ok=True)
    for base in (16, 32, 128, 256, 512):
        master.resize((base, base), Image.LANCZOS).save(
            os.path.join(iconset, f"icon_{base}x{base}.png"))
        master.resize((base * 2, base * 2), Image.LANCZOS).save(
            os.path.join(iconset, f"icon_{base}x{base}@2x.png"))
    print("icons generated")


if __name__ == "__main__":
    main()
