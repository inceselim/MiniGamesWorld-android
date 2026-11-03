"""Vektor cizim yardimcilari: SVG yol verisi uretir, renk yuvalarini cozer, rsvg-convert ile on izleme yapar.
Renk yuvalari: main / mainD / mainL (secilen renk ve tonlari), skin / skinD, hair / hairD, ya da sabit '#RRGGBB' / '#AARRGGBB'.
"""
import subprocess, colorsys

def fmt(v):
    s = f"{v:.1f}"
    return s[:-2] if s.endswith(".0") else s

class P:
    """Mutlak koordinatli basit yol kurucu (M L Q C Z). mirror() x -> 200-x (W) ile aynalar."""
    def __init__(self, W=200):
        self.cmds = []; self.W = W
    def M(self, x, y): self.cmds.append(('M', [x, y])); return self
    def L(self, x, y): self.cmds.append(('L', [x, y])); return self
    def Q(self, x1, y1, x, y): self.cmds.append(('Q', [x1, y1, x, y])); return self
    def C(self, x1, y1, x2, y2, x, y): self.cmds.append(('C', [x1, y1, x2, y2, x, y])); return self
    def Z(self): self.cmds.append(('Z', [])); return self
    def d(self):
        return " ".join(c + " ".join(fmt(v) for v in a) for c, a in self.cmds)
    def mirror(self):
        p = P(self.W)
        for c, a in self.cmds:
            p.cmds.append((c, [self.W - v if i % 2 == 0 else v for i, v in enumerate(a)]))
        return p

def ellipse(cx, cy, rx, ry):
    k = 0.5523
    return (P().M(cx - rx, cy).C(cx - rx, cy - ry * k, cx - rx * k, cy - ry, cx, cy - ry)
            .C(cx + rx * k, cy - ry, cx + rx, cy - ry * k, cx + rx, cy)
            .C(cx + rx, cy + ry * k, cx + rx * k, cy + ry, cx, cy + ry)
            .C(cx - rx * k, cy + ry, cx - rx, cy + ry * k, cx - rx, cy).Z())

def rect(x0, y0, x1, y1):
    return P().M(x0, y0).L(x1, y0).L(x1, y1).L(x0, y1).Z()

def poly(*pts):
    p = P().M(*pts[0])
    for q in pts[1:]: p.L(*q)
    return p.Z()

def join(*paths):
    """Birden cok alt yolu tek yol verisinde birlestir."""
    out = P(paths[0].W if paths else 200); [out.cmds.extend(p.cmds) for p in paths]; return out

def hex2rgb(h):
    h = h.lstrip('#')
    if len(h) == 8: h = h[2:]
    return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))

def rgb2hex(c): return "#%02X%02X%02X" % tuple(max(0, min(255, int(round(v)))) for v in c)

def shade(h, f):
    """f<1 koyulastir, f>1 acar (beyaza dogru)."""
    r, g, b = hex2rgb(h)
    if f <= 1: return rgb2hex((r * f, g * f, b * f))
    t = f - 1
    return rgb2hex((r + (255 - r) * t, g + (255 - g) * t, b + (255 - b) * t))

def resolve(slot, colors):
    if slot.startswith('#'): return ('#' + slot[3:]) if len(slot) == 9 else slot, (int(slot[1:3], 16) / 255 if len(slot) == 9 else 1.0)
    if slot in ('eye', 'lips'): return colors.get(slot, {'eye': '#5B8DD6', 'lips': '#D9536F'}[slot]), 1.0
    base = {'main': colors.get('main', '#E84A8A'), 'skin': colors.get('skin', '#F6D2B8'), 'hair': colors.get('hair', '#6B3E26')}
    for k in base:
        if slot == k: return base[k], 1.0
        if slot == k + 'D': return shade(base[k], 0.78), 1.0
        if slot == k + 'L': return shade(base[k], 1.35), 1.0
    raise ValueError(slot)

def svg(parts, colors, W=200, H=400, bg=None, scale=2):
    """parts: [(d, slot, stroke)]"""
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W*scale}" height="{H*scale}" viewBox="0 0 {W} {H}">']
    if bg: out.append(f'<rect width="{W}" height="{H}" fill="{bg}"/>')
    for d, slot, stroke in parts:
        c, a = resolve(slot, colors)
        st = f' stroke="{shade(c, 0.55)}" stroke-width="1.4" stroke-linejoin="round" stroke-opacity="0.85"' if stroke else ''
        out.append(f'<path d="{d}" fill="{c}" fill-opacity="{a:.2f}"{st}/>')
    out.append('</svg>')
    return "\n".join(out)

def render(svgtext, png):
    subprocess.run(["rsvg-convert", "-o", png], input=svgtext.encode(), check=True)

def kt_parts(parts, indent="        "):
    """[(d, slot, stroke)] -> Kotlin listOf(VP(...))"""
    if not parts: return "emptyList()"
    rows = [f'{indent}    VP("{d}", "{slot}"{", false" if not st else ""}),' for d, slot, st in parts]
    return "listOf(\n" + "\n".join(rows) + f"\n{indent})"
