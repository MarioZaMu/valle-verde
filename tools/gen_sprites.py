"""Draws every sprite of Valle Verde (original pixel art, 32 x 32 px per tile) into src/main/resources/assets.

The images are our own work, drawn here with Pillow from fixed coordinates and a fixed random seed, so the
output is identical on every run (HU-44 · CA-5). No image comes from another game.

    python tools/gen_sprites.py            writes the PNG files
    python tools/gen_sprites.py --check    fails if a file on disk differs from what this script draws

Folders:  terrain/  crops/  animals/  buildings/  items/  characters/  icons/
"""
import os
import random
import sys

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets')
T = 32


def c(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def new(w=T, h=T):
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def shade(col, k):
    return (max(0, min(255, int(col[0] * k))), max(0, min(255, int(col[1] * k))), max(0, min(255, int(col[2] * k))), col[3])


def outline(img, col=(30, 20, 12, 255)):
    """Draws a one pixel outline around the opaque pixels."""
    w, h = img.size
    src = img.copy()
    px = src.load()
    out = img.load()
    for y in range(h):
        for x in range(w):
            if px[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3] > 200:
                        out[x, y] = col
                        break
    return img


# ---------------------------------------------------------------- terrain
GRASS = {'SPRING': '#6AAE4B', 'SUMMER': '#86B33C', 'AUTUMN': '#B98A3E', 'WINTER': '#D8E6EE'}


def grass(season):
    rnd = random.Random('grass' + season)
    base = c(GRASS[season])
    img, d = new()
    d.rectangle([0, 0, T - 1, T - 1], fill=base)
    for _ in range(26):
        x, y = rnd.randrange(T), rnd.randrange(T)
        d.point((x, y), fill=shade(base, 0.82))
    for _ in range(9):
        x, y = rnd.randrange(1, T - 1), rnd.randrange(2, T - 1)
        blade = shade(base, 1.18 if season != 'WINTER' else 0.9)
        d.line([(x, y), (x, y - 2)], fill=blade)
    if season == 'AUTUMN':
        for _ in range(5):
            d.point((rnd.randrange(T), rnd.randrange(T)), fill=c('#C8501E'))
    if season == 'WINTER':
        for _ in range(7):
            d.point((rnd.randrange(T), rnd.randrange(T)), fill=c('#FFFFFF'))
    return img


def soil(watered):
    rnd = random.Random('soil')
    base = c('#5C3A1A' if watered else '#8B5A2B')
    furrow = c('#3E2610' if watered else '#70471F')
    img, d = new()
    d.rectangle([0, 0, T - 1, T - 1], fill=base)
    for y in (7, 15, 23):
        d.line([(2, y), (T - 3, y)], fill=furrow)
        d.line([(2, y + 1), (T - 3, y + 1)], fill=shade(base, 1.12))
    for _ in range(18):
        d.point((rnd.randrange(T), rnd.randrange(T)), fill=shade(base, 0.8))
    return img


def water():
    rnd = random.Random('water')
    img, d = new()
    d.rectangle([0, 0, T - 1, T - 1], fill=c('#3A7BD5'))
    for _ in range(14):
        d.point((rnd.randrange(T), rnd.randrange(T)), fill=c('#2F69BA'))
    for (x, y) in ((4, 8), (18, 4), (10, 20), (22, 26), (3, 28)):
        d.line([(x, y), (x + 5, y)], fill=c('#A9CBF2'))
        d.point((x + 1, y - 1), fill=c('#A9CBF2'))
        d.point((x + 4, y - 1), fill=c('#A9CBF2'))
    return img


def obstacle():
    img = grass('SPRING')
    d = ImageDraw.Draw(img)
    d.rectangle([14, 20, 17, 28], fill=c('#5A3A1E'))
    d.rectangle([14, 20, 14, 28], fill=c('#7A5230'))
    for (x0, y0, x1, y1, col) in ((6, 6, 25, 20, '#2F7A3A'), (8, 3, 23, 15, '#3FA34D'), (11, 5, 19, 10, '#5CC36A')):
        d.ellipse([x0, y0, x1, y1], fill=c(col))
    d.ellipse([22, 24, 27, 28], fill=c('#8C8C8C'))
    d.ellipse([22, 24, 25, 26], fill=c('#B8B8B8'))
    return img


# ---------------------------------------------------------------- crops
LEAF = c('#3FA34D')
LEAF_D = c('#2F7A3A')


def crop_seed(d, col):
    d.ellipse([13, 19, 18, 23], fill=c('#3B2410'))
    d.point((14, 20), fill=c('#6B4A2E'))


def crop_sprout(d, col):
    d.line([(16, 24), (16, 18)], fill=LEAF_D)
    d.polygon([(16, 19), (11, 16), (13, 20)], fill=LEAF)
    d.polygon([(16, 19), (21, 16), (19, 20)], fill=LEAF)


def crop_growing(d, col):
    d.line([(16, 27), (16, 14)], fill=LEAF_D)
    for (x, y, s) in ((16, 22, -1), (16, 18, 1), (16, 14, -1)):
        d.polygon([(x, y), (x + s * 7, y - 4), (x + s * 5, y + 1)], fill=LEAF)
    d.polygon([(16, 12), (14, 8), (18, 8)], fill=LEAF)


def crop_mature(d, name):
    d.line([(16, 27), (16, 12)], fill=LEAF_D)
    d.polygon([(16, 22), (8, 17), (11, 23)], fill=LEAF)
    d.polygon([(16, 22), (24, 17), (21, 23)], fill=LEAF)
    if name == 'CARROT':
        d.polygon([(13, 18), (19, 18), (16, 29)], fill=c('#F28C28'))
        d.line([(14, 20), (17, 20)], fill=c('#C86E14'))
        d.polygon([(16, 18), (12, 10), (14, 17)], fill=LEAF)
        d.polygon([(16, 18), (20, 10), (18, 17)], fill=LEAF)
        d.line([(16, 18), (16, 9)], fill=LEAF_D)
    elif name == 'POTATO':
        for (x, y) in ((11, 22), (17, 24), (14, 27)):
            d.ellipse([x, y - 3, x + 6, y + 2], fill=c('#C9A66B'))
            d.point((x + 2, y - 1), fill=c('#8A6A3A'))
    elif name == 'TOMATO':
        for (x, y) in ((10, 17), (18, 15), (13, 23), (20, 22)):
            d.ellipse([x, y, x + 6, y + 6], fill=c('#D0312D'))
            d.point((x + 2, y + 1), fill=c('#FF8A80'))
            d.point((x + 3, y), fill=LEAF_D)
    elif name == 'CORN':
        d.polygon([(16, 5), (19, 14), (13, 14)], fill=c('#F2D43D'))
        d.line([(16, 7), (16, 14)], fill=c('#C9A81C'))
        d.polygon([(13, 14), (16, 25), (11, 18)], fill=LEAF)
        d.polygon([(19, 14), (16, 25), (21, 18)], fill=LEAF)
    elif name == 'PUMPKIN':
        d.ellipse([7, 15, 25, 28], fill=c('#E8751A'))
        d.line([(16, 15), (16, 28)], fill=c('#B85A10'))
        d.line([(11, 16), (11, 27)], fill=c('#B85A10'))
        d.line([(21, 16), (21, 27)], fill=c('#B85A10'))
        d.rectangle([15, 11, 17, 15], fill=LEAF_D)


def crop_withered(d, col):
    brown = c('#A68B5B')
    d.line([(16, 27), (16, 17)], fill=brown)
    d.line([(16, 20), (11, 17), (9, 20)], fill=brown)
    d.line([(16, 18), (21, 15), (23, 19)], fill=brown)
    d.point((9, 21), fill=shade(brown, 0.7))


def crop(name, stage):
    img, d = new()
    if stage == 'SEED':
        crop_seed(d, name)
    elif stage == 'SPROUT':
        crop_sprout(d, name)
    elif stage == 'GROWING':
        crop_growing(d, name)
    elif stage == 'MATURE':
        crop_mature(d, name)
    else:
        crop_withered(d, name)
    return outline(img, (40, 28, 14, 150))


# ---------------------------------------------------------------- animals
def hen(frame):
    img, d = new()
    bob = (0, -1, 0)[frame]
    legs = ((11, 20), (19, 20))
    for (x, y) in legs:
        d.line([(x + (2 if frame == 1 else 0), 25), (x + (2 if frame == 1 else 0), 29)], fill=c('#E0A030'))
    d.ellipse([6, 14 + bob, 24, 26 + bob], fill=c('#FFFDF2'))
    d.ellipse([8, 18 + bob, 18, 24 + bob], fill=c('#E8E4D4'))
    d.polygon([(6, 17 + bob), (2, 14 + bob), (5, 21 + bob)], fill=c('#E8E4D4'))
    hy = 8 + bob + (4 if frame == 2 else 0)
    d.ellipse([18, hy, 26, hy + 8], fill=c('#FFFDF2'))
    d.polygon([(26, hy + 3), (30, hy + 4), (26, hy + 6)], fill=c('#F2A33A'))
    d.rectangle([20, hy - 2, 23, hy], fill=c('#D8323A'))
    d.point((23, hy + 3), fill=c('#1A1A1A'))
    d.point((24, hy + 7), fill=c('#D8323A'))
    return outline(img)


def cow(frame):
    img, d = new()
    bob = (0, -1, 0)[frame]
    for x in (7, 11, 20, 24):
        d.rectangle([x, 22 + bob, x + 2, 29], fill=c('#F4F0E8'))
        d.rectangle([x, 28, x + 2, 29], fill=c('#3A3A3A'))
    d.ellipse([4, 11 + bob, 27, 26 + bob], fill=c('#F7F4EC'))
    for (x, y, w, h) in ((8, 13, 6, 5), (18, 18, 6, 5), (13, 21, 4, 3)):
        d.ellipse([x, y + bob, x + w, y + h + bob], fill=c('#2B2B2B'))
    hy = 9 + bob + (3 if frame == 2 else 0)
    d.rectangle([22, hy, 30, hy + 8], fill=c('#F7F4EC'))
    d.rectangle([22, hy + 5, 30, hy + 8], fill=c('#F2A8B0'))
    d.point((24, hy + 2), fill=c('#1A1A1A'))
    d.polygon([(22, hy), (21, hy - 3), (24, hy)], fill=c('#E8D8A8'))
    d.polygon([(30, hy), (31, hy - 3), (28, hy)], fill=c('#E8D8A8'))
    tail = (3, 2, 4)[frame]
    d.line([(4, 14 + bob), (tail, 20 + bob)], fill=c('#2B2B2B'))
    return outline(img)


# ---------------------------------------------------------------- buildings
def roof(d, x0, x1, y0, y1, col):
    mid = (x0 + x1) // 2
    d.polygon([(x0 - 2, y1), (mid, y0), (x1 + 2, y1)], fill=col)
    half_base = (x1 + 2) - mid
    for y in range(y0 + 4, y1 - 1, 4):
        half = int((y - y0) * half_base / (y1 - y0)) - 2
        d.line([(mid - half, y), (mid + half, y)], fill=shade(col, 0.75))


def coop():
    img, d = new(64, 64)
    d.rectangle([6, 28, 57, 60], fill=c('#D9A066'))
    for x in range(10, 57, 7):
        d.line([(x, 28), (x, 60)], fill=c('#B98046'))
    roof(d, 6, 57, 6, 30, c('#B5473B'))
    d.rectangle([24, 38, 39, 60], fill=c('#5A3A1E'))
    d.rectangle([26, 40, 37, 58], fill=c('#7A5230'))
    d.ellipse([34, 49, 36, 51], fill=c('#E0C060'))
    d.rectangle([11, 36, 19, 44], fill=c('#F4E7B0'))
    d.line([(15, 36), (15, 44)], fill=c('#8A6A3A'))
    d.line([(11, 40), (19, 40)], fill=c('#8A6A3A'))
    d.ellipse([29, 20, 34, 25], fill=c('#2A1A10'))
    d.rectangle([44, 52, 55, 60], fill=c('#E8C547'))
    return outline(img)


def barn():
    img, d = new(96, 64)
    d.rectangle([6, 24, 89, 60], fill=c('#B5473B'))
    for x in range(10, 89, 8):
        d.line([(x, 24), (x, 60)], fill=c('#933A30'))
    d.polygon([(2, 26), (48, 2), (93, 26)], fill=c('#6B4A3A'))
    for y in range(8, 26, 4):
        d.line([(48 - (y - 2) * 2, y), (48 + (y - 2) * 2, y)], fill=c('#4F362A'))
    d.rectangle([32, 32, 63, 60], fill=c('#F2E8D0'))
    d.line([(32, 32), (63, 60)], fill=c('#B5473B'), width=2)
    d.line([(63, 32), (32, 60)], fill=c('#B5473B'), width=2)
    d.rectangle([32, 32, 63, 60], outline=c('#B5473B'), width=2)
    d.rectangle([42, 12, 53, 20], fill=c('#2A1A10'))
    return outline(img)


def warehouse():
    img, d = new(64, 64)
    d.rectangle([4, 26, 59, 60], fill=c('#9A8F7A'))
    for y in range(30, 60, 5):
        d.line([(4, y), (59, y)], fill=c('#80765F'))
    d.polygon([(2, 28), (32, 8), (61, 28)], fill=c('#4F5A66'))
    d.rectangle([18, 36, 45, 60], fill=c('#6B5A44'))
    d.line([(32, 36), (32, 60)], fill=c('#4A3C2A'))
    d.rectangle([8, 52, 15, 59], fill=c('#C9A66B'))
    d.rectangle([48, 50, 56, 59], fill=c('#C9A66B'))
    return outline(img)


def greenhouse():
    img, d = new(96, 96)
    glass = (189, 232, 224, 120)
    d.rectangle([4, 6, 91, 90], fill=glass)
    d.polygon([(4, 6), (48, 0), (91, 6)], fill=(210, 240, 235, 160))
    frame = c('#F2F6F4')
    edge = c('#2A1A10')
    for i in range(0, 4):
        x = 4 + i * 29
        d.line([(x, 6), (x, 90)], fill=frame, width=2)
    for i in range(0, 4):
        y = 6 + i * 28
        d.line([(4, y), (91, y)], fill=frame, width=2)
    d.rectangle([4, 6, 91, 90], outline=edge, width=1)
    d.line([(10, 12), (22, 12)], fill=(255, 255, 255, 230))
    d.line([(10, 15), (16, 15)], fill=(255, 255, 255, 200))
    return img


# ---------------------------------------------------------------- items
def packet(col_hex):
    img, d = new()
    d.polygon([(8, 6), (24, 6), (23, 28), (9, 28)], fill=c('#F4E7B0'))
    d.rectangle([10, 12, 22, 21], fill=c(col_hex))
    d.rectangle([8, 6, 24, 8], fill=c('#C9A66B'))
    for (x, y) in ((13, 15), (17, 17), (15, 19), (19, 14)):
        d.point((x, y), fill=c('#FFFFFF'))
    return outline(img)


def item_crop(name):
    img, d = new()
    crop_mature(d, name)
    # lift the fruit so the item fills the tile
    return outline(img)


def egg():
    img, d = new()
    d.ellipse([9, 7, 23, 27], fill=c('#FFF8EC'))
    d.ellipse([12, 10, 15, 14], fill=c('#FFFFFF'))
    d.arc([9, 7, 23, 27], 20, 160, fill=c('#E4D4B8'))
    return outline(img)


def milk():
    img, d = new()
    d.rectangle([11, 10, 21, 28], fill=c('#F4F7FA'))
    d.rectangle([13, 5, 19, 10], fill=c('#F4F7FA'))
    d.rectangle([12, 4, 20, 6], fill=c('#4A90D9'))
    d.rectangle([11, 16, 21, 22], fill=c('#4A90D9'))
    d.rectangle([13, 18, 19, 20], fill=c('#F4F7FA'))
    return outline(img)


def hay():
    img, d = new()
    d.ellipse([5, 10, 27, 28], fill=c('#E8C547'))
    rnd = random.Random('hay')
    for _ in range(26):
        x, y = rnd.randrange(8, 25), rnd.randrange(12, 26)
        d.line([(x, y), (x + rnd.randrange(-2, 3), y - 3)], fill=c('#C9A81C'))
    d.rectangle([5, 17, 27, 19], fill=c('#8A6A3A'))
    return outline(img)


def medicine():
    img, d = new()
    d.rectangle([10, 10, 22, 28], fill=c('#F4F7FA'))
    d.rectangle([12, 5, 20, 10], fill=c('#8A8A8A'))
    d.rectangle([14, 15, 18, 25], fill=c('#D8323A'))
    d.rectangle([12, 18, 20, 22], fill=c('#D8323A'))
    return outline(img)


# ---------------------------------------------------------------- characters
SKIN = c('#F2C9A0')


def person(direction, frame, shirt, pants, hair, hat=None, apron=None, coat=None):
    img, d = new()
    step = (0, 2, -2)[frame]
    # legs
    d.rectangle([12, 21, 15, 29], fill=c(pants))
    d.rectangle([17, 21, 20, 29], fill=c(pants))
    if direction in ('S', 'N'):
        d.rectangle([12, 21 + (2 if frame == 1 else 0), 15, 29], fill=c(pants))
        d.rectangle([17, 21 + (2 if frame == 2 else 0), 20, 29], fill=c(pants))
    else:
        d.rectangle([13 + step, 21, 16 + step, 29], fill=c(pants))
        d.rectangle([16 - step, 21, 19 - step, 29], fill=shade(c(pants), 0.8))
    d.rectangle([12, 28, 15, 30], fill=c('#3A2A1A'))
    d.rectangle([17, 28, 20, 30], fill=c('#3A2A1A'))
    # body
    d.rectangle([10, 13, 21, 22], fill=c(coat or shirt))
    if apron:
        d.rectangle([13, 15, 18, 22], fill=c(apron))
    # arms
    swing = (0, 1, -1)[frame]
    d.rectangle([8, 14 + swing, 10, 21 + swing], fill=c(coat or shirt))
    d.rectangle([21, 14 - swing, 23, 21 - swing], fill=c(coat or shirt))
    d.rectangle([8, 21 + swing, 10, 22 + swing], fill=SKIN)
    d.rectangle([21, 21 - swing, 23, 22 - swing], fill=SKIN)
    # head
    d.ellipse([10, 3, 21, 14], fill=SKIN)
    if direction == 'N':
        d.ellipse([10, 3, 21, 12], fill=c(hair))
    else:
        d.rectangle([10, 3, 21, 7], fill=c(hair))
        if direction == 'S':
            d.point((13, 9), fill=c('#1A1A1A'))
            d.point((18, 9), fill=c('#1A1A1A'))
            d.point((15, 12), fill=c('#B5654A'))
            d.point((16, 12), fill=c('#B5654A'))
        elif direction == 'E':
            d.point((18, 9), fill=c('#1A1A1A'))
            d.rectangle([10, 3, 13, 11], fill=c(hair))
        else:
            d.point((13, 9), fill=c('#1A1A1A'))
            d.rectangle([18, 3, 21, 11], fill=c(hair))
    if hat:
        d.rectangle([8, 4, 23, 5], fill=c(hat))
        d.rectangle([11, 0, 20, 4], fill=c(hat))
    return outline(img)


NPCS = {
    'MARTA': dict(shirt='#D8323A', pants='#4A3A5A', hair='#7A3A1E', apron='#FFF4D6'),
    'TOMAS': dict(shirt='#6B8E4E', pants='#3A4A6A', hair='#C8C8C8', hat='#C9A66B'),
    'LUCIA': dict(shirt='#FFFFFF', pants='#2F4F6F', hair='#2A1A10', coat='#F4F7FA'),
    'INES': dict(shirt='#7A5AA8', pants='#7A5AA8', hair='#E0A030'),
}


# ---------------------------------------------------------------- icons
def icon_coin():
    img, d = new()
    d.ellipse([5, 5, 26, 26], fill=c('#F2C23A'))
    d.ellipse([8, 8, 23, 23], outline=c('#C9981C'), width=2)
    d.rectangle([14, 11, 17, 20], fill=c('#C9981C'))
    d.rectangle([12, 13, 19, 14], fill=c('#C9981C'))
    return outline(img)


def icon_energy():
    img, d = new()
    d.polygon([(19, 3), (8, 18), (15, 18), (12, 29), (24, 12), (17, 12)], fill=c('#F2D43D'))
    return outline(img)


def icon_sun(dry=False):
    img, d = new()
    d.ellipse([9, 9, 22, 22], fill=c('#FFC21A'))
    for i in range(8):
        import math
        a = math.pi * i / 4
        d.line([(16 + math.cos(a) * 9, 16 + math.sin(a) * 9), (16 + math.cos(a) * 14, 16 + math.sin(a) * 14)], fill=c('#FFC21A'), width=2)
    if dry:
        d.line([(12, 14), (15, 18), (13, 21)], fill=c('#B5651D'))
    return outline(img)


def icon_cloud(rain=False, snow=False, dark=False):
    img, d = new()
    col = c('#8C99A8' if dark else '#E6EDF5')
    d.ellipse([4, 10, 16, 22], fill=col)
    d.ellipse([10, 5, 24, 19], fill=col)
    d.ellipse([16, 11, 28, 22], fill=col)
    d.rectangle([8, 16, 24, 22], fill=col)
    if rain:
        for x in (9, 15, 21):
            d.line([(x, 24), (x - 2, 29)], fill=c('#3A7BD5'), width=2)
    if snow:
        for x in (9, 15, 21):
            d.point((x, 26), fill=c('#FFFFFF'))
            d.point((x + 1, 28), fill=c('#FFFFFF'))
            d.rectangle([x - 1, 25, x + 1, 27], fill=c('#FFFFFF'))
    return outline(img)


def icon_flake(col='#BFE3FF'):
    import math
    img, d = new()
    for i in range(3):
        a = math.pi * i / 3
        d.line([(16 - math.cos(a) * 13, 16 - math.sin(a) * 13), (16 + math.cos(a) * 13, 16 + math.sin(a) * 13)], fill=c(col), width=2)
    d.ellipse([13, 13, 18, 18], fill=c('#FFFFFF'))
    return outline(img, (40, 70, 110, 255))


def icon_flower():
    img, d = new()
    d.line([(16, 18), (16, 29)], fill=LEAF_D, width=2)
    d.polygon([(16, 25), (22, 21), (20, 26)], fill=LEAF)
    for (x, y) in ((16, 8), (10, 12), (22, 12), (12, 18), (20, 18)):
        d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=c('#F28CB8'))
    d.ellipse([13, 11, 19, 17], fill=c('#FFD23F'))
    return outline(img)


