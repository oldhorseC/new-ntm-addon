package com.ntmit.render;

import com.ntmit.entity.EntityRBMKBlueLight;
import com.ntmit.lib.RbmkJumpHandler;
import com.ntmit.lib.RefStrings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RenderRBMKBlueLight extends Render<EntityRBMKBlueLight> {

	private static final ResourceLocation TEXTURE = new ResourceLocation(RefStrings.MODID, "textures/particle/rbmk_blue_light.png");
	private static final double DISTANCE_INVISIBLE = 30.0D;
	private static final double DISTANCE_FULL = 50.0D;

	public RenderRBMKBlueLight(RenderManager renderManager) {
		super(renderManager);
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityRBMKBlueLight entity) {
		return TEXTURE;
	}

	@Override
	public boolean shouldRender(EntityRBMKBlueLight entity, ICamera camera, double camX, double camY, double camZ) {
		return true;
	}

	@Override
	public void doRender(EntityRBMKBlueLight entity, double x, double y, double z, float entityYaw, float partialTicks) {
	}

	public static void renderColumn(EntityRBMKBlueLight entity, double x, double y, double z, float partialTicks) {
		Minecraft mc = Minecraft.getMinecraft();
		if (entity == null || entity.width <= 0.0F) return;
		if (entity.timeAlpha <= 0.0F) return;
		Entity view = mc.getRenderViewEntity();
		if (view == null) return;
		double dx = view.posX - entity.posX;
		double dz = view.posZ - entity.posZ;
		double clampedY = MathHelper.clamp(view.posY, entity.baseY, entity.topY);
		double dy = view.posY - clampedY;
		double liveDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
		double distance = entity.timeLocked ? entity.capturedDistance : liveDistance;
		float distanceT = MathHelper.clamp((float) ((distance - DISTANCE_INVISIBLE) / (DISTANCE_FULL - DISTANCE_INVISIBLE)), 0.0F, 1.0F);
		float distanceFade = distanceT * distanceT * (3.0F - 2.0F * distanceT);
		if (distanceFade <= 0.0F) return;

		BlockPos basePos = new BlockPos(entity.posX, entity.posY + 0.5D, entity.posZ);
		int sky = entity.world.getLightFor(EnumSkyBlock.SKY, basePos) - entity.world.getSkylightSubtracted();
		int block = entity.world.getLightFor(EnumSkyBlock.BLOCK, basePos);
		float skyFactor = MathHelper.clamp((float) sky / 15.0F, 0.0F, 1.0F);
		float blockFactor = MathHelper.clamp((float) block / 15.0F, 0.0F, 1.0F);
		float lightFactor = MathHelper.clamp(0.20F + 0.45F * skyFactor + 0.55F * blockFactor, 0.20F, 1.0F);
		float visibleLight = Math.max(lightFactor, 0.5F);
		float baseAlpha = 0.9F * visibleLight * entity.timeAlpha;
		float fadeIn = MathHelper.clamp(((float) entity.age + partialTicks) / 15.0F, 0.0F, 1.0F);
		float fadeOut = 1.0F;
		if (entity.fadeOutTicks > 0) {
			float remaining = (float) entity.maxAge - ((float) entity.age + partialTicks);
			fadeOut = MathHelper.clamp(remaining / (float) entity.fadeOutTicks, 0.0F, 1.0F);
		}
		float alphaBottom = baseAlpha * MathHelper.clamp(fadeIn * 1.5F, 0.0F, 1.0F) * distanceFade * fadeOut;
		float alphaTop = baseAlpha * MathHelper.clamp(fadeIn * 1.5F - 0.5F, 0.0F, 1.0F) * distanceFade * fadeOut;
		float brightness = 0.45F + 0.55F * visibleLight;

		double half = (double) entity.width * 0.5D;
		double height = entity.topY - entity.baseY;

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
		GlStateManager.disableCull();
		GlStateManager.disableFog();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		GlStateManager.depthMask(false);
		GlStateManager.disableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(TEXTURE);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);

		int fullBright = 0xF000F0;
		int lightU = fullBright >> 16 & 0xFFFF;
		int lightV = fullBright & 0xFFFF;
		addFace(buffer, -half, half, 0.0D, height, alphaBottom, alphaTop, brightness, lightU, lightV);
		tessellator.draw();

		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.enableCull();
		GlStateManager.enableFog();
		GlStateManager.enableLighting();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	private static void addFace(BufferBuilder buffer, double left, double right, double bottom, double top, float alphaBottom, float alphaTop, float brightness, int lightU, int lightV) {
		buffer.pos(left, bottom, 0.0D).tex(0.0F, 1.0F).color(brightness, brightness, 1.0F, alphaBottom).lightmap(lightU, lightV).endVertex();
		buffer.pos(right, bottom, 0.0D).tex(1.0F, 1.0F).color(brightness, brightness, 1.0F, alphaBottom).lightmap(lightU, lightV).endVertex();
		buffer.pos(right, top, 0.0D).tex(1.0F, 0.0F).color(brightness, brightness, 1.0F, alphaTop).lightmap(lightU, lightV).endVertex();
		buffer.pos(left, top, 0.0D).tex(0.0F, 0.0F).color(brightness, brightness, 1.0F, alphaTop).lightmap(lightU, lightV).endVertex();
	}
}
