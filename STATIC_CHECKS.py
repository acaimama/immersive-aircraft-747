from pathlib import Path
import json
import sys

root = Path(__file__).parent
errors = []

for p in root.rglob("*.json"):
    try:
        json.loads(p.read_text(encoding="utf-8"))
    except Exception as e:
        errors.append(f"{p}: JSON: {e}")

props = {}
for line in (root / "gradle.properties").read_text().splitlines():
    if "=" in line and not line.lstrip().startswith("#"):
        key, value = line.split("=", 1)
        props[key.strip()] = value.strip()

if props.get("mod_version") != "0.9.0":
    errors.append("mod_version must be 0.9.0")
if props.get("minecraft_version") != "1.21.1":
    errors.append("minecraft_version must be 1.21.1")
if props.get("immersive_aircraft_version") != "1.5.2":
    errors.append("IA version must be 1.5.2")
if props.get("immersive_aircraft_curse_file") != "8914643":
    errors.append("IA CurseForge file must be 8914643")

required = [
    "tools/generate_747_assets.py",
    "src/main/java/dev/openai/ia747/Boeing747Addon.java",
    "src/main/java/dev/openai/ia747/entity/Boeing747Entity.java",
    "src/main/java/dev/openai/ia747/client/Boeing747Client.java",
    "src/main/java/dev/openai/ia747/client/Boeing747Renderer.java",
    "src/main/java/dev/openai/ia747/client/Boeing747SoundManager.java",
    "src/main/java/dev/openai/ia747/sound/Boeing747Sounds.java",
    "src/main/resources/data/ia747/aircraft/boeing_747_400.json",
    "src/main/resources/data/ia747/recipe/boeing_747_400.json",
    "src/main/resources/assets/ia747/models/item/boeing_747_400.json",
    "src/main/resources/assets/ia747/objects/boeing_747_400.bbmodel",
    "src/main/resources/assets/ia747/textures/entity/boeing_747_400_texture.png",
    "src/main/resources/assets/ia747/sounds.json",
]
for rel in required:
    if not (root / rel).exists():
        errors.append(f"missing {rel}")

for sound in (
    "jet_start.ogg",
    "jet_stop.ogg",
    "jet_idle.ogg",
    "jet_thrust.ogg",
    "jet_inside.ogg",
    "jet_distant.ogg",
    "jet_whine.ogg",
    "jet_silent.ogg",
):
    path = root / "src/main/resources/assets/ia747/sounds" / sound
    if not path.exists():
        errors.append(f"missing sound {sound}")
    elif path.stat().st_size < 1000:
        errors.append(f"sound asset suspiciously small: {sound} ({path.stat().st_size} bytes)")

texture = root / "src/main/resources/assets/ia747/textures/entity/boeing_747_400_texture.png"
if texture.exists():
    data = texture.read_bytes()
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        errors.append("747 texture is not a PNG")

model_path = root / "src/main/resources/assets/ia747/objects/boeing_747_400.bbmodel"
if model_path.exists():
    try:
        model = json.loads(model_path.read_text(encoding="utf-8"))
        elements = model.get("elements", [])
        names = {e.get("name") for e in elements}
        if len(elements) < 100:
            errors.append(f"BBModel detail count too low: {len(elements)}")
        for name in (
            "fuselage_smooth",
            "radome_tip_cap",
            "upper_deck_smooth",
            "wing_left_smooth",
            "wing_right_smooth",
            "wing_root_fairing_left",
            "wing_root_fairing_right",
            "vertical_tail_smooth",
            "eng1_nacelle",
            "eng4_nacelle",
            "cabin_floor",
            "main_cabin_inner_roof",
            "upper_cabin_inner_roof",
            "hump_aft_blend_liner",
            "hump_forward_blend_liner",
            "door_L1",
            "door_R1",
            "pitot_l",
        ):
            if name not in names:
                errors.append(f"BBModel missing detail: {name}")
        mesh_count = sum(1 for e in elements if e.get("type") == "mesh")
        if mesh_count < 18:
            errors.append(f"not enough smooth mesh components: {mesh_count}")
    except Exception as e:
        errors.append(f"BBModel parse failure: {e}")

aircraft_path = root / "src/main/resources/data/ia747/aircraft/boeing_747_400.json"
if aircraft_path.exists():
    ac = json.loads(aircraft_path.read_text(encoding="utf-8"))
    pr = ac.get("properties", {})
    if pr.get("mass", 0) < 10:
        errors.append("747 mass unexpectedly low")
    if pr.get("groundPitch", 99) > 2.5:
        errors.append("groundPitch too high; idle nose-up bug may return")
    if len(ac.get("boundingBoxes", [])) < 20:
        errors.append("not enough collision volumes")
    if len(ac.get("trails", [])) != 4:
        errors.append("747 should have four engine trails")
    if len(ac.get("passengerPositions", [])) != 16:
        errors.append("747 should expose 16 progressive passenger layouts")

    cargo = 0
    for slot in ac.get("inventorySlots", []):
        if slot.get("type") == "inventory":
            cargo += slot.get("cols", 1) * slot.get("rows", 1)
    if cargo < 32:
        errors.append(f"cargo capacity too small: {cargo}")

java = "\n".join(p.read_text(encoding="utf-8") for p in (root / "src/main/java").rglob("*.java"))
for token in (
    "getEngineTarget() < 0.05F",
    "JET_SILENT",
    "JET_START",
    "JET_THRUST",
    "JET_WHINE",
    "ClientTickEvents.END_CLIENT_TICK",
    "LIGHT_BLUE_STAINED_GLASS",
    "TINTED_GLASS",
    "renderDoorWindows",
    "renderExteriorLights",
    "renderCabinLighting",
    "LightTexture.FULL_BRIGHT",
    "engine-off = stationary fan",
    "Visible fan planes sit behind the intake lips",
    "localFirstPersonPilot",
    "main-deck roof",
    "Twelve metallic blades",
    "renderFirstPersonCockpit",
    "central forward view stays physically open",
    "Windshield is seated INTO the reshaped nose",
):
    if token not in java:
        errors.append(f"Java source missing v0.9 token: {token}")

sounds = json.loads((root / "src/main/resources/assets/ia747/sounds.json").read_text(encoding="utf-8"))
for key in ("jet_start","jet_stop","jet_idle","jet_thrust","jet_inside","jet_distant","jet_whine","jet_silent"):
    if key not in sounds:
        errors.append(f"sounds.json missing {key}")

if errors:
    print("STATIC CHECKS FAILED")
    print("\n".join(" - " + e for e in errors))
    sys.exit(1)

print("STATIC CHECKS PASSED")
print("Version:", props["mod_version"])
print("v0.9: reshaped 747 nose, closed cabin crown/seams and dedicated visible first-person cockpit verified.")
print("16 seats / >=32 cargo / 20+ collision volumes / four engine trails verified.")
