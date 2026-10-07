package com.ntmit.render;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.RBMKDebris;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.ntmit.entity.EntityLidPlate;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class RbmkDebugRender {

	public static boolean enabled = false;
	public static double range = 128.0D;

	private static final int MAX_COLUMN_SCAN = 64;
	private static final int MIN_COLUMN_HEIGHT = 2;
	private static final int MAX_COLUMN_HEIGHT = 16;
	private static final int DEBRIS_RADIUS = 32;
	private static final int DEBRIS_DOWN = 16;
	private static final int DEBRIS_UP = 16;
	private static final int DEBRIS_LIFETIME = 100;
	private static final byte KIND_COLUMN = 1;
	private static final byte KIND_DEBRIS = 2;
	private static final byte KIND_CORIUM = 3;

	private static final Map<Long, Byte> cells = new HashMap<>();
	private static final Set<Long> edgesX = new HashSet<>();
	private static final Set<Long> edgesY = new HashSet<>();
	private static final Set<Long> edgesZ = new HashSet<>();
	private static final Set<Long> visitedColumns = new HashSet<>();
	private static final Map<Long, Integer> debris = new HashMap<>();
	private static final Map<Long, Integer> corium = new HashMap<>();
	private static long scanTick = Long.MIN_VALUE;

	public static void render(RenderWorldLastEvent event) {
		if (!enabled) return;
		Minecraft mc = Minecraft.getMinecraft();
		World world = mc.world;
		EntityPlayer player = mc.player;
		if (world == null || player == null) return;
		float interp = event.getPartialTicks();
		double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * interp;
		double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * interp;
		double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * interp;
		long tick = world.getTotalWorldTime();
		if (tick != scanTick) {
			scanTick = tick;
			scanDebris(world, player, tick);
		}
		int height = MathHelper.clamp(RBMKDials.getColumnHeightRuleValue(world), MIN_COLUMN_HEIGHT, MAX_COLUMN_HEIGHT);
		double rangeSq = range * range;
		Frustum frustum = makeFrustum(px, py, pz);
		cells.clear();
		visitedColumns.clear();
		for (TileEntity te : world.loadedTileEntityList) {
			if (!(te instanceof TileEntityRBMKBase)) continue;
			BlockPos pos = te.getPos();
			if (!world.isBlockLoaded(pos)) continue;
			BlockPos base = columnBase(world, pos);
			if (!visitedColumns.add(base.toLong())) continue;
			AxisAlignedBB box = columnBox(base, height);
			if (box.getCenter().squareDistanceTo(px, py, pz) > rangeSq) continue;
			if (frustum != null && !frustum.isBoundingBoxInFrustum(box)) continue;
			for (int i = 0; i < height; i++) cells.put(cellKey(base.getX(), base.getY() + i, base.getZ()), KIND_COLUMN);
		}
		for (Long key : debris.keySet()) {
			AxisAlignedBB box = new AxisAlignedBB(BlockPos.fromLong(key.longValue()));
			if (box.getCenter().squareDistanceTo(px, py, pz) > rangeSq) continue;
			if (frustum != null && !frustum.isBoundingBoxInFrustum(box)) continue;
			cells.putIfAbsent(key, KIND_DEBRIS);
		}
		for (Long key : corium.keySet()) {
			AxisAlignedBB box = new AxisAlignedBB(BlockPos.fromLong(key.longValue()));
			if (box.getCenter().squareDistanceTo(px, py, pz) > rangeSq) continue;
			if (frustum != null && !frustum.isBoundingBoxInFrustum(box)) continue;
			cells.putIfAbsent(key, KIND_CORIUM);
		}
		setup();
		try {
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder buffer = tessellator.getBuffer();
			edgesX.clear();
			edgesY.clear();
			edgesZ.clear();
			buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
			for (Map.Entry<Long, Byte> entry : cells.entrySet()) {
				byte kind = entry.getValue().byteValue();
				float red = kind == KIND_DEBRIS ? 0.0F : 1.0F;
				float green = kind == KIND_CORIUM ? 0.0F : 1.0F;
				float blue = kind == KIND_COLUMN ? 1.0F : 0.0F;
				emitCell(buffer, BlockPos.fromLong(entry.getKey().longValue()), red, green, blue, -px, -py, -pz);
			}
			tessellator.draw();
			for (Entity entity : world.loadedEntityList) {
				if (!(entity instanceof EntityLidPlate)) continue;
				EntityLidPlate plate = (EntityLidPlate) entity;
				AxisAlignedBB test = plate.crushBox();
				if (test.getCenter().squareDistanceTo(px, py, pz) > rangeSq) continue;
				AxisAlignedBB local = plateModelBox(plate);
				if (local == null) continue;
				float roll = plate.prevRoll + (plate.roll - plate.prevRoll) * interp;
				float tumble = plate.prevTumble + (plate.tumble - plate.prevTumble) * interp;
				GlStateManager.pushMatrix();
				GlStateManager.translate(plate.posX - px, plate.posY - py, plate.posZ - pz);
				GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
				GlStateManager.rotate(tumble, 1.0F, 0.0F, 0.0F);
				buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
				emitBox(buffer, local);
				tessellator.draw();
				GlStateManager.popMatrix();
			}
		} finally {
			cleanup();
		}
	}

	private static void scanDebris(World world, EntityPlayer player, long tick) {
		int span = DEBRIS_DOWN + DEBRIS_UP + 1;
		int layer = (int) (tick % (long) span);
		int y = MathHelper.floor(player.posY) - DEBRIS_DOWN + layer;
		int px = MathHelper.floor(player.posX);
		int pz = MathHelper.floor(player.posZ);
		for (int dx = -DEBRIS_RADIUS; dx <= DEBRIS_RADIUS; dx++) {
			for (int dz = -DEBRIS_RADIUS; dz <= DEBRIS_RADIUS; dz++) {
				BlockPos pos = new BlockPos(px + dx, y, pz + dz);
				if (!world.isBlockLoaded(pos)) continue;
				Block block = world.getBlockState(pos).getBlock();
				if (block instanceof RBMKDebris) {
					debris.put(pos.toLong(), (int) tick);
				} else if (isCorium(block)) {
					corium.put(pos.toLong(), (int) tick);
				}
			}
		}
		if (layer == 0) {
			debris.entrySet().removeIf(entry -> tick - (long) entry.getValue().intValue() > (long) DEBRIS_LIFETIME);
			corium.entrySet().removeIf(entry -> tick - (long) entry.getValue().intValue() > (long) DEBRIS_LIFETIME);
		}
	}

	private static boolean isCorium(Block block) {
		ResourceLocation name = block.getRegistryName();
		return name != null && name.getPath().contains("corium");
	}

	private static BlockPos columnBase(World world, BlockPos pos) {
		BlockPos base = pos;
		for (int i = 0; i < MAX_COLUMN_SCAN; i++) {
			BlockPos below = base.down();
			if (!world.isBlockLoaded(below)) break;
			if (!(world.getBlockState(below).getBlock() instanceof RBMKBase)) break;
			base = below;
		}
		return base;
	}

	private static AxisAlignedBB columnBox(BlockPos base, int height) {
		return new AxisAlignedBB((double) base.getX(), (double) base.getY(), (double) base.getZ(),
				(double) base.getX() + 1.0D, (double) base.getY() + (double) height, (double) base.getZ() + 1.0D);
	}

	private static AxisAlignedBB plateModelBox(EntityLidPlate plate) {
		if (plate.count <= 0) return null;
		int minX = 0;
		int maxX = 0;
		int minZ = 0;
		int maxZ = 0;
		for (int i = 0; i < plate.count; i++) {
			Vec3i offset = plate.offset(i);
			minX = Math.min(minX, offset.getX());
			maxX = Math.max(maxX, offset.getX());
			minZ = Math.min(minZ, offset.getZ());
			maxZ = Math.max(maxZ, offset.getZ());
		}
		return new AxisAlignedBB((double) minX, 0.0D, (double) minZ, (double) maxX + 1.0D, 1.0D, (double) maxZ + 1.0D);
	}

	private static void emitCell(BufferBuilder buffer, BlockPos pos, float r, float g, float b, double ox, double oy, double oz) {
		int x0 = pos.getX();
		int y0 = pos.getY();
		int z0 = pos.getZ();
		int x1 = x0 + 1;
		int y1 = y0 + 1;
		int z1 = z0 + 1;
		if (!cells.containsKey(cellKey(x1, y0, z0))) {
			edge(buffer, edgesY, r, g, b, ox, oy, oz, x1, y0, z0, x1, y1, z0);
			edge(buffer, edgesY, r, g, b, ox, oy, oz, x1, y0, z1, x1, y1, z1);
			edge(buffer, edgesZ, r, g, b, ox, oy, oz, x1, y0, z0, x1, y0, z1);
			edge(buffer, edgesZ, r, g, b, ox, oy, oz, x1, y1, z0, x1, y1, z1);
		}
		if (!cells.containsKey(cellKey(x0 - 1, y0, z0))) {
			edge(buffer, edgesY, r, g, b, ox, oy, oz, x0, y0, z0, x0, y1, z0);
			edge(buffer, edgesY, r, g, b, ox, oy, oz, x0, y0, z1, x0, y1, z1);
			edge(buffer, edgesZ, r, g, b, ox, oy, oz, x0, y0, z0, x0, y0, z1);
			edge(buffer, edgesZ, r, g, b, ox, oy, oz, x0, y1, z0, x0, y1, z1);
		}
		if (!cells.containsKey(cellKey(x0, y1, z0))) {
			edge(buffer, edgesX, r, g, b, ox, oy, oz, x0, y1, z0, x1, y1, z0);
			edge(buffer, edgesX, r, g, b, ox, oy, oz, x0, y1, z1, x1, y1, z1);
			edge(buffer, edgesZ, r, g, b, ox, oy, oz, x0, y1, z0, x0, y1, z1);
			edge(buffer, edgesZ, r, g, b, ox, oy, oz, x1, y1, z0, x1, y1, z1);
		}
		if (!cells.containsKey(cellKey(x0, y0 - 1, z0))) {
			edge(buffer, edgesX, r, g, b, ox, oy, oz, x0, y0, z0, x1, y0, z0);
			edge(buffer, edgesX, r, g, b, ox, oy, oz, x0, y0, z1, x1, y0, z1);
			edge(buffer, edgesZ, r, g, b, ox, oy, oz, x0, y0, z0, x0, y0, z1);
			edge(buffer, edgesZ, r, g, b, ox, oy, oz, x1, y0, z0, x1, y0, z1);
		}
		if (!cells.containsKey(cellKey(x0, y0, z1))) {
			edge(buffer, edgesX, r, g, b, ox, oy, oz, x0, y0, z1, x1, y0, z1);
			edge(buffer, edgesX, r, g, b, ox, oy, oz, x0, y1, z1, x1, y1, z1);
			edge(buffer, edgesY, r, g, b, ox, oy, oz, x0, y0, z1, x0, y1, z1);
			edge(buffer, edgesY, r, g, b, ox, oy, oz, x1, y0, z1, x1, y1, z1);
		}
		if (!cells.containsKey(cellKey(x0, y0, z0 - 1))) {
			edge(buffer, edgesX, r, g, b, ox, oy, oz, x0, y0, z0, x1, y0, z0);
			edge(buffer, edgesX, r, g, b, ox, oy, oz, x0, y1, z0, x1, y1, z0);
			edge(buffer, edgesY, r, g, b, ox, oy, oz, x0, y0, z0, x0, y1, z0);
			edge(buffer, edgesY, r, g, b, ox, oy, oz, x1, y0, z0, x1, y1, z0);
		}
	}

	private static void emitBox(BufferBuilder buffer, AxisAlignedBB box) {
		int x0 = (int) Math.floor(box.minX);
		int y0 = (int) Math.floor(box.minY);
		int z0 = (int) Math.floor(box.minZ);
		int x1 = (int) Math.ceil(box.maxX);
		int y1 = (int) Math.ceil(box.maxY);
		int z1 = (int) Math.ceil(box.maxZ);
		line(buffer, x0, y0, z0, x1, y0, z0);
		line(buffer, x0, y0, z1, x1, y0, z1);
		line(buffer, x0, y1, z0, x1, y1, z0);
		line(buffer, x0, y1, z1, x1, y1, z1);
		line(buffer, x0, y0, z0, x0, y1, z0);
		line(buffer, x1, y0, z0, x1, y1, z0);
		line(buffer, x0, y0, z1, x0, y1, z1);
		line(buffer, x1, y0, z1, x1, y1, z1);
		line(buffer, x0, y0, z0, x0, y0, z1);
		line(buffer, x1, y0, z0, x1, y0, z1);
		line(buffer, x0, y1, z0, x0, y1, z1);
		line(buffer, x1, y1, z0, x1, y1, z1);
	}

	private static void line(BufferBuilder buffer, int x0, int y0, int z0, int x1, int y1, int z1) {
		buffer.pos((double) x0, (double) y0, (double) z0).color(0.0F, 0.5F, 1.0F, 1.0F).endVertex();
		buffer.pos((double) x1, (double) y1, (double) z1).color(0.0F, 0.5F, 1.0F, 1.0F).endVertex();
	}

	private static void edge(BufferBuilder buffer, Set<Long> used, float r, float g, float b, double ox, double oy, double oz,
			int x0, int y0, int z0, int x1, int y1, int z1) {
		int cx = Math.min(x0, x1);
		int cy = Math.min(y0, y1);
		int cz = Math.min(z0, z1);
		if (!used.add(cellKey(cx, cy, cz))) return;
		int axis = y0 == y1 && z0 == z1 ? 0 : (x0 == x1 && z0 == z1 ? 1 : 2);
		if (!silhouette(axis, cx, cy, cz)) return;
		buffer.pos((double) x0 + ox, (double) y0 + oy, (double) z0 + oz).color(r, g, b, 1.0F).endVertex();
		buffer.pos((double) x1 + ox, (double) y1 + oy, (double) z1 + oz).color(r, g, b, 1.0F).endVertex();
	}

	private static boolean silhouette(int axis, int x, int y, int z) {
		boolean p00;
		boolean p10;
		boolean p01;
		boolean p11;
		if (axis == 0) {
			p00 = cells.containsKey(cellKey(x, y - 1, z - 1));
			p10 = cells.containsKey(cellKey(x, y, z - 1));
			p01 = cells.containsKey(cellKey(x, y - 1, z));
			p11 = cells.containsKey(cellKey(x, y, z));
		} else if (axis == 1) {
			p00 = cells.containsKey(cellKey(x - 1, y, z - 1));
			p10 = cells.containsKey(cellKey(x, y, z - 1));
			p01 = cells.containsKey(cellKey(x - 1, y, z));
			p11 = cells.containsKey(cellKey(x, y, z));
		} else {
			p00 = cells.containsKey(cellKey(x - 1, y - 1, z));
			p10 = cells.containsKey(cellKey(x, y - 1, z));
			p01 = cells.containsKey(cellKey(x - 1, y, z));
			p11 = cells.containsKey(cellKey(x, y, z));
		}
		boolean cross1 = p00 != p10 || p01 != p11;
		boolean cross2 = p00 != p01 || p10 != p11;
		return cross1 && cross2;
	}

	private static long cellKey(int x, int y, int z) {
		return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | ((long) z & 0x3FFFFFFL);
	}

	private static Frustum makeFrustum(double px, double py, double pz) {
		try {
			Frustum frustum = new Frustum();
			frustum.setPosition(px, py, pz);
			return frustum;
		} catch (Throwable throwable) {
			return null;
		}
	}

	private static void setup() {
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.glLineWidth(2.0F);
		GlStateManager.disableTexture2D();
		GlStateManager.disableCull();
		GlStateManager.disableDepth();
	}

	private static void cleanup() {
		GlStateManager.enableDepth();
		GlStateManager.enableCull();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}
}
