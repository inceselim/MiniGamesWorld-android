"""Doga ambiyansi: dere, ruzgar/yaprak, kuslar (tamamen sentez, telifsiz). Dongulu stereo ogg uretir."""
import os, subprocess, wave
import numpy as np
from scipy.signal import butter, sosfilt

here = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(here, "../../app/src/main/res/raw")
SR = 32000
DUR = 64.0
N = int(DUR * SR)
FADE = int(3.0 * SR)  # dongu birlesimi icin capraz gecis
rng = np.random.default_rng(2026)

def band(x, lo, hi, order=2):
    sos = butter(order, [lo, hi], btype='band', fs=SR, output='sos'); return sosfilt(sos, x)
def low(x, f, order=2):
    sos = butter(order, f, btype='low', fs=SR, output='sos'); return sosfilt(sos, x)
def high(x, f, order=2):
    sos = butter(order, f, btype='high', fs=SR, output='sos'); return sosfilt(sos, x)

def lfo(n, rate, depth, phase=0.0):
    t = np.arange(n) / SR
    return 1 + depth * np.sin(2 * np.pi * rate * t + phase)

def smooth_noise(n, rate):
    """Yavas degisen rastgele egri (0..1)."""
    k = max(2, int(n / SR * rate) + 2)
    pts = rng.uniform(0, 1, k)
    return np.interp(np.linspace(0, k - 1, n), np.arange(k), pts)

L_total = N + FADE

def stream():
    n = L_total
    w = rng.normal(0, 1, n)
    base = band(w, 250, 2200) * (0.6 + 0.4 * smooth_noise(n, 0.6))
    hiss = band(rng.normal(0, 1, n), 2500, 6000) * 0.12 * (0.5 + 0.5 * smooth_noise(n, 1.5))
    x = base + hiss
    # gurultu baloncuklari (su sesi "glu glu")
    bub = np.zeros(n)
    t_ = 0
    while True:
        t_ += int(rng.exponential(0.035) * SR)
        if t_ >= n - SR // 10: break
        ln = int(rng.uniform(0.012, 0.045) * SR)
        tt = np.arange(ln) / SR
        f0 = rng.uniform(350, 1100); f1 = f0 * rng.uniform(1.3, 2.2)
        f = f0 + (f1 - f0) * tt / tt[-1]
        s = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * rng.uniform(60, 140))
        bub[t_:t_ + ln] += s * rng.uniform(0.15, 0.6)
    x = x / np.std(x) * 0.06 + bub * 0.08
    return x

def wind():
    n = L_total
    w = low(rng.normal(0, 1, n), 700)
    rustle = band(rng.normal(0, 1, n), 1500, 6000) * smooth_noise(n, 3) ** 3
    env = 0.35 + 0.65 * smooth_noise(n, 0.12) ** 2
    x = w / np.std(w) * 0.05 * env + rustle / np.std(rustle) * 0.018 * env
    return x

def chirp_bird():
    """Hizli inen cik-cik serisi."""
    out = []
    k = rng.integers(2, 6); f_top = rng.uniform(3200, 5200)
    for i in range(k):
        ln = int(rng.uniform(0.04, 0.08) * SR); tt = np.arange(ln) / SR
        f = f_top * (1 - 0.35 * tt / tt[-1])
        s = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * tt / tt[-1]) ** 2
        out.append(s); out.append(np.zeros(int(rng.uniform(0.05, 0.11) * SR)))
    return np.concatenate(out)

def warble_bird():
    """Titresimli otus (FM)."""
    ln = int(rng.uniform(0.6, 1.4) * SR); tt = np.arange(ln) / SR
    fc = rng.uniform(2400, 3800) * (1 + 0.15 * np.sin(2 * np.pi * rng.uniform(1, 3) * tt))
    vib = rng.uniform(18, 35)
    f = fc * (1 + 0.08 * np.sin(2 * np.pi * vib * tt))
    amp = np.clip(np.sin(np.pi * tt / tt[-1]), 0, 1) * (0.6 + 0.4 * np.abs(np.sin(2 * np.pi * rng.uniform(4, 8) * tt)))
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * amp

