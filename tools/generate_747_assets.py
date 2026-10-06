from pathlib import Path
import base64, json, math, random, subprocess, uuid, wave

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/ia747"
OBJECTS = ASSETS / "objects"
SOUNDS = ASSETS / "sounds"
TEXTURES = ASSETS / "textures/entity"
for p in (OBJECTS, SOUNDS, TEXTURES):
    p.mkdir(parents=True, exist_ok=True)

SR = 22050
TEXTURE_B64 = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAA4ElEQVR4nO3WIQoCURSF4XkyyZW4AME9WCcLgmAVm2BQsE2zGDSJSzBMMghjNBmMNkURq02tpjOgyAXv/9Xzhne58A4TjufrMxJOl5uKo6SfyTzaT2XcbLVlvsi2Mk/rD5k3RkuZl2TqAAuwHsAaC7AewBoLsB7AWryrVeWBblqR+WFQlvnqPi8cIs83hWd+JTa7+c2w1wmffpusv7vb/RNgAdYDWGMB1gNYYwHWAwAwFcaTmf7ZB/DP6ADANzoA8I0OAHyjAwDf6ADANzoA8I0OAHyjAwDf6ADANzoA8O0FMPswSkOB8wsAAAAASUVORK5CYII="
(TEXTURES / "boeing_747_400_texture.png").write_bytes(base64.b64decode(TEXTURE_B64))

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
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(SR)
        f.writeframes(bytes(pcm))
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav),
         "-c:a", "libvorbis", "-q:a", "3", str(ogg)],
        check=True
    )
    wav.unlink()

def harmonic_set(values, seed):
    r = random.Random(seed)
    return [(freq, amp, r.random() * math.tau) for freq, amp in values]

def harmonics(t, parts):
    return sum(amp * math.sin(math.tau * freq * t + phase) for freq, amp, phase in parts)

idle = harmonic_set([
    (42, .46), (84, .24), (126, .12), (210, .10),
    (315, .07), (420, .045), (630, .025)
], 7471)

# Broad high-power roar: much denser low/mid spectrum than idle.
thrust = harmonic_set([
    (50, .38), (75, .27), (100, .22), (125, .18),
    (175, .15), (225, .13), (300, .12), (400, .105),
    (550, .085), (700, .065), (900, .048), (1150, .032),
    (1400, .020), (1600, .012)
], 7472)

# Interior is intentionally low-passed: mostly rumble and structure-borne components.
inside = harmonic_set([
    (38, .55), (55, .34), (76, .26), (110, .18),
    (145, .13), (190, .09), (260, .055), (360, .03)
], 7473)

# Distant fly-by is even more low-frequency dominant.
distant = harmonic_set([
    (28, .58), (42, .36), (56, .25), (84, .16),
    (112, .10), (168, .06), (240, .025)
], 7474)

# Exterior-only compressor/fan tone. Kept subtle in the mixer.
whine = harmonic_set([
    (1200, .14), (1450, .11), (1700, .085),
    (1950, .060), (2250, .040), (2550, .022)
], 7475)

make_ogg("jet_idle", 4.0, lambda t: harmonics(t, idle) * (0.985 + 0.015 * math.sin(math.pi*t)))
make_ogg("jet_thrust", 4.0, lambda t: harmonics(t, thrust) * (0.94 + 0.06 * math.sin(math.tau*1.5*t)))
make_ogg("jet_inside", 4.0, lambda t: harmonics(t, inside) * (0.97 + 0.03 * math.sin(math.tau*0.75*t)))
make_ogg("jet_distant", 4.0, lambda t: harmonics(t, distant) * (0.98 + 0.02 * math.sin(math.tau*0.5*t)))
make_ogg("jet_whine", 4.0, lambda t: harmonics(t, whine) * (0.96 + 0.04 * math.sin(math.tau*2.0*t)))

def startup(t):
    ramp = min(1.0, t / 2.4)
    fade = min(1.0, max(0.0, (3.4 - t) / 0.25))
    low = .26 * math.sin(math.tau*42*t) + .10 * math.sin(math.tau*84*t)
    # Rising spool tone, deliberately less piercing than v0.5.
    phase = math.tau * (85*t + 85*t*t)
    spool = .16 * math.sin(phase) + .06 * math.sin(2*phase + .4)
    return ramp * fade * (low + spool)