def icon_leaf():
    img, d = new()
    d.polygon([(6, 26), (8, 10), (20, 4), (27, 7), (25, 20), (14, 27)], fill=c('#D9741E'))
    d.line([(6, 26), (22, 10)], fill=c('#8A3F0C'), width=2)
    d.line([(14, 18), (14, 12)], fill=c('#8A3F0C'))
    return outline(img)


def icon_sow():
    img, d = new()
    d.polygon([(7, 14), (25, 14), (23, 29), (9, 29)], fill=c('#C9A66B'))
    d.rectangle([7, 12, 25, 15], fill=c('#8A6A3A'))
    d.line([(16, 11), (16, 4)], fill=LEAF_D, width=2)
    d.polygon([(16, 7), (10, 4), (12, 9)], fill=LEAF)
    d.polygon([(16, 7), (22, 4), (20, 9)], fill=LEAF)
    return outline(img)


def icon_water():
    img, d = new()
    d.rectangle([8, 12, 22, 25], fill=c('#4A90D9'))
    d.rectangle([8, 12, 22, 14], fill=c('#7FB5F0'))
    d.polygon([(22, 16), (30, 9), (30, 12), (22, 21)], fill=c('#4A90D9'))
    d.arc([2, 8, 12, 22], 90, 270, fill=c('#2F69BA'), width=2)
    for (x, y) in ((28, 17), (26, 22), (30, 23)):
        d.point((x, y), fill=c('#7FB5F0'))
    return outline(img)


