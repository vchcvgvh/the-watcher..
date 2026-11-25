"""Synthesizes every custom sound for The Watcher from scratch (no samples) -> mono OGG Vorbis."""
import numpy as np, subprocess, os, sys
from scipy import signal
SR = 44100
OUT = sys.argv[1]
rng = np.random.default_rng(1337)

def t_(d): return np.arange(int(d * SR)) / SR
def env(n, a, r, shape=1.0):
    e = np.ones(n); A = int(a * SR); R = int(r * SR)
    if A: e[:A] = np.linspace(0, 1, A) ** shape
    if R: e[-R:] *= np.linspace(1, 0, R) ** shape
    return e
def bp(x, f, q):
    b, a = signal.iirpeak(min(f, SR / 2 - 100) / (SR / 2), q); return signal.lfilter(b, a, x)
def lp(x, f, o=2):
    b, a = signal.butter(o, min(f, SR / 2 - 100) / (SR / 2)); return signal.lfilter(b, a, x)
def hp(x, f, o=2):
    b, a = signal.butter(o, f / (SR / 2), 'high'); return signal.lfilter(b, a, x)
def noise(n): return rng.standard_normal(n)
def pink(n):
    w = np.fft.rfft(noise(n)); f = np.arange(len(w)); f[0] = 1
    return np.fft.irfft(w / np.sqrt(f), n)
def smooth_rand(n, rate, amt):
    k = max(2, int(n / SR * rate) + 2)
    return np.interp(np.arange(n), np.linspace(0, n, k), rng.standard_normal(k)) * amt
def osc(f, kind='saw'):
    ph = np.cumsum(f) / SR
    if kind == 'saw': return 2 * (ph % 1) - 1
    if kind == 'sin': return np.sin(2 * np.pi * ph)
    if kind == 'sq': return np.sign(np.sin(2 * np.pi * ph))
def reverb(x, decay=1.6, mix=0.35, damp=4000):
    n = int(decay * SR); ir = noise(n) * np.exp(-np.linspace(0, 7, n)); ir = lp(ir, damp)
    wet = signal.fftconvolve(x, ir)[: len(x) + n]
    wet = np.pad(wet, (0, len(x) + n - len(wet))); wet /= np.max(np.abs(wet)) + 1e-9
    y = np.concatenate([x, np.zeros(n)]); return (1 - mix) * y + mix * wet * np.max(np.abs(x))
def norm(x, peak=0.95):
    x = x - np.mean(x); return x / (np.max(np.abs(x)) + 1e-9) * peak
def fade(x, ms=8):
    k = int(ms / 1000 * SR); x[:k] *= np.linspace(0, 1, k); x[-k:] *= np.linspace(1, 0, k); return x
def save(name, x, peak=0.95):
    x = fade(norm(x, peak), 3); os.makedirs(OUT, exist_ok=True)
    raw = (x * 32767).astype('<i2').tobytes()
    subprocess.run(['ffmpeg', '-loglevel', 'error', '-y', '-f', 's16le', '-ar', str(SR), '-ac', '1', '-i', '-',
                    '-c:a', 'libvorbis', '-q:a', '5', f'{OUT}/{name}.ogg'], input=raw, check=True)
    print(name, f'{len(x)/SR:.1f}s')

def voice(f0, formants, breath=0.2, rough=0.0):
    """Source-filter voice: jittery sawtooth glottis + breath noise through formant resonators."""
    n = len(f0); f = f0 * (1 + smooth_rand(n, 40, 0.012) + rough * smooth_rand(n, 300, 0.05))
    src = osc(f) * (1 + rough * smooth_rand(n, 70, 0.6)) + breath * noise(n)
    out = sum(g * bp(src, F, F / bw) for F, bw, g in formants)
    return out

AA = [(850, 120, 1.0), (1300, 150, 0.7), (2700, 250, 0.45), (3600, 300, 0.25)]
EE = [(400, 80, 1.0), (2300, 160, 0.6), (3000, 250, 0.4)]
OO = [(450, 90, 1.0), (850, 110, 0.6), (2500, 250, 0.2)]

