package dev.openai.ia747.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.openai.ia747.Boeing747Addon;
import dev.openai.ia747.entity.Boeing747Entity;
import immersive_aircraft.client.render.entity.renderer.AircraftEntityRenderer;
import immersive_aircraft.client.render.entity.renderer.utils.ModelPartRenderHandler;
import immersive_aircraft.entity.AircraftEntity;
import net.minecraft.client.Minecraft;
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
        this.shadowRadius = 5.0F;
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
        return 12.0;
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

        cuboid(p,b,light,white,0,2.35F,0.2F,3.35F,2.65F,13.4F,0,0,0);
        cuboid(p,b,light,white,0,2.35F,7.55F,2.95F,2.45F,2.25F,0,0,0);
        cuboid(p,b,light,white,0,2.35F,9.05F,2.35F,2.05F,1.15F,0,0,0);
        cuboid(p,b,light,white,0,2.55F,-8.7F,1.9F,2.0F,3.0F,0,0,0);

        cuboid(p,b,light,white,0,3.95F,5.15F,2.78F,1.55F,5.65F,0,0,0);
        cuboid(p,b,light,glass,0.72F,4.12F,7.55F,0.92F,0.46F,0.10F,0,-17,0);
        cuboid(p,b,light,glass,-0.72F,4.12F,7.55F,0.92F,0.46F,0.10F,0,17,0);
        cuboid(p,b,light,blue,1.70F,2.75F,0.25F,0.09F,0.34F,12.7F,0,0,0);
        cuboid(p,b,light,blue,-1.70F,2.75F,0.25F,0.09F,0.34F,12.7F,0,0,0);

        cuboid(p,b,light,gray,3.25F,1.95F,-0.55F,5.45F,0.28F,2.75F,0,11,0);
        cuboid(p,b,light,gray,7.1F,1.92F,-2.0F,5.2F,0.22F,1.7F,0,20,0);
        cuboid(p,b,light,gray,-3.25F,1.95F,-0.55F,5.45F,0.28F,2.75F,0,-11,0);
        cuboid(p,b,light,gray,-7.1F,1.92F,-2.0F,5.2F,0.22F,1.7F,0,-20,0);

        cuboid(p,b,light,gray,2.4F,3.15F,-8.75F,4.5F,0.22F,1.45F,0,21,0);
        cuboid(p,b,light,gray,-2.4F,3.15F,-8.75F,4.5F,0.22F,1.45F,0,-21,0);
        cuboid(p,b,light,white,0,5.15F,-8.95F,0.38F,4.35F,2.25F,-8,0,0);

        engine(entity,tickDelta,p,b,light,-6.2F,0.70F,-1.65F,white,blue,dark,gray);
        engine(entity,tickDelta,p,b,light,-3.45F,0.72F,-0.75F,white,blue,dark,gray);
        engine(entity,tickDelta,p,b,light,3.45F,0.72F,-0.75F,white,blue,dark,gray);
        engine(entity,tickDelta,p,b,light,6.2F,0.70F,-1.65F,white,blue,dark,gray);

        if (entity.onGround() || entity.getDeltaMovement().y <= -0.018D) {
            cuboid(p,b,light,iron,0,0.72F,6.75F,0.18F,1.40F,0.18F,0,0,0);
            cuboid(p,b,light,tire,-0.32F,0.18F,6.75F,0.42F,0.42F,0.30F,0,0,0);
            cuboid(p,b,light,tire,0.32F,0.18F,6.75F,0.42F,0.42F,0.30F,0,0,0);
            for (float x : new float[]{-1.55F,1.55F,-3.10F,3.10F}) {
                cuboid(p,b,light,iron,x,0.82F,-1.6F,0.22F,1.35F,0.22F,0,0,0);
                cuboid(p,b,light,tire,x,0.18F,-1.6F,0.65F,0.50F,0.45F,0,0,0);
            }
        }
    }

    private void engine(Boeing747Entity entity,float tickDelta,PoseStack p,MultiBufferSource b,int light,
                        float x,float y,float z,BlockState white,BlockState blue,BlockState dark,BlockState fan) {
        cuboid(p,b,light,white,x,y,z,1.35F,1.35F,2.30F,0,0,0);
        cuboid(p,b,light,blue,x,y-0.42F,z-0.08F,1.37F,0.30F,1.75F,0,0,0);
        cuboid(p,b,light,dark,x,y,z+1.17F,1.10F,1.10F,0.12F,0,0,0);
        float speed=(float)entity.getDeltaMovement().length();
        float fanAngle=(entity.tickCount+tickDelta)*(35.0F+speed*500.0F);
        cuboid(p,b,light,fan,x,y,z+1.245F,0.12F,0.88F,0.08F,0,0,fanAngle);
        cuboid(p,b,light,fan,x,y,z+1.250F,0.12F,0.88F,0.08F,0,0,fanAngle+90.0F);
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
