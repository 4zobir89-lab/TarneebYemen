#!/usr/bin/env python3
"""Generate realistic-looking Tarneeb playing card PNG images using Pillow."""

from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os, math

OUT_DIR = "/root/project_yemen/app/src/main/res/drawable"
CARD_W, CARD_H = 252, 364
CORNER_RADIUS = 24
BORDER_COLOR = "#FFD700"
SUIT_COLORS = {"hearts": "#D32F2F", "diamonds": "#D32F2F", "clubs": "#1A1A1A", "spades": "#1A1A1A"}
RANK_LABELS = {1: "A", 2: "2", 3: "3", 4: "4", 5: "5", 6: "6", 7: "7", 8: "8", 9: "9", 10: "10"}
SUIT_SYMBOLS = {"hearts": "\u2665", "diamonds": "\u2666", "clubs": "\u2663", "spades": "\u2660"}
FONT_DIR = "/usr/share/fonts/truetype"

def load_font(size, bold=True):
    serif_name = "DejaVuSerif-Bold.ttf" if bold else "DejaVuSerif.ttf"
    path = os.path.join(FONT_DIR, "dejavu", serif_name)
    if os.path.exists(path):
        return ImageFont.truetype(path, size)
    try:
        return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf", size)
    except OSError:
        return ImageFont.load_default()

def rounded_rect(draw, xy, radius, fill=None, outline=None, width=1):
    x1, y1, x2, y2 = xy
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)

def make_gradient(w, h, color_center, color_edge):
    img = Image.new("RGBA", (w, h))
    center_x, center_y = w // 2, h // 2
    max_d = math.sqrt(center_x ** 2 + center_y ** 2)
    def lerp_c(c1, c2, t):
        return tuple(int(a + (b - a) * t) for a, b in zip(c1, c2))
    for y in range(h):
        for x in range(w):
            d = math.sqrt((x - center_x) ** 2 + (y - center_y) ** 2)
            t = min(d / max_d, 1.0)
            px = lerp_c(color_center, color_edge, t)
            img.putpixel((x, y), px)
    return img

def hex_to_rgba(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i+2], 16) for i in (0, 2, 4)) + (255,)

def apply_rounded_corners(img, radius):
    mask = Image.new("L", img.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, img.width - 1, img.height - 1), radius=radius, fill=255)
    result = img.copy()
    result.putalpha(mask)
    return result

def draw_card_front(suit, rank):
    img = Image.new("RGBA", (CARD_W, CARD_H))
    bg = make_gradient(CARD_W, CARD_H, hex_to_rgba("#FFFEFEFE")[:3], hex_to_rgba("#FFF5E6")[:3])
    img.paste(bg, (0, 0))

    draw = ImageDraw.Draw(img)
    rounded_rect(draw, (2, 2, CARD_W - 3, CARD_H - 3), CORNER_RADIUS, outline="#C5A000", width=1)
    rounded_rect(draw, (4, 4, CARD_W - 5, CARD_H - 5), CORNER_RADIUS - 1, outline=BORDER_COLOR, width=2)

    color = SUIT_COLORS[suit]
    suit_sym = SUIT_SYMBOLS[suit]
    rank_label = RANK_LABELS[rank]

    font_rank = load_font(32)
    font_suit = load_font(22)
    font_suit_large = load_font(80)
    font_dec = load_font(14)

    def draw_corner(x, y, angle, label_anchor="la", sym_anchor="lt"):
        lx = x
        ly = y + 2
        if angle != 0:
            lx = x
            ly = y + 28
        draw.text((lx, ly), rank_label, fill=color, font=font_rank, anchor=label_anchor)
        sx = x
        sy = y + 32
        if angle != 0:
            sx = x
            sy = y + 4
        draw.text((sx, sy), suit_sym, fill=color, font=font_suit, anchor=sym_anchor)

    draw_corner(24, 20, 0, "la", "lt")
    draw_corner(CARD_W - 24, CARD_H - 20, 180, "ra", "rb")

    cx, cy = CARD_W // 2, CARD_H // 2

    dec_layer = Image.new("RGBA", img.size, (0,0,0,0))
    ddraw = ImageDraw.Draw(dec_layer)
    for i in range(4):
        angle = math.radians(i * 90 + 45)
        r = 60
        ox = cx + int(math.cos(angle) * r)
        oy = cy + int(math.sin(angle) * r)
        ddraw.text((ox, oy), suit_sym, fill="#D4AF37", font=font_dec, anchor="mm")
    for r in (50, 70, 90):
        ddraw.ellipse([cx - r, cy - r, cx + r, cy + r], outline="#D4AF37", width=1)
    ddraw.ellipse([cx - 100, cy - 70, cx + 100, cy + 70], outline="#D4AF37", width=1)
    img = Image.alpha_composite(img, dec_layer)

    draw = ImageDraw.Draw(img)
    label_y = cy - 10
    if rank_label == "10":
        label_y = cy - 12
    draw.text((cx, label_y - 2), rank_label, fill=color, font=font_rank, anchor="mm")

    if suit == "diamonds":
        draw.text((cx, cy + 38), suit_sym, fill=color, font=font_suit_large, anchor="mm")
    elif suit == "hearts":
        draw.text((cx, cy + 36), suit_sym, fill=color, font=font_suit_large, anchor="mm")
    elif suit == "clubs":
        draw.text((cx, cy + 38), suit_sym, fill=color, font=font_suit_large, anchor="mm")
    else:
        draw.text((cx, cy + 38), suit_sym, fill=color, font=font_suit_large, anchor="mm")

    img = apply_rounded_corners(img, CORNER_RADIUS)
    return img.convert("RGB")

