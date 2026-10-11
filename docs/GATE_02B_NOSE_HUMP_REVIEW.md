# V3 Gate 02B — Nose, windshield trim and upper-deck hump

Status: **Awaiting visual acceptance**. No changes to released V2.1 or main branch.

This checkpoint was built from Gate 02A's real GLB surface model, NOT the V2.1 Minecraft aircraft.

## Geometry changes
- Fuller forward radome loft, preserving nominal 747-400 model envelope.
- Satin-metal fine strips over thicker dark-rubber retention seals around six pre-existing physical windshield apertures.
- More gradual raised upper-deck longitudinal hump transition, with upper-deck window row redistributed.
- A conformal 3D radome panel seam, small paired forward pitot/static sensor mounts.

## Verified
- Actual GLB bounding box: **length 70.66m, wingspan 64.44m, total height 19.41m**.
- 148,697 triangles in 31 material/functional mesh groups.
- Byte-level vertex/face comparison: previously approved tail, wings, pylons and CF6-style engine geometry unchanged.
- Offline Chromium WebGL viewer tested; camera selection, toggle and drag/zoom worked without JS console errors.
- Full editable parametric Python source, GLB, offline HTML viewer, review screenshots and JSON QA are supplied together in the downloadable Gate 02B ZIP in the conversation.

## Not yet complete
This is **not** an approved final exterior or a Minecraft JAR. True optical windshield transparency, full cockpit interior, first-person game camera, production engine/gear detail, aircraft gameplay mechanics and real Minecraft client verification remain pending.

## User review questions
1. Is the fuller nose/radome curve more 747-like?
2. Are the six pane outlines and dark retention trims convincing?
3. Does the upper-deck hump merge naturally into the main fuselage?
4. Are the already accepted tail-root joints still correct?

Keep this checkpoint separate from `main` until the user approves it.