def icon_harvest():
    img, d = new()
    d.polygon([(5, 14), (27, 14), (24, 28), (8, 28)], fill=c('#C9A66B'))
    for x in (10, 15, 20):
        d.line([(x, 14), (x - 1, 28)], fill=c('#8A6A3A'))
    d.arc([6, 3, 26, 22], 190, 350, fill=c('#8A6A3A'), width=2)
    d.ellipse([11, 8, 17, 14], fill=c('#F28C28'))
    d.ellipse([16, 9, 22, 15], fill=c('#D0312D'))
    return outline(img)


def icon_build():
    img, d = new()
    d.rectangle([14, 12, 17, 29], fill=c('#8A6A3A'))
    d.rectangle([6, 5, 26, 13], fill=c('#8C99A8'))
    d.rectangle([6, 5, 26, 7], fill=c('#B8C2CE'))
    return outline(img)


def icon_feed():
    img, d = new()
    d.polygon([(6, 14), (26, 14), (23, 28), (9, 28)], fill=c('#B5651D'))
    d.rectangle([6, 12, 26, 15], fill=c('#8A4A12'))
    rnd = random.Random('feed')
    for _ in range(14):
        x = rnd.randrange(8, 25)
        d.line([(x, 12), (x + rnd.randrange(-2, 3), 5 + rnd.randrange(0, 4))], fill=c('#E8C547'))
    return outline(img)