def watcher_scream(seed):
    global rng; rng = np.random.default_rng(seed)
    d = 1.9; t = t_(d); n = len(t)
    contour = np.interp(t, [0, 0.12, 0.5, 1.3, d], [300, 640, 700, 560, 380]) * (1 + 0.04 * np.sin(2 * np.pi * 6.5 * t))
    a = voice(contour, AA, 0.35, rough=1.0)
    b = voice(contour * 1.5 + 13, EE, 0.3, rough=1.0) * 0.6          # second, inhuman voice a fifth up
    c = voice(contour * 0.5, AA, 0.5, rough=1.5) * 0.7               # growl underneath
    x = np.tanh(3 * (a + b + c) / np.max(np.abs(a + b + c))) * env(n, 0.04, 0.6)
    x += hp(noise(n), 3000) * 0.15 * env(n, 0.01, 1.2)
    return reverb(x, 1.4, 0.3)

def stinger():
    d = 2.6; t = t_(d); n = len(t)
    hit = (lp(noise(n), 300, 4) * 3 + noise(n) * 0.6) * np.exp(-t * 9)
    boom = osc(np.interp(t, [0, 0.4, d], [90, 38, 30]), 'sin') * np.exp(-t * 1.8) * 1.4
    cluster = sum(osc(np.full(n, f * (1 + rng.uniform(-0.004, 0.004)))) for f in [220, 233.1, 246.9, 311.1, 329.6, 466.2, 493.9])
    cluster = lp(cluster, 3500) * np.exp(-t * 1.3) * 0.35
    screech = bp(osc(np.interp(t, [0, d], [1800, 1650]) * (1 + 0.02 * np.sin(2 * np.pi * 17 * t))), 2400, 4) * np.exp(-t * 2.2) * 0.9
    x = np.tanh(1.6 * (hit + boom + cluster + screech))
    return reverb(x, 2.2, 0.35)

def whisper(seed):
    global rng; rng = np.random.default_rng(seed)
    d = 2.4; n = int(d * SR); x = np.zeros(n); pos = int(0.1 * SR)
    while pos < n - int(0.25 * SR):
        L = int(rng.uniform(0.07, 0.22) * SR); seg = hp(noise(L), 600)
        F = [(rng.uniform(300, 900), 1.0), (rng.uniform(1100, 2500), 0.8), (rng.uniform(2600, 3800), 0.5)]
        s = sum(g * bp(seg, f, 5) for f, g in F)
        if rng.random() < 0.35: s += hp(noise(L), 4500) * 0.8 * env(L, 0.005, 0.05)   # "s"/"sh"
        x[pos:pos + L] += s * np.hanning(L) ** 0.7
        pos += L + int(rng.uniform(0.0, 0.12) * SR)
    x *= env(n, 0.2, 0.4)
    return reverb(x, 0.9, 0.25)

def breathing():
    d = 4.2; t = t_(d); n = len(t); x = np.zeros(n)
    for start, L, f, g in [(0.1, 1.3, 1200, 0.8), (1.55, 1.6, 800, 1.0), (3.2, 0.9, 1300, 0.7)]:
        a = int(start * SR); m = int(L * SR); seg = noise(m)
        seg = bp(seg, f, 2) + 0.6 * bp(seg, f * 2.3, 4) + 0.3 * bp(noise(m), 300, 3) * smooth_rand(m, 25, 1.5)
        x[a:a + m] += seg * np.sin(np.linspace(0, np.pi, m)) ** 1.5 * g
    rasp = osc(np.full(n, 47)) * bp(noise(n), 700, 2) * 0.12
    return reverb(lp(x + rasp * (np.abs(x) > 0.05), 2600, 3), 0.6, 0.2)