def shutdown(t):
    env = math.exp(-1.20*t)
    phase = math.tau * (360*t - 42*t*t)
    return env * (.20*math.sin(phase) + .10*math.sin(.5*phase) + .24*math.sin(math.tau*42*t))

make_ogg("jet_start", 3.4, startup)
make_ogg("jet_stop", 3.0, shutdown)
make_ogg("jet_silent", .25, lambda t: 0.0)

patch = {
    "body":[0,0,8,8], "body2":[8,0,16,8], "blue":[16,0,24,8],
    "dark":[24,0,32,8], "metal":[32,0,40,8], "seat":[40,0,48,8],
    "tan":[48,0,56,8], "black":[56,0,64,8],
    "red":[0,8,8,16], "green":[8,8,16,16],
    "yellow":[16,8,24,16], "silver":[24,8,32,16]
}
def uid():
    return str(uuid.uuid4())

def face_map(material):
    uv = patch[material]
    return {side:{"uv":uv,"texture":0} for side in ("north","east","south","west","up","down")}

elements = []
outliner = []

def cube(name, fr, to, material="body", rotation=None, origin=None):
    ident = uid()
    e = {
        "name":name, "box_uv":False, "rescale":False, "locked":False,
        "render_order":"default", "allow_mirror_modeling":True,
        "from":list(fr), "to":list(to), "autouv":0, "color":0,
        "origin":list(origin or [0,0,0]), "faces":face_map(material),
        "type":"cube", "uuid":ident
    }
    if rotation is not None:
        e["rotation"] = list(rotation)
    elements.append(e)
    outliner.append(ident)


def mesh(name, positions, quad_indices, material="body"):
    ident = uid()
    vertices = {f"v{i}": list(v) for i, v in enumerate(positions)}
    uv = patch[material]
    faces = {}
    for fi, quad in enumerate(quad_indices):
        ids = [f"v{i}" for i in quad]
        faces[f"f{fi}"] = {
            "uv": {
                ids[0]: [uv[0], uv[1]],
                ids[1]: [uv[0], uv[3]],
                ids[2]: [uv[2], uv[3]],
                ids[3]: [uv[2], uv[1]],
            },
            "vertices": ids,
            "texture": 0
        }
    elements.append({
        "name": name,
        "color": 0,
        "origin": [0,0,0],
        "rotation": [0,0,0],
        "export": True,
        "visibility": True,
        "locked": False,
        "render_order": "default",
        "allow_mirror_modeling": True,
        "vertices": vertices,
        "faces": faces,
        "type": "mesh",
        "uuid": ident
    })
    outliner.append(ident)

def loft_z(name, stations, segments=24, material="body", omit=None):
    positions = []
    for z, cy, rx, ry in stations:
        for j in range(segments):
            a = math.tau * j / segments
            positions.append((rx * math.cos(a), cy + ry * math.sin(a), z))
    quads = []
    for i in range(len(stations)-1):
        z0, cy0, rx0, ry0 = stations[i]
        z1, cy1, rx1, ry1 = stations[i+1]
        for j in range(segments):
            j2 = (j + 1) % segments
            a = math.tau * (j + 0.5) / segments
            zmid = (z0 + z1) * 0.5
            cymid = (cy0 + cy1) * 0.5
            rymid = (ry0 + ry1) * 0.5
            ymid = cymid + rymid * math.sin(a)
            xside = abs(math.cos(a))
            if omit and omit(zmid, ymid, xside):
                continue
            a0 = i*segments + j
            a1 = i*segments + j2
            b1 = (i+1)*segments + j2
            b0 = (i+1)*segments + j
            quads.append((a0,a1,b1,b0))
    mesh(name, positions, quads, material)

def wing_mesh(name, side):
    s = -1 if side < 0 else 1
    pts = [
        (20*s,34,44),(20*s,34,-28),
        (82*s,31,18),(82*s,31,-42),
        (160*s,29,-8),(160*s,29,-53),
        (20*s,29,44),(20*s,29,-28),
        (82*s,28,18),(82*s,28,-42),
        (160*s,27,-8),(160*s,27,-53),
    ]
    q = [
        (0,2,3,1),(2,4,5,3),
        (7,9,8,6),(9,11,10,8),
        (0,6,8,2),(2,8,10,4),
        (1,3,9,7),(3,5,11,9),(4,10,11,5)
    ]
    mesh(name, pts, q, "body2")

