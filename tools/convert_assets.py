#!/usr/bin/env python3
"""
Converts the TACZ addon "CS2 Knives for LR Tactical" (by grinchhs) into the compact
runtime format used by this client-only mod.

usage: python3 convert_assets.py <path/to/cs2knives-*.jar> <path/to/src/main/resources>

Output (under assets/cs2knifemenu/):
  knives.json             index of knives (id, name, model, texture, icon, sounds)
  knives/<model>.bin      mesh + bones + animations (binary, see KnifeData.java)
  textures/knife/<id>.png knife textures      textures/icon/<id>.png menu icons
  sounds/knife/*.ogg + sounds.json
  lang/en_us.json
"""
import sys, os, json, zipfile, struct, shutil, io, re

NS = 'cs2knifemenu'
jar_path, out_res = sys.argv[1], sys.argv[2]
z = zipfile.ZipFile(jar_path)
names = set(z.namelist())
A = 'assets/cs2knives/'
O = os.path.join(out_res, 'assets', NS)
shutil.rmtree(O, ignore_errors=True)
for d in ('knives', 'textures/knife', 'textures/icon', 'sounds/knife', 'lang'):
    os.makedirs(os.path.join(O, d), exist_ok=True)

def jload(n): return json.loads(z.read(n).decode('utf-8'))

class W:
    def __init__(s): s.b = io.BytesIO()
    def i(s, v): s.b.write(struct.pack('>i', v))
    def s16(s, v): s.b.write(struct.pack('>h', v))
    def f(s, v): s.b.write(struct.pack('>f', float(v)))
    def u8(s, v): s.b.write(struct.pack('>B', v))
    def utf(s, v):
        e = v.encode('utf-8'); s.b.write(struct.pack('>H', len(e))); s.b.write(e)
    def fs(s, vs):
        for v in vs: s.f(v)

# ---------- mesh -------------------------------------------------------------
SKIP_BONES = {'righthand_pos', 'lefthand_pos'}   # arm placeholder cubes
def mesh_for_bone(b):
    """returns (verts[list of 8-float tuples], indices[list]) or None"""
    pm = b.get('poly_mesh')
    if not pm or b['name'] in SKIP_BONES: return None
    P, N, UV = pm['positions'], pm['normals'], pm['uvs']
    verts, index, idx = [], {}, []
    def vid(k):
        key = (k[0], k[1], k[2])
        if key not in index:
            p, n, uv = P[k[0]], N[k[1]], UV[k[2]]
            index[key] = len(verts)
            verts.append((p[0], p[1], p[2], n[0], n[1], n[2], uv[0], 1.0 - uv[1]))  # v flipped (verified visually)
        return index[key]
    # indices come in groups of 4 (quads). Triangles repeat their last vertex; n-gons are fanned.
    for poly in pm['polys']:
        ids = [vid(v) for v in poly]
        if len(ids) == 4: idx += ids
        elif len(ids) == 3: idx += [ids[0], ids[1], ids[2], ids[2]]
        else:
            for k in range(1, len(ids) - 1): idx += [ids[0], ids[k], ids[k + 1], ids[k + 1]]
    return verts, idx

# ---------- animations ---------------------------------------------------------
CH = {'rotation': 0, 'position': 1, 'scale': 2}
def vec(v):
    if isinstance(v, (int, float)): return [float(v)] * 3
    return [float(x) for x in v]
def keyframes(ch):
    """-> list of (t, pre, post, smooth)"""
    if not isinstance(ch, dict): v = vec(ch); return [(0.0, v, v, 0)]
    out = []
    for t, kf in ch.items():
        if isinstance(kf, dict):
            post = vec(kf.get('post', kf.get('pre')))
            pre = vec(kf.get('pre', kf.get('post')))
            sm = 1 if kf.get('lerp_mode') == 'catmullrom' else 0
        else:
            pre = post = vec(kf); sm = 0
        out.append((float(t), pre, post, sm))
    out.sort(key=lambda k: k[0])
    return out