# ---------------------------------------------------------------- assembly
CROPS = ['CARROT', 'POTATO', 'TOMATO', 'CORN', 'PUMPKIN']
STAGES = ['SEED', 'SPROUT', 'GROWING', 'MATURE', 'WITHERED']
SEED_COLOURS = {'CARROT': '#F28C28', 'POTATO': '#C9A66B', 'TOMATO': '#D0312D', 'CORN': '#F2D43D', 'PUMPKIN': '#E8751A'}


def build():
    files = {}
    for season in GRASS:
        files[f'terrain/GRASS_{season}.png'] = grass(season)
    files['terrain/SOIL.png'] = soil(False)
    files['terrain/SOIL_WATERED.png'] = soil(True)
    files['terrain/WATER.png'] = water()
    files['terrain/OBSTACLE.png'] = obstacle()
    for name in CROPS:
        for stage in STAGES:
            files[f'crops/{name}_{stage}.png'] = crop(name, stage)
        files[f'items/{name}_SEED.png'] = packet(SEED_COLOURS[name])
        files[f'items/{name}.png'] = item_crop(name)
    for frame in range(3):
        files[f'animals/HEN_{frame}.png'] = hen(frame)
        files[f'animals/COW_{frame}.png'] = cow(frame)
    files['buildings/COOP.png'] = coop()
    files['buildings/BARN.png'] = barn()
    files['buildings/WAREHOUSE.png'] = warehouse()
    files['buildings/GREENHOUSE.png'] = greenhouse()
    files['items/EGG.png'] = egg()
    files['items/MILK.png'] = milk()
    files['items/HAY.png'] = hay()
    files['items/MEDICINE.png'] = medicine()
    for direction in ('S', 'N', 'E', 'W'):
        for frame in range(3):
            files[f'characters/AVATAR_{direction}_{frame}.png'] = person(direction, frame, '#3A7BD5', '#4A3A2A', '#5A3A1E', hat='#E8C547')
    for npc, look in NPCS.items():
        files[f'characters/{npc}.png'] = person('S', 0, **look)
    files['icons/COIN.png'] = icon_coin()
    files['icons/ENERGY.png'] = icon_energy()
    files['icons/SPRING.png'] = icon_flower()
    files['icons/SUMMER.png'] = icon_sun()
    files['icons/AUTUMN.png'] = icon_leaf()
    files['icons/WINTER.png'] = icon_flake()
    files['icons/SUNNY.png'] = icon_sun()
    files['icons/CLOUDY.png'] = icon_cloud(dark=True)
    files['icons/RAINY.png'] = icon_cloud(rain=True, dark=True)
    files['icons/DROUGHT.png'] = icon_sun(dry=True)
    files['icons/FROST.png'] = icon_flake()
    files['icons/SNOWY.png'] = icon_cloud(snow=True)
    files['icons/SOW.png'] = icon_sow()
    files['icons/WATER.png'] = icon_water()
    files['icons/HARVEST.png'] = icon_harvest()
    files['icons/BUILD.png'] = icon_build()
    files['icons/FEED.png'] = icon_feed()
    return files


def main():
    check = '--check' in sys.argv
    stale = []
    files = build()
    for rel, img in sorted(files.items()):
        path = os.path.join(ROOT, *rel.split('/'))
        if check:
            if not os.path.exists(path) or list(Image.open(path).convert('RGBA').getdata()) != list(img.getdata()):
                stale.append(rel)
        else:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            img.save(path, optimize=False)
    if check:
        print('SIN ERRORES' if not stale else 'DESACTUALIZADOS: ' + ', '.join(stale))
        sys.exit(1 if stale else 0)
    print(f'{len(files)} sprites escritos en {os.path.normpath(ROOT)}')


if __name__ == '__main__':
    main()
