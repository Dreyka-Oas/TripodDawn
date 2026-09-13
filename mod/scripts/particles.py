"""Draws the mod's own particle frames.

The source mod shipped one explosion sheet under three names, so its dust was on fire and its heat
ray was a ring. These two sets replace it: soil for the rise, a hot core for the beam. The impact
frames keep the source's rings, which is the one place they read correctly.

Run from anywhere; it writes straight into the resource pack.
"""
import math
import pathlib
import random

from PIL import Image

OUT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/tripoddawn/textures/particle"

SOIL = [(74, 55, 40), (99, 76, 54), (128, 105, 79), (156, 137, 110), (181, 166, 143)]
CORE = (255, 252, 236)
MID = (255, 174, 56)
EDGE = (232, 74, 12)


def blend(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def dust(path, radius, density, window, alpha):
    """A clump of soil. Lobes rather than a disc, so no two frames read as the same puff scaled."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = image.load()
    lobes = [(8 + random.uniform(-1.6, 1.6), 8 + random.uniform(-1.6, 1.6),
              radius * random.uniform(0.55, 1.0)) for _ in range(4)]
    for y in range(16):
        for x in range(16):
            reach = max((lobe[2] - math.hypot(x + 0.5 - lobe[0], y + 0.5 - lobe[1])) / lobe[2]
                        for lobe in lobes)
            if reach <= 0 or random.random() > density * (0.35 + 0.65 * reach):
                continue
            low, high = window
            shade = SOIL[min(high - 1, low + int((1.0 - reach) * (high - low)))]
            pixels[x, y] = (*shade, min(255, int(alpha * (0.5 + 0.5 * reach))))
    image.save(path)


def glow(path, radius, core_share, alpha):
    """A white-hot centre inside an orange halo, which is what a beam looks like at this size."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = image.load()
    for y in range(16):
        for x in range(16):
            reach = math.hypot(x + 0.5 - 8, y + 0.5 - 8) / radius
            if reach >= 1.0:
                continue
            if reach < core_share:
                shade = blend(CORE, MID, reach / core_share)
            else:
                shade = blend(MID, EDGE, (reach - core_share) / (1.0 - core_share))
            fade = min(1.0, (1.0 - reach) * 2.4) ** 1.3
            pixels[x, y] = (*shade, min(255, round(alpha * fade)))
    image.save(path)


random.seed(1071)

for index, settings in enumerate([(3.4, 0.92, (0, 3), 255), (4.6, 0.80, (0, 4), 240),
                                  (5.8, 0.62, (1, 5), 210), (6.8, 0.42, (2, 5), 160),
                                  (7.6, 0.24, (3, 5), 95)], start=1):
    dust(OUT / f"dirt_cloud_{index}.png", *settings)

for index, settings in enumerate([(4.2, 0.55, 255), (5.2, 0.45, 250), (6.4, 0.34, 225),
                                  (7.2, 0.22, 180), (7.9, 0.10, 120)], start=1):
    glow(OUT / f"heat_ray_{index}.png", *settings)

glow(OUT / "heat_ray_brighter.png", 7.9, 0.62, 255)

print(f"onze images ecrites dans {OUT}")
