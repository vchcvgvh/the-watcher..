"""Jumpscare reel v2: ports client/Jumpscares.java scene by scene onto renders of the real animated models."""
import numpy as np, subprocess, math, random, glob, re
from PIL import Image, ImageDraw, ImageFont
W, H, FPS, G = 1280, 720, 30, 2.0          # G = GUI scale (GUI is 640x360)
gw, gh = W / G, H / G
SND = '/home/claude/the-watcher/src/main/resources/assets/thewatcher/sounds'; SR = 44100
FONT = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf', 54)
MID = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf', 30)
SMALL = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf', 22)
R = random.Random(11)
U = {'wwide': 36, 'wclose': 12, 'fp2': 36, 'fp4': 22, 'fp5': 14, 'fmove': 9, 'cr': 17, 'hrun': 30, 'hbite': 8}
ST = {}
for f in glob.glob('/tmp/jsr/*.png'):
    m = re.match(r'.*/(\w+?)_(\d+)\.png', f); ST.setdefault(m.group(1), {})[int(m.group(2))] = np.asarray(Image.open(f))
def still(name, frame):
    fr = ST[name]; k = min(fr, key=lambda x: abs(x - frame) if x <= frame else 999 + x); return fr[k], U[name]

class Frame:
    def __init__(s): s.img = np.zeros((H, W, 3), np.float32)
    def creature(s, name, frame, cx, cy, zoom, roll):
        im, u = still(name, frame); sh_, sw_ = im.shape[:2]
        sc = (H * zoom / 16) / (sh_ / u)                                 # output px per still px
        th = math.radians(roll); c, sn = math.cos(th), math.sin(th)
        X0, Y0 = cx * G, cy * G
        a, b = c / sc, sn / sc; d, e = -sn / sc, c / sc
        data = (a, b, sw_ / 2 - a * X0 - b * Y0, d, e, sh_ / 2 - d * X0 - e * Y0)
        out = Image.fromarray(im).transform((W, H), Image.Transform.AFFINE, data, Image.Resampling.BICUBIC, fillcolor=(0, 0, 0))
        o = np.asarray(out).astype(np.float32) / 255
        m = o.max(-1, keepdims=True) > 0.01
        s.img = np.where(m, o, s.img)
    def tint(s, alpha, rgb, sl=np.s_[:, :]):
        s.img[sl] = s.img[sl] * (1 - alpha) + np.array(rgb, np.float32) / 255 * alpha
    def rect(s, x0, y0, x1, y1, alpha, rgb):
        x0, x1 = int(max(0, x0 * G)), int(min(W, x1 * G)); y0, y1 = int(max(0, y0 * G)), int(min(H, y1 * G))
        if x1 > x0 and y1 > y0: s.tint(alpha, rgb, np.s_[y0:y1, x0:x1])
    def disc(s, cx, cy, r, alpha, rgb):
        r = max(0.5, r); yy, xx = np.ogrid[:H, :W]
        x0, x1 = int(max(0, (cx - r) * G)), int(min(W, (cx + r) * G + 1)); y0, y1 = int(max(0, (cy - r) * G)), int(min(H, (cy + r) * G + 1))
        if x1 <= x0 or y1 <= y0: return
        sub = s.img[y0:y1, x0:x1]; m = ((xx[:, x0:x1] / G - cx) ** 2 + (yy[y0:y1] / G - cy) ** 2) <= r * r
        sub[m] = sub[m] * (1 - alpha) + np.array(rgb, np.float32) / 255 * alpha
    def scanlines(s, n):
        for _ in range(n):
            y = R.randrange(int(gh)); s.rect(0, y, gw, y + 1, R.randrange(70) / 255, (255, 255, 255) if R.random() < .5 else (0, 0, 0))
    def grain(s, n):
        ys = np.array([R.randrange(H) for _ in range(n)]); xs = np.array([R.randrange(W) for _ in range(n)])
        for x, y in zip(xs, ys): s.tint((20 + R.randrange(50)) / 255, (255, 255, 255) if R.random() < .5 else (0, 0, 0), np.s_[y:y + 2, x:x + 2])
    def vignette(s, strength=0xD0 / 255):
        g = np.linspace(strength, 0, H // 3)[:, None, None]; s.img[:H // 3] *= 1 - g; s.img[H - H // 3:] *= 1 - g[::-1]
        side = W // 4; x = np.arange(side); a = strength * (1 - x / side) ** 2
        s.img[:, :side] *= (1 - a)[None, :, None]; s.img[:, W - side:] *= (1 - a[::-1])[None, :, None]
    def out(s): return (np.clip(s.img, 0, 1) * 255).astype(np.uint8)

def shake(a): return (R.random() * 2 - 1) * a
def A(x): return max(0.0, min(1.0, x))

drops, slashes, dust = [], [], []
def splatter(tick, n):
    for _ in range(n):
        x, y, rad = R.random() * 1.7, R.random() * 0.75, 0.014 + R.random() * 0.04
        drops.append([x, y + rad * .3, rad * .55, tick, 0.004 + R.random() * 0.01, 0.12 + R.random() * 0.5])
        for _ in range(6): drops.append([x + (R.random() - .5) * rad, y + (R.random() - .5) * rad, rad * (.45 + R.random() * .4), tick, 0, 0])
        for _ in range(3 + R.randrange(4)):
            ang = R.random() * math.pi * 2; L = rad * (2 + R.random() * 2.5)
            for j in range(6):
                u = (j + 1) / 6; drops.append([x + math.cos(ang) * L * u, y + math.sin(ang) * L * u, rad * (.32 - .26 * u) * (.8 + R.random() * .4), tick, 0, 0])
def gashes(tick):
    x, y, dx, dy = 0.12 + R.random() * 0.2, 0.04, 0.32 + R.random() * 0.14, 0.85
    for i in range(3): slashes.append([x + i * .11, y + i * .02, x + i * .11 + dx, y + i * .02 + dy, tick, 0.022 + R.random() * 0.01])
def draw_drops(F, time):
    for d in drops:
        age = time - d[3]
        if age < 0: continue
        x = d[0] * gh - (1.7 * gh - gw) / 2; y = d[1] * gh; rad = d[2] * gh * min(1, 0.4 + age * 0.6)
        run = min(d[5], d[4] * age) * gh
        if run > 1:
            rw = max(1, rad * 0.35); F.rect(x - rw, y, x + rw, y + run, 0xE8 / 255, (0x50, 0, 0)); F.disc(x, y + run, rw * 1.3, 0xEE / 255, (0x5A, 2, 2))
        F.disc(x, y, rad, 0xE0 / 255, (0x40, 0, 0)); F.disc(x, y, rad * .72, 0xF0 / 255, (0x70, 8, 8))
        if rad > 4: F.disc(x - rad * .3, y - rad * .35, max(1, rad * .15), 0x70 / 255, (255, 0x90, 0x90))
def draw_gashes(F, time):
    for g in slashes:
        grow = min(1, (time - g[4]) / 3)
        if grow <= 0: continue
        x0, y0, x1, y1 = g[0] * gw, g[1] * gh, g[2] * gw, g[3] * gh; L = math.hypot(x1 - x0, y1 - y0)
        for k, a_, col in [(1, 0xF0 / 255, (0x20, 0, 0)), (.6, 1, (0x9A, 0x10, 0x10)), (.22, 1, (0xD0, 0x3A, 0x30))]:
            for i in range(int(L * grow)):
                u = i / L; px, py = x0 + (x1 - x0) * u, y0 + (y1 - y0) * u; th = (g[5] * gh * math.sin(math.pi * u) + 1) * k
                q = max(1, th); F.rect(px - q, py - q * .5, px + q, py + q * .5, a_, col)
def draw_dust(F, time):
    for d in dust:
        y = (d[1] + time * d[2] * d[3] * 2.2) * gh
        if 0 <= y <= gh: x = d[0] * gw + math.sin(time * .3 + d[0] * 20) * 3; F.rect(x, y, x + 1, y + 2, 0x90 / 255, (0xA0, 0x98, 0x88))

def scene_watcher(F, time):
    if time < 12:
        k = time / 12; F.creature('wwide', 0, gw / 2 + shake(.6), gh * .55, .5 + .15 * k, -4 + 3 * k); F.tint(.93 - .45 * k, (0, 0, 0)); return
    s = time - 12; zoom = 2.2 + .5 * min(1, s / 30); sh = 2 + 18 * math.exp(-s / 6)
    tw = int(s) % 7 == 5; tx = shake(gw * .04) if tw else 0
    F.creature('wclose', 4 + s, gw / 2 + shake(sh) + tx, gh * .5 + shake(sh), zoom, shake(3) + (8 if tw else 0))
    F.tint(.12 + .15 * abs(math.sin(s * 1.7)), (0x70, 0, 0))
    if s < 1.5: F.tint(.85 * (1 - s / 1.5), (255, 255, 255))
    F.scanlines(14)
def scene_faceless(F, time):
    for c0, c1, z, xo, roll, nm in [(8, 11, .55, -.18, 0, 'fp2'), (18, 21, .95, .12, 22, 'fp4'), (27, 30, 1.5, -.05, -35, 'fp5')]:
        if c0 <= time < c1:
            F.creature(nm, 0, gw * (.5 + xo), gh * .5, z, roll); F.tint(.35, (0x0A, 0x14, 0x28))
            if time - c0 < 1: F.tint(.45, (255, 255, 255))
            F.scanlines(10); return
    if time < 34: F.scanlines(6); return
    s = time - 34; roll = 0 if s < 3 else 78 * min(1, (s - 3) / 2.2)
    sh = 3 + 14 * math.exp(-s / 5) + (16 if 3 < s < 6 else 0)
    F.creature('fmove', s, gw / 2 + shake(sh), gh * .5 + shake(sh), 1.9 + .3 * min(1, s / 20), roll + shake(2))
    F.tint(.3, (0x0A, 0x14, 0x28))
    if s < 1.5: F.tint(.9 * (1 - s / 1.5), (255, 255, 255))
    F.scanlines(18)
def scene_crawler(F, time):
    draw_dust(F, time)
    if time < 7: return
    if time < 15:
        s = time - 7; target, start = gh * .45, -gh * .7
        y = target - (target - start) * math.exp(-s * .55) * math.cos(s * 1.1); x, zoom, roll = gw / 2, 1.05, 6 * math.sin(s * 2)
    else:
        step = int((time - 15) / 4); sr = random.Random(step * 7919 + 13)
        x = gw * (.5 + (sr.random() - .5) * .3); y = gh * (.47 + (sr.random() - .5) * .18); zoom = 1.35 + .4 * step; roll = (sr.random() - .5) * 50
    F.creature('cr', time % 30, x + shake(3), y + shake(3), zoom, roll)
    F.tint(.3, (0x1C, 0x24, 0x08))
    if 7 <= time < 8: F.tint(.6, (255, 255, 255))
    F.scanlines(10)
def scene_hound(F, time):
    if time < 24:
        k = time / 24; zoom = .12 + 1.0 * k ** 2.2; bob = math.sin(time * 1.3) * gh * .035 * zoom
        F.creature('hrun', (time * 1.0) % 24, gw / 2 + math.sin(time * .4) * gw * .03, gh * .55 + bob, zoom, math.sin(time * .65) * 4)
        F.tint(.55 - .3 * k, (0x1A, 6, 3)); F.scanlines(6); return
    s = time - 24; jolt = gh * .08 * math.exp(-s / 3); sh = 3 + 16 * math.exp(-s / 5)
    F.creature('hbite', 2 + s, gw / 2 + shake(sh), gh * .5 + jolt + shake(sh), 2.5 + .3 * min(1, s / 20), shake(4))
    F.tint(.18 + .18 * abs(math.sin(s * 2.1)), (0x6A, 0, 0))
    if s < 1.2: F.tint(.65 * (1 - s / 1.2), (0xB0, 0, 0))
    F.scanlines(16)

#            name, title, tagline, ticks, scene, sound cues (tick, file, pitch, vol), setup
SCENES = [
    ("THE WATCHER", "turn around", 48, scene_watcher,
     [(1, 'breathing', .85, .9), (12, 'jumpscare', 1, 1), (12, 'watcher_scream1', 1, 1), (12, 'static', 1, .5)], lambda: splatter(12, 9)),
    ("THE FACELESS", "don't blink", 56, scene_faceless,
     [(1, 'faceless_hum', .8, .8), (8, 'glitch', 1.3, .45), (8, 'tick', .6, .8), (18, 'glitch', 1.3, .45), (18, 'tick', .6, .8),
      (27, 'glitch', 1.3, .45), (27, 'tick', .6, .8), (34, 'jumpscare', .9, 1), (34, 'faceless_hum', 1.6, 1), (34, 'static', .8, .6),
      (37, 'bone_crack', .8, 1), (39, 'bone_crack', .6, 1)], lambda: None),
    ("THE CRAWLER", "from above", 44, scene_crawler,
     [(1, 'crawler_skitter1', .8, .8), (7, 'jumpscare', 1.15, 1), (7, 'crawler_chitter', 1, 1)] + [(t, 'crawler_skitter2', 1.35, .55) for t in range(15, 44, 4)]
     + [(23, 'crawler_chitter', 1.35, 1)],
     lambda: dust.extend([[R.random(), -R.random() * .6, .002 + R.random() * .006, .6 + R.random()] for _ in range(60)])),
    ("THE HOLLOW HOUND", "the charge", 50, scene_hound,
     [(1, 'hound_growl1', .8, .8)] + [(t, 'heartbeat', 1.7, .35 + .65 * t / 24) for t in range(5, 24, 5)]
     + [(24, 'jumpscare', 1, 1), (24, 'hound_growl2', 1.35, 1), (24, 'bone_crack', 1, 1)], lambda: splatter(24, 8)),
]
GASH_AT = {3: 26}

_cache = {}
def decode(name, pitch):
    if name not in _cache:
        raw = subprocess.run(['ffmpeg', '-loglevel', 'error', '-i', f'{SND}/{name}.ogg', '-f', 's16le', '-ac', '1', '-ar', str(SR), '-'], capture_output=True).stdout
        _cache[name] = np.frombuffer(raw, '<i2') / 32768.0
    x = _cache[name]; return np.interp(np.arange(0, len(x), pitch), np.arange(len(x)), x) if pitch != 1 else x

def text_frame(lines, alpha=1.0):
    im = Image.new('RGB', (W, H)); d = ImageDraw.Draw(im)
    for txt, font, y, col in lines:
        w = d.textlength(txt, font=font); d.text(((W - w) / 2, y), txt, font=font, fill=tuple(int(v * alpha) for v in col))
    return np.asarray(im)

frames, audio = [], np.zeros(SR * 60)
def add(f, n=1): frames.extend([f] * n)
n = int(2.4 * FPS)
for i in range(n):
    a = min(1, i / 15, (n - i) / 15); add(text_frame([("THE WATCHER", FONT, 290, (170, 20, 20)), ("four jumpscares", SMALL, 370, (150, 150, 150))], a))
for k, (title, tag, ticks, fn, cues, setup) in enumerate(SCENES):
    drops.clear(); slashes.clear(); dust.clear(); setup()
    n = int(1.6 * FPS)
    for i in range(n):
        a = min(1, i / 10, (n - i) / 10); add(text_frame([(title, MID, 315, (150, 25, 25)), (f'"{tag}"', SMALL, 365, (120, 120, 120))], a))
    start = len(frames) / FPS
    for tk, nm, p, v in cues:
        x = decode(nm, p) * v; i0 = int((start + tk / 20) * SR); audio[i0:i0 + len(x)] += x[:len(audio) - i0]
    nf = int(ticks / 20 * FPS)
    for i in range(nf):
        time = i * 20 / FPS
        if k == 3 and time >= 26 and not slashes: gashes(26)
        F = Frame(); fn(F, time)
        draw_drops(F, time); draw_gashes(F, time); F.grain(260); F.vignette()
        t = time / ticks
        if t > .86: F.img *= 1 - min(1, (t - .86) / .14)
        add(F.out())
    add(np.zeros((H, W, 3), np.uint8), int(1.6 * FPS))
total = len(frames) / FPS
audio = np.tanh(audio[:int(total * SR)] * 1.1) * .97
open('/tmp/js2_audio.raw', 'wb').write((audio * 32767).astype('<i2').tobytes())
p = subprocess.Popen(['ffmpeg', '-loglevel', 'error', '-y', '-f', 'rawvideo', '-pix_fmt', 'rgb24', '-s', f'{W}x{H}', '-r', str(FPS), '-i', '-',
                      '-f', 's16le', '-ar', str(SR), '-ac', '1', '-i', '/tmp/js2_audio.raw', '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '18',
                      '-pix_fmt', 'yuv420p', '-c:a', 'aac', '-b:a', '192k', '-shortest', '/home/claude/the-watcher-jumpscares-v2.mp4'], stdin=subprocess.PIPE)
for f in frames: p.stdin.write(f.tobytes())
p.stdin.close(); p.wait(); print('done', round(total, 1), 's', len(frames), 'frames')
