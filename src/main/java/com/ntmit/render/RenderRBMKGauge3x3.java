package com.ntmit.render;

import com.hbm.render.loader.GroupHandle;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.WaveFrontObjectVAO;
import com.hbm.render.tileentity.RenderArcFurnace;
import com.hbm.util.BobMathUtil;
import com.hbm.util.ColorUtil;
import com.ntmit.lib.RefStrings;
import com.ntmit.tileentity.TileEntityRBMKGauge3x3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderRBMKGauge3x3 extends TileEntitySpecialRenderer<TileEntityRBMKGauge3x3> {

	private static final double CELL_SPACING = 0.31625D;

	private static final float CELL_SCALE = 0.6615F;


	private static final double DIAL_SHIFT_Z = 0.0D;

	private static final double DIAL_LIFT = 0.12D;

	private static final float LABEL_SCALE = 0.009F;

	private static final double LABEL_Y = 0.365D;

	private static final double NEEDLE_SWEEP = 70.5D;
	private static final double NEEDLE_LIMIT = 80.5D;

	private static final double NEEDLE_BASE = 85.0D;

	private static final double[] SCALE_NUMBER_ANGLE = { 10.0D, 80.5D };

	private static final WaveFrontObjectVAO GAUGE_MODEL =
			new HFRWavefrontObject(new ResourceLocation(RefStrings.MODID, "models/rbmk/gauge_3x3.obj")).asVBO();
	private static final GroupHandle GAUGE_FACE = GAUGE_MODEL.resolve("Gauge");
	private static final GroupHandle GAUGE_RIM = GAUGE_MODEL.resolve("Rim");
	private static final GroupHandle GAUGE_NEEDLE = GAUGE_MODEL.resolve("Needle");

	private static final ResourceLocation GAUGE_TEXTURE =
			new ResourceLocation(RefStrings.MODID, "textures/models/rbmk_gauge_3x3.png");

	@Override
	public void render(TileEntityRBMKGauge3x3 te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5D, y, z + 0.5D);
		GlStateManager.enableCull();
		GlStateManager.disableBlend();

		EnumFacing facing = te.getWorld().getBlockState(te.getPos()).getValue(com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase.FACING);
		switch (facing) {
			case NORTH: GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F); break;
			case WEST: GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F); break;
			case SOUTH: GlStateManager.rotate(270.0F, 0.0F, 1.0F, 0.0F); break;
			case EAST: GlStateManager.rotate(0.0F, 0.0F, 1.0F, 0.0F); break;
			default: break;
		}

		for (int i = 0; i < TileEntityRBMKGauge3x3.UNITS; i++) {
			TileEntityRBMKGauge3x3.GaugeUnit unit = te.units[i];
			if (!unit.active) continue;

			int row = i / TileEntityRBMKGauge3x3.COLUMNS;
			int col = i % TileEntityRBMKGauge3x3.COLUMNS;

			GlStateManager.pushMatrix();
			GlStateManager.translate(0.25D,
					DIAL_LIFT + (1 - row) * CELL_SPACING,
					DIAL_SHIFT_Z + (1 - col) * CELL_SPACING);
			GlStateManager.scale(CELL_SCALE, CELL_SCALE, CELL_SCALE);
			GlStateManager.color(1.0F, 1.0F, 1.0F);
			this.bindTexture(GAUGE_TEXTURE);
			GAUGE_FACE.render();

			GlStateManager.color(ColorUtil.fr(unit.color), ColorUtil.fg(unit.color), ColorUtil.fb(unit.color));
			GAUGE_RIM.render();

			this.renderNeedle(unit, partialTicks);

			GlStateManager.popMatrix();
		}

		GlStateManager.popMatrix();
	}

	private void renderNeedle(TileEntityRBMKGauge3x3.GaugeUnit unit, float partialTicks) {
		GlStateManager.pushMatrix();
		GlStateManager.color(ColorUtil.fr(unit.color), ColorUtil.fg(unit.color), ColorUtil.fb(unit.color));

		double value = unit.lastRenderValue + (unit.renderValue - unit.lastRenderValue) * (double) partialTicks;
		long lower = Math.min(unit.min, unit.max);
		long upper = Math.max(unit.min, unit.max);
		if (lower == upper) ++upper;

		double angle = (value - (double) lower) / (double) (upper - lower) * NEEDLE_SWEEP;
		if (unit.min > unit.max) angle = NEEDLE_SWEEP - angle;
		angle = MathHelper.clamp(angle, 0.0D, NEEDLE_LIMIT);

		GlStateManager.translate(0.0D, 0.4375D, -0.125D);
		GlStateManager.rotate((float) (angle - NEEDLE_BASE), -1.0F, 0.0F, 0.0F);
		GlStateManager.translate(0.0D, -0.4375D, 0.125D);
		GlStateManager.disableTexture2D();
		RenderArcFurnace.fullbright(true);
		GAUGE_NEEDLE.render();
		RenderArcFurnace.fullbright(false);
		GlStateManager.enableTexture2D();
		GlStateManager.popMatrix();

		FontRenderer font = Minecraft.getMinecraft().fontRenderer;
		int height = font.FONT_HEIGHT;
		double lineScale = 0.0025D;
		String lineLower = unit.min <= 10000L ? Long.toString(unit.min) : BobMathUtil.getShortNumber(unit.min);
		String lineUpper = unit.max <= 10000L ? Long.toString(unit.max) : BobMathUtil.getShortNumber(unit.max);

		for (int j = 0; j < 2; ++j) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(0.0D, 0.4375D, -0.125D);
			GlStateManager.rotate((float) SCALE_NUMBER_ANGLE[j], -1.0F, 0.0F, 0.0F);
			GlStateManager.translate(0.0D, -0.4375D, 0.125D);
			GlStateManager.translate(0.032D, 0.4375D, 0.125D);
			GlStateManager.scale(lineScale, -lineScale, lineScale);
			GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
			font.drawString(j == 0 ? lineLower : lineUpper, 0, -height / 2, 0);
			GlStateManager.popMatrix();
		}

		if (unit.label != null && !unit.label.isEmpty()) {
			GlStateManager.translate(0.01D, LABEL_Y, 0.0D);
			int width = font.getStringWidth(unit.label);
			float fit = Math.min(LABEL_SCALE, 0.4F / (float) Math.max(width, 1));
			GlStateManager.scale(fit, -fit, fit);
			GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
			RenderArcFurnace.fullbright(true);
			font.drawString(unit.label, -width / 2, -height / 2, 65280);
			RenderArcFurnace.fullbright(false);
		}
	}
}
