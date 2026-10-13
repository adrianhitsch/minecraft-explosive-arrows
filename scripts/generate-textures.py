"""Generates the CBU-87 and BLU-97 entity textures (no dependencies besides the standard library).

The rows/columns must match the meshes in src/client/java/explosivearrows/client/CbuModels.java:
- cbu87.png (128x128): body wrapped around in columns 0..64 (column 16 = top, rows = profile from the
  fuze cap (0) to the rear cover (128)), fins in columns 64..128 / rows 0..64, suspension lugs at 64..80 / 64..80.
- blu97.png (32x32): bomblet body in columns 0..16, ballute in columns 16..32.
- cbu87_launcher.png (128x128): tube in columns 0..64 (rows: muzzle, outer tube, rear flare, inner wall),
  accessories in rows 0..16 of columns 64..128.

Usage: python scripts/generate-textures.py
"""
import math
import os
import random
import struct
import zlib

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "explosivearrows",
                   "textures", "entity")

OLIVE = (74, 80, 52)
OLIVE_DARK = (58, 63, 41)
SEAM = (40, 43, 30)
YELLOW = (214, 168, 34)
FUZE = (50, 52, 54)
METAL = (66, 68, 70)
PLACARD = (196, 196, 184)
STENCIL = (28, 28, 26)

# Body rows of the CBU-87 (see CbuModels): 49.3 rows per metre on the cylinder
ROWS_PER_M = 73.0 / 1.48


def body_row(d):
    """Texture row of the cylinder at distance d (m) behind the nose tip."""
    return round(22 + (d - 0.32) * ROWS_PER_M)


class Image:
    def __init__(self, width, height, seed):
        self.width = width
        self.height = height
        self.pixels = [[(0, 0, 0, 255)] * width for _ in range(height)]
        self.random = random.Random(seed)

    def fill(self, x0, y0, x1, y1, color, noise=0):
        for y in range(max(0, y0), min(self.height, y1)):
            for x in range(max(0, x0), min(self.width, x1)):
                n = self.random.randint(-noise, noise) if noise else 0
                self.pixels[y][x] = tuple(max(0, min(255, c + n)) for c in color) + (255,)

    def save(self, name):
        raw = b"".join(b"\x00" + b"".join(struct.pack("4B", *p) for p in row) for row in self.pixels)

        def chunk(kind, data):
            return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

        png = (b"\x89PNG\r\n\x1a\n"
               + chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0))
               + chunk(b"IDAT", zlib.compress(raw, 9))
               + chunk(b"IEND", b""))
        os.makedirs(OUT, exist_ok=True)
        with open(os.path.join(OUT, name), "wb") as f:
            f.write(png)


def cbu87():
    img = Image(128, 128, 87)
    # --- Body (columns 0..64) ---
    img.fill(0, 0, 64, 128, OLIVE, noise=4)
    # Fuze cap with front ring
    img.fill(0, 0, 64, 7, FUZE, noise=3)
    img.fill(0, 2, 64, 3, (36, 37, 38))
    img.fill(0, 6, 64, 7, (70, 72, 74))
    img.fill(0, 7, 64, 8, OLIVE_DARK)
    # Joint between nose and dispenser
    img.fill(0, 22, 64, 23, SEAM)
    # Yellow band (high explosive)
    band = body_row(0.55)
    img.fill(0, band, 64, band + 6, YELLOW, noise=6)
    # Longitudinal split lines on the sides and the rail on top (column 16)
    for col in (0, 32, 63):
        img.fill(col, 23, col + 1, 95, SEAM)
    img.fill(15, 26, 16, 92, SEAM)
    img.fill(16, 26, 17, 92, (98, 104, 72))
    img.fill(17, 26, 18, 92, SEAM)
    # Circumferential joints
    for row in (body_row(1.10), 95, 111):
        img.fill(0, row, 64, row + 1, SEAM)
    # Lug base plates
    for d in (0.95, 1.31):
        row = body_row(d)
        img.fill(13, row - 2, 20, row + 3, METAL, noise=2)
    # Small white stickers along the rail (both sides) and two placards with "text"
    for d in (0.45, 0.75, 1.15, 1.55):
        row = body_row(d)
        for col in (20, 10):
            img.fill(col, row, col + 4, row + 3, PLACARD, noise=4)
            img.fill(col + 1, row + 1, col + 3, row + 2, (120, 120, 112))
    for d, col in ((1.00, 22), (1.20, 23)):
        row = body_row(d)
        img.fill(col, row, col + 7, row + 6, PLACARD, noise=4)
        for line in range(row + 1, row + 5, 2):
            img.fill(col + 1, line, col + 6, line + 1, (90, 90, 86))
    # Stencilled lettering on the side
    for i, row in enumerate(range(body_row(1.25), body_row(1.25) + 12, 3)):
        img.fill(36, row, 36 + 10 - (i * 3) % 5, row + 1, STENCIL)
    # Boat tail and tail section slightly darker, rear cover dark
    img.fill(0, 96, 64, 111, (68, 73, 48), noise=4)
    img.fill(0, 112, 64, 122, OLIVE_DARK, noise=3)
    img.fill(0, 122, 64, 128, FUZE, noise=3)
    img.fill(0, 125, 64, 126, (36, 37, 38))

    # --- Fins (columns 64..128, rows 0..64): plate with darker frame and a rivet line at the root ---
    img.fill(64, 0, 128, 64, (80, 86, 56), noise=4)
    img.fill(64, 0, 128, 2, SEAM)
    img.fill(64, 62, 128, 64, SEAM)
    img.fill(64, 0, 66, 64, SEAM)
    img.fill(126, 0, 128, 64, SEAM)
    for y in range(6, 60, 6):
        img.fill(69, y, 70, y + 1, (44, 47, 32))

    # --- Suspension lugs ---
    img.fill(64, 64, 80, 80, METAL, noise=4)
    img.save("cbu87.png")


