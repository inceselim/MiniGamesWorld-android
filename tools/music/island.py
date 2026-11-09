"""Ada Patronu muzigi (tamamen sentez, telifsiz):
- music_island: rahat tropikal dongu (marimba + steel drum melodisi, pad, bas, shaker, dalga ambiyansi)
"""
import os, subprocess, wave, tempfile
import numpy as np
from scipy.signal import butter, sosfilt

here = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(here, "../../app/src/main/res/raw")
SR = 32000
rng = np.random.default_rng(2026)

def band(x, lo, hi, order=2): return sosfilt(butter(order, [lo, hi], btype='band', fs=SR, output='sos'), x)
def low(x, f, order=2): return sosfilt(butter(order, f, btype='low', fs=SR, output='sos'), x)
def high(x, f, order=2): return sosfilt(butter(order, f, btype='high', fs=SR, output='sos'), x)
def freq(m): return 440.0 * 2 ** ((m - 69) / 12)

def write_ogg(name, L, R):
    pcm = np.stack([L, R], 1)
    pcm = (pcm / max(1e-9, np.abs(pcm).max()) * 0.85 * 32767).astype(np.int16)
    wav = os.path.join(tempfile.gettempdir(), name + ".wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
    out = os.path.join(RAW, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "2", out], check=True)
    print(out, os.path.getsize(out) // 1024, "KB")

def add(buf, start, sig, gain_l, gain_r):
    L, R = buf
    n = len(L)
    if start >= n: return
    e = min(n, start + len(sig))
    L[start:e] += sig[:e - start] * gain_l
    R[start:e] += sig[:e - start] * gain_r
    # Dongu sonuna tasan kuyrugu basa sar (kesintisiz dongu)
    if start + len(sig) > n:
        rest = sig[e - start:]
        L[:len(rest)] += rest * gain_l; R[:len(rest)] += rest * gain_r

def marimba(m, dur=0.6, vel=1.0):
    tt = np.arange(int(dur * SR)) / SR; f = freq(m)
    v = np.sin(2 * np.pi * f * tt) * np.exp(-tt * 6) + 0.35 * np.sin(2 * np.pi * f * 4 * tt) * np.exp(-tt * 22) \
        + 0.15 * np.sin(2 * np.pi * f * 10 * tt) * np.exp(-tt * 45)
    att = np.minimum(1, tt / 0.003)
    return v * att * vel

def steelpan(m, dur=1.1, vel=1.0):
    # Steel drum: hafif uyumsuz kismi titresimler, yumusak atak
    tt = np.arange(int(dur * SR)) / SR; f = freq(m)
    parts = [(1.0, 1.0, 3.2), (2.0, 0.55, 4.0), (3.01, 0.28, 6.0), (4.03, 0.18, 8.0), (5.1, 0.08, 10.0)]
    v = sum(a * np.sin(2 * np.pi * f * r * tt) * np.exp(-tt * d) for r, a, d in parts)
    att = np.minimum(1, tt / 0.012)
    vib = 1 + 0.002 * np.sin(2 * np.pi * 5.5 * tt)
    return v * att * vel * vib

def music():
    bpm = 96; beat = 60 / bpm
    bars = 16; n = int(bars * 4 * beat * SR)
    L = np.zeros(n); R = np.zeros(n); buf = (L, R)
    # F majorde I - vi - IV - V (F Dm Bb C), her akor 1 olcu, 4 tur
    prog = [(53, [0, 4, 7]), (50, [0, 3, 7]), (46, [0, 4, 7]), (48, [0, 4, 7])] * 4
    bar = 4 * beat
    for i, (root, iv) in enumerate(prog):
        s = int(i * bar * SR); e = int((i + 1) * bar * SR)
        seg = np.arange(e - s) / SR
        envp = np.minimum(1, seg / 0.4) * np.minimum(1, (seg[-1] - seg + 0.02) / 0.4)
        # yumusak pad
        pad = np.zeros(e - s)
        for k in iv:
            f = freq(root + 12 + k)
            for d in (-0.15, 0.15):
                pad += np.sin(2 * np.pi * f * (1 + d / 100) * seg) + 0.25 * np.sin(4 * np.pi * f * (1 + d / 100) * seg)
        pad = low(pad, 1200) * envp * 0.028
        L[s:e] += pad * 1.1; R[s:e] += pad * 0.9
        # reggae/calypso bas: 1 ve 2.5. vuruslar, 4'te gecis
        for (pos, off, ln) in ((0, 0, 0.9), (1.5, 0, 0.5), (3, 7, 0.45), (3.5, 12 if i % 2 else 7, 0.4)):
            tt = np.arange(int(ln * SR)) / SR
            f = freq(root - 12 + off)
            v = (np.sin(2 * np.pi * f * tt) + 0.2 * np.sin(4 * np.pi * f * tt)) * np.exp(-tt * 3.5) * np.minimum(1, tt / 0.01) * 0.2
            add(buf, s + int(pos * beat * SR), v, 1, 1)
        # marimba "skank": vurus arkasi akorlar
        for b in range(4):
            for k in iv:
                add(buf, s + int((b + 0.5) * beat * SR), marimba(root + 24 + k, 0.35, 0.05), 0.7, 1.2)
    # Steel drum melodisi (F majör pentatonik, iki 8 olculuk cumle)
    mel = [
        # (olcu, vurus, nota, sure)
        (0, 0, 77, 1), (0, 1, 79, .5), (0, 1.5, 81, 1.5), (0, 3, 79, 1),
        (1, 0, 77, 1.5), (1, 1.5, 74, .5), (1, 2, 72, 2),
        (2, 0, 70, 1), (2, 1, 72, .5), (2, 1.5, 74, 1), (2, 2.5, 77, 1.5),
        (3, 0, 76, 2), (3, 2, 72, 2),
        (4, 0, 77, 1), (4, 1, 79, .5), (4, 1.5, 81, 1), (4, 2.5, 84, 1.5),
        (5, 0, 81, 1.5), (5, 1.5, 79, .5), (5, 2, 77, 2),
        (6, 0, 74, 1), (6, 1, 77, 1), (6, 2, 79, 1), (6, 3, 81, 1),
        (7, 0, 77, 3),
    ]
    for rep in range(2):
        for (bb, bt, m, d) in mel:
            st = int(((rep * 8 + bb) * 4 + bt) * beat * SR)
            # ikinci turda bir oktav yukaridan eko gibi marimba ikizi
            add(buf, st, steelpan(m, max(0.6, d * beat + 0.3), 0.12), 1.15, 0.85)
            if rep == 1:
                add(buf, st + int(beat / 2 * SR), marimba(m + 12, 0.4, 0.035), 0.6, 1.3)
    # Ritim: yumusak kick, rim, shaker sekizlik + on altilik
    for b in range(bars * 4):
        bs = int(b * beat * SR)
        if b % 2 == 0:
            kl = int(0.22 * SR); tt = np.arange(kl) / SR
            k = np.sin(2 * np.pi * (50 + 70 * np.exp(-tt * 35)) * tt) * np.exp(-tt * 16) * 0.16
            add(buf, bs, k, 1, 1)
        if b % 4 == 2 or b % 4 == 3:
            rl = int(0.05 * SR)
            rim = band(rng.normal(0, 1, rl), 1500, 4500) * np.exp(-np.arange(rl) / SR * 80) * 0.05
            add(buf, bs + int((0.5 if b % 4 == 3 else 0) * beat * SR), rim, 1.1, 0.9)
        for h in (0, 0.25, 0.5, 0.75):
            hl = int(0.07 * SR)
            acc = 0.035 if h in (0.25, 0.75) else 0.02
            sh = high(rng.normal(0, 1, hl), 6000) * np.exp(-np.arange(hl) / SR * 45) * acc
            add(buf, bs + int(h * beat * SR), sh, 0.8, 1.2)
    # Dalga ambiyansi (yavas kabaran gurultu), dongu sinirinda kesintisiz
    t = np.arange(n) / SR
    for ch, ph in ((L, 0.0), (R, 1.3)):
        swell = (0.5 + 0.5 * np.sin(2 * np.pi * t / (n / SR / 4) + ph)) ** 2
        waves = low(band(rng.normal(0, 1, n + SR), 200, 2500), 1800)[SR:] * swell * 0.035
        # Basi ve sonu yumusak birlestir
        f = int(0.5 * SR); r = np.linspace(0, 1, f)
        waves[:f] *= r; waves[-f:] *= r[::-1]
        ch += waves
    # Yanki (dongu icinde sarilan)
    d = int(beat * 0.75 * SR)
    for ch in (L, R):
        tail = ch.copy()
        ch += np.roll(tail, d) * 0.22 + np.roll(tail, 2 * d) * 0.08
    write_ogg("music_island", L, R)

if __name__ == "__main__":
    music()