def draw_card_back():
    img = Image.new("RGBA", (CARD_W, CARD_H))
    bg = make_gradient(CARD_W, CARD_H, (139, 0, 0), (80, 0, 0))
    img.paste(bg, (0, 0))

    draw = ImageDraw.Draw(img)
    rounded_rect(draw, (2, 2, CARD_W - 3, CARD_H - 3), CORNER_RADIUS, outline="#C5A000", width=1)
    rounded_rect(draw, (4, 4, CARD_W - 5, CARD_H - 5), CORNER_RADIUS - 1, outline=BORDER_COLOR, width=2)

    cx, cy = CARD_W // 2, CARD_H // 2
    gold = "#FFD700"

    outer_r = 90
    inner_r = 50
    draw.ellipse([cx - outer_r, cy - outer_r, cx + outer_r, cy + outer_r], outline=gold, width=3)
    draw.ellipse([cx - inner_r, cy - inner_r, cx + inner_r, cy + inner_r], outline=gold, width=2)

    for i in range(12):
        angle = math.radians(i * 30)
        x1 = cx + int(math.cos(angle) * inner_r)
        y1 = cy + int(math.sin(angle) * inner_r)
        x2 = cx + int(math.cos(angle) * outer_r)
        y2 = cy + int(math.sin(angle) * outer_r)
        draw.line([(x1, y1), (x2, y2)], fill=gold, width=2)

    inner2 = 65
    for i in range(4):
        angle = math.radians(i * 90 + 45)
        x1 = cx + int(math.cos(angle) * inner_r)
        y1 = cy + int(math.sin(angle) * inner_r)
        x2 = cx + int(math.cos(angle) * inner2)
        y2 = cy + int(math.sin(angle) * inner2)
        draw.line([(x1, y1), (x2, y2)], fill=gold, width=3)

    font_diamond = load_font(38)
    draw.text((cx, cy), "\u2666", fill=gold, font=font_diamond, anchor="mm")

    dec_r = 110
    for i in range(8):
        angle = math.radians(i * 45)
        x = cx + int(math.cos(angle) * dec_r)
        y = cy + int(math.sin(angle) * dec_r)
        s = 4 if i % 2 == 0 else 3
        draw.ellipse([x - s, y - s, x + s, y + s], fill=gold)

    img = apply_rounded_corners(img, CORNER_RADIUS)
    return img.convert("RGB")

def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    suits = ["hearts", "diamonds", "clubs", "spades"]
    ranks = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

    file_sizes = {}
    total = 0

    for suit in suits:
        for rank in ranks:
            fname = f"card_{suit}_{RANK_LABELS[rank].lower()}.png"
            path = os.path.join(OUT_DIR, fname)
            img = draw_card_front(suit, rank)
            img.save(path, "PNG")
            sz = os.path.getsize(path)
            file_sizes[fname] = sz
            total += 1
            print(f"  Created {fname}  ({sz / 1024:.1f} KB)")

    img = draw_card_back()
    fname = "card_back.png"
    path = os.path.join(OUT_DIR, fname)
    img.save(path, "PNG")
    sz = os.path.getsize(path)
    file_sizes[fname] = sz
    total += 1
    print(f"  Created {fname}  ({sz / 1024:.1f} KB)")

    print(f"\n{'=' * 50}")
    print(f"Total cards generated: {total}")
    print(f"Total size: {sum(file_sizes.values()) / 1024:.1f} KB")
    print(f"Output directory: {OUT_DIR}")

    lines = "card_attributes = {\n"
    for suit in suits:
        for rank in ranks:
            key = f"{suit}_{RANK_LABELS[rank].lower()}"
            fname = f"card_{key}.png"
            lines += f'    "{key}": "{fname}",\n'
    lines += f'    "back": "card_back.png",\n'
    lines += "}\n"
    with open(os.path.join(OUT_DIR, "card_attributes.py"), "w") as f:
        f.write(lines)
    print(f"Created card_attributes.py")

if __name__ == "__main__":
    main()
