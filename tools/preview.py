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
pts=[];cols=[]
for bn,b in bones.items():
    pm=b.get('poly_mesh')
    if not pm: continue
    P=np.array(pm['positions']);UV=np.array(pm['uvs'])
    for poly in pm['polys']:
        for v in poly:
            pp=world(bn,P[v[0]]);u,vv=UV[v[2]]
            if vflip: vv=1-vv
            c=im[min(H-1,max(0,int(vv*H)))][min(W-1,max(0,int(u*W)))]
            pts.append(pp);cols.append(c)
pts=np.array(pts);cols=np.array(cols)
print(name,'bounds',pts.min(0).round(1),pts.max(0).round(1))
# side view: z horizontal (forward=-z to the left), y up
img=np.full((500,900,3),30,np.uint8)
for (x,y,z),c in zip(pts,cols):
    px=int(450+(z)*18);py=int(300-(y-10)*18)
    if 0<=px<900 and 0<=py<500: img[py,px]=c
Image.fromarray(img).save(f'/tmp/prev_{name}_{vflip}.png')
