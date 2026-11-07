"""Kulup Patronu sesleri (tamamen sentez, telifsiz):
- music_football_menu: sakin, umut veren menu muzigi (pad + arpej + bas), dongulu
- music_football_crowd: stadyum ambiyansi (kalabalik ugultusu, tezahurat, alkis), dongulu
"""
import os, subprocess, wave, tempfile
import numpy as np
from scipy.signal import butter, sosfilt

here = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(here, "../../app/src/main/res/raw")
SR = 32000
rng = np.random.default_rng(1907)

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

def loopify(x, n, fade):
    """[n+fade] uzunlugundaki sinyali n uzunlugunda kesintisiz donguye cevir."""
    y = x[:n].copy()
    r = np.linspace(0, 1, fade)
    y[:fade] = y[:fade] * r + x[n:n + fade] * (1 - r)
    return y

# ====================================================================== menu muzigi
def menu():
    bpm = 92; beat = 60 / bpm
    bars = 16; n = int(bars * 4 * beat * SR)
    t = np.arange(n) / SR
    L = np.zeros(n); R = np.zeros(n)
    # D majorde I - V - vi - IV (D A Bm G), her akor 2 olcu
    prog = [(62, [0, 4, 7]), (57, [0, 4, 7]), (59, [0, 3, 7]), (55, [0, 4, 7])] * 2
    for i, (root, iv) in enumerate(prog):
        s = int(i * 8 * beat * SR); e = int((i + 1) * 8 * beat * SR)
        seg = np.arange(e - s) / SR
        envp = np.minimum(1, seg / 0.8) * np.minimum(1, (seg[-1] - seg + 0.01) / 0.8)
        # pad: hafif detune edilmis testere -> alcak geciren
        pad = np.zeros(e - s)
        for k in iv + [12]:
            f = freq(root + k)
            for d in (-0.12, 0.12):
                ph = 2 * np.pi * f * (1 + d / 100) * seg
                pad += (2 * ((f * (1 + d / 100) * seg) % 1) - 1) * 0.5 + np.sin(ph) * 0.5
        pad = low(pad, 1600) * envp * 0.05
        L[s:e] += pad * 0.9; R[s:e] += pad * 1.1
        # bas: kok nota, yarim notalarla
        for b in range(4):
            bs = s + int(b * 2 * beat * SR); bl = int(1.8 * beat * SR)
            tt = np.arange(bl) / SR
            f = freq(root - 12 if root >= 57 else root)
            v = np.sin(2 * np.pi * f * tt) * np.exp(-tt * 1.2) * 0.22
            L[bs:bs + bl] += v; R[bs:bs + bl] += v
        # arpej: sekizlikler, akor tonlari yukari-asagi
        notes = [root + 12 + k for k in iv] + [root + 24] + [root + 12 + k for k in reversed(iv)]
        for b in range(16):
            m = notes[b % len(notes)]
            bs = s + int(b * beat / 2 * SR); bl = int(0.45 * SR)
            if bs + bl > n: break
            tt = np.arange(bl) / SR
            f = freq(m)
            v = (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(4 * np.pi * f * tt)) * np.exp(-tt * 7) * 0.07
            pan = 0.5 + 0.35 * np.sin(b * 0.7)
            L[bs:bs + bl] += v * (1 - pan) * 1.6; R[bs:bs + bl] += v * pan * 1.6
    # hafif davul: tok kick her vurusta, yumusak shaker
    for b in range(bars * 4):
        bs = int(b * beat * SR)
        kl = int(0.25 * SR); tt = np.arange(kl) / SR
        k = np.sin(2 * np.pi * (55 + 60 * np.exp(-tt * 30)) * tt) * np.exp(-tt * 14) * (0.16 if b % 2 == 0 else 0.09)
        e = min(n, bs + kl); L[bs:e] += k[:e - bs]; R[bs:e] += k[:e - bs]
        for h in (0, 0.5):
            hs = bs + int(h * beat * SR); hl = int(0.06 * SR)
            if hs + hl > n: continue
            sh = high(rng.normal(0, 1, hl), 5000) * np.exp(-np.arange(hl) / SR * 60) * 0.025
            L[hs:hs + hl] += sh * 0.8; R[hs:hs + hl] += sh * 1.2
    # basit yanki
    d = int(0.33 * SR)
    for ch in (L, R):
        tail = ch.copy(); ch[d:] += tail[:-d] * 0.25
    write_ogg("music_football_menu", L, R)

# ====================================================================== stadyum
def crowd():
    dur = 60.0; n = int(dur * SR); fade = int(3 * SR); m = n + fade
    def smooth(rate):
        k = max(2, int(m / SR * rate) + 2)
        return np.interp(np.linspace(0, k - 1, m), np.arange(k), rng.uniform(0, 1, k))
    out = []
    for ch in range(2):
        # kalabalik ugultusu: cok sayida "ses" = bant gecirgen gurultu + yavas dalga
        mur = band(rng.normal(0, 1, m), 180, 1400) * (0.55 + 0.45 * smooth(0.25))
        mur += band(rng.normal(0, 1, m), 1400, 3200) * 0.25 * (0.5 + 0.5 * smooth(0.6))
        # yakin seyirci dalgalanmasi
        mur *= 0.8 + 0.2 * smooth(2.0)
        x = mur * 0.5
        # tezahurat: "o-o-o" benzeri sesli (formant) koro, ritmik alkis ile
        for start in (8.0, 31.0, 49.0):
            ln = int(rng.uniform(7, 10) * SR); s = int((start + ch * 0.05) * SR)
            tt = np.arange(ln) / SR
            beat = 0.62
            gate = 0.5 + 0.5 * np.sign(np.sin(2 * np.pi * tt / (beat * 2)))  # iki vurusta bir hece
            gate = low(gate, 12)
            f0 = 160 + 20 * np.sin(2 * np.pi * tt / (beat * 4))
            voices = np.zeros(ln)
            for v in range(14):
                fv = f0 * rng.uniform(0.97, 1.03) * rng.choice([1, 1, 1.5, 2])
                ph = 2 * np.pi * np.cumsum(fv) / SR + rng.uniform(0, 6)
                voices += (2 * ((ph / (2 * np.pi)) % 1) - 1)
            voices = band(voices, 300, 900) + 0.6 * band(voices, 900, 1300)
            envc = np.minimum(1, tt / 1.5) * np.minimum(1, (tt[-1] - tt) / 1.5)
            x[s:s + ln] += voices * gate * envc * 0.07
            # alkislar
            clap_t = 0.0
            while clap_t < tt[-1] - 0.2:
                cs = s + int(clap_t * SR); cl = int(0.05 * SR)
                cnoise = band(rng.normal(0, 1, cl), 900, 4000) * np.exp(-np.arange(cl) / SR * 50)
                x[cs:cs + cl] += cnoise * 0.35 * envc[min(len(envc) - 1, int(clap_t * SR))]
                clap_t += beat
        # heyecan anlari: kisa "ooooh" kabarmalari
        for start in (19.0, 41.5, 56.0):
            ln = int(3.0 * SR); s = int(start * SR); tt = np.arange(ln) / SR
            sw = band(rng.normal(0, 1, ln), 250, 1800) * np.sin(np.pi * tt / tt[-1]) ** 2
            x[s:s + ln] += sw * 0.9
        out.append(loopify(x, n, fade))
    write_ogg("music_football_crowd", out[0], out[1])

if __name__ == "__main__":
    menu(); crowd()
