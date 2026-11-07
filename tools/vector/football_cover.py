"""Kulup Patronu kapagi (960x540): gece stadyumu, perspektif saha, taktik oklari, kupa, top."""
import os, sys, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from vec import render
from PIL import Image
W, H = 960, 540
o = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W*2}" height="{H*2}" viewBox="0 0 {W} {H}">',
 '<defs>',
 '<linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#0B1430"/><stop offset="1" stop-color="#1D3A6B"/></linearGradient>',
 '<linearGradient id="grass" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2F8F46"/><stop offset="1" stop-color="#1B5E2D"/></linearGradient>',
 '<radialGradient id="glow" cx="0.5" cy="0.5" r="0.5"><stop offset="0" stop-color="#FFF7C2" stop-opacity="0.9"/><stop offset="1" stop-color="#FFF7C2" stop-opacity="0"/></radialGradient>',
 '<linearGradient id="gold" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FFE27A"/><stop offset="0.5" stop-color="#F5B82E"/><stop offset="1" stop-color="#B9791A"/></linearGradient>',
 '<radialGradient id="ball" cx="0.35" cy="0.3" r="0.8"><stop offset="0" stop-color="#FFFFFF"/><stop offset="1" stop-color="#C9CED6"/></radialGradient>',
 '</defs>',
 f'<rect width="{W}" height="{H}" fill="url(#sky)"/>']
# tribunler
o.append('<path d="M0 150 L960 150 L960 250 L0 250 Z" fill="#14213F"/>')
for r in range(6):
    y = 160 + r * 15
    for i in range(0, 960, 9):
        c = ['#E04848', '#F7D046', '#FFFFFF', '#38BDF8'][(i * 7 + r * 13) % 4]
        o.append(f'<circle cx="{i + (r % 2) * 4}" cy="{y}" r="2.6" fill="{c}" opacity="0.55"/>')
# isik kuleleri
for x in (90, 870):
    o.append(f'<rect x="{x-3}" y="40" width="6" height="120" fill="#2B3A5E"/>')
    o.append(f'<rect x="{x-28}" y="28" width="56" height="20" rx="3" fill="#E8EEF8"/>')
    o.append(f'<ellipse cx="{x}" cy="40" rx="120" ry="70" fill="url(#glow)" opacity="0.55"/>')
# saha (perspektif)
o.append('<path d="M140 250 L820 250 L960 540 L0 540 Z" fill="url(#grass)"/>')
for k in range(8):
    t0 = k / 8; t1 = (k + 0.5) / 8
    def X(t, side): return (140 - 140 * t) if side == 0 else (820 + 140 * t)
    y0 = 250 + 290 * t0; y1 = 250 + 290 * t1
    o.append(f'<path d="M{X(t0,0)} {y0} L{X(t0,1)} {y0} L{X(t1,1)} {y1} L{X(t1,0)} {y1} Z" fill="#000" opacity="0.06"/>')
L = 'stroke="#E9F5EC" stroke-width="3" fill="none" opacity="0.85"'
o.append(f'<path d="M160 262 L800 262 L925 528 L35 528 Z" {L}/>')
o.append(f'<path d="M480 262 L480 528" {L}/>')
o.append(f'<ellipse cx="480" cy="385" rx="110" ry="42" {L}/>')
o.append(f'<path d="M95 330 L250 330 L215 470 L40 470" {L}/>')
o.append(f'<path d="M865 330 L710 330 L745 470 L920 470" {L}/>')
# taktik oklari ve oyuncular
def player(x, y, c):
    o.append(f'<ellipse cx="{x}" cy="{y+14}" rx="13" ry="5" fill="#000" opacity="0.3"/>')
    o.append(f'<circle cx="{x}" cy="{y}" r="12" fill="{c}" stroke="#FFFFFF" stroke-width="3"/>')
