"""Draws every sprite of Valle Verde (original pixel art, 32 x 32 px per tile) into src/main/resources/assets.

The images are our own work, drawn here with Pillow from fixed coordinates and fixed random seeds, so the
output is identical on every run (HU-44 · CA-5). No image comes from another game: only the general style of
farm games is followed (warm colour ramps, outlines one shade darker than the colour they surround, a 3/4 view
where trees and buildings rise above the tiles they stand on).

    python tools/gen_sprites.py            writes the PNG files
    python tools/gen_sprites.py --check    fails if a file on disk differs from what this script draws
    python tools/gen_sprites.py --sheet F  also writes a contact sheet of every sprite to F (for review)

Folders:  terrain/  crops/  animals/  buildings/  village/  items/  characters/  icons/

Sizes: every image is 32 x 32 except the tall ones, which are whole tiles: trees 64 x 64, the coop and the
warehouse 64 x 96, the barn 96 x 96 and the greenhouse 96 x 128 (one tile of roof above the ground they use).
Ground tiles (grass, soil, water) wrap around, so they repeat with no seam.
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets')
T = 32


# ---------------------------------------------------------------- helpers
def c(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def ramp(*hexes):
    return [c(h) for h in hexes]


def dark(col, k=0.6):
    return (int(col[0] * k), int(col[1] * k), int(col[2] * k), 255)


def new(w=T, h=T):
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def rng(*key):
    return random.Random('-'.join(str(k) for k in key))


_LATTICE = {}


def lattice(seed, ix, iy):
    key = (seed, ix, iy)
    if key not in _LATTICE:
        _LATTICE[key] = random.Random(f'{seed}:{ix}:{iy}').random()
    return _LATTICE[key]


def noise(seed, x, y, cell=8, size=T):
    """Smooth value noise that wraps every `size` pixels, so a tile drawn with it has no seam."""
    period = max(1, size // cell)
    gx, gy = x / cell, y / cell
    x0, y0 = int(math.floor(gx)), int(math.floor(gy))
    fx, fy = gx - x0, gy - y0
    sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)

    def at(i, j):
        return lattice(seed, (x0 + i) % period, (y0 + j) % period)

    top = at(0, 0) + (at(1, 0) - at(0, 0)) * sx
    bottom = at(0, 1) + (at(1, 1) - at(0, 1)) * sx
    return top + (bottom - top) * sy


def put(p, x, y, col, w=T, h=T):
    p[x % w, y % h] = col


def auto_outline(img, k=0.45):
    """Outlines the opaque pixels with a darker shade of the colour they touch (not black)."""
    w, h = img.size
    src = img.copy().load()
    out = img.load()
    for y in range(h):
        for x in range(w):
            if src[x, y][3] != 0:
                continue
            for dx, dy in ((0, 1), (1, 0), (-1, 0), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3] > 140:
                    out[x, y] = dark(src[nx, ny], k)
                    break
    return img


def blob(d, cx, cy, r, R, light=True):
    """A round shaded clump lit from the top left: dark base, middle tone, light spot."""
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=R[0])
    if r >= 3:
        d.ellipse([cx - r + 1, cy - r, cx + r - 2, cy + r - 2], fill=R[1])
    if light and r >= 5:
        d.ellipse([cx - r + 2, cy - r + 1, cx + r // 3, cy - r // 4], fill=R[2])


# ---------------------------------------------------------------- terrain
GRASS = {
    'SPRING': ramp('#3D7A2B', '#4F9234', '#62A83C', '#7DBE48', '#A2D65E'),
    'SUMMER': ramp('#2E6424', '#3D7B2B', '#4F8E32', '#67A53B', '#8CC24D'),
    'AUTUMN': ramp('#6E5E26', '#857330', '#9C8738', '#B49E45', '#CDB863'),
    'WINTER': ramp('#8FA5BE', '#B4C5D8', '#D3DEEA', '#E9F0F7', '#FFFFFF'),
}


def blade(p, x, y, R):
    put(p, x, y, R[0])
    put(p, x - 1, y - 1, R[1])
    put(p, x + 1, y - 1, R[1])
    put(p, x - 1, y - 2, R[3])
    put(p, x + 1, y - 2, R[3])


def tuft(p, x, y, R):
    for dx, height in ((-2, 3), (0, 5), (2, 4)):
        for i in range(height):
            col = R[0] if i == 0 else (R[1] if i < height - 2 else (R[3] if i == height - 2 else R[4]))
            put(p, x + dx + (1 if i == height - 1 and dx < 0 else (-1 if i == height - 1 and dx > 0 else 0)), y - i, col)


def flower(p, x, y, petal, centre):
    for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
        put(p, x + dx, y + dy, petal)
    put(p, x, y, centre)
    put(p, x, y + 2, c('#3E7D2C'))


def grass(season, variant=0):
    R = GRASS[season]
    img, _ = new()
    p = img.load()
    for y in range(T):
        for x in range(T):
            v = noise('g' + season, x, y, 8) * 0.65 + noise('h' + season, x, y, 4) * 0.35
            p[x, y] = R[1] if v < 0.36 else (R[3] if v > 0.68 else R[2])
    r = rng('blades', season, variant)
    winter = season == 'WINTER'
    if winter:
        stalk = ramp('#4E6448', '#6E8A6A', '#6E8A6A', '#9DB496', '#C9D9C2')
        for _ in range(3 + variant):
            blade(p, r.randrange(T), r.randrange(T), stalk)
    else:
        for _ in range(16 if variant == 1 else 9):
            blade(p, r.randrange(T), r.randrange(T), R)
    if variant == 1:
        for _ in range(2):
            tuft(p, r.randrange(T), r.randrange(T), stalk if winter else R)
    elif variant == 2:
        if season == 'SPRING':
            for petal in (c('#FFFFFF'), c('#F7A8C8'), c('#FFE15A')):
                flower(p, r.randrange(T), r.randrange(T), petal, c('#F2A23A'))
        elif season == 'SUMMER':
            for petal in (c('#FFD23F'), c('#FFD23F'), c('#7FB2F0')):
                flower(p, r.randrange(T), r.randrange(T), petal, c('#C9661A'))
        elif season == 'AUTUMN':
            for col in (c('#C2571A'), c('#E07B24'), c('#9E3A1A'), c('#D9A033')):
                x, y = r.randrange(T), r.randrange(T)
                put(p, x, y, col)
                put(p, x + 1, y, col)
                put(p, x, y + 1, dark(col, 0.75))
        else:
            for _ in range(6):
                put(p, r.randrange(T), r.randrange(T), c('#FFFFFF'))
            x, y = r.randrange(T), r.randrange(T)
            for i in range(4):
                put(p, x + i, y - i // 2, c('#6B4A2E'))
    elif variant == 3:
        stone = ramp('#4E4A52', '#6E6A72', '#8E8A92', '#B5B1B8')
        for _ in range(2):
            x, y = r.randrange(T), r.randrange(T)
            put(p, x, y, stone[2])
            put(p, x + 1, y, stone[1])
            put(p, x, y + 1, stone[1])
            put(p, x + 1, y + 1, stone[0])
            put(p, x, y - 1 if not winter else y - 1, stone[3] if not winter else c('#FFFFFF'))
    return img


SOIL = ramp('#5E3A1E', '#764A27', '#8B5A2E', '#A06C39', '#B9844C')
SOIL_WET = ramp('#3A2313', '#4C2F19', '#5E3B20', '#714A29', '#8D6844')


def soil(watered):
    """Tilled ground: four furrows per tile, the ridges lit from above and the troughs in shadow."""
    R = SOIL_WET if watered else SOIL
    img, _ = new()
    p = img.load()
    rows = (R[0], R[1], R[2], R[3], R[3], R[2], R[2], R[1])
    for y in range(T):
        for x in range(T):
            col = rows[y % 8]
            n = noise('soil', x, y, 4)
            if col == R[2] and n > 0.72:
                col = R[3]
            elif col == R[3] and n < 0.25:
                col = R[2]
            p[x, y] = col
    r = rng('clods', watered)
    for _ in range(14):
        x, y = r.randrange(T), r.randrange(T)
        if y % 8 in (3, 4):
            put(p, x, y - 1, R[4])
            put(p, x, y, R[3])
        else:
            put(p, x, y, R[0])
    if watered:
        for _ in range(6):
            x, y = r.randrange(T), r.randrange(T)
            if y % 8 in (3, 4):
                put(p, x, y, c('#9C7C5C'))
    return img


WATER = ramp('#2A5FA8', '#3270BC', '#3D82CC', '#5A9EDF', '#9FD0F2', '#E6F6FF')
WAVES = ((3, 6, 5), (19, 3, 4), (10, 14, 6), (25, 17, 4), (2, 23, 4), (16, 26, 5), (27, 29, 3))


def water(frame):
    R = WATER
    img, _ = new()
    p = img.load()
    for y in range(T):
        for x in range(T):
            v = noise('water', x + frame * 2, y, 8) * 0.7 + noise('ripple', x, y + frame, 4) * 0.3
            p[x, y] = R[1] if v < 0.38 else (R[3] if v > 0.74 else R[2])
    for i, (x, y, length) in enumerate(WAVES):
        shift = (frame * 2 + i) % 3 - 1
        for k in range(length):
            put(p, x + shift + k, y, R[4])
        put(p, x + shift + 1, y - 1, R[4])
        put(p, x + shift + length - 2, y - 1, R[4])
        if (i + frame) % 3 == 0:
            put(p, x + shift + length // 2, y - 1, R[5])
    return img


def rock():
    S = ramp('#3E3A42', '#5E5A62', '#7E7A82', '#A29EA6', '#C9C5CC')
    img, d = new()
    d.ellipse([3, 11, 28, 29], fill=S[0])
    d.ellipse([3, 10, 27, 26], fill=S[1])
    d.ellipse([5, 10, 22, 21], fill=S[2])
    d.ellipse([8, 11, 15, 15], fill=S[3])
    d.point([(9, 12), (10, 12)], fill=S[4])
    d.line([(18, 15), (20, 19), (19, 23)], fill=S[0])
    d.line([(12, 22), (15, 24)], fill=S[0])
    for (x, y) in ((6, 16), (7, 15), (20, 11), (21, 12), (22, 11)):
        d.point((x, y), fill=c('#5E8A3A'))
    return auto_outline(img, 0.55)


# ---------------------------------------------------------------- trees and bushes
CANOPY = {
    'SPRING': ramp('#2F6A2A', '#4A923A', '#6DB64A', '#9BD86A'),
    'SUMMER': ramp('#1F5226', '#2E6E30', '#46893A', '#69A84C'),
    'AUTUMN': ramp('#7A2E12', '#B44E1A', '#DD7A26', '#F2A84A'),
}
BARK = ramp('#3E2714', '#5A3A1E', '#7A5230', '#946640')
OAK_BLOBS = ((32, 24, 18), (18, 31, 11), (46, 31, 11), (24, 14, 11), (40, 14, 11), (32, 36, 11), (32, 8, 8))


def trunk(d, x0, x1, y0, y1):
    d.rectangle([x0, y0, x1, y1], fill=BARK[1])
    d.rectangle([x0, y0, x0 + 1, y1], fill=BARK[2])
    d.rectangle([x1, y0, x1, y1], fill=BARK[0])
    d.polygon([(x0 - 4, y1), (x0, y1 - 5), (x0, y1)], fill=BARK[1])
    d.polygon([(x1 + 4, y1), (x1, y1 - 5), (x1, y1)], fill=BARK[0])
    for (x, y) in ((x0 + 3, y0 + 4), (x0 + 5, y0 + 10), (x0 + 3, y0 + 15)):
        d.line([(x, y), (x, y + 3)], fill=BARK[0])


def oak(season):
    img, d = new(64, 64)
    trunk(d, 28, 36, 38, 61)
    p = img.load()
    if season == 'WINTER':
        for (ex, ey, w) in ((14, 20, 2), (22, 9, 2), (32, 4, 2), (42, 9, 2), (50, 20, 2), (24, 27, 1), (40, 27, 1)):
            d.line([(32, 40), (ex, ey)], fill=BARK[1], width=w)
            d.line([(ex, ey), (ex + (4 if ex < 32 else -4), ey - 4)], fill=BARK[1], width=1)
        d.line([(32, 40), (32, 4)], fill=BARK[2], width=2)
        for y in range(1, 64):
            for x in range(64):
                if p[x, y][3] and p[x, y - 1][3] == 0 and y < 40:
                    p[x, y - 1] = c('#FFFFFF')
        return auto_outline(img, 0.5)
    R = CANOPY[season]
    for cx, cy, r in OAK_BLOBS:
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=R[0])
    for cx, cy, r in OAK_BLOBS:
        d.ellipse([cx - r + 1, cy - r, cx + r - 2, cy + r - 3], fill=R[1])
    for cx, cy, r in OAK_BLOBS:
        if r > 8:
            d.ellipse([cx - r + 3, cy - r + 2, cx + r // 3, cy - r // 5], fill=R[2])
    r = rng('leaves', season)
    for _ in range(90):
        x, y = r.randrange(6, 58), r.randrange(0, 46)
        if p[x, y][3] == 0:
            continue
        if p[x, y] == R[1]:
            p[x, y] = R[0]
            if x + 1 < 64 and p[x + 1, y - 1][3]:
                p[x + 1, y - 1] = R[0]
        elif p[x, y] == R[2]:
            p[x, y] = R[3]
    extras = {'SPRING': (c('#F7B6D2'), c('#FFFFFF'), 16), 'SUMMER': (c('#D23A2A'), c('#FF9A80'), 8),
              'AUTUMN': (c('#F2C04A'), c('#FFE08A'), 10)}[season]
    placed = 0
    while placed < extras[2]:
        x, y = r.randrange(10, 54), r.randrange(4, 42)
        if p[x, y][3] and p[x + 1, y + 1][3] and p[x, y] in (R[1], R[2]):
            p[x, y] = extras[0]
            p[x + 1, y] = extras[0]
            p[x, y + 1] = dark(extras[0], 0.8)
            p[x + 1, y + 1] = dark(extras[0], 0.8)
            p[x, y] = extras[1]
            placed += 1
    return auto_outline(img, 0.5)


PINE = ramp('#163D24', '#21552F', '#2F6E3B', '#468A4C')
TIERS = ((4, 26, 13), (14, 38, 17), (25, 51, 21))


def pine(season):
    img, d = new(64, 64)
    trunk(d, 29, 34, 48, 61)
    p = img.load()
    for top, bottom, half in TIERS:
        d.polygon([(32, top), (32 - half, bottom), (32 + half, bottom)], fill=PINE[0])
        d.polygon([(32, top + 1), (32 - half + 2, bottom - 1), (32 + half - 5, bottom - 1)], fill=PINE[1])
        d.polygon([(31, top + 3), (32 - half + 5, bottom - 2), (31, bottom - 4)], fill=PINE[2])
        for k in range(-half + 3, half - 2, 4):
            d.point((32 + k, bottom), fill=PINE[0])
            d.point((32 + k + 1, bottom + 1), fill=PINE[0])
    r = rng('needles', season)
    for _ in range(50):
        x, y = r.randrange(10, 54), r.randrange(4, 52)
        if p[x, y] == PINE[1]:
            p[x, y] = PINE[3] if r.random() < 0.4 else PINE[0]
    if season == 'WINTER':
        for top, bottom, half in TIERS:
            snow_line = top + (bottom - top) // 2
            for y in range(top, snow_line + 1):
                for x in range(64):
                    if p[x, y][3] and (y < snow_line - 1 or (x + y) % 3):
                        p[x, y] = c('#FFFFFF') if x < 32 else c('#D9E5F2')
    return auto_outline(img, 0.5)


def bush(season):
    img, d = new()
    R = CANOPY.get(season, ramp('#6E7F72', '#8C9C90', '#B2C0B6', '#D6E0DA'))
    for cx, cy, rr in ((16, 19, 10), (9, 23, 6), (23, 23, 6), (16, 13, 6)):
        blob(d, cx, cy, rr, R)
    p = img.load()
    if season == 'SUMMER':
        for (x, y) in ((10, 18), (20, 15), (14, 24), (22, 22)):
            p[x, y] = c('#5A6BD8')
            p[x + 1, y] = c('#3A4AB8')
    if season == 'WINTER':
        for y in range(1, T):
            for x in range(T):
                if p[x, y][3] and p[x, y - 1][3] == 0:
                    p[x, y] = c('#FFFFFF')
                    if y + 1 < T and p[x, y + 1][3]:
                        p[x, y + 1] = c('#FFFFFF')
    return auto_outline(img, 0.5)


FENCE = ramp('#4A2F18', '#6E4826', '#986636', '#BD8A50')


# ---------------------------------------------------------------- buildings
def shingles(img, box, R, row=6, width=6):
    """Fills `box` with rows of overlapping shingles; the top row is the ridge."""
    x0, y0, x1, y1 = box
    p = img.load()
    for y in range(y0, y1 + 1):
        band = (y - y0) % row
        offset = ((y - y0) // row) % 2 * (width // 2)
        for x in range(x0, x1 + 1):
            col = R[2] if band in (0, 1) else R[1]
            if band == row - 1:
                col = R[0]
            elif (x + offset) % width == 0 and band > 0:
                col = R[0]
            elif band == 0 and (x + offset) % width in (1, 2):
                col = R[3]
            p[x, y] = col
    for y in range(y0, y0 + 3):
        for x in range(x0, x1 + 1):
            p[x, y] = R[3] if y == y0 else R[2]


def planks(img, box, R, width=5, vertical=True, seed='planks'):
    x0, y0, x1, y1 = box
    p = img.load()
    r = rng(seed)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            k = (x - x0) if vertical else (y - y0)
            band = k // width
            col = R[1] if band % 2 else R[2]
            if k % width == 0:
                col = R[0]
            p[x, y] = col
    for _ in range((x1 - x0) * (y1 - y0) // 60):
        x, y = r.randrange(x0 + 1, x1), r.randrange(y0 + 1, y1)
        p[x, y] = R[0]


def stones(img, box, R, seed='stones'):
    x0, y0, x1, y1 = box
    p = img.load()
    for y in range(y0, y1 + 1):
        row = (y - y0) // 6
        for x in range(x0, x1 + 1):
            off = 5 if row % 2 else 0
            if (y - y0) % 6 == 5 or (x - x0 + off) % 10 == 0:
                p[x, y] = R[0]
            elif (y - y0) % 6 == 0:
                p[x, y] = R[3]
            else:
                p[x, y] = R[2] if (x + y + row) % 7 else R[1]


def window(d, x0, y0, w, h, frame):
    d.rectangle([x0 - 1, y0 - 1, x0 + w, y0 + h], fill=frame[0])
    d.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=c('#7FC0DC'))
    d.line([(x0, y0 + h - 1), (x0 + w - 1, y0)], fill=c('#BFE6F5'))
    d.line([(x0 + 1, y0 + h - 1), (x0 + w - 1, y0 + 1)], fill=c('#BFE6F5'))
    d.line([(x0 + w // 2, y0), (x0 + w // 2, y0 + h - 1)], fill=frame[2])
    d.line([(x0, y0 + h // 2), (x0 + w - 1, y0 + h // 2)], fill=frame[2])
    d.rectangle([x0 - 2, y0 + h, x0 + w + 1, y0 + h + 1], fill=frame[3])


def roof_shape(img, box, R, inset=5):
    """A trapezoid roof seen from above and in front: narrower at the ridge than at the eaves."""
    x0, y0, x1, y1 = box
    layer = Image.new('RGBA', img.size, (0, 0, 0, 0))
    shingles(layer, box, R)
    mask = Image.new('L', img.size, 0)
    ImageDraw.Draw(mask).polygon([(x0 + inset, y0), (x1 - inset, y0), (x1, y1), (x0, y1)], fill=255)
    img.paste(layer, (0, 0), mask)
    d = ImageDraw.Draw(img)
    d.line([(x0, y1 + 1), (x1, y1 + 1)], fill=R[0])


WOOD = ramp('#6E4222', '#91592E', '#B37640', '#D09456')
RED_WOOD = ramp('#6E2418', '#8F3221', '#B5473B', '#CF6A55')
TRIM = ramp('#B9AE94', '#D9CFB6', '#F2E8D0', '#FFFFFF')
FOUNDATION = ramp('#4E4A52', '#6E6A72', '#8E8A92', '#B5B1B8')


def foundation(d, x0, x1, y):
    d.rectangle([x0, y, x1, y + 3], fill=FOUNDATION[1])
    d.line([(x0, y), (x1, y)], fill=FOUNDATION[3])
    for x in range(x0 + 3, x1, 7):
        d.line([(x, y + 1), (x, y + 3)], fill=FOUNDATION[0])


def coop():
    img, d = new(64, 96)
    planks(img, (4, 48, 59, 90), WOOD, seed='coop')
    d.rectangle([4, 48, 59, 50], fill=WOOD[0])
    roof_shape(img, (0, 6, 63, 47), ramp('#6E2418', '#9A3524', '#BF4E3A', '#DE7A5E'))
    d.rectangle([24, 62, 39, 90], fill=WOOD[0])
    planks(img, (25, 63, 38, 90), ramp('#4A2C16', '#5E3A1E', '#764A27', '#946640'), width=4, seed='coop-door')
    d.point((36, 77), fill=c('#E8C547'))
    window(d, 9, 58, 10, 9, WOOD)
    window(d, 45, 58, 10, 9, WOOD)
    for i in range(4):
        d.line([(22 - i, 91 + i), (41 + i, 91 + i)], fill=WOOD[2] if i % 2 else WOOD[1])
    foundation(d, 4, 59, 90)
    d.rectangle([50, 82, 57, 89], fill=c('#E8C547'))
    d.line([(50, 85), (57, 85)], fill=c('#B8941C'))
    return auto_outline(img, 0.5)


def barn():
    img, d = new(96, 96)
    planks(img, (3, 46, 92, 90), RED_WOOD, width=6, seed='barn')
    d.rectangle([3, 46, 92, 48], fill=RED_WOOD[0])
    roof_shape(img, (0, 4, 95, 45), ramp('#33241C', '#4C362A', '#654838', '#83604B'), inset=7)
    d.rectangle([30, 54, 65, 90], fill=TRIM[2])
    for (x0, x1) in ((32, 47), (48, 63)):
        planks(img, (x0, 56, x1, 90), RED_WOOD, width=4, seed=f'barn-door{x0}')
        d.line([(x0, 56), (x1, 90)], fill=TRIM[2], width=2)
        d.line([(x1, 56), (x0, 90)], fill=TRIM[2], width=2)
        d.rectangle([x0, 56, x1, 90], outline=TRIM[1])
    window(d, 10, 58, 11, 9, TRIM)
    window(d, 75, 58, 11, 9, TRIM)
    foundation(d, 3, 92, 90)
    return auto_outline(img, 0.5)


def warehouse():
    img, d = new(64, 96)
    stones(img, (3, 46, 60, 90), FOUNDATION)
    roof_shape(img, (0, 6, 63, 45), ramp('#253044', '#36435E', '#4A5A7A', '#6A7C9C'))
    d.rectangle([18, 58, 45, 90], fill=WOOD[0])
    planks(img, (19, 59, 44, 90), WOOD, width=4, vertical=False, seed='wh-door')
    d.line([(31, 59), (31, 90)], fill=WOOD[0])
    d.line([(32, 59), (32, 90)], fill=WOOD[0])
    for (x, y) in ((5, 81), (50, 80)):
        d.rectangle([x, y, x + 8, y + 9], fill=c('#C9A66B'))
        d.rectangle([x, y, x + 8, y + 1], fill=c('#E0C48A'))
        d.line([(x, y + 5), (x + 8, y + 5)], fill=c('#8A6A3A'))
        d.line([(x + 4, y + 2), (x + 4, y + 9)], fill=c('#8A6A3A'))
    foundation(d, 3, 60, 90)
    return auto_outline(img, 0.5)


def greenhouse():
    img, d = new(96, 128)
    frame = TRIM
    glass = (176, 226, 222, 110)
    roof_glass = (196, 236, 232, 150)
    d.polygon([(8, 4), (87, 4), (95, 40), (0, 40)], fill=roof_glass)
    d.rectangle([2, 41, 93, 123], fill=glass)
    for k in range(0, 9):
        x_top = 8 + k * 79 // 8
        x_bottom = k * 95 // 8
        d.line([(x_top, 4), (x_bottom, 40)], fill=frame[2])
    d.line([(8, 4), (87, 4)], fill=frame[3], width=2)
    d.line([(0, 40), (95, 40)], fill=frame[1], width=2)
    for x in (2, 32, 63, 93):
        d.line([(x, 41), (x, 123)], fill=frame[2], width=2)
    for y in (72, 100):
        d.line([(2, y), (93, y)], fill=frame[2])
    for (x0, y0) in ((10, 10), (44, 10), (8, 48), (40, 76), (70, 48)):
        d.line([(x0, y0 + 8), (x0 + 8, y0)], fill=(255, 255, 255, 170), width=2)
    d.rectangle([40, 98, 55, 123], fill=(150, 205, 200, 160))
    d.rectangle([40, 98, 55, 123], outline=frame[1])
    d.point((53, 111), fill=c('#E8C547'))
    foundation(d, 1, 94, 124)
    return img


# ---------------------------------------------------------------- path and village
PATH = ramp('#7A5B36', '#977445', '#AE8A55', '#C4A068', '#D8B880')


def path_tile():
    """Packed dirt of the path: lighter than the soil, no furrows, a few pebbles; it repeats without seams."""
    img, _ = new()
    p = img.load()
    for y in range(T):
        for x in range(T):
            v = noise('path', x, y, 8) * 0.6 + noise('path2', x, y, 4) * 0.4
            p[x, y] = PATH[1] if v < 0.33 else (PATH[3] if v > 0.66 else PATH[2])
    r = rng('pebbles')
    for _ in range(10):
        x, y = r.randrange(T), r.randrange(T)
        put(p, x, y, PATH[4])
        put(p, x + 1, y + 1, PATH[0])
    return img


PLASTER = ramp('#B9A88A', '#D6C6A6', '#EDE0C4', '#FFF6E2')
WHITE_WALL = ramp('#A9B4BE', '#CBD4DC', '#E8EEF2', '#FFFFFF')
STONE = ramp('#5E5A58', '#7E7A76', '#9C9790', '#BDB8B0')


def door(img, d, x0, y0, x1, y1, R=None):
    R = R or ramp('#4A2C16', '#5E3A1E', '#764A27', '#946640')
    d.rectangle([x0 - 1, y0 - 1, x1 + 1, y1], fill=WOOD[0])
    planks(img, (x0, y0, x1, y1), R, width=3, seed=f'door{x0}{y0}')
    d.point((x1 - 2, (y0 + y1) // 2), fill=c('#E8C547'))
    d.rectangle([x0 - 3, y1 + 1, x1 + 3, y1 + 2], fill=STONE[2])


def house_body(img, d, w, h, wall, roof, seed, roof_h=None):
    """A house seen in 3/4: a roof that takes the upper part and a front wall with a foundation."""
    roof_h = roof_h or h // 2
    if callable(wall):
        wall(img, (3, roof_h, w - 4, h - 6))
    else:
        planks(img, (3, roof_h, w - 4, h - 6), wall, width=5, vertical=False, seed=seed)
    roof_shape(img, (0, 4, w - 1, roof_h - 1), roof, inset=8)
    foundation(d, 3, w - 4, h - 6)


def home():
    img, d = new(128, 128)
    house_body(img, d, 128, 128, WOOD, ramp('#5E2018', '#8A2F22', '#B04434', '#D06650'), 'home')
    d.rectangle([92, 6, 103, 26], fill=STONE[1])
    d.rectangle([92, 6, 103, 8], fill=STONE[3])
    door(img, d, 56, 92, 71, 121)
    window(d, 16, 84, 16, 14, WOOD)
    window(d, 96, 84, 16, 14, WOOD)
    for x in (14, 96):
        d.rectangle([x, 100, x + 19, 104], fill=WOOD[1])
        for k in range(4):
            d.point((x + 3 + k * 5, 99), fill=c('#E85A7A' if k % 2 else '#FFD23F'))
    return auto_outline(img, 0.5)


def shop():
    img, d = new(160, 128)
    house_body(img, d, 160, 128, lambda im, box: planks(im, box, PLASTER, width=6, vertical=False, seed='shop'),
               ramp('#2F4F2A', '#3F6A36', '#548A46', '#74AA5E'), 'shop')
    for i in range(10):
        col = c('#D8323A') if i % 2 == 0 else c('#FFF4D6')
        d.polygon([(8 + i * 14, 70), (22 + i * 14, 70), (22 + i * 14, 80), (8 + i * 14, 80)], fill=col)
    d.line([(8, 80), (148, 80)], fill=c('#8F1F25'))
    d.rectangle([58, 54, 101, 66], fill=WOOD[1])
    d.rectangle([60, 56, 99, 64], fill=c('#F2E3B8'))
    for k, col in enumerate(('#E07B24', '#D8323A', '#56A03A', '#E8C547', '#7A5AA8')):
        d.rectangle([64 + k * 7, 58, 68 + k * 7, 62], fill=c(col))
    door(img, d, 72, 92, 87, 121)
    window(d, 18, 88, 22, 16, WOOD)
    window(d, 118, 88, 22, 16, WOOD)
    for x in (16, 120):
        d.rectangle([x, 110, x + 22, 121], fill=WOOD[2])
        for k in range(4):
            blob(d, x + 4 + k * 5, 109, 3, ramp('#9E3A1A', '#E07B24', '#FFB060'))
    return auto_outline(img, 0.5)


def farmhouse():
    img, d = new(128, 128)
    house_body(img, d, 128, 128, lambda im, box: stones(im, box, STONE, seed='farmhouse'),
               ramp('#3A2A1E', '#56402C', '#735840', '#927458'), 'farmhouse')
    door(img, d, 56, 92, 71, 121)
    window(d, 18, 86, 16, 14, WOOD)
    window(d, 94, 86, 16, 14, WOOD)
    d.rectangle([100, 104, 118, 121], fill=c('#C9A66B'))
    d.line([(100, 112), (118, 112)], fill=c('#8A6A3A'))
    d.line([(109, 104), (109, 121)], fill=c('#8A6A3A'))
    return auto_outline(img, 0.5)


def clinic():
    img, d = new(160, 128)
    house_body(img, d, 160, 128, lambda im, box: planks(im, box, WHITE_WALL, width=6, vertical=False, seed='clinic'),
               ramp('#1F3F6E', '#2C5490', '#3E6EB5', '#6A95D6'), 'clinic')
    d.rectangle([70, 58, 89, 77], fill=WHITE_WALL[3])
    d.rectangle([77, 60, 82, 75], fill=c('#D8323A'))
    d.rectangle([72, 65, 87, 70], fill=c('#D8323A'))
    door(img, d, 72, 92, 87, 121, ramp('#4C6A84', '#5E7E9A', '#7494B0', '#9AB6CE'))
    window(d, 20, 88, 22, 16, WHITE_WALL)
    window(d, 118, 88, 22, 16, WHITE_WALL)
    return auto_outline(img, 0.5)


def cottage():
    img, d = new(128, 128)
    house_body(img, d, 128, 128, lambda im, box: planks(im, box, PLASTER, width=5, vertical=False, seed='cottage'),
               ramp('#3E2A5E', '#55397E', '#7050A0', '#9478C2'), 'cottage')
    door(img, d, 56, 92, 71, 121)
    window(d, 18, 86, 16, 14, WOOD)
    window(d, 94, 86, 16, 14, WOOD)
    for x in (16, 92):
        d.rectangle([x, 102, x + 19, 106], fill=WOOD[1])
        for k in range(4):
            d.point((x + 3 + k * 5, 101), fill=c('#F7A8C8' if k % 2 else '#FFFFFF'))
    for k, col in enumerate(('#E85A7A', '#FFD23F', '#7FB2F0')):
        blob(d, 10 + k * 8, 120, 3, ramp('#2C5E22', '#3E7D2C', '#56A03A'))
        d.point((10 + k * 8, 118), fill=c(col))
    return auto_outline(img, 0.5)


# ---------------------------------------------------------------- crops
LEAF = ramp('#2C5E22', '#3E7D2C', '#56A03A', '#7CC24E', '#A6DE6A')


def mound(d, small=False):
    if small:
        d.ellipse([11, 25, 20, 29], fill=SOIL[1])
        d.line([(13, 25), (18, 25)], fill=SOIL[3])
    else:
        d.ellipse([9, 24, 22, 30], fill=SOIL[1])
        d.ellipse([10, 24, 21, 27], fill=SOIL[2])
        d.line([(12, 24), (18, 24)], fill=SOIL[4])


def leaf(d, x, y, side, size=4, R=LEAF):
    """A small leaf growing from (x, y) to the left (side=-1) or right (side=1)."""
    tip = (x + side * size, y - size // 2)
    d.polygon([(x, y), tip, (x + side * (size // 2), y + 1)], fill=R[2])
    d.line([(x, y), tip], fill=R[1])
    d.point((x + side * (size - 1), y - size // 2), fill=R[4])


def stem(d, x, y0, y1, R=LEAF):
    d.line([(x, y0), (x, y1)], fill=R[1])
    d.line([(x + 1, y0), (x + 1, y1)], fill=R[0])


def crop_growing(d, name):
    if name == 'CORN':
        stem(d, 15, 27, 8)
        for (y, side, size) in ((24, -1, 8), (19, 1, 8), (14, -1, 7), (10, 1, 6)):
            leaf(d, 15 if side < 0 else 16, y, side, size)
    elif name == 'PUMPKIN':
        for (cx, cy, r) in ((10, 23, 5), (21, 22, 5), (16, 18, 4)):
            blob(d, cx, cy, r, LEAF[1:])
        d.arc([4, 22, 14, 30], 180, 330, fill=LEAF[1])
    elif name == 'TOMATO':
        d.rectangle([20, 8, 21, 28], fill=FENCE[2])
        stem(d, 14, 27, 12)
        for (y, side) in ((24, -1), (20, 1), (16, -1), (12, 1)):
            leaf(d, 14 if side < 0 else 15, y, side, 5)
        d.ellipse([16, 18, 19, 21], fill=c('#7CB342'))
    elif name == 'POTATO':
        for (cx, cy, r) in ((12, 21, 5), (20, 21, 5), (16, 15, 5)):
            blob(d, cx, cy, r, LEAF[1:])
    else:
        for (x, top) in ((13, 13), (16, 10), (19, 13)):
            d.line([(16, 26), (x, top)], fill=LEAF[1])
            for k in range(top, 24, 3):
                d.point((x - 1 + (k % 2) * 2, k), fill=LEAF[3])
                d.point((x, k + 1), fill=LEAF[2])
    mound(d, small=True)


def crop_mature(d, name):
    if name == 'CARROT':
        mound(d)
        d.polygon([(13, 23), (19, 23), (16, 30)], fill=c('#F28C28'))
        d.line([(14, 24), (16, 24)], fill=c('#FFB35C'))
        d.line([(15, 26), (17, 26)], fill=c('#C86E14'))
        for (x, top) in ((11, 9), (14, 6), (16, 5), (18, 6), (21, 9)):
            d.line([(16, 23), (x, top)], fill=LEAF[1])
            for k in range(top, 21, 2):
                fx = x + (16 - x) * (k - top) // max(1, 23 - top)
                d.point((fx - 1, k), fill=LEAF[3])
                d.point((fx + 1, k + 1), fill=LEAF[2])
    elif name == 'POTATO':
        for (cx, cy, r) in ((10, 18, 6), (22, 18, 6), (16, 12, 6), (16, 20, 6)):
            blob(d, cx, cy, r, LEAF[1:])
        for (x, y) in ((11, 11), (20, 10), (15, 6), (23, 15)):
            d.point([(x, y - 1), (x - 1, y), (x + 1, y), (x, y + 1)], fill=c('#E9D8FF'))
            d.point((x, y), fill=c('#F2C23A'))
        for (x, y) in ((8, 26), (19, 27)):
            d.ellipse([x, y - 2, x + 5, y + 2], fill=c('#C9A66B'))
            d.point((x + 1, y - 1), fill=c('#E6CD96'))
    elif name == 'TOMATO':
        d.rectangle([22, 4, 23, 29], fill=FENCE[2])
        d.point((22, 4), fill=FENCE[3])
        stem(d, 15, 28, 6)
        for (y, side) in ((25, -1), (21, 1), (17, -1), (13, 1), (9, -1)):
            leaf(d, 15 if side < 0 else 16, y, side, 6)
        for (x, y) in ((9, 15), (18, 11), (10, 22), (18, 19)):
            d.ellipse([x, y, x + 5, y + 5], fill=c('#D0312D'))
            d.ellipse([x + 1, y + 1, x + 3, y + 3], fill=c('#E8584A'))
            d.point((x + 1, y + 1), fill=c('#FFB3A3'))
            d.point((x + 2, y), fill=LEAF[1])
    elif name == 'CORN':
        stem(d, 15, 29, 3)
        for (y, side, size) in ((26, -1, 8), (21, 1, 8), (16, -1, 7), (11, 1, 6)):
            leaf(d, 15 if side < 0 else 16, y, side, size)
        for (x, y) in ((17, 12), (10, 17)):
            d.ellipse([x, y, x + 4, y + 9], fill=c('#F2D43D'))
            for k in range(y + 1, y + 9, 2):
                d.point((x + 1, k), fill=c('#C9A81C'))
                d.point((x + 3, k + 1), fill=c('#FFF08A'))
            d.polygon([(x, y + 9), (x + 2, y + 3), (x + 4, y + 9)], fill=LEAF[2])
        d.line([(15, 3), (13, 0)], fill=c('#D9B85A'))
        d.line([(16, 3), (18, 0)], fill=c('#D9B85A'))
    elif name == 'PUMPKIN':
        d.polygon([(3, 22), (9, 15), (12, 22)], fill=LEAF[2])
        d.polygon([(20, 14), (28, 16), (24, 21)], fill=LEAF[2])
        PK = ramp('#9A4A0C', '#C9621A', '#E8802A', '#F7A64A')
        d.ellipse([6, 14, 26, 30], fill=PK[0])
        d.ellipse([7, 14, 25, 28], fill=PK[1])
        d.ellipse([11, 14, 21, 28], fill=PK[2])
        d.ellipse([13, 16, 16, 20], fill=PK[3])
        for x in (11, 21):
            d.line([(x, 16), (x, 27)], fill=PK[0])
        d.rectangle([15, 10, 17, 15], fill=c('#5E7A2A'))
        d.point((15, 10), fill=c('#7E9A3A'))
    else:
        mound(d)


def crop_sprout(d, name):
    mound(d, small=True)
    stem(d, 15, 26, 19)
    leaf(d, 15, 20, -1, 5)
    leaf(d, 16, 19, 1, 5)


def crop_seed(d, name):
    mound(d)
    for (x, y) in ((12, 26), (16, 25), (19, 27)):
        d.point((x, y), fill=c('#E6CD96'))
        d.point((x + 1, y), fill=c('#B8955A'))


def crop_withered(d, name):
    brown = ramp('#5A4024', '#7A5A34', '#A68B5B', '#C4AA78')
    mound(d, small=True)
    d.line([(16, 27), (16, 17), (14, 13)], fill=brown[1])
    d.line([(16, 21), (11, 18), (9, 21)], fill=brown[2])
    d.line([(16, 19), (21, 16), (23, 20)], fill=brown[2])
    d.line([(14, 13), (12, 15)], fill=brown[2])
    d.point([(9, 22), (23, 21)], fill=brown[0])
    d.polygon([(20, 26), (24, 25), (23, 28)], fill=brown[3])


def crop(name, stage):
    img, d = new()
    {'SEED': crop_seed, 'SPROUT': crop_sprout, 'GROWING': crop_growing,
     'MATURE': crop_mature, 'WITHERED': crop_withered}[stage](d, name)
    return auto_outline(img, 0.5)


# ---------------------------------------------------------------- animals (facing right)
HEN = ramp('#B9AE9C', '#DCD3C4', '#F2EDE3', '#FFFFFF')


def hen(frame):
    img, d = new()
    bob = (0, -1, 0)[frame]
    for (x, lift) in ((13, frame == 1), (18, frame == 2)):
        d.line([(x, 24), (x, 28 - (1 if lift else 0))], fill=c('#E0A030'))
        d.line([(x - 1, 28 - (1 if lift else 0)), (x + 1, 28 - (1 if lift else 0))], fill=c('#E0A030'))
    d.ellipse([6, 13 + bob, 24, 26 + bob], fill=HEN[1])
    d.ellipse([7, 13 + bob, 22, 23 + bob], fill=HEN[2])
    d.ellipse([9, 14 + bob, 15, 18 + bob], fill=HEN[3])
    d.polygon([(7, 16 + bob), (2, 11 + bob), (4, 19 + bob)], fill=HEN[1])
    d.polygon([(6, 16 + bob), (3, 12 + bob), (5, 17 + bob)], fill=HEN[2])
    d.arc([10, 17 + bob, 19, 24 + bob], 20, 160, fill=HEN[0])
    hy = 7 + bob + (5 if frame == 2 else 0)
    d.ellipse([18, hy, 26, hy + 8], fill=HEN[2])
    d.point((19, hy + 2), fill=HEN[3])
    d.polygon([(26, hy + 3), (30, hy + 4), (26, hy + 5)], fill=c('#F2A33A'))
    d.rectangle([20, hy - 2, 24, hy], fill=c('#D8323A'))
    d.point((21, hy - 3), fill=c('#D8323A'))
    d.point((23, hy - 3), fill=c('#D8323A'))
    d.point((23, hy + 3), fill=c('#1A1A1A'))
    d.rectangle([25, hy + 6, 26, hy + 8], fill=c('#D8323A'))
    return auto_outline(img, 0.45)


COW = ramp('#BDB4A8', '#DED7CC', '#F4F0E8', '#FFFFFF')


def cow(frame):
    img, d = new()
    bob = (0, -1, 0)[frame]
    for i, x in enumerate((6, 10, 19, 23)):
        lift = (frame == 1 and i % 2 == 0) or (frame == 2 and i % 2 == 1)
        d.rectangle([x, 22 + bob, x + 2, 28 - (1 if lift else 0)], fill=COW[1])
        d.rectangle([x, 27 - (1 if lift else 0), x + 2, 28 - (1 if lift else 0)], fill=c('#4A3A2A'))
    d.ellipse([3, 10 + bob, 27, 25 + bob], fill=COW[1])
    d.ellipse([4, 10 + bob, 25, 22 + bob], fill=COW[2])
    for (x, y, w, h) in ((7, 12, 6, 5), (16, 15, 6, 5), (10, 18, 4, 3)):
        d.ellipse([x, y + bob, x + w, y + h + bob], fill=c('#3A2E28'))
        d.point((x + 1, y + 1 + bob), fill=c('#5A4A40'))
    d.ellipse([6, 22 + bob, 10, 26 + bob], fill=c('#F2A8B0'))
    hy = 8 + bob + (4 if frame == 2 else 0)
    d.rectangle([22, hy, 30, hy + 8], fill=COW[2])
    d.rectangle([22, hy, 23, hy + 8], fill=COW[1])
    d.rectangle([24, hy + 5, 30, hy + 8], fill=c('#F2A8B0'))
    d.point([(26, hy + 6), (29, hy + 6)], fill=c('#B5656A'))
    d.point((25, hy + 2), fill=c('#1A1A1A'))
    d.polygon([(22, hy), (20, hy - 3), (24, hy)], fill=c('#E8D8A8'))
    d.polygon([(30, hy), (31, hy - 3), (28, hy)], fill=c('#E8D8A8'))
    d.rectangle([19, hy + 2, 21, hy + 4], fill=COW[1])
    tail = (2, 1, 3)[frame]
    d.line([(4, 13 + bob), (tail, 20 + bob)], fill=COW[0])
    d.point((tail, 21 + bob), fill=c('#3A2E28'))
    return auto_outline(img, 0.45)


# ---------------------------------------------------------------- items
def packet(col_hex):
    img, d = new()
    d.polygon([(8, 6), (24, 6), (23, 28), (9, 28)], fill=c('#F4E7B0'))
    d.polygon([(8, 6), (12, 6), (11, 28), (9, 28)], fill=c('#FFF6D2'))
    d.rectangle([10, 12, 22, 21], fill=c(col_hex))
    d.rectangle([10, 20, 22, 21], fill=dark(c(col_hex), 0.8))
    d.rectangle([8, 6, 24, 8], fill=c('#C9A66B'))
    for (x, y) in ((13, 15), (17, 17), (15, 19), (19, 14)):
        d.point((x, y), fill=c('#FFFFFF'))
    return auto_outline(img, 0.5)


def item_crop(name):
    img, d = new()
    crop_mature(d, name)
    return auto_outline(img, 0.5)


def egg():
    img, d = new()
    d.ellipse([9, 7, 23, 27], fill=c('#E8DCC6'))
    d.ellipse([9, 7, 22, 25], fill=c('#FFF8EC'))
    d.ellipse([12, 10, 15, 14], fill=c('#FFFFFF'))
    return auto_outline(img, 0.5)


def milk():
    img, d = new()
    d.rectangle([11, 10, 21, 28], fill=c('#F4F7FA'))
    d.rectangle([11, 10, 12, 28], fill=c('#FFFFFF'))
    d.rectangle([20, 10, 21, 28], fill=c('#D2DCE6'))
    d.rectangle([13, 5, 19, 10], fill=c('#F4F7FA'))
    d.rectangle([12, 3, 20, 5], fill=c('#4A90D9'))
    d.rectangle([11, 16, 21, 22], fill=c('#4A90D9'))
    d.rectangle([13, 18, 19, 20], fill=c('#F4F7FA'))
    return auto_outline(img, 0.5)


def hay():
    img, d = new()
    d.ellipse([5, 10, 27, 28], fill=c('#C9A81C'))
    d.ellipse([5, 10, 26, 25], fill=c('#E8C547'))
    r = rng('hay')
    for _ in range(26):
        x, y = r.randrange(8, 25), r.randrange(12, 26)
        d.line([(x, y), (x + r.randrange(-2, 3), y - 3)], fill=c('#B8941C') if r.random() < 0.5 else c('#F7E08A'))
    d.rectangle([5, 17, 27, 19], fill=c('#8A6A3A'))
    return auto_outline(img, 0.5)


def medicine():
    img, d = new()
    d.rectangle([10, 10, 22, 28], fill=c('#F4F7FA'))
    d.rectangle([10, 10, 11, 28], fill=c('#FFFFFF'))
    d.rectangle([12, 5, 20, 10], fill=c('#8A8A8A'))
    d.rectangle([12, 5, 20, 6], fill=c('#B5B5B5'))
    d.rectangle([14, 15, 18, 25], fill=c('#D8323A'))
    d.rectangle([12, 18, 20, 22], fill=c('#D8323A'))
    return auto_outline(img, 0.5)


# ---------------------------------------------------------------- characters
SKIN = ramp('#C98E64', '#E6B088', '#F6CBA4', '#FFE2C6')


def hair_extras(d, direction, flip, side, h, h_dark, style):
    """What makes a hair style: long hair over the shoulders, a bun on top, two braids, or thin grey hair."""
    if style == 'long':
        if direction == 'N':
            d.rounded_rectangle([6, 12, 25, 36], radius=4, fill=h)
            d.line([(12, 16), (12, 35)], fill=h_dark)
            d.line([(19, 16), (19, 35)], fill=h_dark)
        elif side:
            bx = (6, 13) if not flip else (18, 25)
            d.rounded_rectangle([bx[0], 12, bx[1], 36], radius=3, fill=h)
            d.line([(bx[0] + 3, 18), (bx[0] + 3, 35)], fill=h_dark)
        else:
            d.rounded_rectangle([5, 14, 9, 36], radius=2, fill=h)
            d.rounded_rectangle([22, 14, 26, 36], radius=2, fill=h)
            d.line([(7, 18), (7, 35)], fill=h_dark)
            d.line([(24, 18), (24, 35)], fill=h_dark)
    elif style == 'bun':
        if side:
            bx = 4 if not flip else 20
            d.ellipse([bx, 6, bx + 8, 14], fill=h)
            d.arc([bx + 1, 7, bx + 7, 13], 200, 340, fill=h_dark)
        else:
            d.ellipse([11, 1, 20, 9], fill=h)
            d.arc([12, 2, 19, 8], 200, 340, fill=h_dark)
    elif style == 'braids':
        xs = [(5, 8), (23, 26)] if not side else ([(5, 8)] if not flip else [(23, 26)])
        for x0, x1 in xs:
            for y in range(18, 36, 4):
                d.ellipse([x0, y, x1, y + 4], fill=h)
                d.line([(x0 + 1, y + 3), (x1 - 1, y + 3)], fill=h_dark)
            d.rectangle([x0, 36, x1, 37], fill=c('#D8323A'))
    elif style == 'thin':
        d.rectangle([9, 9, 22, 11], fill=dark(h, 0.92))


def person(direction, frame, shirt, pants, hair, hat=None, apron=None, coat=None, skirt=False, skin=None,
           style='short', beard=False, glasses=False, cap=None, overalls=None, flower=None, earrings=False,
           stethoscope=False, eyes='#2A1A10', wide=False):
    """A villager of 32 x 64 seen in 3/4: one plot wide and two tall, the feet on the bottom row (RN-40).

    A big head (about a third of the height) over a short body, as in classic farm games; frame 0 stands still,
    frames 1 and 2 swing the legs and arms the opposite way. Each villager differs in skin, hair style (short,
    long, bun, braids or grey and thin), face (beard, glasses, eye colour), build and what they wear, so no two
    look like the same person.
    """
    SKIN = ramp(*skin) if skin else globals()['SKIN']
    img, d = new(T, 2 * T)
    top = c(coat or shirt)
    top_lit = tuple(min(255, int(v * 1.12)) for v in top[:3]) + (255,)
    top_dark = dark(top, 0.74)
    legs = c(pants)
    legs_dark = dark(legs, 0.78)
    shoe = c('#3A2A1A')
    side = direction in ('E', 'W')
    flip = direction == 'W'
    lift = (0, 1, -1)[frame]
    # legs and shoes
    if skirt:
        d.polygon([(9, 43), (22, 43), (24, 55), (7, 55)], fill=legs)
        d.line([(16, 45), (16, 54)], fill=legs_dark)
        d.rectangle([11, 55, 14, 59 - max(0, lift)], fill=SKIN[1])
        d.rectangle([17, 55, 20, 59 - max(0, -lift)], fill=SKIN[1])
        d.rectangle([10, 59 - max(0, lift), 15, 62 - max(0, lift)], fill=shoe)
        d.rectangle([16, 59 - max(0, -lift), 21, 62 - max(0, -lift)], fill=shoe)
    elif side:
        stride = (0, 3, -3)[frame]
        d.rectangle([13 - stride, 45, 17 - stride, 59], fill=legs_dark)
        d.rectangle([14 + stride, 45, 18 + stride, 59], fill=legs)
        d.rectangle([12 - stride, 59, 18 - stride, 62], fill=shoe)
        d.rectangle([13 + stride, 59, 19 + stride, 62], fill=shoe)
    else:
        d.rectangle([11, 45, 15, 59 - max(0, lift)], fill=legs)
        d.rectangle([16, 45, 20, 59 - max(0, -lift)], fill=legs_dark)
        d.line([(15, 45), (15, 58)], fill=legs_dark)
        d.rectangle([10, 59 - max(0, lift), 15, 62 - max(0, lift)], fill=shoe)
        d.rectangle([16, 59 - max(0, -lift), 21, 62 - max(0, -lift)], fill=shoe)
    # body
    bx0, bx1 = (8, 23) if wide else (9, 22)
    d.rounded_rectangle([bx0, 30, bx1, 46], radius=3, fill=top)
    d.rectangle([10, 31, 12, 44], fill=top_lit)
    d.rectangle([19, 31, 22, 45], fill=top_dark)
    d.line([(9, 45), (22, 45)], fill=top_dark)
    if apron:
        d.rectangle([12, 35, 19, 46], fill=c(apron))
        d.line([(12, 35), (19, 35)], fill=dark(c(apron), 0.85))
    elif coat and not side:
        d.line([(15, 31), (15, 45)], fill=top_dark)
        d.rectangle([12, 31, 13, 37], fill=c(shirt))
        d.rectangle([18, 31, 19, 37], fill=c(shirt))
        if direction == 'S':
            d.rectangle([17, 38, 19, 40], fill=c('#D8323A'))
    elif direction == 'S':
        d.rectangle([11, 32, 12, 44], fill=legs)
        d.rectangle([19, 32, 20, 44], fill=legs)
        d.point([(11, 34), (20, 34)], fill=c('#E8C547'))
    if overalls and not side:
        o = c(overalls)
        d.rectangle([11, 36, 20, 46], fill=o)
        d.rectangle([11, 30, 12, 36], fill=o)
        d.rectangle([19, 30, 20, 36], fill=o)
        d.rectangle([14, 38, 17, 40], fill=dark(o, 0.8))
        if direction == 'S':
            d.point([(12, 36), (19, 36)], fill=c('#E8C547'))
    elif overalls:
        d.rectangle([12, 36, 19, 46], fill=c(overalls))
    if stethoscope and direction == 'S':
        d.arc([11, 27, 20, 38], 20, 160, fill=c('#5A5A6A'))
        d.rectangle([15, 38, 16, 39], fill=c('#9AA4B0'))
    d.rectangle([bx0, 43, bx1, 44], fill=dark(legs, 0.6) if not apron else dark(c(apron), 0.8))
    # arms
    swing = (0, 2, -2)[frame]
    if side:
        ax = 13 if not flip else 15
        d.rectangle([ax, 32 + swing, ax + 3, 42 + swing], fill=top_dark)
        d.rectangle([ax, 42 + swing, ax + 3, 44 + swing], fill=SKIN[2])
    else:
        d.rectangle([6, 32 + swing, 8, 42 + swing], fill=top)
        d.rectangle([23, 32 - swing, 25, 42 - swing], fill=top_dark)
        d.rectangle([6, 42 + swing, 8, 44 + swing], fill=SKIN[2])
        d.rectangle([23, 42 - swing, 25, 44 - swing], fill=SKIN[1])
    # neck and head
    d.rectangle([14, 27, 17, 30], fill=SKIN[1])
    d.ellipse([7, 9, 24, 29], fill=SKIN[2])
    d.ellipse([8, 10, 20, 22], fill=SKIN[3])
    if not side:
        d.chord([7, 9, 24, 29], 300, 60, fill=SKIN[1])
    h = c(hair)
    h_dark = dark(h, 0.75)
    if direction == 'N':
        d.ellipse([7, 8, 24, 28], fill=h)
        d.arc([9, 12, 22, 26], 200, 340, fill=h_dark)
        d.line([(15, 12), (15, 26)], fill=h_dark)
    else:
        d.chord([7, 7, 24, 24], 180, 360, fill=h)
        d.rectangle([7, 15, 9, 22], fill=h)
        d.rectangle([22, 15, 24, 22], fill=h)
        if direction == 'S':
            d.polygon([(10, 15), (15, 15), (12, 18)], fill=h)
            d.polygon([(16, 15), (22, 15), (20, 18)], fill=h)
            d.rectangle([11, 19, 12, 21], fill=c(eyes))
            d.rectangle([19, 19, 20, 21], fill=c(eyes))
            d.point([(11, 19), (19, 19)], fill=c('#FFFFFF'))
            d.rectangle([9, 23, 10, 23], fill=c('#F2A0A0'))
            d.rectangle([21, 23, 22, 23], fill=c('#F2A0A0'))
            d.line([(14, 25), (17, 25)], fill=c('#B5654A'))
        else:
            back = (7, 15) if not flip else (16, 24)
            d.rectangle([back[0], 9, back[1], 24], fill=h)
            d.ellipse([back[0] + (2 if not flip else -2), 9, back[1] + (2 if not flip else -2), 26], fill=h)
            ex = 20 if not flip else 11
            d.rectangle([ex, 19, ex + 1, 21], fill=c(eyes))
            nose = 24 if not flip else 7
            d.point((nose, 22), fill=SKIN[1])
            d.line([(ex - 1, 25), (ex + 1, 25)], fill=c('#B5654A'))
    hair_extras(d, direction, flip, side, h, h_dark, style)
    if beard and direction != 'N':
        if side:
            fx0 = 16 if not flip else 7
            d.chord([fx0, 18, fx0 + 9, 30], 0, 180, fill=h)
        else:
            d.chord([8, 16, 23, 30], 20, 160, fill=h)
            d.rectangle([12, 23, 19, 24], fill=h_dark)
            d.line([(14, 26), (17, 26)], fill=c('#B5654A'))
    if glasses and direction != 'N':
        g = c('#3A3A4A')
        if side:
            gx = 19 if not flip else 10
            d.rectangle([gx - 1, 18, gx + 2, 22], outline=g)
        else:
            d.rectangle([10, 18, 13, 22], outline=g)
            d.rectangle([18, 18, 21, 22], outline=g)
            d.line([(14, 19), (17, 19)], fill=g)
    if earrings and not side and direction == 'S':
        d.rectangle([7, 23, 7, 24], fill=c('#F2C94C'))
        d.rectangle([24, 23, 24, 24], fill=c('#F2C94C'))
    if flower and direction != 'N':
        fx = 21 if not flip else 9
        d.point([(fx, 8), (fx - 1, 9), (fx + 1, 9), (fx, 10)], fill=c(flower))
        d.point((fx, 9), fill=c('#F7E08A'))
    if cap:
        cc = c(cap)
        d.chord([7, 4, 24, 20], 180, 360, fill=cc)
        d.line([(9, 6), (14, 5)], fill=tuple(min(255, int(v * 1.15)) for v in cc[:3]) + (255,))
        if direction == 'S':
            d.rectangle([7, 11, 24, 13], fill=dark(cc, 0.75))
        elif side:
            bx = 18 if not flip else 3
            d.rectangle([bx, 11, bx + 10, 13], fill=dark(cc, 0.75))
    if hat:
        hc = c(hat)
        d.ellipse([3, 11, 28, 17], fill=dark(hc, 0.82))
        d.rounded_rectangle([9, 3, 22, 14], radius=4, fill=hc)
        d.rectangle([9, 11, 22, 13], fill=c('#B5473B'))
        d.line([(11, 4), (16, 4)], fill=c('#FFF2B0'))
    return auto_outline(img, 0.45)


NPCS = {
    # Marta: market seller, long auburn hair, earrings, red blouse with apron and skirt.
    'MARTA': dict(shirt='#D8323A', pants='#4A3A5A', hair='#A0431E', apron='#FFF4D6', skirt=True, style='long',
                  earrings=True, eyes='#3A6A2A', skin=('#B87A50', '#D69A6E', '#E8B488', '#F6CDA8')),
    # Don Tomás: veteran farmer, grey beard, flat cap, broad, plaid green shirt under brown overalls.
    'TOMAS': dict(shirt='#6B8E4E', pants='#5A4030', hair='#B8B8B8', cap='#6A5A48', beard=True, style='thin',
                  overalls='#7A5A3A', wide=True, skin=('#A06A44', '#BE845A', '#D29C72', '#E2B48C')),
    # Dra. Lucía: vet, black hair in a bun, glasses, white coat and stethoscope.
    'LUCIA': dict(shirt='#7FB3D5', pants='#2F4F6F', hair='#1E1410', coat='#F4F7FA', style='bun', glasses=True,
                  stethoscope=True, skin=('#D49A72', '#ECBA92', '#F8D4B4', '#FFE8D2')),
    # Inés: neighbour, dark skin, two black braids with red ties, a flower, purple dress.
    'INES': dict(shirt='#7A5AA8', pants='#5A3F86', hair='#2A1A14', skirt=True, style='braids', flower='#F28AB8',
                 skin=('#6E4428', '#8A5A38', '#A0704A', '#B8885E')),
}


# ---------------------------------------------------------------- icons
def icon_coin():
    img, d = new()
    d.ellipse([5, 5, 26, 26], fill=c('#C9981C'))
    d.ellipse([5, 5, 25, 24], fill=c('#F2C23A'))
    d.ellipse([8, 8, 23, 23], outline=c('#C9981C'), width=2)
    d.rectangle([14, 11, 17, 20], fill=c('#C9981C'))
    d.rectangle([12, 13, 19, 14], fill=c('#C9981C'))
    d.line([(8, 10), (10, 8)], fill=c('#FFF2B0'))
    return auto_outline(img, 0.5)


def icon_energy():
    img, d = new()
    d.polygon([(19, 3), (8, 18), (15, 18), (12, 29), (24, 12), (17, 12)], fill=c('#F2D43D'))
    d.line([(18, 5), (11, 16)], fill=c('#FFF2B0'))
    return auto_outline(img, 0.5)


def icon_sun(dry=False):
    img, d = new()
    for i in range(8):
        a = math.pi * i / 4
        d.line([(16 + math.cos(a) * 9, 16 + math.sin(a) * 9), (16 + math.cos(a) * 14, 16 + math.sin(a) * 14)],
               fill=c('#F2A33A'), width=2)
    d.ellipse([9, 9, 22, 22], fill=c('#F2A33A'))
    d.ellipse([9, 9, 21, 21], fill=c('#FFC21A'))
    d.ellipse([11, 11, 15, 15], fill=c('#FFE58A'))
    if dry:
        d.line([(12, 14), (15, 18), (13, 21)], fill=c('#B5651D'))
    return auto_outline(img, 0.5)


def icon_cloud(rain=False, snow=False, dark_cloud=False):
    img, d = new()
    col = c('#8C99A8' if dark_cloud else '#E6EDF5')
    hi = c('#B3BDC9' if dark_cloud else '#FFFFFF')
    for box in ([4, 10, 16, 22], [10, 5, 24, 19], [16, 11, 28, 22]):
        d.ellipse(box, fill=col)
    d.rectangle([8, 16, 24, 22], fill=col)
    d.ellipse([12, 7, 18, 12], fill=hi)
    if rain:
        for x in (9, 15, 21):
            d.line([(x, 24), (x - 2, 29)], fill=c('#3A7BD5'), width=2)
    if snow:
        for x in (9, 15, 21):
            d.rectangle([x - 1, 25, x + 1, 27], fill=c('#FFFFFF'))
    return auto_outline(img, 0.5)


def icon_flake(col='#BFE3FF'):
    img, d = new()
    for i in range(3):
        a = math.pi * i / 3
        d.line([(16 - math.cos(a) * 13, 16 - math.sin(a) * 13), (16 + math.cos(a) * 13, 16 + math.sin(a) * 13)],
               fill=c(col), width=2)
    d.ellipse([13, 13, 18, 18], fill=c('#FFFFFF'))
    return auto_outline(img, 0.4)


def icon_flower():
    img, d = new()
    d.line([(16, 18), (16, 29)], fill=LEAF[1], width=2)
    d.polygon([(16, 25), (22, 21), (20, 26)], fill=LEAF[2])
    for (x, y) in ((16, 8), (10, 12), (22, 12), (12, 18), (20, 18)):
        d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=c('#F28CB8'))
    d.ellipse([13, 11, 19, 17], fill=c('#FFD23F'))
    d.point((14, 12), fill=c('#FFF2B0'))
    return auto_outline(img, 0.5)


def icon_leaf():
    img, d = new()
    d.polygon([(6, 26), (8, 10), (20, 4), (27, 7), (25, 20), (14, 27)], fill=c('#D9741E'))
    d.polygon([(8, 10), (20, 4), (14, 14)], fill=c('#F29A3A'))
    d.line([(6, 26), (22, 10)], fill=c('#8A3F0C'), width=2)
    d.line([(14, 18), (14, 12)], fill=c('#8A3F0C'))
    return auto_outline(img, 0.5)


def icon_sow():
    img, d = new()
    d.polygon([(7, 14), (25, 14), (23, 29), (9, 29)], fill=c('#C9A66B'))
    d.polygon([(7, 14), (11, 14), (11, 29), (9, 29)], fill=c('#E0C48A'))
    d.rectangle([7, 12, 25, 15], fill=c('#8A6A3A'))
    d.line([(16, 11), (16, 4)], fill=LEAF[1], width=2)
    d.polygon([(16, 7), (10, 4), (12, 9)], fill=LEAF[2])
    d.polygon([(16, 7), (22, 4), (20, 9)], fill=LEAF[3])
    return auto_outline(img, 0.5)


def icon_water():
    img, d = new()
    d.rectangle([8, 12, 22, 25], fill=c('#4A90D9'))
    d.rectangle([8, 12, 22, 14], fill=c('#7FB5F0'))
    d.rectangle([8, 12, 9, 25], fill=c('#7FB5F0'))
    d.polygon([(22, 16), (30, 9), (30, 12), (22, 21)], fill=c('#4A90D9'))
    d.arc([2, 8, 12, 22], 90, 270, fill=c('#2F69BA'), width=2)
    for (x, y) in ((28, 17), (26, 22), (30, 23)):
        d.point((x, y), fill=c('#7FB5F0'))
    return auto_outline(img, 0.5)


def icon_harvest():
    img, d = new()
    d.polygon([(5, 14), (27, 14), (24, 28), (8, 28)], fill=c('#C9A66B'))
    for x in (10, 15, 20):
        d.line([(x, 14), (x - 1, 28)], fill=c('#8A6A3A'))
    d.arc([6, 3, 26, 22], 190, 350, fill=c('#8A6A3A'), width=2)
    d.ellipse([11, 8, 17, 14], fill=c('#F28C28'))
    d.ellipse([16, 9, 22, 15], fill=c('#D0312D'))
    d.point([(12, 9), (17, 10)], fill=c('#FFFFFF'))
    return auto_outline(img, 0.5)


def icon_build():
    img, d = new()
    d.rectangle([14, 12, 17, 29], fill=c('#8A6A3A'))
    d.rectangle([14, 12, 14, 29], fill=c('#B08A50'))
    d.rectangle([6, 5, 26, 13], fill=c('#8C99A8'))
    d.rectangle([6, 5, 26, 7], fill=c('#B8C2CE'))
    return auto_outline(img, 0.5)


def icon_feed():
    img, d = new()
    d.polygon([(6, 14), (26, 14), (23, 28), (9, 28)], fill=c('#B5651D'))
    d.polygon([(6, 14), (10, 14), (11, 28), (9, 28)], fill=c('#D08A3A'))
    d.rectangle([6, 12, 26, 15], fill=c('#8A4A12'))
    r = rng('feed')
    for _ in range(14):
        x = r.randrange(8, 25)
        d.line([(x, 12), (x + r.randrange(-2, 3), 5 + r.randrange(0, 4))], fill=c('#E8C547'))
    return auto_outline(img, 0.5)


# ---------------------------------------------------------------- assembly
SEASONS = ['SPRING', 'SUMMER', 'AUTUMN', 'WINTER']
CROPS = ['CARROT', 'POTATO', 'TOMATO', 'CORN', 'PUMPKIN']
STAGES = ['SEED', 'SPROUT', 'GROWING', 'MATURE', 'WITHERED']
SEED_COLOURS = {'CARROT': '#F28C28', 'POTATO': '#C9A66B', 'TOMATO': '#D0312D', 'CORN': '#F2D43D', 'PUMPKIN': '#E8751A'}


def build():
    files = {}
    for season in SEASONS:
        files[f'terrain/GRASS_{season}.png'] = grass(season, 0)
        for variant in (1, 2, 3):
            files[f'terrain/GRASS_{season}_{variant}.png'] = grass(season, variant)
        files[f'terrain/TREE_{season}.png'] = oak(season)
        files[f'terrain/PINE_{season}.png'] = pine(season)
        files[f'terrain/BUSH_{season}.png'] = bush(season)
    files['terrain/SOIL.png'] = soil(False)
    files['terrain/SOIL_WATERED.png'] = soil(True)
    files['terrain/WATER.png'] = water(0)
    files['terrain/WATER_1.png'] = water(1)
    files['terrain/WATER_2.png'] = water(2)
    files['terrain/OBSTACLE.png'] = rock()
    files['terrain/PATH.png'] = path_tile()
    files['village/HOME.png'] = home()
    files['village/SHOP.png'] = shop()
    files['village/FARMHOUSE.png'] = farmhouse()
    files['village/CLINIC.png'] = clinic()
    files['village/COTTAGE.png'] = cottage()
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
            files[f'characters/AVATAR_{direction}_{frame}.png'] = person(
                direction, frame, '#C8453A', '#3A5A8A', '#5A3A1E', hat='#E8C547', overalls='#3A6FB5')
    for npc, look in NPCS.items():
        files[f'characters/{npc}.png'] = person('S', 0, **look)
    files['icons/COIN.png'] = icon_coin()
    files['icons/ENERGY.png'] = icon_energy()
    files['icons/SPRING.png'] = icon_flower()
    files['icons/SUMMER.png'] = icon_sun()
    files['icons/AUTUMN.png'] = icon_leaf()
    files['icons/WINTER.png'] = icon_flake()
    files['icons/SUNNY.png'] = icon_sun()
    files['icons/CLOUDY.png'] = icon_cloud(dark_cloud=True)
    files['icons/RAINY.png'] = icon_cloud(rain=True, dark_cloud=True)
    files['icons/DROUGHT.png'] = icon_sun(dry=True)
    files['icons/FROST.png'] = icon_flake()
    files['icons/SNOWY.png'] = icon_cloud(snow=True)
    files['icons/SOW.png'] = icon_sow()
    files['icons/WATER.png'] = icon_water()
    files['icons/HARVEST.png'] = icon_harvest()
    files['icons/BUILD.png'] = icon_build()
    files['icons/FEED.png'] = icon_feed()
    return files


def contact_sheet(files, path, scale=3):
    names = sorted(files)
    cell = 64 * scale + 8
    cols = 10
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * cell, rows * (cell + 4)), c('#62A83C'))
    for i, name in enumerate(names):
        img = files[name]
        k = 2 if img.height > 64 else 1
        big = img.resize((img.width * scale // k, img.height * scale // k), Image.NEAREST)
        x, y = (i % cols) * cell + 4, (i // cols) * (cell + 4) + 4
        sheet.alpha_composite(big, (x, y))
    sheet.save(path)


def main():
    check = '--check' in sys.argv
    stale = []
    files = build()
    if '--sheet' in sys.argv:
        contact_sheet(files, sys.argv[sys.argv.index('--sheet') + 1])
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
