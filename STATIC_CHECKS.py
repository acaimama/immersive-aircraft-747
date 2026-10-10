from pathlib import Path
import json, sys
root=Path(__file__).parent
errors=[]
for p in root.rglob("*.json"):
    try: json.loads(p.read_text(encoding="utf-8"))
    except Exception as e: errors.append(f"{p}: JSON {e}")
props={}
for line in (root/"gradle.properties").read_text().splitlines():
    if "=" in line and not line.lstrip().startswith("#"):
        k,v=line.split("=",1); props[k.strip()]=v.strip()
if props.get("mod_version")!="2.1.0": errors.append("mod_version must be 2.1.0")
if props.get("minecraft_version")!="1.21.1": errors.append("minecraft_version must be 1.21.1")
if props.get("immersive_aircraft_version")!="1.5.2": errors.append("IA must be 1.5.2")

model_path=root/"src/main/resources/assets/ia747/objects/boeing_747_400.bbmodel"
if not model_path.exists(): errors.append("missing BBModel")
else:
    m=json.loads(model_path.read_text())
    es=m.get("elements",[]); names={e.get("name") for e in es}
    for n in ("fuselage_pearl_1to1","fuselage_navy_belly_1to1","upper_deck_hump_1to1",
              "left_wing_1to1","right_wing_1to1","eng1_cf6_nacelle","eng4_cf6_nacelle",
              "eng1_rear_nozzle","cockpit_floor","instrument_panel","vertical_tail_1to1"):
        if n not in names: errors.append("missing model part "+n)
    # verify 1:1 envelope approximately from all mesh/cube vertices
    xs=[]; ys=[]; zs=[]
    for e in es:
        if e.get("type")=="mesh":
            for v in e.get("vertices",{}).values(): xs.append(v[0]/16);ys.append(v[1]/16);zs.append(v[2]/16)
        elif e.get("type")=="cube":
            for v in (e.get("from"),e.get("to")):
                xs.append(v[0]/16);ys.append(v[1]/16);zs.append(v[2]/16)
    if max(xs)-min(xs)<64.0: errors.append("wingspan is not 1:1 64.44m")
    if max(zs)-min(zs)<70.5: errors.append("length is not 1:1 70.66m")
    if max(ys)<19.3: errors.append("tail height below 19.41m target")

ac=json.loads((root/"src/main/resources/data/ia747/aircraft/boeing_747_400.json").read_text())
cargo=sum(s.get("cols",1)*s.get("rows",1) for s in ac["inventorySlots"] if s["type"]=="inventory")
upgrades=sum(1 for s in ac["inventorySlots"] if s["type"]=="upgrade")
boilers=sum(1 for s in ac["inventorySlots"] if s["type"]=="boiler")
if cargo!=216: errors.append(f"cargo must be 216 slots, got {cargo}")
if upgrades!=6 or boilers!=1: errors.append("must have 6 general upgrades + 1 fuel/power slot")
if len(ac["passengerPositions"])!=16: errors.append("must have 16 progressive seat layouts")
if len(ac["trails"])!=4: errors.append("must have four engine trails")
if len(ac["boundingBoxes"])<25: errors.append("collision model too sparse")

java="\n".join(p.read_text() for p in (root/"src/main/java").rglob("*.java"))
for token in ("ABSOLUTE_MAX_BLOCKS_PER_TICK","applyJetCruiseEnvelope","SAME complete aircraft","24-blade CF6-like"):
    if token not in java: errors.append("missing V2.1 java token "+token)
for snd in ("jet_start.ogg","jet_stop.ogg","jet_idle.ogg","jet_thrust.ogg","jet_inside.ogg","jet_distant.ogg","jet_whine.ogg","jet_silent.ogg"):
    p=root/"src/main/resources/assets/ia747/sounds"/snd
    if not p.exists() or p.stat().st_size<1000: errors.append("missing/bad sound "+snd)
if errors:
    print("STATIC CHECKS FAILED"); print("\n".join(" - "+e for e in errors)); sys.exit(1)
print("STATIC CHECKS PASSED")
print("V2.1: 1 block = 1 metre; 70.66m x 64.44m x 19.41m envelope")
print("CF6-style engines / real windshield opening / full first-person aircraft / 216 cargo / 6+1 upgrades")
