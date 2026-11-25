"""Render jumpscare stills exactly as InventoryScreen.drawEntity frames them: front-on, orthographic,
centred on the model's axis at (height/2 + FACE_Y) blocks. Output: image covering U model units vertically."""
import numpy as np, sys, os
from PIL import Image
src = open('render.py').read().split("rest=load(")[0]
src = src.replace("img[:]=np.array([0.05,0.055,0.065])*(1-gy)+np.array([0.015,0.015,0.02])*gy", "img[:]=0")
exec(src)
HB = {'watcher': 2.9, 'faceless': 2.9, 'crawler': 1.1, 'hound': 1.1}
NF = {'js_watcher': 30, 'js_crawler': 30, 'js_hound_run': 24, 'js_hound_bite': 26, 'js_faceless_move': 24,
      'js_faceless_p2': 1, 'js_faceless_p4': 1, 'js_faceless_p5': 1}
creature, anim, U, Hpx, out = sys.argv[1], sys.argv[2], float(sys.argv[3]), int(sys.argv[4]), sys.argv[5]
FYv = float(sys.argv[7])
frames = [int(f) for f in sys.argv[6].split(',')]
d = np.fromfile(f'anim_{anim}.bin', '<f4').reshape(NF[anim], -1, 4, 11)
ref = d[0][..., 0:3].reshape(-1, 3) * np.array([1, -1, 1])
ymin = ref[:, 1].min()
c = np.array([0.0, -24 + (HB[creature] / 2 + FYv) * 16, (ref[:, 2].min() + ref[:, 2].max()) / 2])
os.makedirs('/tmp/jsr', exist_ok=True)
for f in frames:
    Wpx = int(Hpx * 2)
    im, _, _ = render(d[f], 0, W=Wpx, H=Hpx, pitch_deg=0, scale=Hpx / U, center=c)
    im = np.clip(im * 1.9, 0, 1) ** 0.85
    Image.fromarray((im * 255).astype(np.uint8)).save(f'/tmp/jsr/{out}_{f:02d}.png')
print('ok', out, len(frames))
