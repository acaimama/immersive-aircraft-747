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
        this.shadowRadius = 16.0F;
    }

    @Override
    protected ResourceLocation getModelId() { return MODEL_ID; }

    @Override
    protected ModelPartRenderHandler<Boeing747Entity> getModel(AircraftEntity entity) { return model; }

    @Override
    protected double getCullingBoundingBoxInflation() { return 38.0; }

    @Override
    public void renderLocal(Boeing747Entity entity, float yaw, float tickDelta, PoseStack p,
                            PoseStack.Pose peek, MultiBufferSource b, int light) {
        // V2.1 deliberately renders the SAME complete aircraft in first and third person.
        // The cockpit windshield is a real opening in the mesh, so the pilot can see through
        // it without deleting the exterior aircraft model.
        super.renderLocal(entity, yaw, tickDelta, p, peek, b, light);

        p.pushPose();
        renderCabinGlass(p,b,light);
        renderCockpitGlass(p,b,light);
        renderFans(entity,tickDelta,p,b,light);
        renderGear(entity,p,b,light);
        renderLights(entity,p,b);
        p.popPose();
    }

    private void renderCabinGlass(PoseStack p, MultiBufferSource b, int light) {
        BlockState glass=Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        // Long transparent panes sit behind the structural window pillars.
        cuboid(p,b,light,glass,-3.245F,7.69F,-0.65F,.055F,.92F,52.7F,0,0,0);
        cuboid(p,b,light,glass, 3.245F,7.69F,-0.65F,.055F,.92F,52.7F,0,0,0);
        cuboid(p,b,light,glass,-2.91F,11.63F,23.45F,.045F,.72F,9.45F,0,0,0);
        cuboid(p,b,light,glass, 2.91F,11.63F,23.45F,.045F,.72F,9.45F,0,0,0);
    }

    private void renderCockpitGlass(PoseStack p, MultiBufferSource b, int light) {
        BlockState glass=Blocks.TINTED_GLASS.defaultBlockState();
        // Six windshield panes close the real mesh opening while remaining transparent.
        cuboid(p,b,light,glass,-.72F,10.25F,31.52F,1.18F,.78F,.055F,-9,-7,0);
        cuboid(p,b,light,glass, .72F,10.25F,31.52F,1.18F,.78F,.055F,-9,7,0);
        cuboid(p,b,light,glass,-1.70F,10.18F,31.05F,.84F,.72F,.050F,-8,-25,0);
        cuboid(p,b,light,glass, 1.70F,10.18F,31.05F,.84F,.72F,.050F,-8,25,0);
        cuboid(p,b,light,glass,-2.30F,10.03F,30.38F,.54F,.62F,.045F,-6,-42,0);
        cuboid(p,b,light,glass, 2.30F,10.03F,30.38F,.54F,.62F,.045F,-6,42,0);
    }

    private void renderFans(Boeing747Entity entity,float tickDelta,PoseStack p,MultiBufferSource b,int light) {
        BlockState blade=Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState spinner=Blocks.IRON_BLOCK.defaultBlockState();
        float power=entity.getEnginePower();
        int id=entity.getId(), tick=entity.tickCount, prev=fanLastTicks.getOrDefault(id,tick);
        float angle=fanAngles.getOrDefault(id,0F);
        if(tick!=prev && power>.012F){
            angle=(angle+(5F+power*105F)*Math.max(1,tick-prev))%360F;
            fanAngles.put(id,angle);
        }
        fanLastTicks.put(id,tick);
        float a=angle+(power>.012F?(5F+power*105F)*tickDelta:0F);
        fan(p,b,light,blade,spinner,-19.3F,4.75F,-1.95F,a);
        fan(p,b,light,blade,spinner,-9.2F,4.95F,1.45F,a+13F);
        fan(p,b,light,blade,spinner, 9.2F,4.95F,1.45F,a+27F);
        fan(p,b,light,blade,spinner,19.3F,4.75F,-1.95F,a+41F);
    }

    private void fan(PoseStack p,MultiBufferSource b,int light,BlockState blade,BlockState spinner,
                     float x,float y,float z,float angle){
        // 24-blade CF6-like visible fan disc, recessed behind the intake lip.
        for(int i=0;i<24;i++)
            cuboid(p,b,light,blade,x,y,z,.035F,1.72F,.035F,0,0,angle+i*7.5F);
        cuboid(p,b,light,spinner,x,y,z+.03F,.34F,.34F,.09F,0,0,0);
    }

    private void renderGear(Boeing747Entity e,PoseStack p,MultiBufferSource b,int light){
        boolean down=e.onGround()||e.getDeltaMovement().y<=-.018D;
        if(!down)return;
        BlockState strut=Blocks.IRON_BLOCK.defaultBlockState(), tire=Blocks.BLACK_CONCRETE.defaultBlockState(),
                   hub=Blocks.POLISHED_ANDESITE.defaultBlockState();

        // Nose gear.
        cuboid(p,b,light,strut,0,2.65F,24.1F,.20F,4.4F,.20F,0,0,0);
        wheel(p,b,light,tire,hub,-.32F,.48F,24.25F); wheel(p,b,light,tire,hub,.32F,.48F,24.25F);

        // Four 747 main bogies.
        bogie(p,b,light,strut,tire,hub,-2.2F,-4.0F);
        bogie(p,b,light,strut,tire,hub, 2.2F,-4.0F);
        bogie(p,b,light,strut,tire,hub,-6.2F,-5.2F);
        bogie(p,b,light,strut,tire,hub, 6.2F,-5.2F);
    }

    private void bogie(PoseStack p,MultiBufferSource b,int light,BlockState strut,BlockState tire,BlockState hub,float x,float z){
        cuboid(p,b,light,strut,x,2.7F,z,.24F,4.5F,.24F,0,0,0);
        cuboid(p,b,light,strut,x,.72F,z,1.45F,.16F,2.0F,0,0,0);
        for(float dx:new float[]{-.48F,.48F}) for(float dz:new float[]{-.54F,.54F}) wheel(p,b,light,tire,hub,x+dx,.48F,z+dz);
    }

    private void wheel(PoseStack p,MultiBufferSource b,int light,BlockState tire,BlockState hub,float x,float y,float z){
        cuboid(p,b,light,tire,x,y,z,.62F,.62F,.42F,0,0,0);
        cuboid(p,b,light,hub,x,y,z+.22F,.24F,.24F,.04F,0,0,0);
    }

    private void renderLights(Boeing747Entity e,PoseStack p,MultiBufferSource b){
        if(!(e.isVehicle()||e.getEngineTarget()>.01F))return;
        int full=LightTexture.FULL_BRIGHT;
        BlockState core=Blocks.SEA_LANTERN.defaultBlockState(), red=Blocks.RED_STAINED_GLASS.defaultBlockState(),
                   green=Blocks.LIME_STAINED_GLASS.defaultBlockState(), white=Blocks.WHITE_STAINED_GLASS.defaultBlockState();
        lightPair(p,b,full,core,red,-32.05F,6.15F,-14.9F,.18F,.32F);
        lightPair(p,b,full,core,green,32.05F,6.15F,-14.9F,.18F,.32F);
        lightPair(p,b,full,core,white,0,11.0F,-34.6F,.16F,.28F);

        int beacon=e.tickCount%22;
        if(beacon<8){
            lightPair(p,b,full,core,red,0,10.73F,-2.0F,.18F,.34F);
            lightPair(p,b,full,core,red,0,4.0F,-1.5F,.16F,.30F);
        }

        int strobe=e.tickCount%30;
        if(strobe<2||(strobe>=5&&strobe<7)){
            lightPair(p,b,full,core,white,-32.15F,6.10F,-14.85F,.25F,.46F);
            lightPair(p,b,full,core,white, 32.15F,6.10F,-14.85F,.25F,.46F);
        }

        boolean landing=e.onGround()||e.getDeltaMovement().y<=-.018D;
        if(landing){
            lightPair(p,b,full,core,white,-5.5F,6.45F,4.0F,.26F,.48F);
            lightPair(p,b,full,core,white, 5.5F,6.45F,4.0F,.26F,.48F);
            lightPair(p,b,full,core,white,0,2.4F,24.2F,.22F,.42F);
        }
    }

    private void lightPair(PoseStack p,MultiBufferSource b,int light,BlockState core,BlockState lens,
                           float x,float y,float z,float c,float l){
        cuboid(p,b,light,core,x,y,z,c,c,c,0,0,0);
        cuboid(p,b,light,lens,x,y,z,l,l,l,0,0,0);
    }

    private void cuboid(PoseStack p,MultiBufferSource b,int light,BlockState state,float cx,float cy,float cz,
                        float sx,float sy,float sz,float rx,float ry,float rz){
        p.pushPose(); p.translate(cx,cy,cz);
        if(ry!=0)p.mulPose(Axis.YP.rotationDegrees(ry));
        if(rx!=0)p.mulPose(Axis.XP.rotationDegrees(rx));
        if(rz!=0)p.mulPose(Axis.ZP.rotationDegrees(rz));
        p.translate(-sx*.5F,-sy*.5F,-sz*.5F); p.scale(sx,sy,sz);
        blocks.renderSingleBlock(state,p,b,light,OverlayTexture.NO_OVERLAY); p.popPose();
    }
}
