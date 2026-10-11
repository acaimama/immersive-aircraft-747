# V3 Gate 02A — True flight-deck window apertures
Status: **Awaiting user visual approval**. Do not merge into `main` or produce a Minecraft JAR.

## Implemented prototype
- Replaces the old, pasted-on 747 cockpit window surfaces with a real six-pane opening in the parametric fuselage crown shell. Distinct glazing and frame geometry.
- Eliminates the upper deck preview windows placed too far forward of the flight deck.
- Preserves the previously user-approved V3 tail-root correction and all wing/engine mesh geometry.
- Current model bounding dimensions remain length **70.66 m**, span **64.44 m**, total height **19.41 m**.
- An offline HTML WebGL viewer allows close nose angles and toggling windows off to examine the real hole.
- **Not yet implemented:** full cockpit interior/first-person runtime, transparent glass shading, production sealing/welding, Minecraft entity or JAR.

## Acceptance questions
1. Does the forward/side angle windshield look like a 747-400?
2. Are the curved shell, frame, and nose surface visually coherent?
3. When panes are hidden, is the opening present rather than a white patch?
4. Is the approved tail preserved?

## Delivery details
The downloadable Gate 02A review package supplied to the user includes:
`747-400-V3-风挡验收-双击打开.html`, a real GLB model, before/after screenshots,
`build_v3_gate2.py` (editable source), `make_viewer.py`, and geometry QA JSON.
The package has not been uploaded to GitHub; this document records the checkpoint only.
