"""Oyun muzikleri: basit chiptune sentezleyici (telifsiz, tamamen burada uretilir).
Cikti: app/src/main/res/raw/music_*.ogg (dongu icin tam olcu uzunlugunda)."""
import os, subprocess, random
import numpy as np
here = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(here, "../../app/src/main/res/raw")
SR = 32000

NOTE = {'C': 0, 'C#': 1, 'Db': 1, 'D': 2, 'D#': 3, 'Eb': 3, 'E': 4, 'F': 5, 'F#': 6, 'Gb': 6, 'G': 7, 'G#': 8, 'Ab': 8, 'A': 9, 'A#': 10, 'Bb': 10, 'B': 11}
def midi(n):
    if n is None: return None
    name, octv = n[:-1], int(n[-1])
    return 12 * (octv + 1) + NOTE[name]
def freq(m): return 440.0 * 2 ** ((m - 69) / 12)

def env(n, a=0.005, d=0.08, s=0.6, r=0.05):
    t = np.arange(n) / SR
    e = np.ones(n) * s
    ai = int(a * SR); di = int(d * SR); ri = int(r * SR)
    if ai: e[:ai] = np.linspace(0, 1, ai)
    if di: e[ai:ai + di] = np.linspace(1, s, len(e[ai:ai + di]))
    if ri and n > ri: e[-ri:] *= np.linspace(1, 0, ri)
    return e

def pulse(f, n, duty=0.5):
    ph = (np.arange(n) * f / SR) % 1.0
    return np.where(ph < duty, 1.0, -1.0)

def tri(f, n):
    ph = (np.arange(n) * f / SR) % 1.0
    return 4 * np.abs(ph - 0.5) - 1

def noise(n, seed=0):
    return np.random.default_rng(seed).uniform(-1, 1, n)