def heartbeat():
    d = 1.3; t = t_(d); n = len(t); x = np.zeros(n)
    for s, g in [(0.02, 1.0), (0.3, 0.7)]:
        a = int(s * SR); m = int(0.25 * SR); tt = np.arange(m) / SR
        x[a:a + m] += (np.sin(2 * np.pi * np.interp(tt, [0, 0.25], [65, 40]) * tt) * np.exp(-tt * 22) + lp(noise(m), 150) * np.exp(-tt * 30) * 0.8) * g
    return np.tanh(lp(x, 400) * 2)

def static_burst():
    d = 1.6; t = t_(d); n = len(t)
    gate = (smooth_rand(n, 30, 1) > -0.3).astype(float); gate = lp(gate, 200)
    crackle = np.zeros(n); idx = rng.integers(0, n, 400); crackle[idx] = rng.uniform(-1, 1, 400); crackle = lp(crackle, 6000)
    tone = osc(np.full(n, 60), 'sq') * 0.1 + osc(np.full(n, 1000 + 400 * np.sin(2 * np.pi * 3 * t)), 'sin') * 0.08 * (smooth_rand(n, 8, 1) > 0.6)
    x = bp(noise(n), 2500, 0.7) * gate + crackle * 3 + tone
    return np.round(x * 12) / 12 * env(n, 0.01, 0.2)       # crushed

def skitter(seed):
    global rng; rng = np.random.default_rng(seed)
    d = 1.3; n = int(d * SR); x = np.zeros(n); pos = 0
    while pos < n - 3000:
        m = int(0.012 * SR); tt = np.arange(m) / SR
        click = noise(m) * np.exp(-tt * 400) + np.sin(2 * np.pi * rng.uniform(1800, 3600) * tt) * np.exp(-tt * 250) * 0.6
        x[pos:pos + m] += click * rng.uniform(0.4, 1)
        pos += int(rng.choice([0.025, 0.035, 0.05, 0.12]) * SR)
    x = x + 0.5 * bp(x, 900, 3)
    return reverb(x * env(n, 0.05, 0.3), 0.4, 0.2)

def chitter():
    d = 1.4; t = t_(d); n = len(t)
    clicks = np.zeros(n); rate = 32 + 10 * np.sin(2 * np.pi * 1.3 * t)
    ph = np.cumsum(rate) / SR; edges = np.where(np.diff(np.floor(ph)) > 0)[0]
    for e in edges:
        m = min(400, n - e); tt = np.arange(m) / SR; clicks[e:e + m] += noise(m) * np.exp(-tt * 700)
    shriek = voice(np.interp(t, [0, 0.6, d], [1100, 1500, 900]), EE, 0.5, rough=1.2) * env(n, 0.3, 0.5) * 0.3
    return reverb(bp(clicks, 2200, 1.5) * 2 + clicks * 0.5 + shriek, 0.5, 0.2)

def growl(seed):
    global rng; rng = np.random.default_rng(seed)
    d = 2.0; t = t_(d); n = len(t)
    f0 = 68 + smooth_rand(n, 4, 8)
    am = 0.55 + 0.45 * np.abs(np.sin(2 * np.pi * (11 + smooth_rand(n, 3, 3)) * t))
    x = voice(f0, OO, 0.6, rough=2.0) * am + lp(noise(n), 250) * am * 0.8
    return np.tanh(2.5 * norm(x)) * env(n, 0.25, 0.5)

def howl():
    d = 3.6; t = t_(d); n = len(t)
    f = np.interp(t, [0, 0.5, 1.6, 2.6, d], [260, 520, 560, 470, 220]) * (1 + 0.025 * np.sin(2 * np.pi * 5 * t))
    a = voice(f, OO, 0.15, rough=0.4); b = voice(f * 1.06, AA, 0.15, rough=0.6) * 0.5   # two throats, out of tune
    x = (a + b) * env(n, 0.4, 1.2)
    return reverb(x, 3.0, 0.45)

