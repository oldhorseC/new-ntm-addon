package com.ntmit.render;

import com.ntmit.entity.EntityRBMKBlueLightMarker;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderRBMKBlueLightMarker extends Render<EntityRBMKBlueLightMarker> {

	public RenderRBMKBlueLightMarker(RenderManager renderManager) {
		super(renderManager);
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityRBMKBlueLightMarker entity) {
		return null;
	}

	@Override
	public void doRender(EntityRBMKBlueLightMarker entity, double x, double y, double z, float entityYaw, float partialTicks) {
	}
}