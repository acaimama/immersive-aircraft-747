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

public final class Boeing747Renderer extends AircraftEntityRenderer<Boeing747Entity> {
    private static final ResourceLocation MODEL_ID = Boeing747Addon.id("boeing_747_400_procedural");
    private final ModelPartRenderHandler<Boeing747Entity> emptyModel = new ModelPartRenderHandler<>();
    private final BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();

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
        return emptyModel;
    }

    @Override
    protected double getCullingBoundingBoxInflation() {
        return 13.0;
    }

    @Override
    public void renderLocal(Boeing747Entity entity, float yaw, float tickDelta, PoseStack poseStack,
                            PoseStack.Pose peek, MultiBufferSource buffers, int packedLight) {
        super.renderLocal(entity, yaw, tickDelta, poseStack, peek, buffers, packedLight);
        poseStack.pushPose();
        poseStack.translate(0.0, 0.20, 0.0);
        renderAirframe(entity, tickDelta, poseStack, buffers, packedLight);
        poseStack.popPose();
    }

    private void renderAirframe(Boeing747Entity entity, float tickDelta, PoseStack p, MultiBufferSource b, int light) {
        BlockState white = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState gray = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState dark = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState glass = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
        BlockState blue = Blocks.BLUE_CONCRETE.defaultBlockState();
        BlockState iron = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState tire = Blocks.BLACK_CONCRETE.defaultBlockState();

        // Long wide-body fuselage, nose and tail cone.
        cuboid(p,b,light,white,0,2.35F,0.2F,3.35F,2.65F,13.4F,0,0,0);
        cuboid(p,b,light,white,0,2.35F,7.55F,2.95F,2.45F,2.25F,0,0,0);
        cuboid(p,b,light,white,0,2.35F,9.05F,2.35F,2.05F,1.15F,0,0,0);
        cuboid(p,b,light,white,0,2.55F,-8.7F,1.9F,2.0F,3.0F,0,0,0);

        // 747 upper-deck hump.
        cuboid(p,b,light,white,0,3.95F,5.10F,2.78F,1.55F,5.75F,0,0,0);
        cuboid(p,b,light,white,0,4.15F,7.18F,2.48F,1.25F,1.65F,0,0,0);

        // Cockpit glazing.
        cuboid(p,b,light,glass,0.72F,4.12F,7.62F,0.92F,0.46F,0.11F,0,-17,0);
        cuboid(p,b,light,glass,-0.72F,4.12F,7.62F,0.92F,0.46F,0.11F,0,17,0);

        // Main-deck windows and upper-deck windows.
        for (float z = 5.55F; z >= -6.45F; z -= 1.18F) {
            cuboid(p,b,light,glass,1.69F,2.92F,z,0.08F,0.30F,0.48F,0,0,0);
            cuboid(p,b,light,glass,-1.69F,2.92F,z,0.08F,0.30F,0.48F,0,0,0);
        }
        for (float z = 5.65F; z >= 2.15F; z -= 1.10F) {
            cuboid(p,b,light,glass,1.38F,4.17F,z,0.07F,0.25F,0.42F,0,0,0);
            cuboid(p,b,light,glass,-1.38F,4.17F,z,0.07F,0.25F,0.42F,0,0,0);
        }

        // Blue cheatline.
        cuboid(p,b,light,blue,1.70F,2.64F,0.15F,0.09F,0.27F,13.2F,0,0,0);
        cuboid(p,b,light,blue,-1.70F,2.64F,0.15F,0.09F,0.27F,13.2F,0,0,0);

        // Swept main wings.
        cuboid(p,b,light,gray,3.25F,1.95F,-0.55F,5.45F,0.28F,2.75F,0,11,0);
        cuboid(p,b,light,gray,7.10F,1.92F,-2.00F,5.20F,0.22F,1.70F,0,20,0);
        cuboid(p,b,light,gray,-3.25F,1.95F,-0.55F,5.45F,0.28F,2.75F,0,-11,0);
        cuboid(p,b,light,gray,-7.10F,1.92F,-2.00F,5.20F,0.22F,1.70F,0,-20,0);

        // 747-400 style winglets.
        cuboid(p,b,light,white,10.03F,2.66F,-3.55F,0.30F,1.55F,0.58F,-8,0,-8);
        cuboid(p,b,light,blue,10.04F,3.14F,-3.56F,0.31F,0.48F,0.60F,-8,0,-8);
        cuboid(p,b,light,white,-10.03F,2.66F,-3.55F,0.30F,1.55F,0.58F,-8,0,8);
        cuboid(p,b,light,blue,-10.04F,3.14F,-3.56F,0.31F,0.48F,0.60F,-8,0,8);

        // Tailplane and vertical tail.
        cuboid(p,b,light,gray,2.4F,3.15F,-8.75F,4.5F,0.22F,1.45F,0,21,0);
        cuboid(p,b,light,gray,-2.4F,3.15F,-8.75F,4.5F,0.22F,1.45F,0,-21,0);
        cuboid(p,b,light,white,0,5.15F,-8.95F,0.38F,4.35F,2.25F,-8,0,0);
        cuboid(p,b,light,blue,0,6.55F,-9.15F,0.40F,1.50F,1.30F,-8,0,0);

        // Four engine pylons and turbofan nacelles.
        engine(entity,tickDelta,p,b,light,-6.2F,0.70F,-1.65F,white,blue,dark,gray);
        engine(entity,tickDelta,p,b,light,-3.45F,0.72F,-0.75F,white,blue,dark,gray);
        engine(entity,tickDelta,p,b,light,3.45F,0.72F,-0.75F,white,blue,dark,gray);
        engine(entity,tickDelta,p,b,light,6.2F,0.70F,-1.65F,white,blue,dark,gray);

        renderFlaps(entity,p,b,light,gray);
        renderLights(entity,p,b);
        renderLandingGear(entity,p,b,light,iron,tire);
    }

    private void renderFlaps(Boeing747Entity entity, PoseStack p, MultiBufferSource b, int light, BlockState gray) {
        float flap = 0.0F;
        double horizontalSpeed = Math.sqrt(
                entity.getDeltaMovement().x * entity.getDeltaMovement().x +
                entity.getDeltaMovement().z * entity.getDeltaMovement().z
        );
        if (entity.onGround() && entity.getEngineTarget() > 0.10F) {
            flap = 10.0F;
        } else if (!entity.onGround() && entity.getDeltaMovement().y < -0.012D && horizontalSpeed < 0.85D) {
            flap = 22.0F;
        }

        cuboid(p,b,light,gray,4.45F,1.78F,-2.15F,4.50F,0.18F,0.72F,flap,15,0);
        cuboid(p,b,light,gray,7.55F,1.72F,-3.15F,3.20F,0.16F,0.62F,flap,22,0);
        cuboid(p,b,light,gray,-4.45F,1.78F,-2.15F,4.50F,0.18F,0.72F,flap,-15,0);
        cuboid(p,b,light,gray,-7.55F,1.72F,-3.15F,3.20F,0.16F,0.62F,flap,-22,0);
    }

    private void renderLandingGear(Boeing747Entity entity, PoseStack p, MultiBufferSource b, int light,
                                   BlockState iron, BlockState tire) {
        boolean gearDown = entity.onGround() || entity.getDeltaMovement().y <= -0.018D;
        if (!gearDown) return;

        // Nose gear.
        cuboid(p,b,light,iron,0,0.76F,6.85F,0.20F,1.48F,0.20F,0,0,0);
        wheel(p,b,light,tire,-0.31F,0.18F,6.90F);
        wheel(p,b,light,tire,0.31F,0.18F,6.90F);

        // Four main gear bogies: two body gear + two wing gear.
        gearBogie(p,b,light,iron,tire,-1.45F,-1.60F);
        gearBogie(p,b,light,iron,tire,1.45F,-1.60F);
        gearBogie(p,b,light,iron,tire,-3.35F,-1.25F);
        gearBogie(p,b,light,iron,tire,3.35F,-1.25F);
    }

    private void gearBogie(PoseStack p, MultiBufferSource b, int light, BlockState iron, BlockState tire,
                           float x, float z) {
        cuboid(p,b,light,iron,x,0.82F,z,0.22F,1.38F,0.22F,0,0,0);
        cuboid(p,b,light,iron,x,0.34F,z,0.94F,0.14F,1.18F,0,0,0);
        wheel(p,b,light,tire,x-0.38F,0.16F,z-0.38F);
        wheel(p,b,light,tire,x+0.38F,0.16F,z-0.38F);
        wheel(p,b,light,tire,x-0.38F,0.16F,z+0.38F);
        wheel(p,b,light,tire,x+0.38F,0.16F,z+0.38F);
    }

    private void wheel(PoseStack p, MultiBufferSource b, int light, BlockState tire, float x, float y, float z) {
        cuboid(p,b,light,tire,x,y,z,0.46F,0.46F,0.34F,0,0,0);
    }

    private void renderLights(Boeing747Entity entity, PoseStack p, MultiBufferSource b) {
        if (!entity.isVehicle() && entity.getEngineTarget() <= 0.01F) return;

        BlockState red = Blocks.RED_STAINED_GLASS.defaultBlockState();
        BlockState green = Blocks.LIME_STAINED_GLASS.defaultBlockState();
        BlockState white = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
        int fullBright = LightTexture.FULL_BRIGHT;

        // Navigation lights: left red, right green.
        cuboid(p,b,fullBright,red,-10.25F,2.24F,-3.30F,0.30F,0.30F,0.30F,0,0,0);
        cuboid(p,b,fullBright,green,10.25F,2.24F,-3.30F,0.30F,0.30F,0.30F,0,0,0);

        // Tail nav light.
        cuboid(p,b,fullBright,white,0,5.10F,-10.18F,0.24F,0.24F,0.24F,0,0,0);

        // White strobes flash briefly every ~1.2 seconds.
        boolean strobe = entity.tickCount % 24 < 3;
        if (strobe) {
            cuboid(p,b,fullBright,white,-10.34F,2.12F,-3.18F,0.42F,0.24F,0.42F,0,0,0);
            cuboid(p,b,fullBright,white,10.34F,2.12F,-3.18F,0.42F,0.24F,0.42F,0,0,0);
            cuboid(p,b,fullBright,white,0,4.00F,-9.92F,0.30F,0.30F,0.30F,0,0,0);
        }

        // Landing lights when the gear is down.
        if (entity.onGround() || entity.getDeltaMovement().y <= -0.018D) {
            cuboid(p,b,fullBright,white,-1.15F,1.88F,4.35F,0.35F,0.22F,0.35F,0,0,0);
            cuboid(p,b,fullBright,white,1.15F,1.88F,4.35F,0.35F,0.22F,0.35F,0,0,0);
        }
    }

    private void engine(Boeing747Entity entity,float tickDelta,PoseStack p,MultiBufferSource b,int light,
                        float x,float y,float z,BlockState white,BlockState blue,BlockState dark,BlockState fan) {
        // Pylon.
        cuboid(p,b,light,fan,x,y+0.92F,z-0.10F,0.42F,1.00F,1.22F,-8,0,0);

        // Nacelle.
        cuboid(p,b,light,white,x,y,z,1.38F,1.38F,2.34F,0,0,0);
        cuboid(p,b,light,blue,x,y-0.43F,z-0.08F,1.40F,0.30F,1.80F,0,0,0);
        cuboid(p,b,light,dark,x,y,z+1.19F,1.13F,1.13F,0.14F,0,0,0);
        cuboid(p,b,light,dark,x,y,z-1.18F,0.76F,0.76F,0.16F,0,0,0);

        // Animated fan blades.
        float speed=(float)entity.getDeltaMovement().length();
        float power=entity.getEnginePower();
        float fanAngle=(entity.tickCount+tickDelta)*(24.0F+power*155.0F+speed*480.0F);
        cuboid(p,b,light,fan,x,y,z+1.26F,0.12F,0.90F,0.08F,0,0,fanAngle);
        cuboid(p,b,light,fan,x,y,z+1.265F,0.12F,0.90F,0.08F,0,0,fanAngle+90.0F);
    }

    private void cuboid(PoseStack p,MultiBufferSource buffers,int light,BlockState state,
                        float cx,float cy,float cz,float sx,float sy,float sz,float rotX,float rotY,float rotZ) {
        p.pushPose();
        p.translate(cx,cy,cz);
        if(rotY!=0)p.mulPose(Axis.YP.rotationDegrees(rotY));
        if(rotX!=0)p.mulPose(Axis.XP.rotationDegrees(rotX));
        if(rotZ!=0)p.mulPose(Axis.ZP.rotationDegrees(rotZ));
        p.translate(-sx*0.5F,-sy*0.5F,-sz*0.5F);
        p.scale(sx,sy,sz);
        blocks.renderSingleBlock(state,p,buffers,light,OverlayTexture.NO_OVERLAY);
        p.popPose();
    }
}