def hstab_mesh(name, side):
    s = -1 if side < 0 else 1
    pts = [
        (8*s,51,-132),(8*s,51,-160),(72*s,49,-142),(72*s,49,-166),
        (8*s,47,-132),(8*s,47,-160),(72*s,47,-142),(72*s,47,-166)
    ]
    q=[(0,2,3,1),(5,7,6,4),(0,4,6,2),(1,3,7,5),(2,6,7,3)]
    mesh(name, pts, q, "body2")

def nacelle_mesh(prefix, x, y, z):
    seg = 20
    stations = [(-21,7.2),(-17,8.2),(-9,10.2),(8,11.0),(15,10.7),(20,10.0)]
    positions = []
    for dz,r in stations:
        for j in range(seg):
            a = math.tau*j/seg
            positions.append((x+r*math.cos(a), y+r*math.sin(a), z+dz))
    q = []
    for i in range(len(stations)-1):
        for j in range(seg):
            j2=(j+1)%seg
            q.append((i*seg+j,i*seg+j2,(i+1)*seg+j2,(i+1)*seg+j))
    mesh(prefix+"_nacelle",positions,q,"body")

    positions = []
    outer, inner = 10.0, 7.4
    for radius,dz in ((outer,20.0),(inner,19.2)):
        for j in range(seg):
            a=math.tau*j/seg
            positions.append((x+radius*math.cos(a),y+radius*math.sin(a),z+dz))
    q=[]
    for j in range(seg):
        j2=(j+1)%seg
        q.append((j,j2,seg+j2,seg+j))
    mesh(prefix+"_intake_lip",positions,q,"dark")

    positions=[]
    for radius,dz in ((7.4,19.2),(7.1,12.0)):
        for j in range(seg):
            a=math.tau*j/seg
            positions.append((x+radius*math.cos(a),y+radius*math.sin(a),z+dz))
    q=[]
    for j in range(seg):
        j2=(j+1)%seg
        q.append((j,j2,seg+j2,seg+j))
    mesh(prefix+"_intake_duct",positions,q,"dark")

def wing_root_fairing(name, side):
    s=-1 if side<0 else 1
    stations=[
        (27*s,36.5,17,18),
        (34*s,35.5,15,15),
        (43*s,34.0,11,11),
        (52*s,32.5,6,7)
    ]
    seg=16
    positions=[]
    for x,cy,rz,ry in stations:
        for j in range(seg):
            a=math.tau*j/seg
            positions.append((x,cy+ry*math.sin(a),-3+rz*math.cos(a)))
    q=[]
    for i in range(len(stations)-1):
        for j in range(seg):
            j2=(j+1)%seg
            q.append((i*seg+j,i*seg+j2,(i+1)*seg+j2,(i+1)*seg+j))
    mesh(name,positions,q,"body2")

# Smooth polygon-mesh airframe.
def main_window_opening(zmid, ymid, xside):
    # Only remove the near-vertical side strip. This prevents window holes
    # from creeping onto the crown and becoming visible from above.
    return (-120 < zmid < 120) and (39.0 < ymid < 48.5) and (xside > 0.94)

fuselage_stations = [
    # Tail cone.
    (-172,36.5,2.5,3.5),
    (-167,36.5,8.0,9.0),
    (-158,36.5,15.5,16.5),
    (-146,36.5,22.0,21.0),
    (-128,36.5,27.0,24.0),

    # Constant-section wide body.
    (-104,36.5,28.0,24.5),
    (0,36.5,28.0,24.5),
    (100,36.5,28.0,24.5),
    (116,36.5,28.0,24.5),

    # 747-style nose: broad shoulders, then a rounded radome that drops
    # slightly toward the tip instead of converging to a sharp cone.
    (126,36.4,27.6,24.1),
    (136,36.1,26.0,22.8),
    (145,35.7,23.5,20.7),
    (153,35.2,20.0,17.6),
    (160,34.6,15.8,14.0),
    (165,34.0,11.5,10.3),
    (169,33.5,7.5,7.0),
    (171.5,33.2,4.6,4.6),
]
loft_z("fuselage_smooth", fuselage_stations, 32, "body", main_window_opening)

