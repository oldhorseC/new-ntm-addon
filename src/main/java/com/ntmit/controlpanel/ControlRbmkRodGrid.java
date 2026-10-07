package com.ntmit.controlpanel;

import com.hbm.inventory.control_panel.Control;
import com.hbm.inventory.control_panel.ControlEvent;
import com.hbm.inventory.control_panel.ControlPanel;
import com.hbm.inventory.control_panel.ControlRegistry;
import com.hbm.inventory.control_panel.GuiControlEdit;
import com.hbm.inventory.control_panel.controls.ControlType;
import com.hbm.inventory.control_panel.controls.configs.SubElementBaseConfig;
import com.hbm.inventory.control_panel.types.DataValue;
import com.hbm.inventory.control_panel.types.DataValueFloat;
import com.hbm.inventory.control_panel.types.DataValueString;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.IModelCustom;
import com.hbm.render.loader.WaveFrontObjectVAO;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKControlManual;
import com.ntmit.tileentity.TileEntityITServerCore;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ControlRbmkRodGrid extends Control {

	public static final float DEFAULT_WIDTH = 3.0F;
	public static final float DEFAULT_LENGTH = 3.0F;
	public static final float MIN_SIZE = 1.0F;
	public static final float MAX_SIZE = 16.0F;
	private static final float THICKNESS = 0.2F;

	public ControlRbmkRodGrid(String name, String registryName, ControlPanel panel) {
		super(name, registryName, panel);
		this.ensureSizeConfigs();
	}

	private void ensureSizeConfigs() {
		if (!(this.configMap.get("width") instanceof DataValueFloat)) {
			this.configMap.put("width", new DataValueFloat(DEFAULT_WIDTH));
		}
		if (!(this.configMap.get("length") instanceof DataValueFloat)) {
			this.configMap.put("length", new DataValueFloat(DEFAULT_LENGTH));
		}
	}

	private static float clampSize(float v) {
		if (Float.isNaN(v)) return MIN_SIZE;
		return Math.max(MIN_SIZE, Math.min(MAX_SIZE, v));
	}

	public float sizeW() {
		DataValue v = this.configMap.get("width");
		return v == null ? DEFAULT_WIDTH : clampSize(v.getNumber());
	}

	public float sizeL() {
		DataValue v = this.configMap.get("length");
		return v == null ? DEFAULT_LENGTH : clampSize(v.getNumber());
	}

	@Override
	public ControlType getControlType() {
		return ControlType.METER;
	}

	@Override
	public float[] getSize() {
		return new float[]{this.sizeW(), this.sizeL(), THICKNESS};
	}

	@SideOnly(Side.CLIENT)
	@Override
	public SubElementBaseConfig getConfigSubElement(GuiControlEdit gui, java.util.Map<String, DataValue> configs) {
		return new SubElementRbmkRodGrid(gui, configs);
	}

	@Override
	public List<String> getOutEvents() {
		return Collections.singletonList("ctrl_press");
	}

	private static final String HIT_X = "ntmitHitX";
	private static final String HIT_Z = "ntmitHitZ";
	public static final String VAR_PRESS = "ntmitPress";
	public static final String VAR_CELLS = "ntmitCells";

	@Override
	public void receiveEvent(ControlEvent evt) {
		super.receiveEvent(evt);
		if (evt == null || !"ctrl_press".equals(evt.name)) return;

		DataValue hx = evt.vars.get(HIT_X);
		DataValue hz = evt.vars.get(HIT_Z);
		if (hx == null || hz == null) {
			float press = this.getVar(VAR_PRESS).getNumber();
			setVar(VAR_PRESS, press > 0.5F ? 0.0F : 1.0F);
			return;
		}
		this.toggleCell(hx.getNumber(), hz.getNumber());
	}

	private java.util.Set<String> pressedCells() {
		java.util.Set<String> set = new java.util.HashSet<>();
		String s = this.getVar(VAR_CELLS) == null ? "" : this.getVar(VAR_CELLS).toString();
		if (s.isEmpty()) return set;
		for (String part : s.split(",")) {
			if (!part.isEmpty()) set.add(part);
		}
		return set;
	}

	private void toggleCell(float hitX, float hitZ) {
		int[] cell = this.hitToCell(hitX, hitZ);
		if (cell == null) return;

		java.util.Set<String> set = this.pressedCells();
		String key = cell[0] + ":" + cell[1];
		if (!set.remove(key)) set.add(key);

		StringBuilder sb = new StringBuilder();
		for (String s : set) {
			if (sb.length() > 0) sb.append(',');
			sb.append(s);
		}
		this.vars.put(VAR_CELLS, new DataValueString(sb.toString()));
		this.customVarNames.add(VAR_CELLS);
	}

	@SideOnly(Side.CLIENT)
	private int[] hitToCell(float hitX, float hitZ) {
		java.util.List<int[]> cells = this.rodCells();
		if (cells.isEmpty()) return null;

		float px = hitX;
		float pz = hitZ;
		if (this.panel != null && this.panel.inv_transform != null) {
			org.lwjgl.util.vector.Vector4f v =
					new org.lwjgl.util.vector.Vector4f(px, 0.0F, pz, 1.0F);
			org.lwjgl.util.vector.Matrix4f.transform(this.panel.inv_transform, v, v);
			px = v.x;
			pz = v.z;
		}

		float[] lay = this.layout(cells);
		float cx = lay[0];
		float cz = lay[1];
		float cell = lay[2];
		float half = lay[3];
		float pad = lay[4];

		float localX = px - this.posX;
		float localZ = pz - this.posY;

		int[] best = null;
		float bestDist = Float.MAX_VALUE;
		for (int[] c : cells) {
			float x0 = (c[0] - cx) * cell - half + pad;
			float z0 = (c[1] - cz) * cell - half + pad;
			float x1 = x0 + cell - pad * 2.0F;
			float z1 = z0 + cell - pad * 2.0F;

			if (localX >= x0 && localX <= x1 && localZ >= z0 && localZ <= z1) return new int[]{c[0], c[1]};

			float dx = localX - (x0 + x1) * 0.5F;
			float dz = localZ - (z0 + z1) * 0.5F;
			float d = dx * dx + dz * dz;
			if (d < bestDist) {
				bestDist = d;
				best = new int[]{c[0], c[1]};
			}
		}
		return best;
	}

	private float[] layout(java.util.List<int[]> cells) {
		int minDx = Integer.MAX_VALUE;
		int minDz = Integer.MAX_VALUE;
		int maxDx = Integer.MIN_VALUE;
		int maxDz = Integer.MIN_VALUE;
		for (int[] c : cells) {
			if (c[0] < minDx) minDx = c[0];
			if (c[0] > maxDx) maxDx = c[0];
			if (c[1] < minDz) minDz = c[1];
			if (c[1] > maxDz) maxDz = c[1];
		}
		float width = this.sizeW();
		float length = this.sizeL();
		int cols = Math.max(1, maxDx - minDx + 1);
		int rows = Math.max(1, maxDz - minDz + 1);
		float cell = Math.min(width / cols, length / rows);
		float pad = Math.max(0.005F, cell * 0.08F);
		return new float[]{(minDx + maxDx) / 2.0F, (minDz + maxDz) / 2.0F, cell, cell * 0.5F, pad};
	}

	private void setVar(String name, float value) {
		this.vars.put(name, new DataValueFloat(value));
		this.customVarNames.add(name);
	}

	private static final int PRESS_TICKS = 10;

	private float lastSeenPress = -1.0F;
	private long pressStartTick = Long.MIN_VALUE;

	@SideOnly(Side.CLIENT)
	private boolean isPressed() {
		return this.getVar(VAR_PRESS).getNumber() > 0.5F;
	}

	@Override
	public Control newControl(ControlPanel panel) {
		return new ControlRbmkRodGrid(this.name, this.registryName, panel);
	}


	private long cacheTime = -1L;
	private List<int[]> cacheCells = Collections.emptyList();

	@SideOnly(Side.CLIENT)
	@Override
	public IModelCustom getModel() {
		return ResourceManager.ctrl_button_push;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public ResourceLocation getGuiTexture() {
		return ResourceManager.ctrl_indicator_lamp_gui_tex;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void render() {
		List<int[]> cells = this.rodCells();
		if (cells.isEmpty()) return;

		float[] lay = this.layout(cells);
		float cx = lay[0];
		float cz = lay[1];
		float cell = lay[2];
		float half = lay[3];
		float pad = lay[4];

		float boxH = cell * 0.55F;
		final float dim = 0.45F;

		GlStateManager.pushMatrix();
		GlStateManager.translate(this.posX, 0.0D, this.posY);
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();

		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buf = tess.getBuffer();
		buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

		for (int[] c : cells) {
			float x0 = (c[0] - cx) * cell - half + pad;
			float z0 = (c[1] - cz) * cell - half + pad;
			float x1 = x0 + cell - pad * 2.0F;
			float z1 = z0 + cell - pad * 2.0F;

			float r = (((c[2] >> 16) & 0xFF) / 255.0F) * dim;
			float g = (((c[2] >> 8) & 0xFF) / 255.0F) * dim;
			float b = ((c[2] & 0xFF) / 255.0F) * dim;

			this.box(buf, x0, 0.0F, z0, x1, boxH, z1, r, g, b);
		}

		tess.draw();
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	@SideOnly(Side.CLIENT)
	private void box(BufferBuilder buf, float x0, float y0, float z0, float x1, float y1, float z1,
			float r, float g, float b) {
		quad(buf, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, 1.00F);
		quad(buf, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, 0.72F);
		quad(buf, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, r, g, b, 0.64F);
		quad(buf, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, r, g, b, 0.84F);
		quad(buf, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r, g, b, 0.56F);
	}

	@SideOnly(Side.CLIENT)
	private void quad(BufferBuilder buf, float ax, float ay, float az, float bx, float by, float bz,
			float cx, float cy, float cz, float dx, float dy, float dz, float r, float g, float b, float mul) {
		buf.pos(ax, ay, az).color(r * mul, g * mul, b * mul, 1.0F).endVertex();
		buf.pos(bx, by, bz).color(r * mul, g * mul, b * mul, 1.0F).endVertex();
		buf.pos(cx, cy, cz).color(r * mul, g * mul, b * mul, 1.0F).endVertex();
		buf.pos(dx, dy, dz).color(r * mul, g * mul, b * mul, 1.0F).endVertex();
	}


	private static final int SCAN_RADIUS = TileEntityITServerCore.RBMK_RADIUS;

	private BlockPos anchorRod() {
		for (Map.Entry<String, BlockPos> e : this.taggedLinks.entrySet()) {
			BlockPos p = e.getValue();
			if (p != null && !BlockPos.ORIGIN.equals(p)) return p;
		}
		return null;
	}

	@SideOnly(Side.CLIENT)
	private BlockPos resolveAnchor(net.minecraft.world.World world) {
		BlockPos bound = this.anchorRod();
		if (bound == null) return null;

		TileEntity te = world.getTileEntity(bound);
		if (te instanceof TileEntityITServerCore core) {
			for (BlockPos p : core.getBoundRbmkPositions()) return p;
		}
		return bound;
	}

	@SideOnly(Side.CLIENT)
	private List<int[]> rodCells() {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.world == null) return Collections.emptyList();

		long now = mc.world.getTotalWorldTime();
		if (this.cacheTime == now) return this.cacheCells;
		this.cacheTime = now;

		List<int[]> cells = new ArrayList<>();
		BlockPos anchor = this.resolveAnchor(mc.world);

		if (anchor != null) {
			for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
				for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
					TileEntity te = mc.world.getTileEntity(anchor.add(dx, 0, dz));
					if (!(te instanceof TileEntityRBMKControlManual control)) continue;
					int ordinal = control.color == null ? -1 : control.color.ordinal();
					cells.add(new int[]{dx, dz, RbmkRodPalette.colorOf(ordinal)});
				}
			}
		}

		this.cacheCells = cells;
		return cells;
	}

	@Override
	public void populateDefaultNodes(List<ControlEvent> receiveEvents) {
	}
}

