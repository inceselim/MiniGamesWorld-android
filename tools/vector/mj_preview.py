"""On izleme: tema tas govdesi + yuzler."""
from mjlib import *
from vec import resolve, render

THEMES = {
 'classic': dict(face='#FBF5E6', face2='#EFE3C6', back='#2F8F5B', edge='#D9C9A3'),
 'jade':    dict(face='#F1FAF2', face2='#D8EEDC', back='#13785A', edge='#B9D9C0'),
 'ocean':   dict(face='#F2F8FC', face2='#D9E9F4', back='#1D5FA0', edge='#B7CFE2'),
 'forest':  dict(face='#F8EBD2', face2='#E9D2A8', back='#8A5A2E', edge='#CFAF7C'),
 'fruit':   dict(face='#FFF3F5', face2='#FBDDE4', back='#E0567F', edge='#EDB8C6'),
 'night':   dict(face='#F4F1FF', face2='#DCD6F5', back='#2C2F7A', edge='#E3B341'),
}

def tile(face, th, x, y, s=1.0, gid=0, free=True):
    o = [f'<g transform="translate({x},{y}) scale({s})">']
    o.append(f'<defs><linearGradient id="g{gid}" x1="0" y1="0" x2="0.3" y2="1"><stop offset="0" stop-color="{th["face"]}"/><stop offset="1" stop-color="{th["face2"]}"/></linearGradient></defs>')
    o.append(f'<path d="{rrect(-8, 9, 92, 139, 13).d()}" fill="#000" fill-opacity="0.22" transform="translate(-3,4)"/>')
    o.append(f'<path d="{rrect(-8, 9, 92, 139, 13).d()}" fill="{th["back"]}"/>')
    o.append(f'<path d="{rrect(-4.5, 4.5, 95.5, 134.5, 13).d()}" fill="{th["edge"]}"/>')
    o.append(f'<path d="{rrect(0, 0, 100, 130, 12).d()}" fill="url(#g{gid})"/>')
    o.append(f'<path d="{rrect(1.5, 1.5, 98.5, 128.5, 11).d()}" fill="none" stroke="#FFFFFF" stroke-opacity="0.8" stroke-width="2"/>')
    for d, slot, stroke in face:
        c, a = resolve(slot, {})
        st = f' stroke="{shade(c, 0.55)}" stroke-width="1.2" stroke-linejoin="round" stroke-opacity="0.8"' if stroke else ''
        o.append(f'<path d="{d}" fill="{c}" fill-opacity="{a:.2f}"{st}/>')
    if not free: o.append(f'<path d="{rrect(0, 0, 100, 130, 12).d()}" fill="#000" fill-opacity="0.28"/>')
    o.append('</g>')
    return "\n".join(o)

def sheet(face_list, th, cols, out, s=0.9, title=None, bg='#2E4A3B'):
    W = cols * 112 * s + 40
    rows = (len(face_list) + cols - 1) // cols
    H = rows * 150 * s + 40
    o = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W*2:.0f}" height="{H*2:.0f}" viewBox="0 0 {W:.0f} {H:.0f}"><rect width="100%" height="100%" fill="{bg}"/>']
    for i, f in enumerate(face_list):
        o.append(tile(f, th, 30 + (i % cols) * 112 * s, 18 + (i // cols) * 150 * s, s, i))
    o.append('</svg>')
    render("\n".join(o), out)