# Small rounded-looking radome face closes the otherwise open loft end.
mesh(
    "radome_tip_cap",
    [(-3.2,30.0,171.6),(3.2,30.0,171.6),(3.2,36.4,171.6),(-3.2,36.4,171.6)],
    [(0,1,2,3)],
    "body2"
)

for side in (-1,1):
    xa, xb = ((-28.8,-27.4) if side < 0 else (27.4,28.8))
    for z in range(-120,121,15):
        cube("window_pillar",(xa,38,z-2),(xb,49,z+2),"body2")
    outer_a, outer_b = ((-29.1,-28.3) if side < 0 else (28.3,29.1))
    cube("cheatline",(outer_a,35,-120),(outer_b,39,120),"blue")

def upper_window_opening(zmid, ymid, xside):
    return (48 < zmid < 119) and (67.2 < ymid < 73.0) and (xside > 0.93)

upper_stations = [
    # Both ends taper down into the main fuselage so the hump has no open seam.
    (22,60.5,2.8,1.2),
    (30,61.0,8.5,2.8),
    (40,64.0,16.5,6.0),
    (52,68.0,21.5,10.0),
    (66,69.5,23.0,11.5),
    (103,69.5,23.0,11.5),
    (118,69.0,21.5,10.5),
    (129,67.0,17.0,7.5),
    (138,63.0,8.0,3.0),
    (144,60.8,2.8,1.2),
]
loft_z("upper_deck_smooth", upper_stations, 28, "body", upper_window_opening)

for side in (-1,1):
    xa, xb = ((-23.8,-22.5) if side < 0 else (22.5,23.8))
    for z in (48,62,76,90,104,118):
        cube("upper_pillar",(xa,67,z-2),(xb,73,z+2),"body2")

wing_mesh("wing_left_smooth",-1)
wing_mesh("wing_right_smooth",1)
wing_root_fairing("wing_root_fairing_left",-1)
wing_root_fairing("wing_root_fairing_right",1)

cube("winglet_l",(-160,28,-55),(-154,52,-45),"body",[0,0,10],[-157,29,-50])
cube("winglet_l_blue",(-160,45,-55),(-154,53,-45),"blue",[0,0,10],[-157,29,-50])
cube("winglet_r",(154,28,-55),(160,52,-45),"body",[0,0,-10],[157,29,-50])
cube("winglet_r_blue",(154,45,-55),(160,53,-45),"blue",[0,0,-10],[157,29,-50])

hstab_mesh("hstab_left_smooth",-1)
hstab_mesh("hstab_right_smooth",1)

vpts=[
    (-5,49,-166),(5,49,-166),(-4,94,-151),(4,94,-151),(-2,114,-143),(2,114,-143),
    (-5,47,-166),(5,47,-166),(-4,92,-151),(4,92,-151),(-2,112,-143),(2,112,-143)
]
vq=[
    (0,2,3,1),(2,4,5,3),
    (7,9,8,6),(9,11,10,8),
    (0,6,8,2),(2,8,10,4),
    (1,3,9,7),(3,5,11,9),(4,10,11,5)
]
mesh("vertical_tail_smooth",vpts,vq,"body")
cube("vstab_blue",(-4,88,-159),(4,111,-144),"blue",[-8,0,0],[0,88,-151])

for prefix,x,y,z in (
    ("eng1",-100,12,-25),
    ("eng2",-55,13,-10),
    ("eng3",55,13,-10),
    ("eng4",100,12,-25),
):
    cube(prefix+"_pylon",(x-5,y+14,z-9),(x+5,y+34,z+10),"metal",[-10,0,0],[x,y+20,z])
    nacelle_mesh(prefix,x,y,z)