class Song:
    def __init__(self, bpm, bars, beats=4):
        self.spb = 60 / bpm
        self.n = int(round(bars * beats * self.spb * SR))
        self.buf = np.zeros(self.n)
    def at(self, beat): return int(round(beat * self.spb * SR))
    def add(self, start, sig, vol):
        i = self.at(start)
        j = min(self.n, i + len(sig))
        if i < self.n: self.buf[i:j] += sig[:j - i] * vol
    def check(self, notes, start):
        total = start + sum(d for _, d in notes)
        assert total <= self.n / SR / self.spb + 0.01, (total, self.n / SR / self.spb)
    def voice(self, notes, wave='pulse', vol=0.2, duty=0.5, start=0.0, legato=0.9, envp=None, vib=0.0):
        self.check(notes, start)
        b = start
        for n, dur in notes:
            if n is not None:
                m = midi(n); f = freq(m)
                ln = int(dur * self.spb * SR * legato)
                if wave == 'pulse':
                    if vib:
                        t = np.arange(ln) / SR
                        ph = np.cumsum(f * (1 + vib * np.sin(2 * np.pi * 5.5 * t) * np.clip(t * 4 - 0.4, 0, 1))) / SR
                        s = np.where(ph % 1 < duty, 1.0, -1.0)
                    else:
                        s = pulse(f, ln, duty)
                elif wave == 'tri':
                    s = tri(f, ln)
                else:
                    s = np.sin(2 * np.pi * f * np.arange(ln) / SR)
                s = s * env(ln, **(envp or {}))
                self.add(b, s, vol)
            b += dur
    def kick(self, beat, vol=0.6):
        ln = int(0.18 * SR); t = np.arange(ln) / SR
        f = 120 * np.exp(-t * 28) + 45
        s = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 18)
        self.add(beat, s, vol)
    def snare(self, beat, vol=0.3, seed=1):
        ln = int(0.14 * SR); t = np.arange(ln) / SR
        s = noise(ln, seed) * np.exp(-t * 26) + 0.4 * np.sin(2 * np.pi * 190 * t) * np.exp(-t * 30)
        self.add(beat, s, vol)
    def hat(self, beat, vol=0.08, seed=2, length=0.035):
        ln = int(length * SR); n_ = noise(ln, seed)
        n_ = np.diff(np.concatenate([[0], n_]))  # tiz
        self.add(beat, n_ * np.exp(-np.arange(ln) / SR * 90), vol)
    def save(self, name):
        x = self.buf
        # Basit yankı
        d = int(self.spb * 0.75 * SR)
        echo = np.roll(x, d) * 0.18  # dongu icin dairesel yanki
        x = x + echo
        # Dongu sonu ile basi arasinda tiklama olmasin diye ilk/son 5 ms yumusat
        x = x / max(1e-6, np.max(np.abs(x))) * 0.85
        pcm = (x * 32767).astype(np.int16)
        wav = f"/tmp/{name}.wav"
        import wave
        with wave.open(wav, "wb") as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
        out = os.path.join(RAW, f"{name}.ogg")
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "3", out], check=True)
        print(name, f"{len(x) / SR:.1f}s", os.path.getsize(out) // 1024, "KB")

def chord_notes(root, kind, octv):
    r = midi(root + str(octv))
    iv = {'maj': [0, 4, 7], 'min': [0, 3, 7], 'dom': [0, 4, 7, 10]}[kind]
    names = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B']
    return [names[(r + i) % 12] + str((r + i) // 12 - 1) for i in iv]

# ---------------------------------------------------------------- 1) Maden Kosusu: hizli, gerilimli
def mine_run():
    s = Song(152, 16)
    prog = [('A', 'min'), ('F', 'maj'), ('C', 'maj'), ('G', 'maj'), ('A', 'min'), ('F', 'maj'), ('G', 'maj'), ('E', 'maj')] * 2
    for bar, (root, kind) in enumerate(prog):
        b0 = bar * 4
        c = chord_notes(root, kind, 2)
        # Bas: sekizlik, kok-oktav
        bass = []
        for k in range(8):
            n = c[0] if k % 2 == 0 else c[0][:-1] + str(int(c[0][-1]) + 1)
            bass.append((n, 0.5))
        s.voice(bass, 'tri', 0.42, start=b0, legato=0.8)
        # Arpej (dortte bir hizli)
        arp = chord_notes(root, kind, 4)
        s.voice([(arp[k % 3], 0.25) for k in range(16)], 'pulse', 0.06, duty=0.125, start=b0, legato=0.6, envp=dict(d=0.03, s=0.3))
        # Davul
        for k in range(4):
            if k in (0, 2): s.kick(b0 + k)
            else: s.snare(b0 + k, seed=bar * 4 + k)
            s.hat(b0 + k + 0.5, seed=bar * 8 + k)
        if bar % 4 == 3: s.kick(b0 + 3.5, 0.4)
    # Melodi (A A' B A'')
    A = [('E5', 0.5), ('E5', 0.5), ('A5', 1), ('G5', 0.5), ('E5', 0.5), ('C5', 1),
         ('D5', 0.5), ('E5', 0.5), ('F5', 1), ('E5', 0.5), ('C5', 0.5), ('A4', 1),
         ('C5', 0.5), ('E5', 0.5), ('G5', 1), ('A5', 0.5), ('G5', 0.5), ('E5', 1),
         ('D5', 0.5), ('B4', 0.5), ('G4', 1), ('B4', 1), ('D5', 1)]
    A2 = A[:12] + [('B4', 0.5), ('D5', 0.5), ('G5', 1), ('F5', 0.5), ('D5', 0.5), ('B4', 1),
                   ('G#4', 0.5), ('B4', 0.5), ('E5', 1), ('D5', 0.5), ('B4', 0.5), ('G#4', 1)]
    s.voice(A + A2, 'pulse', 0.13, duty=0.25, start=0, envp=dict(d=0.06, s=0.5))
    s.voice(A + A2, 'pulse', 0.13, duty=0.25, start=32, envp=dict(d=0.06, s=0.5))
    s.save("music_mine_run")

# ---------------------------------------------------------------- 2) Tilki Macerasi: nese, ziplayan
def fox():
    s = Song(136, 16)
    prog = [('C', 'maj'), ('A', 'min'), ('F', 'maj'), ('G', 'maj')] * 4
    for bar, (root, kind) in enumerate(prog):
        b0 = bar * 4
        c = chord_notes(root, kind, 2)
        fifth = chord_notes(root, kind, 2)[2]
        s.voice([(c[0], 1), (fifth, 1), (c[0], 1), (fifth, 1)], 'tri', 0.42, start=b0, legato=0.55)
        up = chord_notes(root, kind, 4)
        for k in (0.5, 1.5, 2.5, 3.5):
            s.voice([(up[0], 0.5)], 'pulse', 0.04, duty=0.5, start=b0 + k, legato=0.4)
            s.voice([(up[1], 0.5)], 'pulse', 0.04, duty=0.5, start=b0 + k, legato=0.4)
        s.kick(b0); s.kick(b0 + 2)
        s.snare(b0 + 1, 0.18, seed=bar); s.snare(b0 + 3, 0.18, seed=bar + 50)
        for k in range(8): s.hat(b0 + k * 0.5, 0.05, seed=bar * 8 + k)
    M = [('E5', 0.5), ('G5', 0.5), ('C6', 0.75), ('B5', 0.25), ('A5', 0.5), ('G5', 0.5), ('E5', 1),
         ('A5', 0.5), ('C6', 0.5), ('E6', 0.75), ('D6', 0.25), ('C6', 0.5), ('A5', 0.5), ('E5', 1),
         ('F5', 0.5), ('A5', 0.5), ('C6', 1), ('A5', 0.5), ('F5', 0.5), ('A5', 1),
         ('G5', 0.5), ('B5', 0.5), ('D6', 0.5), ('B5', 0.5), ('G5', 2)]
    M2 = M[:20] + [('D6', 0.5), ('C6', 0.5), ('B5', 0.5), ('D6', 0.5), ('C6', 2)]
    B = [('C6', 1), ('G5', 0.5), ('E5', 0.5), ('C5', 1), (None, 1),
         ('E6', 1), ('C6', 0.5), ('A5', 0.5), ('E5', 1), (None, 1),
         ('F5', 0.5), ('G5', 0.5), ('A5', 0.5), ('C6', 0.5), ('F6', 1), ('E6', 1),
         ('D6', 0.5), ('B5', 0.5), ('G5', 0.5), ('B5', 0.5), ('D6', 2)]
    s.voice(M + M2 + B + M2, 'pulse', 0.12, duty=0.5, start=0, envp=dict(d=0.05, s=0.55), vib=0.004)
    s.save("music_fox")

# ---------------------------------------------------------------- 3) Orman Muhafizi: gizemli, gergin
def forest():
    s = Song(100, 16)
    prog = [('D', 'min'), ('C', 'maj'), ('A#', 'maj'), ('C', 'maj'), ('D', 'min'), ('C', 'maj'), ('A#', 'maj'), ('A', 'maj')] * 2
    for bar, (root, kind) in enumerate(prog):
        b0 = bar * 4
        c = chord_notes(root, kind, 2)
        s.voice([(c[0], 2), (c[0], 1.5), (c[2], 0.5)], 'tri', 0.45, start=b0, legato=0.95, envp=dict(a=0.02, d=0.2, s=0.7, r=0.1))
        arp = chord_notes(root, kind, 4)
        seq = [arp[0], arp[1], arp[2], arp[1], arp[0], arp[2], arp[1], arp[2]]
        s.voice([(n, 0.5) for n in seq], 'pulse', 0.05, duty=0.25, start=b0, legato=0.7, envp=dict(a=0.01, d=0.15, s=0.25, r=0.08))
        # Davul: yavas, tok
        s.kick(b0, 0.5)
        s.kick(b0 + 2.5, 0.35)
        s.snare(b0 + 2, 0.12, seed=bar)
        for k in range(8): s.hat(b0 + k * 0.5 + 0.25, 0.03, seed=bar * 8 + k, length=0.05)
    L = [('A5', 1.5), ('G5', 0.5), ('F5', 1), ('E5', 1), ('G5', 1.5), ('F5', 0.5), ('E5', 2),
         ('D5', 1.5), ('E5', 0.5), ('F5', 1), ('G5', 1), ('E5', 3), (None, 1),
         ('A5', 1.5), ('C6', 0.5), ('A5', 1), ('G5', 1), ('E5', 1.5), ('G5', 0.5), ('F5', 2),
         ('D5', 1.5), ('F5', 0.5), ('E5', 1), ('C#5', 1), ('D5', 3), (None, 1)]
    s.voice(L, 'pulse', 0.1, duty=0.5, start=0, envp=dict(a=0.03, d=0.2, s=0.6, r=0.15), vib=0.006)
    s.voice(L, 'pulse', 0.1, duty=0.25, start=32, envp=dict(a=0.03, d=0.2, s=0.6, r=0.15), vib=0.006)
    s.save("music_forest")

# ---------------------------------------------------------------- 4) Mahjong: sakin, pentatonik, telli calgi
def mahjong():
    s = Song(72, 16)
    pluck = dict(a=0.003, d=1.1, s=0.0, r=0.05)
    # Bas: yumusak, uzun notalar (D pentatonik: D E F# A B)
    roots = ['D3', 'B2', 'G2', 'A2'] * 4
    for bar, r in enumerate(roots):
        s.voice([(r, 4)], 'sine', 0.22, start=bar * 4, legato=0.98, envp=dict(a=0.25, d=0.5, s=0.7, r=0.6))
        fifth = {'D3': 'A3', 'B2': 'F#3', 'G2': 'D3', 'A2': 'E3'}[r]
        s.voice([(fifth, 2), (r[:-1] + str(int(r[-1]) + 1), 2)], 'sine', 0.1, start=bar * 4, legato=0.95, envp=dict(a=0.2, d=0.4, s=0.6, r=0.4))
        # Arpej: telli calgi gibi
        arp = {'D3': ['D4', 'F#4', 'A4', 'D5'], 'B2': ['B3', 'D4', 'F#4', 'B4'], 'G2': ['G3', 'B3', 'D4', 'G4'], 'A2': ['A3', 'E4', 'A4', 'C#5']}[r]
        s.voice([(arp[k % 4], 0.5) for k in range(8)], 'tri', 0.07, start=bar * 4, legato=1.0, envp=pluck)
    M = [('A5', 1), ('F#5', 0.5), ('E5', 0.5), ('D5', 1.5), ('E5', 0.5), ('F#5', 2), ('A5', 1), ('B5', 1),
         ('A5', 1.5), ('F#5', 0.5), ('E5', 1), ('D5', 1), ('E5', 3), (None, 1),
         ('D6', 1), ('B5', 0.5), ('A5', 0.5), ('F#5', 1.5), ('A5', 0.5), ('B5', 2), ('A5', 1), ('F#5', 1),
         ('E5', 1.5), ('F#5', 0.5), ('A5', 1), ('E5', 1), ('D5', 3), (None, 1)]
    s.voice(M, 'tri', 0.2, start=0, legato=1.0, envp=dict(a=0.004, d=1.4, s=0.0, r=0.05))
    s.voice(M, 'sine', 0.12, start=32, legato=1.0, envp=dict(a=0.004, d=1.6, s=0.0, r=0.05))
    s.voice([(n if n is None else n[:-1] + str(int(n[-1]) - 1), d) for n, d in M], 'tri', 0.1, start=32, legato=1.0, envp=dict(a=0.004, d=1.2, s=0.0, r=0.05))
    s.save("music_mahjong")

if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1: globals()[sys.argv[1]]()
    else: mine_run(); fox(); forest(); mahjong()
