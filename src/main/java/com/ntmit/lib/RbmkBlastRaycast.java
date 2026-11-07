package com.ntmit.lib;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.hbm.util.DelayedTick;
import com.ntmit.entity.EntityBlastChunk;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RbmkBlastRaycast {

	public static final int EXPAND = 7;
	public static final int BOTTOM_OFFSET = 1;
	public static final int EXTRA_HEIGHT = 5;
	public static final int RAYS_PER_TICK = 384;
	public static final int BLOCKS_PER_CHUNK = 27;
	public static final int MAX_CHUNKS = 64;
	public static final int MAX_STEPS = 128;
	public static final int MAX_PILE = 6;
	public static final double REACH_MIN = 39.875D;
	public static final double REACH_MAX = 46.2D;
	public static final double BLAST_RESIST_LIMIT = 2000.0D;
	public static final double RAGGED_TOP = 0.5D;
	public static final double RAGGED_KEEP = 0.65D;
	public static final int[] CHUNK_SIZES = new int[] { 27, 18, 12, 8, 6, 4, 2, 1 };
	public static final double LIFT_MIN = 0.20D;
	public static final double LIFT_MAX = 0.60D;
	public static final double LIFT_TOP_BONUS = 0.25D;
	public static final double THRUST_MIN = 0.05D;
	public static final double THRUST_MAX = 0.11D;
	public static final double THRUST_TOP_BONUS = 0.02D;
	public static final int THRUST_TICKS = 8;
	public static final double REACH_GAMMA = 2.0D;
	public static final double REACH_EDGE = 0.35D;
	public static final double REACH_TOP_EDGE = 0.85D;
	public static final double SPEED_MIN = 0.4D;
	public static final double SPEED_MAX = 3.2D;
	public static final double TOP_SPEED_MIN = 3.2D;
	public static final double TOP_SPEED_MAX = 4.4D;
	public static final double KNOCK_SCALE = 0.55D;

	public static void schedule(World world, BlockPos origin, int[] bounds, int columnHeight) {

		if (world == null || world.isRemote || origin == null || bounds == null) return;
		Pass pass = new Pass(world, origin, bounds, Math.max(columnHeight, 1));
		pass.build();
		int batches = Math.max(1, (pass.targets.size() + RAYS_PER_TICK - 1) / RAYS_PER_TICK);
		for (int i = 0; i < batches; i++) {
			final int index = i;
			final int last = batches - 1;
			DelayedTick.scheduleWorldStart(world, i + 1, delayed -> {
				pass.run(index);
				if (index == last) pass.finish();
			});
		}
	}

	private static final class Pass {

		final World world;
		final int baseY;
		final int bottomY;
		final int topY;
		final int minX;
		final int maxX;
		final int minZ;
		final int maxZ;
		final int reactorMinX;
		final int reactorMaxX;
		final int reactorMinZ;
		final int reactorMaxZ;
		final int reactorTop;
		final double cx;
		final double cy;
		final double cz;
		final List<double[]> targets = new ArrayList<>();
		final Map<Long, Group> groups = new LinkedHashMap<>();
		final long salt;

		Pass(World world, BlockPos origin, int[] bounds, int height) {

			this.world = world;
			this.salt = world.rand.nextLong();
			this.reactorMinX = Math.min(bounds[0], bounds[1]);
			this.reactorMaxX = Math.max(bounds[0], bounds[1]);
			this.reactorMinZ = Math.min(bounds[2], bounds[3]);
			this.reactorMaxZ = Math.max(bounds[2], bounds[3]);
			this.baseY = origin.getY();
			this.reactorTop = this.baseY + height - 1;
			this.bottomY = this.baseY + height + BOTTOM_OFFSET;
			this.topY = this.bottomY + (height + EXPAND + EXTRA_HEIGHT) - 1;
			this.minX = this.reactorMinX - EXPAND;
			this.maxX = this.reactorMaxX + EXPAND;
			this.minZ = this.reactorMinZ - EXPAND;
			this.maxZ = this.reactorMaxZ + EXPAND;
			this.cx = ((double) (this.reactorMinX + this.reactorMaxX) + 1.0D) * 0.5D;
			this.cz = ((double) (this.reactorMinZ + this.reactorMaxZ) + 1.0D) * 0.5D;
			this.cy = ((double) (this.bottomY + this.topY) + 1.0D) * 0.5D;
		}

		void build() {

			for (int y = this.bottomY; y <= this.topY; y++) {
				for (int z = this.minZ; z <= this.maxZ; z++) {
					this.ray(this.cx, this.cy, this.cz, (double) this.minX + 0.5D, (double) y + 0.5D,
							(double) z + 0.5D + jitter(this.world));
					this.ray(this.cx, this.cy, this.cz, (double) this.maxX + 0.5D, (double) y + 0.5D,
							(double) z + 0.5D + jitter(this.world));
				}
				for (int x = this.minX; x <= this.maxX; x++) {
					this.ray(this.cx, this.cy, this.cz, (double) x + 0.5D + jitter(this.world), (double) y + 0.5D,
							(double) this.minZ + 0.5D);
					this.ray(this.cx, this.cy, this.cz, (double) x + 0.5D + jitter(this.world), (double) y + 0.5D,
							(double) this.maxZ + 0.5D);
				}
			}
			for (int x = this.minX; x <= this.maxX; x++) {
				for (int z = this.minZ; z <= this.maxZ; z++) {
					this.ray(this.cx, this.cy, this.cz, (double) x + 0.5D + jitter(this.world),
							(double) this.topY + 0.5D, (double) z + 0.5D + jitter(this.world));
				}
			}
		}

		void ray(double ox, double oy, double oz, double tx, double ty, double tz) {

			this.rayRaw(ox, oy, oz, tx, ty, tz, this.reach(tx, ty, tz));
		}

		void rayRaw(double ox, double oy, double oz, double tx, double ty, double tz, double reach) {

			this.targets.add(new double[] { ox, oy, oz, tx, ty, tz, Math.max(0.0D, reach) });
		}

		void run(int batch) {

			int from = batch * RAYS_PER_TICK;
			int to = Math.min(this.targets.size(), from + RAYS_PER_TICK);
			for (int i = from; i < to; i++) {
				this.cast(this.targets.get(i));
			}
		}

		boolean topFace(double x, double y, double z) {

			double dTop = (double) this.topY - y;
			double dX = Math.min(x - (double) this.minX, (double) this.maxX + 1.0D - x);
			double dZ = Math.min(z - (double) this.minZ, (double) this.maxZ + 1.0D - z);
			return dTop <= dX && dTop <= dZ;
		}

		double power(double x, double y, double z) {

			double halfX = ((double) (this.maxX - this.minX) + 1.0D) * 0.5D;
			double halfZ = ((double) (this.maxZ - this.minZ) + 1.0D) * 0.5D;
			if (this.topFace(x, y, z)) {
				double dx = x - this.cx;
				double dz = z - this.cz;
				double radius = Math.sqrt(halfX * halfX + halfZ * halfZ);
				double r = Math.min(1.0D, Math.sqrt(dx * dx + dz * dz) / radius);
				return REACH_TOP_EDGE + (1.0D - REACH_TOP_EDGE) * (1.0D - r);
			}
			double dX = Math.min(x - (double) this.minX, (double) this.maxX + 1.0D - x);
			double dZ = Math.min(z - (double) this.minZ, (double) this.maxZ + 1.0D - z);
			double t = MathHelper.clamp((y - (double) this.bottomY)
					/ (double) Math.max(1, this.topY - this.bottomY), 0.0D, 1.0D);
			double u = dX <= dZ ? Math.min(1.0D, Math.abs(z - this.cz) / halfZ)
					: Math.min(1.0D, Math.abs(x - this.cx) / halfX);
			return Math.pow(t, REACH_GAMMA) * (1.0D - REACH_EDGE * u);
		}

		double reach(double x, double y, double z) {

			return REACH_MIN + (REACH_MAX - REACH_MIN) * this.power(x, y, z);
		}

		double throwSpeed(double x, double y, double z) {

			double power = this.power(x, y, z);
			if (this.topFace(x, y, z)) return TOP_SPEED_MIN + (TOP_SPEED_MAX - TOP_SPEED_MIN) * power;
			return SPEED_MIN + (SPEED_MAX - SPEED_MIN) * power;
		}

		double resistance(IBlockState state, BlockPos pos) {

			try {
				return state.getBlock().getExplosionResistance(null);
			} catch (Throwable throwable) {
				return 0.0D;
			}
		}

		boolean ragged(BlockPos pos) {

			double t = (double) (pos.getY() - this.bottomY)
					/ (double) Math.max(1, this.topY - this.bottomY);
			if (t >= RAGGED_TOP) return false;
			double keep = RAGGED_KEEP * (1.0D - t / RAGGED_TOP);
			if (keep <= 0.0D) return false;
			return this.noise(pos.getX(), pos.getY(), pos.getZ()) < keep;
		}

		double noise(int x, int y, int z) {

			long h = this.salt;
			h = h * 6364136223846793005L + (long) x * 0x9E3779B97F4A7C15L;
			h ^= h >>> 33;
			h *= 0xff51afd7ed558ccdL;
			h = h * 6364136223846793005L + (long) y * 0x94D049BB133111EBL;
			h ^= h >>> 29;
			h *= 0xc4ceb9fe1a85ec53L;
			h = h * 6364136223846793005L + (long) z * 0xD6E8FEB86659FD93L;
			h ^= h >>> 32;
			double fine = (double) (h >>> 11) / (double) (1L << 53);

			long c = this.salt;
			c = c * 6364136223846793005L + (long) (x >> 1) * 0x9E3779B97F4A7C15L;
			c ^= c >>> 33;
			c *= 0xff51afd7ed558ccdL;
			c = c * 6364136223846793005L + (long) (y >> 1) * 0x94D049BB133111EBL;
			c ^= c >>> 29;
			c *= 0xc4ceb9fe1a85ec53L;
			c = c * 6364136223846793005L + (long) (z >> 1) * 0xD6E8FEB86659FD93L;
			c ^= c >>> 32;
			double coarse = (double) (c >>> 11) / (double) (1L << 53);
			return coarse * 0.6D + fine * 0.4D;
		}

		void cast(double[] target) {

			double ox = target[0];
			double oy = target[1];
			double oz = target[2];
			double dx = target[3] - ox;
			double dy = target[4] - oy;
			double dz = target[5] - oz;
			double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (length < 1.0E-4D) return;
			dx /= length;
			dy /= length;
			dz /= length;

			int x = (int) Math.floor(ox);
			int y = (int) Math.floor(oy);
			int z = (int) Math.floor(oz);
			int stepX = dx > 0.0D ? 1 : -1;
			int stepY = dy > 0.0D ? 1 : -1;
			int stepZ = dz > 0.0D ? 1 : -1;
			double tDeltaX = Math.abs(dx) < 1.0E-9D ? Double.MAX_VALUE : Math.abs(1.0D / dx);
			double tDeltaY = Math.abs(dy) < 1.0E-9D ? Double.MAX_VALUE : Math.abs(1.0D / dy);
			double tDeltaZ = Math.abs(dz) < 1.0E-9D ? Double.MAX_VALUE : Math.abs(1.0D / dz);
			double tMaxX = Math.abs(dx) < 1.0E-9D ? Double.MAX_VALUE
					: (dx > 0.0D ? (double) (x + 1) - ox : ox - (double) x) / Math.abs(dx);
			double tMaxY = Math.abs(dy) < 1.0E-9D ? Double.MAX_VALUE
					: (dy > 0.0D ? (double) (y + 1) - oy : oy - (double) y) / Math.abs(dy);
			double tMaxZ = Math.abs(dz) < 1.0E-9D ? Double.MAX_VALUE
					: (dz > 0.0D ? (double) (z + 1) - oz : oz - (double) z) / Math.abs(dz);

			for (int i = 0; i < MAX_STEPS; i++) {
				if (tMaxX < tMaxY) {
					if (tMaxX < tMaxZ) {
						x += stepX;
						tMaxX += tDeltaX;
					} else {
						z += stepZ;
						tMaxZ += tDeltaZ;
					}
				} else {
					if (tMaxY < tMaxZ) {
						y += stepY;
						tMaxY += tDeltaY;
					} else {
						z += stepZ;
						tMaxZ += tDeltaZ;
					}
				}
				if (x < this.minX || x > this.maxX || z < this.minZ || z > this.maxZ) return;
				if (y < this.bottomY || y > this.topY) return;
				double reach = target[6];
				double distX = (double) x + 0.5D - ox;
				double distY = (double) y + 0.5D - oy;
				double distZ = (double) z + 0.5D - oz;
				if (distX * distX + distY * distY + distZ * distZ > reach * reach) return;
				if (x >= this.reactorMinX && x <= this.reactorMaxX && z >= this.reactorMinZ && z <= this.reactorMaxZ
						&& y <= this.reactorTop) {
					continue;
				}
				BlockPos pos = new BlockPos(x, y, z);
				if (!this.world.isBlockLoaded(pos)) return;
				IBlockState state = this.world.getBlockState(pos);
				if (state.getBlock().isAir(state, this.world, pos)) continue;
				if (protectedBlock(this.world, pos, state)) return;
				if (this.resistance(state, pos) >= BLAST_RESIST_LIMIT) return;
				if (this.ragged(pos)) continue;
				this.destroy(pos, state);
			}
		}

		void destroy(BlockPos pos, IBlockState state) {

			this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
			Long key = Long.valueOf(cellKey(pos));
			Group group = this.groups.get(key);
			if (group == null) {
				group = new Group();
				this.groups.put(key, group);
			}
			group.weight += blockWeight(this.world, pos, state);
			if (group.blocks.size() >= BLOCKS_PER_CHUNK) return;
			group.blocks.add(new int[] { pos.getX(), pos.getY(), pos.getZ(), Block.getStateId(state) });
		}

		void finish() {

			knockback(this);
			if (NTMITWorldRules.mobileMode(this.world)) return;
			spawnChunks(this);
		}
	}

	private static final class Group {
		final List<int[]> blocks = new ArrayList<>(BLOCKS_PER_CHUNK);
		float weight;
	}

	private static void spawnChunks(Pass pass) {

		int spawned = 0;
		for (Group group : pass.groups.values()) {
			if (spawned >= MAX_CHUNKS) break;
			if (group.blocks.isEmpty()) continue;
			int wanted = CHUNK_SIZES[pass.world.rand.nextInt(CHUNK_SIZES.length)];
			int size = Math.min(group.blocks.size(), Math.min(BLOCKS_PER_CHUNK, Math.max(1, wanted)));
			int start = group.blocks.size() <= size ? 0 : pass.world.rand.nextInt(group.blocks.size());
			int baseX = Integer.MAX_VALUE;
			int baseY = Integer.MAX_VALUE;
			int baseZ = Integer.MAX_VALUE;
			double sumX = 0.0D;
			double sumY = 0.0D;
			double sumZ = 0.0D;
			int[] states = new int[size];
			int[] positions = new int[size * 3];
			for (int i = 0; i < size; i++) {
				int[] block = group.blocks.get((start + i) % group.blocks.size());
				baseX = Math.min(baseX, block[0]);
				baseY = Math.min(baseY, block[1]);
				baseZ = Math.min(baseZ, block[2]);
				sumX += (double) block[0] + 0.5D;
				sumY += (double) block[1] + 0.5D;
				sumZ += (double) block[2] + 0.5D;
				states[i] = block[3];
				positions[i * 3] = block[0];
				positions[i * 3 + 1] = block[1];
				positions[i * 3 + 2] = block[2];
			}
			double centerX = sumX / (double) size;
			double centerY = sumY / (double) size;
			double centerZ = sumZ / (double) size;
			float ratio = (float) size / (float) Math.max(1, group.blocks.size());
			int pile = MathHelper.clamp(Math.round(group.weight * ratio), 1, MAX_PILE);
			EntityBlastChunk chunk = new EntityBlastChunk(pass.world);
			chunk.configure(baseX, baseY, baseZ, states, positions, pile);
			double dx = centerX - pass.cx;
			double dz = centerZ - pass.cz;
			double horizontal = Math.sqrt(dx * dx + dz * dz);
			double t = MathHelper.clamp((centerY - (double) pass.bottomY)
					/ (double) Math.max(1, pass.topY - pass.bottomY), 0.0D, 1.0D);
			boolean top = pass.topFace(centerX, centerY, centerZ);
			double speed = pass.throwSpeed(centerX, centerY, centerZ);
			double vx = horizontal < 1.0E-3D ? (pass.world.rand.nextDouble() - 0.5D) * 0.4D : dx / horizontal * speed;
			double vz = horizontal < 1.0E-3D ? (pass.world.rand.nextDouble() - 0.5D) * 0.4D : dz / horizontal * speed;
			chunk.motionX = vx * (0.8D + pass.world.rand.nextDouble() * 0.4D);
			chunk.motionZ = vz * (0.8D + pass.world.rand.nextDouble() * 0.4D);
			chunk.motionY = LIFT_MIN + (LIFT_MAX - LIFT_MIN) * t + (top ? LIFT_TOP_BONUS : 0.0D);
			chunk.setThrust(THRUST_MIN + (THRUST_MAX - THRUST_MIN) * t + (top ? THRUST_TOP_BONUS : 0.0D),
					THRUST_TICKS);
			chunk.setSpin((pass.world.rand.nextBoolean() ? 1.0F : -1.0F)
					* (2.0F + pass.world.rand.nextFloat() * 6.0F));
			pass.world.spawnEntity(chunk);
			spawned++;
		}
	}

	private static void knockback(Pass pass) {

		AxisAlignedBB area = new AxisAlignedBB((double) pass.minX, (double) pass.bottomY,
				(double) pass.minZ, (double) pass.maxX + 1.0D, (double) pass.topY + 1.0D, (double) pass.maxZ + 1.0D);
		for (EntityLivingBase entity : pass.world.getEntitiesWithinAABB(EntityLivingBase.class, area)) {
			if (entity == null || entity.isDead) continue;
			double dx = entity.posX - pass.cx;
			double dz = entity.posZ - pass.cz;
			double horizontal = Math.sqrt(dx * dx + dz * dz);
			double nx = horizontal < 1.0E-3D ? 0.0D : dx / horizontal;
			double nz = horizontal < 1.0E-3D ? 0.0D : dz / horizontal;
			double t = MathHelper.clamp((entity.posY - (double) pass.baseY)
					/ (double) Math.max(1, pass.topY - pass.baseY), 0.0D, 1.0D);
			double speed = pass.throwSpeed(entity.posX, entity.posY, entity.posZ) * KNOCK_SCALE;
			entity.motionX += nx * speed;
			entity.motionZ += nz * speed;
			entity.motionY += 0.35D + 0.55D * t;
			entity.velocityChanged = true;
		}
	}

	private static boolean protectedBlock(World world, BlockPos pos, IBlockState state) {

		Block block = state.getBlock();
		if (block instanceof RBMKBase) return true;
		if (block == com.hbm.blocks.ModBlocks.pribris) return true;
		if (block instanceof com.hbm.blocks.machine.rbmk.RBMKDebris) return true;
		if (state.getBlockHardness(world, pos) < 0.0F) return true;
		net.minecraft.util.ResourceLocation name = block.getRegistryName();
		if (name != null && name.getPath().contains("pribris")) return true;
		return world.getTileEntity(pos) instanceof TileEntityRBMKBase;
	}

	private static float blockWeight(World world, BlockPos pos, IBlockState state) {

		Material material = state.getMaterial();
		if (material.isLiquid()) return 0.0F;
		if (state.getBlockHardness(world, pos) < 0.0F) return 0.0F;
		if (material == Material.LEAVES || material == Material.PLANTS || material == Material.GLASS
				|| material == Material.WEB || material == Material.CLOTH || material == Material.VINE) {
			return 0.1F;
		}
		if (material == Material.GROUND || material == Material.SAND || material == Material.CLAY
				|| material == Material.SNOW || material == Material.CRAFTED_SNOW) {
			return 0.6F;
		}
		return 1.0F;
	}

	private static long cellKey(BlockPos pos) {

		long x = (long) (Math.floorDiv(pos.getX(), 3) & 0x1FFFFF);
		long y = (long) (Math.floorDiv(pos.getY(), 3) & 0x1FFFFF);
		long z = (long) (Math.floorDiv(pos.getZ(), 3) & 0x1FFFFF);
		return (x << 42) | (y << 21) | z;
	}

	private static double jitter(World world) {
		return (world.rand.nextDouble() - 0.5D) * 0.75D;
	}

	private RbmkBlastRaycast() {
	}
}
