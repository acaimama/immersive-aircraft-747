# Boeing 747-400 exterior reboot: quality-first workflow

## Current status
- V2.1 JAR and its geometry are an **unaccepted gameplay baseline**, not the approved model.
- Exterior geometry: **not yet rebuilt/approved**; no new JAR or 747 silhouette is claimed ready.
- Existing main branch is preserved untouched.
- The user authorized fully discarding the prior exterior to meet realistic 747-400 fidelity.

## Locked requirements
- Boeing 747-400 passenger, 1 block = 1 metre.
- Real dimensions: length 70.66m, span 64.44m, tail height about 19.41m.
- GE CF6-80C2-style x4, with correctly modeled front and rear.
- Smooth real 747 nose and radome; cockpit glazing is an actual aperture, not black cubes stuck to the surface.
- Properly joined 747 upper-deck hump and main fuselage, no visible top gaps.
- Aerodynamic wing cross-sections; wing root blends and characteristic -400 winglets.
- First-person cockpit shares the same spatial design as the exterior and allows a forward view.
- Pearl-white/navy modern livery.
- Civilian Aviation's L-1049G is a *quality benchmark*, not a mesh to copy.
- The old 216-slot cargo, 6 general upgrades + 1 fuel/power and audio/flight systems may be reintegrated **after** the model is approved.

Authoritative reference baseline:
Boeing *747-400 Airplane Characteristics for Airport Planning*, D6-58326-1, Revision F.
https://www.boeing.com/content/dam/boeing/v2/airports/acaps/747-400_Rev_F.pdf

## No repeating previous failures
1. No cone-like nose built only by shrinking a few arbitrary elliptical rings.
2. No fake cockpit glazing pasted onto opaque outside surface.
3. No independently intersecting top-deck hump without proper topology/transition.
4. No rectangular board wings instead of actual airfoil/thickness and swept planform.
5. No hollow rear nacelles without turbines/nozzle/plug.
6. No whole-aircraft invisibility in first person.
7. No stating that a successful build means the aircraft *looks right*.

## Validation stage gates
### Gate 0: reference alignment and coordinate conventions
Set up top, side and front reference views, scale all axes to the Boeing dimensions, record visual landmarks. Reference images remain references; do not redistribute proprietary graphics.

### Gate 1: exterior silhouette — **first user acceptance**
Sculpt an editable 3D model with deliberately designed topology: fuselage, correctly full/rounded radome, original -400 hump, swept wings, tail, engines in plausible scale and positions. Integrate the nose-to-flightdeck transition, not just a sectional cone.
Deliver **real 3D GLB mesh** and **six screenshots rendered from the real mesh** (front, side, top, 45° forward, 45° rear, rear). Include independent dimensional QA and Boeing-view side-by-side comparisons. If user rejects, revise **this model**, do not bypass the gate.

### Gate 2: fuselage seam, cockpit glazing and wing-root quality
Cut true windows into coherent 3D shell, weld/fix hump at roof, refine wing-root fairings, remove overlapping geometry/shading. Deliver magnified nose and top screenshots for acceptance.

### Gate 3: engine and exterior detail
Create CF6 intake ring/duct/fan, exhaust turbine/nozzle/center plug, proper pylon, flap lines, gear, doors and livery. User accepts both engine front and rear views.

### Gate 4: cockpit model + first-person visual
Place driver camera inside real modeled cockpit; render actual Minecraft first-person tests. The exterior remains visible where appropriate; camera-specific clipping must be narrowly scoped and disclosed.

### Gate 5: game systems and tested JAR
Only after Gates 1–4 are accepted: integrate into IA/Fabric. Then test real client, collisions, seats, sound, cargo, upgrades, and measured speed. A Java build alone never counts as visual validation.

## Required repeatable outputs for each checkpoint
- Model interchange file, normally `.glb` with source/project file where available.
- Six fixed camera screenshots created from the **actual geometry**, not generative 2D art.
- `geometry_report.json` with bounds, triangle counts, independent measurements.
- Side-by-side official-reference comparison and visible defect list.
- Explicit user result **ACCEPT / REVISE**; no inferred approval.

## QA tool
```bash
xvfb-run -a python tools/exterior_review.py models/747-400-exterior.glb --output review/checkpoint-01
```
V2.1 axes: X wing span, Y up, +Z nose. Geometry bounds tolerance defaults to 0.12m. A passing dimension report does **not** prove a faithful 747.

Runtime: this environment has Trimesh, CadQuery, VTK and headless rendering, but not Blender itself. We can deliver a real editable/portable GLB and 3D screenshot reviews; do **not** promise a native .blend until Blender actually runs.
