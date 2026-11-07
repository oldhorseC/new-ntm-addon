package com.ntmit.render;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.RBMKDebris;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKRodReaSim;
import com.ntmit.entity.EntityLidPlate;
import com.ntmit.lib.RbmkJumpHandler;
import com.ntmit.lib.RbmkSteamCurve;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
	private static final Set<Long> visibleBases = new HashSet<>();
	private static final Set<Long> reaSimBases = new HashSet<>();
	private static final Map<Long, TileEntityRBMKBase> columnTiles = new HashMap<>();
	private static final Map<Long, Float> columnFills = new HashMap<>();
	private static final Map<Long, Long> hudBurstStart = new HashMap<>();
	private static final Map<Long, Long> hudSecondAt = new HashMap<>();
	private static final Map<Long, Integer> debris = new HashMap<>();
	private static final Map<Long, Integer> corium = new HashMap<>();
	private static final long HUD_ANCHOR_HOLD = 200L;
	private static BlockPos hudAnchor;
	private static long hudAnchorTick = Long.MIN_VALUE;

	private static final float FILL_RED = 1.0F;
	private static final float FILL_GREEN = 0.45F;
	private static final float FILL_BLUE = 0.45F;
	private static final float FILL_ALPHA = 0.32F;
	private static long scanTick = Long.MIN_VALUE;

	public static void setEnabled(boolean value) {
		enabled = value;
		if (!value) reset();
	}

	public static void reset() {
		enabled = false;
		cells.clear();
		edgesX.clear();
		edgesY.clear();
		edgesZ.clear();
		visitedColumns.clear();
		visibleBases.clear();
		reaSimBases.clear();
		columnTiles.clear();
		columnFills.clear();
		hudBurstStart.clear();
		hudSecondAt.clear();
		debris.clear();
		corium.clear();
		hudAnchor = null;
		hudAnchorTick = Long.MIN_VALUE;
		scanTick = Long.MIN_VALUE;
	}

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
		visibleBases.clear();
		reaSimBases.clear();
		columnTiles.clear();
		columnFills.clear();
		for (TileEntity te : world.loadedTileEntityList) {
			if (!(te instanceof TileEntityRBMKBase)) continue;
			BlockPos pos = te.getPos();
			if (!world.isBlockLoaded(pos)) continue;
			BlockPos base = columnBase(world, pos);
			long baseKey = base.toLong();
			if (te instanceof TileEntityRBMKRodReaSim) reaSimBases.add(baseKey);
			if (!columnTiles.containsKey(baseKey)) {
				TileEntity baseTe = world.getTileEntity(base);
				if (baseTe instanceof TileEntityRBMKBase) {
					columnTiles.put(baseKey, (TileEntityRBMKBase) baseTe);
				} else {
					columnTiles.put(baseKey, (TileEntityRBMKBase) te);
				}
			}
			if (!visitedColumns.add(baseKey)) continue;
			AxisAlignedBB box = columnBox(base, height);
			if (box.getCenter().squareDistanceTo(px, py, pz) > rangeSq) continue;
			if (frustum != null && !frustum.isBoundingBoxInFrustum(box)) continue;
			visibleBases.add(baseKey);
			for (int i = 0; i < height; i++) cells.put(cellKey(base.getX(), base.getY() + i, base.getZ()), KIND_COLUMN);
		}
		computeColumnFills();
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
			emitColumnFills(tessellator, buffer, height, -px, -py, -pz);
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
			emitBlastBoxes(buffer, world, Math.max(1, RBMKDials.getColumnHeight(world)), -px, -py, -pz);
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
				GlStateManager.rotate(roll, 0.0F, 1.0F, 0.0F);
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

	private static void ensureColumnTiles(World world) {
		if (!columnTiles.isEmpty()) return;
		reaSimBases.clear();
		for (TileEntity te : world.loadedTileEntityList) {
			if (!(te instanceof TileEntityRBMKBase)) continue;
			BlockPos pos = te.getPos();
			if (!world.isBlockLoaded(pos)) continue;
			BlockPos base = columnBase(world, pos);
			long baseKey = base.toLong();
			if (te instanceof TileEntityRBMKRodReaSim) reaSimBases.add(baseKey);
			if (columnTiles.containsKey(baseKey)) continue;
			TileEntity baseTe = world.getTileEntity(base);
			if (baseTe instanceof TileEntityRBMKBase) {
				columnTiles.put(baseKey, (TileEntityRBMKBase) baseTe);
			} else {
				columnTiles.put(baseKey, (TileEntityRBMKBase) te);
			}
		}
	}

	public static void renderHud(ScaledResolution resolution) {
		if (!enabled) return;
		Minecraft mc = Minecraft.getMinecraft();
		World world = mc.world;
		EntityPlayer player = mc.player;
		if (world == null || player == null) return;
		ensureColumnTiles(world);
		long closestKey = Long.MIN_VALUE;
		double closestDistance = Double.MAX_VALUE;
		for (Map.Entry<Long, TileEntityRBMKBase> entry : columnTiles.entrySet()) {
			BlockPos pos = BlockPos.fromLong(entry.getKey().longValue());
			double dx = (double) pos.getX() + 0.5D - player.posX;
			double dy = (double) pos.getY() + 0.5D - player.posY;
			double dz = (double) pos.getZ() + 0.5D - player.posZ;
			double distance = dx * dx + dy * dy + dz * dz;
			if (distance >= closestDistance) continue;
			closestDistance = distance;
			closestKey = entry.getKey().longValue();
		}
		BlockPos closest = closestKey == Long.MIN_VALUE ? null : BlockPos.fromLong(closestKey);
		long now = world.getTotalWorldTime();
		if (closest != null) {
			hudAnchor = closest;
			hudAnchorTick = now;
		}
		BlockPos anchor = closest;
		if (anchor == null && hudAnchor != null && now - hudAnchorTick <= HUD_ANCHOR_HOLD) anchor = hudAnchor;
		long anchorKey = anchor == null ? Long.MIN_VALUE : anchor.toLong();
		List<TileEntityRBMKBase> group = hudGroup(anchorKey);
		long maxNormal = 0L;
		long maxReaSim = 0L;
		for (TileEntityRBMKBase base : group) {
			if (base instanceof TileEntityRBMKBoiler boiler && boiler.steam != null) {
				maxNormal = Math.max(maxNormal, (long) boiler.steam.getFill());
			}
			maxReaSim = Math.max(maxReaSim, (long) base.reasimSteam);
		}
		boolean boilerMode = false;
		try {
			boilerMode = world.getGameRules().getBoolean("dialReasimBoilers");
		} catch (Throwable throwable) {
		}
		double maxFraction = hudMaxFraction(group);
		RbmkSteamCurve.Zone zone = RbmkSteamCurve.zone(maxFraction);
		int explosionRemaining = anchor == null ? -1 : RbmkJumpHandler.reactorBurstRemaining(world, anchor);
		int forcedRemaining = anchor == null ? -1 : RbmkJumpHandler.forcedBurstRemaining(world, anchor);
		if (forcedRemaining >= 0 && (explosionRemaining < 0 || forcedRemaining < explosionRemaining)) explosionRemaining = forcedRemaining;
		if (explosionRemaining < 0 && anchor != null && !group.isEmpty() && zone == RbmkSteamCurve.Zone.D) {
			Long start = hudBurstStart.get(Long.valueOf(anchorKey));
			if (start == null) {
				start = Long.valueOf(now);
				hudBurstStart.put(Long.valueOf(anchorKey), start);
			}
			explosionRemaining = (int) Math.max(0L, (long) RbmkJumpHandler.BURST_DELAY_TICKS - (now - start.longValue()));
		} else if (zone != RbmkSteamCurve.Zone.D) {
			hudBurstStart.remove(Long.valueOf(anchorKey));
		}
		int secondRemaining = anchor == null ? -1 : RbmkJumpHandler.secondBurstRemaining(world, anchor);
		EntityLidPlate lid = findLid(world, anchor, player);
		if (secondRemaining < 0 && lid != null && !lid.blasted) {
			Long due = hudSecondAt.get(Long.valueOf(anchorKey));
			if (due == null) {
				due = Long.valueOf(now + (long) RbmkJumpHandler.AFTERMATH_DELAY);
				hudSecondAt.put(Long.valueOf(anchorKey), due);
			}
			secondRemaining = (int) Math.max(0L, due.longValue() - now);
		} else if (lid == null || lid.blasted) {
			hudSecondAt.remove(Long.valueOf(anchorKey));
		}
		String lidState = "none";
		String lidCoords = "none";
		if (lid != null) {
			if (lid.blasted) {
				lidState = lid.landed ? "final still" : "falling";
			} else {
				lidState = lid.landed ? "first still" : "ascending";
			}
			lidCoords = String.format(Locale.ROOT, "%.2f,%.2f,%.2f", lid.posX, lid.posY, lid.posZ);
		}
		String title = anchor == null
				? "closest rbmk reacter (none)"
				: "closest rbmk reacter (" + anchor.getX() + "," + anchor.getY() + "," + anchor.getZ() + ")";
		String[] lines = new String[] {
				"Current max steam (normal): " + maxNormal,
				"Current max steam (reasim): " + maxReaSim,
				"RBMK boiler mode: " + boilerMode,
				"Explosion delay remaining: " + (explosionRemaining >= 0 ? String.valueOf(explosionRemaining) : "none"),
				"Until second explosion: " + (secondRemaining >= 0 ? String.valueOf(secondRemaining) : "none"),
				"Lid state: " + lidState,
				"Lid coords: " + lidCoords
		};
		int x = 4;
		int y = 4;
		mc.fontRenderer.drawStringWithShadow(title, x, y, 0xFFFFFF);
		y += 10;
		for (String line : lines) {
			mc.fontRenderer.drawStringWithShadow(line, x, y, 0xD0D0D0);
			y += 10;
		}
	}

	private static double hudMaxFraction(List<TileEntityRBMKBase> group) {
		double max = 0.0D;
		for (TileEntityRBMKBase base : group) {
			double dmax = RbmkSteamCurve.reasimFraction(base.reasimSteam);
			if (base instanceof TileEntityRBMKBoiler boiler && boiler.steam != null && boiler.steam.getTankType() != null) {
				dmax = Math.max(dmax, RbmkSteamCurve.normalFraction(boiler.steam.getFill(), boiler.steam.getTankType()));
			}
			max = Math.max(max, dmax);
		}
		return max;
	}

	private static List<TileEntityRBMKBase> hudGroup(long start) {
		List<TileEntityRBMKBase> group = new ArrayList<>();
		if (start == Long.MIN_VALUE) return group;
		Set<Long> visited = new HashSet<>();
		Deque<Long> queue = new ArrayDeque<>();
		visited.add(Long.valueOf(start));
		queue.add(Long.valueOf(start));
		while (!queue.isEmpty()) {
			Long key = queue.removeFirst();
			TileEntityRBMKBase base = columnTiles.get(key);
			if (base == null) continue;
			group.add(base);
			BlockPos pos = BlockPos.fromLong(key.longValue());
			hudNeighbour(queue, visited, pos.getX() + 1, pos.getY(), pos.getZ());
			hudNeighbour(queue, visited, pos.getX() - 1, pos.getY(), pos.getZ());
			hudNeighbour(queue, visited, pos.getX(), pos.getY(), pos.getZ() + 1);
			hudNeighbour(queue, visited, pos.getX(), pos.getY(), pos.getZ() - 1);
		}
		return group;
	}

	private static void hudNeighbour(Deque<Long> queue, Set<Long> visited, int x, int y, int z) {
		long key = new BlockPos(x, y, z).toLong();
		if (!columnTiles.containsKey(Long.valueOf(key))) return;
		if (!visited.add(Long.valueOf(key))) return;
		queue.add(Long.valueOf(key));
	}

	private static EntityLidPlate findLid(World world, BlockPos center, EntityPlayer player) {
		if (world == null || player == null) return null;
		double centerX = center != null ? (double) center.getX() + 0.5D : player.posX;
		double centerY = center != null ? (double) center.getY() + 0.5D : player.posY;
		double centerZ = center != null ? (double) center.getZ() + 0.5D : player.posZ;
		EntityLidPlate best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Entity entity : world.loadedEntityList) {
			if (!(entity instanceof EntityLidPlate)) continue;
			EntityLidPlate plate = (EntityLidPlate) entity;
			double dx = plate.posX - centerX;
			double dy = plate.posY - centerY;
			double dz = plate.posZ - centerZ;
			double distance = dx * dx + dy * dy + dz * dz;
			if (distance >= bestDistance) continue;
			bestDistance = distance;
			best = plate;
		}
		return best;
	}

	private static void computeColumnFills() {
		Set<Long> grouped = new HashSet<>();
		for (Long start : columnTiles.keySet()) {
			if (!grouped.add(start)) continue;
			List<Long> group = new ArrayList<>();
			Deque<Long> queue = new ArrayDeque<>();
			boolean reaSim = false;
			double reaSimSum = 0.0D;
			queue.add(start);
			while (!queue.isEmpty()) {
				Long key = queue.removeFirst();
				group.add(key);
				TileEntityRBMKBase base = columnTiles.get(key);
				if (base == null) continue;
				if (reaSimBases.contains(key)) reaSim = true;
				reaSimSum += (double) base.reasimSteam;
				BlockPos pos = BlockPos.fromLong(key.longValue());
				addColumnNeighbour(queue, grouped, pos.getX() + 1, pos.getY(), pos.getZ());
				addColumnNeighbour(queue, grouped, pos.getX() - 1, pos.getY(), pos.getZ());
				addColumnNeighbour(queue, grouped, pos.getX(), pos.getY(), pos.getZ() + 1);
				addColumnNeighbour(queue, grouped, pos.getX(), pos.getY(), pos.getZ() - 1);
			}
			if (reaSim) {
				int denominator = Math.max(1, RbmkSteamCurve.REASIM_CAPACITY);
				float fill = clamp((float) (reaSimSum / (double) Math.max(1, group.size()) / (double) denominator));
				if (fill <= 0.0F) continue;
				for (Long key : group) columnFills.put(key, Float.valueOf(fill));
			} else {
				for (Long key : group) {
					TileEntityRBMKBase base = columnTiles.get(key);
					if (!(base instanceof TileEntityRBMKBoiler)) continue;
					float fill = boilerFill((TileEntityRBMKBoiler) base);
					if (fill > 0.0F) columnFills.put(key, Float.valueOf(fill));
				}
			}
		}
	}

	private static void addColumnNeighbour(Deque<Long> queue, Set<Long> grouped, int x, int y, int z) {
		long key = new BlockPos(x, y, z).toLong();
		if (!columnTiles.containsKey(Long.valueOf(key))) return;
		if (!grouped.add(Long.valueOf(key))) return;
		queue.add(Long.valueOf(key));
	}

	private static float boilerFill(TileEntityRBMKBoiler boiler) {
		if (boiler == null || boiler.steam == null || boiler.steam.getTankType() == null) return 0.0F;
		int threshold = RbmkSteamCurve.normalThreshold(boiler.steam.getTankType());
		if (threshold <= 0) return 0.0F;
		return clamp((float) boiler.steam.getFill() / (float) threshold);
	}

	private static float clamp(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private static void emitColumnFills(Tessellator tessellator, BufferBuilder buffer, int height, double ox, double oy, double oz) {
		boolean hasFill = false;
		for (Long key : visibleBases) {
			Float fill = columnFills.get(key);
			if (fill != null && fill.floatValue() > 0.0F) {
				hasFill = true;
				break;
			}
		}
		if (!hasFill) return;
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		for (Long key : visibleBases) {
			Float fill = columnFills.get(key);
			if (fill == null || fill.floatValue() <= 0.0F) continue;
			BlockPos base = BlockPos.fromLong(key.longValue());
			double x0 = (double) base.getX() + ox;
			double y0 = (double) base.getY() + oy;
			double z0 = (double) base.getZ() + oz;
			double x1 = x0 + 1.0D;
			double z1 = z0 + 1.0D;
			double y1 = y0 + (double) height * (double) fill.floatValue();
			fillBox(buffer, x0, y0, z0, x1, y1, z1);
		}
		tessellator.draw();
	}

	private static void fillBox(BufferBuilder buffer, double x0, double y0, double z0, double x1, double y1, double z1) {
		fillQuad(buffer, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		fillQuad(buffer, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		fillQuad(buffer, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
		fillQuad(buffer, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		fillQuad(buffer, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		fillQuad(buffer, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
	}

	private static void fillQuad(BufferBuilder buffer, double ax, double ay, double az, double bx, double by, double bz,
			double cx, double cy, double cz, double dx, double dy, double dz) {
		buffer.pos(ax, ay, az).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
		buffer.pos(bx, by, bz).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
		buffer.pos(cx, cy, cz).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
		buffer.pos(dx, dy, dz).color(FILL_RED, FILL_GREEN, FILL_BLUE, FILL_ALPHA).endVertex();
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

	private static void line(BufferBuilder buffer, int x0, int y0, int z0, int x1, int y1, int z1, float red,
			float green, float blue) {
		buffer.pos((double) x0, (double) y0, (double) z0).color(red, green, blue, 1.0F).endVertex();
		buffer.pos((double) x1, (double) y1, (double) z1).color(red, green, blue, 1.0F).endVertex();
	}

	private static void emitBlastBoxes(BufferBuilder buffer, World world, int columnHeight, double ox, double oy,
			double oz) {

		Set<Long> done = new HashSet<>();
		for (Long key : visibleBases) {
			if (key == null || !done.add(key)) continue;
			Deque<Long> queue = new ArrayDeque<>();
			queue.add(key);
			int minX = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE;
			int minZ = Integer.MAX_VALUE;
			int maxZ = Integer.MIN_VALUE;
			while (!queue.isEmpty()) {
				BlockPos pos = BlockPos.fromLong(queue.poll().longValue());
				minX = Math.min(minX, pos.getX());
				maxX = Math.max(maxX, pos.getX());
				minY = Math.min(minY, pos.getY());
				minZ = Math.min(minZ, pos.getZ());
				maxZ = Math.max(maxZ, pos.getZ());
				for (EnumFacing facing : new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST,
						EnumFacing.EAST }) {
					BlockPos next = pos.offset(facing);
					long nextKey = next.toLong();
					if (!visibleBases.contains(Long.valueOf(nextKey))) continue;
					if (!done.add(Long.valueOf(nextKey))) continue;
					queue.add(Long.valueOf(nextKey));
				}
			}
			if (minY == Integer.MAX_VALUE) continue;
			int[] reactorBounds = new int[] { minX, maxX, minZ, maxZ };
			int blastTop = com.ntmit.lib.RbmkBlastRaycast.blastTop(world, minY, reactorBounds, columnHeight);
			double x0 = (double) (minX - com.ntmit.lib.RbmkBlastRaycast.EXPAND);
			double x1 = (double) (maxX + 1 + com.ntmit.lib.RbmkBlastRaycast.EXPAND);
			double z0 = (double) (minZ - com.ntmit.lib.RbmkBlastRaycast.EXPAND);
			double z1 = (double) (maxZ + 1 + com.ntmit.lib.RbmkBlastRaycast.EXPAND);
			double y0 = (double) (minY + columnHeight + com.ntmit.lib.RbmkBlastRaycast.BOTTOM_OFFSET);
			double y1 = (double) (blastTop + 1);
			boxLine(buffer, ox, oy, oz, x0, y1, z0, x1, y1, z0, 0.2F, 0.9F, 1.0F);
			boxLine(buffer, ox, oy, oz, x0, y1, z1, x1, y1, z1, 0.2F, 0.9F, 1.0F);
			boxLine(buffer, ox, oy, oz, x0, y1, z0, x0, y1, z1, 0.2F, 0.9F, 1.0F);
			boxLine(buffer, ox, oy, oz, x1, y1, z0, x1, y1, z1, 0.2F, 0.9F, 1.0F);
			boxLine(buffer, ox, oy, oz, x0, y0, z0, x0, y1, z0, 0.2F, 0.9F, 1.0F);
			boxLine(buffer, ox, oy, oz, x1, y0, z0, x1, y1, z0, 0.2F, 0.9F, 1.0F);
			boxLine(buffer, ox, oy, oz, x0, y0, z1, x0, y1, z1, 0.2F, 0.9F, 1.0F);
			boxLine(buffer, ox, oy, oz, x1, y0, z1, x1, y1, z1, 0.2F, 0.9F, 1.0F);
			boxLine(buffer, ox, oy, oz, x0, y0, z0, x1, y0, z0, 0.05F, 0.25F, 0.4F);
			boxLine(buffer, ox, oy, oz, x0, y0, z1, x1, y0, z1, 0.05F, 0.25F, 0.4F);
			boxLine(buffer, ox, oy, oz, x0, y0, z0, x0, y0, z1, 0.05F, 0.25F, 0.4F);
			boxLine(buffer, ox, oy, oz, x1, y0, z0, x1, y0, z1, 0.05F, 0.25F, 0.4F);
		}
	}

	private static void boxLine(BufferBuilder buffer, double ox, double oy, double oz, double x0, double y0, double z0,
			double x1, double y1, double z1, float red, float green, float blue) {
		buffer.pos(x0 + ox, y0 + oy, z0 + oz).color(red, green, blue, 1.0F).endVertex();
		buffer.pos(x1 + ox, y1 + oy, z1 + oz).color(red, green, blue, 1.0F).endVertex();
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