# Passenger doors, upper-deck doors, cargo doors and handles.
for side in (-1,1):
    xa, xb = ((-29.6,-28.2) if side < 0 else (28.2,29.6))
    for i,z in enumerate((100,42,-38,-98),1):
        cube("door_"+("L" if side<0 else "R")+str(i),(xa,23,z-6),(xb,52,z+6),"body2")
        hx1,hx2 = ((-30.1,-29.6) if side<0 else (29.6,30.1))
        cube("door_handle",(hx1,35,z+2),(hx2,38,z+5),"dark")
    ux1,ux2 = ((-24,-22.5) if side<0 else (22.5,24))
    cube("upper_door",(ux1,63,93),(ux2,76,103),"body2")

cube("cargo_door_1",(28.2,22,15),(29.8,36,48),"body2")
cube("cargo_door_2",(28.2,22,-83),(29.8,36,-48),"body2")
for z in (15,48,-83,-48):
    cube("cargo_frame",(29.8,22,z-1),(30.3,36,z+1),"dark")

# Visible interior.
cube("cabin_floor",(-23,20,-115),(23,22,118),"dark")
cube("aisle",(-4,22,-112),(4,22.8,112),"tan")
cube("cabin_ceiling",(-21,54,-112),(21,56,112),"body2")

# Secondary inner roof skins sit just under the exterior mesh. They are not
# visible from normal side views, but prevent sky/top-down views from seeing
# through microscopic mesh/window seams into an empty cabin.
cube("main_cabin_inner_roof",(-22.8,56.0,-122),(22.8,58.2,122),"body2")
cube("upper_cabin_inner_roof",(-17.8,75.0,42),(17.8,77.3,124),"body2")
cube("hump_aft_blend_liner",(-17.0,57.0,20),(17.0,61.5,48),"body2")
cube("hump_forward_blend_liner",(-15.0,57.0,120),(15.0,61.8,145),"body2")
for z in (-72,-24,24,72):
    for x in (-15,-8,8,15):
        cube("seat_base",(x-3,22,z-3),(x+3,27,z+3),"seat")
        cube("seat_back",(x-3,27,z-2),(x+3,39,z+1),"seat",[-6,0,0],[x,27,z])
for x in (-8,8):
    cube("pilot_seat",(x-4,23,132),(x+4,37,140),"seat")
cube("cockpit_console",(-15,23,145),(15,36,154),"dark",[-10,0,0],[0,24,145])

# Antennas, pitot tubes and flap-track fairings.
for z in (25,-25,-75):
    cube("antenna",(-2,61,z-3),(2,72,z+3),"body2",[-18,0,0],[0,61,z])
for z in (55,-55):
    cube("belly_antenna",(-2,8,z-3),(2,13,z+3),"dark",[18,0,0],[0,13,z])
cube("pitot_l",(-26,38,151),(-20,40,166),"metal",[0,-8,0],[-23,39,151])
cube("pitot_r",(20,38,151),(26,40,166),"metal",[0,8,0],[23,39,151])
for x in (-115,-75,-35,35,75,115):
    cube("flap_fairing",(x-3,23,-43),(x+3,29,-18),"body2")

model = {
    "meta":{"format_version":"4.10","model_format":"free","box_uv":False},
    "name":"boeing_747_400", "model_identifier":"",
    "visible_box":[1,1,0], "variable_placeholders":"",
    "variable_placeholder_buttons":[], "timeline_setups":[],
    "unhandled_root_fields":{}, "resolution":{"width":64,"height":64},
    "elements":elements, "outliner":outliner,
    "textures":[{
        "path":"boeing_747_400_texture.png",
        "name":"boeing_747_400_texture.png",
        "folder":"","namespace":"","id":"0",
        "width":64,"height":64,"uv_width":64,"uv_height":64,
        "particle":False,"layers_enabled":False,"sync_to_project":"",
        "render_mode":"default","render_sides":"auto","frame_time":1,
        "frame_order_type":"loop","frame_order":"","frame_interpolate":False,
        "visible":True,"internal":False,"saved":True,"uuid":uid(),
        "relative_path":"boeing_747_400_texture.png"
    }]
}
(OBJECTS / "boeing_747_400.bbmodel").write_text(
    json.dumps(model, separators=(",",":")), encoding="utf-8"
)

print("Generated original 747 BBModel elements:", len(elements))
print("Generated original jet sounds:", len(list(SOUNDS.glob("*.ogg"))))
print("Generated external texture:", TEXTURES / "boeing_747_400_texture.png")