def whistle_bird():
    """Iki notali islik: 'fii-bii'."""
    parts = []
    notes = [(rng.uniform(2600, 3200), rng.uniform(2400, 3000)), (rng.uniform(2000, 2400), rng.uniform(1900, 2300))]
    if rng.random() < 0.4: notes.append(notes[1])
    for a, b in notes:
        ln = int(rng.uniform(0.18, 0.3) * SR); tt = np.arange(ln) / SR
        f = a + (b - a) * tt / tt[-1]
        env = np.minimum(1, tt / 0.02) * np.minimum(1, (tt[-1] - tt) / 0.06)
        parts.append(np.sin(2 * np.pi * np.cumsum(f) / SR) * env + 0.15 * np.sin(4 * np.pi * np.cumsum(f) / SR) * env)
        parts.append(np.zeros(int(0.08 * SR)))
    return np.concatenate(parts)

def cuckoo():
    """Uzaktan guguk: iki inen ton."""
    parts = []
    for f in (690, 560):
        ln = int(0.32 * SR); tt = np.arange(ln) / SR
        env = np.minimum(1, tt / 0.03) * np.exp(-tt * 3)
        parts.append(np.sin(2 * np.pi * f * tt) * env); parts.append(np.zeros(int(0.12 * SR)))
    return np.concatenate(parts)

def birds():
    left = np.zeros(L_total); right = np.zeros(L_total)
    t_ = int(1.0 * SR)
    kinds = [(chirp_bird, 0.35), (warble_bird, 0.25), (whistle_bird, 0.3), (cuckoo, 0.1)]
    last_cuckoo = -99
    while t_ < N - 2 * SR:
        r = rng.random(); acc = 0
        for fn, p in kinds:
            acc += p
            if r <= acc: break
        if fn is cuckoo:
            if t_ / SR - last_cuckoo < 20: fn = whistle_bird
            else: last_cuckoo = t_ / SR
        s = fn()
        dist = rng.uniform(0.25, 1.0)                  # uzaklik: ses ve parlaklik
        s = high(s, 1200) if fn is not cuckoo else s
        if dist < 0.5: s = low(s, 3500)
        vol = (0.09 + 0.2 * dist) * (0.5 if fn is cuckoo else 1)
        pan = rng.uniform(-0.9, 0.9)
        # basit yanki (orman)
        s2 = np.concatenate([s, np.zeros(int(0.35 * SR))])
        for d, g in ((0.09, 0.25), (0.17, 0.14), (0.29, 0.08)):
            k = int(d * SR); s2[k:k + len(s)] += s * g
        seg = s2[: L_total - t_]
        left[t_:t_ + len(seg)] += seg * vol * (1 - pan) / 2 * 1.4
        right[t_:t_ + len(seg)] += seg * vol * (1 + pan) / 2 * 1.4
        # kuslar bazen art arda cevap verir
        t_ += len(s) + int((rng.exponential(1.6) + 0.25) * SR)
    return left, right

def make():
    st = stream(); wd = wind()
    bl, br = birds()
    # dere biraz sola, ruzgar genis
    L = st * 0.75 + wd * 0.9 + bl
    R = st * 0.55 + np.roll(wd, int(0.013 * SR)) * 0.9 + br
    # dongu: son FADE ornegini basa capraz gecis ile karistir
    def loop(x):
        y = x[:N].copy(); fade = np.linspace(0, 1, FADE)
        y[:FADE] = x[:FADE] * fade + x[N:N + FADE] * (1 - fade)
        return y
    L, R = loop(L), loop(R)
    m = max(np.max(np.abs(L)), np.max(np.abs(R)))
    L, R = L / m * 0.8, R / m * 0.8
    pcm = (np.stack([L, R], axis=1) * 32767).astype(np.int16)
    wav = "/tmp/music_mahjong.wav"
    with wave.open(wav, "wb") as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
    out = os.path.join(RAW, "music_mahjong.ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "2", out], check=True)
    print("music_mahjong", f"{N / SR:.1f}s", os.path.getsize(out) // 1024, "KB")

if __name__ == "__main__":
    make()