def blu97():
    img = Image(32, 32, 97)
    # Body: probe, front, yellow body with stencil, rear cover
    img.fill(0, 0, 16, 1, (60, 60, 60))
    img.fill(0, 1, 16, 4, (150, 150, 146), noise=6)
    img.fill(0, 4, 16, 8, (72, 72, 68), noise=3)
    img.fill(0, 8, 16, 29, (226, 182, 36), noise=6)
    img.fill(0, 11, 16, 12, (120, 96, 30))
    img.fill(2, 17, 7, 18, STENCIL)
    img.fill(2, 19, 5, 20, STENCIL)
    img.fill(10, 17, 14, 18, STENCIL)
    img.fill(0, 29, 16, 32, (110, 110, 106), noise=4)
    # Ballute: khaki fabric with seams
    img.fill(16, 0, 32, 32, (184, 160, 108), noise=5)
    for col in range(16, 32, 4):
        img.fill(col, 0, col + 1, 32, (150, 128, 84))
    img.fill(16, 19, 32, 21, (130, 110, 72))
    img.save("blu97.png")


def launcher():
    img = Image(128, 128, 1987)
    # Tube (columns 0..64, column 16 = top): muzzle, body, rear venturi, inner wall
    img.fill(0, 0, 64, 2, (34, 35, 34))
    img.fill(0, 2, 64, 7, (48, 50, 48), noise=3)
    img.fill(0, 7, 64, 100, (84, 90, 58), noise=4)
    for row in (12, 52, 92):
        img.fill(0, row, 64, row + 2, OLIVE_DARK)
    # Rail with rivets on top, stencilled lettering on the side
    img.fill(15, 14, 18, 90, (70, 75, 50))
    for row in range(18, 90, 8):
        img.fill(16, row, 17, row + 1, SEAM)
    for i, row in enumerate(range(28, 40, 3)):
        img.fill(36, row, 36 + 12 - (i * 4) % 6, row + 1, STENCIL)
    # Yellow ammunition band near the muzzle, red/white backblast warning near the rear
    img.fill(0, 8, 64, 10, YELLOW, noise=5)
    for col in range(4, 28):
        img.fill(col, 80, col + 1, 88, (190, 30, 28) if (col // 3) % 2 == 0 else (230, 228, 220))
    img.fill(0, 100, 64, 116, (46, 48, 46), noise=3)
    img.fill(0, 116, 64, 117, (34, 35, 34))
    img.fill(0, 117, 64, 128, (24, 24, 24), noise=2)
    # Accessories: grips, shoulder pad, scope (rows along its profile: objective lens, body, eyepiece), metal
    img.fill(64, 0, 80, 16, (32, 32, 32), noise=3)
    img.fill(80, 0, 96, 16, (44, 40, 36), noise=4)
    img.fill(96, 0, 112, 2, (60, 110, 140), noise=6)
    img.fill(96, 2, 112, 14, (52, 54, 56), noise=3)
    img.fill(96, 8, 112, 9, (36, 37, 38))
    img.fill(96, 14, 112, 16, (20, 26, 30))
    img.fill(112, 0, 128, 16, (96, 98, 100), noise=5)
    img.save("cbu87_launcher.png")


# Ballistics of a launched bomb, must match ClusterBombEntity / ClusterBombLauncherItem
LAUNCH_SPEED = 3.0
MOTOR_TICKS = 20
GRAVITY = 0.06
DRAG = 0.985
# Scope (LauncherScope): FOV 70 * 0.25, overlay drawn at 1.125x the screen height like the spyglass
SCOPE_HALF_FOV = math.radians(70 * 0.25 / 2)
OVERLAY_SCALE = 1.125
HOLD_OVER_DISTANCES = (80, 100, 120)


def drop_at(distance):
    """Height (negative = below the line of sight) of a bomb fired horizontally when it has flown distance blocks."""
    x = y = 0.0
    vx, vy = LAUNCH_SPEED, 0.0
    tick = 0
    while x < distance:
        tick += 1
        motor = tick < MOTOR_TICKS
        if not motor:
            vy -= GRAVITY
        px, py = x, y
        x += vx
        y += vy
        if not motor:
            vx *= DRAG
            vy *= DRAG
    t = (distance - px) / (x - px)
    return py + (y - py) * t


DIGITS = {
    "0": ["111", "101", "101", "101", "111"], "1": ["010", "110", "010", "010", "111"],
    "2": ["111", "001", "111", "100", "111"], "8": ["111", "101", "111", "101", "111"],
}


def text(img, x, y, string, color, size=2):
    for ch in string:
        for row, bits in enumerate(DIGITS[ch]):
            for col, bit in enumerate(bits):
                if bit == "1":
                    img.fill(x + col * size, y + row * size, x + (col + 1) * size, y + (row + 1) * size, color)
        x += 4 * size


def scope():
    """Reticle of the launcher scope (256x256): transparent inside the circle, black outside."""
    img = Image(256, 256, 7)
    size, center, radius = 256, 128, 118
    black = (0, 0, 0)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x + 0.5 - center, y + 0.5 - center)
            if d >= radius:
                img.pixels[y][x] = (0, 0, 0, 255)
            elif d >= radius - 10:
                # Dark ring inside the tube
                alpha = int(255 * ((d - (radius - 10)) / 10) ** 1.5)
                img.pixels[y][x] = (0, 0, 0, alpha)
            else:
                img.pixels[y][x] = (0, 0, 0, 0)
    # Thin cross with a gap in the middle, thick posts towards the edge
    img.fill(center - 1, center - 100, center + 1, center - 8, black)
    img.fill(center - 1, center + 8, center + 1, center + 112, black)
    img.fill(center - 100, center - 1, center - 8, center + 1, black)
    img.fill(center + 8, center - 1, center + 100, center + 1, black)
    img.fill(center - 3, center - 118, center + 3, center - 70, black)
    img.fill(center - 118, center - 3, center - 70, center + 3, black)
    img.fill(center + 70, center - 3, center + 118, center + 3, black)
    for i in range(-60, 61, 12):
        if i:
            img.fill(center + i - 1, center - 4, center + i + 1, center + 4, black)
    # Red aiming point for straight flight (up to ~60 blocks under rocket power)
    img.fill(center - 2, center - 2, center + 2, center + 2, (220, 30, 20))
    # Hold-over marks for longer distances
    pixels_per_tan = size / 2 / OVERLAY_SCALE / math.tan(SCOPE_HALF_FOV)
    for i, distance in enumerate(HOLD_OVER_DISTANCES):
        offset = round(-drop_at(distance) / distance * pixels_per_tan)
        if offset >= radius - 20:
            continue
        half = 16 - i * 4
        img.fill(center - half, center + offset - 1, center + half, center + offset + 1, black)
        text(img, center + half + 4, center + offset - 5, str(distance), black)
    os.makedirs(os.path.join(OUT, "..", "misc"), exist_ok=True)
    img.save(os.path.join("..", "misc", "cbu87_scope.png"))


if __name__ == "__main__":
    cbu87()
    blu97()
    launcher()
    scope()
    for distance in HOLD_OVER_DISTANCES:
        print(f"drop at {distance} blocks: {-drop_at(distance):.1f}")
