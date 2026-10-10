from pathlib import Path
import json, math, random, subprocess, uuid, wave, struct, zlib

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/ia747"
OBJECTS = ASSETS / "objects"
SOUNDS = ASSETS / "sounds"
TEXTURES = ASSETS / "textures/entity"
for p in (OBJECTS, SOUNDS, TEXTURES):
    p.mkdir(parents=True, exist_ok=True)

# ---------------------------------------------------------------------------
# Audio: keep the proven v0.9 sound architecture.
# ---------------------------------------------------------------------------
SR = 22050

def clamp(v):
    return max(-1.0, min(1.0, v))

def make_ogg(name, duration, fn):
    wav = SOUNDS / (name + ".wav")
    ogg = SOUNDS / (name + ".ogg")
    n = int(SR * duration)
    values = [fn(i / SR) for i in range(n)]
    peak = max(0.001, max(abs(v) for v in values))
    gain = 0.90 / peak
    pcm = bytearray()
    for value in values:
        q = int(clamp(value * gain) * 32767)
        pcm += q.to_bytes(2, "little", signed=True)
    with wave.open(str(wav), "wb") as f:
        f.setnchannels(1); f.setsampwidth(2); f.setframerate(SR)
        f.writeframes(bytes(pcm))
    subprocess.run(["ffmpeg","-y","-loglevel","error","-i",str(wav),"-c:a","libvorbis","-q:a","3",str(ogg)], check=True)
    wav.unlink()

def harmonic_set(values, seed):
    r = random.Random(seed)
    return [(freq, amp, r.random() * math.tau) for freq, amp in values]

def harmonics(t, parts):
    return sum(amp * math.sin(math.tau * freq * t + phase) for freq, amp, phase in parts)

idle = harmonic_set([(42,.46),(84,.24),(126,.12),(210,.10),(315,.07),(420,.045),(630,.025)],7471)
thrust = harmonic_set([(50,.38),(75,.27),(100,.22),(125,.18),(175,.15),(225,.13),(300,.12),(400,.105),(550,.085),(700,.065),(900,.048),(1150,.032),(1400,.020),(1600,.012)],7472)
inside = harmonic_set([(38,.55),(55,.34),(76,.26),(110,.18),(145,.13),(190,.09),(260,.055),(360,.03)],7473)
distant = harmonic_set([(28,.58),(42,.36),(56,.25),(84,.16),(112,.10),(168,.06),(240,.025)],7474)
whine = harmonic_set([(1200,.14),(1450,.11),(1700,.085),(1950,.060),(2250,.040),(2550,.022)],7475)

make_ogg("jet_idle",4.0,lambda t:harmonics(t,idle)*(0.985+0.015*math.sin(math.pi*t)))
make_ogg("jet_thrust",4.0,lambda t:harmonics(t,thrust)*(0.94+0.06*math.sin(math.tau*1.5*t)))
make_ogg("jet_inside",4.0,lambda t:harmonics(t,inside)*(0.97+0.03*math.sin(math.tau*.75*t)))
make_ogg("jet_distant",4.0,lambda t:harmonics(t,distant)*(0.98+0.02*math.sin(math.tau*.5*t)))
make_ogg("jet_whine",4.0,lambda t:harmonics(t,whine)*(0.96+0.04*math.sin(math.tau*2*t)))

def startup(t):
    ramp=min(1.0,t/2.4); fade=min(1.0,max(0.0,(3.4-t)/.25))
    low=.26*math.sin(math.tau*42*t)+.10*math.sin(math.tau*84*t)
    phase=math.tau*(85*t+85*t*t)
    return ramp*fade*(low+.16*math.sin(phase)+.06*math.sin(2*phase+.4))

def shutdown(t):
    env=math.exp(-1.20*t); phase=math.tau*(360*t-42*t*t)
    return env*(.20*math.sin(phase)+.10*math.sin(.5*phase)+.24*math.sin(math.tau*42*t))

