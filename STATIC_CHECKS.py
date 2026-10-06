from pathlib import Path
import json, sys

root = Path(__file__).parent
errors = []

for p in root.rglob('*.json'):
    try:
        json.loads(p.read_text(encoding='utf-8'))
    except Exception as e:
        errors.append(f'{p}: JSON: {e}')

props = {}
for line in (root / 'gradle.properties').read_text().splitlines():
    if '=' in line and not line.lstrip().startswith('#'):
        k, v = line.split('=', 1)
        props[k.strip()] = v.strip()

if props.get('mod_version') != '0.4.0':
    errors.append('mod_version must be 0.4.0')
if props.get('minecraft_version') != '1.21.1':
    errors.append('minecraft_version must be 1.21.1')
if props.get('immersive_aircraft_version') != '1.5.2':
    errors.append('IA version must be 1.5.2')
if props.get('immersive_aircraft_curse_file') != '8914643':
    errors.append('IA CurseForge file must be 8914643')

mod = json.loads((root / 'src/main/resources/fabric.mod.json').read_text())
if mod['depends'].get('immersive_aircraft') != '>=1.5.2':
    errors.append('fabric.mod.json IA dependency mismatch')

required = [
    'src/main/java/dev/openai/ia747/Boeing747Addon.java',
    'src/main/java/dev/openai/ia747/entity/Boeing747Entity.java',
    'src/main/java/dev/openai/ia747/client/Boeing747Client.java',
    'src/main/java/dev/openai/ia747/client/Boeing747Renderer.java',
    'src/main/resources/data/ia747/aircraft/boeing_747_400.json',
    'src/main/resources/data/ia747/recipe/boeing_747_400.json',
    'src/main/resources/assets/ia747/lang/en_us.json',
    'src/main/resources/assets/ia747/lang/zh_cn.json',
    'src/main/resources/assets/ia747/models/item/boeing_747_400.json',
]
for rel in required:
    if not (root / rel).exists():
        errors.append(f'missing {rel}')

ac = json.loads((root / 'src/main/resources/data/ia747/aircraft/boeing_747_400.json').read_text())
pr = ac.get('properties', {})

for k in (
    'fuel', 'yawSpeed', 'pitchSpeed', 'engineSpeed', 'engineMaxSpeed',
    'pushSpeed', 'glideFactor', 'driftDrag', 'lift', 'rollFactor',
    'groundPitch', 'mass'
):
    if k not in pr:
        errors.append(f'aircraft properties missing {k}')

if pr.get('mass', 0) < 10:
    errors.append('747 mass unexpectedly low')
if pr.get('groundPitch', 99) > 2.5:
    errors.append('groundPitch too high; idle nose-up bug may return')
if len(ac.get('boundingBoxes', [])) < 20:
    errors.append('not enough collision volumes')
if len(ac.get('trails', [])) != 4:
    errors.append('747 should have four engine trails')
if len(ac.get('passengerPositions', [])) != 16:
    errors.append('747 should expose 16 progressive passenger layouts')

cargo = 0
for slot in ac.get('inventorySlots', []):
    if slot.get('type') == 'inventory':
        cargo += slot.get('cols', 1) * slot.get('rows', 1)
if cargo < 32:
    errors.append(f'cargo capacity too small: {cargo}')

java = '\n'.join(p.read_text() for p in (root / 'src/main/java').rglob('*.java'))
for token in (
    'class Boeing747Entity',
    'extends AirplaneEntity',
    'class Boeing747Renderer',
    'EntityRendererRegistry.register',
    'getEngineTarget() < 0.05F',
    'LightTexture.FULL_BRIGHT',
    'renderLandingGear',
    'renderFlaps'
):
    if token not in java:
        errors.append(f'Java source missing token: {token}')

if errors:
    print('STATIC CHECKS FAILED')
    print('\n'.join(' - ' + e for e in errors))
    sys.exit(1)

print('STATIC CHECKS PASSED')
print(
    f"v{props['mod_version']} / MC {props['minecraft_version']} / "
    f"IA {props['immersive_aircraft_version']}"
)
print(
    f"Passengers: {len(ac.get('passengerPositions', []))}; "
    f"cargo slots: {cargo}; "
    f"bounding boxes: {len(ac.get('boundingBoxes', []))}; "
    f"trails: {len(ac.get('trails', []))}; "
    f"groundPitch: {pr.get('groundPitch')}"
)
