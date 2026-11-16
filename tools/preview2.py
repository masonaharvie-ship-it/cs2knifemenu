import json, sys, math
import numpy as np
from PIL import Image
SRC='/tmp/src/assets/cs2knives'
name=sys.argv[1] if len(sys.argv)>1 else 'karambit'
vflip=int(sys.argv[2]) if len(sys.argv)>2 else 0
g=json.load(open(f'{SRC}/geo_models/melee/{name}_geo.json'))['minecraft:geometry'][0]
bones={b['name']:b for b in g['bones']}
def rotm(r):
    x,y,z=[math.radians(a) for a in r]
    cx,sx,cy,sy,cz,sz=math.cos(x),math.sin(x),math.cos(y),math.sin(y),math.cos(z),math.sin(z)
    Rx=np.array([[1,0,0],[0,cx,-sx],[0,sx,cx]]);Ry=np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]]);Rz=np.array([[cz,-sz,0],[sz,cz,0],[0,0,1]])
    return Rz@Ry@Rx
def world(bn,p):
    chain=[];b=bones[bn]
    while b: chain.append(b); b=bones.get(b.get('parent'))
    for b in chain:  # child first
        piv=np.array(b.get('pivot',[0,0,0]));R=rotm(b.get('rotation',[0,0,0]))
        p=R@(p-piv)+piv
    return p
tex=[f for f in ['_uv.png'] ]
im=np.array(Image.open(f'{SRC}/textures/melee/{name}_uv.png').convert('RGB'))
H,W=im.shape[:2]

from PIL import ImageDraw
out=[]
for vflip in (0,1):
    img=Image.new('RGB',(900,360),(30,30,30));d=ImageDraw.Draw(img)
    tris=[]
    for bn,b in bones.items():
        pm=b.get('poly_mesh')
        if not pm: continue
        P=np.array(pm['positions']);UV=np.array(pm['uvs'])
        for poly in pm['polys']:
            ws=[world(bn,P[v[0]]) for v in poly]
            uvs=np.array([UV[v[2]] for v in poly]);uv=uvs.mean(0)
            if vflip: uv[1]=1-uv[1]
            c=tuple(int(x) for x in im[min(H-1,max(0,int(uv[1]*H)))][min(W-1,max(0,int(uv[0]*W)))])
            tris.append((np.mean([w[0] for w in ws]),ws,c))
    tris.sort(key=lambda t:t[0])
    for _,ws,c in tris:
        d.polygon([(450+w[2]*22,200-(w[1]-10)*22) for w in ws],fill=c)
    out.append(img)
both=Image.new('RGB',(900,720));both.paste(out[0],(0,0));both.paste(out[1],(0,360));both.save(f'/tmp/tri_{name}.png')