make_ogg("jet_start",3.4,startup)
make_ogg("jet_stop",3.0,shutdown)
make_ogg("jet_silent",.25,lambda t:0.0)

# ---------------------------------------------------------------------------
# Original texture atlas: pearl white + deep navy + metals + cabin materials.
# No external/copyrighted texture assets are used.
# ---------------------------------------------------------------------------
W=H=128
palette = {
    "pearl": (232,236,240,255),
    "pearl2": (204,212,220,255),
    "navy": (18,31,58,255),
    "navy2": (31,52,88,255),
    "metal": (150,156,162,255),
    "darkmetal": (54,58,64,255),
    "black": (18,20,23,255),
    "glass": (50,83,105,255),
    "seat": (49,61,77,255),
    "tan": (174,153,118,255),
    "red": (142,31,34,255),
    "green": (37,120,73,255),
    "warm": (224,199,138,255),
    "silver": (188,194,201,255),
    "floor": (47,49,52,255),
    "screen": (35,134,154,255),
}
names=list(palette)
cell=32
pixels=bytearray(W*H*4)
for idx,name in enumerate(names):
    x0=(idx%4)*cell; y0=(idx//4)*cell
    col=palette[name]
    for y in range(y0,min(H,y0+cell)):
        for x in range(x0,min(W,x0+cell)):
            o=(y*W+x)*4
            # tiny deterministic variation gives pearl/metal surfaces less toy-flat appearance
            delta=((x+y+idx*7)%5)-2
            pixels[o:o+4]=bytes((max(0,min(255,col[0]+delta)),max(0,min(255,col[1]+delta)),max(0,min(255,col[2]+delta)),col[3]))

def png_chunk(tag,data):
    return struct.pack(">I",len(data))+tag+data+struct.pack(">I",zlib.crc32(tag+data)&0xffffffff)

raw=b"".join(b"\x00"+bytes(pixels[y*W*4:(y+1)*W*4]) for y in range(H))
png=b"\x89PNG\r\n\x1a\n"+png_chunk(b"IHDR",struct.pack(">IIBBBBB",W,H,8,6,0,0,0))+png_chunk(b"IDAT",zlib.compress(raw,9))+png_chunk(b"IEND",b"")
(TEXTURES/"boeing_747_400_texture.png").write_bytes(png)

patch={}
for idx,name in enumerate(names):
    x=(idx%4)*cell; y=(idx//4)*cell
    patch[name]=[x,y,x+cell,y+cell]

# ---------------------------------------------------------------------------
# BBModel helpers. 1 Minecraft block = 1 metre. BBModel units = 1/16 block.
# ---------------------------------------------------------------------------
S=16.0
elements=[]
outliner=[]

def uid(): return str(uuid.uuid4())
def U(v): return v*S

def uv_faces(material):
    uv=patch[material]
    return {s:{"uv":uv,"texture":0} for s in ("north","east","south","west","up","down")}

def cube_m(name, fr, to, material="pearl", rotation=None, origin=None):
    ident=uid()
    e={"name":name,"box_uv":False,"rescale":False,"locked":False,
       "render_order":"default","allow_mirror_modeling":True,
       "from":[U(v) for v in fr],"to":[U(v) for v in to],"autouv":0,"color":0,
       "origin":[U(v) for v in (origin or (0,0,0))],"faces":uv_faces(material),
       "type":"cube","uuid":ident}
    if rotation is not None: e["rotation"]=list(rotation)
    elements.append(e); outliner.append(ident)

def mesh_raw(name, positions_m, quads, material="pearl"):
    ident=uid()
    vertices={f"v{i}":[U(v) for v in p] for i,p in enumerate(positions_m)}
    uv=patch[material]; faces={}
    for fi,q in enumerate(quads):
        ids=[f"v{i}" for i in q]
        faces[f"f{fi}"]={"uv":{
            ids[0]:[uv[0],uv[1]],ids[1]:[uv[0],uv[3]],
            ids[2]:[uv[2],uv[3]],ids[3]:[uv[2],uv[1]]
        },"vertices":ids,"texture":0}
    elements.append({"name":name,"color":0,"origin":[0,0,0],"rotation":[0,0,0],
                     "export":True,"visibility":True,"locked":False,
                     "render_order":"default","allow_mirror_modeling":True,
                     "vertices":vertices,"faces":faces,"type":"mesh","uuid":ident})
    outliner.append(ident)

def loft_filtered(name, stations, segments, material, include, omit=None):
    pos=[]
    for z,cy,rx,ry in stations:
        for j in range(segments):
            a=math.tau*j/segments
            pos.append((rx*math.cos(a),cy+ry*math.sin(a),z))
    q=[]
    for i in range(len(stations)-1):
        z0,cy0,rx0,ry0=stations[i]; z1,cy1,rx1,ry1=stations[i+1]
        for j in range(segments):
            j2=(j+1)%segments
            a=math.tau*(j+.5)/segments
            zmid=(z0+z1)*.5; cym=(cy0+cy1)*.5; rxm=(rx0+rx1)*.5; rym=(ry0+ry1)*.5
            xmid=rxm*math.cos(a); ymid=cym+rym*math.sin(a)
            if not include(zmid,xmid,ymid): continue
            if omit and omit(zmid,xmid,ymid): continue
            a0=i*segments+j; a1=i*segments+j2; b1=(i+1)*segments+j2; b0=(i+1)*segments+j
            q.append((a0,a1,b1,b0))
    mesh_raw(name,pos,q,material)

# ---------------------------------------------------------------------------
# 747-400 1:1 reference envelope
# length 70.66 m, span 64.44 m, height 19.41 m.
# Coordinate convention: +Z nose, -Z tail, X wing span, Y up from ground.
# ---------------------------------------------------------------------------
NOSE=35.33
TAIL=-35.33
FUSE_Y=7.35
FUSE_RX=3.25
FUSE_RY=3.30

stations=[
    (-35.33,7.35,.25,.35),(-34.7,7.35,.9,1.0),(-33.3,7.35,1.8,2.0),(-31.2,7.35,2.7,2.8),
    (-28.5,7.35,3.18,3.25),(-24.0,7.35,3.25,3.30),(-16.0,7.35,3.25,3.30),
    (-6.0,7.35,3.25,3.30),(6.0,7.35,3.25,3.30),(16.0,7.35,3.25,3.30),(23.5,7.35,3.25,3.30),
    (27.0,7.30,3.20,3.22),(29.3,7.18,3.05,3.05),(31.1,6.98,2.78,2.78),
    (32.5,6.72,2.40,2.45),(33.55,6.45,1.95,2.05),(34.35,6.18,1.45,1.58),
    (34.92,5.98,.95,1.08),(35.33,5.86,.42,.52)
]

def shell_omit(z,x,y):
    # Main deck side window strip; crown remains closed.
    if -27.0 < z < 25.7 and 7.20 < y < 8.18 and abs(x) > 3.05:
        return True
    # Real cockpit windshield opening. This is the key to a true first-person view:
    # the full aircraft can remain rendered because the pilot looks through an actual hole.
    if 28.7 < z < 33.7 and 8.35 < y < 10.05 and abs(x) < 2.85:
        return True
    return False

# Pearl upper shell / navy lower belly share the same vertices, so the transition is seamless.
loft_filtered("fuselage_pearl_1to1",stations,40,"pearl",lambda z,x,y:y>=5.85,shell_omit)
loft_filtered("fuselage_navy_belly_1to1",stations,40,"navy",lambda z,x,y:y<5.85,shell_omit)

# Close nose/tail tips.
mesh_raw("radome_cap_1to1",[(-.38,5.45,35.34),(.38,5.45,35.34),(.38,6.28,35.34),(-.38,6.28,35.34)],[(0,1,2,3)],"pearl2")
mesh_raw("apu_tail_cap_1to1",[(-.18,7.05,-35.34),(.18,7.05,-35.34),(.18,7.65,-35.34),(-.18,7.65,-35.34)],[(0,1,2,3)],"darkmetal")

# Main deck window frames around the continuous transparent glass strip.
for side in (-1,1):
    x0=side*3.255
    # lower/upper sill strips seal the fuselage around windows
    cube_m("main_window_lower_sill",((x0-.04 if side<0 else x0-.02),6.90,-27.2),(x0+.02 if side<0 else x0+.04,7.22,25.9),"pearl2")
    cube_m("main_window_upper_sill",((x0-.04 if side<0 else x0-.02),8.15,-27.2),(x0+.02 if side<0 else x0+.04,8.50,25.9),"pearl")
    z=-26.6
    while z<25.7:
        cube_m("main_window_pillar",((x0-.045 if side<0 else x0-.02),7.18,z),(x0+.02 if side<0 else x0+.045,8.19,z+.13),"pearl2")
        z+=.82

# Signature upper-deck hump: continuous curve buried into main fuselage at both ends.
hump=[
    (15.0,10.38,.45,.18),(16.2,10.55,1.35,.45),(17.7,10.88,2.25,.88),(19.3,11.25,2.72,1.30),
    (21.0,11.55,2.92,1.58),(23.0,11.70,3.00,1.72),(25.2,11.72,2.95,1.72),
    (27.1,11.60,2.75,1.55),(28.6,11.35,2.35,1.18),(29.7,10.95,1.75,.72),(30.5,10.62,.75,.25)
]
def hump_omit(z,x,y):
    return 18.5<z<28.4 and 11.25<y<12.05 and abs(x)>2.45
loft_filtered("upper_deck_hump_1to1",hump,36,"pearl",lambda z,x,y:True,hump_omit)
for side in (-1,1):
    x0=side*2.92
    z=19.0
    while z<28.25:
        cube_m("upper_window_pillar",((x0-.04 if side<0 else x0-.02),11.22,z),(x0+.02 if side<0 else x0+.04,12.05,z+.12),"pearl2")
        z+=.78

# Navy cheatline below the windows, slightly proud of the skin to avoid z fighting.
for side in (-1,1):
    x=side*3.285
    cube_m("navy_cheatline",((x-.025 if side<0 else x-.015),6.62,-27.2),(x+.015 if side<0 else x+.025,6.88,27.2),"navy2")

# Wings: realistic full span 64.44 m, thin tapered mesh, swept trailing geometry.
def wing(name,side):
    s=-1 if side<0 else 1
    pts=[
        (3.0*s,7.10,8.1),(3.0*s,7.05,-7.8),
        (12.0*s,6.82,5.0),(12.0*s,6.68,-10.2),
        (22.0*s,6.38,1.0),(22.0*s,6.18,-13.3),
        (32.22*s,6.05,-4.8),(32.22*s,5.88,-15.2),
        (3.0*s,6.70,8.1),(3.0*s,6.65,-7.8),
        (12.0*s,6.48,5.0),(12.0*s,6.34,-10.2),
        (22.0*s,6.08,1.0),(22.0*s,5.90,-13.3),
        (32.22*s,5.84,-4.8),(32.22*s,5.70,-15.2)
    ]
    q=[(0,2,3,1),(2,4,5,3),(4,6,7,5),(9,11,10,8),(11,13,12,10),(13,15,14,12),
       (0,8,10,2),(2,10,12,4),(4,12,14,6),(1,3,11,9),(3,5,13,11),(5,7,15,13),(6,14,15,7)]
    mesh_raw(name,pts,q,"pearl2")
wing("left_wing_1to1",-1); wing("right_wing_1to1",1)

# Wing root fairings provide the smooth aircraft-like transition requested by the user.
def fairing(name,side):
    s=-1 if side<0 else 1; seg=20
    st=[(3.0*s,7.0,4.6,1.65),(4.5*s,6.85,4.2,1.4),(6.3*s,6.65,3.3,1.05),(8.0*s,6.50,2.1,.72)]
    pos=[]
    for x,cy,rz,ry in st:
        for j in range(seg):
            a=math.tau*j/seg; pos.append((x,cy+ry*math.sin(a),-.7+rz*math.cos(a)))
    q=[]
    for i in range(len(st)-1):
        for j in range(seg):
            j2=(j+1)%seg; q.append((i*seg+j,i*seg+j2,(i+1)*seg+j2,(i+1)*seg+j))
    mesh_raw(name,pos,q,"pearl2")
fairing("left_wing_body_fairing_1to1",-1); fairing("right_wing_body_fairing_1to1",1)

# 747-400 winglets.
for side in (-1,1):
    x=side*31.9
    cube_m("winglet",((x-.20 if side<0 else x-.05),5.85,-15.15),(x+.05 if side<0 else x+.20,8.55,-14.55),"pearl",[0,0,10*side],[x,5.9,-14.8])
    cube_m("winglet_navy",((x-.21 if side<0 else x-.04),7.65,-15.16),(x+.04 if side<0 else x+.21,8.62,-14.54),"navy",[0,0,10*side],[x,5.9,-14.8])

# Tailplanes and vertical stabilizer.
def hstab(name,side):
    s=-1 if side<0 else 1
    pts=[(2.0*s,10.0,-28.0),(2.0*s,10.0,-34.0),(13.0*s,9.75,-30.2),(13.0*s,9.75,-35.0),
         (2.0*s,9.65,-28.0),(2.0*s,9.65,-34.0),(13.0*s,9.48,-30.2),(13.0*s,9.48,-35.0)]
    mesh_raw(name,pts,[(0,2,3,1),(5,7,6,4),(0,4,6,2),(1,3,7,5),(2,6,7,3)],"pearl2")
hstab("left_hstab_1to1",-1); hstab("right_hstab_1to1",1)
vpts=[(-.42,9.2,-32.4),(.42,9.2,-32.4),(-.32,15.1,-30.5),(.32,15.1,-30.5),(-.16,19.41,-27.2),(.16,19.41,-27.2),
       (-.42,8.9,-32.4),(.42,8.9,-32.4),(-.32,14.8,-30.5),(.32,14.8,-30.5),(-.16,19.10,-27.2),(.16,19.10,-27.2)]
vq=[(0,2,3,1),(2,4,5,3),(7,9,8,6),(9,11,10,8),(0,6,8,2),(2,8,10,4),(1,3,9,7),(3,5,11,9),(4,10,11,5)]
mesh_raw("vertical_tail_1to1",vpts,vq,"navy")

# CF6-80C2-inspired engines. Fan diameter 2.362 m, overall core/nacelle length ~4.1 m.
def engine(prefix,x,y,z):
    seg=32
    # Pylon
    cube_m(prefix+"_pylon",(x-.32,y+.95,z-1.0),(x+.32,y+2.35,z+1.05),"metal",[-9,0,0],[x,y+1.4,z])
    # Outer nacelle smooth loft
    st=[(-2.05,.72),(-1.82,.91),(-1.20,1.10),(.55,1.20),(1.45,1.18),(2.05,1.08)]
    pos=[]
    for dz,r in st:
        for j in range(seg):
            a=math.tau*j/seg; pos.append((x+r*math.cos(a),y+r*math.sin(a),z+dz))
    q=[]
    for i in range(len(st)-1):
        for j in range(seg):
            j2=(j+1)%seg; q.append((i*seg+j,i*seg+j2,(i+1)*seg+j2,(i+1)*seg+j))
    mesh_raw(prefix+"_cf6_nacelle",pos,q,"pearl")
    # Metallic intake lip ring, radius close to 1.18m.
    pos=[]
    for r,dz in ((1.18,2.08),(.93,1.91)):
        for j in range(seg):
            a=math.tau*j/seg; pos.append((x+r*math.cos(a),y+r*math.sin(a),z+dz))
    q=[(j,(j+1)%seg,seg+(j+1)%seg,seg+j) for j in range(seg)]
    mesh_raw(prefix+"_intake_lip",pos,q,"silver")
    # Dark intake duct
    pos=[]
    for r,dz in ((.93,1.91),(.88,1.08)):
        for j in range(seg):
            a=math.tau*j/seg; pos.append((x+r*math.cos(a),y+r*math.sin(a),z+dz))
    mesh_raw(prefix+"_intake_duct",pos,q,"darkmetal")
    # Rear turbine/nozzle ring + exhaust cone so rear view is never hollow.
    pos=[]
    for r,dz in ((.73,-2.02),(.58,-2.24)):
        for j in range(seg):
            a=math.tau*j/seg; pos.append((x+r*math.cos(a),y+r*math.sin(a),z+dz))
    mesh_raw(prefix+"_rear_nozzle",pos,q,"darkmetal")
    # central exhaust plug
    cone=[(x-.26,y-.26,z-2.25),(x+.26,y-.26,z-2.25),(x+.26,y+.26,z-2.25),(x-.26,y+.26,z-2.25),
          (x-.10,y-.10,z-2.65),(x+.10,y-.10,z-2.65),(x+.10,y+.10,z-2.65),(x-.10,y+.10,z-2.65)]
    mesh_raw(prefix+"_exhaust_plug",cone,[(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7),(4,5,6,7)],"metal")

engines=[("eng1",-19.3,4.75,-3.2),("eng2",-9.2,4.95,.2),("eng3",9.2,4.95,.2),("eng4",19.3,4.75,-3.2)]
for e in engines: engine(*e)

# Doors: actual proportions, integrated on the side skin.
for side in (-1,1):
    x=side*3.29
    for i,z in enumerate((25.0,12.5,-8.0,-24.0),1):
        cube_m(f"door_{'L' if side<0 else 'R'}{i}",((x-.045 if side<0 else x-.015),6.25,z-.48),(x+.015 if side<0 else x+.045,8.85,z+.48),"pearl2")
        cube_m("door_handle",((x-.06 if side<0 else x-.01),7.28,z+.24),(x+.01 if side<0 else x+.06,7.38,z+.42),"darkmetal")
# Upper-deck forward doors
for side in (-1,1):
    x=side*2.93
    cube_m("upper_door",((x-.04 if side<0 else x-.015),10.55,23.5),(x+.015 if side<0 else x+.04,12.55,24.35),"pearl2")

# Lower cargo doors on right side.
cube_m("cargo_door_forward",(3.24,5.35,8.5),(3.31,6.65,14.3),"pearl2")
cube_m("cargo_door_aft",(3.24,5.35,-20.0),(3.31,6.65,-14.0),"pearl2")

# ---------------------------------------------------------------------------
# Interior: real cockpit/cabin exists in the SAME full model used by 1st/3rd person.
# ---------------------------------------------------------------------------
# Main cabin floors/ceiling
cube_m("main_cabin_floor",(-2.85,5.35,-27.0),(2.85,5.48,25.8),"floor")
cube_m("main_cabin_ceiling",(-2.80,9.48,-27.0),(2.80,9.62,25.0),"pearl2")
cube_m("upper_cabin_floor",(-2.25,10.25,16.6),(2.25,10.38,28.7),"floor")
cube_m("upper_cabin_ceiling",(-2.00,12.72,18.0),(2.00,12.84,27.4),"pearl2")

# Cabin wall liners behind transparent windows; dark lower strip gives depth.
for side in (-1,1):
    x=side*3.04
    cube_m("cabin_side_liner",((x-.03 if side<0 else x-.01),6.0,-27.0),(x+.01 if side<0 else x+.03,9.2,25.8),"pearl2")

# Cabin seats as representative rows visible through windows, not hundreds of heavy entities.
for z in (-22,-15,-8,-1,6,13,20):
    for x in (-2.15,-1.35,1.35,2.15):
        cube_m("seat_base",(x-.28,5.48,z-.28),(x+.28,5.78,z+.28),"seat")
        cube_m("seat_back",(x-.30,5.72,z-.18),(x+.30,6.62,z+.04),"seat",[-6,0,0],[x,5.72,z])

# Flight deck is physically placed behind the real windshield opening.
cube_m("cockpit_floor",(-2.45,8.55,25.8),(2.45,8.68,32.8),"floor")
cube_m("cockpit_rear_bulkhead",(-2.50,8.60,25.7),(2.50,11.9,25.85),"pearl2")
cube_m("instrument_panel",(-2.25,9.18,29.15),(2.25,9.78,30.25),"darkmetal",[-9,0,0],[0,9.25,29.2])
cube_m("glare_shield",(-2.35,9.73,29.05),(2.35,9.91,30.15),"black",[-7,0,0],[0,9.7,29.1])
cube_m("center_pedestal",(-.48,8.70,26.7),(.48,9.35,29.05),"darkmetal")
cube_m("overhead_panel",(-1.75,11.28,27.0),(1.75,11.45,29.6),"darkmetal",[8,0,0],[0,11.3,28.2])
# windshield frames around the actual hole, leaving forward center unobstructed
for x in (-2.45,2.45):
    cube_m("cockpit_a_pillar",(x-.08,9.55,29.0),(x+.08,11.25,31.4),"darkmetal",[-8,0,-8 if x<0 else 8],[x,9.6,29.2])
cube_m("cockpit_center_post",(-.07,9.62,30.0),(.07,11.18,31.7),"darkmetal",[-6,0,0],[0,9.65,30.0])
cube_m("cockpit_lower_sill",(-2.55,9.43,29.65),(2.55,9.58,31.25),"pearl2",[-5,0,0],[0,9.45,29.7])
# pilot / copilot seats
for x in (-.82,.82):
    cube_m("pilot_seat_base",(x-.38,8.70,26.7),(x+.38,9.10,27.55),"seat")
    cube_m("pilot_seat_back",(x-.40,9.05,26.65),(x+.40,10.25,27.05),"seat",[-5,0,0],[x,9.05,26.8])
# instrument screens
for x in (-1.55,-.75,.05,.85,1.55):
    cube_m("cockpit_screen",(x-.27,9.58,29.42),(x+.27,9.94,29.49),"screen",[-8,0,0],[x,9.6,29.4])

# Antennas/pitots.
for z in (3.0,-8.0,-19.0):
    cube_m("top_antenna",(-.08,10.55,z-.18),(.08,11.18,z+.18),"pearl2",[-18,0,0],[0,10.55,z])
for side in (-1,1):
    x=side*2.75
    cube_m("pitot",(x-.06,7.45,31.1),(x+.06,7.58,33.2),"metal",[0,6*side,0],[x,7.5,31.1])

model={
    "meta":{"format_version":"4.10","model_format":"free","box_uv":False},
    "name":"boeing_747_400_v21_1to1","model_identifier":"",
    "visible_box":[1,1,0],"variable_placeholders":"","variable_placeholder_buttons":[],
    "timeline_setups":[],"unhandled_root_fields":{},"resolution":{"width":128,"height":128},
    "elements":elements,"outliner":outliner,
    "textures":[{
        "path":"boeing_747_400_texture.png","name":"boeing_747_400_texture.png","folder":"","namespace":"","id":"0",
        "width":128,"height":128,"uv_width":128,"uv_height":128,"particle":False,"layers_enabled":False,
        "sync_to_project":"","render_mode":"default","render_sides":"auto","frame_time":1,"frame_order_type":"loop",
        "frame_order":"","frame_interpolate":False,"visible":True,"internal":False,"saved":True,"uuid":uid(),
        "relative_path":"boeing_747_400_texture.png"
    }]
}
(OBJECTS/"boeing_747_400.bbmodel").write_text(json.dumps(model,separators=(",",":")),encoding="utf-8")

print("V2.1 1:1 BBModel elements:",len(elements))
print("Envelope target: 70.66m length / 64.44m span / 19.41m height")
print("Generated original jet sounds:",len(list(SOUNDS.glob("*.ogg"))))
