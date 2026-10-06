package dev.openai.ia747.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.openai.ia747.Boeing747Addon;
import dev.openai.ia747.entity.Boeing747Entity;
import immersive_aircraft.client.render.entity.renderer.AircraftEntityRenderer;
import immersive_aircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import immersive_aircraft.entity.AircraftEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public final class Boeing747Renderer extends AircraftEntityRenderer<Boeing747Entity> {
    private static final ResourceLocation MODEL_ID = Boeing747Addon.id("boeing_747_400");

    private final ModelPartRenderHandler<Boeing747Entity> model = new ModelPartRenderHandler<>();
    private final BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
    private final Map<Integer, Float> fanAngles = new HashMap<>();
    private final Map<Integer, Integer> fanLastTicks = new HashMap<>();

    public Boeing747Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 5.5F;
    }

    @Override
    protected ResourceLocation getModelId() {
        return MODEL_ID;
    }

    @Override
    protected ModelPartRenderHandler<Boeing747Entity> getModel(AircraftEntity entity) {
        return model;
    }

    @Override
    protected double getCullingBoundingBoxInflation() {
        return 14.0;
    }

    @Override
    public void renderLocal(
            Boeing747Entity entity,
            float yaw,
            float tickDelta,
            PoseStack poseStack,
            PoseStack.Pose peek,
            MultiBufferSource buffers,
            int packedLight
    ) {
        boolean localFirstPersonPilot =
                Minecraft.getInstance().player != null
                        && entity.hasPassenger(Minecraft.getInstance().player)
                        && Minecraft.getInstance().options.getCameraType() == net.minecraft.client.CameraType.FIRST_PERSON;

        // In first-person the camera sits inside the cockpit. Rendering the entire exterior
        // shell around the camera blocks the windshield, so only third-person/external views
        // draw the aircraft body. This does not change what other players see.
        if (localFirstPersonPilot) {
            return;
        }

        // IA renders the detailed BBModel first.
        super.renderLocal(entity, yaw, tickDelta, poseStack, peek, buffers, packedLight);

        // Transparent / emissive / animated details are layered on top.
        poseStack.pushPose();
        renderTransparentWindows(poseStack, buffers, packedLight);
        renderCabinLighting(entity, poseStack, buffers);
        renderCockpitGlass(poseStack, buffers, packedLight);
        renderDoorWindows(poseStack, buffers, packedLight);
        renderEngineFans(entity, tickDelta, poseStack, buffers, packedLight);
        renderFlaps(entity, poseStack, buffers, packedLight);
        renderLandingGear(entity, poseStack, buffers, packedLight);
        renderExteriorLights(entity, poseStack, buffers);
        poseStack.popPose();
    }

    private void renderTransparentWindows(PoseStack p, MultiBufferSource b, int light) {
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();

        // Main-deck windows match the BBModel frame grid exactly:
        // model pillars every 15 model-units, 4 units wide => 11-unit opening.
        for (int i = 0; i < 16; i++) {
            float z = (-112.5F + i * 15.0F) / 16.0F;
            cuboid(p,b,light,glass,-1.758F,2.71875F,z,0.085F,0.675F,0.675F,0,0,0);
            cuboid(p,b,light,glass, 1.758F,2.71875F,z,0.085F,0.675F,0.675F,0,0,0);
        }

        // Upper-deck windows use the same exact shared geometry:
        // pillars every 14 units, 4 wide => 10-unit opening.
        for (int i = 0; i < 5; i++) {
            float z = (55.0F + i * 14.0F) / 16.0F;
            cuboid(p,b,light,glass,-1.445F,4.375F,z,0.080F,0.365F,0.615F,0,0,0);
            cuboid(p,b,light,glass, 1.445F,4.375F,z,0.080F,0.365F,0.615F,0,0,0);
        }
    }

    private void renderCockpitGlass(PoseStack p, MultiBufferSource b, int light) {
        BlockState cockpit = Blocks.TINTED_GLASS.defaultBlockState();

        // Six-piece windshield gives the nose a real cockpit rather than a black painted strip.
        cuboid(p,b,light,cockpit,-0.84F,3.45F,9.66F,0.62F,0.38F,0.075F,0,-18,0);
        cuboid(p,b,light,cockpit,-0.28F,3.48F,9.79F,0.48F,0.38F,0.075F,0,-7,0);
        cuboid(p,b,light,cockpit, 0.28F,3.48F,9.79F,0.48F,0.38F,0.075F,0,7,0);
        cuboid(p,b,light,cockpit, 0.84F,3.45F,9.66F,0.62F,0.38F,0.075F,0,18,0);

        cuboid(p,b,light,cockpit,-1.28F,3.35F,9.30F,0.48F,0.34F,0.070F,0,-36,0);
        cuboid(p,b,light,cockpit, 1.28F,3.35F,9.30F,0.48F,0.34F,0.070F,0,36,0);
    }

    private void renderDoorWindows(PoseStack p, MultiBufferSource b, int light) {
        BlockState doorGlass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();

        // Four main-deck passenger doors per side.
        float[] doorZ = {6.25F, 2.625F, -2.375F, -6.125F};
        for (float z : doorZ) {
            cuboid(p,b,light,doorGlass,-1.862F,2.72F,z,0.060F,0.34F,0.34F,0,0,0);
            cuboid(p,b,light,doorGlass, 1.862F,2.72F,z,0.060F,0.34F,0.34F,0,0,0);
        }

        // Upper-deck forward doors.
        cuboid(p,b,light,doorGlass,-1.47F,4.36F,6.13F,0.055F,0.27F,0.30F,0,0,0);
        cuboid(p,b,light,doorGlass, 1.47F,4.36F,6.13F,0.055F,0.27F,0.30F,0,0,0);
    }

    private void renderCabinLighting(Boeing747Entity entity, PoseStack p, MultiBufferSource b) {
        int full = LightTexture.FULL_BRIGHT;
        BlockState warm = Blocks.OCHRE_FROGLIGHT.defaultBlockState();

        // A continuous-looking warm cabin light line is visible through the transparent windows.
        for (float z = -6.4F; z <= 6.5F; z += 1.55F) {
            cuboid(p,b,full,warm,-1.10F,3.28F,z,0.18F,0.10F,0.50F,0,0,0);
            cuboid(p,b,full,warm, 1.10F,3.28F,z,0.18F,0.10F,0.50F,0,0,0);
        }

        for (float z = 3.6F; z <= 6.7F; z += 1.35F) {
            cuboid(p,b,full,warm,-0.82F,4.72F,z,0.15F,0.09F,0.42F,0,0,0);
            cuboid(p,b,full,warm, 0.82F,4.72F,z,0.15F,0.09F,0.42F,0,0,0);
        }

        // Cockpit instrument glow while occupied or powered.
        if (entity.isVehicle() || entity.getEngineTarget() > 0.01F) {
            BlockState instrument = Blocks.CYAN_STAINED_GLASS.defaultBlockState();
            cuboid(p,b,full,instrument,0.0F,2.32F,9.35F,1.45F,0.12F,0.28F,-15,0,0);
        }
    }

    private void renderEngineFans(
            Boeing747Entity entity,
            float tickDelta,
            PoseStack p,
            MultiBufferSource b,
            int light
    ) {
        BlockState fan = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState spinner = Blocks.IRON_BLOCK.defaultBlockState();

        float power = entity.getEnginePower();
        int id = entity.getId();
        int currentTick = entity.tickCount;
        int previousTick = fanLastTicks.getOrDefault(id, currentTick);
        float baseAngle = fanAngles.getOrDefault(id, 0.0F);

        // Real turbofan behavior: parked / engine-off = stationary fan.
        // Once the engine actually spools, rotation ramps with engine power.
        if (currentTick != previousTick && power > 0.012F) {
            int elapsedTicks = Math.max(1, currentTick - previousTick);
            float degreesPerTick = 4.0F + power * 92.0F;
            baseAngle = (baseAngle + degreesPerTick * elapsedTicks) % 360.0F;
            fanAngles.put(id, baseAngle);
        }
        fanLastTicks.put(id, currentTick);

        float previewAdvance = power > 0.012F ? (4.0F + power * 92.0F) * tickDelta : 0.0F;
        float angle = baseAngle + previewAdvance;

        // Visible fan planes sit behind the intake lips, not outside them.
        // Outer engine center z=-25/16, inner z=-10/16; fan is recessed by ~0.30 block.
        fan(p,b,light,fan,spinner,-6.25F,0.75F,-0.6875F,angle);
        fan(p,b,light,fan,spinner,-3.44F,0.81F, 0.2500F,angle + 13.0F);
        fan(p,b,light,fan,spinner, 3.44F,0.81F, 0.2500F,angle + 27.0F);
        fan(p,b,light,fan,spinner, 6.25F,0.75F,-0.6875F,angle + 41.0F);
    }

    private void fan(
            PoseStack p, MultiBufferSource b, int light,
            BlockState fan, BlockState spinner,
            float x, float y, float z, float angle
    ) {
        // Twelve metallic blades form a clearly visible turbofan disc inside the open nacelle.
        for (int i = 0; i < 12; i++) {
            cuboid(p,b,light,fan,x,y,z,0.060F,0.84F,0.060F,0,0,angle + i*15.0F);
        }
        cuboid(p,b,light,spinner,x,y,z+0.035F,0.24F,0.24F,0.10F,0,0,0);
    }

    private void renderFlaps(Boeing747Entity entity, PoseStack p, MultiBufferSource b, int light) {
        BlockState flap = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();

        double horizontalSpeed = Math.sqrt(
                entity.getDeltaMovement().x * entity.getDeltaMovement().x +
                entity.getDeltaMovement().z * entity.getDeltaMovement().z
        );

        float angle = 0.0F;
        if (entity.onGround() && entity.getEngineTarget() > 0.12F) {
            angle = 9.0F;
        } else if (!entity.onGround() && entity.getDeltaMovement().y < -0.010D && horizontalSpeed < 0.90D) {
            angle = 23.0F;
        }

        // Inboard and outboard flap segments.
        cuboid(p,b,light,flap,-4.35F,1.72F,-2.10F,4.15F,0.14F,0.72F,angle,-14,0);
        cuboid(p,b,light,flap,-7.30F,1.67F,-3.05F,3.20F,0.13F,0.62F,angle,-22,0);
        cuboid(p,b,light,flap, 4.35F,1.72F,-2.10F,4.15F,0.14F,0.72F,angle,14,0);
        cuboid(p,b,light,flap, 7.30F,1.67F,-3.05F,3.20F,0.13F,0.62F,angle,22,0);

        // Aileron hint follows roll input visually through aircraft roll rate / airborne state.
        float aileron = entity.onGround() ? 0.0F : Math.max(-10.0F, Math.min(10.0F, entity.getRoll(1.0F) * 0.25F));
        cuboid(p,b,light,flap,-8.75F,1.75F,-2.05F,2.15F,0.12F,0.48F,-aileron,-24,0);
        cuboid(p,b,light,flap, 8.75F,1.75F,-2.05F,2.15F,0.12F,0.48F, aileron,24,0);
    }

    private void renderLandingGear(
            Boeing747Entity entity,
            PoseStack p,
            MultiBufferSource b,
            int light
    ) {
        boolean gearDown = entity.onGround() || entity.getDeltaMovement().y <= -0.018D;
        if (!gearDown) {
            return;
        }

        BlockState strut = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState tire = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState hub = Blocks.POLISHED_ANDESITE.defaultBlockState();

        // Nose gear and twin wheels.
        cuboid(p,b,light,strut,0,0.72F,6.75F,0.18F,1.35F,0.18F,0,0,0);
        wheel(p,b,light,tire,hub,-0.28F,0.16F,6.82F);
        wheel(p,b,light,tire,hub, 0.28F,0.16F,6.82F);

        // Four 4-wheel main bogies characteristic of the 747.
        mainBogie(p,b,light,strut,tire,hub,-1.42F,-1.48F);
        mainBogie(p,b,light,strut,tire,hub, 1.42F,-1.48F);
        mainBogie(p,b,light,strut,tire,hub,-3.36F,-1.20F);
        mainBogie(p,b,light,strut,tire,hub, 3.36F,-1.20F);

        // Gear door hints.
        BlockState door = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        cuboid(p,b,light,door,-0.58F,0.84F,6.40F,0.08F,0.85F,0.78F,0,0,-14);
        cuboid(p,b,light,door, 0.58F,0.84F,6.40F,0.08F,0.85F,0.78F,0,0,14);
        cuboid(p,b,light,door,-2.25F,1.12F,-1.25F,0.08F,0.85F,1.55F,0,0,-10);
        cuboid(p,b,light,door, 2.25F,1.12F,-1.25F,0.08F,0.85F,1.55F,0,0,10);
    }

    private void mainBogie(
            PoseStack p, MultiBufferSource b, int light,
            BlockState strut, BlockState tire, BlockState hub,
            float x, float z
    ) {
        cuboid(p,b,light,strut,x,0.78F,z,0.20F,1.30F,0.20F,0,0,0);
        cuboid(p,b,light,strut,x,0.32F,z,0.92F,0.13F,1.02F,0,0,0);
        wheel(p,b,light,tire,hub,x-0.36F,0.15F,z-0.34F);
        wheel(p,b,light,tire,hub,x+0.36F,0.15F,z-0.34F);
        wheel(p,b,light,tire,hub,x-0.36F,0.15F,z+0.34F);
        wheel(p,b,light,tire,hub,x+0.36F,0.15F,z+0.34F);
    }

    private void wheel(
            PoseStack p, MultiBufferSource b, int light,
            BlockState tire, BlockState hub,
            float x, float y, float z
    ) {
        cuboid(p,b,light,tire,x,y,z,0.43F,0.43F,0.31F,0,0,0);
        cuboid(p,b,light,hub,x,y,z+0.17F,0.18F,0.18F,0.035F,0,0,0);
    }

    private void renderExteriorLights(Boeing747Entity entity, PoseStack p, MultiBufferSource b) {
        int full = LightTexture.FULL_BRIGHT;

        BlockState whiteCore = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState red = Blocks.RED_STAINED_GLASS.defaultBlockState();
        BlockState green = Blocks.LIME_STAINED_GLASS.defaultBlockState();
        BlockState white = Blocks.WHITE_STAINED_GLASS.defaultBlockState();

        // Navigation lights stay visible whenever the aircraft is occupied/powered.
        if (entity.isVehicle() || entity.getEngineTarget() > 0.01F) {
            lightPair(p,b,full,whiteCore,red,-10.15F,2.46F,-2.55F,0.28F,0.48F);
            lightPair(p,b,full,whiteCore,green,10.15F,2.46F,-2.55F,0.28F,0.48F);
            lightPair(p,b,full,whiteCore,white,0.0F,3.15F,-10.55F,0.24F,0.40F);
        }

        // Red anti-collision beacon: top + belly, slower pulse.
        int beaconPhase = entity.tickCount % 22;
        if (beaconPhase < 8 && (entity.isVehicle() || entity.getEngineTarget() > 0.01F)) {
            // Top beacon is attached to the main-deck roof (roof surface ~= y 3.81 here).
            lightPair(p,b,full,whiteCore,red,0.0F,3.86F,0.10F,0.24F,0.44F);
            lightPair(p,b,full,whiteCore,red,0.0F,0.72F,-0.15F,0.24F,0.44F);
        }

        // Airliner-style double white strobe.
        int strobePhase = entity.tickCount % 30;
        boolean doubleFlash = strobePhase < 2 || (strobePhase >= 5 && strobePhase < 7);
        if (doubleFlash && (entity.isVehicle() || entity.getEngineTarget() > 0.01F)) {
            lightPair(p,b,full,whiteCore,white,-10.28F,2.35F,-2.45F,0.38F,0.70F);
            lightPair(p,b,full,whiteCore,white, 10.28F,2.35F,-2.45F,0.38F,0.70F);
            lightPair(p,b,full,whiteCore,white,0.0F,3.22F,-10.68F,0.32F,0.58F);
        }

        boolean landingConfig = entity.onGround() || entity.getDeltaMovement().y <= -0.018D;
        if (landingConfig && (entity.isVehicle() || entity.getEngineTarget() > 0.01F)) {
            // Wing-root landing lights.
            lightPair(p,b,full,whiteCore,white,-2.55F,1.94F,2.10F,0.40F,0.72F);
            lightPair(p,b,full,whiteCore,white, 2.55F,1.94F,2.10F,0.40F,0.72F);
            lightPair(p,b,full,whiteCore,white,-4.80F,1.82F,0.90F,0.34F,0.62F);
            lightPair(p,b,full,whiteCore,white, 4.80F,1.82F,0.90F,0.34F,0.62F);

            // Nose / taxi light.
            lightPair(p,b,full,whiteCore,white,0.0F,0.82F,6.80F,0.32F,0.58F);
        }

        // Logo/tail illumination at night-like visual intensity whenever powered.
        if (entity.getEngineTarget() > 0.01F) {
            lightPair(p,b,full,whiteCore,white,-0.44F,5.80F,-8.75F,0.23F,0.38F);
            lightPair(p,b,full,whiteCore,white, 0.44F,5.80F,-8.75F,0.23F,0.38F);
        }
    }

    private void lightPair(
            PoseStack p, MultiBufferSource b, int light,
            BlockState core, BlockState lens,
            float x, float y, float z,
            float coreSize, float lensSize
    ) {
        cuboid(p,b,light,core,x,y,z,coreSize,coreSize,coreSize,0,0,0);
        cuboid(p,b,light,lens,x,y,z,lensSize,lensSize,lensSize,0,0,0);
    }

    private void cuboid(
            PoseStack p,
            MultiBufferSource buffers,
            int light,
            BlockState state,
            float cx, float cy, float cz,
            float sx, float sy, float sz,
            float rotX, float rotY, float rotZ
    ) {
        p.pushPose();
        p.translate(cx, cy, cz);

        if (rotY != 0) {
            p.mulPose(Axis.YP.rotationDegrees(rotY));
        }
        if (rotX != 0) {
            p.mulPose(Axis.XP.rotationDegrees(rotX));
        }
        if (rotZ != 0) {
            p.mulPose(Axis.ZP.rotationDegrees(rotZ));
        }

        p.translate(-sx * 0.5F, -sy * 0.5F, -sz * 0.5F);
        p.scale(sx, sy, sz);
        blocks.renderSingleBlock(state, p, buffers, light, OverlayTexture.NO_OVERLAY);
        p.popPose();
    }
}