def write_model(model, anim_name):
    g = jload(A + f'geo_models/melee/{model}_geo.json')['minecraft:geometry'][0]
    bones = g['bones']
    bidx = {b['name']: i for i, b in enumerate(bones)}
    w = W(); w.i(0x434B4E32)
    w.s16(len(bones))
    for b in bones:
        w.utf(b['name']); w.s16(bidx.get(b.get('parent'), -1))
        w.fs(b.get('pivot', [0, 0, 0])); w.fs(b.get('rotation', [0, 0, 0]))
        m = mesh_for_bone(b)
        if not m: w.i(0); w.i(0); continue
        verts, idx = m
        w.i(len(verts)); w.i(len(idx))
        for v in verts: w.fs(v)
        for k in idx: w.i(k)
    anim = jload(A + f'animations/melee/{anim_name}.animation.json')['animations']
    sounds = set()
    usable = {n: a for n, a in anim.items() if 'animation_length' in a}
    w.s16(len(usable))
    for n, a in usable.items():
        w.utf(n); w.f(a['animation_length'])
        se = []
        for t, e in (a.get('sound_effects') or {}).items():
            eff = e.get('effect') if isinstance(e, dict) else None
            if eff:
                s = 'knife.' + eff.split('/')[-1]; sounds.add(s); se.append((float(t), s))
        se.sort()
        w.s16(len(se))
        for t, s in se: w.f(t); w.utf(s)
        chans = []
        for bn, bd in (a.get('bones') or {}).items():
            if bn not in bidx: continue
            for cn, cv in bd.items():
                if cn in CH: chans.append((bidx[bn], CH[cn], keyframes(cv)))
        w.s16(len(chans))
        for bi, c, kfs in chans:
            w.s16(bi); w.u8(c); w.s16(len(kfs))
            for t, pre, post, sm in kfs:
                w.f(t); w.fs(pre); w.fs(post); w.u8(sm)
    open(os.path.join(O, 'knives', model + '.bin'), 'wb').write(w.b.getvalue())
    return sounds

# ---------- index --------------------------------------------------------------
lang = jload(A + 'lang/en_us.json')
disp_files = sorted(n for n in names if n.startswith(A + 'display/melee/') and n.endswith('.json'))
all_sounds, done_models, knives = set(), set(), []
for df in disp_files:
    kid = os.path.basename(df)[:-5]
    if kid.startswith('push_tp'): continue            # third-person helper models only
    d = jload(df)
    model = d['model'].split('/')[-1].replace('_geo', '')
    anim = d['animation'].split('/')[-1]
    tex = d['texture'].split('/')[-1]
    idx = jload('data/cs2knives/index/melee/%s.json' % kid)
    raw = lang.get(idx['name'], kid)
    color = re.match(r'§(.)', raw)
    clean = re.sub(r'§.', '', raw).replace(' Knife', '').strip()
    if model not in done_models:
        all_sounds |= write_model(model, anim); done_models.add(model)
    shutil.copy(io.BytesIO(z.read(A + f'textures/melee/{tex}.png')), os.path.join(O, 'textures/knife', kid + '.png')) \
        if False else open(os.path.join(O, 'textures/knife', kid + '.png'), 'wb').write(z.read(A + f'textures/melee/{tex}.png'))
    icon = A + f'textures/melee/slot/{kid}.png'
    has_icon = icon in names
    iw = ih = 0
    if has_icon:
        raw_icon = z.read(icon)
        open(os.path.join(O, 'textures/icon', kid + '.png'), 'wb').write(raw_icon)
        iw, ih = struct.unpack('>II', raw_icon[16:24])   # PNG IHDR width/height
    knives.append({'id': kid, 'name': clean, 'model': model, 'icon': has_icon, 'iw': iw, 'ih': ih,
                   'color': color.group(1) if color else None,
                   'skin': bool(color), 'sort': idx.get('sort', 99)})
knives.sort(key=lambda k: (k['skin'], k['sort'], k['name']))
json.dump({'knives': knives}, open(os.path.join(O, 'knives.json'), 'w'), indent=1)

# ---------- sounds -------------------------------------------------------------
sj = {}
for s in sorted(all_sounds):
    src = A + 'tacz_sounds/melee/knife/' + s.split('.', 1)[1] + '.ogg'
    if src not in names: print('missing sound', src); continue
    open(os.path.join(O, 'sounds/knife', s.split('.', 1)[1] + '.ogg'), 'wb').write(z.read(src))
    sj[s] = {'sounds': [{'name': f'{NS}:knife/' + s.split('.', 1)[1], 'stream': False}]}
json.dump(sj, open(os.path.join(O, 'sounds.json'), 'w'), indent=1)

json.dump({f'knife.{k["id"]}': k['name'] for k in knives} | {
    'key.categories.cs2knifemenu': 'CS2 Knife Menu'},
    open(os.path.join(O, 'lang/en_us.json'), 'w'), indent=1)
print(len(knives), 'knives,', len(done_models), 'models,', len(sj), 'sounds')
