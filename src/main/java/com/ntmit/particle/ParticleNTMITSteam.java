package com.ntmit.particle;

import com.hbm.main.client.NTMClientRegistry;
import com.hbm.particle.ParticleCoolingTower;
import com.hbm.render.util.NTMBufferBuilder;
import com.ntmit.lib.RbmkJumpHandler;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class ParticleNTMITSteam extends ParticleCoolingTower {

	private float cloudBase = 1.5F;
	private float cloudMax = 4.0F;

	public ParticleNTMITSteam(World world, double x, double y, double z) {
		super(world, x, y, z);
		this.particleRed = 1.0F;
		this.particleGreen = 1.0F;
		this.particleBlue = 1.0F;
		this.alphaMod(0.6F);
		this.noWind();
		this.setLift(0.0F);
		this.setStrafe(0.05F);
		this.setParticleTexture(NTMClientRegistry.contrail);
	}

	public void setColor(int rgb) {
		this.particleRed = (float) ((rgb >> 16) & 0xFF) / 255.0F;
		this.particleGreen = (float) ((rgb >> 8) & 0xFF) / 255.0F;
		this.particleBlue = (float) (rgb & 0xFF) / 255.0F;
	}

	public void setCloudScale(float base, float max) {
		this.cloudBase = base;
		this.cloudMax = max;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		float ageScale = this.particleMaxAge <= 0 ? 0.0F : (float) this.particleAge / (float) this.particleMaxAge;
		this.particleScale = this.cloudBase + (this.cloudMax - this.cloudBase) * (float) Math.pow(ageScale, RbmkJumpHandler.PLATE_STEAM_EXPAND);
	}

	@Override
	public int getFXLayer() {
		return 1;
	}

	@Override
	public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
		if (this.particleAge == 0) return;
		GlStateManager.enableCull();
		float uMin = this.particleTexture.getMinU();
		float uMax = this.particleTexture.getMaxU();
		float vMin = this.particleTexture.getMinV();
		float vMax = this.particleTexture.getMaxV();
		float size = 0.75F * this.particleScale;
		Random urandom = new Random(this.hashCode());
		NTMBufferBuilder fast = (NTMBufferBuilder) buffer;
		for (int layer = 0; layer < RbmkJumpHandler.PLATE_STEAM_LAYERS; layer++) {
			float px = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) partialTicks - interpPosX + urandom.nextGaussian() * 0.45D);
			float py = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) partialTicks - interpPosY + urandom.nextGaussian() * 0.45D);
			float pz = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTicks - interpPosZ + urandom.nextGaussian() * 0.45D);
			int light = this.getBrightnessForRender(partialTicks);
			int packedColor = NTMBufferBuilder.packColor(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
			int packedLight = NTMBufferBuilder.packLightmap(light >> 16 & 0xFFFF, light & 0xFFFF);
			Vec3d[] quad = new Vec3d[] {
				new Vec3d((double) (-rotationX * size - rotationXY * size), (double) (-rotationZ * size), (double) (-rotationYZ * size - rotationXZ * size)),
				new Vec3d((double) (-rotationX * size + rotationXY * size), (double) (rotationZ * size), (double) (-rotationYZ * size + rotationXZ * size)),
				new Vec3d((double) (rotationX * size + rotationXY * size), (double) (rotationZ * size), (double) (rotationYZ * size + rotationXZ * size)),
				new Vec3d((double) (rotationX * size - rotationXY * size), (double) (-rotationZ * size), (double) (rotationYZ * size - rotationXZ * size))
			};
			fast.appendParticlePositionTexColorLmap(px + (float) quad[0].x, py + (float) quad[0].y, pz + (float) quad[0].z, uMax, vMax, packedColor, packedLight);
			fast.appendParticlePositionTexColorLmap(px + (float) quad[1].x, py + (float) quad[1].y, pz + (float) quad[1].z, uMax, vMin, packedColor, packedLight);
			fast.appendParticlePositionTexColorLmap(px + (float) quad[2].x, py + (float) quad[2].y, pz + (float) quad[2].z, uMin, vMin, packedColor, packedLight);
			fast.appendParticlePositionTexColorLmap(px + (float) quad[3].x, py + (float) quad[3].y, pz + (float) quad[3].z, uMin, vMax, packedColor, packedLight);
		}
		GlStateManager.disableCull();
	}
}