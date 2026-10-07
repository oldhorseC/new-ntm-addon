package com.ntmit.render;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.render.model.AbstractRBMKLiddedBakedModel;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKControl;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKControlAuto;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKControlManual;
import com.ntmit.entity.EntityLidPlate;
import com.ntmit.lib.RbmkJumpHandler;

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
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class RenderLidPlate extends Render<EntityLidPlate> {

	private static Method buildWorldQuads;
	private static Method lookupGet;
	private static boolean reflectTried;

	public RenderLidPlate(RenderManager renderManager) {
		super(renderManager);
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityLidPlate entity) {
		return TextureMap.LOCATION_BLOCKS_TEXTURE;
	}

	@Override
	public void doRender(EntityLidPlate entity, double x, double y, double z, float entityYaw, float partialTicks) {
		if (entity.count <= 0) return;
		float roll = entity.prevRoll + (entity.roll - entity.prevRoll) * partialTicks;
		float tumble = entity.prevTumble + (entity.tumble - entity.prevTumble) * partialTicks;
		int light = plateLight(entity);
		Minecraft mc = Minecraft.getMinecraft();
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
		GlStateManager.rotate(tumble, 1.0F, 0.0F, 0.0F);
		GlStateManager.enableRescaleNormal();
		GlStateManager.disableCull();
		RenderHelper.enableStandardItemLighting();
		mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		int mode = -1;
		for (int i = 0; i < entity.count; i++) {
			IBlockState state = Block.getStateById(entity.states[i]);
			if (state == null) continue;
			Vec3i offset = entity.offset(i);
			boolean special = entity.isSpecialPlate();
			float darkR = special ? RbmkJumpHandler.SPECIAL_PLATE_R : RbmkJumpHandler.PLATE_DARKEN;
			float darkG = special ? RbmkJumpHandler.SPECIAL_PLATE_G : RbmkJumpHandler.PLATE_DARKEN;
			float darkB = special ? RbmkJumpHandler.SPECIAL_PLATE_B : RbmkJumpHandler.PLATE_DARKEN;
			for (BakedQuad quad : collectQuads(mc, state)) {
				int[] data = quad.getVertexData();
				int quadMode = data.length / 4 >= 8 ? 8 : 7;
				if (mode != quadMode) {
					if (mode != -1) tessellator.draw();
					buffer.begin(GL11.GL_QUADS, quadMode == 8 ? DefaultVertexFormats.BLOCK : DefaultVertexFormats.ITEM);
					mode = quadMode;
				}
				addQuadData(buffer, data, offset.getX(), offset.getY(), offset.getZ(), light, quadMode, darkR, darkG, darkB);
			}
		}
		if (mode != -1) tessellator.draw();
		for (int i = 0; i < entity.count; i++) {
			if (!entity.isRod(i)) continue;
			renderRod(entity, i);
		}
		GlStateManager.enableCull();
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableRescaleNormal();
		GlStateManager.popMatrix();
	}

	private static TileEntityRBMKControlManual ntmitRodManual;
	private static TileEntityRBMKControlAuto ntmitRodAuto;
	private static boolean ntmitRodFailed;

	private static int plateLight(EntityLidPlate entity) {
		int light = entity.getBrightnessForRender();
		int sky = (light >> 16) & 0xFFFF;
		int block = light & 0xFFFF;
		int baseX = (int) Math.floor(entity.posX);
		int baseY = (int) Math.floor(entity.posY);
		int baseZ = (int) Math.floor(entity.posZ);
		for (int i = 0; i < entity.count; i++) {
			Vec3i offset = entity.offset(i);
			for (int dy = 0; dy <= 2; dy++) {
				BlockPos pos = new BlockPos(baseX + offset.getX(), baseY + offset.getY() + dy, baseZ + offset.getZ());
				if (!entity.world.isBlockLoaded(pos)) continue;
				int other = entity.world.getCombinedLight(pos, 0);
				sky = Math.max(sky, (other >> 16) & 0xFFFF);
				block = Math.max(block, other & 0xFFFF);
			}
		}
		return (sky << 16) | block;
	}

	private static void renderRod(EntityLidPlate entity, int index) {
		if (ntmitRodFailed) return;
		try {
			Vec3i offset = entity.offset(index);
			int columnHeight = Math.max(RBMKDials.getColumnHeight(entity.world), 1);
			float lift = entity.lift(index);
			byte ordinal = entity.rodColor(index);
			TileEntityRBMKControl rod;
			if (ordinal == -1) {
				if (ntmitRodAuto == null) ntmitRodAuto = new TileEntityRBMKControlAuto();
				rod = ntmitRodAuto;
			} else {
				if (ntmitRodManual == null) ntmitRodManual = new TileEntityRBMKControlManual();
				TileEntityRBMKControlManual.RBMKColor[] values = TileEntityRBMKControlManual.RBMKColor.values();
				ntmitRodManual.color = ordinal >= 0 ? values[Math.abs(ordinal) % values.length] : null;
				rod = ntmitRodManual;
			}
			rod.setWorld(entity.world);
			rod.setPos(new BlockPos((int) Math.floor(entity.posX) + offset.getX(), (int) Math.floor(entity.posY) + offset.getY() - columnHeight, (int) Math.floor(entity.posZ) + offset.getZ()));
			rod.lastLevel = lift;
			rod.level = lift;
			boolean specialRod = entity.isSpecialPlate();
			GlStateManager.color(specialRod ? RbmkJumpHandler.SPECIAL_PLATE_R : RbmkJumpHandler.PLATE_DARKEN, specialRod ? RbmkJumpHandler.SPECIAL_PLATE_G : RbmkJumpHandler.PLATE_DARKEN, specialRod ? RbmkJumpHandler.SPECIAL_PLATE_B : RbmkJumpHandler.PLATE_DARKEN, 1.0F);
			try {
				TileEntityRendererDispatcher.instance.render(rod, (double) offset.getX(), (double) (offset.getY() - columnHeight), (double) offset.getZ(), 0.0F, -1, 1.0F);
			} finally {
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			}
		} catch (Throwable throwable) {
			ntmitRodFailed = true;
			RbmkJumpHandler.diag("plate rod render failed: " + throwable);
		}
	}

	private static List<BakedQuad> collectQuads(Minecraft mc, IBlockState state) {
		IBakedModel model = mc.getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
		List<BakedQuad> quads = new ArrayList<>();
		if (initReflection() && model instanceof AbstractRBMKLiddedBakedModel) {
			try {
				int lidType = RBMKBase.metaToLid(state.getBlock().getMetaFromState(state));
				if (lidType <= 0) lidType = 1;
				Object lookup = buildWorldQuads.invoke(model, lidType, 1);
				if (lookup != null) {
					Object general = lookupGet.invoke(lookup, new Object[] { null });
					if (general instanceof List) quads.addAll((List<BakedQuad>) general);
					for (EnumFacing facing : EnumFacing.values()) {
						Object side = lookupGet.invoke(lookup, facing);
						if (side instanceof List) quads.addAll((List<BakedQuad>) side);
					}
					if (!quads.isEmpty()) return quads;
				}
			} catch (Throwable throwable) {
			}
			quads.clear();
		}
		quads.addAll(model.getQuads(state, null, 0L));
		for (EnumFacing facing : EnumFacing.values()) {
			quads.addAll(model.getQuads(state, facing, 0L));
		}
		return quads;
	}

	private static boolean initReflection() {
		if (!reflectTried) {
			reflectTried = true;
			try {
				buildWorldQuads = AbstractRBMKLiddedBakedModel.class.getDeclaredMethod("buildWorldQuads", int.class, int.class);
				buildWorldQuads.setAccessible(true);
				Class<?> lookup = Class.forName("com.hbm.render.model.AbstractRBMKLiddedBakedModel$QuadLookup");
				lookupGet = lookup.getDeclaredMethod("get", EnumFacing.class);
				lookupGet.setAccessible(true);
			} catch (Throwable throwable) {
				buildWorldQuads = null;
				lookupGet = null;
			}
		}
		return buildWorldQuads != null && lookupGet != null;
	}

	private static void addQuadData(BufferBuilder buffer, int[] data, int ox, int oy, int oz, int light, int mode, float darkR, float darkG, float darkB) {
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
				int r = (int) ((float) (color & 0xFF) * darkR);
				int g = (int) ((float) ((color >> 8) & 0xFF) * darkG);
				int b = (int) ((float) ((color >> 16) & 0xFF) * darkB);
				copy[o + 3] = (r & 0xFF) | ((g & 0xFF) << 8) | ((b & 0xFF) << 16) | (color & 0xFF000000);
			}
		}
		buffer.addVertexData(copy);
	}
}
