package com.ntmit.render;

import com.ntmit.entity.EntityBlastChunk;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class RenderBlastChunk extends Render<EntityBlastChunk> {

	private static final float DARKEN = 0.72F;

	public RenderBlastChunk(RenderManager renderManager) {
		super(renderManager);
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityBlastChunk entity) {
		return TextureMap.LOCATION_BLOCKS_TEXTURE;
	}

	@Override
	public void doRender(EntityBlastChunk entity, double x, double y, double z, float entityYaw, float partialTicks) {

		if (entity.count <= 0) return;
		float roll = entity.prevRoll + (entity.roll - entity.prevRoll) * partialTicks;
		float tumble = entity.prevTumble + (entity.tumble - entity.prevTumble) * partialTicks;
		int light = entity.getBrightnessForRender();
		Minecraft mc = Minecraft.getMinecraft();
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(roll, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotate(tumble, 1.0F, 0.0F, 0.0F);
		GlStateManager.enableRescaleNormal();
		GlStateManager.disableCull();
		RenderHelper.enableStandardItemLighting();
		mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		int mode = -1;
		try {
			for (int i = 0; i < entity.count; i++) {
				IBlockState state = Block.getStateById(entity.states[i]);
				if (state == null) continue;
				int ox = entity.offsets[i * 3];
				int oy = entity.offsets[i * 3 + 1];
				int oz = entity.offsets[i * 3 + 2];
				for (BakedQuad quad : collectQuads(mc, state)) {
					int[] data = quad.getVertexData();
					int quadMode = data.length / 4 >= 8 ? 8 : 7;
					if (mode != quadMode) {
						if (mode != -1) tessellator.draw();
						buffer.begin(GL11.GL_QUADS,
								quadMode == 8 ? DefaultVertexFormats.BLOCK : DefaultVertexFormats.ITEM);
						mode = quadMode;
					}
					addQuadData(buffer, data, ox, oy, oz, light, quadMode);
				}
			}
			if (mode != -1) tessellator.draw();
		} finally {
			GlStateManager.enableCull();
			RenderHelper.disableStandardItemLighting();
			GlStateManager.disableRescaleNormal();
			GlStateManager.popMatrix();
		}
	}

	private static List<BakedQuad> collectQuads(Minecraft mc, IBlockState state) {

		IBakedModel model = mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
		List<BakedQuad> quads = new ArrayList<>();
		quads.addAll(model.getQuads(state, null, 0L));
		for (EnumFacing facing : EnumFacing.values()) {
			quads.addAll(model.getQuads(state, facing, 0L));
		}
		return quads;
	}

	private static void addQuadData(BufferBuilder buffer, int[] data, int ox, int oy, int oz, int light, int mode) {

		int stride = data.length / 4;
		if (stride < 3) return;
		int[] copy = new int[data.length];
		System.arraycopy(data, 0, copy, 0, data.length);
		int lu = light & 0xFFFF;
		int lv = (light >> 16) & 0xFFFF;
		for (int v = 0; v < 4; v++) {
			int o = v * stride;
			if (o + 2 >= copy.length) return;
			copy[o] = Float.floatToRawIntBits(Float.intBitsToFloat(copy[o]) + (float) ox);
			copy[o + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(copy[o + 1]) + (float) oy);
			copy[o + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(copy[o + 2]) + (float) oz);
			if (mode == 8 && o + 6 < copy.length) copy[o + 6] = lu | (lv << 16);
			if (o + 3 < copy.length) {
				int color = copy[o + 3];
				int r = (int) ((float) (color & 0xFF) * DARKEN);
				int g = (int) ((float) ((color >> 8) & 0xFF) * DARKEN);
				int b = (int) ((float) ((color >> 16) & 0xFF) * DARKEN);
				copy[o + 3] = (r & 0xFF) | ((g & 0xFF) << 8) | ((b & 0xFF) << 16) | (color & 0xFF000000);
			}
		}
		buffer.addVertexData(copy);
	}
}