def faceless_hum():
    d = 4.5; t = t_(d); n = len(t)
    drone = sum(osc(np.full(n, f), 'sin') * g for f, g in [(55, 1), (55.7, 1), (110.4, 0.5), (164, 0.3), (233, 0.15)])
    moan = voice(np.interp(t, [0, 2, d], [140, 125, 150]), [(300, 60, 1.0), (700, 100, 0.3)], 0.3, rough=0.5)   # through a sewn mouth
    moan = lp(moan, 600) * env(n, 1.0, 1.2) * smooth_rand(n, 2, 0.5) ** 2
    return reverb(drone * env(n, 1.0, 1.0) * 0.6 + moan, 2.0, 0.3)

def drone():
    d = 9.0; t = t_(d); n = len(t)
    x = lp(pink(n), 180, 3) * 2 + sum(osc(np.full(n, f), 'sin') * 0.2 for f in [41, 41.3, 61.7, 87.3])
    x += bp(noise(n), 1700, 30) * 0.15 * (0.5 + 0.5 * np.sin(2 * np.pi * 0.2 * t))      # far-off metallic whine
    return x * env(n, 2.5, 2.5)

def glitch():
    d = 0.9; n = int(d * SR); x = np.zeros(n); pos = 0
    while pos < n:
        m = int(rng.uniform(0.02, 0.09) * SR); k = rng.integers(0, 4)
        tt = np.arange(m) / SR
        seg = [osc(np.full(m, rng.uniform(80, 2000)), 'sq'), noise(m), np.sin(2 * np.pi * rng.uniform(3000, 7000) * tt), np.zeros(m)][k]
        x[pos:pos + m] = seg[: n - pos][: m] if pos + m <= n else seg[: n - pos]
        pos += m
    return np.round(x * 4) / 4

def knock():
    d = 2.0; n = int(d * SR); x = np.zeros(n)
    for s in [0.05, 0.42, 0.78]:
        a = int(s * SR); m = int(0.25 * SR); tt = np.arange(m) / SR
        k = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * dmp) * g for f, dmp, g in [(140, 30, 1), (310, 45, 0.6), (520, 60, 0.4)]) + noise(m) * np.exp(-tt * 200) * 0.5
        x[a:a + m] += k
    return reverb(x, 0.7, 0.25)

def distant_scream():
    x = watcher_scream(99); x = lp(x, 1400); return reverb(x * 0.6, 3.5, 0.6, 2000)

def bone_crack():
    d = 0.5; t = t_(d); n = len(t)
    x = np.zeros(n)
    for s in [0.0, 0.03, 0.07]:
        a = int(s * SR); m = int(0.06 * SR); tt = np.arange(m) / SR
        x[a:a + m] += (noise(m) * np.exp(-tt * 120) + np.sin(2 * np.pi * rng.uniform(900, 1500) * tt) * np.exp(-tt * 90)) * rng.uniform(0.6, 1)
    squelch = bp(noise(n), 400, 2) * np.exp(-t * 7) * smooth_rand(n, 40, 1) * 0.7
    return np.tanh(2 * (x + squelch))

def tick():
    m = int(0.09 * SR); tt = np.arange(m) / SR
    x = np.sin(2 * np.pi * 2800 * tt) * np.exp(-tt * 180) + noise(m) * np.exp(-tt * 400) * 0.3
    return np.concatenate([np.zeros(int(0.01 * SR)), x])

save('watcher_scream1', watcher_scream(1)); save('watcher_scream2', watcher_scream(2))
save('jumpscare', stinger())
for i in range(3): save(f'whisper{i+1}', whisper(10 + i), 0.6)
save('breathing', breathing(), 0.7); save('heartbeat', heartbeat())
save('static', static_burst(), 0.7)
save('crawler_skitter1', skitter(20), 0.8); save('crawler_skitter2', skitter(21), 0.8); save('crawler_chitter', chitter())
save('hound_growl1', growl(30)); save('hound_growl2', growl(31)); save('hound_howl', howl())
save('faceless_hum', faceless_hum(), 0.8); save('drone', drone(), 0.7)
save('glitch', glitch(), 0.6); save('knock', knock()); save('distant_scream', distant_scream(), 0.8)
save('bone_crack', bone_crack()); save('tick', tick(), 0.5)
