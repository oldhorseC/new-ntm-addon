package com.ntmit.render;

import com.ntmit.entity.EntityRBMKBlueLight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RBMKBlueLightWorldRenderer {

	@SubscribeEvent
	public void onRenderWorldLast(RenderWorldLastEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.world == null) return;
		Entity view = mc.getRenderViewEntity();
		if (view == null) return;
		float partialTicks = event.getPartialTicks();
		double camX = view.lastTickPosX + (view.posX - view.lastTickPosX) * (double) partialTicks;
		double camY = view.lastTickPosY + (view.posY - view.lastTickPosY) * (double) partialTicks;
		double camZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * (double) partialTicks;

		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();
		try {
			for (Entity entity : mc.world.loadedEntityList) {
				if (!(entity instanceof EntityRBMKBlueLight)) continue;
				EntityRBMKBlueLight light = (EntityRBMKBlueLight) entity;
				double x = light.lastTickPosX + (light.posX - light.lastTickPosX) * (double) partialTicks - camX;
				double y = light.lastTickPosY + (light.posY - light.lastTickPosY) * (double) partialTicks - camY;
				double z = light.lastTickPosZ + (light.posZ - light.lastTickPosZ) * (double) partialTicks - camZ;
				RenderRBMKBlueLight.renderColumn(light, x, y, z, partialTicks);
			}
		} finally {
			GlStateManager.popMatrix();
			GlStateManager.popAttrib();
		}
	}
}