for (x, y) in [(330, 330), (300, 420), (420, 300), (440, 450), (560, 360)]: player(x, y, '#E04848')
for (x, y) in [(640, 320), (690, 420), (600, 470)]: player(x, y, '#38BDF8')
arrow = 'stroke="#F7D046" stroke-width="5" fill="none" stroke-linecap="round" stroke-dasharray="12 9"'
o.append(f'<path d="M330 330 Q420 250 545 350" {arrow}/>')
o.append(f'<path d="M440 450 Q560 480 655 405" {arrow}/>')
o.append('<path d="M545 350 l-18 -2 l8 -14 Z" fill="#F7D046"/>')
o.append('<path d="M655 405 l-6 -16 l-12 10 Z" fill="#F7D046"/>')
# top
o.append('<circle cx="520" cy="470" r="22" fill="url(#ball)" stroke="#333" stroke-width="2"/>')
o.append('<path d="M520 458 l9 7 l-3 11 l-12 0 l-3 -11 Z" fill="#222"/>')
# kupa (sag)
cx, cy = 820, 150
o.append(f'<ellipse cx="{cx}" cy="{cy+175}" rx="80" ry="16" fill="#000" opacity="0.35"/>')
o.append(f'<path d="M{cx-60} {cy} Q{cx-60} {cy+95} {cx} {cy+110} Q{cx+60} {cy+95} {cx+60} {cy} Z" fill="url(#gold)" stroke="#8A5A10" stroke-width="3"/>')
o.append(f'<path d="M{cx-60} {cy+15} C{cx-115} {cy+10} {cx-105} {cy+80} {cx-40} {cy+85}" stroke="url(#gold)" stroke-width="12" fill="none"/>')
o.append(f'<path d="M{cx+60} {cy+15} C{cx+115} {cy+10} {cx+105} {cy+80} {cx+40} {cy+85}" stroke="url(#gold)" stroke-width="12" fill="none"/>')
o.append(f'<rect x="{cx-14}" y="{cy+108}" width="28" height="34" fill="url(#gold)"/>')
o.append(f'<rect x="{cx-48}" y="{cy+140}" width="96" height="30" rx="5" fill="#3B2A12" stroke="#F5B82E" stroke-width="3"/>')
o.append(f'<path d="M{cx-35} {cy+18} Q{cx-30} {cy+80} {cx-5} {cy+95}" stroke="#FFF6CF" stroke-width="7" fill="none" opacity="0.6"/>')
# arma (sol)
ax, ay = 140, 140
o.append(f'<path d="M{ax-62} {ay-70} L{ax+62} {ay-70} L{ax+62} {ay+10} Q{ax+62} {ay+70} {ax} {ay+95} Q{ax-62} {ay+70} {ax-62} {ay+10} Z" fill="#E04848" stroke="#FFFFFF" stroke-width="6"/>')
o.append(f'<path d="M{ax-62} {ay-20} L{ax+62} {ay-20} L{ax+62} {ay+15} L{ax-62} {ay+15} Z" fill="#FFFFFF" opacity="0.9"/>')
for i in range(5):
    a = -math.pi / 2 + i * 2 * math.pi / 5
    pts = []
    for k in range(10):
        b = a + k * math.pi / 5; r = 18 if k % 2 == 0 else 8
        pts.append(f'{ax + r * math.cos(b - a - math.pi/2):.1f},{ay - 45 + r * math.sin(b - a - math.pi/2):.1f}')
o.append(f'<polygon points="{" ".join(pts)}" fill="#F7D046"/>')
o.append('</svg>')
os.makedirs('/tmp/fbc', exist_ok=True)
render("\n".join(o), '/tmp/fbc/cover.png')
im = Image.open('/tmp/fbc/cover.png').convert('RGB').resize((W, H), Image.LANCZOS)
im.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../app/src/main/res/drawable-nodpi/game_football_manager.webp'), 'WEBP', quality=90)
im.save('/tmp/fbc/game_football_manager.png')
